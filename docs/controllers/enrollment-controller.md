# 🎓 수강 신청 API

## 📋 개요

수강 신청, 신청 취소, 신청 내역 조회를 담당하는 API입니다.

- **컨트롤러**: `EnrollmentController` (예정)
- **기본 경로**: `/api/enrollments`
- **기능**: 수강 신청, 신청 취소, 신청 내역 조회

## 🚧 개발 예정

이 API는 아직 개발되지 않았습니다. 수강 신청 기능 구현 시 업데이트될 예정입니다.

## 📝 예상 기능

### 1. 수강 신청
- **POST** `/api/enrollments/register`
- 여러 강의 동시 신청 가능
- 선착순 처리 (최대 수강 인원까지)

### 2. 신청 내역 조회
- **GET** `/api/enrollments` - 사용자별 신청 내역
- **GET** `/api/enrollments/{id}` - 특정 신청 내역 조회

### 3. 신청 취소
- **DELETE** `/api/enrollments/{id}` - 수강 신청 취소

### 4. 강의 목록 조회
- **GET** `/api/courses` - 강의 목록 (페이징, 정렬)
  - 최근 등록순
  - 신청자 많은 순
  - 신청률 높은 순

## 🔗 관련 파일

- **컨트롤러**: `src/main/java/com/wb/edutask/controller/EnrollmentController.java` (예정)
- **서비스**: `src/main/java/com/wb/edutask/service/EnrollmentService.java` (예정)
- **엔티티**: `src/main/java/com/wb/edutask/entity/Enrollment.java` (예정)
- **테스트**: `src/test/java/com/wb/edutask/service/EnrollmentServiceTest.java` (예정)

---

**수강 신청 API** - 편리한 수강 신청 시스템! 🎓
