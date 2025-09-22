# 🌍 환경 설정 가이드

## 📋 프로젝트 구성 개요

**WB Education Task Management System**은 **과제용 단순 구성**으로 설계되었습니다.

| 구성 요소 | 설정 | 설명 |
|-----------|------|------|
| **Redis** | Docker Redis 7-alpine | 동시성 제어, ZSet 랭킹 |
| **데이터베이스** | H2 TCP 서버 (포트 9092) | 외부 접근 가능한 인메모리 DB |
| **설정 파일** | `application.yml` 단일 파일 | 과제용 단순 구성 |
| **테스트** | `application-test.yml` | 테스트 전용 설정 |

## 🚀 실행 방법

### 1단계: Redis 컨테이너 실행 (필수)

```bash
# Docker Redis 시작
docker-compose up -d

# Redis 연결 확인
docker exec wb-edutask-redis redis-cli ping   # PONG 출력되면 정상
```

### 2단계: 애플리케이션 실행

```bash
# 기본 실행 (application.yml 사용)
./gradlew bootRun

# 또는 JAR 실행
./gradlew build
java -jar build/libs/wb-edutask-1.0.0.jar
```

### 3단계: 접속 확인 (서버 실행 후)

**⚠️ 주의: 아래 명령어들은 서버가 완전히 실행된 후에 동작합니다**

**서버 실행 완료 대기 (약 10-20초)**
- 로그에서 "Started WbEdutaskApplication" 메시지 확인

**1. 애플리케이션 상태 확인**
```bash
curl http://localhost:8080/actuator/health
```

**2. 강의 목록 확인 (ZSet 랭킹)**
```bash
curl "http://localhost:8080/api/v1/courses?sort=applicants"
```

**3. 웹 UI 접속**
```bash
open http://localhost:8080
```

## 🔧 핵심 설정

### application.yml (메인 설정)

```yaml
# 서버 설정
server:
  port: 8080

# Redis 설정 (Docker Redis 필수)
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      client-type: jedis

# H2 TCP 서버 설정
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console
```

### application-test.yml (테스트 설정)

```yaml
# 테스트용 간소화 설정
spring:
  jpa:
    show-sql: false  # 테스트 시 SQL 로그 비활성화
    hibernate:
      ddl-auto: create-drop
```

## 🐳 Docker Redis 설정

### docker-compose.yml

```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    container_name: wb-edutask-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped
```

### Redis 관리 명령어

```bash
# Redis 시작
docker-compose up -d

# Redis 상태 확인
docker ps | grep redis

# Redis CLI 접속
docker exec -it wb-edutask-redis redis-cli

# Redis 중지
docker-compose down
```

## 🗄️ H2 TCP 서버

### 자동 구성 (H2ServerConfig)

```java
@Configuration
public class H2ServerConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws SQLException {
        return Server.createTcpServer(
            "-tcp", "-tcpAllowOthers", "-tcpPort", "9092"
        );
    }
}
```

### H2 콘솔 접속

**방법 1: 웹 콘솔**
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:tcp://localhost:9092/mem:testdb`
- **Username**: `sa`
- **Password**: (비어있음)

**방법 2: 외부 DB 클라이언트**
- **Host**: localhost
- **Port**: 9092
- **Database**: mem:testdb
- **Username**: sa

## 🧪 테스트 실행

### 전체 테스트

```bash
# 모든 테스트 실행
./gradlew test

# 테스트 결과 확인
open build/reports/tests/test/index.html
```

### 동시성 테스트

```bash
# Redis 동시성 테스트 (50명 동시 수강신청)
./gradlew test --tests ConcurrencyTest

# 비동기 처리 테스트
./gradlew test --tests AsyncEnrollmentTest
```

### 컨트롤러 테스트

```bash
# 강의 API 테스트
./gradlew test --tests CourseControllerTest

# 수강신청 API 테스트
./gradlew test --tests EnrollmentControllerTest

# 회원 API 테스트
./gradlew test --tests MemberControllerTest
```

## 🌐 웹 UI 접속

### 메인 페이지들

- **강의 목록**: http://localhost:8080/
- **회원 관리**: http://localhost:8080/members
- **회원가입**: http://localhost:8080/signup
- **강의등록**: http://localhost:8080/course-register
- **신청현황**: http://localhost:8080/enrollment

### 관리 도구

