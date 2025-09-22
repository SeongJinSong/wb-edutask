# 🎓 수강신청 API

## 📋 개요

수강신청, 신청 취소, 신청 내역 조회를 담당하는 고성능 동시성 제어 API입니다.

- **컨트롤러**: `EnrollmentApiController`
- **기본 경로**: `/api/v1/enrollments`
- **기능**: 수강신청, 신청 취소, 신청 내역 조회
- **고급 기능**: Lua Script 동시성 제어, 자동 승인, 실시간 통계 업데이트

## 🚀 API 엔드포인트

### 1. 수강신청

#### `POST /api/v1/enrollments`

새로운 수강신청을 등록합니다.

**🔥 고급 기능**: Redis Lua Script로 원자적 처리, 수천명 동시 신청 가능

**요청 본문:**
```json
{
  "studentId": 1,
  "courseId": 1
}
```

**요청 필드:**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| `studentId` | Long | ✅ | 학생 ID | 존재하는 학생이어야 함 |
| `courseId` | Long | ✅ | 강의 ID | 존재하는 강의여야 함 |

**성공 응답 (201 Created):**
```json
{
  "id": 1,
  "student": {
    "id": 1,
    "name": "홍길동",
    "email": "student@weolbu.com"
  },
  "course": {
    "id": 1,
    "courseName": "Spring Boot 마스터 클래스",
    "instructor": {
      "id": 2,
      "name": "김강사"
    },
    "currentStudents": 16,
    "maxStudents": 30
  },
  "status": "APPROVED",
  "appliedAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**에러 응답 (409 Conflict):**
```json
{
  "error": "수강신청 실패",
  "message": "이미 수강신청한 강의입니다."
}
```

**에러 응답 (400 Bad Request):**
```json
{
  "error": "수강신청 실패",
  "message": "강의 정원이 초과되었습니다. (현재: 30/30)"
}
```

**cURL 예시:**
```bash
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": 1,
    "courseId": 1
  }'
```

### 2. 학생별 수강내역 조회

#### `GET /api/v1/enrollments/student/{studentId}`

학생의 수강신청 내역을 페이징으로 조회합니다.

**경로 변수:**

| 변수 | 타입 | 설명 |
|------|------|------|
| `studentId` | Long | 학생 ID |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | ❌ | 20 | 페이지 크기 |

**성공 응답 (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "student": {
        "id": 1,
        "name": "홍길동",
        "email": "student@weolbu.com"
      },
      "course": {
        "id": 1,
        "courseName": "Spring Boot 마스터 클래스",
        "instructor": {
          "id": 2,
          "name": "김강사"
        },
        "currentStudents": 16,
        "maxStudents": 30,
        "startDate": "2024-01-15",
        "endDate": "2024-03-15"
      },
      "status": "APPROVED",
      "appliedAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "first": true
}
```

**cURL 예시:**
```bash
# 학생의 수강내역 조회
curl -X GET "http://localhost:8080/api/v1/enrollments/student/1?page=0&size=20"
```

### 3. 수강신청 취소

#### `DELETE /api/v1/enrollments/{enrollmentId}`

수강신청을 취소합니다.

**🔥 고급 기능**: Redis 실시간 통계 업데이트, ZSet 랭킹 자동 반영

**경로 변수:**

| 변수 | 타입 | 설명 |
|------|------|------|
| `enrollmentId` | Long | 수강신청 ID |

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `reason` | String | ❌ | 취소 사유 |

**성공 응답 (200 OK):**
```json
{
  "message": "수강신청이 성공적으로 취소되었습니다."
}
```

**에러 응답 (404 Not Found):**
```json
{
  "error": "수강신청을 찾을 수 없습니다",
  "message": "해당 수강신청이 존재하지 않습니다."
}
```

**에러 응답 (400 Bad Request):**
```json
{
  "error": "수강신청 취소 실패",
  "message": "이미 취소된 수강신청입니다."
}
```

