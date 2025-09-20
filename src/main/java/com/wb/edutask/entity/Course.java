package com.wb.edutask.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.wb.edutask.enums.CourseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 강의 정보를 나타내는 JPA 엔티티
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "instructor")
public class Course {
    
    /**
     * 강의 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 강의명 (필수, 최대 100자)
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "강의명은 필수입니다")
    @Size(max = 100, message = "강의명은 100자 이하여야 합니다")
    private String courseName;
    
    /**
     * 강의 설명 (최대 1000자)
     */
    @Column(length = 1000)
    @Size(max = 1000, message = "강의 설명은 1000자 이하여야 합니다")
    private String description;
    
    /**
     * 강사 정보 (Member와 다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    @NotNull(message = "강사 정보는 필수입니다")
    private Member instructor;
    
    /**
     * 수강 정원 (1명 이상 100명 이하)
     */
    @Column(nullable = false)
    @NotNull(message = "수강 정원은 필수입니다")
    @Min(value = 1, message = "수강 정원은 최소 1명 이상이어야 합니다")
    @Max(value = 100, message = "수강 정원은 최대 100명 이하여야 합니다")
    private Integer maxStudents;
    
    /**
     * 현재 수강 인원
     */
    @Column(nullable = false)
    private Integer currentStudents = 0;
    
    /**
     * 강의 시작일
     */
    @Column(nullable = false)
    @NotNull(message = "강의 시작일은 필수입니다")
    private LocalDate startDate;
    
    /**
     * 강의 종료일
     */
    @Column(nullable = false)
    @NotNull(message = "강의 종료일은 필수입니다")
    private LocalDate endDate;
    
    /**
     * 강의 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.SCHEDULED;
    
    /**
     * 생성 일시
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 수정 일시
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    
    /**
     * Course 생성자
     * 
     * @param courseName 강의명
     * @param description 강의 설명
     * @param instructor 강사
     * @param maxStudents 수강 정원
     * @param startDate 시작일
     * @param endDate 종료일
     */
    public Course(String courseName, String description, Member instructor, 
                  Integer maxStudents, LocalDate startDate, LocalDate endDate) {
        this.courseName = courseName;
        this.description = description;
        this.instructor = instructor;
        this.maxStudents = maxStudents;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentStudents = 0;
        this.status = CourseStatus.SCHEDULED;
    }
    
    /**
     * 수강 인원을 증가시킵니다
     * 
     * @throws IllegalStateException 정원이 초과된 경우
     */
    public void increaseCurrentStudents() {
        if (currentStudents >= maxStudents) {
            throw new IllegalStateException("수강 정원이 초과되었습니다");
        }
        this.currentStudents++;
    }
    
    /**
     * 수강 인원을 감소시킵니다
     * 
     * @throws IllegalStateException 현재 수강 인원이 0명인 경우
     */
    public void decreaseCurrentStudents() {
        if (currentStudents <= 0) {
            throw new IllegalStateException("현재 수강 인원이 0명입니다");
        }
        this.currentStudents--;
    }
    
    /**
     * 수강 신청이 가능한지 확인합니다
     * 
     * @return 수강 신청 가능 여부
     */
    public boolean canEnroll() {
        return currentStudents < maxStudents && status == CourseStatus.SCHEDULED;
    }
    
    /**
     * 강의 기간이 유효한지 검증합니다
     * 
     * @return 유효성 여부
     */
    public boolean isValidDateRange() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
}
