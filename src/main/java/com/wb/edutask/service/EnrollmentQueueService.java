package com.wb.edutask.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wb.edutask.entity.Course;
import com.wb.edutask.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 수강신청 큐 서비스
 * Lua 스크립트를 사용한 원자적 처리로 동시성 제어
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@Service
public class EnrollmentQueueService {

    private static final String RESULT_KEY_PREFIX = "enrollment:result:";
    private static final String COURSE_KEY_PREFIX = "course:";
    private static final int QUEUE_TIMEOUT_HOURS = 24;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CourseRepository courseRepository;
    
    // Lua Script: 정원 확인 + 수강생 수 증가 (원자적 처리)
    private static final String ENROLLMENT_SCRIPT = """
        local courseKey = KEYS[1]
        local studentId = ARGV[1]
        local courseId = ARGV[2]
        
        -- 현재 수강생 수와 최대 정원 조회
        local currentStudents = redis.call('HGET', courseKey, 'currentStudents')
        local maxStudents = redis.call('HGET', courseKey, 'maxStudents')
        
        if currentStudents == false or maxStudents == false then
            return {0, 'COURSE_NOT_FOUND', 0}
        end
        
        currentStudents = tonumber(currentStudents)
        maxStudents = tonumber(maxStudents)
        
        -- 정원 초과 확인
        if currentStudents >= maxStudents then
            return {0, 'CAPACITY_EXCEEDED', currentStudents}
        end
        
        -- 수강생 수 증가 (원자적)
        local newCount = redis.call('HINCRBY', courseKey, 'currentStudents', 1)
        
        -- 성공 시 수강생 ID 추가
        redis.call('HSET', courseKey, 'student_' .. studentId, '1')
        
        return {1, 'SUCCESS', newCount}
        """;
    
    private final DefaultRedisScript<List> enrollmentScript;
    
    public EnrollmentQueueService() {
        this.enrollmentScript = new DefaultRedisScript<>();
        this.enrollmentScript.setScriptText(ENROLLMENT_SCRIPT);
        this.enrollmentScript.setResultType(List.class);
    }

    /**
     * Lua Script를 사용하여 원자적으로 수강신청을 처리합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @return 큐 ID
     */
    public String enqueueEnrollmentRequest(Long studentId, Long courseId) {
        String queueId = UUID.randomUUID().toString();
        String courseKey = COURSE_KEY_PREFIX + courseId;
        
        try {
            // 강의 정보를 Redis에 동기화 (필요시)
            syncCourseToRedisIfNeeded(courseId);
            
            // Lua Script 실행 (원자적 처리)
            List<Object> result = stringRedisTemplate.execute(
                enrollmentScript,
                Collections.singletonList(courseKey),
                studentId.toString(),
                courseId.toString()
            );
            
            if (result != null && result.size() >= 3) {
                Long success = (Long) result.get(0);
                String message = (String) result.get(1);
                Long newCount = (Long) result.get(2);
                
                // 결과 저장
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("queueId", queueId);
                resultData.put("studentId", studentId);
                resultData.put("courseId", courseId);
                resultData.put("requestedAt", LocalDateTime.now().toString());
                resultData.put("processedAt", LocalDateTime.now().toString());
                resultData.put("success", success == 1);
                resultData.put("message", message);
                resultData.put("newStudentCount", newCount);
                resultData.put("status", success == 1 ? "SUCCESS" : "FAILED");
                
                String resultKey = RESULT_KEY_PREFIX + queueId;
                String json = objectMapper.writeValueAsString(resultData);
                stringRedisTemplate.opsForValue().set(resultKey, json, QUEUE_TIMEOUT_HOURS, TimeUnit.HOURS);
                
                log.info("수강신청 처리 완료 - QueueId: {}, StudentId: {}, CourseId: {}, Success: {}, Message: {}", 
                        queueId, studentId, courseId, success == 1, message);
                
                return queueId;
            } else {
                throw new RuntimeException("Lua Script 실행 결과가 예상과 다릅니다");
            }
            
        } catch (Exception e) {
            log.error("수강신청 처리 실패: {}", e.getMessage(), e);
            
            // 실패 결과 저장
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("queueId", queueId);
            resultData.put("studentId", studentId);
            resultData.put("courseId", courseId);
            resultData.put("requestedAt", LocalDateTime.now().toString());
            resultData.put("processedAt", LocalDateTime.now().toString());
            resultData.put("success", false);
            resultData.put("message", "처리 중 오류 발생: " + e.getMessage());
            resultData.put("status", "ERROR");
            
            try {
                String resultKey = RESULT_KEY_PREFIX + queueId;
                String json = objectMapper.writeValueAsString(resultData);
                stringRedisTemplate.opsForValue().set(resultKey, json, QUEUE_TIMEOUT_HOURS, TimeUnit.HOURS);
            } catch (Exception ex) {
                log.error("실패 결과 저장 실패: {}", ex.getMessage(), ex);
            }
            
            return queueId;
        }
    }
    
