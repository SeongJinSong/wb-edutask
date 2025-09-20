package com.wb.edutask.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.wb.edutask.enums.MemberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 회원 정보를 나타내는 엔티티
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Entity
@Table(name = "members")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(exclude = "password")
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호는 010-XXXX-XXXX 형식이어야 합니다")
    @Column(name = "phone_number", nullable = false, unique = true, length = 13)
    private String phoneNumber;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 10, message = "비밀번호는 6자 이상 10자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", 
             message = "비밀번호는 영문 소문자, 대문자, 숫자를 포함해야 합니다")
    @Column(name = "password", nullable = false, length = 10)
    private String password;
    
    @NotNull(message = "회원 유형은 필수입니다")
    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false)
    private MemberType memberType;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Member 생성자
     * 
     * @param name 이름
     * @param email 이메일
     * @param phoneNumber 휴대폰 번호
     * @param password 비밀번호
     * @param memberType 회원 유형
     */
    public Member(String name, String email, String phoneNumber, String password, MemberType memberType) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.memberType = memberType;
    }
}
