package com.wb.edutask.enums;

/**
 * 강의 상태를 나타내는 열거형
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
public enum CourseStatus {
    
    /**
     * 개설 예정 - 강의가 개설되었지만 아직 시작되지 않은 상태
     */
    SCHEDULED("개설예정"),
    
    /**
     * 진행 중 - 현재 강의가 진행되고 있는 상태
     */
    IN_PROGRESS("진행중"),
    
    /**
     * 종료 - 강의가 완료된 상태
     */
    COMPLETED("종료"),
    
    /**
     * 취소 - 강의가 취소된 상태
     */
    CANCELLED("취소");
    
    private final String description;
    
    /**
     * CourseStatus 생성자
     * 
     * @param description 상태 설명
     */
    CourseStatus(String description) {
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
