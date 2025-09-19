# 📚 강의 관리 API

## 📋 개요

강의 등록, 조회, 수정, 삭제를 담당하는 API입니다.

- **컨트롤러**: `CourseController` (예정)
- **기본 경로**: `/api/courses`
- **기능**: 강의 등록, 강의 조회, 강의 수정/삭제

## 🚧 개발 예정

이 API는 아직 개발되지 않았습니다. 강의 등록 기능 구현 시 업데이트될 예정입니다.

## 📝 예상 기능

### 1. 강의 등록
- **POST** `/api/courses/register`
- 강사만 강의 등록 가능
- 강의명, 최대 수강 인원, 가격 입력

### 2. 강의 조회
- **GET** `/api/courses` - 강의 목록 조회 (페이징, 정렬)
- **GET** `/api/courses/{id}` - 특정 강의 조회

### 3. 강의 수정/삭제
- **PUT** `/api/courses/{id}` - 강의 정보 수정
- **DELETE** `/api/courses/{id}` - 강의 삭제

## 🔗 관련 파일

- **컨트롤러**: `src/main/java/com/wb/edutask/controller/CourseController.java` (예정)
- **서비스**: `src/main/java/com/wb/edutask/service/CourseService.java` (예정)
- **엔티티**: `src/main/java/com/wb/edutask/entity/Course.java` (예정)
- **테스트**: `src/test/java/com/wb/edutask/service/CourseServiceTest.java` (예정)

---

**강의 관리 API** - 체계적인 강의 관리 시스템! 📚
