package com.wb.edutask.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.wb.edutask.entity.Course;
import com.wb.edutask.repository.CourseRepository;
import com.wb.edutask.repository.EnrollmentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis 기반 동시성 제어 서비스
 * Lua 스크립트를 사용한 원자적 처리로 동시성 제어
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisConcurrencyService {

    private static final String COURSE_KEY_PREFIX = "course:";
    private static final int COURSE_CACHE_TTL_MINUTES = 2; // 강의 정보 캐시 TTL (2분 - 개발용)
    
    private final StringRedisTemplate stringRedisTemplate;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    
    // Lua Script: 정원 확인 + 수강생 수 증가 (원자적 처리)
    private static final String ENROLLMENT_SCRIPT = """
        local courseKey = KEYS[1]
        local studentId = ARGV[1]
        local courseId = ARGV[2]
        
        -- 강의 정보 조회
        local courseData = redis.call('HMGET', courseKey, 'currentStudents', 'maxStudents', 'courseId', 'courseName')
        local currentStudents = tonumber(courseData[1])
        local maxStudents = tonumber(courseData[2])
        local storedCourseId = courseData[3]
        local courseName = courseData[4]
        
        -- 강의 정보가 없는 경우
        if not currentStudents or not maxStudents or not storedCourseId then
            return {0, 'COURSE_NOT_FOUND', 0}
        end
        
        -- 정원 초과 확인
        if currentStudents >= maxStudents then
            return {0, 'CAPACITY_EXCEEDED', currentStudents}
        end
        
        -- 수강생 수 증가
        local newCount = redis.call('HINCRBY', courseKey, 'currentStudents', 1)
        
        return {1, 'SUCCESS', newCount}
        """;
    
    private DefaultRedisScript<List> enrollmentScript;
    
    /**
     * 초기화 시 Lua 스크립트 설정
     */
    @PostConstruct
    public void init() {
        this.enrollmentScript = new DefaultRedisScript<>();
        this.enrollmentScript.setScriptText(ENROLLMENT_SCRIPT);
        this.enrollmentScript.setResultType(List.class);
    }
    
    /**
     * 강의 정보를 Redis에 동기화합니다 (필요시)
     * 
     * @param courseId 강의 ID
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
                // 실제 DB에서 현재 수강생 수 조회
                long actualCurrentStudents = enrollmentRepository.countActiveEnrollmentsByCourse(courseId);
                
                Map<String, String> courseData = new HashMap<>();
                courseData.put("courseId", courseId.toString());
                courseData.put("courseName", course.getCourseName());
                courseData.put("currentStudents", String.valueOf(actualCurrentStudents));
                courseData.put("maxStudents", course.getMaxStudents().toString());
                courseData.put("instructorId", course.getInstructor().getId().toString());
                
                stringRedisTemplate.opsForHash().putAll(courseKey, courseData);
                // 강의 정보에 TTL 설정 (1분 - 개발용)
                stringRedisTemplate.expire(courseKey, COURSE_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("강의 정보 Redis 동기화 완료: {} (currentStudents: {}, maxStudents: {}, TTL: {}분)", 
                        courseId, actualCurrentStudents, course.getMaxStudents(), COURSE_CACHE_TTL_MINUTES);
            } else {
                log.debug("Redis에 강의 정보가 이미 존재함 - CourseId: {}, CurrentStudents: {}", 
                        courseId, existingCurrentStudents);
            }
        } catch (Exception e) {
            log.warn("강의 정보 Redis 동기화 실패: {}", e.getMessage());
        }
    }
    
    /**
     * 수강신청 Lua 스크립트를 동기 실행합니다
     * 
     * @param studentId 학생 ID
     * @param courseId 강의 ID
     * @return 실행 결과 (success, message, newStudentCount 포함)
     */
    public Map<String, Object> executeEnrollmentLuaScript(Long studentId, Long courseId) {
        String courseKey = COURSE_KEY_PREFIX + courseId;
        try {
            syncCourseToRedisIfNeeded(courseId);
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
                
                if (result != null && result.size() >= 3) {
                    Long success = (Long) result.get(0);
                    String message = (String) result.get(1);
                    
                    if (success == 0 && "COURSE_NOT_FOUND".equals(message)) {
                        log.warn("Redis에서 강의 정보를 찾을 수 없음 - 재동기화 시도 {}/{}, CourseId: {}", 
                                retryCount + 1, maxRetries, courseId);
                        syncCourseToRedisIfNeeded(courseId);
                        retryCount++;
                        if (retryCount < maxRetries) {
                            Thread.sleep(50);
                            continue;
                        }
                    }
                }
                break;
            }
            
            if (result.size() >= 3) {
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
            return errorResult;
        }
    }
    
    /**
     * Redis에서 강의의 수강생 수를 감소시킵니다 (취소 시 사용)
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
     * Redis 메시지를 한글로 변환합니다
     * 
     * @param redisMessage Redis에서 반환된 메시지
     * @return 한글 메시지
     */
    public String convertRedisMessageToKorean(String redisMessage) {
        return switch (redisMessage) {
            case "SUCCESS" -> "수강신청이 완료되었습니다";
            case "CAPACITY_EXCEEDED" -> "강의 정원이 초과되었습니다";
            case "COURSE_NOT_FOUND" -> "강의 정보를 찾을 수 없습니다";
            case "SCRIPT_EXECUTION_ERROR" -> "시스템 오류가 발생했습니다";
            default -> "알 수 없는 오류가 발생했습니다: " + redisMessage;
        };
    }
}