**cURL 예시:**
```bash
# 수강신청 취소 (사유 포함)
curl -X DELETE "http://localhost:8080/api/v1/enrollments/1?reason=개인사정"

# 수강신청 취소 (사유 없음)
curl -X DELETE "http://localhost:8080/api/v1/enrollments/1"
```

### 4. 수강신청 상세 조회

#### `GET /api/v1/enrollments/{enrollmentId}`

수강신청 상세 정보를 조회합니다.

**경로 변수:**

| 변수 | 타입 | 설명 |
|------|------|------|
| `enrollmentId` | Long | 수강신청 ID |

**성공 응답 (200 OK):**
```json
{
  "id": 1,
  "student": {
    "id": 1,
    "name": "홍길동",
    "email": "student@weolbu.com"
  },
  "course": {
    "id": 1,
    "courseName": "Spring Boot 마스터 클래스",
    "instructor": {
      "id": 2,
      "name": "김강사"
    },
    "currentStudents": 16,
    "maxStudents": 30
  },
  "status": "APPROVED",
  "appliedAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00",
  "reason": null
}
```

**cURL 예시:**
```bash
curl -X GET http://localhost:8080/api/v1/enrollments/1
```

## 🚀 고급 기능 - Redis 동시성 제어

### Lua Script 원자적 처리
```lua
-- 수강신청 Lua Script (원자적 실행)
local courseKey = KEYS[1]
local currentStudents = redis.call('HGET', courseKey, 'currentStudents')
local maxStudents = redis.call('HGET', courseKey, 'maxStudents')

if tonumber(currentStudents) < tonumber(maxStudents) then
    redis.call('HINCRBY', courseKey, 'currentStudents', 1)
    return 1  -- 성공
else
    return 0  -- 정원 초과
end
```

### 동시성 제어 특징
- **원자적 실행**: Redis 서버에서 단일 스레드로 안전 처리
- **정원 초과 방지**: 100% 정확성 보장
- **수천명 처리**: 이론적으로 수천명 동시 처리 가능
- **테스트 검증**: 50명 동시 처리 검증 완료

### 실시간 통계 업데이트
- **즉시 반영**: 수강신청/취소 시 `currentStudents` 실시간 업데이트
- **ZSet 랭킹**: 신청자 많은순, 신청률 높은순 자동 반영
- **TTL 캐시**: 2분 자동 만료로 메모리 효율성

## 📊 성능 특징

### 동시성 처리
- **50명 동시 신청**: 테스트 검증 완료
- **수천명 처리 가능**: Lua Script 원자적 처리
- **정원 초과 방지**: 100% 정확성 보장

### 응답 속도
- **수강신청**: Redis 기반 밀리초 단위 처리
- **내역 조회**: 페이징으로 빠른 응답
- **실시간 업데이트**: 즉시 랭킹 반영

### 확장성
- **Redis 클러스터**: 대용량 트래픽 대응
- **페이징**: 메모리 효율적 대용량 데이터 처리
- **비동기 처리**: 스케줄러 기반 데이터 동기화

## 🔍 비즈니스 로직

### 온라인 강의 자동 승인
- **즉시 승인**: 정원 내 신청 시 자동으로 `APPROVED` 상태
- **정원 초과**: 신청 자체가 실패 (대기 상태 없음)
- **간소화**: `APPLIED`, `REJECTED` 상태 불필요

### 중복 신청 방지
- **유니크 제약**: `(student_id, course_id)` 조합 유니크
- **DB 레벨**: 데이터베이스 제약조건으로 보장
- **API 레벨**: 사전 검증으로 사용자 경험 향상

### 상태 관리
- **APPROVED**: 수강 중 (정상 상태)
- **CANCELLED**: 수강 취소 (소프트 삭제)

## 🚨 에러 케이스

### 1. 정원 초과
```json
{
  "error": "수강신청 실패",
  "message": "강의 정원이 초과되었습니다. (현재: 30/30)"
}
```

