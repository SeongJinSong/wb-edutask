package com.wb.edutask.service;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentResponseDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Enrollment;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.CourseStatus;
import com.wb.edutask.enums.EnrollmentStatus;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;

/**
 * 수강신청 관리를 위한 서비스 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@Transactional
public class EnrollmentService {
    
    private final EnrollmentRepository enrollmentRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;
    
    /**
     * EnrollmentService 생성자
     * 
     * @param enrollmentRepository 수강신청 Repository
     * @param memberRepository 회원 Repository
     * @param courseRepository 강의 Repository
     */
    @Autowired
    public EnrollmentService(EnrollmentRepository enrollmentRepository, 
                           MemberRepository memberRepository,
                           CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.memberRepository = memberRepository;
        this.courseRepository = courseRepository;
    }
    
    /**
     * 수강신청을 처리합니다
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return 생성된 수강신청 정보
     * @throws RuntimeException 유효하지 않은 요청인 경우
     */
    public EnrollmentResponseDto enrollCourse(EnrollmentRequestDto enrollmentRequestDto) {
        // 1. 학생 정보 조회 및 검증
        Member student = memberRepository.findById(enrollmentRequestDto.getStudentId())
                .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다: " + enrollmentRequestDto.getStudentId()));
        
        if (student.getMemberType() != MemberType.STUDENT) {
            throw new RuntimeException("학생만 수강신청할 수 있습니다");
        }
        
        // 2. 강의 정보 조회 및 검증
        Course course = courseRepository.findById(enrollmentRequestDto.getCourseId())
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + enrollmentRequestDto.getCourseId()));
        
        // 3. 수강신청 가능 여부 검증
        validateEnrollment(student, course);
        
        // 4. 수강신청 생성
        Enrollment enrollment = new Enrollment(student, course);
        
        // 5. 자동 승인 처리 (정원 내인 경우)
        if (course.getCurrentStudents() < course.getMaxStudents()) {
            enrollment.approve();
            course.increaseCurrentStudents();
            courseRepository.save(course);
        }
        
        // 6. 수강신청 저장
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
        return EnrollmentResponseDto.from(savedEnrollment);
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
    public EnrollmentResponseDto cancelEnrollment(Long enrollmentId, String reason) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("수강신청을 찾을 수 없습니다: " + enrollmentId));
        
        if (!enrollment.isActive()) {
            throw new RuntimeException("이미 취소되었거나 거절된 수강신청입니다");
        }
        
        // 승인된 수강신청인 경우 강의 수강 인원 감소
        if (enrollment.isApproved()) {
            Course course = enrollment.getCourse();
            course.decreaseCurrentStudents();
            courseRepository.save(course);
        }
        
        enrollment.cancel(reason);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        
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
     * 수강신청 가능 여부를 검증합니다
     * 
     * @param student 학생
     * @param course 강의
     * @throws RuntimeException 수강신청할 수 없는 경우
     */
    private void validateEnrollment(Member student, Course course) {
        // 1. 중복 수강신청 확인
        if (enrollmentRepository.existsByStudentAndCourse(student, course)) {
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
        
        // 4. 자기 자신이 강사인 강의 확인
        if (course.getInstructor().getId().equals(student.getId())) {
            throw new RuntimeException("자신이 강사인 강의는 수강신청할 수 없습니다");
        }
        
        // 5. 정원 확인 (대기열 없이 바로 거절)
        if (course.getCurrentStudents() >= course.getMaxStudents()) {
            throw new RuntimeException("강의 정원이 초과되었습니다");
        }
    }
}
