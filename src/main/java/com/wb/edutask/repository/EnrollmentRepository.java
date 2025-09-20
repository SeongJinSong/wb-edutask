package com.wb.edutask.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Enrollment;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.EnrollmentStatus;

/**
 * 수강신청 정보 데이터 접근을 위한 Repository 인터페이스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    
    /**
     * 학생과 강의로 수강신청을 조회합니다
     * 
     * @param student 학생
     * @param course 강의
     * @return 수강신청 정보
     */
    Optional<Enrollment> findByStudentAndCourse(Member student, Course course);
    
    /**
     * 학생 ID와 강의 ID로 수강신청을 조회합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @return 수강신청 정보
     */
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    
    /**
     * 학생과 강의로 수강신청 존재 여부를 확인합니다
     * 
     * @param student 학생
     * @param course 강의
     * @return 존재 여부
     */
    boolean existsByStudentAndCourse(Member student, Course course);
    
    /**
     * 학생 ID와 강의 ID로 수강신청 존재 여부를 확인합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @return 존재 여부
     */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    
    /**
     * 학생 ID, 강의 ID, 상태로 수강신청 존재 여부를 확인합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @param status 수강신청 상태
     * @return 존재 여부
     */
    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);
    
    /**
     * 학생별 수강신청 목록을 조회합니다
     * 
     * @param student 학생
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByStudent(Member student, Pageable pageable);
    
    /**
     * 학생 ID로 수강신청 목록을 조회합니다
     * 
     * @param studentId 학생 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByStudentId(Long studentId, Pageable pageable);
    
    /**
     * 강의별 수강신청 목록을 조회합니다
     * 
     * @param course 강의
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByCourse(Course course, Pageable pageable);
    
    /**
     * 강의 ID로 수강신청 목록을 조회합니다
     * 
     * @param courseId 강의 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByCourseId(Long courseId, Pageable pageable);
    
    /**
     * 수강신청 상태별로 조회합니다
     * 
     * @param status 수강신청 상태
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByStatus(EnrollmentStatus status, Pageable pageable);
    
    /**
     * 학생의 특정 상태 수강신청 목록을 조회합니다
     * 
     * @param studentId 학생 ID
     * @param status 수강신청 상태
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);
    
    /**
     * 강의의 특정 상태 수강신청 목록을 조회합니다
     * 
     * @param courseId 강의 ID
     * @param status 수강신청 상태
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    Page<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status, Pageable pageable);
    
    /**
     * 강의별 승인된 수강신청 개수를 조회합니다
     * 
     * @param courseId 강의 ID
     * @return 승인된 수강신청 개수
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.status = 'APPROVED'")
    long countApprovedEnrollmentsByCourse(@Param("courseId") Long courseId);
    
    /**
     * 학생별 승인된 수강신청 개수를 조회합니다
     * 
     * @param studentId 학생 ID
     * @return 승인된 수강신청 개수
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = :studentId AND e.status = 'APPROVED'")
    long countApprovedEnrollmentsByStudent(@Param("studentId") Long studentId);
    
    /**
     * 강의별 활성 수강신청 개수를 조회합니다 (신청 + 승인 상태)
     * 
     * @param courseId 강의 ID
     * @return 활성 수강신청 개수
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.status IN ('APPLIED', 'APPROVED')")
    long countActiveEnrollmentsByCourse(@Param("courseId") Long courseId);
}
