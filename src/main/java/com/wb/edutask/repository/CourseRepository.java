package com.wb.edutask.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * 수강 신청 가능한 강의 목록을 조회합니다 (N+1 문제 해결을 위한 Fetch Join 사용)
     * 
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @Query("SELECT c FROM Course c JOIN FETCH c.instructor WHERE c.status IN ('SCHEDULED', 'IN_PROGRESS')")
    Page<Course> findAvailableCoursesForEnrollment(Pageable pageable);
    
    /**
     * 수강 신청 가능한 강의 목록을 정렬 기준에 따라 조회합니다 (N+1 문제 해결)
     * 
     * @param sortBy 정렬 기준 (recent, applicants, remaining)
     * @param pageable 페이징 정보
     * @return 강의 목록
     */
    @Query("SELECT c FROM Course c JOIN FETCH c.instructor " +
           "WHERE c.status IN ('SCHEDULED', 'IN_PROGRESS') " +
           "ORDER BY " +
           "CASE WHEN :sortBy = 'recent' THEN c.createdAt END DESC, " +
           "CASE WHEN :sortBy = 'applicants' THEN c.currentStudents END DESC, " +
           "CASE WHEN :sortBy = 'remaining' THEN (CAST(c.currentStudents AS double) / CAST(c.maxStudents AS double)) END DESC")
    Page<Course> findAvailableCoursesForEnrollmentWithSort(@Param("sortBy") String sortBy, Pageable pageable);
    
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
    
    /**
     * 강의의 현재 수강인원을 업데이트합니다 (Redis 동기화용)
     * 
     * @param courseId 강의 ID
     * @param currentStudents 현재 수강인원
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Course c SET c.currentStudents = :currentStudents WHERE c.id = :courseId")
    int updateCurrentStudents(@Param("courseId") Long courseId, @Param("currentStudents") Integer currentStudents);
}
