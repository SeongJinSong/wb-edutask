package com.wb.edutask.service;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.dto.CourseRequestDto;
import com.wb.edutask.dto.CourseResponseDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.CourseStatus;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

/**
 * 강의 관리를 위한 서비스 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final MemberRepository memberRepository;
    private final EnrollmentRepository enrollmentRepository;
    
    
    /**
     * 새로운 강의를 생성합니다
     * 
     * @param courseRequestDto 강의 생성 요청 정보
     * @return 생성된 강의 정보
     * @throws IllegalArgumentException 유효하지 않은 요청인 경우
     * @throws RuntimeException 강사를 찾을 수 없거나 강의명이 중복인 경우
     */
    public CourseResponseDto createCourse(CourseRequestDto courseRequestDto) {
        // 1. 요청 데이터 유효성 검증
        validateCourseRequest(courseRequestDto);
        
        // 2. 강의명 중복 확인
        if (courseRepository.existsByCourseName(courseRequestDto.getCourseName())) {
            throw new RuntimeException("이미 존재하는 강의명입니다: " + courseRequestDto.getCourseName());
        }
        
        // 3. 강사 정보 조회 및 검증
        Member instructor = memberRepository.findById(courseRequestDto.getInstructorId())
                .orElseThrow(() -> new RuntimeException("강사를 찾을 수 없습니다: " + courseRequestDto.getInstructorId()));
        
        if (instructor.getMemberType() != MemberType.INSTRUCTOR) {
            throw new RuntimeException("강사 권한이 없는 회원입니다: " + instructor.getName());
        }
        
        // 4. Course 엔티티 생성
        Course course = new Course(
                courseRequestDto.getCourseName(),
                courseRequestDto.getDescription(),
                instructor,
                courseRequestDto.getMaxStudents(),
                courseRequestDto.getStartDate(),
                courseRequestDto.getEndDate()
        );
        
        // 5. 강의 저장
        Course savedCourse = courseRepository.save(course);
        
        return CourseResponseDto.from(savedCourse);
    }
    
    /**
     * 강의 ID로 강의 정보를 조회합니다
     * 
     * @param courseId 강의 ID
     * @return 강의 정보
     * @throws RuntimeException 강의를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public CourseResponseDto getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));

        // 실제 수강인원 계산 (APPLIED, APPROVED 상태)
        int currentEnrollments = (int) enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
        return CourseResponseDto.from(course, currentEnrollments);
    }
    
    /**
     * 모든 강의 목록을 페이징으로 조회합니다
     * 
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getAllCourses(Pageable pageable) {
        Page<Course> courses = courseRepository.findAll(pageable);
        return courses.map(course -> {
            // 실제 수강인원 계산 (APPLIED, APPROVED 상태)
            int currentEnrollments = (int) enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
            return CourseResponseDto.from(course, currentEnrollments);
        });
    }
    
    /**
     * 강사별 강의 목록을 조회합니다
     * 
     * @param instructorId 강사 ID
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getCoursesByInstructor(Long instructorId, Pageable pageable) {
        Page<Course> courses = courseRepository.findByInstructorId(instructorId, pageable);
        return courses.map(CourseResponseDto::from);
    }
    
    /**
     * 강의 상태별 강의 목록을 조회합니다
     * 
     * @param status 강의 상태
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getCoursesByStatus(CourseStatus status, Pageable pageable) {
        Page<Course> courses = courseRepository.findByStatus(status, pageable);
        return courses.map(CourseResponseDto::from);
    }
    
    /**
     * 수강 신청 가능한 강의 목록을 조회합니다 (정렬 기준 포함)
     * 
     * @param pageable 페이징 정보
     * @return 수강 신청 가능한 강의 목록
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getAvailableCoursesForEnrollment(Pageable pageable) {
        Page<Course> courses = courseRepository.findAvailableCoursesForEnrollment(pageable);
        return courses.map(course -> {
            // 실제 수강인원 계산 (APPLIED, APPROVED 상태)
            int currentEnrollments = (int) enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
            return CourseResponseDto.from(course, currentEnrollments);
        });
    }
    
    /**
     * 수강 신청 가능한 강의 목록을 정렬 기준에 따라 조회합니다 (N+1 문제 해결)
     * 
     * @param sortBy 정렬 기준 (recent, applicants, remaining)
     * @param pageable 페이징 정보
     * @return 수강 신청 가능한 강의 목록
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> getAvailableCoursesForEnrollmentWithSort(String sortBy, Pageable pageable) {
        Page<Course> courses = courseRepository.findAvailableCoursesForEnrollmentWithSort(sortBy, pageable);
        return courses.map(course -> {
            // 실제 수강인원 계산 (APPLIED, APPROVED 상태)
            int currentEnrollments = (int) enrollmentRepository.countActiveEnrollmentsByCourse(course.getId());
            return CourseResponseDto.from(course, currentEnrollments);
        });
    }
    
    /**
     * 강의명으로 강의를 검색합니다
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 강의 목록
     */
    @Transactional(readOnly = true)
    public Page<CourseResponseDto> searchCoursesByName(String keyword, Pageable pageable) {
        Page<Course> courses = courseRepository.findByCourseNameContainingIgnoreCase(keyword, pageable);
        return courses.map(CourseResponseDto::from);
    }
    
    /**
     * 강의 정보를 수정합니다
     * 
     * @param courseId 강의 ID
     * @param courseRequestDto 수정할 강의 정보
     * @return 수정된 강의 정보
     * @throws RuntimeException 강의를 찾을 수 없는 경우
     */
    public CourseResponseDto updateCourse(Long courseId, CourseRequestDto courseRequestDto) {
        // 1. 기존 강의 조회
        Course existingCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));
        
        // 2. 요청 데이터 유효성 검증
        validateCourseRequest(courseRequestDto);
        
        // 3. 강의명 중복 확인 (자기 자신 제외)
        if (!existingCourse.getCourseName().equals(courseRequestDto.getCourseName()) &&
            courseRepository.existsByCourseName(courseRequestDto.getCourseName())) {
            throw new RuntimeException("이미 존재하는 강의명입니다: " + courseRequestDto.getCourseName());
        }
        
        // 4. 강사 정보 조회 및 검증
        Member instructor = memberRepository.findById(courseRequestDto.getInstructorId())
                .orElseThrow(() -> new RuntimeException("강사를 찾을 수 없습니다: " + courseRequestDto.getInstructorId()));
        
        if (instructor.getMemberType() != MemberType.INSTRUCTOR) {
            throw new RuntimeException("강사 권한이 없는 회원입니다: " + instructor.getName());
        }
        
        // 5. 강의 정보 업데이트
        existingCourse.setCourseName(courseRequestDto.getCourseName());
        existingCourse.setDescription(courseRequestDto.getDescription());
        existingCourse.setInstructor(instructor);
        existingCourse.setMaxStudents(courseRequestDto.getMaxStudents());
        existingCourse.setStartDate(courseRequestDto.getStartDate());
        existingCourse.setEndDate(courseRequestDto.getEndDate());
        
        // 6. 저장
        Course updatedCourse = courseRepository.save(existingCourse);
        
        return CourseResponseDto.from(updatedCourse);
    }
    
    /**
     * 강의를 삭제합니다
     * 
     * @param courseId 강의 ID
     * @throws RuntimeException 강의를 찾을 수 없거나 삭제할 수 없는 경우
     */
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));
        
        // 수강생이 있는 경우 삭제 불가 (실시간 DB 조회)
        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByCourse(courseId);
        if (activeEnrollments > 0) {
            throw new RuntimeException("수강생이 있는 강의는 삭제할 수 없습니다");
        }
        
        courseRepository.delete(course);
    }
    
    /**
     * 강의 상태를 변경합니다
     * 
     * @param courseId 강의 ID
     * @param status 변경할 상태
     * @return 상태가 변경된 강의 정보
     * @throws RuntimeException 강의를 찾을 수 없는 경우
     */
    public CourseResponseDto updateCourseStatus(Long courseId, CourseStatus status) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("강의를 찾을 수 없습니다: " + courseId));
        
        course.setStatus(status);
        Course updatedCourse = courseRepository.save(course);
        
        return CourseResponseDto.from(updatedCourse);
    }
    
    /**
     * 강의 요청 데이터의 유효성을 검증합니다
     * 
     * @param courseRequestDto 검증할 요청 데이터
     * @throws IllegalArgumentException 유효하지 않은 데이터인 경우
     */
    private void validateCourseRequest(CourseRequestDto courseRequestDto) {
        // 날짜 범위 검증
        if (!courseRequestDto.isValidDateRange()) {
            throw new IllegalArgumentException("강의 시작일은 종료일보다 이전이어야 합니다");
        }
        
        // 시작일이 현재 날짜 이후인지 검증
        if (courseRequestDto.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("강의 시작일은 현재 날짜 이후여야 합니다");
        }
    }
}