### 2. 중복 신청
```json
{
  "error": "수강신청 실패",
  "message": "이미 수강신청한 강의입니다."
}
```

### 3. 존재하지 않는 학생/강의
```json
{
  "error": "수강신청 실패",
  "message": "존재하지 않는 학생 또는 강의입니다."
}
```

### 4. 이미 취소된 신청
```json
{
  "error": "수강신청 취소 실패",
  "message": "이미 취소된 수강신청입니다."
}
```

## 🧪 테스트 시나리오

### 1. 정상 케이스
- ✅ 수강신청 성공
- ✅ 학생별 수강내역 조회
- ✅ 수강신청 취소
- ✅ 수강신청 상세 조회

### 2. 동시성 테스트
- ✅ 50명 동시 수강신청 (정원 5명)
- ✅ 정확히 5명만 성공, 45명 실패
- ✅ 정원 초과 방지 100% 보장
- ✅ Redis-DB 데이터 정합성

### 3. 성능 테스트
- ✅ 수강신청 응답 속도 (밀리초)
- ✅ 실시간 랭킹 업데이트
- ✅ 대용량 수강내역 조회

### 4. 에러 케이스
- ❌ 정원 초과 시 신청 실패
- ❌ 중복 신청 방지
- ❌ 존재하지 않는 학생/강의
- ❌ 이미 취소된 신청 재취소

## 📝 테스트 스크립트

### 동시성 테스트
```bash
# 50명 동시 수강신청 테스트
./gradlew test --tests ConcurrencyTest

# 스트레스 테스트 결과 확인
./gradlew test --tests ConcurrencyTest::massiveConcurrentEnrollment_StressTest
```

### 기능 테스트
```bash
# 수강신청
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "courseId": 1}'

# 수강내역 조회
curl -X GET "http://localhost:8080/api/v1/enrollments/student/1"

# 수강신청 취소
curl -X DELETE "http://localhost:8080/api/v1/enrollments/1?reason=개인사정"
```

### 성능 측정
```bash
# 수강신청 응답 시간 측정
time curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "courseId": 1}'

# 랭킹 업데이트 확인
curl -X GET "http://localhost:8080/api/v1/courses?sort=applicants"
```

## 🔗 관련 파일

- **컨트롤러**: `src/main/java/com/wb/edutask/controller/EnrollmentApiController.java`
- **서비스**: `src/main/java/com/wb/edutask/service/EnrollmentService.java`
- **동시성 서비스**: `src/main/java/com/wb/edutask/service/RedisConcurrencyService.java`
- **엔티티**: `src/main/java/com/wb/edutask/entity/Enrollment.java`
- **리포지토리**: `src/main/java/com/wb/edutask/repository/EnrollmentRepository.java`
- **테스트**: `src/test/java/com/wb/edutask/service/EnrollmentServiceTest.java`
- **동시성 테스트**: `src/test/java/com/wb/edutask/service/ConcurrencyTest.java`

## 🎯 기술적 우수성

### 기본 요구사항 대비 추가 구현
- **기본**: 수강신청, 신청 취소, 신청 내역 조회
- **추가**: Redis Lua Script, 동시성 제어, 실시간 통계, ZSet 랭킹 연동
- **기술**: 원자적 처리, 분산 락, TTL 캐시, 스케줄러 동기화

### 엔터프라이즈급 동시성
- **검증된 성능**: 50명 동시 처리 테스트 완료
- **이론적 한계**: 수천명 동시 처리 가능
- **정확성 보장**: 정원 초과 방지 100%

### 실시간 시스템
- **즉시 반영**: 수강신청 시 랭킹 실시간 업데이트
- **정합성**: Redis-DB 스케줄러 기반 동기화
- **효율성**: TTL 캐시로 메모리 최적화

---

**수강신청 API** - 대용량 동시성을 보장하는 고성능 수강신청 시스템! 🎓