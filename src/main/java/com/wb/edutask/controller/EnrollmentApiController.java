package com.wb.edutask.controller;

import java.util.HashMap;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wb.edutask.dto.BulkEnrollmentRequestDto;
import com.wb.edutask.dto.BulkEnrollmentResponseDto;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentResponseDto;
import com.wb.edutask.enums.EnrollmentStatus;
import com.wb.edutask.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 수강신청 관리를 위한 REST API 컨트롤러
 * API 전용 컨트롤러로 JSON 응답만 처리
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@Validated
@RequiredArgsConstructor
public class EnrollmentApiController {
    
    private final EnrollmentService enrollmentService;
    
    
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
    
    
    // ==================== 비동기 처리 API (멀티서버 환경 대응) ====================
    
    /**
     * 비동기 수강신청 (멀티서버 환경 대응)
     * Lua 스크립트로 동시성 제어 후 즉시 DB 저장
     * 
     * @param enrollmentRequestDto 수강신청 요청 정보
     * @return 수강신청 응답 (비동기 처리 시작 확인)
     */
    @PostMapping("/async")
    public ResponseEntity<Map<String, Object>> enrollCourseAsync(
            @Valid @RequestBody EnrollmentRequestDto enrollmentRequestDto) {
        
        try {
            // 비동기 처리 시작 (CompletableFuture 반환하지만 즉시 응답)
            enrollmentService.enrollCourseAsync(enrollmentRequestDto);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "수강신청이 비동기로 처리 중입니다.");
            response.put("status", "PROCESSING");
            response.put("studentId", enrollmentRequestDto.getStudentId());
            response.put("courseId", enrollmentRequestDto.getCourseId());
            response.put("processingMethod", "ASYNC_WITH_LUA_SCRIPT");
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 비동기 수강신청 취소 (멀티서버 환경 대응)
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @param reason 취소 사유
     * @return 취소 응답 (비동기 처리 시작 확인)
     */
    @DeleteMapping("/async/student/{studentId}/course/{courseId}")
    public ResponseEntity<Map<String, Object>> cancelEnrollmentAsync(
            @PathVariable @Positive Long studentId,
            @PathVariable @Positive Long courseId,
            @RequestParam(required = false) String reason) {
        
        try {
            // 비동기 처리 시작
            enrollmentService.cancelEnrollmentAsync(studentId, courseId, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "수강신청 취소가 비동기로 처리 중입니다.");
            response.put("status", "PROCESSING");
            response.put("studentId", studentId);
            response.put("courseId", courseId);
            response.put("reason", reason);
            response.put("processingMethod", "ASYNC_DIRECT_DB");
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 비동기 처리 상태를 조회합니다
     * 
     * @return 비동기 처리 상태 정보
     */
    @GetMapping("/async/status")
    public ResponseEntity<Map<String, Object>> getAsyncStatus() {
        
        try {
            Map<String, Object> status = new HashMap<>();
            
            status.put("message", "비동기 처리 상태입니다.");
            status.put("multiServerReady", true);
            status.put("processingMethod", "ASYNC_WITH_LUA_SCRIPT");
            status.put("concurrencyControl", "REDIS_LUA_SCRIPT");
            status.put("description", "Lua 스크립트로 동시성 제어 후 즉시 DB 저장하는 방식");
            
            // 스레드 풀 상태 (간단한 정보만)
            status.put("threadPoolInfo", "enrollmentTaskExecutor: 5-20 threads, queue: 100");
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "ERROR");
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
