# 👤 회원 관리 API

## 📋 개요

회원 가입, 조회, 검색, 필터링을 담당하는 API입니다.

- **컨트롤러**: `MemberApiController`
- **기본 경로**: `/api/v1/members`
- **기능**: 회원가입, 회원 조회, 검색/필터링, 페이징
- **고급 기능**: 동적 검색, 회원유형 필터링, 페이징 지원

## 🚀 API 엔드포인트

### 1. 회원 목록 조회 (검색 + 페이징)

#### `GET /api/v1/members`

회원 목록을 페이징으로 조회하며, 검색 및 필터링을 지원합니다.

**쿼리 파라미터:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | Integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | ❌ | 20 | 페이지 크기 |
| `sort` | String | ❌ | createdAt,desc | 정렬 기준 |
| `search` | String | ❌ | - | 검색어 (이름, 이메일) |
| `memberType` | String | ❌ | - | 회원유형 (STUDENT, INSTRUCTOR) |

**성공 응답 (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "name": "홍길동",
      "email": "hong@weolbu.com",
      "phoneNumber": "010-1234-5678",
      "memberType": "STUDENT",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "direction": "DESC",
      "property": "createdAt"
    }
  },
  "totalElements": 100,
  "totalPages": 5,
  "last": false,
  "first": true
}
```

**cURL 예시:**
```bash
# 전체 회원 목록 (첫 페이지)
curl -X GET "http://localhost:8080/api/v1/members?page=0&size=20"

# 이름으로 검색
curl -X GET "http://localhost:8080/api/v1/members?search=홍길동"

# 학생만 필터링
curl -X GET "http://localhost:8080/api/v1/members?memberType=STUDENT"

# 검색 + 필터링 + 페이징
curl -X GET "http://localhost:8080/api/v1/members?search=김&memberType=INSTRUCTOR&page=0&size=10"
```

### 2. 회원 가입

#### `POST /api/v1/members/register`

새로운 회원을 등록합니다.

**요청 본문:**
```json
{
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "phoneNumber": "010-1234-5678",
  "password": "Test123",
  "memberType": "STUDENT"
}
```

**요청 필드:**

| 필드 | 타입 | 필수 | 설명 | 제약사항 |
|------|------|------|------|----------|
| `name` | String | ✅ | 회원 이름 | 최대 50자 |
| `email` | String | ✅ | 이메일 주소 | • 유효한 이메일 형식<br/>• 최대 100자<br/>• 시스템 내 고유 |
| `phoneNumber` | String | ✅ | 휴대폰 번호 | • 010-XXXX-XXXX 형식<br/>• 시스템 내 고유 |
| `password` | String | ✅ | 비밀번호 | • 6-10자<br/>• 영문 대소문자+숫자 조합 |
| `memberType` | Enum | ✅ | 회원 유형 | STUDENT, INSTRUCTOR |

**성공 응답 (201 Created):**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "phoneNumber": "010-1234-5678",
  "memberType": "STUDENT",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**에러 응답 (400 Bad Request):**
```json
{
  "error": "유효성 검증 실패",
  "message": "입력값을 확인해주세요",
  "details": {
    "password": "비밀번호는 6자 이상 10자 이하여야 합니다"
  }
}
```

**cURL 예시:**
```bash
curl -X POST http://localhost:8080/api/v1/members/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "email": "hong@weolbu.com",
    "phoneNumber": "010-1234-5678",
    "password": "Test123",
    "memberType": "STUDENT"
  }'
