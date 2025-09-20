package com.wb.edutask.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강의 생성/수정 요청을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequestDto {
    
    /**
     * 강의명 (필수, 최대 100자)
     */
    @NotBlank(message = "강의명은 필수입니다")
    @Size(max = 100, message = "강의명은 100자 이하여야 합니다")
    private String courseName;
    
    /**
     * 강의 설명 (최대 1000자)
     */
    @Size(max = 1000, message = "강의 설명은 1000자 이하여야 합니다")
    private String description;
    
    /**
     * 강사 ID (필수)
     */
    @NotNull(message = "강사 ID는 필수입니다")
    @Positive(message = "강사 ID는 양수여야 합니다")
    private Long instructorId;
    
    /**
     * 수강 정원 (1명 이상 100명 이하)
     */
    @NotNull(message = "수강 정원은 필수입니다")
    @Min(value = 1, message = "수강 정원은 최소 1명 이상이어야 합니다")
    @Max(value = 100, message = "수강 정원은 최대 100명 이하여야 합니다")
    private Integer maxStudents;
    
    /**
     * 강의 시작일 (필수)
     */
    @NotNull(message = "강의 시작일은 필수입니다")
    @Future(message = "강의 시작일은 현재 날짜보다 이후여야 합니다")
    private LocalDate startDate;
    
    /**
     * 강의 종료일 (필수)
     */
    @NotNull(message = "강의 종료일은 필수입니다")
    @Future(message = "강의 종료일은 현재 날짜보다 이후여야 합니다")
    private LocalDate endDate;
    
    /**
     * 날짜 범위가 유효한지 검증합니다
     * 
     * @return 유효성 여부
     */
    public boolean isValidDateRange() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
}
