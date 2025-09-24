package com.wb.edutask.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리를 위한 핸들러
 * 표준화된 에러 응답과 로깅을 제공합니다
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 유효성 검증 실패 예외 처리
     * 
     * @param ex 유효성 검증 예외
     * @param request HTTP 요청
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("유효성 검증 실패 - Path: {}, Errors: {}", request.getRequestURI(), fieldErrors);
        
        Map<String, Object> response = createErrorResponse(
            "VALIDATION_FAILED",
            "유효성 검증 실패",
            "입력값을 확인해주세요",
            request.getRequestURI(),
            fieldErrors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * IllegalArgumentException 처리
     * 
     * @param ex 잘못된 인자 예외
     * @return 에러 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "잘못된 요청");
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Constraint Violation 예외 처리 (Path Variable, Request Parameter 검증)
     * 
     * @param ex Constraint Violation 예외
     * @return 에러 응답
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> details = new HashMap<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            details.put(fieldName, message);
        }
        
        response.put("error", "유효성 검증 실패");
        response.put("message", "입력 값이 유효하지 않습니다");
        response.put("details", details);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 필수 파라미터 누락 예외 처리
     * 
     * @param ex 파라미터 누락 예외
     * @return 에러 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "필수 파라미터 누락");
        response.put("message", "필수 파라미터가 누락되었습니다: " + ex.getParameterName());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 타입 불일치 예외 처리
     * 
     * @param ex 타입 불일치 예외
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "타입 불일치");
        response.put("message", "파라미터 타입이 올바르지 않습니다: " + ex.getName());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 런타임 예외 처리
     * 
     * @param ex 런타임 예외
     * @return 에러 응답
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "요청 처리 실패");
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 일반적인 예외 처리 (액추에이터 경로 제외)
     * 
     * @param ex 예외
     * @param request HTTP 요청
     * @return 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        // 액추에이터 경로는 제외 (Spring Boot가 자체 처리하도록)
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/actuator")) {
            throw new RuntimeException(ex); // 다시 던져서 Spring Boot가 처리하도록
        }
        
        log.error("예상치 못한 서버 오류 - Path: {}, Error: {}", requestPath, ex.getMessage(), ex);
        
        Map<String, Object> response = createErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "서버 오류",
            "요청 처리 중 오류가 발생했습니다",
            requestPath,
            null
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 표준화된 에러 응답을 생성합니다
     * 
     * @param errorCode 에러 코드
     * @param error 에러 제목
     * @param message 에러 메시지
     * @param path 요청 경로
     * @param details 상세 정보
     * @return 표준화된 에러 응답
     */
    private Map<String, Object> createErrorResponse(String errorCode, String error, String message, 
                                                   String path, Object details) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("errorCode", errorCode);
        response.put("error", error);
        response.put("message", message);
        response.put("path", path);
        
        if (details != null) {
            response.put("details", details);
        }
        
        return response;
    }
}
