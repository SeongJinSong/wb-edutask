# WB Education Task Management System

## 📋 프로젝트 개요

WB Education Task Management System은 교육 업무를 효율적으로 관리하기 위한 Spring Boot 기반의 웹 애플리케이션입니다.

현재는 **기본 Spring Boot 애플리케이션**으로 구성되어 있으며, 헬스 체크 기능을 제공합니다.

## 🚀 기술 스택

- **Java**: 21 (LTS)
- **Spring Boot**: 3.3.5
- **Spring Data JPA**: 3.3.5
- **H2 Database**: 2.2.224 (인메모리)
- **Gradle**: 8.10.2
- **Jakarta EE**: 10

## 📁 프로젝트 구조

```
wb-edutask/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/wb/edutask/
│   │   │       ├── WbEdutaskApplication.java          # 메인 애플리케이션 클래스
│   │   │       ├── controller/
│   │   │       │   ├── HealthController.java          # 헬스 체크 API
│   │   │       │   └── MemberController.java          # 회원 관리 API
│   │   │       ├── service/
│   │   │       │   └── MemberService.java             # 회원 관리 서비스
│   │   │       ├── repository/
│   │   │       │   └── MemberRepository.java          # 회원 데이터 접근
│   │   │       ├── entity/
│   │   │       │   ├── Member.java                    # 회원 엔티티
│   │   │       │   └── MemberType.java                # 회원 유형 열거형
│   │   │       ├── dto/
│   │   │       │   ├── MemberRequestDto.java          # 회원 요청 DTO
│   │   │       │   └── MemberResponseDto.java         # 회원 응답 DTO
│   │   │       └── exception/
│   │   │           └── GlobalExceptionHandler.java    # 전역 예외 처리
│   │   └── resources/
│   │       └── application.yml                        # 애플리케이션 설정
│   └── test/
│       └── java/
│           └── com/wb/edutask/
│               ├── WbEdutaskApplicationTests.java     # 기본 테스트 클래스
│               ├── service/
│               │   └── MemberServiceTest.java         # 회원 서비스 테스트
│               └── controller/
│                   └── MemberControllerTest.java      # 회원 컨트롤러 테스트
├── docs/
│   ├── controllers/
│   │   ├── README.md                                  # 컨트롤러 전체 개요
│   │   ├── member-controller.md                       # 회원 관리 API 문서
│   │   ├── course-controller.md                       # 강의 관리 API 문서 (예정)
│   │   └── enrollment-controller.md                   # 수강 신청 API 문서 (예정)
│   └── api/
│       ├── postman-collection.json                    # Postman 컬렉션
│       └── test-scripts/                              # 테스트 스크립트들
├── test-member-api.sh                                 # 회원 API 테스트 스크립트
├── build.gradle                                       # Gradle 빌드 설정
├── settings.gradle                                    # Gradle 프로젝트 설정
├── gradle.properties                                  # Gradle 속성 설정
├── gradlew                                            # Gradle Wrapper (Unix)
├── gradlew.bat                                        # Gradle Wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar                         # Gradle Wrapper JAR
│       └── gradle-wrapper.properties                  # Gradle Wrapper 설정
└── README.md                                          # 프로젝트 문서
```

## 🛠️ 설치 및 실행

### 1. 사전 요구사항

- Java 21 이상
- Gradle 8.10.2 (Gradle Wrapper 포함)

### 2. 프로젝트 빌드

```bash
# 의존성 다운로드 및 컴파일
./gradlew clean compile

# 테스트 실행
./gradlew test

# 패키지 빌드
./gradlew clean build
```

### 3. 애플리케이션 실행

```bash
# Gradle을 통한 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/wb-edutask-1.0.0.jar
```

### 4. 애플리케이션 접속

- **애플리케이션**: http://localhost:8080/api
- **H2 콘솔**: http://localhost:8080/api/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: (비어있음)

## 📚 API 문서

### 📖 상세 API 문서

체계적인 API 문서는 다음 위치에서 확인할 수 있습니다:

- **[컨트롤러별 API 문서](./docs/controllers/README.md)** - 전체 API 개요
- **[회원 관리 API](./docs/controllers/member-controller.md)** - 회원가입, 조회 API
- **[강의 관리 API](./docs/controllers/course-controller.md)** - 강의 등록 API (개발 예정)
- **[수강 신청 API](./docs/controllers/enrollment-controller.md)** - 수강 신청 API (개발 예정)

### 🚀 현재 사용 가능한 API

#### 회원 관리 API

**1. 회원 가입**
```http
POST /api/members/register
Content-Type: application/json

{
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "phoneNumber": "010-1234-5678",
  "password": "Test123",
  "memberType": "STUDENT"
}
```

**2. 회원 조회**
```http
GET /api/members/{id}                    # ID로 조회
GET /api/members/email/{email}           # 이메일로 조회
```

**3. 테스트 실행**
```bash
# 전체 API 테스트
./test-member-api.sh

# 개별 테스트
curl -X POST http://localhost:8080/api/members/register \
  -H "Content-Type: application/json" \
  -d '{"name":"홍길동","email":"hong@weolbu.com","phoneNumber":"010-1234-5678","password":"Test123","memberType":"STUDENT"}'
```

### 헬스 체크 API

#### 1. 애플리케이션 상태 확인
```http
GET /api/health
```

#### 2. 간단한 상태 확인
```http
GET /api/health/ping
```

