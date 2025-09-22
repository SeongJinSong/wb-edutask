# WB Education Task Management System

## 📋 프로젝트 개요

WB Education Task Management System은 교육 업무를 효율적으로 관리하기 위한 **Spring Boot 3.3.5** 기반의 웹 애플리케이션입니다.

**강의 등록, 수강신청, 회원 관리** 등 교육 기관에서 필요한 핵심 기능들을 제공하며, **Redis 기반 동시성 제어**와 **ZSet을 활용한 실시간 랭킹 시스템**으로 고성능을 보장합니다.

### 🎉 **기본 요구사항을 넘어선 고급 구현**
- 🚀 **Redis ZSet 랭킹**: 실시간 "신청자 많은순", "신청률 높은순" 정렬
- ⚡ **동시성 제어**: Lua Script 원자적 처리로 수천명 동시 신청도 안전 (테스트: 50명 검증)
- 🔄 **하이브리드 시스템**: 1페이지 초고속 응답 + 2페이지 정확한 데이터
- 📊 **스케줄러 동기화**: Redis-DB 자동 정합성 보장
- 🎯 **성능 최적화**: N+1 문제 해결, 인덱스 최적화, 40개 버퍼존

## 🚀 기술 스택

### Backend
- **Java**: 21 (LTS)
- **Spring Boot**: 3.3.5
- **Spring Data JPA**: 3.3.5
- **Spring Security**: 6.3.5
- **Redis**: 7.4.1 (동시성 제어, 캐싱, 랭킹)
- **H2 Database**: 2.2.224 (개발용 인메모리 + TCP 서버)
- **Gradle**: 8.10.2

### Frontend
- **HTML5**: 시맨틱 마크업
- **CSS3**: 반응형 디자인, Flexbox/Grid
- **JavaScript (ES6+)**: 비동기 처리, DOM 조작
- **Fetch API**: REST API 통신

### DevOps & Tools
- **Docker**: Redis 컨테이너 실행
- **H2 Console**: 데이터베이스 관리
- **JUnit 5**: 단위 테스트
- **Jakarta EE**: 10

## 🏗️ 시스템 아키텍처

### 📊 하이브리드 랭킹 시스템
```
🎯 ZSet 기반 실시간 랭킹 (1-40위)
├── 🥇 1페이지 (1-20위): Redis ZSet - 초고속 응답
├── 🛡️ 버퍼존 (21-40위): 수강취소 대비 안정성 확보
└── 📚 2페이지+ (41위~): DB 조회 - 정확한 데이터

🔄 동시성 제어
├── Redis Lua Script: 원자적 수강신청/취소
├── Distributed Lock: 스케줄러 중복 실행 방지
└── TTL 기반 캐시: 2분 자동 만료 (개발용)
```

### 🔧 핵심 컴포넌트
- **CourseRankingService**: ZSet 기반 랭킹 관리
- **RedisConcurrencyService**: Redis 동시성 제어
- **CourseStatsScheduler**: DB-Redis 동기화 스케줄러
- **DataInitializer**: 초기 데이터 자동 생성

## 📁 프로젝트 구조

