package com.wb.edutask.dto;

import com.wb.edutask.entity.MemberType;
import java.time.LocalDateTime;

/**
 * 회원 정보 응답을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class MemberResponseDto {
    
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private MemberType memberType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 기본 생성자
    public MemberResponseDto() {}
    
    // 생성자
    public MemberResponseDto(Long id, String name, String email, String phoneNumber, 
                           MemberType memberType, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.memberType = memberType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Getter와 Setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public MemberType getMemberType() {
        return memberType;
    }
    
    public void setMemberType(MemberType memberType) {
        this.memberType = memberType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "MemberResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", memberType=" + memberType +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
