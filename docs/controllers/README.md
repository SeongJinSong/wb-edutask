# 📚 API 컨트롤러 문서

## 🎯 개요

WB Education Task Management System의 REST API 컨트롤러별 상세 문서입니다.

## 📋 컨트롤러 목록

### 1. [회원 관리 API](./member-controller.md)
- **컨트롤러**: `MemberController`
- **기능**: 회원가입, 회원 조회, 유효성 검증
- **엔드포인트**: `/api/members/*`

### 2. [강의 관리 API](./course-controller.md) 🚧
- **컨트롤러**: `CourseController` (예정)
- **기능**: 강의 등록, 강의 조회, 강의 수정/삭제
- **엔드포인트**: `/api/courses/*`

### 3. [수강 신청 API](./enrollment-controller.md) 🚧
- **컨트롤러**: `EnrollmentController` (예정)
- **기능**: 수강 신청, 신청 취소, 신청 내역 조회
- **엔드포인트**: `/api/enrollments/*`

## 🔧 공통 설정

### 기본 URL
```
http://localhost:8080/api
```

### 공통 헤더
```http
Content-Type: application/json
Accept: application/json
```

### 공통 응답 형식

#### 성공 응답
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

#### 에러 응답
```json
{
  "error": "에러 유형",
  "message": "에러 메시지",
  "details": {
    "field": "구체적인 에러 내용"
  }
}
```

### HTTP 상태 코드
- `200 OK`: 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 잘못된 요청
- `404 Not Found`: 리소스 없음
- `500 Internal Server Error`: 서버 오류

## 🧪 테스트 방법

### 1. cURL 명령어
각 컨트롤러 문서에 cURL 예시가 포함되어 있습니다.

### 2. 테스트 스크립트
```bash
# 회원 관리 API 테스트
./test-member-api.sh

# 강의 관리 API 테스트 (예정)
./test-course-api.sh

# 수강 신청 API 테스트 (예정)
./test-enrollment-api.sh
```

### 3. Postman 컬렉션
`docs/api/postman-collection.json` 파일을 Postman에 import하여 사용할 수 있습니다.

## 📝 개발 가이드

### 새로운 컨트롤러 추가 시
1. 컨트롤러 클래스 생성
2. 해당 컨트롤러 문서 생성 (`{controller-name}-controller.md`)
3. 테스트 스크립트 생성
4. Postman 컬렉션 업데이트
5. 이 README에 컨트롤러 추가

### 문서 업데이트 규칙
- API 변경 시 해당 컨트롤러 문서 즉시 업데이트
- 새로운 엔드포인트 추가 시 예시 코드 포함
- 에러 케이스 문서화 필수

## 🔗 관련 링크

- [메인 프로젝트 README](../../README.md)
- [API 테스트 스크립트](../api/test-scripts/)
- [Postman 컬렉션](../api/postman-collection.json)

---

**WB Education Task Management System** - 체계적인 API 문서화로 개발 효율성을 높입니다! 🚀
