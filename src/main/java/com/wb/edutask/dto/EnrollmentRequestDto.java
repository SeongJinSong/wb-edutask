package com.wb.edutask.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수강신청 요청을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequestDto {
    
    /**
     * 학생 ID (필수)
     */
    @NotNull(message = "학생 ID는 필수입니다")
    @Positive(message = "학생 ID는 양수여야 합니다")
    private Long studentId;
    
    /**
     * 강의 ID (필수)
     */
    @NotNull(message = "강의 ID는 필수입니다")
    @Positive(message = "강의 ID는 양수여야 합니다")
    private Long courseId;
}