- **H2 콘솔**: http://localhost:8080/h2-console
- **액추에이터**: http://localhost:8080/actuator
- **헬스체크**: http://localhost:8080/actuator/health
- **앱 정보**: http://localhost:8080/actuator/info
- **메트릭스**: http://localhost:8080/actuator/metrics

## 🚨 문제 해결

### Redis 연결 실패

```bash
# 1. Redis 컨테이너 확인
docker ps | grep redis

# 2. Redis 재시작
docker-compose restart redis

# 3. Redis 로그 확인
docker logs wb-edutask-redis

# 4. 포트 충돌 확인
netstat -an | grep 6379
```

### H2 TCP 서버 오류

```bash
# 1. H2 포트 확인
netstat -an | grep 9092

# 2. 애플리케이션 재시작
./gradlew bootRun

# 3. H2 로그 확인 (애플리케이션 로그에서)
tail -f logs/application.log | grep H2
```

### 초기 데이터 문제

```bash
# 1. 데이터 초기화 확인
curl http://localhost:8080/api/v1/courses | jq '.content | length'

# 2. H2 콘솔에서 직접 확인
# SELECT COUNT(*) FROM COURSES;
# SELECT COUNT(*) FROM MEMBERS;
# SELECT COUNT(*) FROM ENROLLMENTS;
```

## 📊 성능 모니터링

### Redis 모니터링

```bash
# Redis 정보
docker exec wb-edutask-redis redis-cli info

# ZSet 랭킹 확인
docker exec wb-edutask-redis redis-cli zrange course:ranking:applicants 0 -1 withscores

# 메모리 사용량
docker exec wb-edutask-redis redis-cli info memory
```

### 애플리케이션 모니터링 (서버 실행 중일 때)

**⚠️ 전제조건: 서버가 실행 중이어야 합니다 (`./gradlew bootRun`)**

**1. 서버 실행 상태 확인**
```bash
curl -f http://localhost:8080/actuator/health || echo "서버가 실행되지 않았습니다"
```

**2. 헬스체크 (DB, Redis 연결 상태 포함)**
```bash
curl http://localhost:8080/actuator/health
```

**3. 애플리케이션 정보**
```bash
curl http://localhost:8080/actuator/info
```

**4. 사용 가능한 메트릭스 목록**
```bash
curl http://localhost:8080/actuator/metrics
```

**5. JVM 메모리 사용량**
```bash
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

**6. HTTP 요청 수 (API 호출 후 확인)**
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
```

## 🎯 개발 팁

### 로그 레벨 조정

```yaml
# application.yml에 추가
logging:
  level:
    com.wb.edutask: DEBUG
    org.springframework.data.redis: DEBUG
```

### 개발 도구 활용

```bash
# 자동 재시작 (Spring Boot DevTools)
./gradlew bootRun

# 코드 변경 시 자동 컴파일
./gradlew build --continuous
```

### API 테스트 (서버 실행 후)

**⚠️ 전제조건: 서버 실행 + 초기 데이터 생성 완료**

**1. 서버 및 초기 데이터 확인**
```bash
curl -s http://localhost:8080/api/v1/courses | jq '.totalElements' || echo "서버 미실행 또는 jq 미설치"
```

**2. 강의 목록 (ZSet 랭킹)**
```bash
curl "http://localhost:8080/api/v1/courses?sort=applicants&page=0&size=20"
```

**3. 수강신청 (동시성 테스트)**
```bash
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Content-Type: application/json" \
  -d '{"studentId": 1, "courseIds": [1, 2, 3]}'
```

**4. 회원 검색**
```bash
curl "http://localhost:8080/api/v1/members?search=김&memberType=STUDENT"
```

**5. 웹 UI에서 직접 테스트 (권장)**
```bash
open http://localhost:8080
```

## ✅ 체크리스트

### 실행 전 확인사항

- [ ] Docker가 설치되어 있는가?
- [ ] `docker-compose up -d`로 Redis가 실행되었는가?
- [ ] `docker exec wb-edutask-redis redis-cli ping`이 PONG을 반환하는가?
- [ ] Java 21이 설치되어 있는가?

### 실행 후 확인사항

- [ ] http://localhost:8080 접속이 되는가?
- [ ] H2 콘솔 접속이 되는가?
- [ ] 초기 데이터가 생성되었는가? (50개 강의, 30명 회원)
- [ ] Redis ZSet 랭킹이 동작하는가?

---

**WB Education** - 단순하지만 강력한 과제용 시스템! 🚀

**핵심**: Docker Redis + H2 TCP + 단일 설정 파일로 **최대 성능, 최소 복잡도**