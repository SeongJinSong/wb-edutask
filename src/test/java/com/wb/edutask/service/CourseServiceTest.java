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
import com.wb.edutask.dto.CourseRequestDto;
import com.wb.edutask.dto.CourseResponseDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.CourseStatus;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 강의 서비스 테스트
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseServiceTest {
    
    @Autowired
    private CourseService courseService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    private Member instructor;
    private Member student;
    
    @BeforeEach
    void setUp() {
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
    void createCourse_InstructorSuccess() {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "Java 프로그래밍",
            "Java 기초부터 심화까지",
            instructor.getId(),
            20,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        
        // When
        CourseResponseDto responseDto = courseService.createCourse(requestDto);
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getCourseName()).isEqualTo("Java 프로그래밍");
        assertThat(responseDto.getDescription()).isEqualTo("Java 기초부터 심화까지");
        assertThat(responseDto.getInstructor().getId()).isEqualTo(instructor.getId());
        assertThat(responseDto.getMaxStudents()).isEqualTo(20);
        assertThat(responseDto.getCurrentStudents()).isEqualTo(0);
        assertThat(responseDto.getStatus()).isEqualTo(CourseStatus.SCHEDULED);
    }
    
    @Test
    @DisplayName("수강생이 강의 생성 시도 시 실패 테스트")
    void createCourse_StudentFail() {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "Python 프로그래밍",
            "Python 기초 강의",
            student.getId(), // 수강생 ID 사용
            15,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        
        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("강사 권한이 없는 회원입니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 강사 ID로 강의 생성 실패 테스트")
    void createCourse_InstructorNotFound() {
        // Given
        CourseRequestDto requestDto = new CourseRequestDto(
            "JavaScript 프로그래밍",
            "JavaScript 기초 강의",
            999L, // 존재하지 않는 ID
            15,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        
        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(requestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("강사를 찾을 수 없습니다");
    }
    
    @Test
    @DisplayName("중복 강의명으로 강의 생성 실패 테스트")
    void createCourse_DuplicateCourseName() {
        // Given
        // 첫 번째 강의 생성
        CourseRequestDto firstRequest = new CourseRequestDto(
            "React 프로그래밍",
            "React 기초 강의",
            instructor.getId(),
            20,
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(30)
        );
        courseService.createCourse(firstRequest);
        
        // 동일한 강의명으로 두 번째 강의 생성 시도
        CourseRequestDto secondRequest = new CourseRequestDto(
            "React 프로그래밍", // 동일한 강의명
            "React 심화 강의",
            instructor.getId(),
            15,
            LocalDate.now().plusDays(14),
            LocalDate.now().plusDays(45)
        );
        
        // When & Then
        assertThatThrownBy(() -> courseService.createCourse(secondRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이미 존재하는 강의명입니다");
    }
    
    @Test
    @DisplayName("강의 ID로 조회 성공 테스트")
    void getCourseById_Success() {
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
        
        // When
        CourseResponseDto responseDto = courseService.getCourseById(course.getId());
        
        // Then
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(course.getId());
        assertThat(responseDto.getCourseName()).isEqualTo("Vue.js 프로그래밍");
        assertThat(responseDto.getInstructor().getName()).isEqualTo(instructor.getName());
    }
    
    @Test
    @DisplayName("존재하지 않는 강의 ID로 조회 실패 테스트")
    void getCourseById_NotFound() {
        // When & Then
        assertThatThrownBy(() -> courseService.getCourseById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("강의를 찾을 수 없습니다");
    }
}
