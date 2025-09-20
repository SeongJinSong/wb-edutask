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
    private static final String QUEUE_KEY = "enrollment:queue";
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
        
        -- 디버깅을 위한 로그 (Redis에서는 실제로 출력되지 않음)
        if currentStudents == false or maxStudents == false then
            return {0, 'COURSE_NOT_FOUND', 0}
        end
        
        currentStudents = tonumber(currentStudents)
        maxStudents = tonumber(maxStudents)
        
        -- 정원 초과 확인 (>= 대신 > 사용)
        if currentStudents >= maxStudents then
            return {0, 'CAPACITY_EXCEEDED', currentStudents}
        end
        
        -- 수강생 수 증가 (원자적)
        local newCount = redis.call('HINCRBY', courseKey, 'currentStudents', 1)
        
        -- 성공 시 수강생 ID 추가
        redis.call('HSET', courseKey, 'student_' .. studentId, '1')
        
        return {1, 'SUCCESS', newCount}
        """;

    // Lua Script: 수강생 수 감소 (원자적 처리, 정원 확인 없음)
    private static final String CANCELLATION_SCRIPT = """
        local courseKey = KEYS[1]
        local studentId = ARGV[1]
        local courseId = ARGV[2]
        
        -- 현재 수강생 수 조회
        local currentStudents = redis.call('HGET', courseKey, 'currentStudents')
        
        if currentStudents == false then
            return {0, 'COURSE_NOT_FOUND', 0}
        end
        
        currentStudents = tonumber(currentStudents)
        
        -- 수강생 수가 0명 이하인지 확인 (음수 방지)
        if currentStudents <= 0 then
            return {0, 'NO_STUDENTS_TO_DECREASE', currentStudents}
        end
        
        -- 수강생 수 감소 (원자적)
        local newCount = redis.call('HINCRBY', courseKey, 'currentStudents', -1)
        
        -- 취소 시 수강생 ID 제거
        redis.call('HDEL', courseKey, 'student_' .. studentId)
        
        return {1, 'SUCCESS', newCount}
        """;
    
    private final DefaultRedisScript<List> enrollmentScript;
    private final DefaultRedisScript<List> cancellationScript;
    
    public EnrollmentQueueService() {
        this.enrollmentScript = new DefaultRedisScript<>();
        this.enrollmentScript.setScriptText(ENROLLMENT_SCRIPT);
        this.enrollmentScript.setResultType(List.class);
        
        this.cancellationScript = new DefaultRedisScript<>();
        this.cancellationScript.setScriptText(CANCELLATION_SCRIPT);
        this.cancellationScript.setResultType(List.class);
    }

    /**
     * Lua Script를 직접 실행하여 동기적으로 수강신청을 처리합니다 (동시성 제어용)
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @return 처리 결과 (success, message 포함)
     */
    public Map<String, Object> executeEnrollmentLuaScript(Long studentId, Long courseId) {
        String courseKey = COURSE_KEY_PREFIX + courseId;
        
        try {
            // 강의 정보를 Redis에 동기화 (필요시)
            syncCourseToRedisIfNeeded(courseId);
            
            // Lua Script 실행 (원자적 처리) - 최대 3회 재시도
            List<Object> result = null;
            int retryCount = 0;
            int maxRetries = 3;
            
            while (retryCount < maxRetries) {
                result = stringRedisTemplate.execute(
                    enrollmentScript,
                    Collections.singletonList(courseKey),
                    studentId.toString(),
                    courseId.toString()
                );
                
                // 결과 확인
                if (result != null && result.size() >= 3) {
                    Long success = (Long) result.get(0);
                    String message = (String) result.get(1);
                    
                    // COURSE_NOT_FOUND인 경우 재동기화 후 재시도
                    if (success == 0 && "COURSE_NOT_FOUND".equals(message)) {
                        log.warn("Redis에서 강의 정보를 찾을 수 없음 - 재동기화 시도 {}/{}, CourseId: {}", 
                                retryCount + 1, maxRetries, courseId);
                        
                        // 강제로 다시 동기화
                        syncCourseToRedisIfNeeded(courseId);
                        retryCount++;
                        
                        if (retryCount < maxRetries) {
                            Thread.sleep(50); // 50ms 대기 후 재시도
                            continue;
                        }
                    }
                }
                break; // 성공하거나 다른 오류인 경우 루프 종료
            }
            
            if (result != null && result.size() >= 3) {
                Long success = (Long) result.get(0);
                String message = (String) result.get(1);
                Long newCount = (Long) result.get(2);
                
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("success", success == 1);
                resultMap.put("message", message);
                resultMap.put("newStudentCount", newCount);
                resultMap.put("studentId", studentId);
                resultMap.put("courseId", courseId);
                
                log.info("Lua 스크립트 동기 실행 완료 - StudentId: {}, CourseId: {}, Success: {}, Message: {}, NewCount: {}", 
                        studentId, courseId, success == 1, message, newCount);
                
                return resultMap;
            } else {
                throw new RuntimeException("Lua Script 실행 결과가 예상과 다릅니다");
            }
            
        } catch (Exception e) {
            log.error("Lua 스크립트 동기 실행 실패: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "SCRIPT_EXECUTION_ERROR");
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    /**
     * Lua Script를 사용하여 원자적으로 수강신청을 처리합니다 (기존 큐 방식)
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
            
            // Redis에 강의 정보가 제대로 동기화되었는지 확인
            log.debug("Redis 강의 정보 확인 - CourseKey: {}, CurrentStudents: {}, MaxStudents: {}", 
                    courseKey, 
                    stringRedisTemplate.opsForHash().get(courseKey, "currentStudents"),
                    stringRedisTemplate.opsForHash().get(courseKey, "maxStudents"));
            
            // Lua Script 실행 (원자적 처리) - 최대 3회 재시도
            List<Object> result = null;
            int retryCount = 0;
            int maxRetries = 3;
            
            while (retryCount < maxRetries) {
                result = stringRedisTemplate.execute(
                    enrollmentScript,
                    Collections.singletonList(courseKey),
                    studentId.toString(),
                    courseId.toString()
                );
                
                // 결과 확인
                if (result != null && result.size() >= 3) {
                    Long success = (Long) result.get(0);
                    String message = (String) result.get(1);
                    
                    // COURSE_NOT_FOUND인 경우 재동기화 후 재시도
                    if (success == 0 && "COURSE_NOT_FOUND".equals(message)) {
                        log.warn("Redis에서 강의 정보를 찾을 수 없음 - 재동기화 시도 {}/{}, CourseId: {}", 
                                retryCount + 1, maxRetries, courseId);
                        
                        // 강제로 다시 동기화
                        syncCourseToRedisIfNeeded(courseId);
                        retryCount++;
                        
                        if (retryCount < maxRetries) {
                            Thread.sleep(50); // 50ms 대기 후 재시도
                            continue;
                        }
                    }
                }
                break; // 성공하거나 다른 오류인 경우 루프 종료
            }
            
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
                
                // Lua Script에서 성공한 경우에만 Redis Queue에 추가 (순차 DB 처리용)
                if (success == 1) {
                    Map<String, Object> queueData = new HashMap<>();
                    queueData.put("queueId", queueId);
                    queueData.put("studentId", studentId);
                    queueData.put("courseId", courseId);
                    queueData.put("queuedAt", LocalDateTime.now().toString());
                    queueData.put("status", "PENDING_DB_INSERT");
                    
                    String queueJson = objectMapper.writeValueAsString(queueData);
                    stringRedisTemplate.opsForList().rightPush(QUEUE_KEY, queueJson);
                    
                    log.info("수강신청이 DB 처리 큐에 추가되었습니다 - QueueId: {}, StudentId: {}, CourseId: {}", 
                            queueId, studentId, courseId);
                }
                
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
     * Redis에서 강의 수강생 수를 감소시킵니다 (롤백용)
     * 
     * @param courseId 강의 ID
     */
    public void decreaseCourseStudents(Long courseId) {
        String courseKey = COURSE_KEY_PREFIX + courseId;
        
        try {
            // 현재 수강생 수를 1 감소 (0 이하로는 내려가지 않도록)
            String currentStudentsStr = (String) stringRedisTemplate.opsForHash().get(courseKey, "currentStudents");
            if (currentStudentsStr != null) {
                int currentStudents = Integer.parseInt(currentStudentsStr);
                if (currentStudents > 0) {
                    stringRedisTemplate.opsForHash().put(courseKey, "currentStudents", String.valueOf(currentStudents - 1));
                    log.info("Redis 수강생 수 감소 - CourseId: {}, 변경 후: {}", courseId, currentStudents - 1);
                } else {
                    log.warn("Redis 수강생 수가 이미 0입니다 - CourseId: {}", courseId);
                }
            } else {
                log.warn("Redis에서 강의 정보를 찾을 수 없습니다 - CourseId: {}", courseId);
            }
        } catch (Exception e) {
            log.error("Redis 수강생 수 감소 실패 - CourseId: {}, Error: {}", courseId, e.getMessage());
        }
    }

    /**
     * 강의 정보를 Redis에 동기화합니다 (필요시)
     */
    public void syncCourseToRedisIfNeeded(Long courseId) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return;
            }
            
            String courseKey = COURSE_KEY_PREFIX + courseId;
            
            // Redis에 강의 정보가 없을 때만 동기화 (동시성 문제 방지)
            String existingCurrentStudents = (String) stringRedisTemplate.opsForHash().get(courseKey, "currentStudents");
            
            if (existingCurrentStudents == null) {
                // Redis에 데이터가 없을 때만 DB에서 동기화
                Map<String, String> courseData = new HashMap<>();
                courseData.put("courseId", courseId.toString());
                courseData.put("courseName", course.getCourseName());
                courseData.put("currentStudents", course.getCurrentStudents().toString());
                courseData.put("maxStudents", course.getMaxStudents().toString());
                courseData.put("instructorId", course.getInstructor().getId().toString());
                
                stringRedisTemplate.opsForHash().putAll(courseKey, courseData);
                log.debug("강의 정보 Redis 동기화 완료: {} (currentStudents: {}, maxStudents: {})", 
                        courseId, course.getCurrentStudents(), course.getMaxStudents());
            } else {
                log.debug("Redis에 강의 정보가 이미 존재함 - CourseId: {}, CurrentStudents: {}", 
                        courseId, existingCurrentStudents);
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
     * DB 처리 대기 중인 큐 크기를 조회합니다
     * 
     * @return DB 처리 대기 큐 크기
     */
    public long getDbQueueSize() {
        try {
            return stringRedisTemplate.opsForList().size(QUEUE_KEY);
        } catch (Exception e) {
            log.error("DB 큐 크기 조회 실패: {}", e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * Redis Queue에서 다음 DB 처리 항목을 가져옵니다 (순차 처리용)
     * 
     * @return 큐 항목 데이터 (없으면 null)
     */
    public Map<String, Object> getNextDbQueueItem() {
        try {
            String queueItem = stringRedisTemplate.opsForList().leftPop(QUEUE_KEY);
            if (queueItem == null) {
                return null;
            }
            
            return objectMapper.readValue(queueItem, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("DB 큐 항목 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * DB 처리 완료된 항목을 큐에서 제거합니다
     * 
     * @param queueId 큐 ID
     */
    public void markDbQueueItemCompleted(String queueId) {
        try {
            // 결과 상태를 DB_COMPLETED로 업데이트
            String resultKey = RESULT_KEY_PREFIX + queueId;
            String json = stringRedisTemplate.opsForValue().get(resultKey);
            if (json != null) {
                Map<String, Object> resultData = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                resultData.put("status", "DB_COMPLETED");
                resultData.put("dbCompletedAt", LocalDateTime.now().toString());
                
                String updatedJson = objectMapper.writeValueAsString(resultData);
                stringRedisTemplate.opsForValue().set(resultKey, updatedJson, QUEUE_TIMEOUT_HOURS, TimeUnit.HOURS);
                
                log.info("DB 처리 완료 표시 - QueueId: {}", queueId);
            }
        } catch (Exception e) {
            log.error("DB 큐 항목 완료 표시 실패 - QueueId: {}, Error: {}", queueId, e.getMessage(), e);
        }
    }

    /**
     * 취소 요청을 Redis Queue에 등록합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @param reason 취소 사유
     * @return 큐 ID
     */
    public String enqueueCancelRequest(Long studentId, Long courseId, String reason) {
        String queueId = UUID.randomUUID().toString();
        
        try {
            // 강의 정보가 Redis에 있는지 확인하고 없으면 동기화
            syncCourseToRedisIfNeeded(courseId);
            
            // Lua Script 실행 (원자적 처리) - 최대 3회 재시도
            String courseKey = COURSE_KEY_PREFIX + courseId;
            List<Object> result = null;
            int retryCount = 0;
            int maxRetries = 3;
            
            while (retryCount < maxRetries) {
                result = stringRedisTemplate.execute(
                    cancellationScript,
                    Collections.singletonList(courseKey),
                    studentId.toString(),
                    courseId.toString()
                );
                
                // 결과 확인
                if (result != null && result.size() >= 3) {
                    Long success = (Long) result.get(0);
                    String message = (String) result.get(1);
                    
                    // COURSE_NOT_FOUND인 경우 재동기화 후 재시도
                    if (success == 0 && "COURSE_NOT_FOUND".equals(message)) {
                        log.warn("취소 처리 중 Redis에서 강의 정보를 찾을 수 없음 - 재동기화 시도 {}/{}, CourseId: {}", 
                                retryCount + 1, maxRetries, courseId);
                        
                        // 강제로 다시 동기화
                        syncCourseToRedisIfNeeded(courseId);
                        retryCount++;
                        
                        if (retryCount < maxRetries) {
                            Thread.sleep(50); // 50ms 대기 후 재시도
                            continue;
                        }
                    }
                }
                break; // 성공하거나 다른 오류인 경우 루프 종료
            }
            
            if (result != null && result.size() >= 3) {
                Long success = (Long) result.get(0);
                String message = (String) result.get(1);
                Long newCount = (Long) result.get(2);
            
                Map<String, String> cancelResult = new HashMap<>();
                cancelResult.put("success", success == 1 ? "true" : "false");
                cancelResult.put("message", message);
                cancelResult.put("studentId", studentId.toString());
                cancelResult.put("courseId", courseId.toString());
                cancelResult.put("reason", reason);
                cancelResult.put("queueId", queueId);
                cancelResult.put("queuedAt", LocalDateTime.now().toString());
                cancelResult.put("newStudentCount", newCount.toString());
                cancelResult.put("status", "CANCEL_PROCESSED");
                
                // 결과를 Redis에 저장
                String resultKey = RESULT_KEY_PREFIX + queueId;
                stringRedisTemplate.opsForHash().putAll(resultKey, cancelResult);
            
                log.info("취소 요청이 큐에 등록되었습니다 - QueueId: {}, StudentId: {}, CourseId: {}, Success: {}, Message: {}", 
                        queueId, studentId, courseId, success == 1, message);
                
                return queueId;
            } else {
                throw new RuntimeException("취소 Lua Script 실행 결과가 예상과 다릅니다");
            }
            
        } catch (Exception e) {
            log.error("취소 요청 큐 등록 실패 - QueueId: {}, StudentId: {}, CourseId: {}, Error: {}", 
                    queueId, studentId, courseId, e.getMessage(), e);
            
            // 실패한 경우에도 결과를 저장
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("success", "false");
            errorResult.put("message", "CANCEL_FAILED: " + e.getMessage());
            errorResult.put("studentId", studentId.toString());
            errorResult.put("courseId", courseId.toString());
            errorResult.put("reason", reason);
            errorResult.put("queueId", queueId);
            errorResult.put("queuedAt", LocalDateTime.now().toString());
            errorResult.put("status", "CANCEL_FAILED");
            
            String resultKey = RESULT_KEY_PREFIX + queueId;
            stringRedisTemplate.opsForHash().putAll(resultKey, errorResult);
            
            return queueId;
        }
    }


    /**
     * Redis에서 결과 데이터를 읽어옵니다
     */
    public Map<String, Object> readResult(String queueId) {
        try {
            String key = RESULT_KEY_PREFIX + queueId;
            
            // Redis 키의 타입을 먼저 확인
            String keyType = stringRedisTemplate.type(key).code();
            
            if ("hash".equals(keyType)) {
                // Hash 타입인 경우 (취소 요청 결과)
                Map<Object, Object> hashData = stringRedisTemplate.opsForHash().entries(key);
                if (!hashData.isEmpty()) {
                    Map<String, Object> result = new HashMap<>();
                    for (Map.Entry<Object, Object> entry : hashData.entrySet()) {
                        result.put(entry.getKey().toString(), entry.getValue());
                    }
                    return result;
                }
            } else if ("string".equals(keyType)) {
                // String 타입인 경우 (수강신청 결과)
                String json = stringRedisTemplate.opsForValue().get(key);
                if (json != null) {
                    return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("결과 조회 실패: {}", e.getMessage(), e);
            return null;
        }
    }
}