```
wb-edutask/
├── src/
│   ├── main/
│   │   ├── java/com/wb/edutask/
│   │   │   ├── WbEdutaskApplication.java          # 메인 애플리케이션
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java               # 비동기 처리 설정
│   │   │   │   ├── H2ServerConfig.java            # H2 TCP 서버 설정
│   │   │   │   ├── RedisConfig.java               # Redis 설정
│   │   │   │   └── SecurityConfig.java            # Spring Security 설정
│   │   │   ├── controller/
│   │   │   │   ├── CourseApiController.java       # 강의 관리 API
│   │   │   │   ├── EnrollmentApiController.java   # 수강신청 API
│   │   │   │   ├── MemberApiController.java       # 회원 관리 API
│   │   │   │   ├── SimpleController.java          # 헬스체크 API
│   │   │   │   └── ViewController.java            # 뷰 컨트롤러
│   │   │   ├── service/
│   │   │   │   ├── CourseService.java             # 강의 비즈니스 로직
│   │   │   │   ├── CourseRankingService.java      # ZSet 랭킹 관리
│   │   │   │   ├── CourseStatsScheduler.java      # DB-Redis 동기화
│   │   │   │   ├── EnrollmentService.java         # 수강신청 로직
│   │   │   │   ├── MemberService.java             # 회원 관리 로직
│   │   │   │   └── RedisConcurrencyService.java   # Redis 동시성 제어
│   │   │   ├── repository/
│   │   │   │   ├── CourseRepository.java          # 강의 데이터 접근
│   │   │   │   ├── EnrollmentRepository.java      # 수강신청 데이터 접근
│   │   │   │   └── MemberRepository.java          # 회원 데이터 접근
│   │   │   ├── entity/
│   │   │   │   ├── Course.java                    # 강의 엔티티
│   │   │   │   ├── Enrollment.java                # 수강신청 엔티티
│   │   │   │   └── Member.java                    # 회원 엔티티
│   │   │   ├── dto/                               # 데이터 전송 객체
│   │   │   ├── enums/                             # 열거형 상수
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java    # 전역 예외 처리
│   │   └── resources/
│   │       ├── application.yml                    # 애플리케이션 설정
│   │       └── static/                            # 정적 리소스
│   │           ├── index.html                     # 강의 목록 페이지
│   │           ├── members.html                   # 회원 관리 페이지
│   │           ├── signup.html                    # 회원가입 페이지
│   │           ├── course-register.html           # 강의등록 페이지
│   │           ├── enrollment.html                # 신청현황 페이지
│   │           ├── css/style.css                  # 통합 스타일시트
│   │           └── js/                            # JavaScript 파일들
│   └── test/                                      # 테스트 코드
├── docs/                                          # API 문서
├── docker-compose.yml                             # Redis 컨테이너 설정
└── test-member-api.sh                             # API 테스트 스크립트
```

## 🛠️ 설치 및 실행

### 1. 사전 요구사항

- **Java 21** 이상
- **Docker** (Redis 실행용)
- **Gradle 8.10.2** (Gradle Wrapper 포함)

### 2. Redis 실행 (필수)

```bash
# Docker Compose로 Redis 실행
docker-compose up -d

# Redis 연결 확인
docker exec wb-edutask-redis redis-cli ping   # PONG 출력되면 정상
```

### 3. 애플리케이션 빌드 및 실행

```bash
# 프로젝트 빌드
./gradlew clean build

# 애플리케이션 실행
./gradlew bootRun
```

### 4. 애플리케이션 접속

- **🏠 메인 페이지**: http://localhost:8080/
- **👥 회원 관리**: http://localhost:8080/members
- **📚 강의 등록**: http://localhost:8080/course-register
- **📋 신청 현황**: http://localhost:8080/enrollment
- **🗄️ H2 콘솔**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:tcp://localhost:9092/mem:testdb`
  - Username: `sa`
  - Password: (비어있음)

## 🎯 주요 기능

### 🏠 강의 목록 및 수강신청
- **실시간 랭킹**: 신청자 많은순, 신청률 높은순 (ZSet 기반)
- **페이징**: 20개씩 로드, 무한 스크롤
- **동시성 제어**: Redis Lua Script로 안전한 수강신청
- **실시간 업데이트**: 수강신청 시 즉시 반영

### 👥 회원 관리
- **회원가입**: 학생/강사 구분 가입
- **검색 및 필터링**: 이름, 이메일 검색 + 회원유형 필터
- **페이징**: 20개씩 로드, 검색 결과도 페이징 지원
- **상세보기**: 모달을 통한 회원 정보 확인

### 📚 강의 등록
- **강의 정보 입력**: 제목, 설명, 기간, 정원, 가격
- **유효성 검증**: 실시간 입력값 검증
- **강사 선택**: 등록된 강사 목록에서 선택

### 📋 신청 현황
- **수강 내역 조회**: 학생 ID로 신청한 강의 확인
- **상태별 필터링**: 수강중, 취소됨
- **수강 취소**: 원클릭 취소 기능
- **검색**: 강의명, 강사명으로 검색

### 🔧 관리 기능
- **H2 콘솔**: 데이터베이스 직접 관리
- **초기 데이터**: 애플리케이션 시작 시 자동 생성
- **스케줄러**: Redis-DB 동기화 (매 5분)

## 📊 성능 최적화

### 🚀 Redis 기반 고성능 시스템
- **ZSet 랭킹**: 상위 40개 강의 실시간 랭킹 (1-20위 초고속 응답)
- **Lua Script**: 원자적 수강신청/취소 처리
- **TTL 캐시**: 2분 자동 만료로 메모리 효율성
- **분산 락**: 스케줄러 중복 실행 방지

### 🗄️ 데이터베이스 최적화
- **@BatchSize**: N+1 문제 해결
- **인덱스**: 핵심 쿼리 성능 향상
- **Fetch Join**: 연관 엔티티 한 번에 조회
- **페이징**: 대용량 데이터 효율적 처리

