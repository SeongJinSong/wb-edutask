package com.wb.edutask.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.wb.edutask.dto.CourseResponseDto;
import com.wb.edutask.entity.Course;
import com.wb.edutask.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis ZSet을 활용한 강의 랭킹 서비스
 * 1페이지(상위 20개)만 ZSet 사용, 나머지는 DB 정렬
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRankingService {
    
    private final StringRedisTemplate stringRedisTemplate;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    
    // ZSet 키 상수
    private static final String RANKING_APPLICANTS = "course:ranking:applicants";
    private static final String RANKING_RATE = "course:ranking:rate";
    private static final int ZSET_MAX_SIZE = 40; // ZSet 최대 크기 (상위 40개, 수강취소 대비 버퍼존)
    private static final int FIRST_PAGE_SIZE = 20; // 첫 페이지 크기
    private static final int ZSET_TTL_MINUTES = 2; // ZSet TTL: 2분 (개발용)
    
    /**
     * 정렬된 강의 목록을 조회합니다 (1페이지는 ZSet, 나머지는 DB)
     * 
     * @param sortBy 정렬 기준 (recent, applicants, remaining)
     * @param pageable 페이징 정보
     * @return 정렬된 강의 목록
     */
    public Page<CourseResponseDto> getRankedCourses(String sortBy, Pageable pageable) {
        try {
            // 1페이지이고 ZSet 지원 정렬이면 ZSet 사용
            if (pageable.getPageNumber() == 0 && isZSetSupported(sortBy)) {
                return getZSetRanking(sortBy, pageable);
            }
        } catch (Exception e) {
            log.warn("ZSet 랭킹 조회 실패, DB로 Fallback - SortBy: {}, Error: {}", sortBy, e.getMessage());
        }
        
        // 2페이지 이상이거나 ZSet 실패 시 기존 DB 정렬 사용
        return courseService.getAvailableCoursesForEnrollmentWithSort(sortBy, pageable);
    }
    
    /**
     * ZSet을 사용한 1페이지 랭킹 조회
     * 
     * @param sortBy 정렬 기준
     * @param pageable 페이징 정보
     * @return ZSet 기반 랭킹 결과
     */
    private Page<CourseResponseDto> getZSetRanking(String sortBy, Pageable pageable) {
        String rankingKey = getRankingKey(sortBy);
        
        // ZSet에서 상위 20개 강의 ID 조회 (내림차순)
        Set<String> courseKeys = stringRedisTemplate.opsForZSet()
            .reverseRange(rankingKey, 0, pageable.getPageSize() - 1);
        
        if (courseKeys == null || courseKeys.isEmpty()) {
            log.debug("ZSet에 랭킹 데이터가 없습니다 - Key: {}", rankingKey);
            // ZSet이 비어있으면 DB로 Fallback
            return courseService.getAvailableCoursesForEnrollmentWithSort(sortBy, pageable);
        }
        
        // "course:1" → 1L 변환
        List<Long> zsetCourseIds = courseKeys.stream()
            .map(key -> Long.parseLong(key.replace("course:", "")))
            .collect(Collectors.toList());
        
        // ZSet 데이터가 첫 페이지 크기보다 적으면 ZSet에 추가 데이터 넣기
        List<Long> allCourseIds = new ArrayList<>(zsetCourseIds);
        
        if (zsetCourseIds.size() < FIRST_PAGE_SIZE) {
            int remainingCount = FIRST_PAGE_SIZE - zsetCourseIds.size();
            
            // DB에서 ZSet에 없는 강의들을 정렬 순서대로 가져오기
            Page<Course> remainingCourses = courseRepository.findAvailableCoursesForEnrollmentWithSort(
                sortBy, 
                PageRequest.of(0, remainingCount * 2) // 여유분 확보
            );
            
            // ZSet에 이미 있는 강의는 제외하고 선별
            List<Course> additionalCourses = remainingCourses.getContent().stream()
                .filter(course -> !zsetCourseIds.contains(course.getId()))
                .limit(remainingCount)
                .collect(Collectors.toList());
            
            // 선별된 강의들을 ZSet에 추가 (@BatchSize로 N+1 문제 자동 해결)
            for (Course course : additionalCourses) {
                double score = calculateScore(sortBy, course);
                stringRedisTemplate.opsForZSet().add(rankingKey, "course:" + course.getId(), score);
                allCourseIds.add(course.getId());
                
                log.debug("ZSet에 강의 추가 - CourseId: {}, Score: {:.2f}", course.getId(), score);
            }
            
            // ZSet 크기 제한 (상위 40개만 유지, 수강취소 대비 버퍼존)
            trimZSet(rankingKey);
            
            log.debug("ZSet 데이터 부족으로 {}개 추가 - 기존: {}, 추가: {}, 총: {}", 
                    additionalCourses.size(), zsetCourseIds.size(), additionalCourses.size(), allCourseIds.size());
        }
        
        // DB에서 강의 정보 조회 (순서 유지)
        List<Course> courses = courseRepository.findByIdInOrderByField(allCourseIds);
        
        // 원래 순서대로 정렬 (ZSet 우선, 그 다음 DB 순서)
        Map<Long, Course> courseMap = courses.stream()
            .collect(Collectors.toMap(Course::getId, course -> course));
        
        List<Course> orderedCourses = allCourseIds.stream()
            .map(courseMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        // CourseResponseDto 변환
        List<CourseResponseDto> courseDtos = orderedCourses.stream()
            .map(course -> CourseResponseDto.from(course, course.getCurrentStudents()))
            .collect(Collectors.toList());
        
        // 전체 개수는 DB에서 조회
        long totalCount = courseRepository.countAvailableCoursesForEnrollment();
        
        log.debug("ZSet 랭킹 조회 완료 - SortBy: {}, ZSet: {}, Total: {}", 
                sortBy, zsetCourseIds.size(), courseDtos.size());
        
        return new PageImpl<>(courseDtos, pageable, totalCount);
    }
    
    /**
     * 강의 랭킹을 업데이트합니다 (수강신청/취소 시 호출)
     * 
     * @param courseId 강의 ID
     * @param currentStudents 현재 수강인원
     * @param maxStudents 최대 수강인원
     */
    public void updateCourseRanking(Long courseId, int currentStudents, int maxStudents) {
        try {
            String courseKey = "course:" + courseId;
            double rate = maxStudents > 0 ? (double) currentStudents / maxStudents : 0.0;
            
            // 1. 신청자 많은순 처리 (ZSet에 이미 있는 경우만 업데이트)
            updateZSetIfRelevant(RANKING_APPLICANTS, courseKey, currentStudents);
            
            // 2. 신청률 높은순 처리 (ZSet에 이미 있는 경우만 업데이트)
            updateZSetIfRelevant(RANKING_RATE, courseKey, rate);
            
            log.debug("강의 랭킹 업데이트 완료 - CourseId: {}, Students: {}, Rate: {:.2f}", 
                    courseId, currentStudents, rate);
            
        } catch (Exception e) {
            log.error("강의 랭킹 업데이트 실패 - CourseId: {}, Error: {}", courseId, e.getMessage(), e);
        }
    }
    
    /**
     * ZSet에 이미 있는 강의만 업데이트하거나, 상위 20개에 들 수 있는 경우만 추가
     * 
     * @param zsetKey ZSet 키
     * @param courseKey 강의 키
     * @param score 점수
     */
    private void updateZSetIfRelevant(String zsetKey, String courseKey, double score) {
        try {
            // 1. 이미 ZSet에 있는지 확인
            Double existingScore = stringRedisTemplate.opsForZSet().score(zsetKey, courseKey);
            
            if (existingScore != null) {
                // 이미 있으면 점수 업데이트
                stringRedisTemplate.opsForZSet().add(zsetKey, courseKey, score);
                log.debug("ZSet 업데이트 - Key: {}, Course: {}, Score: {:.2f} (이전: {:.2f})", 
                        zsetKey, courseKey, score, existingScore);
                
                // 점수 감소 시에도 교체하지 않음 (성능 우선)
                // 2페이지부터는 DB 조회하므로 정확성 보장됨
            } else {
                // 없으면 상위 20개에 들 수 있는지 확인
                Long zsetSize = stringRedisTemplate.opsForZSet().zCard(zsetKey);
                
                if (zsetSize == null || zsetSize < ZSET_MAX_SIZE) {
                    // ZSet에 여유가 있으면 추가
                    stringRedisTemplate.opsForZSet().add(zsetKey, courseKey, score);
                    log.debug("ZSet 추가 (여유공간) - Key: {}, Course: {}, Score: {:.2f}", zsetKey, courseKey, score);
                } else {
                    // ZSet이 가득 찬 경우, 최하위 점수와 비교
                    Set<String> lowestScoreSet = stringRedisTemplate.opsForZSet().range(zsetKey, 0, 0);
                    if (lowestScoreSet != null && !lowestScoreSet.isEmpty()) {
                        String lowestCourse = lowestScoreSet.iterator().next();
                        Double lowestScore = stringRedisTemplate.opsForZSet().score(zsetKey, lowestCourse);
                        
                        if (lowestScore != null && score > lowestScore) {
                            // 현재 점수가 더 높으면 최하위 제거하고 추가
                            stringRedisTemplate.opsForZSet().remove(zsetKey, lowestCourse);
                            stringRedisTemplate.opsForZSet().add(zsetKey, courseKey, score);
                            log.debug("ZSet 교체 - Key: {}, 제거: {} ({}), 추가: {} ({})", 
                                    zsetKey, lowestCourse, lowestScore, courseKey, score);
                        } else {
                            log.debug("ZSet 추가 불가 (점수 부족) - Key: {}, Course: {}, Score: {:.2f}, 최하위: {:.2f}", 
                                    zsetKey, courseKey, score, lowestScore != null ? lowestScore : 0.0);
                        }
                    }
                }
            }
            
            // TTL 설정
            setZSetTTL(zsetKey);
            
        } catch (Exception e) {
            log.warn("ZSet 업데이트 실패 - Key: {}, Course: {}, Error: {}", zsetKey, courseKey, e.getMessage());
        }
    }
    
    
    /**
     * 강의를 랭킹에서 제거합니다 (강의 삭제 시 호출)
     * 
     * @param courseId 강의 ID
     */
    public void removeCourseFromRanking(Long courseId) {
        try {
            String courseKey = "course:" + courseId;
            
            stringRedisTemplate.opsForZSet().remove(RANKING_APPLICANTS, courseKey);
            stringRedisTemplate.opsForZSet().remove(RANKING_RATE, courseKey);
            
            log.debug("강의 랭킹에서 제거 완료 - CourseId: {}", courseId);
            
        } catch (Exception e) {
            log.error("강의 랭킹 제거 실패 - CourseId: {}, Error: {}", courseId, e.getMessage(), e);
        }
    }
    
    /**
     * 정렬 기준에 따른 점수를 계산합니다 (@BatchSize로 N+1 문제 자동 해결)
     * 
     * @param sortBy 정렬 기준
     * @param course 강의 엔티티
     * @return 계산된 점수
     */
    private double calculateScore(String sortBy, Course course) {
        switch (sortBy) {
            case "applicants":
                return course.getCurrentStudents(); // 신청자 수 (@BatchSize로 최적화)
            case "remaining":
                // 신청률 (currentStudents / maxStudents)
                return course.getMaxStudents() > 0 ? 
                    (double) course.getCurrentStudents() / course.getMaxStudents() : 0.0;
            default:
                return 0.0; // recent는 ZSet 사용 안함
        }
    }
    
    /**
     * ZSet 크기를 제한합니다 (상위 N개만 유지)
     * 
     * @param rankingKey ZSet 키
     */
    private void trimZSet(String rankingKey) {
        try {
            Long size = stringRedisTemplate.opsForZSet().zCard(rankingKey);
            if (size != null && size > ZSET_MAX_SIZE) {
                // 하위 항목들 제거 (상위 40개만 유지)
                stringRedisTemplate.opsForZSet().removeRange(rankingKey, 0, size - ZSET_MAX_SIZE - 1);
                log.debug("ZSet 크기 제한 적용 - Key: {}, 제거된 항목: {}", rankingKey, size - ZSET_MAX_SIZE);
            }
        } catch (Exception e) {
            log.warn("ZSet 크기 제한 실패 - Key: {}, Error: {}", rankingKey, e.getMessage());
        }
    }
    
    /**
     * ZSet에 TTL을 설정합니다 (2분 - 개발용)
     * 
     * @param rankingKey ZSet 키
     */
    private void setZSetTTL(String rankingKey) {
        try {
            stringRedisTemplate.expire(rankingKey, java.time.Duration.ofMinutes(ZSET_TTL_MINUTES));
            log.debug("ZSet TTL 설정 완료 - Key: {}, TTL: {}분", rankingKey, ZSET_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("ZSet TTL 설정 실패 - Key: {}, Error: {}", rankingKey, e.getMessage());
        }
    }
    
    /**
     * 정렬 기준이 ZSet을 지원하는지 확인합니다
     * 
     * @param sortBy 정렬 기준
     * @return ZSet 지원 여부
     */
    private boolean isZSetSupported(String sortBy) {
        return "applicants".equals(sortBy) || "remaining".equals(sortBy);
    }
    
    /**
     * 정렬 기준에 따른 ZSet 키를 반환합니다
     * 
     * @param sortBy 정렬 기준
     * @return ZSet 키
     */
    private String getRankingKey(String sortBy) {
        switch (sortBy) {
            case "applicants":
                return RANKING_APPLICANTS;
            case "remaining":
                return RANKING_RATE;
            default:
                throw new IllegalArgumentException("지원하지 않는 정렬 기준: " + sortBy);
        }
    }
}
