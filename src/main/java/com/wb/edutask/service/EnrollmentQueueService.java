package com.wb.edutask.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 수강신청 큐 서비스
 * H2DB에 저장할 request를 큐잉하는 역할만 담당
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentQueueService {

    private static final String RESULT_KEY_PREFIX = "enrollment:result:";
    private static final String QUEUE_KEY = "enrollment:queue";
    private static final int QUEUE_TIMEOUT_HOURS = 24;
    
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 수강신청 요청을 큐에 등록합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @return 큐 ID
     */
    public String enqueueEnrollmentRequest(Long studentId, Long courseId) {
        String queueId = UUID.randomUUID().toString();
        
        try {
            // 큐 데이터 생성
            Map<String, Object> queueData = new HashMap<>();
            queueData.put("queueId", queueId);
            queueData.put("studentId", studentId);
            queueData.put("courseId", courseId);
            queueData.put("type", "ENROLLMENT");
            queueData.put("timestamp", LocalDateTime.now().toString());
            queueData.put("status", "PENDING");
            
            // Redis 큐에 저장
            String queueDataJson = objectMapper.writeValueAsString(queueData);
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, queueDataJson);
            
            // 처리 상태 초기화
            stringRedisTemplate.opsForValue().set(
                RESULT_KEY_PREFIX + queueId, 
                "PENDING", 
                QUEUE_TIMEOUT_HOURS, 
                TimeUnit.HOURS
            );
            
            log.info("수강신청 요청이 큐에 등록되었습니다 - QueueId: {}, StudentId: {}, CourseId: {}", 
                    queueId, studentId, courseId);
            
            return queueId;
        } catch (Exception e) {
            log.error("수강신청 큐 등록 실패 - StudentId: {}, CourseId: {}, Error: {}", 
                    studentId, courseId, e.getMessage(), e);
            throw new RuntimeException("수강신청 요청 등록에 실패했습니다", e);
        }
    }
    
    /**
     * 수강신청 취소 요청을 큐에 등록합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @param reason 취소 사유
     * @return 큐 ID
     */
    public String enqueueCancelRequest(Long studentId, Long courseId, String reason) {
        String queueId = UUID.randomUUID().toString();
        
        try {
            // 큐 데이터 생성
            Map<String, Object> queueData = new HashMap<>();
            queueData.put("queueId", queueId);
            queueData.put("studentId", studentId);
            queueData.put("courseId", courseId);
            queueData.put("type", "CANCELLATION");
            queueData.put("reason", reason);
            queueData.put("timestamp", LocalDateTime.now().toString());
            queueData.put("status", "PENDING");
            
            // Redis 큐에 저장
            String queueDataJson = objectMapper.writeValueAsString(queueData);
            stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, queueDataJson);
            
            // 처리 상태 초기화
            stringRedisTemplate.opsForValue().set(
                RESULT_KEY_PREFIX + queueId, 
                "PENDING", 
                QUEUE_TIMEOUT_HOURS, 
                TimeUnit.HOURS
            );
            
            log.info("수강신청 취소 요청이 큐에 등록되었습니다 - QueueId: {}, StudentId: {}, CourseId: {}", 
                    queueId, studentId, courseId);
            
            return queueId;
        } catch (Exception e) {
            log.error("수강신청 취소 큐 등록 실패 - StudentId: {}, CourseId: {}, Error: {}", 
                    studentId, courseId, e.getMessage(), e);
            throw new RuntimeException("수강신청 취소 요청 등록에 실패했습니다", e);
        }
    }
    
    /**
     * 큐에서 다음 요청을 가져옵니다
     * 
     * @return 큐 데이터 (없으면 null)
     */
    public Map<String, Object> dequeueRequest() {
        try {
            String queueDataJson = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY);
            if (queueDataJson == null) {
                return null;
            }
            
            Map<String, Object> queueData = objectMapper.readValue(
                queueDataJson, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            log.debug("큐에서 요청을 가져왔습니다 - QueueId: {}, Type: {}", 
                    queueData.get("queueId"), queueData.get("type"));
            
            return queueData;
        } catch (Exception e) {
            log.error("큐 데이터 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 처리 결과를 저장합니다
     * 
     * @param queueId 큐 ID
     * @param result 처리 결과
     */
    public void storeResult(String queueId, Map<String, Object> result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(
                RESULT_KEY_PREFIX + queueId, 
                resultJson, 
                QUEUE_TIMEOUT_HOURS, 
                TimeUnit.HOURS
            );
            
            log.debug("처리 결과 저장 완료 - QueueId: {}", queueId);
        } catch (Exception e) {
            log.error("처리 결과 저장 실패 - QueueId: {}, Error: {}", queueId, e.getMessage(), e);
        }
    }
    
    /**
     * 처리 결과를 조회합니다
     * 
     * @param queueId 큐 ID
     * @return 처리 결과 (없으면 null)
     */
    public Map<String, Object> readResult(String queueId) {
        try {
            // 먼저 키 타입 확인
            String keyType = stringRedisTemplate.type(RESULT_KEY_PREFIX + queueId).code();
            
            if ("string".equals(keyType)) {
                // String 타입으로 저장된 경우
                String resultJson = stringRedisTemplate.opsForValue().get(RESULT_KEY_PREFIX + queueId);
                if (resultJson == null || "PENDING".equals(resultJson)) {
                    return null;
                }
                
                return objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
            } else if ("hash".equals(keyType)) {
                // Hash 타입으로 저장된 경우 (이전 버전 호환성)
                Map<Object, Object> hashResult = stringRedisTemplate.opsForHash().entries(RESULT_KEY_PREFIX + queueId);
                if (hashResult.isEmpty()) {
                    return null;
                }
                
                Map<String, Object> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : hashResult.entrySet()) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
                return result;
            } else {
                log.warn("알 수 없는 키 타입: {} - QueueId: {}", keyType, queueId);
                return null;
            }
        } catch (Exception e) {
            log.error("처리 결과 조회 실패 - QueueId: {}, Error: {}", queueId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 수강신청 상태를 조회합니다
     * 
     * @param queueId 큐 ID
     * @return 큐 상태
     */
    public String getEnrollmentStatus(String queueId) {
        Map<String, Object> data = readResult(queueId);
        if (data == null) {
            return "NOT_FOUND";
        }
        
        Object status = data.get("status");
        return status != null ? status.toString() : "UNKNOWN";
    }
    
    /**
     * 큐의 현재 크기를 조회합니다
     * 
     * @return 큐 크기
     */
    public Long getQueueSize() {
        try {
            return stringRedisTemplate.opsForList().size(QUEUE_KEY);
        } catch (Exception e) {
            log.error("큐 크기 조회 실패: {}", e.getMessage());
            return 0L;
        }
    }
    
    /**
     * 큐를 비웁니다 (테스트용)
     */
    public void clearQueue() {
        try {
            stringRedisTemplate.delete(QUEUE_KEY);
            log.info("큐가 비워졌습니다");
        } catch (Exception e) {
            log.error("큐 비우기 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 수강신청 결과를 조회합니다 (호환성 메서드)
     * 
     * @param queueId 큐 ID
     * @return 처리 결과
     */
    public Object getEnrollmentResult(String queueId) {
        return readResult(queueId);
    }
    
    /**
     * 전체 큐 크기를 조회합니다 (호환성 메서드)
     * 
     * @return 큐 크기
     */
    public Long getTotalQueueSize() {
        return getQueueSize();
    }
    
    /**
     * DB 큐 크기를 조회합니다 (호환성 메서드)
     * 
     * @return 큐 크기
     */
    public Long getDbQueueSize() {
        return getQueueSize();
    }
    
    /**
     * 다음 DB 큐 아이템을 조회합니다 (호환성 메서드)
     * 
     * @return 큐 데이터
     */
    public Map<String, Object> getNextDbQueueItem() {
        return dequeueRequest();
    }
    
    /**
     * DB 큐 아이템을 완료 처리합니다 (호환성 메서드)
     * 
     * @param queueId 큐 ID
     */
    public void markDbQueueItemCompleted(String queueId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "COMPLETED");
            result.put("completedAt", java.time.LocalDateTime.now().toString());
            storeResult(queueId, result);
            log.debug("DB 큐 아이템 완료 처리 - QueueId: {}", queueId);
        } catch (Exception e) {
            log.error("DB 큐 아이템 완료 처리 실패 - QueueId: {}, Error: {}", queueId, e.getMessage());
        }
    }
    
    /**
     * 강의별 큐 상태를 조회합니다 (호환성 메서드)
     * 
     * @param courseId 강의 ID
     * @return 큐 상태
     */
    public Map<String, Object> getCourseQueueStatus(Long courseId) {
        Map<String, Object> status = new HashMap<>();
        status.put("courseId", courseId);
        status.put("queueSize", getQueueSize());
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        return status;
    }
}