package com.wb.edutask.enums;

/**
 * 회원 유형을 나타내는 열거형
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum MemberType {
    
    /**
     * 학생 - 강의를 수강하는 회원
     */
    STUDENT("학생"),
    
    /**
     * 강사 - 강의를 개설하고 진행하는 회원
     */
    INSTRUCTOR("강사");
    
    private final String description;
    
    /**
     * MemberType 생성자
     * 
     * @param description 회원 유형 설명
     */
    MemberType(String description) {
        this.description = description;
    }
    
    /**
     * 회원 유형 설명을 반환합니다
     * 
     * @return 회원 유형 설명
     */
    public String getDescription() {
        return description;
    }
}
