package com.wb.edutask.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.dto.EnrollmentRequestDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.entity.Member;
import com.wb.edutask.enums.MemberType;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.MemberRepository;

/**
 * Redis Queue를 사용한 동시성 테스트
 * H2 DB 락 문제 해결 검증
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RedisQueueConcurrencyTest {

    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private EnrollmentQueueService enrollmentQueueService;
    
    @Autowired
    private MemberRepository memberRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    private Member instructor;
    private Course limitedCourse;
    private List<Member> students;
    
    @BeforeEach
    void setUp() {
        // 고유한 데이터 생성을 위한 타임스탬프 사용
        long timestamp = System.currentTimeMillis();
        String uniqueId = String.valueOf(timestamp % 100000);
        
        // 강사 생성
        instructor = new Member(
            "이강사" + uniqueId, 
            "instructor" + uniqueId + "@test.com", 
            "010-2222-" + String.format("%04d", Integer.parseInt(uniqueId) % 10000), 
            "Pass456", 
            MemberType.INSTRUCTOR
        );
        instructor = memberRepository.save(instructor);
        
        // 제한된 정원의 강의 생성 (정원 5명)
        limitedCourse = new Course(
            "인기 강의" + uniqueId, 
            "정원이 제한된 인기 강의", 
            instructor, 
            5, // 최대 5명
            java.time.LocalDate.now().plusDays(7), 
            java.time.LocalDate.now().plusDays(37)
        );
        limitedCourse = courseRepository.save(limitedCourse);
        
        // 10명의 학생 생성
        students = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Member student = new Member(
                "학생" + i + "_" + uniqueId, 
                "student" + i + "_" + uniqueId + "@test.com", 
                "010-1111-" + String.format("%04d", (i + 1000) % 10000), 
                "Pass123", 
                MemberType.STUDENT
            );
            student = memberRepository.save(student);
            students.add(student);
        }
    }
    
    @AfterEach
    void tearDown() {
        // Redis 데이터 정리
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.getConnectionFactory().getConnection().flushAll();
            }
        } catch (Exception e) {
            // Redis 연결 실패 시 무시 (테스트 환경에서 Redis가 없을 수 있음)
        }
    }
    
    @Test
    @DisplayName("Redis Queue를 사용한 동시 수강신청 테스트 - H2 락 문제 해결")
    void concurrentEnrollmentWithRedisQueue_ShouldNotExceedCapacity() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<String>> futures = new ArrayList<>();
        
        // When: 10명이 동시에 수강신청 (정원 5명)
        for (int i = 0; i < threadCount; i++) {
            final int studentIndex = i;
            Future<String> future = executor.submit(() -> {
                try {
                    EnrollmentRequestDto request = new EnrollmentRequestDto(
                        students.get(studentIndex).getId(), 
                        limitedCourse.getId()
                    );
                    return enrollmentService.enrollCourseWithQueue(request);
                } catch (Exception e) {
                    return "ERROR: " + e.getMessage();
                }
            });
            futures.add(future);
        }
        
        // 모든 요청이 완료될 때까지 대기
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        
        // Then: Redis Queue에 성공한 요청들이 저장되었는지 확인
        List<String> queueIds = new ArrayList<>();
        for (Future<String> future : futures) {
            String result = future.get();
            if (!result.startsWith("ERROR")) {
                queueIds.add(result);
            }
        }
        
        // Redis Queue 크기 확인 (성공한 요청들)
        long dbQueueSize = enrollmentQueueService.getDbQueueSize();
        System.out.println("Redis DB Queue 크기: " + dbQueueSize);
        System.out.println("성공한 큐 ID 개수: " + queueIds.size());
        
        // Redis에서 성공한 요청들이 5개 이하인지 확인 (정원 제한)
        assertThat(dbQueueSize).isLessThanOrEqualTo(5);
        assertThat(queueIds.size()).isLessThanOrEqualTo(5);
        
        // Redis Queue를 순차적으로 DB에 처리
        int processedCount = enrollmentService.processDbQueue();
        System.out.println("DB 처리된 수강신청 수: " + processedCount);
        
        // DB 처리 후 강의의 현재 수강생 수 확인
        Course updatedCourse = courseRepository.findById(limitedCourse.getId()).orElseThrow();
        System.out.println("강의 현재 수강생 수: " + updatedCourse.getCurrentStudents());
        
        // 정원을 초과하지 않았는지 확인
        assertThat(updatedCourse.getCurrentStudents()).isLessThanOrEqualTo(5);
        assertThat(processedCount).isLessThanOrEqualTo(5);
        
        // Redis Queue가 비어있는지 확인
        long finalQueueSize = enrollmentQueueService.getDbQueueSize();
        assertThat(finalQueueSize).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Redis Queue 상태 조회 테스트")
    void getRedisQueueStatus_ShouldReturnCorrectStatus() {
        // Given
        EnrollmentRequestDto request = new EnrollmentRequestDto(
            students.get(0).getId(), 
            limitedCourse.getId()
        );
        
        // When: 수강신청을 큐에 등록
        String queueId = enrollmentService.enrollCourseWithQueue(request);
        
        // Then: 큐 상태 확인
        String status = enrollmentQueueService.getEnrollmentStatus(queueId);
        assertThat(status).isEqualTo("SUCCESS");
        
        long dbQueueSize = enrollmentQueueService.getDbQueueSize();
        assertThat(dbQueueSize).isEqualTo(1);
        
        long totalQueueSize = enrollmentQueueService.getTotalQueueSize();
        assertThat(totalQueueSize).isEqualTo(1);
    }
}
