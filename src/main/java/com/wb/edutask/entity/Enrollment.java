package com.wb.edutask.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.wb.edutask.enums.EnrollmentStatus;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 수강신청 정보를 나타내는 JPA 엔티티
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Entity
@Table(name = "enrollments", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString(exclude = {"student", "course"})
public class Enrollment {
    
    /**
     * 수강신청 고유 식별자
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 수강신청한 학생 (Member와 다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @NotNull(message = "학생 정보는 필수입니다")
    private Member student;
    
    /**
     * 신청한 강의 (Course와 다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "강의 정보는 필수입니다")
    private Course course;
    
    /**
     * 수강신청 상태
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.APPLIED;
    
    /**
     * 신청 일시
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime appliedAt;
    
    /**
     * 수정 일시 (상태 변경 등)
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 승인 일시
     */
    @Column
    private LocalDateTime approvedAt;
    
    /**
     * 취소/거절 일시
     */
    @Column
    private LocalDateTime cancelledAt;
    
    /**
     * 취소/거절 사유
     */
    @Column(length = 500)
    private String reason;
    
    /**
     * Enrollment 생성자
     * 
     * @param student 학생
     * @param course 강의
     */
    public Enrollment(Member student, Course course) {
        this.student = student;
        this.course = course;
        this.status = EnrollmentStatus.APPLIED;
    }
    
    /**
     * 수강신청을 승인합니다
     */
    public void approve() {
        this.status = EnrollmentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.reason = null;
    }
    
    /**
     * 수강신청을 취소합니다
     * 
     * @param reason 취소 사유
     */
    public void cancel(String reason) {
        this.status = EnrollmentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.reason = reason;
    }
    
    /**
     * 수강신청을 거절합니다
     * 
     * @param reason 거절 사유
     */
    public void reject(String reason) {
        this.status = EnrollmentStatus.REJECTED;
        this.cancelledAt = LocalDateTime.now();
        this.reason = reason;
    }
    
    /**
     * 수강신청이 활성 상태인지 확인합니다 (신청 또는 승인 상태)
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return status == EnrollmentStatus.APPLIED || status == EnrollmentStatus.APPROVED;
    }
    
    /**
     * 수강신청이 승인된 상태인지 확인합니다
     * 
     * @return 승인 상태 여부
     */
    public boolean isApproved() {
        return status == EnrollmentStatus.APPROVED;
    }
}
