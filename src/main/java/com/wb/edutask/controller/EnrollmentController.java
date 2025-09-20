package com.wb.edutask.controller;

import java.util.HashMap;
import java.util.Map;
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
import com.wb.edutask.dto.BulkEnrollmentRequestDto;
import com.wb.edutask.dto.BulkEnrollmentResponseDto;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentResponseDto;
import com.wb.edutask.enums.EnrollmentStatus;
import com.wb.edutask.service.EnrollmentQueueService;
import com.wb.edutask.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 수강신청 관리를 위한 REST API 컨트롤러
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@Validated
public class EnrollmentController {
    
    private final EnrollmentService enrollmentService;
    private final EnrollmentQueueService enrollmentQueueService;
    
    /**
     * EnrollmentController 생성자
     * 
     * @param enrollmentService 수강신청 서비스
     * @param enrollmentQueueService 수강신청 큐 서비스
     */
    @Autowired
    public EnrollmentController(EnrollmentService enrollmentService, 
                              EnrollmentQueueService enrollmentQueueService) {
        this.enrollmentService = enrollmentService;
        this.enrollmentQueueService = enrollmentQueueService;
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
    
    /**
     * 여러 강의에 동시 수강신청을 처리합니다
     * 
     * @param bulkRequestDto 여러 강의 수강신청 요청 정보
     * @return 수강신청 결과 (성공/실패 목록 포함)
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkEnrollmentResponseDto> enrollMultipleCourses(
            @Valid @RequestBody BulkEnrollmentRequestDto bulkRequestDto) {
        
        BulkEnrollmentResponseDto result = enrollmentService.enrollMultipleCourses(bulkRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    /**
     * 큐 기반 수강신청을 요청합니다 (대용량 트래픽 대응)
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return 큐 ID와 상태 정보
     */
    @PostMapping("/queue")
    public ResponseEntity<Map<String, Object>> enrollCourseWithQueue(
            @Valid @RequestBody EnrollmentRequestDto enrollmentRequestDto) {
        
        String queueId = enrollmentQueueService.enqueueEnrollmentRequest(
            enrollmentRequestDto.getStudentId(), 
            enrollmentRequestDto.getCourseId()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("queueId", queueId);
        response.put("status", "QUEUED");
        response.put("message", "수강신청 요청이 큐에 등록되었습니다. 잠시 후 결과를 확인해주세요.");
        response.put("queueSize", enrollmentQueueService.getQueueSize());
        
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * 큐 기반 수강신청 결과를 조회합니다
     * 
     * @param queueId 큐 ID
     * @return 처리 결과
     */
    @GetMapping("/queue/{queueId}")
    public ResponseEntity<Map<String, Object>> getEnrollmentQueueResult(
            @PathVariable String queueId) {
        
        String status = enrollmentQueueService.getEnrollmentStatus(queueId);
        Object result = enrollmentQueueService.getEnrollmentResult(queueId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("queueId", queueId);
        response.put("status", status);
        
        if ("COMPLETED".equals(status)) {
            response.put("result", result);
            response.put("message", "수강신청이 성공적으로 완료되었습니다.");
        } else if ("FAILED".equals(status)) {
            response.put("error", result);
            response.put("message", "수강신청 처리에 실패했습니다.");
        } else if ("PROCESSING".equals(status)) {
            response.put("message", "수강신청을 처리 중입니다. 잠시만 기다려주세요.");
        } else {
            response.put("message", "수강신청 요청이 큐에서 대기 중입니다.");
            response.put("queueSize", enrollmentQueueService.getQueueSize());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 현재 큐 상태를 조회합니다
     * 
     * @return 큐 상태 정보
     */
    @GetMapping("/queue/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalQueueSize", enrollmentQueueService.getTotalQueueSize());
        response.put("message", "현재 전체 큐 대기 상태입니다.");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 강의의 큐 상태를 조회합니다
     * 
     * @param courseId 강의 ID
     * @return 강의별 큐 상태 정보
     */
    @GetMapping("/queue/course/{courseId}")
    public ResponseEntity<Map<String, Object>> getCourseQueueStatus(
            @PathVariable @Positive Long courseId) {
        
        Map<String, Object> status = enrollmentQueueService.getCourseQueueStatus(courseId);
        status.put("message", "강의별 큐 상태입니다.");
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Redis Queue에서 순차적으로 DB INSERT를 처리합니다 (H2 락 문제 해결)
     * 
     * @return 처리 결과
     */
    @PostMapping("/queue/process")
    public ResponseEntity<Map<String, Object>> processDbQueue() {
        try {
            int processedCount = enrollmentService.processDbQueue();
            
            Map<String, Object> response = new HashMap<>();
            response.put("processedCount", processedCount);
            response.put("message", "DB 큐 처리가 완료되었습니다");
            response.put("status", "SUCCESS");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * DB 큐 상태를 조회합니다
     * 
     * @return DB 큐 상태
     */
    @GetMapping("/queue/db/status")
    public ResponseEntity<Map<String, Object>> getDbQueueStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("dbQueueSize", enrollmentQueueService.getDbQueueSize());
            status.put("totalQueueSize", enrollmentQueueService.getTotalQueueSize());
            status.put("message", "DB 큐 상태 조회 완료");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
