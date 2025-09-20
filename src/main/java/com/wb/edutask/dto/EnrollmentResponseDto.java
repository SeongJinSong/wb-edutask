package com.wb.edutask.dto;

import java.time.LocalDateTime;
import com.wb.edutask.entity.Enrollment;
import com.wb.edutask.enums.EnrollmentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수강신청 응답을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@Setter
@NoArgsConstructor
public class EnrollmentResponseDto {
    
    /**
     * 수강신청 ID
     */
    private Long id;
    
    /**
     * 학생 정보
     */
    private StudentInfo student;
    
    /**
     * 강의 정보
     */
    private CourseInfo course;
    
    /**
     * 수강신청 상태
     */
    private EnrollmentStatus status;
    
    /**
     * 수강신청 상태 설명
     */
    private String statusDescription;
    
    /**
     * 신청 일시
     */
    private LocalDateTime appliedAt;
    
    /**
     * 승인 일시
     */
    private LocalDateTime approvedAt;
    
    /**
     * 취소/거절 일시
     */
    private LocalDateTime cancelledAt;
    
    /**
     * 취소/거절 사유
     */
    private String reason;
    
    /**
     * 수정 일시
     */
    private LocalDateTime updatedAt;
    
    /**
     * Enrollment 엔티티로부터 EnrollmentResponseDto를 생성합니다
     * 
     * @param enrollment Enrollment 엔티티
     * @return EnrollmentResponseDto
     */
    public static EnrollmentResponseDto from(Enrollment enrollment) {
        EnrollmentResponseDto dto = new EnrollmentResponseDto();
        dto.id = enrollment.getId();
        dto.student = StudentInfo.from(enrollment.getStudent());
        dto.course = CourseInfo.from(enrollment.getCourse());
        dto.status = enrollment.getStatus();
        dto.statusDescription = enrollment.getStatus().getDescription();
        dto.appliedAt = enrollment.getAppliedAt();
        dto.approvedAt = enrollment.getApprovedAt();
        dto.cancelledAt = enrollment.getCancelledAt();
        dto.reason = enrollment.getReason();
        dto.updatedAt = enrollment.getUpdatedAt();
        return dto;
    }
    
    /**
     * 학생 정보를 나타내는 내부 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class StudentInfo {
        private Long id;
        private String name;
        private String email;
        
        /**
         * Member 엔티티로부터 StudentInfo를 생성합니다
         * 
         * @param member Member 엔티티
         * @return StudentInfo
         */
        public static StudentInfo from(com.wb.edutask.entity.Member member) {
            StudentInfo info = new StudentInfo();
            info.id = member.getId();
            info.name = member.getName();
            info.email = member.getEmail();
            return info;
        }
    }
    
    /**
     * 강의 정보를 나타내는 내부 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CourseInfo {
        private Long id;
        private String courseName;
        private String instructorName;
        
        /**
         * Course 엔티티로부터 CourseInfo를 생성합니다
         * 
         * @param course Course 엔티티
         * @return CourseInfo
         */
        public static CourseInfo from(com.wb.edutask.entity.Course course) {
            CourseInfo info = new CourseInfo();
            info.id = course.getId();
            info.courseName = course.getCourseName();
            info.instructorName = course.getInstructor().getName();
            return info;
        }
    }
}
