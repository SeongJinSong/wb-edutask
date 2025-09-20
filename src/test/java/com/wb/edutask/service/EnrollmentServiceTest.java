package com.wb.edutask.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
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
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import jakarta.persistence.EntityManager;

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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(OrderAnnotation.class)
class EnrollmentServiceTest {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private EntityManager entityManager;
    
    private Member student;
    private Member instructor;
    private Course course;
    
    @BeforeEach
    void setUp() {
        // Redis 초기화 (테스트 간 격리를 위해)
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // 고유한 데이터 생성을 위한 타임스탬프 사용
        long timestamp = System.currentTimeMillis();
        String uniqueId = String.format("%05d", timestamp % 100000); // 5자리 숫자 (앞에 0 패딩)
        
        // 학생 생성
        student = new Member(
            "김학생" + uniqueId, 
            "student" + uniqueId + "@test.com", 
            "010-" + uniqueId.substring(0, 4) + "-" + uniqueId.substring(1, 5), 
            "Pass123", 
            MemberType.STUDENT
        );
        student = memberRepository.save(student);
        
        // 강사 생성
        instructor = new Member(
            "이강사" + uniqueId, 
            "instructor" + uniqueId + "@test.com", 
            "010-" + uniqueId.substring(1, 5) + "-" + uniqueId.substring(0, 4), 
            "Pass456", 
            MemberType.INSTRUCTOR
        );
        instructor = memberRepository.save(instructor);
        
        // 강의 생성
        course = Course.builder()
                .courseName("Java 프로그래밍" + uniqueId)
                .description("Java 기초부터 심화까지")
                .instructor(instructor)
                .maxStudents(10)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(37))
                .build();
        course = courseRepository.save(course);
    }
    
    @AfterEach
    void tearDown() {
        // Redis 데이터 정리
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
            }
        } catch (Exception e) {
            // Redis 연결 실패 시 무시 (테스트 환경에서 Redis가 없을 수 있음)
        }
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
        
        // 실제 DB에서 수강신청 수 확인
        long actualEnrollments = enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
        assertThat(actualEnrollments).isEqualTo(1);
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
        Course otherCourse = Course.builder()
                .courseName("Python 프로그래밍")
                .description("Python 기초부터 심화까지")
                .instructor(otherInstructor)
                .maxStudents(15)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .build();
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
    @DisplayName("진행 중인 강의 수강신청 성공 테스트")
    void enrollCourse_CourseInProgress() {
        // Given
        course.setStatus(CourseStatus.IN_PROGRESS);
        courseRepository.save(course);
        
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        
        // When
        EnrollmentResponseDto responseDto = enrollmentService.enrollCourse(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getStudent().getId()).isEqualTo(student.getId());
        assertThat(responseDto.getCourse().getId()).isEqualTo(course.getId());
        assertThat(responseDto.getStatus()).isEqualTo(EnrollmentStatus.APPROVED);
    }
    
    @Test
    @DisplayName("종료된 강의 수강신청 실패 테스트")
    void enrollCourse_CourseCompleted() {
        // Given
        course.setStatus(CourseStatus.COMPLETED);
        courseRepository.save(course);
        
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        
        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("수강신청할 수 없는 강의 상태입니다");
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
        
        // 실제 DB에서 활성 수강신청 수 확인 (취소되어 0이어야 함)
        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
        assertThat(activeEnrollments).isEqualTo(0);
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
        // 정원을 0으로 설정하여 자동 승인되지 않게 함
        course.setMaxStudents(0);
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
        
        // 실제 DB에서 승인된 수강신청 수 확인
        long approvedEnrollments = enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
        assertThat(approvedEnrollments).isEqualTo(1);
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
    
    @Test
    @DisplayName("여러 강의 동시 수강신청 성공 테스트")
    void enrollMultipleCourses_AllSuccess() {
        // Given
        // 추가 강의 생성
        Course course2 = Course.builder()
                .courseName("Python 프로그래밍")
                .description("Python 기초부터 심화까지")
                .instructor(instructor)
                .maxStudents(20)
                .startDate(LocalDate.now().plusDays(15))
                .endDate(LocalDate.now().plusDays(45))
                .build();
        course2 = courseRepository.save(course2);
        
        Course course3 = Course.builder()
                .courseName("JavaScript 프로그래밍")
                .description("JavaScript 기초부터 심화까지")
                .instructor(instructor)
                .maxStudents(15)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(50))
                .build();
        course3 = courseRepository.save(course3);
        
        List<Long> courseIds = Arrays.asList(course.getId(), course2.getId(), course3.getId());
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(student.getId(), courseIds);
        
        // When
        BulkEnrollmentResponseDto responseDto = enrollmentService.enrollMultipleCourses(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getStudentId()).isEqualTo(student.getId());
        assertThat(responseDto.getTotalRequested()).isEqualTo(3);
        assertThat(responseDto.getSuccessCount()).isEqualTo(3);
        assertThat(responseDto.getFailureCount()).isEqualTo(0);
        assertThat(responseDto.getSuccessfulEnrollments()).hasSize(3);
        assertThat(responseDto.getFailedEnrollments()).isEmpty();
    }
    
    @Test
    @DisplayName("여러 강의 동시 수강신청 부분 실패 테스트")
    void enrollMultipleCourses_PartialFailure() {
        // Given
        // 정원이 1명인 강의 생성하고 이미 가득 채우기 (실패할 강의)
        Course fullCourse = Course.builder()
                .courseName("정원 초과 강의")
                .description("정원이 가득 찬 강의")
                .instructor(instructor)
                .maxStudents(1) // 정원 1명
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .build();
        fullCourse = courseRepository.save(fullCourse); // Course를 먼저 저장
        // 정원을 가득 채우기 위해 더미 학생과 수강신청 생성
        Member dummyStudent = Member.builder()
                .name("더미학생")
                .email("dummy@test.com")
                .password("Pass123!")
                .phoneNumber("010-9999-9999")
                .memberType(MemberType.STUDENT)
                .build();
        dummyStudent = memberRepository.save(dummyStudent);
        Enrollment dummyEnrollment1 = new Enrollment(dummyStudent, fullCourse);
        dummyEnrollment1.approve();
        enrollmentRepository.save(dummyEnrollment1);
        
        // 성공할 강의
        Course availableCourse = Course.builder()
                .courseName("수강 가능 강의")
                .description("수강 가능한 강의")
                .instructor(instructor)
                .maxStudents(20)
                .startDate(LocalDate.now().plusDays(15))
                .endDate(LocalDate.now().plusDays(45))
                .build();
        availableCourse = courseRepository.save(availableCourse);
        
        List<Long> courseIds = Arrays.asList(fullCourse.getId(), availableCourse.getId());
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(student.getId(), courseIds);
        
        // When
        BulkEnrollmentResponseDto responseDto = enrollmentService.enrollMultipleCourses(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getStudentId()).isEqualTo(student.getId());
        assertThat(responseDto.getTotalRequested()).isEqualTo(2);
        assertThat(responseDto.getSuccessCount()).isEqualTo(1);
        assertThat(responseDto.getFailureCount()).isEqualTo(1);
        assertThat(responseDto.getSuccessfulEnrollments()).hasSize(1);
        assertThat(responseDto.getFailedEnrollments()).hasSize(1);
        
        // 실패한 수강신청 정보 확인
        BulkEnrollmentResponseDto.EnrollmentFailureDto failure = responseDto.getFailedEnrollments().get(0);
        assertThat(failure.getCourseId()).isEqualTo(fullCourse.getId());
        assertThat(failure.getCourseName()).isEqualTo("정원 초과 강의");
        assertThat(failure.getReason()).contains("강의 정원이 초과되었습니다");
    }
    
    @Test
    @DisplayName("여러 강의 동시 수강신청 모두 실패 테스트")
    void enrollMultipleCourses_AllFailure() {
        // Given
        // 종료된 강의 (실패할 강의)
        Course completedCourse = Course.builder()
                .courseName("종료된 강의")
                .description("종료된 강의")
                .instructor(instructor)
                .maxStudents(20)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1))
                .build();
        completedCourse.setStatus(CourseStatus.COMPLETED); // 종료 상태로 변경
        completedCourse = courseRepository.save(completedCourse);
        
        // 정원 초과 강의 (실패할 강의)
        Course fullCourse = Course.builder()
                .courseName("정원 초과 강의")
                .description("정원이 가득 찬 강의")
                .instructor(instructor)
                .maxStudents(1) // 정원 1명
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(40))
                .build();
        fullCourse = courseRepository.save(fullCourse); // Course를 먼저 저장
        // 정원을 가득 채우기 위해 더미 학생과 수강신청 생성
        Member dummyStudent2 = Member.builder()
                .name("더미학생2")
                .email("dummy2@test.com")
                .password("Pass123!")
                .phoneNumber("010-9999-9998")
                .memberType(MemberType.STUDENT)
                .build();
        dummyStudent2 = memberRepository.save(dummyStudent2);
        Enrollment dummyEnrollment2 = new Enrollment(dummyStudent2, fullCourse);
        dummyEnrollment2.approve();
        enrollmentRepository.save(dummyEnrollment2);
        
        List<Long> courseIds = Arrays.asList(completedCourse.getId(), fullCourse.getId());
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(student.getId(), courseIds);
        
        // When
        BulkEnrollmentResponseDto responseDto = enrollmentService.enrollMultipleCourses(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getStudentId()).isEqualTo(student.getId());
        assertThat(responseDto.getTotalRequested()).isEqualTo(2);
        assertThat(responseDto.getSuccessCount()).isEqualTo(0);
        assertThat(responseDto.getFailureCount()).isEqualTo(2);
        assertThat(responseDto.getSuccessfulEnrollments()).isEmpty();
        assertThat(responseDto.getFailedEnrollments()).hasSize(2);
    }
    
    @Test
    @DisplayName("존재하지 않는 학생 ID로 여러 강의 수강신청 실패 테스트")
    void enrollMultipleCourses_StudentNotFound() {
        // Given
        List<Long> courseIds = Arrays.asList(course.getId());
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(999L, courseIds);
        
        // When
        BulkEnrollmentResponseDto result = enrollmentService.enrollMultipleCourses(requestDto);
        
        // Then
        assertThat(result.getSuccessfulEnrollments()).isEmpty();
        assertThat(result.getFailedEnrollments()).hasSize(1);
        assertThat(result.getFailedEnrollments().get(0).getReason())
            .contains("회원을 찾을 수 없습니다");
        assertThat(result.getSuccessCount()).isEqualTo(0);
        assertThat(result.getFailureCount()).isEqualTo(1);
    }
}
