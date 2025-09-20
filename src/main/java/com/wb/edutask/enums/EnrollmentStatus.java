package com.wb.edutask.enums;

/**
 * 수강신청 상태를 나타내는 열거형
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum EnrollmentStatus {
    
    /**
     * 신청 - 수강신청이 접수된 상태
     */
    APPLIED("신청"),
    
    /**
     * 승인 - 수강신청이 승인된 상태
     */
    APPROVED("승인"),
    
    /**
     * 취소 - 수강신청이 취소된 상태
     */
    CANCELLED("취소"),
    
    /**
     * 거절 - 수강신청이 거절된 상태 (정원 초과 등)
     */
    REJECTED("거절");
    
    private final String description;
    
    /**
     * EnrollmentStatus 생성자
     * 
     * @param description 상태 설명
     */
    EnrollmentStatus(String description) {
        this.description = description;
    }
    
    /**
     * 상태 설명을 반환합니다
     * 
     * @return 상태 설명
     */
    public String getDescription() {
        return description;
    }
}