```

### 3. ID로 회원 조회

#### `GET /api/v1/members/{id}`

회원 ID로 회원 정보를 조회합니다.

**경로 변수:**

| 변수 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 회원 ID |

**성공 응답 (200 OK):**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "phoneNumber": "010-1234-5678",
  "memberType": "STUDENT",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**에러 응답 (404 Not Found):**
```json
{
  "error": "회원을 찾을 수 없습니다",
  "message": "해당 ID의 회원이 존재하지 않습니다."
}
```

**cURL 예시:**
```bash
curl -X GET http://localhost:8080/api/v1/members/1
```

### 4. 이메일로 회원 조회

#### `GET /api/v1/members/email/{email}`

이메일로 회원 정보를 조회합니다.

**경로 변수:**

| 변수 | 타입 | 설명 |
|------|------|------|
| `email` | String | 이메일 주소 |

**성공 응답 (200 OK):**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "phoneNumber": "010-1234-5678",
  "memberType": "STUDENT",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

**cURL 예시:**
```bash
curl -X GET http://localhost:8080/api/v1/members/email/hong@weolbu.com
```

## 🔍 고급 검색 기능

### 동적 검색 (JpaSpecificationExecutor)
- **이름 검색**: 부분 일치 (대소문자 무시)
- **이메일 검색**: 부분 일치 (대소문자 무시)
- **복합 검색**: 이름 OR 이메일 조건

### 필터링
- **회원유형**: STUDENT, INSTRUCTOR
- **조합 가능**: 검색어 + 회원유형 + 페이징

### 페이징 최적화
- **기본 크기**: 20개
- **정렬**: 가입일 기준 내림차순
- **대용량 데이터**: 효율적 메모리 사용

## 🔍 유효성 검증 규칙

### 이메일 검증
- **형식**: 유효한 이메일 형식이어야 함
- **길이**: 최대 100자
- **중복**: 시스템 내에서 고유해야 함

### 휴대폰 번호 검증
- **형식**: `010-XXXX-XXXX` 형식
- **중복**: 시스템 내에서 고유해야 함

### 비밀번호 검증
- **길이**: 6자 이상 10자 이하
- **조합**: 영문 소문자, 대문자, 숫자 포함

### 이름 검증
- **길이**: 최대 50자
- **필수**: 반드시 입력해야 함

## 🚨 에러 케이스

### 1. 중복 이메일
```json
{
  "error": "회원 가입 실패",
  "message": "이미 사용 중인 이메일입니다: hong@weolbu.com"
}
```

### 2. 중복 휴대폰 번호
```json
{
  "error": "회원 가입 실패",
  "message": "이미 사용 중인 휴대폰 번호입니다: 010-1234-5678"
}
```

### 3. 유효성 검증 실패
```json
{
  "error": "유효성 검증 실패",
  "message": "입력값을 확인해주세요",
  "details": {
    "email": "올바른 이메일 형식이 아닙니다",
    "password": "비밀번호는 영문 소문자, 대문자, 숫자를 포함해야 합니다"
  }
}
```

## 📊 성능 특징

### 검색 성능
- **인덱스**: 이메일, 휴대폰 번호에 유니크 인덱스
- **동적 쿼리**: JpaSpecificationExecutor로 효율적 검색
- **페이징**: 메모리 효율적 대용량 데이터 처리

### 확장성
- **검색 조건 추가**: Specification 패턴으로 쉬운 확장
- **필터링 옵션**: 새로운 필터 조건 추가 용이
- **정렬 옵션**: 다양한 정렬 기준 지원

## 🧪 테스트 시나리오

### 1. 정상 케이스
- ✅ 수강생 회원가입
- ✅ 강사 회원가입
- ✅ ID로 회원 조회
- ✅ 이메일로 회원 조회
- ✅ 이름으로 검색
- ✅ 회원유형 필터링
- ✅ 페이징 처리

### 2. 에러 케이스
- ❌ 중복 이메일로 가입 시도
- ❌ 중복 휴대폰 번호로 가입 시도
- ❌ 잘못된 비밀번호 형식
- ❌ 잘못된 이메일 형식
- ❌ 잘못된 휴대폰 번호 형식
- ❌ 존재하지 않는 회원 조회

## 📝 테스트 스크립트

### 전체 테스트 실행
```bash
./test-member-api.sh
```

### 개별 테스트
```bash
# 회원가입 테스트
curl -X POST http://localhost:8080/api/v1/members/register \
  -H "Content-Type: application/json" \
  -d '{"name":"홍길동","email":"hong@weolbu.com","phoneNumber":"010-1234-5678","password":"Test123","memberType":"STUDENT"}'

# 회원 목록 조회 (페이징)
curl -X GET "http://localhost:8080/api/v1/members?page=0&size=5"

# 검색 테스트
curl -X GET "http://localhost:8080/api/v1/members?search=홍길동"

# 필터링 테스트
curl -X GET "http://localhost:8080/api/v1/members?memberType=STUDENT"
```

## 🔗 관련 파일

- **컨트롤러**: `src/main/java/com/wb/edutask/controller/MemberApiController.java`
- **서비스**: `src/main/java/com/wb/edutask/service/MemberService.java`
- **엔티티**: `src/main/java/com/wb/edutask/entity/Member.java`
- **리포지토리**: `src/main/java/com/wb/edutask/repository/MemberRepository.java`
- **테스트**: `src/test/java/com/wb/edutask/service/MemberServiceTest.java`
- **테스트 스크립트**: `test-member-api.sh`

## 🎯 기술적 우수성

### 기본 요구사항 대비 추가 구현
- **기본**: 회원가입, 회원 조회
- **추가**: 동적 검색, 필터링, 페이징, 성능 최적화
- **기술**: JpaSpecificationExecutor, 동적 쿼리, 인덱스 최적화

### 사용자 경험
- **실시간 검색**: 타이핑과 동시에 결과 표시
- **필터링**: 회원유형별 구분 조회
- **페이징**: 대용량 데이터도 빠른 로딩

---

**회원 관리 API** - 안전하고 효율적인 회원 관리 시스템! 👤