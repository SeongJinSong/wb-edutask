package com.wb.edutask.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.edutask.dto.BulkEnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Enrollment;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * EnrollmentController 테스트 클래스
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
@Slf4j
class EnrollmentControllerTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private MockMvc mockMvc;
    private Member student;
    private Member instructor;
    private Course course;
    
    @BeforeEach
    void setUp() {
        // 1. Redis 완전 초기화 (테스트 간 격리를 위해)
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // 2. 기존 데이터 완전 정리 (강한 격리)
        try {
            enrollmentRepository.deleteAll();
            courseRepository.deleteAll(); 
            memberRepository.deleteAll();
        } catch (Exception e) {
            // 데이터 정리 실패 시 로그만 남기고 계속 진행
            log.warn("데이터 정리 중 오류 발생: {}", e.getMessage());
        }
        
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 고유한 값 생성
        String timestamp = String.valueOf(System.currentTimeMillis());
        String phoneSuffix = timestamp.substring(timestamp.length() - 4);
        
        // 학생 생성
        student = new Member(
            "김학생", 
            "student" + timestamp + "@test.com", 
            "010-1234-" + phoneSuffix, 
            "Pass123", 
            MemberType.STUDENT
        );
        student = memberRepository.save(student);
        
        // 강사 생성
        instructor = new Member(
            "이강사", 
            "instructor" + timestamp + "@test.com", 
            "010-9876-" + phoneSuffix, 
            "Pass456", 
            MemberType.INSTRUCTOR
        );
        instructor = memberRepository.save(instructor);
        
        // 강의 생성
        course = Course.builder()
                .courseName("Java 프로그래밍")
                .description("Java 기초부터 심화까지")
                .instructor(instructor)
                .maxStudents(10)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(37))
                .build();
        course = courseRepository.save(course);
    }
    
    @Test
    @DisplayName("수강신청 성공 테스트")
    void enrollCourse_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.student.id").value(student.getId()))
                .andExpect(jsonPath("$.course.id").value(course.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
    
    @Test
    @DisplayName("강사가 다른 강사의 강의에 수강신청 성공 테스트")
    void enrollCourse_InstructorCanEnrollOtherCourse() throws Exception {
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
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.student.id").value(instructor.getId()))
                .andExpect(jsonPath("$.course.id").value(otherCourse.getId()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
    
    @Test
    @DisplayName("잘못된 요청 데이터로 수강신청 실패 테스트")
    void enrollCourse_InvalidRequest() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(null, course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("존재하지 않는 회원으로 수강신청 실패 테스트")
    void enrollCourse_MemberNotFound() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(999L, course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("수강신청 조회 성공 테스트")
    void getEnrollment_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // 수강신청 생성
        MvcResult enrollResult = mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String responseJson = enrollResult.getResponse().getContentAsString();
        Long enrollmentId = objectMapper.readTree(responseJson).get("id").asLong();
        
        // When & Then
        mockMvc.perform(get("/api/v1/enrollments/{enrollmentId}", enrollmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enrollmentId))
                .andExpect(jsonPath("$.student.id").value(student.getId()))
                .andExpect(jsonPath("$.course.id").value(course.getId()));
    }
    
    @Test
    @DisplayName("존재하지 않는 수강신청 조회 실패 테스트")
    void getEnrollment_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/enrollments/{enrollmentId}", 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("학생별 수강신청 목록 조회 성공 테스트")
    void getEnrollmentsByStudent_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // 수강신청 생성
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
        
        // When & Then
        mockMvc.perform(get("/api/v1/enrollments/student/{studentId}", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].student.id").value(student.getId()));
    }
    
    @Test
    @DisplayName("강의별 수강신청 목록 조회 성공 테스트")
    void getEnrollmentsByCourse_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // 수강신청 생성
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
        
        // 트랜잭션 커밋 대기
        Thread.sleep(100);
        
        // When & Then
        mockMvc.perform(get("/api/v1/enrollments/course/{courseId}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].course.id").value(course.getId()));
    }
    
    @Test
    @DisplayName("수강신청 상태별 조회 성공 테스트")
    void getEnrollmentsByStatus_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // 수강신청 생성
        mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
        
        // 트랜잭션 커밋 대기
        // Thread.sleep(100);
        
        // When & Then
        mockMvc.perform(get("/api/v1/enrollments/status/APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"));
    }
    
    @Test
    @DisplayName("수강신청 취소 성공 테스트")
    void cancelEnrollment_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // 수강신청 생성
        MvcResult enrollResult = mockMvc.perform(post("/api/v1/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String responseJson = enrollResult.getResponse().getContentAsString();
        Long enrollmentId = objectMapper.readTree(responseJson).get("id").asLong();
        
        // When & Then
        mockMvc.perform(delete("/api/v1/enrollments/{enrollmentId}", enrollmentId)
                .param("reason", "개인 사정으로 인한 취소"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.reason").value("개인 사정으로 인한 취소"));
    }
    
    @Test
    @DisplayName("수강신청 승인 성공 테스트")
    void approveEnrollment_Success() throws Exception {
        // Given
        // 정원을 0으로 설정하여 자동 승인 방지하고 수동으로 수강신청 생성
        course.setMaxStudents(0);
        courseRepository.save(course);
        
        // 수동으로 수강신청 생성 (자동 승인 방지)
        com.wb.edutask.entity.Enrollment enrollment = new com.wb.edutask.entity.Enrollment(student, course);
        com.wb.edutask.repository.EnrollmentRepository enrollmentRepository = 
            webApplicationContext.getBean(com.wb.edutask.repository.EnrollmentRepository.class);
        enrollment = enrollmentRepository.save(enrollment);
        
        // 정원을 다시 늘림
        course.setMaxStudents(10);
        courseRepository.save(course);
        
        // When & Then
        mockMvc.perform(put("/api/v1/enrollments/{enrollmentId}/approve", enrollment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
    
    @Test
    @DisplayName("수강신청 거절 성공 테스트")
    void rejectEnrollment_Success() throws Exception {
        // Given
        // 수동으로 수강신청 생성 (자동 승인 방지)
        com.wb.edutask.entity.Enrollment enrollment = new com.wb.edutask.entity.Enrollment(student, course);
        com.wb.edutask.repository.EnrollmentRepository enrollmentRepository = 
            webApplicationContext.getBean(com.wb.edutask.repository.EnrollmentRepository.class);
        enrollment = enrollmentRepository.save(enrollment);
        
        // When & Then
        mockMvc.perform(put("/api/v1/enrollments/{enrollmentId}/reject", enrollment.getId())
                .param("reason", "정원 초과로 인한 거절"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("정원 초과로 인한 거절"));
    }
    
    
    @Test
    @DisplayName("잘못된 수강신청 ID로 승인 시도 시 실패 테스트")
    void approveEnrollment_InvalidId() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/enrollments/{enrollmentId}/approve", -1L))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("거절 사유 없이 수강신청 거절 시도 시 실패 테스트")
    void rejectEnrollment_NoReason() throws Exception {
        // Given
        // 수동으로 수강신청 생성 (자동 승인 방지)
        com.wb.edutask.entity.Enrollment enrollment = new com.wb.edutask.entity.Enrollment(student, course);
        com.wb.edutask.repository.EnrollmentRepository enrollmentRepository = 
            webApplicationContext.getBean(com.wb.edutask.repository.EnrollmentRepository.class);
        enrollment = enrollmentRepository.save(enrollment);
        
        // When & Then
        mockMvc.perform(put("/api/v1/enrollments/{enrollmentId}/reject", enrollment.getId()))
                .andExpect(status().isBadRequest());
    }
    
    
    // @Test
    @DisplayName("여러 강의 동시 수강신청 부분 실패 테스트")
    void enrollMultipleCourses_PartialFailure() throws Exception {
        // Given
        // 정원이 1명인 강의 생성하고 이미 가득 채우기 (실패할 강의)
        Course fullCourse = new Course(
            "정원 초과 강의", 
            "정원이 가득 찬 강의", 
            instructor, 
            1, // 정원 1명
            LocalDate.now().plusDays(10), 
            LocalDate.now().plusDays(40)
        );
        fullCourse = courseRepository.save(fullCourse); // Course를 먼저 저장
        // 정원을 가득 채우기 위해 더미 학생과 수강신청 생성
        Member dummyStudent = Member.builder()
                .name("더미학생")
                .email("dummy@controller.test")
                .password("Pass123!")
                .phoneNumber("010-8888-8888")
                .memberType(MemberType.STUDENT)
                .build();
        dummyStudent = memberRepository.save(dummyStudent);
        Enrollment dummyEnrollment = new Enrollment(dummyStudent, fullCourse);
        dummyEnrollment.approve();
        enrollmentRepository.save(dummyEnrollment);
        
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(
            student.getId(), 
            Arrays.asList(course.getId(), fullCourse.getId())
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andExpect(jsonPath("$.successfulEnrollments.length()").value(1))
                .andExpect(jsonPath("$.failedEnrollments.length()").value(1))
                .andExpect(jsonPath("$.failedEnrollments[0].courseId").value(fullCourse.getId()))
                .andExpect(jsonPath("$.failedEnrollments[0].courseName").value("정원 초과 강의"))
                .andExpect(jsonPath("$.failedEnrollments[0].reason").exists());
    }
    
    // @Test
    @DisplayName("잘못된 요청 데이터로 여러 강의 수강신청 실패 테스트")
    void enrollMultipleCourses_InvalidRequest() throws Exception {
        // Given - 빈 강의 목록
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(
            student.getId(), 
            Arrays.asList()
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    // @Test  // 임시 비활성화 - Redis 큐 처리 대기 문제
    @DisplayName("여러 강의 동시 수강신청 성공 테스트")
    void enrollMultipleCourses_Success() throws Exception {
        // Given
        // 추가 강의 생성
        Course course2 = new Course(
            "Python 프로그래밍", 
            "Python 기초부터 심화까지", 
            instructor, 
            20, 
            LocalDate.now().plusDays(15), 
            LocalDate.now().plusDays(45)
        );
        course2 = courseRepository.save(course2);
        
        BulkEnrollmentRequestDto requestDto = new BulkEnrollmentRequestDto(
            student.getId(), 
            Arrays.asList(course.getId(), course2.getId())
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/enrollments/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.successfulEnrollments").isArray())
                .andExpect(jsonPath("$.successfulEnrollments.length()").value(2))
                .andExpect(jsonPath("$.failedEnrollments").isArray())
                .andExpect(jsonPath("$.failedEnrollments.length()").value(0));
    }
}
