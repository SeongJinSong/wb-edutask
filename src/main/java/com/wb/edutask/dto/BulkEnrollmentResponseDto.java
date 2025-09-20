package com.wb.edutask.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 여러 강의 동시 수강신청 응답을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentResponseDto {
    
    /**
     * 학생 ID
     */
    private Long studentId;
    
    /**
     * 총 신청한 강의 수
     */
    private Integer totalRequested;
    
    /**
     * 성공한 수강신청 수
     */
    private Integer successCount;
    
    /**
     * 실패한 수강신청 수
     */
    private Integer failureCount;
    
    /**
     * 성공한 수강신청 목록
     */
    private List<EnrollmentResponseDto> successfulEnrollments;
    
    /**
     * 실패한 수강신청 목록
     */
    private List<EnrollmentFailureDto> failedEnrollments;
    
    /**
     * 수강신청 실패 정보를 담는 내부 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentFailureDto {
        
        /**
         * 실패한 강의 ID
         */
        private Long courseId;
        
        /**
         * 강의명
         */
        private String courseName;
        
        /**
         * 실패 사유
         */
        private String reason;
    }
}
