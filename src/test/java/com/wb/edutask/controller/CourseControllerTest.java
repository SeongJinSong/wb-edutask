package com.wb.edutask.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.edutask.dto.CourseRequestDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 강의 컨트롤러 테스트
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(OrderAnnotation.class)
class CourseControllerTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    private MockMvc mockMvc;
    private Member instructor;
    private Member student;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 강사 회원 생성
        instructor = new Member(
            "김강사", 
            "instructor@test.com", 
            "010-1234-5678", 
            "Pass123", 
            MemberType.INSTRUCTOR
        );
        instructor = memberRepository.save(instructor);
        
        // 수강생 회원 생성
        student = new Member(
            "홍학생", 
            "student@test.com", 
            "010-9876-5432", 
            "Pass456", 
            MemberType.STUDENT
        );
        student = memberRepository.save(student);
    }
    
    @Test
    @DisplayName("강사가 강의 생성 성공 테스트")
    void createCourse_InstructorSuccess() throws Exception {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "Java 프로그래밍",
            "Java 기초부터 심화까지",
            instructor.getId(),
            20,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseName").value("Java 프로그래밍"))
                .andExpect(jsonPath("$.description").value("Java 기초부터 심화까지"))
                .andExpect(jsonPath("$.instructor.id").value(instructor.getId()))
                .andExpect(jsonPath("$.maxStudents").value(20))
                .andExpect(jsonPath("$.currentStudents").value(0))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }
    
    @Test
    @DisplayName("수강생이 강의 생성 시도 시 실패 테스트")
    void createCourse_StudentFail() throws Exception {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "Python 프로그래밍",
            "Python 기초 강의",
            student.getId(), // 수강생 ID 사용
            15,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("강사 권한이 없는 회원입니다: " + student.getName()));
    }
    
    @Test
    @DisplayName("존재하지 않는 강사 ID로 강의 생성 실패 테스트")
    void createCourse_InstructorNotFound() throws Exception {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "JavaScript 프로그래밍",
            "JavaScript 기초 강의",
            999L, // 존재하지 않는 ID
            15,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("잘못된 요청 데이터로 강의 생성 실패 테스트")
    void createCourse_InvalidRequest() throws Exception {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "", // 빈 강의명
            "설명",
            instructor.getId(),
            15,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        String requestJson = objectMapper.writeValueAsString(requestDto);
        
        // When & Then
        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("강의 ID로 조회 성공 테스트")
    void getCourseById_Success() throws Exception {
        // Given
        Course course = new Course(
            "Vue.js 프로그래밍",
            "Vue.js 기초부터 실무까지",
            instructor,
            25,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(40)
        );
        course = courseRepository.save(course);
        
        // When & Then
        mockMvc.perform(get("/api/v1/courses/{courseId}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(course.getId()))
                .andExpect(jsonPath("$.courseName").value("Vue.js 프로그래밍"))
                .andExpect(jsonPath("$.instructor.name").value(instructor.getName()));
    }
    
    @Test
    @DisplayName("존재하지 않는 강의 ID로 조회 실패 테스트")
    void getCourseById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/courses/{courseId}", 999L))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("모든 강의 목록 조회 성공 테스트")
    @DirtiesContext
    void getAllCourses_Success() throws Exception {
        // Given
        // 기존 데이터 정리
        courseRepository.deleteAll();
        
        Course course1 = new Course(
            "React 프로그래밍",
            "React 기초 강의",
            instructor,
            20,
            LocalDate.now().plusDays(5),
            LocalDate.now().plusDays(25)
        );
        Course course2 = new Course(
            "Angular 프로그래밍",
            "Angular 기초 강의",
            instructor,
            15,
            LocalDate.now().plusDays(10),
            LocalDate.now().plusDays(35)
        );
        courseRepository.save(course1);
        courseRepository.save(course2);
        
        // When & Then
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }
    
    @Test
    @DisplayName("수강 가능한 강의 목록 조회 성공 테스트")
    @DirtiesContext
    void getAvailableCourses_Success() throws Exception {
        // Given
        // 기존 데이터 정리
        courseRepository.deleteAll();
        
        Course availableCourse = new Course(
            "Node.js 프로그래밍",
            "Node.js 기초 강의",
            instructor,
            20,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        courseRepository.save(availableCourse);
        
        // When & Then
        mockMvc.perform(get("/api/v1/courses/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }
}
