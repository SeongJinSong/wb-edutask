package com.wb.edutask.dto;

import com.wb.edutask.entity.MemberType;
import jakarta.validation.constraints.*;

/**
 * 회원 가입 요청을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public class MemberRequestDto {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    private String name;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;
    
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호는 010-XXXX-XXXX 형식이어야 합니다")
    private String phoneNumber;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 10, message = "비밀번호는 6자 이상 10자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", 
             message = "비밀번호는 영문 소문자, 대문자, 숫자를 포함해야 합니다")
    private String password;
    
    @NotNull(message = "회원 유형은 필수입니다")
    private MemberType memberType;
    
    // 기본 생성자
    public MemberRequestDto() {}
    
    // 생성자
    public MemberRequestDto(String name, String email, String phoneNumber, String password, MemberType memberType) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.memberType = memberType;
    }
    
    // Getter와 Setter
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public MemberType getMemberType() {
        return memberType;
    }
    
    public void setMemberType(MemberType memberType) {
        this.memberType = memberType;
    }
    
    @Override
    public String toString() {
        return "MemberRequestDto{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", memberType=" + memberType +
                '}';
    }
}
