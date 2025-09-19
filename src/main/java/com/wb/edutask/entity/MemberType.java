package com.wb.edutask.entity;

/**
 * 회원 유형을 나타내는 열거형
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum MemberType {
    /**
     * 수강생 회원
     */
    STUDENT("수강생"),
    
    /**
     * 강사 회원
     */
    INSTRUCTOR("강사");
    
    private final String description;
    
    MemberType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
