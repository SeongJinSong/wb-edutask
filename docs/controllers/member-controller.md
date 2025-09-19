# 👤 회원 관리 API

## 📋 개요

회원 가입, 조회, 유효성 검증을 담당하는 API입니다.

- **컨트롤러**: `MemberController`
- **기본 경로**: `/api/members`
- **기능**: 회원가입, 회원 조회, 중복 검증

## 🚀 API 엔드포인트

### 1. 회원 가입

#### `POST /api/members/register`

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
| `email` | String | ✅ | 이메일 주소 | 유효한 이메일 형식, 최대 100자 |
| `phoneNumber` | String | ✅ | 휴대폰 번호 | 010-XXXX-XXXX 형식 |
| `password` | String | ✅ | 비밀번호 | 6-10자, 영문 대소문자+숫자 조합 |
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
curl -X POST http://localhost:8080/api/members/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "홍길동",
    "email": "hong@weolbu.com",
    "phoneNumber": "010-1234-5678",
    "password": "Test123",
    "memberType": "STUDENT"
  }'
```

### 2. ID로 회원 조회

#### `GET /api/members/{id}`

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
curl -X GET http://localhost:8080/api/members/1
```

### 3. 이메일로 회원 조회

#### `GET /api/members/email/{email}`

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

**에러 응답 (404 Not Found):**
```json
{
  "error": "회원을 찾을 수 없습니다",
  "message": "해당 이메일의 회원이 존재하지 않습니다."
}
```

**cURL 예시:**
```bash
curl -X GET http://localhost:8080/api/members/email/hong@weolbu.com
```

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
- **조합**: 영문 소문자, 대문자, 숫자 중 최소 2가지 이상 조합

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

## 🧪 테스트 시나리오

### 1. 정상 케이스
- ✅ 수강생 회원가입
- ✅ 강사 회원가입
- ✅ ID로 회원 조회
- ✅ 이메일로 회원 조회

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
curl -X POST http://localhost:8080/api/members/register \
  -H "Content-Type: application/json" \
  -d '{"name":"홍길동","email":"hong@weolbu.com","phoneNumber":"010-1234-5678","password":"Test123","memberType":"STUDENT"}'

# 회원 조회 테스트
curl -X GET http://localhost:8080/api/members/1
curl -X GET http://localhost:8080/api/members/email/hong@weolbu.com
```

## 🔗 관련 파일

- **컨트롤러**: `src/main/java/com/wb/edutask/controller/MemberController.java`
- **서비스**: `src/main/java/com/wb/edutask/service/MemberService.java`
- **엔티티**: `src/main/java/com/wb/edutask/entity/Member.java`
- **테스트**: `src/test/java/com/wb/edutask/service/MemberServiceTest.java`
- **테스트 스크립트**: `test-member-api.sh`

---

**회원 관리 API** - 안전하고 효율적인 회원 관리 시스템! 👤
