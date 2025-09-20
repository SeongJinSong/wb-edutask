# 🌍 환경별 설정 가이드

## 📋 환경 구성 개요

| 환경 | 프로필 | Redis | 용도 | 설명 |
|------|--------|-------|------|------|
| **로컬** | `local` | 내장 Redis | 개인 개발 | 별도 설치 없이 바로 개발 가능 |
| **개발** | `dev` | 외부 Redis | 팀 개발/통합 | 실제 운영과 유사한 환경 |
| **테스트** | `test` | 내장 Redis | 자동화 테스트 | CI/CD에서 별도 설치 없이 테스트 |
| **운영** | `prod` | 외부 Redis | 실제 서비스 | 고가용성, 클러스터링 지원 |

## 🚀 환경별 실행 방법

### 1. 로컬 개발 환경 (local)
```bash
# 기본 실행 (내장 Redis 자동 시작)
./gradlew bootRun

# 또는 명시적 지정
./gradlew bootRun --args='--spring.profiles.active=local'
```

**특징:**
- ✅ Redis 설치 불필요
- ✅ 자동 포트 할당 (6379부터 시도)
- ✅ 메모리 제한 128MB
- ✅ 개발 도구 활성화 (LiveReload 등)

### 2. 개발 환경 (dev)
```bash
# Redis 서버 먼저 시작
redis-server

# 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# 환경변수로 Redis 설정 가능
REDIS_HOST=dev-redis REDIS_PORT=6379 ./gradlew bootRun --args='--spring.profiles.active=dev'
```

**특징:**
- 🔧 외부 Redis 서버 필요
- 🔧 환경변수로 Redis 설정 가능
- 🔧 운영과 유사한 커넥션 풀 설정
- 🔧 상세한 디버그 로깅

### 3. 테스트 환경 (test)
```bash
# 자동으로 test 프로필 사용
./gradlew test

# 특정 테스트 실행
./gradlew test --tests="ConcurrencyTest"
```

**특징:**
- ✅ 내장 Redis 자동 시작 (포트 6380)
- ✅ 테스트 격리 (각 테스트마다 독립적)
- ✅ 빠른 실행을 위한 최적화된 설정

### 4. 운영 환경 (prod)
```bash
# 환경변수 설정 후 실행
export REDIS_HOST=prod-redis-cluster
export REDIS_PORT=6379
export REDIS_PASSWORD=secure-password

./gradlew bootRun --args='--spring.profiles.active=prod'

# 또는 Docker 환경에서
docker run -e SPRING_PROFILES_ACTIVE=prod \
           -e REDIS_HOST=redis-cluster \
           -e REDIS_PORT=6379 \
           -e REDIS_PASSWORD=password \
           wb-edutask:latest
```

**특징:**
- 🚀 외부 Redis 클러스터 사용
- 🚀 고성능 커넥션 풀 (max-active: 20)
- 🚀 보안 강화 (비밀번호 인증)
- 🚀 로그 파일 저장

## 🔧 Redis 설치 가이드

### macOS (Homebrew)
```bash
# Redis 설치
brew install redis

# Redis 서버 시작
redis-server

# 백그라운드 서비스로 시작
brew services start redis
```

### Ubuntu/Debian
```bash
# Redis 설치
sudo apt update
sudo apt install redis-server

# Redis 서비스 시작
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

### Windows
```bash
# WSL2 사용 권장
wsl --install
# 이후 Ubuntu 환경에서 위 명령어 사용
```

### Docker
```bash
# Redis 컨테이너 실행
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 비밀번호 설정
docker run -d --name redis -p 6379:6379 redis:7-alpine redis-server --requirepass mypassword
```

## 🛠️ 개발 팁

### 1. 환경 전환
```bash
# IDE에서 환경 변수 설정
SPRING_PROFILES_ACTIVE=dev

# 또는 application.yml에서 기본값 변경
spring:
  profiles:
    active: dev
```

### 2. Redis 연결 확인
```bash
# Redis CLI 접속
redis-cli

# 연결 테스트
127.0.0.1:6379> ping
PONG

# 큐 상태 확인
127.0.0.1:6379> llen enrollment:queue
```

### 3. 로그 모니터링
```bash
# 실시간 로그 확인
tail -f logs/wb-edutask.log

# Redis 관련 로그만 필터링
tail -f logs/wb-edutask.log | grep -i redis
```

## 🚨 주의사항

1. **개발 환경**: Redis 서버가 실행 중이어야 함
2. **운영 환경**: Redis 클러스터 및 보안 설정 필수
3. **테스트 환경**: 포트 충돌 시 자동으로 다른 포트 사용
4. **로컬 환경**: 메모리 사용량 모니터링 권장

## 📊 성능 벤치마크

| 환경 | 동시 요청 | 처리 시간 | 메모리 사용량 |
|------|-----------|-----------|---------------|
| 로컬 | 100개 | ~500ms | ~256MB |
| 개발 | 500개 | ~800ms | ~512MB |
| 운영 | 1000개+ | ~1000ms | ~1GB+ |

---

**환경별 설정으로 개발부터 운영까지 안정적인 서비스를 제공합니다!** 🎯
