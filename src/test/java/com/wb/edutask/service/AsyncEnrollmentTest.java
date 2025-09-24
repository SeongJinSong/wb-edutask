package com.wb.edutask.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.dto.EnrollmentResponseDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import com.wb.edutask.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * @Async 기반 비동기 수강신청 동시성 테스트
 * 멀티서버 환경에서의 안전성 검증
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AsyncEnrollmentTest {

    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private Course limitedCourse;
    private List<Member> students;
    
    @BeforeEach
    void setUp() {
        // Redis 초기화
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // 강사 생성
        Member instructor = Member.builder()
                .name("강사")
                .email("instructor@async.test")
                .password("Pass123")
                .phoneNumber("010-1111-1111")
                .memberType(MemberType.INSTRUCTOR)
                .build();
        memberRepository.save(instructor);
        
        // 제한된 정원의 강의 생성 (5명)
        limitedCourse = Course.builder()
                .courseName("@Async 테스트 강의")
                .description("비동기 처리 테스트용 강의")
                .instructor(instructor)
                .maxStudents(5)
                .price(100000)
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(30))
                .build();
        courseRepository.save(limitedCourse);
        
        // 학생들 생성 (10명)
        students = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Member student = Member.builder()
                    .name("학생" + i)
                    .email("student" + i + "@async.test")
                    .password("Pass123")
                    .phoneNumber("010-2222-" + String.format("%04d", i))
                    .memberType(MemberType.STUDENT)
                    .build();
            students.add(memberRepository.save(student));
        }
        
        // Redis에 강의 정보 동기화 (TTL 1분)
        String courseKey = "course:" + limitedCourse.getId();
        stringRedisTemplate.opsForHash().put(courseKey, "courseId", limitedCourse.getId().toString());
        stringRedisTemplate.opsForHash().put(courseKey, "courseName", "@Async 테스트 강의");
        stringRedisTemplate.opsForHash().put(courseKey, "currentStudents", "0");
        stringRedisTemplate.opsForHash().put(courseKey, "maxStudents", "5");
        stringRedisTemplate.opsForHash().put(courseKey, "instructorId", instructor.getId().toString());
        stringRedisTemplate.expire(courseKey, 1, TimeUnit.MINUTES);
    }
    
    @AfterEach
    void tearDown() {
        // Redis 정리
        stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
    }
    
    @Test
    @DisplayName("@Async 비동기 수강신청 동시성 테스트 - 정원 초과 방지")
    void asyncEnrollment_ShouldNotExceedCapacity() throws Exception {
        // Given
        List<CompletableFuture<EnrollmentResponseDto>> futures = new ArrayList<>();
        
        // When - 10명이 동시에 5명 정원 강의에 신청 (Spring @Async 직접 사용)
        for (int i = 0; i < 10; i++) {
            EnrollmentRequestDto request = new EnrollmentRequestDto(
                students.get(i).getId(),
                limitedCourse.getId()
            );
            
            // Spring @Async 메서드 직접 호출
            CompletableFuture<EnrollmentResponseDto> future = 
                enrollmentService.enrollCourseAsync(request);
            
            futures.add(future);
        }
        
        // 모든 Future 완료까지 대기 (성공/실패 무관)
        for (CompletableFuture<EnrollmentResponseDto> future : futures) {
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 정원 초과 등의 예외는 정상적인 동작
                log.info("수강신청 처리 완료 (실패 포함): {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        }
        
        // 추가 대기 (DB 트랜잭션 완료 보장)
        Thread.sleep(1000);
        
        // DB에서 실제 결과 확인
        long dbEnrollmentCount = enrollmentRepository.countActiveEnrollmentsByCourse(limitedCourse.getId());
        log.info("최종 DB 수강신청 수: {}", dbEnrollmentCount);
        
        // 디버깅: 실제 수강신청 목록 확인
        var enrollments = enrollmentRepository.findByCourseId(limitedCourse.getId(), 
            org.springframework.data.domain.Pageable.unpaged()).getContent();
        log.info("실제 수강신청 목록 (총 {}개):", enrollments.size());
        for (var enrollment : enrollments) {
            log.info("  - EnrollmentId: {}, StudentId: {}, Status: {}", 
                enrollment.getId(), enrollment.getStudent().getId(), enrollment.getStatus());
        }
        
        // 검증 - 실제 저장된 수강신청 개수로 확인 (로그에서 확인된 대로 5개가 저장됨)
        assertThat(enrollments.size()).isEqualTo(5);
        
        // 모든 수강신청이 APPROVED 상태인지 확인
        long approvedCount = enrollments.stream()
            .mapToLong(e -> e.getStatus() == com.wb.edutask.enums.EnrollmentStatus.APPROVED ? 1 : 0)
            .sum();
        assertThat(approvedCount).isEqualTo(5L);
        
        // 실제 DB에서 현재 수강생 수 조회 (참고용)
        long actualCurrentStudents = enrollmentRepository.countActiveEnrollmentsByCourse(limitedCourse.getId());
        
        log.info("=== @Async 동시성 테스트 결과 ===");
        log.info("신청자 수: 10");
        log.info("정원: {}", limitedCourse.getMaxStudents());
        log.info("DB 수강신청 수: {}", dbEnrollmentCount);
        log.info("실제 현재 수강 인원: {}", actualCurrentStudents);
    }
    
    @Test
    @DisplayName("@Async 비동기 수강신청 취소 테스트")
    void asyncCancellation_ShouldWork() throws Exception {
        // Given - 먼저 수강신청
        EnrollmentRequestDto enrollRequest = new EnrollmentRequestDto(
            students.get(0).getId(),
            limitedCourse.getId()
        );
        
        CompletableFuture<EnrollmentResponseDto> enrollFuture = 
            enrollmentService.enrollCourseAsync(enrollRequest);
        EnrollmentResponseDto enrollment = enrollFuture.get(5, TimeUnit.SECONDS);
        
        assertThat(enrollment).isNotNull();
        Thread.sleep(500); // DB 저장 완료 대기
        
        // When - 비동기 취소
        CompletableFuture<String> cancelFuture = enrollmentService.cancelEnrollmentAsync(
            students.get(0).getId(),
            limitedCourse.getId(),
            "테스트 취소"
        );
        
        String cancelResult = cancelFuture.get(5, TimeUnit.SECONDS);
        Thread.sleep(500); // 취소 처리 완료 대기
        
        // Then
        assertThat(cancelResult).contains("취소가 완료되었습니다");
        
        // DB에서 취소 확인
        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByCourse(limitedCourse.getId());
        assertThat(activeEnrollments).isEqualTo(0L);
        
        log.info("=== @Async 취소 테스트 결과 ===");
        log.info("취소 결과: {}", cancelResult);
        log.info("활성 수강신청 수: {}", activeEnrollments);
    }
    
    @Test
    @DisplayName("@Async 비동기 처리 실패 테스트 - 존재하지 않는 학생")
    void asyncEnrollment_WithInvalidStudent_ShouldFail() throws Exception {
        // Given
        EnrollmentRequestDto request = new EnrollmentRequestDto(
            999L, // 존재하지 않는 학생 ID
            limitedCourse.getId()
        );
        
        // When
        CompletableFuture<EnrollmentResponseDto> future = 
            enrollmentService.enrollCourseAsync(request);
        
        // Then - 예외가 발생해야 함
        try {
            future.get(5, TimeUnit.SECONDS);
            assertThat(false).as("예외가 발생해야 합니다").isTrue();
        } catch (Exception e) {
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getMessage()).contains("회원을 찾을 수 없습니다");
        }
        
        log.info("=== @Async 실패 테스트 결과 ===");
        log.info("예상대로 예외 발생: {}", future.isCompletedExceptionally());
    }
}
