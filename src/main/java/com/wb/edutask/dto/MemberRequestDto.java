package com.wb.edutask.dto;

import com.wb.edutask.enums.MemberType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 가입 요청을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}