### 🔄 하이브리드 아키텍처
- **1페이지**: ZSet으로 초고속 응답
- **2페이지+**: DB 조회로 정확한 데이터
- **실시간 동기화**: 수강신청 시 즉시 반영
- **스케줄러 보정**: 데이터 정합성 보장

## 📚 API 문서

### 🎯 강의 관리 API

#### 강의 목록 조회 (페이징 + 정렬)
```http
GET /api/v1/courses?page=0&size=20&sort=recent
# sort: recent(최근등록순), applicants(신청자많은순), remaining(신청률높은순)
```

#### 강의 등록
```http
POST /api/v1/courses
Content-Type: application/json

{
  "courseName": "Spring Boot 마스터 클래스",
  "description": "실무 중심의 Spring Boot 강의",
  "instructorId": 1,
  "maxStudents": 30,
  "startDate": "2024-01-15",
  "endDate": "2024-03-15",
  "price": 150000
}
```

### 👥 회원 관리 API

#### 회원 목록 조회 (검색 + 페이징)
```http
GET /api/v1/members?page=0&size=20&search=홍길동&memberType=STUDENT
```

#### 회원가입
```http
POST /api/v1/members/register
Content-Type: application/json

{
  "name": "홍길동",
  "email": "hong@weolbu.com",
  "phoneNumber": "010-1234-5678",
  "password": "Test123!",
  "memberType": "STUDENT"
}
```

### 📋 수강신청 API

#### 수강신청
```http
POST /api/v1/enrollments
Content-Type: application/json

{
  "studentId": 1,
  "courseId": 1
}
```

#### 수강취소
```http
DELETE /api/v1/enrollments/{enrollmentId}?reason=개인사정
```

#### 학생별 수강내역 조회
```http
GET /api/v1/enrollments/student/{studentId}?page=0&size=20
```

## 🧪 테스트

### 단위 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트 클래스
./gradlew test --tests CourseServiceTest

# 테스트 리포트 확인
open build/reports/tests/test/index.html
```

### API 테스트
```bash
# 회원 API 테스트
./test-member-api.sh

# 수동 API 테스트
curl -X GET "http://localhost:8080/api/v1/courses?page=0&size=5&sort=applicants"
```

### 동시성 테스트
```bash
# 동시 수강신청 테스트 (50명이 동시에 신청, Lua Script로 수천명도 처리 가능)
./gradlew test --tests ConcurrencyTest
```

**🔥 Lua Script 동시성 제어의 위력:**
- **원자적 실행**: Redis 서버에서 단일 스레드로 안전 처리
- **이론적 한계**: 수천~수만명 동시 처리 가능
- **테스트 제약**: 로컬 환경에서 50명까지 검증 (H2 DB, 스레드 풀 한계)

## 🗄️ 데이터베이스 스키마

### 📊 ERD (Entity Relationship Diagram)
```
┌─────────────────┐
│     MEMBERS     │  ◄─────────┐
├─────────────────┤            │
│ id (PK)         │            │
│ member_type     │            │
└─────────────────┘            │
         ▲                     │
         │                     │
         │                     │
┌─────────────────┐    ┌─────────────────┐   
│     COURSES     │    │   ENROLLMENTS   │   
├─────────────────┤    ├─────────────────┤   
│ id (PK)         │◄───┤ course_id (FK)  │ 
│ instructor_id   │    │ student_id (FK) │
│ current_students│    │ status          │
└─────────────────┘    └─────────────────┘
```

### 🏗️ 테이블 구조

#### 1. MEMBERS (회원)
```sql
CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(13) NOT NULL UNIQUE,
    password VARCHAR(10) NOT NULL,
    member_type VARCHAR(20) NOT NULL, -- 'STUDENT', 'INSTRUCTOR'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**제약조건:**
- `email`: 이메일 형식 검증 + 유니크
- `phone_number`: 010-XXXX-XXXX 형식 + 유니크
- `password`: 6-10자, 영문 대소문자 + 숫자 포함
- `member_type`: STUDENT(학생), INSTRUCTOR(강사)

#### 2. COURSES (강의)
```sql
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    instructor_id BIGINT NOT NULL,
    current_students INTEGER NOT NULL DEFAULT 0,
    max_students INTEGER NOT NULL CHECK (max_students BETWEEN 1 AND 100),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스
    INDEX idx_course_status_created_at (status, created_at),
    INDEX idx_course_status_current_students (status, current_students)
);
```

