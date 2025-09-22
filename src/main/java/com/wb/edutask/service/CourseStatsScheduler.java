package com.wb.edutask.service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wb.edutask.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 강의 통계 동기화 스케줄러
 * Redis의 실시간 수강인원을 DB로 주기적 동기화
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
     * Redis에서 DB로 수강인원 동기화 (1분마다 실행, 분산락 적용)
     * Redis에 있는 강의만 동기화 = 활성 강의만 처리
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    @Transactional
    public void syncCurrentStudentsFromRedis() {
        // 분산락 획득 시도
        String lockValue = UUID.randomUUID().toString();
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
            .setIfAbsent(SYNC_LOCK_KEY, lockValue, Duration.ofSeconds(50)); // 50초 락 유지
        
        if (!Boolean.TRUE.equals(lockAcquired)) {
            log.debug("다른 서버에서 동기화 작업 중입니다. 스킵합니다.");
            return;
        }
        
        log.info("🔒 분산락 획득 성공! Redis → DB 동기화 시작");
        
        try {
            // 실제 동기화 작업 수행
            performSync();
            
        } catch (Exception e) {
            log.error("Redis → DB 동기화 중 오류 발생", e);
        } finally {
            // 안전한 락 해제 (Lua 스크립트 사용)
            releaseLock(lockValue);
        }
    }
    
    /**
     * 실제 동기화 작업을 수행합니다
     */
    private void performSync() {
        // Redis에서 활성 강의 키 조회 (course:* 패턴 - Hash 타입)
        Set<String> activeCourseKeys = stringRedisTemplate.keys("course:*");
        
        if (activeCourseKeys == null || activeCourseKeys.isEmpty()) {
            log.debug("동기화할 활성 강의가 없습니다");
            return;
        }
        
        log.debug("Redis에서 발견된 강의 키: {}", activeCourseKeys);
        
        int syncCount = 0;
        
        for (String key : activeCourseKeys) {
            try {
                // 키에서 강의 ID 추출: "course:123" → 123
                Long courseId = extractCourseIdFromKey(key);
                
                // Redis Hash에서 현재 수강인원 조회
                String currentStudentsStr = (String) stringRedisTemplate.opsForHash().get(key, "currentStudents");
                
                if (currentStudentsStr != null) {
                    int currentStudents = Integer.parseInt(currentStudentsStr);
                    
                    // DB 업데이트
                    int updatedRows = courseRepository.updateCurrentStudents(courseId, currentStudents);
                    
                    if (updatedRows > 0) {
                        syncCount++;
                        log.debug("강의 ID {} 수강인원 동기화: {} → DB", courseId, currentStudents);
                        
                        // Redis 키에 TTL 재설정 (2분 - 개발용)
                        stringRedisTemplate.expire(key, java.time.Duration.ofMinutes(2));
                    }
                }
                
            } catch (Exception e) {
                log.warn("개별 강의 동기화 실패 - Key: {}, Error: {}", key, e.getMessage());
            }
        }
        
        log.info("✅ Redis → DB 동기화 완료: {}개 강의 처리", syncCount);
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
