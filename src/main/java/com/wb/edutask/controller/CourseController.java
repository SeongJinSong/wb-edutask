package com.wb.edutask.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wb.edutask.dto.CourseRequestDto;
import com.wb.edutask.dto.CourseResponseDto;
import com.wb.edutask.enums.CourseStatus;
import com.wb.edutask.service.CourseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * 강의 관리를 위한 REST API 컨트롤러
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/api/v1/courses")
@Validated
public class CourseController {
    
    private final CourseService courseService;
    
    /**
     * CourseController 생성자
     * 
     * @param courseService 강의 서비스
     */
    @Autowired
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }
    
    /**
     * 새로운 강의를 생성합니다
     * 
     * @param courseRequestDto 강의 생성 요청 정보
     * @return 생성된 강의 정보
     */
    @PostMapping
    public ResponseEntity<CourseResponseDto> createCourse(
            @Valid @RequestBody CourseRequestDto courseRequestDto) {
        
        try {
            CourseResponseDto course = courseService.createCourse(courseRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(course);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 강의 ID로 강의 정보를 조회합니다
     * 
     * @param courseId 강의 ID
     * @return 강의 정보
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> getCourse(
            @PathVariable @Positive(message = "강의 ID는 양수여야 합니다") Long courseId) {
        
        try {
            CourseResponseDto course = courseService.getCourseById(courseId);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 모든 강의 목록을 페이징으로 조회합니다
     * 
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @GetMapping
    public ResponseEntity<Page<CourseResponseDto>> getAllCourses(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<CourseResponseDto> courses = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(courses);
    }
    
    /**
     * 강사별 강의 목록을 조회합니다
     * 
     * @param instructorId 강사 ID
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<Page<CourseResponseDto>> getCoursesByInstructor(
            @PathVariable @Positive(message = "강사 ID는 양수여야 합니다") Long instructorId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<CourseResponseDto> courses = courseService.getCoursesByInstructor(instructorId, pageable);
        return ResponseEntity.ok(courses);
    }
    
    /**
     * 강의 상태별 강의 목록을 조회합니다
     * 
     * @param status 강의 상태
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<CourseResponseDto>> getCoursesByStatus(
            @PathVariable CourseStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<CourseResponseDto> courses = courseService.getCoursesByStatus(status, pageable);
        return ResponseEntity.ok(courses);
    }
    
    /**
     * 수강 신청 가능한 강의 목록을 조회합니다
     * 
     * @param pageable 페이징 정보
     * @return 수강 신청 가능한 강의 목록
     */
    @GetMapping("/available")
    public ResponseEntity<Page<CourseResponseDto>> getAvailableCoursesForEnrollment(
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<CourseResponseDto> courses = courseService.getAvailableCoursesForEnrollment(pageable);
        return ResponseEntity.ok(courses);
    }
    
    /**
     * 강의명으로 강의를 검색합니다
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 강의 목록
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CourseResponseDto>> searchCoursesByName(
            @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<CourseResponseDto> courses = courseService.searchCoursesByName(keyword, pageable);
        return ResponseEntity.ok(courses);
    }
    
    /**
     * 강의 정보를 수정합니다
     * 
     * @param courseId 강의 ID
     * @param courseRequestDto 수정할 강의 정보
     * @return 수정된 강의 정보
     */
    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> updateCourse(
            @PathVariable @Positive(message = "강의 ID는 양수여야 합니다") Long courseId,
            @Valid @RequestBody CourseRequestDto courseRequestDto) {
        
        try {
            CourseResponseDto course = courseService.updateCourse(courseId, courseRequestDto);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 강의를 삭제합니다
     * 
     * @param courseId 강의 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable @Positive(message = "강의 ID는 양수여야 합니다") Long courseId) {
        
        try {
            courseService.deleteCourse(courseId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 강의 상태를 변경합니다
     * 
     * @param courseId 강의 ID
     * @param status 변경할 상태
     * @return 상태가 변경된 강의 정보
     */
    @PutMapping("/{courseId}/status")
    public ResponseEntity<CourseResponseDto> updateCourseStatus(
            @PathVariable @Positive(message = "강의 ID는 양수여야 합니다") Long courseId,
            @RequestParam CourseStatus status) {
        
        try {
            CourseResponseDto course = courseService.updateCourseStatus(courseId, status);
            return ResponseEntity.ok(course);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 강의 통계 정보를 조회합니다
     * 
     * @return 강의 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<CourseStats> getCourseStats() {
        // TODO: 통계 서비스 구현 후 연결
        CourseStats stats = new CourseStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 강의 통계 정보를 나타내는 내부 클래스
     */
    public static class CourseStats {
        private long totalCourses = 0;
        private long scheduledCourses = 0;
        private long inProgressCourses = 0;
        private long completedCourses = 0;
        private long availableCoursesForEnrollment = 0;
        
        // Getter 메서드들
        public long getTotalCourses() {
            return totalCourses;
        }
        
        public long getScheduledCourses() {
            return scheduledCourses;
        }
        
        public long getInProgressCourses() {
            return inProgressCourses;
        }
        
        public long getCompletedCourses() {
            return completedCourses;
        }
        
        public long getAvailableCoursesForEnrollment() {
            return availableCoursesForEnrollment;
        }
        
        // Setter 메서드들
        public void setTotalCourses(long totalCourses) {
            this.totalCourses = totalCourses;
        }
        
        public void setScheduledCourses(long scheduledCourses) {
            this.scheduledCourses = scheduledCourses;
        }
        
        public void setInProgressCourses(long inProgressCourses) {
            this.inProgressCourses = inProgressCourses;
        }
        
        public void setCompletedCourses(long completedCourses) {
            this.completedCourses = completedCourses;
        }
        
        public void setAvailableCoursesForEnrollment(long availableCoursesForEnrollment) {
            this.availableCoursesForEnrollment = availableCoursesForEnrollment;
        }
    }
}
