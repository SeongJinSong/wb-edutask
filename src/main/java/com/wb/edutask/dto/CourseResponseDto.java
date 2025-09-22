package com.wb.edutask.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.wb.edutask.entity.Course;
import com.wb.edutask.enums.CourseStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 강의 응답을 위한 DTO
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Getter
@Setter
@NoArgsConstructor
public class CourseResponseDto {
    
    /**
     * 강의 ID
     */
    private Long id;
    
    /**
     * 강의명
     */
    private String courseName;
    
    /**
     * 강의 설명
     */
    private String description;
    
    /**
     * 강사 정보
     */
    private InstructorInfo instructor;
    
    /**
     * 수강 정원
     */
    private Integer maxStudents;
    
    /**
     * 현재 수강인원
     */
    private Integer currentEnrollments;
    
    
    /**
     * 강의 시작일
     */
    private LocalDate startDate;
    
    /**
     * 강의 종료일
     */
    private LocalDate endDate;
    
    /**
     * 강의 상태
     */
    private CourseStatus status;
    
    /**
     * 강의 상태 설명
     */
    private String statusDescription;
    
    /**
     * 수강 신청 가능 여부
     */
    private boolean canEnroll;
    
    /**
     * 생성 일시
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 일시
     */
    private LocalDateTime updatedAt;
    
    /**
     * Course 엔티티로부터 CourseResponseDto를 생성합니다
     * 
     * @param course Course 엔티티
     * @return CourseResponseDto
     */
    public static CourseResponseDto from(Course course) {
        CourseResponseDto dto = new CourseResponseDto();
        dto.id = course.getId();
        dto.courseName = course.getCourseName();
        dto.description = course.getDescription();
        dto.instructor = InstructorInfo.from(course.getInstructor());
        dto.maxStudents = course.getMaxStudents();
        dto.currentEnrollments = 0; // 기본값, 서비스에서 별도 설정
        dto.startDate = course.getStartDate();
        dto.endDate = course.getEndDate();
        dto.status = course.getStatus();
        dto.statusDescription = course.getStatus().getDescription();
        dto.canEnroll = course.canEnroll();
        dto.createdAt = course.getCreatedAt();
        dto.updatedAt = course.getUpdatedAt();
        return dto;
    }
    
    /**
     * Course 엔티티와 현재 수강인원으로부터 CourseResponseDto를 생성합니다
     * 
     * @param course Course 엔티티
     * @param currentEnrollments 현재 수강인원
     * @return CourseResponseDto
     */
    public static CourseResponseDto from(Course course, Integer currentEnrollments) {
        CourseResponseDto dto = from(course);
        dto.currentEnrollments = currentEnrollments;
        return dto;
    }
    
    /**
     * 강사 정보를 나타내는 내부 클래스
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class InstructorInfo {
        private Long id;
        private String name;
        private String email;
        
        /**
         * Member 엔티티로부터 InstructorInfo를 생성합니다
         * 
         * @param member Member 엔티티
         * @return InstructorInfo
         */
        public static InstructorInfo from(com.wb.edutask.entity.Member member) {
            InstructorInfo info = new InstructorInfo();
            info.id = member.getId();
            info.name = member.getName();
            info.email = member.getEmail();
            return info;
        }
    }
}
