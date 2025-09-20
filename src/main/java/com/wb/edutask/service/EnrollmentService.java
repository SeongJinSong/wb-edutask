package com.wb.edutask.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.dto.BulkEnrollmentRequestDto;
import com.wb.edutask.dto.BulkEnrollmentResponseDto;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentResponseDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Enrollment;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.CourseStatus;
import com.wb.edutask.enums.EnrollmentStatus;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 수강신청 관리를 위한 서비스 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentService {
    
    private final EnrollmentRepository enrollmentRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentQueueService enrollmentQueueService;
    private final RedisConcurrencyService redisConcurrencyService;
    private final StringRedisTemplate stringRedisTemplate;
    
    
    /**
     * 수강신청을 처리합니다 (Lua 스크립트 동기 실행)
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return 생성된 수강신청 정보
     * @throws RuntimeException 유효하지 않은 요청인 경우
     */
    @Transactional
    public EnrollmentResponseDto enrollCourse(EnrollmentRequestDto enrollmentRequestDto) {
        log.info("수강신청 처리 시작 - StudentId: {}, CourseId: {}", 
                enrollmentRequestDto.getStudentId(), enrollmentRequestDto.getCourseId());
        
        // 1. 기본 검증 (회원 및 강의 존재 확인)
        Member member = memberRepository.findById(enrollmentRequestDto.getStudentId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + enrollmentRequestDto.getStudentId()));
        
        Course course = courseRepository.findById(enrollmentRequestDto.getCourseId())
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + enrollmentRequestDto.getCourseId()));
        
        validateEnrollmentBasic(member, course);
        
        // 2. Lua 스크립트를 통한 원자적 정원 확인 및 처리 (동기 실행)
        Map<String, Object> luaResult = redisConcurrencyService.executeEnrollmentLuaScript(
            enrollmentRequestDto.getStudentId(), 
            enrollmentRequestDto.getCourseId()
        );
        
        Boolean success = (Boolean) luaResult.get("success");
        String message = (String) luaResult.get("message");
        Long newCount = (Long) luaResult.get("newStudentCount");
        
        log.info("🔍 Lua 결과 확인 - StudentId: {}, Success: {}, Message: {}, NewCount: {}", 
                enrollmentRequestDto.getStudentId(), success, message, newCount);
        
        if (!success) {
            // Lua 스크립트에서 실패 → DB 저장하지 않음
            String koreanMessage = redisConcurrencyService.convertRedisMessageToKorean(message);
            log.info("❌ Lua 실패로 DB 저장 안함 - StudentId: {}, Reason: {}", 
                    enrollmentRequestDto.getStudentId(), koreanMessage);
            throw new RuntimeException(koreanMessage);
        }
        
        log.info("✅ Lua 성공으로 DB 저장 진행 - StudentId: {}, NewCount: {}", 
                enrollmentRequestDto.getStudentId(), newCount);
        
        // 3. Lua 스크립트 성공 → DB에 저장
        // Lua 스크립트가 성공했다는 것은 정원 확인이 완료되었다는 의미
        Enrollment enrollment = new Enrollment(member, course);
        enrollment.approve(); // Lua 스크립트에서 이미 정원 확인했으므로 자동 승인
        
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        // Course의 currentStudents를 원자적으로 증가 (Redis와 동기화)
        courseRepository.incrementCurrentStudents(enrollmentRequestDto.getCourseId());
        
        log.info("수강신청 완료 - StudentId: {}, CourseId: {}", 
                enrollmentRequestDto.getStudentId(), enrollmentRequestDto.getCourseId());
        
        return EnrollmentResponseDto.from(savedEnrollment, course);
    }
    
    /**
     * Redis Queue를 사용하여 수강신청을 처리합니다 (DB 락 문제 해결)
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return 큐 ID (비동기 처리)
     */
    public String enrollCourseWithQueue(EnrollmentRequestDto enrollmentRequestDto) {
        // 1. 기본 검증 (회원 및 강의 존재 확인)
        Member member = memberRepository.findById(enrollmentRequestDto.getStudentId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + enrollmentRequestDto.getStudentId()));
        
        Course course = courseRepository.findById(enrollmentRequestDto.getCourseId())
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + enrollmentRequestDto.getCourseId()));
        
        validateEnrollmentBasic(member, course);
        
        // 2. Redis Queue를 통한 원자적 처리
        String queueId = enrollmentQueueService.enqueueEnrollmentRequest(
            enrollmentRequestDto.getStudentId(), 
            enrollmentRequestDto.getCourseId()
        );
        
        log.info("수강신청이 큐에 등록되었습니다 - QueueId: {}, StudentId: {}, CourseId: {}", 
                queueId, enrollmentRequestDto.getStudentId(), enrollmentRequestDto.getCourseId());
        
        return queueId;
    }
    
    
    
    /**
     * Redis Queue 처리 결과를 확인하고 DB에 실제 INSERT를 수행합니다
     * 
     * @param queueId 큐 ID
     * @return 수강신청 결과
     */
    @Transactional
    public EnrollmentResponseDto processQueueResult(String queueId) {
        try {
            // 1. 큐 결과 조회
            Object result = enrollmentQueueService.getEnrollmentResult(queueId);
            if (result == null) {
                throw new RuntimeException("큐 결과를 찾을 수 없습니다: " + queueId);
            }
            
            // 2. 결과 파싱 (Map으로 변환)
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) result;
            
            Boolean success = Boolean.valueOf(resultMap.get("success").toString());
            String message = (String) resultMap.get("message");
            Long studentId = Long.valueOf(resultMap.get("studentId").toString());
            Long courseId = Long.valueOf(resultMap.get("courseId").toString());
            
            if (!success) {
                // Redis 메시지를 한국어로 변환
                String koreanMessage = redisConcurrencyService.convertRedisMessageToKorean(message);
                throw new RuntimeException(koreanMessage);
            }
            
            // 3. Redis에서 성공한 경우에만 DB에 실제 INSERT
            Member student = memberRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다: " + studentId));
            
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));
            
            // 4. 중복 신청 확인
            if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                throw new RuntimeException("이미 수강신청한 강의입니다");
            }
            
            // 5. 수강신청 엔티티 생성 및 저장
            Enrollment enrollment = new Enrollment(student, course);
            enrollment.approve(); // Redis에서 이미 정원 확인했으므로 자동 승인
            
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
            
            // 6. 강의 정보 재조회 및 Redis 동기화
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));
            
            // 7. Redis와 DB 동기화 (DB 상태를 Redis에 반영)
            try {
                redisConcurrencyService.syncCourseToRedisIfNeeded(courseId);
                log.debug("Redis 동기화 완료 - CourseId: {}, CurrentStudents: {}", courseId, course.getCurrentStudents());
            } catch (Exception e) {
                log.warn("Redis 동기화 실패: {}", e.getMessage());
            }
            
            log.info("수강신청 DB 저장 완료 - QueueId: {}, StudentId: {}, CourseId: {}", 
                    queueId, studentId, courseId);
            
            return EnrollmentResponseDto.from(savedEnrollment, course);
            
        } catch (Exception e) {
            log.error("큐 결과 처리 실패 - QueueId: {}, Error: {}", queueId, e.getMessage(), e);
            throw new RuntimeException("큐 결과 처리 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * Redis Queue에서 순차적으로 DB INSERT를 처리합니다 (H2 락 문제 해결)
     * 
     * @return 처리된 수강신청 수
     */
    @Transactional
    public int processDbQueue() {
        int processedCount = 0;
        
        try {
            // Redis Queue에서 대기 중인 항목들을 순차적으로 처리
            while (true) {
                Map<String, Object> queueItem = enrollmentQueueService.getNextDbQueueItem();
                if (queueItem == null) {
                    break; // 처리할 항목이 없음
                }
                
                try {
                    String queueId = (String) queueItem.get("queueId");
                    Long studentId = Long.valueOf(queueItem.get("studentId").toString());
                    Long courseId = Long.valueOf(queueItem.get("courseId").toString());
                    
                    log.info("DB 큐 처리 시작 - QueueId: {}, StudentId: {}, CourseId: {}", 
                            queueId, studentId, courseId);
                    
                    // 1. 회원 및 강의 정보 조회
                    Member student = memberRepository.findById(studentId)
                            .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다: " + studentId));
                    
                    Course course = courseRepository.findById(courseId)
                            .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));
                    
                    // 2. 중복 신청 확인 (Redis에서 이미 정원 확인했지만 DB에서도 한 번 더 확인)
                    if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                        log.warn("중복 수강신청 감지 - QueueId: {}, StudentId: {}, CourseId: {}", 
                                queueId, studentId, courseId);
                        enrollmentQueueService.markDbQueueItemCompleted(queueId);
                        continue;
                    }
                    
                    // 3. 수강신청 엔티티 생성 및 저장 (순차적으로 하나씩)
                    Enrollment enrollment = new Enrollment(student, course);
                    enrollment.approve(); // Redis에서 이미 정원 확인했으므로 자동 승인
                    
                    Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
                    
                    // 4. DB 처리 완료 표시
                    enrollmentQueueService.markDbQueueItemCompleted(queueId);
                    
                    processedCount++;
                    log.info("DB 큐 처리 완료 - QueueId: {}, StudentId: {}, CourseId: {}, ProcessedCount: {}", 
                            queueId, studentId, courseId, processedCount);
                    
                } catch (Exception e) {
                    log.error("DB 큐 항목 처리 실패 - QueueItem: {}, Error: {}", queueItem, e.getMessage(), e);
                    // 실패한 항목은 큐에서 제거하지 않고 다음으로 넘어감
                }
            }
            
            if (processedCount > 0) {
                log.info("DB 큐 처리 완료 - 총 처리된 수강신청 수: {}", processedCount);
            }
            
        } catch (Exception e) {
            log.error("DB 큐 처리 중 오류 발생: {}", e.getMessage(), e);
        }
        
        return processedCount;
    }
    
    /**
     * 수강신청 ID로 수강신청 정보를 조회합니다
     * 
     * @param enrollmentId 수강신청 ID
     * @return 수강신청 정보
     * @throws RuntimeException 수강신청을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public EnrollmentResponseDto getEnrollmentById(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
        
        return EnrollmentResponseDto.from(enrollment);
    }
    
    /**
     * 학생별 수강신청 목록을 조회합니다
     * 
     * @param studentId 학생 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentResponseDto> getEnrollmentsByStudent(Long studentId, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId, pageable);
        return enrollments.map(EnrollmentResponseDto::from);
    }
    
    /**
     * 강의별 수강신청 목록을 조회합니다
     * 
     * @param courseId 강의 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentResponseDto> getEnrollmentsByCourse(Long courseId, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId, pageable);
        return enrollments.map(EnrollmentResponseDto::from);
    }
    
    /**
     * 수강신청 상태별로 조회합니다
     * 
     * @param status 수강신청 상태
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentResponseDto> getEnrollmentsByStatus(EnrollmentStatus status, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findByStatus(status, pageable);
        return enrollments.map(EnrollmentResponseDto::from);
    }
    
    /**
     * 수강신청을 취소합니다
     * 
     * @param enrollmentId 수강신청 ID
     * @param reason 취소 사유
     * @return 취소된 수강신청 정보
     * @throws RuntimeException 수강신청을 찾을 수 없거나 취소할 수 없는 경우
     */
    @Transactional
    public EnrollmentResponseDto cancelEnrollment(Long enrollmentId, String reason) {
        // 1. 수강신청 조회
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
        
        // 2. 취소 가능 여부 확인
        if (!enrollment.isActive()) {
            throw new RuntimeException("이미 취소되었거나 거절된 수강신청입니다");
        }
        
        // 3. 수강신청 취소 처리
        enrollment.cancel(reason);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        // 4. 강의의 현재 수강생 수 감소
        Course course = enrollment.getCourse();
        if (course.getCurrentStudents() > 0) {
            course.decreaseCurrentStudents();
            courseRepository.save(course);
        }
        
        // 5. Redis 동기화 (단순 increment)
        try {
            String courseKey = "course:" + course.getId();
            stringRedisTemplate.opsForHash().increment(courseKey, "currentStudents", -1);
            log.debug("Redis 수강생 수 감소 완료: {}", course.getId());
        } catch (Exception e) {
            log.warn("Redis 수강생 수 감소 실패: {}", e.getMessage());
        }
        
        log.info("수강신청이 취소되었습니다 - EnrollmentId: {}, StudentId: {}, CourseId: {}, Reason: {}", 
                enrollmentId, enrollment.getStudent().getId(), course.getId(), reason);
        
        return EnrollmentResponseDto.from(savedEnrollment);
    }
    
    /**
     * 수강신청을 승인합니다 (관리자/강사용)
     * 
     * @param enrollmentId 수강신청 ID
     * @return 승인된 수강신청 정보
     * @throws RuntimeException 수강신청을 찾을 수 없거나 승인할 수 없는 경우
     */
    public EnrollmentResponseDto approveEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
        
        if (enrollment.getStatus() != EnrollmentStatus.APPLIED) {
            throw new RuntimeException("신청 상태의 수강신청만 승인할 수 있습니다");
        }
        
        Course course = enrollment.getCourse();
        
        // 정원 확인
        if (course.getCurrentStudents() >= course.getMaxStudents()) {
            throw new RuntimeException("강의 정원이 초과되었습니다");
        }
        
        enrollment.approve();
        course.increaseCurrentStudents();
        
        courseRepository.save(course);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        return EnrollmentResponseDto.from(savedEnrollment);
    }
    
    /**
     * 수강신청을 거절합니다 (관리자/강사용)
     * 
     * @param enrollmentId 수강신청 ID
     * @param reason 거절 사유
     * @return 거절된 수강신청 정보
     * @throws RuntimeException 수강신청을 찾을 수 없거나 거절할 수 없는 경우
     */
    public EnrollmentResponseDto rejectEnrollment(Long enrollmentId, String reason) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
        
        if (enrollment.getStatus() != EnrollmentStatus.APPLIED) {
            throw new RuntimeException("신청 상태의 수강신청만 거절할 수 있습니다");
        }
        
        enrollment.reject(reason);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        return EnrollmentResponseDto.from(savedEnrollment);
    }
    
    /**
     * 수강신청 가능 여부를 검증합니다 (정원 제외)
     * 
     * @param member 수강신청하는 회원 (학생 또는 강사)
     * @param course 강의
     * @throws RuntimeException 수강신청할 수 없는 경우
     */
    private void validateEnrollmentBasic(Member member, Course course) {
        // 1. 중복 수강신청 확인
        if (enrollmentRepository.existsByStudentAndCourse(member, course)) {
            throw new RuntimeException("이미 수강신청한 강의입니다");
        }
        
        // 2. 강의 상태 확인
        if (course.getStatus() != CourseStatus.SCHEDULED) {
            throw new RuntimeException("수강신청할 수 없는 강의 상태입니다: " + course.getStatus().getDescription());
        }
        
        // 3. 강의 시작일 확인
        if (course.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("이미 시작된 강의는 수강신청할 수 없습니다");
        }
        
        // 4. 자기 자신이 강사인 강의 확인 (강사도 다른 강사의 강의는 수강 가능)
        if (course.getInstructor().getId().equals(member.getId())) {
            throw new RuntimeException("자신이 강사인 강의는 수강신청할 수 없습니다");
        }
        
        // 5. 정원 확인은 DB 원자적 업데이트에서 처리
    }
    
    /**
     * 여러 강의에 동시 수강신청을 처리합니다 (Redis Queue 방식으로 H2 락 문제 해결)
     * 일부 강의가 실패해도 나머지 강의는 계속 처리됩니다
     * 
     * @param bulkRequestDto 여러 강의 수강신청 요청 정보
     * @return 수강신청 결과 (성공/실패 목록 포함)
     */
    public BulkEnrollmentResponseDto enrollMultipleCourses(BulkEnrollmentRequestDto bulkRequestDto) {
        // 빈 강의 목록 검증
        if (bulkRequestDto.getCourseIds() == null || bulkRequestDto.getCourseIds().isEmpty()) {
            throw new RuntimeException("수강신청할 강의 목록이 비어있습니다");
        }
        
        
        List<EnrollmentResponseDto> successfulEnrollments = new ArrayList<>();
        List<BulkEnrollmentResponseDto.EnrollmentFailureDto> failedEnrollments = new ArrayList<>();
        
        // 각 강의에 대해 동기 방식으로 수강신청 처리
        for (Long courseId : bulkRequestDto.getCourseIds()) {
            try {
                // 동기 방식으로 수강신청 처리
                EnrollmentRequestDto singleRequest = new EnrollmentRequestDto(
                    bulkRequestDto.getStudentId(), courseId);
                EnrollmentResponseDto result = enrollCourse(singleRequest);
                successfulEnrollments.add(result);
                
                log.info("벌크 수강신청 성공 - StudentId: {}, CourseId: {}", 
                        bulkRequestDto.getStudentId(), courseId);
                
            } catch (RuntimeException e) {
                // 수강신청 실패한 경우 실패 목록에 추가
                try {
                    Course course = courseRepository.findById(courseId).orElse(null);
                    String courseName = course != null ? course.getCourseName() : "알 수 없는 강의";
                    
                    BulkEnrollmentResponseDto.EnrollmentFailureDto failure = 
                        new BulkEnrollmentResponseDto.EnrollmentFailureDto(
                            courseId, courseName, e.getMessage());
                    failedEnrollments.add(failure);
                    
                    log.info("벌크 수강신청 실패 - StudentId: {}, CourseId: {}, Reason: {}", 
                            bulkRequestDto.getStudentId(), courseId, e.getMessage());
                } catch (Exception ex) {
                    BulkEnrollmentResponseDto.EnrollmentFailureDto failure = 
                        new BulkEnrollmentResponseDto.EnrollmentFailureDto(
                            courseId, "알 수 없는 강의", e.getMessage());
                    failedEnrollments.add(failure);
                }
            }
        }
        
        // 결과 DTO 생성
        return new BulkEnrollmentResponseDto(
            bulkRequestDto.getStudentId(),
            bulkRequestDto.getCourseIds().size(),
            successfulEnrollments.size(),
            failedEnrollments.size(),
            successfulEnrollments,
            failedEnrollments
        );
    }
    
    
    /**
     * Redis Queue를 사용하여 수강신청 취소를 처리합니다
     * 
     * @param enrollmentId 수강신청 ID
     * @param reason 취소 사유
     * @return 큐 ID
     */
    public String cancelEnrollmentWithQueue(Long enrollmentId, String reason) {
        // 1. 수강신청 정보 조회
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
        
        // 2. Redis Queue를 통한 원자적 취소 처리
        String queueId = enrollmentQueueService.enqueueCancelRequest(
            enrollment.getStudent().getId(), 
            enrollment.getCourse().getId(),
            reason
        );
        
        log.info("수강신청 취소가 큐에 등록되었습니다 - QueueId: {}, EnrollmentId: {}, StudentId: {}, CourseId: {}", 
                queueId, enrollmentId, enrollment.getStudent().getId(), enrollment.getCourse().getId());
        
        return queueId;
    }
    
    /**
     * 취소 큐 결과를 처리하여 DB에 저장합니다
     * 
     * @param result 큐 결과
     * @param enrollmentId 수강신청 ID
     */
    @Transactional
    public void processCancelQueueResult(Map<String, Object> result, Long enrollmentId) {
        try {
            Boolean success = Boolean.valueOf(result.get("success").toString());
            String message = (String) result.get("message");
            String reason = (String) result.get("reason");
            
            if (!success) {
                String koreanMessage = redisConcurrencyService.convertRedisMessageToKorean(message);
                throw new RuntimeException(koreanMessage);
            }
            
            // Redis에서 성공한 경우에만 DB에 실제 취소 처리
            Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                    .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
            
            enrollment.cancel(reason);
            enrollmentRepository.save(enrollment);
            
            // 강의의 현재 수강생 수 감소 (Redis에서 이미 처리됨)
            Course course = enrollment.getCourse();
            if (course.getCurrentStudents() > 0) {
                course.decreaseCurrentStudents();
                courseRepository.save(course);
            }
            
            log.info("수강신청 취소 DB 저장 완료 - EnrollmentId: {}, StudentId: {}, CourseId: {}", 
                    enrollmentId, enrollment.getStudent().getId(), course.getId());
            
        } catch (Exception e) {
            log.error("취소 큐 결과 처리 실패 - EnrollmentId: {}, Error: {}", enrollmentId, e.getMessage(), e);
            throw new RuntimeException("취소 큐 결과 처리 실패: " + e.getMessage(), e);
        }
    }
    
}
