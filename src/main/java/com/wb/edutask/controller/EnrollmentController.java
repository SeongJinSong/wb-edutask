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
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentResponseDto;
import com.wb.edutask.enums.EnrollmentStatus;
import com.wb.edutask.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 수강신청 관리를 위한 REST API 컨트롤러
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/enrollments")
@Validated
public class EnrollmentController {
    
    private final EnrollmentService enrollmentService;
    
    /**
     * EnrollmentController 생성자
     * 
     * @param enrollmentService 수강신청 서비스
     */
    @Autowired
    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }
    
    /**
     * 수강신청을 처리합니다
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return 생성된 수강신청 정보
     */
    @PostMapping
    public ResponseEntity<EnrollmentResponseDto> enrollCourse(
            @Valid @RequestBody EnrollmentRequestDto enrollmentRequestDto) {
        
        try {
            EnrollmentResponseDto enrollment = enrollmentService.enrollCourse(enrollmentRequestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 수강신청 ID로 수강신청 정보를 조회합니다
     * 
     * @param enrollmentId 수강신청 ID
     * @return 수강신청 정보
     */
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentResponseDto> getEnrollment(
            @PathVariable @Positive(message = "수강신청 ID는 양수여야 합니다") Long enrollmentId) {
        
        try {
            EnrollmentResponseDto enrollment = enrollmentService.getEnrollmentById(enrollmentId);
            return ResponseEntity.ok(enrollment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 학생별 수강신청 목록을 조회합니다
     * 
     * @param studentId 학생 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<Page<EnrollmentResponseDto>> getEnrollmentsByStudent(
            @PathVariable @Positive(message = "학생 ID는 양수여야 합니다") Long studentId,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<EnrollmentResponseDto> enrollments = enrollmentService.getEnrollmentsByStudent(studentId, pageable);
        return ResponseEntity.ok(enrollments);
    }
    
    /**
     * 강의별 수강신청 목록을 조회합니다
     * 
     * @param courseId 강의 ID
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<Page<EnrollmentResponseDto>> getEnrollmentsByCourse(
            @PathVariable @Positive(message = "강의 ID는 양수여야 합니다") Long courseId,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<EnrollmentResponseDto> enrollments = enrollmentService.getEnrollmentsByCourse(courseId, pageable);
        return ResponseEntity.ok(enrollments);
    }
    
    /**
     * 수강신청 상태별로 조회합니다
     * 
     * @param status 수강신청 상태
     * @param pageable 페이징 정보
     * @return 수강신청 목록
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<EnrollmentResponseDto>> getEnrollmentsByStatus(
            @PathVariable EnrollmentStatus status,
            @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<EnrollmentResponseDto> enrollments = enrollmentService.getEnrollmentsByStatus(status, pageable);
        return ResponseEntity.ok(enrollments);
    }
    
    /**
     * 수강신청을 취소합니다
     * 
     * @param enrollmentId 수강신청 ID
     * @param reason 취소 사유 (선택사항)
     * @return 취소된 수강신청 정보
     */
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentResponseDto> cancelEnrollment(
            @PathVariable @Positive(message = "수강신청 ID는 양수여야 합니다") Long enrollmentId,
            @RequestParam(required = false, defaultValue = "학생 요청에 의한 취소") String reason) {
        
        try {
            EnrollmentResponseDto enrollment = enrollmentService.cancelEnrollment(enrollmentId, reason);
            return ResponseEntity.ok(enrollment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 수강신청을 승인합니다 (관리자/강사용)
     * 
     * @param enrollmentId 수강신청 ID
     * @return 승인된 수강신청 정보
     */
    @PutMapping("/{enrollmentId}/approve")
    public ResponseEntity<EnrollmentResponseDto> approveEnrollment(
            @PathVariable @Positive(message = "수강신청 ID는 양수여야 합니다") Long enrollmentId) {
        
        try {
            EnrollmentResponseDto enrollment = enrollmentService.approveEnrollment(enrollmentId);
            return ResponseEntity.ok(enrollment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 수강신청을 거절합니다 (관리자/강사용)
     * 
     * @param enrollmentId 수강신청 ID
     * @param reason 거절 사유
     * @return 거절된 수강신청 정보
     */
    @PutMapping("/{enrollmentId}/reject")
    public ResponseEntity<EnrollmentResponseDto> rejectEnrollment(
            @PathVariable @Positive(message = "수강신청 ID는 양수여야 합니다") Long enrollmentId,
            @RequestParam @NotBlank(message = "거절 사유는 필수입니다") String reason) {
        
        try {
            EnrollmentResponseDto enrollment = enrollmentService.rejectEnrollment(enrollmentId, reason);
            return ResponseEntity.ok(enrollment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 수강신청 통계 정보를 조회합니다
     * 
     * @return 수강신청 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<EnrollmentStats> getEnrollmentStats() {
        // TODO: 통계 서비스 구현 후 연결
        EnrollmentStats stats = new EnrollmentStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 수강신청 통계 정보를 나타내는 내부 클래스
     */
    public static class EnrollmentStats {
        private long totalEnrollments = 0;
        private long approvedEnrollments = 0;
        private long pendingEnrollments = 0;
        private long cancelledEnrollments = 0;
        
        // Getter 메서드들
        public long getTotalEnrollments() {
            return totalEnrollments;
        }
        
        public long getApprovedEnrollments() {
            return approvedEnrollments;
        }
        
        public long getPendingEnrollments() {
            return pendingEnrollments;
        }
        
        public long getCancelledEnrollments() {
            return cancelledEnrollments;
        }
        
        // Setter 메서드들
        public void setTotalEnrollments(long totalEnrollments) {
            this.totalEnrollments = totalEnrollments;
        }
        
        public void setApprovedEnrollments(long approvedEnrollments) {
            this.approvedEnrollments = approvedEnrollments;
        }
        
        public void setPendingEnrollments(long pendingEnrollments) {
            this.pendingEnrollments = pendingEnrollments;
        }
        
        public void setCancelledEnrollments(long cancelledEnrollments) {
            this.cancelledEnrollments = cancelledEnrollments;
        }
    }
}
