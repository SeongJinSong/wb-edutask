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
│   │   │       │   └── HealthController.java          # 헬스 체크 API
│   │   │       ├── entity/                            # JPA 엔티티 (비어있음)
│   │   │       └── repository/                        # 데이터 접근 계층 (비어있음)
│   │   └── resources/
│   │       └── application.yml                        # 애플리케이션 설정
│   └── test/
│       └── java/
│           └── com/wb/edutask/
│               └── WbEdutaskApplicationTests.java     # 기본 테스트 클래스
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

### 현재 사용 가능한 API

현재 프로젝트는 **기본 Spring Boot 애플리케이션**으로 구성되어 있으며, 다음 API만 사용 가능합니다.

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

### 🎯 **기본 구성**
- **Spring Boot 3.3.5** 기본 애플리케이션
- **H2 인메모리 데이터베이스** 설정 완료
- **Spring Data JPA** 의존성 포함
- **헬스 체크 API** 제공

### 📁 **패키지 구조**
- `com.wb.edutask.controller`: REST API 컨트롤러 (HealthController만 존재)
- `com.wb.edutask.entity`: JPA 엔티티 (비어있음)
- `com.wb.edutask.repository`: 데이터 접근 계층 (비어있음)

### 🚀 **향후 개발 예정**
- 업무 관리 엔티티 및 API 개발
- 사용자 관리 기능
- 인증 및 권한 관리
- 파일 업로드 기능

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
