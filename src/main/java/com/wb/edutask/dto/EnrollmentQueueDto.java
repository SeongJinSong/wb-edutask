package com.wb.edutask.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수강신청 큐 처리를 위한 DTO
 * Redis 큐에 저장되는 데이터 구조
 *
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentQueueDto {
    
    /**
     * 큐 ID (UUID)
     */
    private String queueId;
    
    /**
     * 학생 ID
     */
    private Long studentId;
    
    /**
     * 강의 ID
     */
    private Long courseId;
    
    /**
     * 요청 시간
     */
    private LocalDateTime requestedAt;
    
    /**
     * 처리 상태 (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    private String status;
    
    /**
     * 처리 결과 (성공 시 EnrollmentResponseDto, 실패 시 에러 메시지)
     */
    private Object result;
    
    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;
}
