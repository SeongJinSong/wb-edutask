package com.wb.edutask.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 동시성 테스트
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    private Course limitedCourse;
    private List<Member> students;
    
    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
        
        // 강사 생성
        Member instructor = new Member(
            "김강사", 
            "instructor@test.com", 
            "010-1234-5678", 
            "Pass123", 
            MemberType.INSTRUCTOR
        );
        instructor = memberRepository.save(instructor);
        
        // 정원이 제한된 인기 강의 생성 (정원 5명)
        limitedCourse = new Course(
            "인기 강의", 
            "선착순 5명만 수강 가능한 인기 강의", 
            instructor, 
            5, // 정원 5명
            LocalDate.now().plusDays(7), 
            LocalDate.now().plusDays(30)
        );
        limitedCourse = courseRepository.save(limitedCourse);
        
        // 학생 10명 생성 (정원보다 많음)
        students = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Member student = new Member(
                "학생" + i, 
                "student" + i + "@test.com", 
                String.format("010-1111-%04d", 1000 + i), 
                "Pass12" + i, 
                MemberType.STUDENT
            );
            students.add(memberRepository.save(student));
        }
    }
    
    @Test
    @DisplayName("동시 수강신청 시 정원 초과 방지 테스트")
    void concurrentEnrollment_ShouldNotExceedCapacity() throws Exception {
        // Given
        final int THREAD_COUNT = 10;
        final int COURSE_CAPACITY = 5;
        
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When - 10명의 학생이 동시에 5명 정원의 강의에 수강신청
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int studentIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    EnrollmentRequestDto requestDto = new EnrollmentRequestDto(
                        students.get(studentIndex).getId(), 
                        limitedCourse.getId()
                    );
                    
                    enrollmentService.enrollCourse(requestDto);
                    successCount.incrementAndGet();
                    log.info("학생{} 수강신청 성공", studentIndex + 1);
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.info("학생{} 수강신청 실패: {}", studentIndex + 1, e.getMessage());
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 모든 스레드 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();
        
        // Then - 정확히 5명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(COURSE_CAPACITY);
        assertThat(failureCount.get()).isEqualTo(THREAD_COUNT - COURSE_CAPACITY);
        
        // 데이터베이스에서 실제 수강신청 수 확인
        long actualEnrollmentCount = enrollmentRepository.count();
        assertThat(actualEnrollmentCount).isEqualTo(COURSE_CAPACITY);
        
        // 강의의 현재 수강생 수 확인
        Course updatedCourse = courseRepository.findById(limitedCourse.getId()).orElseThrow();
        assertThat(updatedCourse.getCurrentStudents()).isEqualTo(COURSE_CAPACITY);
        
        log.info("=== 동시성 테스트 결과 ===");
        log.info("성공: {}명, 실패: {}명", successCount.get(), failureCount.get());
        log.info("실제 DB 수강신청 수: {}명", actualEnrollmentCount);
        log.info("강의 현재 수강생 수: {}명", updatedCourse.getCurrentStudents());
    }
    
    @Test
    @DisplayName("대용량 동시 수강신청 스트레스 테스트")
    void massiveConcurrentEnrollment_StressTest() throws Exception {
        // Given
        final int THREAD_COUNT = 50; // 더 많은 동시 요청
        final int COURSE_CAPACITY = 5;
        
        // 추가 학생 생성
        List<Member> moreStudents = new ArrayList<>(students);
        for (int i = 11; i <= THREAD_COUNT; i++) {
            Member student = new Member(
                "학생" + i, 
                "student" + i + "@test.com", 
                String.format("010-2222-%04d", 2000 + i), 
                "Pass12" + i, 
                MemberType.STUDENT
            );
            moreStudents.add(memberRepository.save(student));
        }
        
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // When - 50명의 학생이 동시에 5명 정원의 강의에 수강신청
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int studentIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    EnrollmentRequestDto requestDto = new EnrollmentRequestDto(
                        moreStudents.get(studentIndex).getId(), 
                        limitedCourse.getId()
                    );
                    
                    enrollmentService.enrollCourse(requestDto);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 모든 스레드 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Then - 정확히 5명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(COURSE_CAPACITY);
        assertThat(failureCount.get()).isEqualTo(THREAD_COUNT - COURSE_CAPACITY);
        
        // 성능 검증 (5초 이내 완료)
        assertThat(executionTime).isLessThan(5000);
        
        log.info("=== 스트레스 테스트 결과 ===");
        log.info("동시 요청: {}개", THREAD_COUNT);
        log.info("성공: {}명, 실패: {}명", successCount.get(), failureCount.get());
        log.info("실행 시간: {}ms", executionTime);
        log.info("평균 처리 시간: {}ms/request", executionTime / (double) THREAD_COUNT);
    }
}