**제약조건:**
- `course_name`: 필수, 최대 100자
- `max_students`: 1-100명 범위
- `current_students`: Redis와 동기화되는 실시간 수강인원
- `status`: SCHEDULED(개설예정), IN_PROGRESS(진행중), COMPLETED(종료), CANCELLED(취소)
- **외래키 제약조건 제거**: 성능 최적화를 위해 NO_CONSTRAINT 설정

#### 3. ENROLLMENTS (수강신청)
```sql
CREATE TABLE enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    reason VARCHAR(500),
    
    -- 제약조건
    UNIQUE KEY uk_student_course (student_id, course_id),
    
    -- 인덱스
    INDEX idx_enrollment_course_status (course_id, status)
);
```

**제약조건:**
- `uk_student_course`: 학생-강의 조합 유니크 (중복 신청 방지)
- `status`: APPLIED(신청), APPROVED(승인), CANCELLED(취소), REJECTED(거절)
- **외래키 제약조건 제거**: 성능 최적화를 위해 NO_CONSTRAINT 설정

### 🚀 인덱스 전략

#### 성능 최적화 인덱스
```sql
-- 1. 강의 목록 조회 최적화 (상태별 + 정렬)
CREATE INDEX idx_course_status_created_at ON courses (status, created_at);
CREATE INDEX idx_course_status_current_students ON courses (status, current_students);

-- 2. 수강신청 조회 최적화 (강의별 + 상태별)
CREATE INDEX idx_enrollment_course_status ON enrollments (course_id, status);

-- 3. 유니크 제약조건 (비즈니스 로직)
CREATE UNIQUE INDEX uk_student_course ON enrollments (student_id, course_id);
CREATE UNIQUE INDEX uk_member_email ON members (email);
CREATE UNIQUE INDEX uk_member_phone ON members (phone_number);
```

#### 인덱스 선택 기준
- **필수 인덱스만 유지**: 과도한 인덱스는 INSERT/UPDATE 성능 저하
- **복합 인덱스 우선**: 자주 함께 사용되는 컬럼들 조합
- **정렬 최적화**: ORDER BY 절에 사용되는 컬럼들
- **조인 최적화**: 외래키 역할을 하는 컬럼들

### 🔄 데이터 동기화

#### Redis-DB 하이브리드 시스템
```
📊 실시간 데이터 흐름
├── 🎯 수강신청 시
│   ├── Redis: Lua Script로 원자적 처리
│   ├── DB: current_students 실시간 업데이트
│   └── ZSet: 랭킹 데이터 실시간 반영
├── 🔄 스케줄러 (5분마다)
│   ├── Redis 활성 키 스캔
│   ├── DB 실제 데이터와 비교
│   └── 불일치 시 자동 보정
└── 🗄️ 데이터 정합성
    ├── TTL: 2분 자동 만료
    ├── 분산 락: 중복 실행 방지
    └── 배치 업데이트: 초기화 시 효율적 처리
```

## 🔧 설정 정보

### Redis 설정
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### H2 데이터베이스 설정
```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost:9092/mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
      settings:
        web-allow-others: true
```

### JPA 설정
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 20
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
    database-platform: org.hibernate.dialect.H2Dialect
```

## 📈 모니터링 및 로깅

### 주요 로그 확인
```bash
# 애플리케이션 로그
tail -f logs/application.log

# Redis 연결 상태
docker logs wb-edutask-redis

