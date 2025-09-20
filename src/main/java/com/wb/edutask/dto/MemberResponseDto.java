package com.wb.edutask.dto;

import java.time.LocalDateTime;
import com.wb.edutask.enums.MemberType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원 정보 응답을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponseDto {
    
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private MemberType memberType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
}
