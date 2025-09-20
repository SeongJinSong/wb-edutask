package com.wb.edutask.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.CourseStatus;

/**
 * 강의 정보 데이터 접근을 위한 Repository 인터페이스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    
    /**
     * 강의명으로 강의를 조회합니다
     * 
     * @param courseName 강의명
     * @return 강의 정보
     */
    Optional<Course> findByCourseName(String courseName);
    
    /**
     * 강의명 중복 여부를 확인합니다
     * 
     * @param courseName 강의명
     * @return 중복 여부
     */
    boolean existsByCourseName(String courseName);
    
    /**
     * 강사별 강의 목록을 조회합니다
     * 
     * @param instructor 강사 정보
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    Page<Course> findByInstructor(Member instructor, Pageable pageable);
    
    /**
     * 강사 ID로 강의 목록을 조회합니다
     * 
     * @param instructorId 강사 ID
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    Page<Course> findByInstructorId(Long instructorId, Pageable pageable);
    
    /**
     * 강의 상태별로 강의 목록을 조회합니다
     * 
     * @param status 강의 상태
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);
    
    /**
     * 강의명에 특정 키워드가 포함된 강의를 검색합니다
     * 
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    Page<Course> findByCourseNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    /**
     * 수강 신청 가능한 강의 목록을 조회합니다 (정원 미달 + 개설예정 상태)
     * 
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @Query("SELECT c FROM Course c WHERE c.status IN ('SCHEDULED', 'IN_PROGRESS')")
    Page<Course> findAvailableCoursesForEnrollment(Pageable pageable);
    
    /**
     * 특정 기간 내에 시작하는 강의 목록을 조회합니다
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    Page<Course> findByStartDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 강사별 진행 중인 강의 개수를 조회합니다
     * 
     * @param instructorId 강사 ID
     * @return 진행 중인 강의 개수
     */
    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructor.id = :instructorId AND c.status = 'IN_PROGRESS'")
    long countInProgressCoursesByInstructor(@Param("instructorId") Long instructorId);
    
    /**
     * 강사별 전체 강의 개수를 조회합니다
     * 
     * @param instructorId 강사 ID
     * @return 전체 강의 개수
     */
    long countByInstructorId(Long instructorId);
    
    /**
     * 수강 신청 가능한 강의 개수를 조회합니다
     * 
     * @return 수강 신청 가능한 강의 개수
     */
    @Query("SELECT COUNT(c) FROM Course c WHERE c.status IN ('SCHEDULED', 'IN_PROGRESS')")
    long countAvailableCoursesForEnrollment();
}