    /**
     * 강의 정보를 Redis에 동기화합니다 (필요시)
     */
    private void syncCourseToRedisIfNeeded(Long courseId) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return;
            }
            
            String courseKey = COURSE_KEY_PREFIX + courseId;
            
            // 강의 정보가 Redis에 없으면 동기화
            if (!stringRedisTemplate.hasKey(courseKey)) {
                Map<String, String> courseData = new HashMap<>();
                courseData.put("courseId", courseId.toString());
                courseData.put("courseName", course.getCourseName());
                courseData.put("currentStudents", course.getCurrentStudents().toString());
                courseData.put("maxStudents", course.getMaxStudents().toString());
                courseData.put("instructorId", course.getInstructor().getId().toString());
                
                stringRedisTemplate.opsForHash().putAll(courseKey, courseData);
                log.debug("강의 정보 Redis 동기화 완료: {}", courseId);
            }
        } catch (Exception e) {
            log.warn("강의 정보 Redis 동기화 실패: {}", e.getMessage());
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
        return status == null ? "UNKNOWN" : status.toString();
    }

    /**
     * 수강신청 결과를 조회합니다
     * 
     * @param queueId 큐 ID
     * @return 처리 결과
     */
    public Object getEnrollmentResult(String queueId) {
        return readResult(queueId);
    }

    /**
     * 전체 큐 크기를 조회합니다 (모든 강의 큐 합계)
     * 
     * @return 전체 큐 크기
     */
    public long getTotalQueueSize() {
        try {
            // Redis에서 모든 enrollment:result:* 키의 개수 조회
            return stringRedisTemplate.keys(RESULT_KEY_PREFIX + "*").size();
        } catch (Exception e) {
            log.error("전체 큐 크기 조회 실패: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 강의별 큐 상태를 조회합니다
     * 
     * @param courseId 강의 ID
     * @return 큐 상태 정보
     */
    public Map<String, Object> getCourseQueueStatus(Long courseId) {
        Map<String, Object> status = new HashMap<>();
        status.put("courseId", courseId);
        
        try {
            String courseKey = COURSE_KEY_PREFIX + courseId;
            
            // 강의 정보 조회
            Map<Object, Object> courseData = stringRedisTemplate.opsForHash().entries(courseKey);
            if (courseData.isEmpty()) {
                status.put("queueSize", 0);
                status.put("isProcessing", false);
                status.put("currentStudents", 0);
                status.put("maxStudents", 0);
            } else {
                status.put("queueSize", 0); // Lua Script는 즉시 처리하므로 대기 큐는 0
                status.put("isProcessing", false);
                status.put("currentStudents", courseData.get("currentStudents"));
                status.put("maxStudents", courseData.get("maxStudents"));
            }
            
            status.put("queueKey", courseKey);
        } catch (Exception e) {
            log.error("강의별 큐 상태 조회 실패: {}", e.getMessage(), e);
            status.put("queueSize", 0);
            status.put("isProcessing", false);
            status.put("queueKey", COURSE_KEY_PREFIX + courseId);
        }
        
        return status;
    }
    
    /**
     * (호환용) 전체 큐 크기 반환
     */
    public long getQueueSize() {
        return getTotalQueueSize();
    }

    /**
     * Redis에서 결과 데이터를 읽어옵니다
     */
    private Map<String, Object> readResult(String queueId) {
        try {
            String key = RESULT_KEY_PREFIX + queueId;
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("결과 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
}


