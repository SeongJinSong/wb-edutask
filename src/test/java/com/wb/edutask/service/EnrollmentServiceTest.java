package com.wb.edutask.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
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
 * EnrollmentService 테스트 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentServiceTest {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    private Member student;
    private Member instructor;
    private Course course;
    
    @BeforeEach
    void setUp() {
        // 학생 생성
        student = new Member(
            "김학생", 
            "student@test.com", 
            "010-1234-5678", 
            "Pass123", 
            MemberType.STUDENT
        );
        student = memberRepository.save(student);
        
        // 강사 생성
        instructor = new Member(
            "이강사", 
            "instructor@test.com", 
            "010-9876-5432", 
            "Pass456", 
            MemberType.INSTRUCTOR
        );
        instructor = memberRepository.save(instructor);
        
        // 강의 생성
        course = new Course(
            "Java 프로그래밍", 
            "Java 기초부터 심화까지", 
            instructor, 
            10, 
            LocalDate.now().plusDays(7), 
            LocalDate.now().plusDays(37)
        );
        course = courseRepository.save(course);
    }
    
    @Test
    @DisplayName("수강신청 성공 테스트")
    void enrollCourse_Success() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        
        // When
        EnrollmentResponseDto responseDto = enrollmentService.enrollCourse(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getStudent().getId()).isEqualTo(student.getId());
        assertThat(responseDto.getCourse().getId()).isEqualTo(course.getId());
        assertThat(responseDto.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
        
        // 강의 수강 인원 증가 확인
        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updatedCourse.getCurrentStudents()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("존재하지 않는 학생 ID로 수강신청 실패 테스트")
    void enrollCourse_StudentNotFound() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(999L, course.getId());
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("회원을 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 강의 ID로 수강신청 실패 테스트")
    void enrollCourse_CourseNotFound() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), 999L);
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("강의를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("강사가 다른 강사의 강의에 수강신청 성공 테스트")
    void enrollCourse_InstructorCanEnrollOtherCourse() {
        // Given
        // 다른 강사 생성
        Member otherInstructor = new Member(
            "박강사", 
            "other.instructor@test.com", 
            "010-5555-6666", 
            "Pass789", 
            MemberType.INSTRUCTOR
        );
        otherInstructor = memberRepository.save(otherInstructor);
        
        // 다른 강사의 강의 생성
        Course otherCourse = new Course(
            "Python 프로그래밍", 
            "Python 기초부터 심화까지", 
            otherInstructor, 
            15, 
            LocalDate.now().plusDays(10), 
            LocalDate.now().plusDays(40)
        );
        otherCourse = courseRepository.save(otherCourse);
        
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(instructor.getId(), otherCourse.getId());
        
        // When
        EnrollmentResponseDto responseDto = enrollmentService.enrollCourse(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getStudent().getId()).isEqualTo(instructor.getId());
        assertThat(responseDto.getCourse().getId()).isEqualTo(otherCourse.getId());
        assertThat(responseDto.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
    }
    
    @Test
    @DisplayName("중복 수강신청 실패 테스트")
    void enrollCourse_DuplicateEnrollment() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        enrollmentService.enrollCourse(requestDto); // 첫 번째 수강신청
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이미 수강신청한 강의입니다");
    }
    
    @Test
    @DisplayName("강사가 자신의 강의에 수강신청 시도 시 실패 테스트")
    void enrollCourse_InstructorCannotEnrollOwnCourse() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(instructor.getId(), course.getId());
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("자신이 강사인 강의는 수강신청할 수 없습니다");
    }
    
    @Test
    @DisplayName("정원 초과 시 수강신청 실패 테스트")
    void enrollCourse_ExceedsCapacity() {
        // Given
        // 강의 정원을 1명으로 설정
        course.setMaxStudents(1);
        courseRepository.save(course);
        
        // 다른 학생 생성 및 수강신청
        Member otherStudent = new Member(
            "박학생", 
            "other@test.com", 
            "010-1111-2222", 
            "Pass789", 
            MemberType.STUDENT
        );
        otherStudent = memberRepository.save(otherStudent);
        
        EnrollmentRequestDto firstRequest = new EnrollmentRequestDto(otherStudent.getId(), course.getId());
        enrollmentService.enrollCourse(firstRequest); // 정원 채움
        
        EnrollmentRequestDto secondRequest = new EnrollmentRequestDto(student.getId(), course.getId());
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(secondRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("강의 정원이 초과되었습니다");
    }
    
    @Test
    @DisplayName("진행 중인 강의 수강신청 실패 테스트")
    void enrollCourse_CourseInProgress() {
        // Given
        course.setStatus(CourseStatus.IN_PROGRESS);
        courseRepository.save(course);
        
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("수강신청할 수 없는 강의 상태입니다");
    }
    
    @Test
    @DisplayName("이미 시작된 강의 수강신청 실패 테스트")
    void enrollCourse_CourseAlreadyStarted() {
        // Given
        course.setStartDate(LocalDate.now().minusDays(1)); // 어제 시작
        courseRepository.save(course);
        
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이미 시작된 강의는 수강신청할 수 없습니다");
    }
    
    @Test
    @DisplayName("수강신청 취소 성공 테스트")
    void cancelEnrollment_Success() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        EnrollmentResponseDto enrollment = enrollmentService.enrollCourse(requestDto);
        
        // When
        EnrollmentResponseDto cancelledEnrollment = enrollmentService.cancelEnrollment(
            enrollment.getId(), "개인 사정으로 인한 취소"
        );
        
        // Then
        assertThat(cancelledEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(cancelledEnrollment.getReason()).isEqualTo("개인 사정으로 인한 취소");
        
        // 강의 수강 인원 감소 확인
        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updatedCourse.getCurrentStudents()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("이미 취소된 수강신청 재취소 시도 시 실패 테스트")
    void cancelEnrollment_AlreadyCancelled() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        EnrollmentResponseDto enrollment = enrollmentService.enrollCourse(requestDto);
        enrollmentService.cancelEnrollment(enrollment.getId(), "첫 번째 취소");
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.cancelEnrollment(enrollment.getId(), "두 번째 취소"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이미 취소되었거나 거절된 수강신청입니다");
    }
    
    @Test
    @DisplayName("수강신청 승인 성공 테스트")
    void approveEnrollment_Success() {
        // Given
        // 정원을 초과하도록 설정하여 자동 승인되지 않게 함
        course.setMaxStudents(0);
        course.setCurrentStudents(0);
        courseRepository.save(course);
        
        // 수동으로 수강신청 생성 (자동 승인 방지)
        Enrollment enrollment = new Enrollment(student, course);
        enrollment = enrollmentRepository.save(enrollment);
        
        // 정원을 다시 늘림
        course.setMaxStudents(10);
        courseRepository.save(course);
        
        // When
        EnrollmentResponseDto approvedEnrollment = enrollmentService.approveEnrollment(enrollment.getId());
        
        // Then
        assertThat(approvedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
        assertThat(approvedEnrollment.getApprovedAt()).isNotNull();
        
        // 강의 수강 인원 증가 확인
        Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updatedCourse.getCurrentStudents()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("수강신청 거절 성공 테스트")
    void rejectEnrollment_Success() {
        // Given
        // 수동으로 수강신청 생성
        Enrollment enrollment = new Enrollment(student, course);
        enrollment = enrollmentRepository.save(enrollment);
        
        // When
        EnrollmentResponseDto rejectedEnrollment = enrollmentService.rejectEnrollment(
            enrollment.getId(), "정원 초과로 인한 거절"
        );
        
        // Then
        assertThat(rejectedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.REJECTED);
        assertThat(rejectedEnrollment.getReason()).isEqualTo("정원 초과로 인한 거절");
        assertThat(rejectedEnrollment.getCancelledAt()).isNotNull();
    }
    
    @Test
    @DisplayName("수강신청 ID로 조회 성공 테스트")
    void getEnrollmentById_Success() {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        EnrollmentResponseDto enrollment = enrollmentService.enrollCourse(requestDto);
        
        // When
        EnrollmentResponseDto foundEnrollment = enrollmentService.getEnrollmentById(enrollment.getId());
        
        // Then
        assertThat(foundEnrollment).isNotNull();
        assertThat(foundEnrollment.getId()).isEqualTo(enrollment.getId());
        assertThat(foundEnrollment.getStudent().getId()).isEqualTo(student.getId());
        assertThat(foundEnrollment.getCourse().getId()).isEqualTo(course.getId());
    }
    
    @Test
    @DisplayName("존재하지 않는 수강신청 ID로 조회 실패 테스트")
    void getEnrollmentById_NotFound() {
        // When & Then
        assertThatThrownBy(() -> enrollmentService.getEnrollmentById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("수강신청을 찾을 수 없습니다");
    }
}
