package com.wb.edutask.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 여러 강의 동시 수강신청 요청을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentRequestDto {
    
    /**
     * 학생 ID (필수)
     */
    @NotNull(message = "학생 ID는 필수입니다")
    @Positive(message = "학생 ID는 양수여야 합니다")
    private Long studentId;
    
    /**
     * 수강신청할 강의 ID 목록 (필수, 최대 10개)
     */
    @NotEmpty(message = "수강신청할 강의 목록은 필수입니다")
    @Size(max = 10, message = "한 번에 최대 10개의 강의까지 신청할 수 있습니다")
    private List<@NotNull(message = "강의 ID는 필수입니다") @Positive(message = "강의 ID는 양수여야 합니다") Long> courseIds;
}
