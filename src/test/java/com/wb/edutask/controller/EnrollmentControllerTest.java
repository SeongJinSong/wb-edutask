package com.wb.edutask.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * EnrollmentController 테스트 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
class EnrollmentControllerTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    private Member student;
    private Member instructor;
    private Course course;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
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
    void enrollCourse_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/enrollments")
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
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/enrollments")
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
        mockMvc.perform(post("/enrollments")
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
        mockMvc.perform(post("/enrollments")
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
        MvcResult enrollResult = mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String responseJson = enrollResult.getResponse().getContentAsString();
        Long enrollmentId = objectMapper.readTree(responseJson).get("id").asLong();
        
        // When & Then
        mockMvc.perform(get("/enrollments/{enrollmentId}", enrollmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(enrollmentId))
                .andExpect(jsonPath("$.student.id").value(student.getId()))
                .andExpect(jsonPath("$.course.id").value(course.getId()));
    }
    
    @Test
    @DisplayName("존재하지 않는 수강신청 조회 실패 테스트")
    void getEnrollment_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/enrollments/{enrollmentId}", 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("학생별 수강신청 목록 조회 성공 테스트")
    void getEnrollmentsByStudent_Success() throws Exception {
        // Given
        EnrollmentRequestDto requestDto = new EnrollmentRequestDto(student.getId(), course.getId());
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // 수강신청 생성
        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
        
        // When & Then
        mockMvc.perform(get("/enrollments/student/{studentId}", student.getId()))
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
        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
        
        // When & Then
        mockMvc.perform(get("/enrollments/course/{courseId}", course.getId()))
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
        mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());
        
        // When & Then
        mockMvc.perform(get("/enrollments/status/APPROVED"))
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
        MvcResult enrollResult = mockMvc.perform(post("/enrollments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String responseJson = enrollResult.getResponse().getContentAsString();
        Long enrollmentId = objectMapper.readTree(responseJson).get("id").asLong();
        
        // When & Then
        mockMvc.perform(delete("/enrollments/{enrollmentId}", enrollmentId)
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
        mockMvc.perform(put("/enrollments/{enrollmentId}/approve", enrollment.getId()))
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
        mockMvc.perform(put("/enrollments/{enrollmentId}/reject", enrollment.getId())
                .param("reason", "정원 초과로 인한 거절"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("정원 초과로 인한 거절"));
    }
    
    @Test
    @DisplayName("수강신청 통계 조회 성공 테스트")
    void getEnrollmentStats_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/enrollments/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEnrollments").exists())
                .andExpect(jsonPath("$.approvedEnrollments").exists())
                .andExpect(jsonPath("$.pendingEnrollments").exists())
                .andExpect(jsonPath("$.cancelledEnrollments").exists());
    }
    
    @Test
    @DisplayName("잘못된 수강신청 ID로 승인 시도 시 실패 테스트")
    void approveEnrollment_InvalidId() throws Exception {
        // When & Then
        mockMvc.perform(put("/enrollments/{enrollmentId}/approve", -1L))
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
        mockMvc.perform(put("/enrollments/{enrollmentId}/reject", enrollment.getId()))
                .andExpect(status().isBadRequest());
    }
}