## 📊 현재 프로젝트 상태

### 🎯 **현재 구현된 기능**
- **Spring Boot 3.3.5** 기본 애플리케이션
- **H2 인메모리 데이터베이스** 설정 완료
- **Spring Data JPA** 의존성 포함
- **회원가입 기능** 구현 완료
- **헬스 체크 API** 제공

### 📁 **패키지 구조**
- `com.wb.edutask.controller`: REST API 컨트롤러 (HealthController, MemberController)
- `com.wb.edutask.entity`: JPA 엔티티 (Member, MemberType)
- `com.wb.edutask.repository`: 데이터 접근 계층 (MemberRepository)
- `com.wb.edutask.service`: 비즈니스 로직 (MemberService)
- `com.wb.edutask.dto`: 데이터 전송 객체 (MemberRequestDto, MemberResponseDto)
- `com.wb.edutask.exception`: 예외 처리 (GlobalExceptionHandler)

### 🚀 **향후 개발 예정**
- 강의 등록 기능
- 강의 신청 기능
- 인증 및 권한 관리
- 파일 업로드 기능

## ✅ 체크리스트

### 🚀 **애플리케이션 실행**
- ✅ **Spring Boot 애플리케이션 정상 실행** - `./gradlew bootRun`으로 실행 가능
- ✅ **H2 데이터베이스 연결** - 인메모리 DB로 자동 설정
- ✅ **API 엔드포인트 정상 동작** - 회원가입, 조회 API 테스트 완료

### 📋 **요구사항 구현**
- ✅ **회원가입 기능** - 수강생/강사 회원가입 지원
- 🚧 **강의 등록 기능** - 개발 예정
- 🚧 **강의 신청 기능** - 개발 예정

### ⚠️ **예외 상황 고려**
- ✅ **유효성 검증** - 이메일, 휴대폰 번호, 비밀번호 형식 검증
- ✅ **중복 검증** - 이메일, 휴대폰 번호 중복 방지
- ✅ **전역 예외 처리** - `GlobalExceptionHandler`로 통합 예외 처리
- ✅ **적절한 HTTP 상태 코드** - 200, 201, 400, 404, 500 응답

### 🏗️ **설계의 간결성과 효율성**
- ✅ **계층형 아키텍처** - Controller → Service → Repository 패턴
- ✅ **DTO 분리** - Request/Response DTO로 데이터 전송 최적화
- ✅ **JPA 활용** - 엔티티 매핑과 자동 쿼리 생성
- ✅ **의존성 주입** - Spring의 DI 컨테이너 활용

### 📚 **API 스펙 명확성**
- ✅ **RESTful API 설계** - HTTP 메서드와 상태 코드 적절히 사용
- ✅ **상세한 API 문서** - 컨트롤러별 상세 문서화
- ✅ **요청/응답 예시** - cURL, JSON 예시 포함
- ✅ **Postman 컬렉션** - API 테스트용 컬렉션 제공

### 🧪 **테스트 코드**
- ✅ **서비스 레이어 테스트** - `MemberServiceTest` (8개 테스트 통과)
- ✅ **통합 테스트** - `@SpringBootTest`로 전체 컨텍스트 테스트
- ✅ **API 테스트 스크립트** - `test-member-api.sh`로 실제 API 테스트
- ✅ **테스트 커버리지** - 정상/예외 케이스 모두 테스트

### 📖 **문서화**
- ✅ **메인 README** - 프로젝트 개요, 설치, 실행 방법
- ✅ **API 문서** - 컨트롤러별 상세 문서
- ✅ **코드 주석** - 한글 주석으로 가독성 향상
- ✅ **프로젝트 구조** - 명확한 디렉토리 구조 설명

### 🎁 **추가 구현 사항**
- ✅ **체계적인 문서화** - 컨트롤러별 README 분리
- ✅ **Postman 컬렉션** - API 테스트 도구 제공
- ✅ **테스트 스크립트** - 자동화된 API 테스트
- ✅ **예외 처리** - 전역 예외 핸들러로 일관된 에러 응답
- ✅ **유효성 검증** - Bean Validation으로 입력값 검증
- ✅ **한글 주석** - 코드 가독성 향상

## 🔧 설정 정보

### 데이터베이스 설정
- **타입**: H2 인메모리 데이터베이스
- **URL**: `jdbc:h2:mem:testdb`
- **사용자명**: `sa`
- **비밀번호**: (없음)

### JPA 설정
- **DDL 모드**: `update` (스키마 자동 업데이트)
- **SQL 로깅**: 활성화
- **SQL 포맷팅**: 활성화

## 🧪 테스트

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests WbEdutaskApplicationTests

# 테스트 리포트 확인
./gradlew test --info
```

## 📝 개발 가이드

### 코드 스타일
- Java 21 문법 사용
- 한글 주석 작성
- 명확한 변수명 사용
- 예외 처리 필수

### 패키지 구조
- `com.wb.edutask.controller`: REST API 컨트롤러 (HealthController만 존재)
- `com.wb.edutask.entity`: JPA 엔티티 (비어있음)
- `com.wb.edutask.repository`: 데이터 접근 계층 (비어있음)
- `com.wb.edutask.service`: 비즈니스 로직 (향후 추가)

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 개발팀

- **WB Development Team** - *초기 개발* - [WB Team](https://github.com/wb-team)

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.

---

**WB Education Task Management System** - 교육 업무를 더 효율적으로 관리하세요! 🚀