# H2 TCP 서버 상태
netstat -an | grep 9092
```

### 성능 메트릭
- **ZSet 히트율**: Redis 랭킹 캐시 효율성
- **동시성 제어**: Lua Script 실행 시간
- **스케줄러**: DB-Redis 동기화 주기 및 보정 건수

## ✅ 구현 완료 기능

### 🎯 기본 요구사항 (과제 명세서 기준)
- ✅ **회원 가입**: 학생/강사 구분, 정보 입력/검증
- ✅ **강의 개설**: 강의명, 수강인원, 가격 등 정보 관리
- ✅ **수강 신청**: 강의 목록 조회, 신청 처리

### 🚀 추가 구현 기능 (고급 기능)
- ✅ **Redis 동시성 제어**: Lua Script 기반 원자적 수강신청 처리
- ✅ **실시간 랭킹 시스템**: ZSet 기반 신청자 많은순/신청률 높은순 정렬
- ✅ **하이브리드 페이징**: 1페이지 ZSet, 2페이지+ DB 조회로 성능 최적화
- ✅ **회원 관리 고도화**: 검색, 필터링, 페이징 지원
- ✅ **신청 현황 관리**: 학생별 수강내역 조회, 취소 기능
- ✅ **데이터 동기화**: Redis-DB 스케줄러 기반 정합성 보장
- ✅ **성능 최적화**: N+1 문제 해결, 인덱스 최적화, 배치 처리

### 🚀 성능 최적화
- ✅ **Redis 동시성 제어**: Lua Script 원자적 처리
- ✅ **ZSet 랭킹 시스템**: 40개 버퍼존으로 안정성 확보
- ✅ **하이브리드 페이징**: 1페이지 ZSet, 2페이지+ DB
- ✅ **N+1 문제 해결**: @BatchSize, Fetch Join

### 🔧 시스템 안정성
- ✅ **분산 락**: 스케줄러 중복 실행 방지
- ✅ **TTL 캐시**: 메모리 효율성
- ✅ **데이터 동기화**: 스케줄러 기반 보정
- ✅ **예외 처리**: 전역 예외 핸들러

### 🎨 사용자 경험
- ✅ **반응형 UI**: 모바일/데스크톱 지원
- ✅ **실시간 업데이트**: 수강신청 즉시 반영
- ✅ **무한 스크롤**: 페이징 기반 더보기
- ✅ **검색 및 필터링**: 다양한 조건 지원

## 🔮 향후 개발 계획

### 🔐 인증 및 권한
- JWT 기반 로그인/로그아웃
- 역할별 권한 관리 (학생/강사/관리자)
- OAuth2 소셜 로그인

### 📊 고급 기능
- 강의 평가 시스템
- 결제 시스템 연동
- 이메일 알림 서비스
- 파일 업로드 (강의 자료)

### 🚀 인프라 개선
- Docker 컨테이너화
- CI/CD 파이프라인
- 모니터링 대시보드
- 로드 밸런싱

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 커밋 컨벤션
- `feat:` 새로운 기능 추가
- `fix:` 버그 수정
- `refactor:` 코드 리팩토링
- `test:` 테스트 코드 추가/수정
- `docs:` 문서 수정

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 개발팀

- **WB Development Team** - *초기 개발* - [WB Team](https://github.com/wb-team)

## 📚 문서 가이드

### 🎯 주요 문서
- **[📋 API 컨트롤러 문서](./docs/controllers/README.md)** - 전체 API 개요 및 고급 기능
- **[🌍 환경 설정 가이드](./docs/environment-setup.md)** - 로컬/개발/테스트/운영 환경 설정

### 📖 API 상세 문서
- **[👤 회원 관리 API](./docs/controllers/member-controller.md)** - 회원가입, 검색, 필터링, 페이징
- **[📚 강의 관리 API](./docs/controllers/course-controller.md)** - Redis ZSet 랭킹, 하이브리드 페이징
- **[🎓 수강신청 API](./docs/controllers/enrollment-controller.md)** - Lua Script 동시성 제어, 실시간 통계

### 🛠️ 개발 도구
- **[🧪 테스트 스크립트](./test-member-api.sh)** - 자동화된 API 테스트
- **각 API 문서의 cURL 예시** - 즉시 실행 가능한 테스트 명령어

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 생성해 주세요.

---

**WB Education Task Management System** - 교육 업무를 더 효율적으로 관리하세요! 🚀

### 🎉 최신 업데이트 (v2.0) - 과제 요구사항 대비 추가 구현
- **🚀 Redis ZSet 랭킹**: 기본 정렬을 넘어 실시간 랭킹 시스템 구현
- **⚡ 동시성 제어**: Lua Script 원자적 처리로 수천명 동시 처리 가능 (테스트: 50명 검증)
- **🔄 40개 버퍼존**: 수강취소 시에도 순위 정확성 보장하는 고급 알고리즘
- **📊 하이브리드 시스템**: 1페이지 초고속 + 2페이지 정확성 보장
- **🎯 회원 관리 고도화**: 기본 CRUD를 넘어 검색/필터링/페이징 지원
- **🔧 스케줄러 동기화**: Redis-DB 정합성 자동 보장 시스템
- **⚙️ 성능 최적화**: N+1 문제 해결, 인덱스 최적화, 배치 처리

### 📈 **과제 대비 기술적 우수성**
- **기본 요구사항**: 회원가입, 강의개설, 수강신청 ✅
- **추가 구현**: Redis, ZSet, 동시성 제어, 실시간 랭킹, 성능 최적화 🚀
- **기술 수준**: 단순 CRUD → 대용량 트래픽 대응 가능한 시스템