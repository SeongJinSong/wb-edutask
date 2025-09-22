package com.wb.edutask.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Enrollment;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.CourseStatus;
import com.wb.edutask.enums.EnrollmentStatus;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 애플리케이션 시작 시 초기 데이터를 생성하는 컴포넌트
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 기존 데이터가 있으면 초기화하지 않음
        if (memberRepository.count() > 0) {
            log.info("기존 데이터가 존재하여 초기 데이터 생성을 건너뜁니다.");
            return;
        }
        
        log.info("초기 데이터 생성을 시작합니다...");
        
        // 1. 회원 데이터 생성
        createMembers();
        
        // 2. 강의 데이터 생성
        createCourses();
        
        // 3. 수강신청 데이터 생성
        createEnrollments();
        
        log.info("초기 데이터 생성이 완료되었습니다.");
    }
    
    /**
     * 회원 데이터 생성 (학생 30명, 강사 20명)
     */
    private void createMembers() {
        List<Member> members = new ArrayList<>();
        
        // 학생 30명 생성
        String[] studentNames = {
            "김민수", "이지은", "박준호", "최서연", "정우진", "한소희", "조민재", "윤아영", "임태현", "송하늘",
            "강지훈", "오수빈", "신동욱", "배유진", "홍성민", "노예린", "구자현", "서미래", "양준석", "문채원",
            "장시우", "권나연", "유재혁", "김다은", "이현우", "박소영", "최민호", "정예나", "한지원", "조성훈"
        };
        
        for (int i = 0; i < studentNames.length; i++) {
            members.add(Member.builder()
                .name(studentNames[i])
                .email(String.format("student%d@wb.com", i + 1))
                .phoneNumber(String.format("010-%04d-%04d", 1000 + i, 5678))
                .password("Pass123")
                .memberType(MemberType.STUDENT)
                .build());
        }
        
        // 강사 20명 생성
        String[] instructorNames = {
            "니나위", "자음과모음", "너바나", "양파링", "제주바다", "김강사", "이교수", "박선생", "최멘토", "정코치",
            "한튜터", "조마스터", "윤전문가", "임컨설턴트", "송리더", "강가이드", "오스승", "신마에스트로", "배프로", "홍엑스퍼트",
        };
        
        for (int i = 0; i < instructorNames.length; i++) {
            members.add(Member.builder()
                .name(instructorNames[i])
                .email(String.format("instructor%d@wb.com", i + 1))
                .phoneNumber(String.format("010-%04d-%04d", 2000 + i, 1234))
                .password("Inst123")
                .memberType(MemberType.INSTRUCTOR)
                .build());
        }
        
        memberRepository.saveAll(members);
        log.info("회원 데이터 {}개가 생성되었습니다. (학생: {}명, 강사: {}명)", 
                members.size(), studentNames.length, instructorNames.length);
    }
    
    /**
     * 강의 데이터 생성 (50개)
     */
    private void createCourses() {
        // 강사 조회
        List<Member> instructors = memberRepository.findByMemberType(MemberType.INSTRUCTOR);
        
        if (instructors.isEmpty()) {
            log.warn("강사가 없어서 강의 데이터를 생성할 수 없습니다.");
            return;
        }
        
        List<Course> courses = new ArrayList<>();
        
        // 강의 카테고리별 템플릿
        String[][] courseTemplates = {
            {"부동산 기초", "부동산 투자의 기초부터 실전까지 배우는 강의입니다."},
            {"신도시 투자", "신도시 개발과 투자 전략을 배우는 강의입니다."},
            {"경매 투자", "부동산 경매와 공매에 대해 배우는 강의입니다."},
            {"재개발 투자", "재개발/재건축 투자 전략을 배우는 강의입니다."},
            {"상가 투자", "상가 부동산 투자의 노하우를 배우는 강의입니다."},
            {"오피스텔 투자", "오피스텔 투자 전략과 관리법을 배우는 강의입니다."},
            {"아파트 투자", "아파트 투자의 핵심 포인트를 배우는 강의입니다."},
            {"토지 투자", "토지 투자와 개발 전략을 배우는 강의입니다."},
            {"해외 부동산", "해외 부동산 투자 방법을 배우는 강의입니다."},
            {"부동산 세무", "부동산 관련 세무 지식을 배우는 강의입니다."}
        };
        
        String[] levels = {"기초반", "중급반", "고급반", "실전반", "마스터반"};
        
        Random random = new Random();
        
        // 50개 강의 생성
        for (int i = 0; i < 50; i++) {
            String[] template = courseTemplates[i % courseTemplates.length];
            String level = levels[random.nextInt(levels.length)];
            Member instructor = instructors.get(i % instructors.size());
            
            // 시작일을 랜덤하게 설정 (1일 ~ 60일 후)
            int startDaysLater = random.nextInt(60) + 1;
            LocalDate startDate = LocalDate.now().plusDays(startDaysLater);
            LocalDate endDate = startDate.plusDays(30 + random.nextInt(30)); // 30~60일 과정
            
            // 정원을 랜덤하게 설정 (5~20명)
            int maxStudents = 5 + random.nextInt(16);
            
            courses.add(Course.builder()
                .courseName(String.format("%s %s (%s)", template[0], level, instructor.getName()))
                .description(template[1])
                .instructor(instructor)
                .maxStudents(maxStudents)
                .startDate(startDate)
                .endDate(endDate)
                .status(CourseStatus.SCHEDULED)
                .build());
        }
        
        courseRepository.saveAll(courses);
        log.info("강의 데이터 {}개가 생성되었습니다.", courses.size());
    }
    
    /**
     * 수강신청 데이터 생성
     * 일부 학생들이 여러 강의에 수강신청한 상황을 시뮬레이션
     */
    private void createEnrollments() {
        List<Member> students = memberRepository.findByMemberType(MemberType.STUDENT);
        List<Course> courses = courseRepository.findAll();
        
        if (students.isEmpty() || courses.isEmpty()) {
            log.warn("학생 또는 강의 데이터가 없어서 수강신청 데이터를 생성할 수 없습니다.");
            return;
        }
        
        List<Enrollment> enrollments = new ArrayList<>();
        Random random = new Random();
        
        // 각 강의마다 랜덤하게 수강신청자 생성 (정원의 30-80% 정도)
        for (Course course : courses) {
            int maxEnrollments = Math.max(1, (int) (course.getMaxStudents() * (0.3 + random.nextDouble() * 0.5)));
            
            // 중복 수강신청 방지를 위한 Set 대신 List 사용 (간단한 중복 체크)
            List<Long> enrolledStudentIds = new ArrayList<>();
            
            for (int i = 0; i < maxEnrollments && enrolledStudentIds.size() < students.size(); i++) {
                Member student;
                Long studentId;
                
                // 중복되지 않는 학생 선택
                do {
                    student = students.get(random.nextInt(students.size()));
                    studentId = student.getId();
                } while (enrolledStudentIds.contains(studentId));
                
                enrolledStudentIds.add(studentId);
                
                // 수강신청 상태 랜덤 결정 (90% 승인, 10% 신청중)
                EnrollmentStatus status = random.nextDouble() < 0.9 ? 
                    EnrollmentStatus.APPROVED : EnrollmentStatus.APPLIED;
                
                LocalDateTime appliedAt = LocalDateTime.now().minusDays(random.nextInt(30));
                LocalDateTime approvedAt = status == EnrollmentStatus.APPROVED ? 
                    appliedAt.plusHours(random.nextInt(48)) : null;
                
                Enrollment enrollment = Enrollment.builder()
                    .student(student)
                    .course(course)
                    .status(status)
                    .appliedAt(appliedAt)
                    .approvedAt(approvedAt)
                    .build();
                
                enrollments.add(enrollment);
            }
        }
        
        enrollmentRepository.saveAll(enrollments);
        log.info("수강신청 데이터 {}개가 생성되었습니다.", enrollments.size());
        
        // 통계 정보 출력
        long approvedCount = enrollments.stream()
            .mapToLong(e -> e.getStatus() == EnrollmentStatus.APPROVED ? 1 : 0)
            .sum();
        long appliedCount = enrollments.size() - approvedCount;
        
        log.info("수강신청 통계 - 승인: {}건, 신청중: {}건", approvedCount, appliedCount);
    }
}
