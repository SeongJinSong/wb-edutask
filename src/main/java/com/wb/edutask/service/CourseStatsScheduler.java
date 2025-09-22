package com.wb.edutask.service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.entity.Course;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 강의 통계 데이터 보정 스케줄러
 * DB 실제 수강인원과 currentStudents 컬럼 간 차이 보정 (동시성 이슈 해결)
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseStatsScheduler {
    
    private final StringRedisTemplate stringRedisTemplate;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    
    // 분산락 키
    private static final String SYNC_LOCK_KEY = "lock:course-stats-sync";
    
    // 락 해제용 Lua 스크립트 (안전한 해제를 위해)
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    
    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    
    /**
     * 강의 통계 데이터 보정 (1분마다 실행, 분산락 적용)
     * DB 실제 수강인원과 currentStudents 컬럼 간 차이 보정
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    @Transactional
    public void correctCurrentStudentsData() {
        // 분산락 획득 시도
        String lockValue = UUID.randomUUID().toString();
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(SYNC_LOCK_KEY, lockValue, Duration.ofSeconds(50)); // 50초 락 유지
        
        if (!Boolean.TRUE.equals(lockAcquired)) {
            log.debug("다른 서버에서 보정 작업 중입니다. 스킵합니다.");
            return;
        }
        
        log.info("🔒 분산락 획득 성공! 강의 통계 데이터 보정 시작");
        
        try {
            // 실제 보정 작업 수행
            performCorrection();
            
        } catch (Exception e) {
            log.error("강의 통계 보정 중 오류 발생", e);
        } finally {
            // 안전한 락 해제 (Lua 스크립트 사용)
            releaseLock(lockValue);
        }
    }
    
    /**
     * 실제 보정 작업을 수행합니다
     * DB의 실제 수강인원과 currentStudents 컬럼을 비교하여 차이가 있으면 보정
     */
    private void performCorrection() {
        // Redis에서 활성 강의 키 조회 (course:숫자 패턴만)
        Set<String> allCourseKeys = stringRedisTemplate.keys("course:*");
        
        if (allCourseKeys == null || allCourseKeys.isEmpty()) {
            log.debug("Redis에 활성 강의가 없습니다. 보정 불필요");
            return;
        }
        
        // ZSet 키 제외하고 강의 키만 필터링 (course:숫자 형태만)
        Set<String> activeCourseKeys = allCourseKeys.stream()
            .filter(key -> key.matches("course:\\d+"))
            .collect(Collectors.toSet());
        
        if (activeCourseKeys.isEmpty()) {
            log.debug("Redis에 활성 강의가 없습니다. 보정 불필요");
            return;
        }
        
        log.debug("Redis 활성 강의 키 (필터링됨): {}", activeCourseKeys);
        
        int correctionCount = 0;
        
        for (String key : activeCourseKeys) {
            try {
                // 키에서 강의 ID 추출: "course:123" → 123
                Long courseId = extractCourseIdFromKey(key);
                
                // DB에서 강의 조회
                Course course = courseRepository.findById(courseId).orElse(null);
                if (course == null) {
                    log.warn("강의를 찾을 수 없습니다 - CourseId: {}", courseId);
                    continue;
                }
                
                // 실제 승인된 수강신청 수 조회
                long actualCount = enrollmentRepository.countActiveEnrollmentsByCourse(courseId);
                int currentStudents = course.getCurrentStudents();
                
                // 차이가 있으면 보정
                if (actualCount != currentStudents) {
                    course.setCurrentStudents((int) actualCount);
                    courseRepository.save(course);
                    correctionCount++;
                    
                    log.warn("강의 통계 보정 - CourseId: {}, CourseName: '{}', 기존: {}, 실제: {}", 
                            courseId, course.getCourseName(), currentStudents, actualCount);
                    
                    // Redis Hash도 보정
                    stringRedisTemplate.opsForHash().put(key, "currentStudents", String.valueOf(actualCount));
                    
                    // Redis 키 TTL 갱신 (2분)
                    stringRedisTemplate.expire(key, Duration.ofMinutes(2));
                }
                
            } catch (Exception e) {
                log.warn("강의 키 {} 보정 실패: {}", key, e.getMessage());
            }
        }
        
        if (correctionCount > 0) {
            log.info("✅ 강의 통계 보정 완료: {}개 활성 강의 보정됨", correctionCount);
        } else {
            log.debug("모든 활성 강의 통계가 정확합니다. 보정 불필요");
        }
    }
    
    /**
     * 분산락을 안전하게 해제합니다 (Lua 스크립트 사용)
     * 
     * @param lockValue 락 획득 시 사용한 값
     */
    private void releaseLock(String lockValue) {
        try {
            Long result = stringRedisTemplate.execute(unlockScript, 
                java.util.Collections.singletonList(SYNC_LOCK_KEY), lockValue);
            
            if (result != null && result == 1) {
                log.debug("🔓 분산락 해제 성공");
            } else {
                log.warn("분산락 해제 실패 - 이미 만료되었거나 다른 서버가 해제함");
            }
        } catch (Exception e) {
            log.error("분산락 해제 중 오류 발생", e);
        }
    }
    
    /**
     * Redis 키에서 강의 ID를 추출합니다
     * 
     * @param key Redis 키 (예: "course:123")
     * @return 강의 ID
     */
    private Long extractCourseIdFromKey(String key) {
        // "course:123" → ["course", "123"]
        String[] parts = key.split(":");
        
        if (parts.length >= 2) {
            return Long.parseLong(parts[1]);
        }
        
        throw new IllegalArgumentException("잘못된 Redis 키 형식: " + key);
    }
    
}
