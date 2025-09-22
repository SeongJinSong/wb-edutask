package com.wb.edutask.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private final RedisConcurrencyService redisConcurrencyService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CourseRankingService courseRankingService;
    
    
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
        
        // currentStudents 실시간 증가
        try {
            course.setCurrentStudents(course.getCurrentStudents() + 1);
            courseRepository.save(course);
            log.debug("currentStudents 실시간 증가 - CourseId: {}, 현재: {}", 
                    course.getId(), course.getCurrentStudents());
        } catch (Exception e) {
            log.warn("currentStudents 업데이트 실패 - CourseId: {}, Error: {}", course.getId(), e.getMessage());
        }
        
        // ZSet 랭킹 업데이트
        try {
            courseRankingService.updateCourseRanking(course.getId(), course.getCurrentStudents(), course.getMaxStudents());
            log.debug("ZSet 랭킹 업데이트 완료 - CourseId: {}", course.getId());
        } catch (Exception e) {
            log.warn("ZSet 랭킹 업데이트 실패 - CourseId: {}, Error: {}", course.getId(), e.getMessage());
        }
        
        log.info("수강신청 완료 - StudentId: {}, CourseId: {}", 
                enrollmentRequestDto.getStudentId(), enrollmentRequestDto.getCourseId());
        
        return EnrollmentResponseDto.from(savedEnrollment, course);
    }
    
    
    /**
     * 비동기 수강신청 처리 (멀티서버 환경 대응)
     * Lua 스크립트로 Redis 체크 후 성공시 즉시 DB 저장
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return CompletableFuture<EnrollmentResponseDto>
     */
    @Async("enrollmentTaskExecutor")
    public CompletableFuture<EnrollmentResponseDto> enrollCourseAsync(EnrollmentRequestDto enrollmentRequestDto) {
        try {
            // 1. 기본 검증 (회원 및 강의 존재 확인)
            Member member = memberRepository.findById(enrollmentRequestDto.getStudentId())
                    .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다: " + enrollmentRequestDto.getStudentId()));
            
            Course course = courseRepository.findById(enrollmentRequestDto.getCourseId())
                    .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + enrollmentRequestDto.getCourseId()));
            
            validateEnrollmentBasic(member, course);
            
            // 2. Lua 스크립트로 Redis 동시성 체크
            Map<String, Object> luaResult = redisConcurrencyService.executeEnrollmentLuaScript(
                enrollmentRequestDto.getStudentId(), 
                enrollmentRequestDto.getCourseId()
            );
            
            Boolean success = (Boolean) luaResult.get("success");
            String message = (String) luaResult.get("message");
            
            if (!success) {
                String koreanMessage = redisConcurrencyService.convertRedisMessageToKorean(message);
                throw new RuntimeException(koreanMessage);
            }
            
            // 3. Lua 스크립트 성공시 즉시 DB 저장
            Enrollment enrollment = Enrollment.builder()
                    .student(member)
                    .course(course)
                    .status(EnrollmentStatus.APPROVED)
                    .build();
            
            Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
            
            
            log.info("비동기 수강신청 처리 완료 - StudentId: {}, CourseId: {}, EnrollmentId: {}", 
                    enrollmentRequestDto.getStudentId(), enrollmentRequestDto.getCourseId(), savedEnrollment.getId());
            
            // 5. 응답 DTO 생성 (기존 from 메서드 사용)
            EnrollmentResponseDto responseDto = EnrollmentResponseDto.from(savedEnrollment, course);
            
            return CompletableFuture.completedFuture(responseDto);
            
        } catch (Exception e) {
            log.error("비동기 수강신청 처리 실패 - StudentId: {}, CourseId: {}, Error: {}", 
                    enrollmentRequestDto.getStudentId(), enrollmentRequestDto.getCourseId(), e.getMessage(), e);
            
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * 비동기 수강신청 취소 처리
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @param reason 취소 사유
     * @return CompletableFuture<String>
     */
    @Async("enrollmentTaskExecutor")
    public CompletableFuture<String> cancelEnrollmentAsync(Long studentId, Long courseId, String reason) {
        try {
            // 수강신청 조회
            Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                    .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다"));
            
            // 기존 취소 로직 호출
            cancelEnrollment(enrollment.getId(), reason);
            
            log.info("비동기 수강신청 취소 완료 - StudentId: {}, CourseId: {}, EnrollmentId: {}", 
                    studentId, courseId, enrollment.getId());
            
            return CompletableFuture.completedFuture("수강신청 취소가 완료되었습니다.");
            
        } catch (Exception e) {
            log.error("비동기 수강신청 취소 실패 - StudentId: {}, CourseId: {}, Error: {}", 
                    studentId, courseId, e.getMessage(), e);
            
            return CompletableFuture.failedFuture(e);
        }
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
        
        // 5. Redis에서 수강생 수 처리 (동시성 제어를 위해)
        try {
            // Redis에 강의 정보가 있으면 감소, 없으면 DB 기준으로 동기화만
            String courseKey = "course:" + course.getId();
            String existingCurrentStudents = (String) stringRedisTemplate.opsForHash().get(courseKey, "currentStudents");
            
            if (existingCurrentStudents != null) {
                // Redis에 데이터가 있으면 감소만
                redisConcurrencyService.decreaseCourseStudents(course.getId());
                log.info("Redis 수강생 수 감소 완료 - CourseId: {}", course.getId());
            } else {
                // Redis에 데이터가 없으면 DB 기준으로 동기화만 (감소 없이)
                redisConcurrencyService.syncCourseToRedisIfNeeded(course.getId());
                log.info("Redis 강의 정보 동기화 완료 - CourseId: {}", course.getId());
            }
        } catch (Exception e) {
            log.warn("Redis 처리 실패 - CourseId: {}, Error: {}", course.getId(), e.getMessage());
            // Redis 실패는 치명적이지 않으므로 계속 진행
        }
        
        // currentStudents 실시간 감소
        try {
            int newCount = Math.max(0, course.getCurrentStudents() - 1);
            course.setCurrentStudents(newCount);
            courseRepository.save(course);
            log.debug("currentStudents 실시간 감소 - CourseId: {}, 현재: {}", 
                    course.getId(), newCount);
        } catch (Exception e) {
            log.warn("currentStudents 업데이트 실패 (취소) - CourseId: {}, Error: {}", course.getId(), e.getMessage());
        }
        
        // ZSet 랭킹 업데이트
        try {
            courseRankingService.updateCourseRanking(course.getId(), course.getCurrentStudents(), course.getMaxStudents());
            log.debug("ZSet 랭킹 업데이트 완료 (취소) - CourseId: {}", course.getId());
        } catch (Exception e) {
            log.warn("ZSet 랭킹 업데이트 실패 (취소) - CourseId: {}, Error: {}", course.getId(), e.getMessage());
        }
        
        log.info("수강신청이 취소되었습니다 - EnrollmentId: {}, StudentId: {}, CourseId: {}, Reason: {}", 
                enrollmentId, enrollment.getStudent().getId(), course.getId(), reason);
        
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
        
        // 2. 강의 상태 확인 (온라인 강의 특성상 진행 중인 강의도 수강신청 가능)
        if (course.getStatus() == CourseStatus.COMPLETED || course.getStatus() == CourseStatus.CANCELLED) {
            throw new RuntimeException("수강신청할 수 없는 강의 상태입니다: " + course.getStatus().getDescription());
        }
        
        // 3. 자기 자신이 강사인 강의 확인 (강사도 다른 강사의 강의는 수강 가능)
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
            
            // 강의 정보 (로그용)
            Course course = enrollment.getCourse();
            
            log.info("수강신청 취소 DB 저장 완료 - EnrollmentId: {}, StudentId: {}, CourseId: {}", 
                    enrollmentId, enrollment.getStudent().getId(), course.getId());
            
        } catch (Exception e) {
            log.error("취소 큐 결과 처리 실패 - EnrollmentId: {}, Error: {}", enrollmentId, e.getMessage(), e);
            throw new RuntimeException("취소 큐 결과 처리 실패: " + e.getMessage(), e);
        }
    }
    
}
