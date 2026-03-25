# SoulPal — AI 개발 컨텍스트 문서

> 이 문서는 AI(Claude)가 사용자 없이도 이 프로젝트를 이해하고 자율적으로 개발할 수 있도록 작성된 컨텍스트 파일입니다.
> 새로운 결정이나 패턴이 추가될 때마다 이 문서를 업데이트하세요.

---

## 프로젝트 핵심 요약

SoulPal은 **로컬 LLM(Ollama llama3)** 기반 AI 캐릭터 대화 웹앱입니다.
외부 AI API를 사용하지 않는 것이 핵심 설계 원칙입니다 (프라이버시, 비용 0).

```
브라우저 → Nginx:8080 → Spring Boot:9090 → PostgreSQL:5432
                                          → Redis:6379
                                          → Ollama:11434
```

---

## 명령어 레퍼런스

### Makefile — 권장 진입점 (모든 명령어의 단일 창구)

```bash
make test-unit        # 백엔드 단위 + 아키텍처 테스트 (빠름, Docker 불필요)
make test-integration # 백엔드 통합 테스트 (Docker 필요, TestContainers 자동 실행)
make test-frontend    # 프론트엔드 Vitest
make test             # 위 셋 전부
make lint             # 프론트엔드 ESLint 검사
make lint-fix         # ESLint 자동 수정
make check            # lint + test-unit + test-frontend (PR 생성 전 필수)
make doctor           # 환경 진단 — 출력을 AI에게 붙여넣으면 상태 즉시 파악
make docker-rebuild   # 이미지 재빌드 후 서비스 재시작
```

### 직접 실행 (Makefile 없는 환경)

```bash
# 개발 서버
cd backend && ./gradlew bootRun -x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend
cd frontend && npm run dev               # Vite dev server :5173

# 테스트 — 단위
cd backend && ./gradlew test --tests "com.soulpal.service.*" --tests "com.soulpal.exception.*" --tests "com.soulpal.architecture.*" -x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend
cd frontend && npm run test

# 테스트 — 통합 (Docker 필요: TestContainers 자동 실행)
cd backend && ./gradlew test --tests "com.soulpal.integration.*" -x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend

# 프론트엔드 lint
cd frontend && npm run lint              # 검사만
cd frontend && npm run lint:fix          # 자동 수정

# Docker (운영 환경)
docker compose up -d                     # 전체 시작
docker compose up -d backend             # 백엔드만 재시작

# 로그 분석
docker logs soulpal-backend 2>&1 | grep '"requestId":"<ID>"'   # 요청 추적
docker logs soulpal-backend 2>&1 | grep '"level":"ERROR"'       # 에러 확인
docker logs soulpal-backend 2>&1 | grep '응답 지연'             # Ollama 느린 응답
docker exec soulpal-redis redis-cli KEYS "refresh:*"            # Redis 토큰 상태
```

---

## 아키텍처 결정 기록 (ADR)

### ADR-001: SSE, WebSocket 아닌 이유
- **결정:** Ollama 스트리밍에 Server-Sent Events(SSE) 사용
- **이유:** AI 응답은 서버→클라이언트 단방향. WebSocket은 양방향 오버헤드가 불필요하고, Nginx 설정이 복잡해짐
- **구현:** `produces = MediaType.TEXT_EVENT_STREAM_VALUE`, Nginx `proxy_buffering off`
- **금지:** WebSocket으로 교체하지 말 것

### ADR-002: Ollama, 외부 AI API 아닌 이유
- **결정:** OpenAI/Anthropic API 대신 로컬 Ollama 사용
- **이유:** 사용자 대화 데이터가 외부로 나가지 않는 프라이버시 설계. API 비용 없음
- **트레이드오프:** 응답 속도가 GPU 사양에 의존함. 현재 기본 모델은 `llama3`
- **금지:** 외부 AI API를 기본 경로로 추가하지 말 것

### ADR-003: Redis JWT 블랙리스트
- **결정:** 로그아웃 시 액세스 토큰을 Redis에 블랙리스트로 등록
- **이유:** JWT는 stateless라 서버에서 즉시 무효화 불가. 블랙리스트로 보완
- **TTL:** 토큰 잔여 만료 시간만큼만 저장 (불필요한 메모리 낭비 없음)
- **주의:** Redis 재시작 시 블랙리스트 소실 → `appendonly yes` 설정으로 영속화

### ADR-004: 단일 세션 정책 (refresh token)
- **결정:** 사용자당 Redis에 리프레시 토큰 1개만 저장 (최신 로그인이 이전 것을 덮어씀)
- **이유:** 설계 단순성. 다중 기기 동시 접속보다 단일 세션 보안 우선
- **결과:** 기기 A로 로그인한 뒤 기기 B로 로그인하면 기기 A의 리프레시 토큰 무효화
- **금지:** Redis key를 `userId`가 아닌 `userId:deviceId`로 바꾸지 말 것 (설계 변경이므로 논의 필요)

### ADR-005: Caffeine 컨텍스트 캐시 5분 TTL
- **결정:** `ContextBuilderService.buildUserContext()` 결과를 5분 캐싱
- **이유:** 대화 이력 120개 분석은 매 요청마다 하기엔 DB 부담이 큼. 5분은 신선도와 비용의 트레이드오프
- **금지:** TTL을 0으로 설정하거나 캐시를 제거하지 말 것

### ADR-006: 액세스 토큰 1시간, 리프레시 7일
- **결정:** 액세스 토큰 1시간, 리프레시 토큰 7일
- **이유:** 짧은 액세스 토큰으로 탈취 위험 최소화. 7일 리프레시로 UX 보장
- **Redis TTL:** 리프레시 토큰 저장 TTL도 동일하게 7일 (`refreshExpiration` 값 사용)

---

## 핵심 버그 & 해결 기록

### BUG-001: 토큰 갱신 요청 자체가 401을 반환할 때 프론트 deadlock
- **증상:** 액세스 토큰 만료 → `/api/auth/refresh` 호출 → 서버 401 반환 → 프론트 앱 완전 멈춤
- **원인:** `http.js` axios 인터셉터가 `/auth/refresh` 응답 401도 다시 refresh를 시도. `isRefreshing=true` 상태에서 큐에 추가되어 영원히 대기
- **해결:** 인터셉터 조건에 `original.url === '/auth/refresh'` 체크 추가

```js
// frontend/src/api/http.js
if (err.response?.status !== 401 || original._retry || original.url === '/auth/refresh') {
  return Promise.reject(err)
}
```

### BUG-002: JWT 시그니처 불일치 (signature does not match)
- **증상:** `[REFRESH] JWT 파싱 실패: JWT signature does not match locally computed signature`
- **원인:** 백엔드를 Docker 외부(`./gradlew bootRun`)로 실행 시 기본 JWT secret 사용. Docker 재시작 시 `.env`의 JWT secret으로 전환 → 기존 토큰 무효화
- **해결:** 항상 동일한 환경(Docker 또는 로컬)에서 실행. 환경 전환 시 브라우저 localStorage 초기화 필요
- **예방:** `StartupValidator`가 prod 프로파일에서 기본 secret 사용 시 시작을 차단함

```java
// backend/.../config/StartupValidator.java
// prod에서 기본 JWT_SECRET 감지 시 throw IllegalStateException
```

### BUG-003: SSE 스트림 executor 스레드에서 MDC 컨텍스트 소실
- **증상:** SSE 스트리밍 중 로그에 `requestId`, `userId` 없음
- **원인:** MDC는 thread-local. `executor.execute()` 로 넘기면 새 스레드에 MDC 없음
- **해결:** executor로 넘기기 전 MDC 캡처 → 스레드 내에서 복원 → finally에서 clear

```java
// ChatController.java, GroupChatController.java
Map<String, String> mdcCtx = MDC.getCopyOfContextMap();
executor.execute(() -> {
    if (mdcCtx != null) MDC.setContextMap(mdcCtx);
    try {
        // ... 스트리밍 로직
    } finally {
        MDC.clear();
    }
});
```

### BUG-005: SSE executor 스레드에서 SecurityContext 소실 → NullPointerException
- **증상:** `POST /api/chat/stream` 요청 시 `[CHAT] 스트림 오류: ... SecurityContext.getAuthentication() is null` (로그에 200 반환이지만 실제론 에러)
- **원인:** `SecurityContextHolder`는 ThreadLocal 기반. `executor.execute()` 내부의 새 스레드에서는 SecurityContext가 없음. `CharacterService.getById()` → `currentUserId()` → `SecurityContextHolder.getContext().getAuthentication().getPrincipal()` → NPE
- **해결:** MDC와 동일하게 executor 실행 전에 SecurityContext를 캡처하고 스레드 내에서 복원, finally에서 clear

```java
// ChatController.java, GroupChatController.java
SecurityContext securityContext = SecurityContextHolder.getContext();
executor.execute(() -> {
    SecurityContextHolder.setContext(securityContext);
    try {
        // ... 스트리밍 로직
    } finally {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }
});
```

- **주의:** MDC와 SecurityContext는 항상 세트로 전파해야 함. 둘 중 하나만 전파하면 안 됨

### BUG-004: 라우터 가드가 만료된 토큰을 유효로 판단
- **증상:** 만료된 access token이 localStorage에 있어도 보호된 페이지 진입 허용 → API 호출 후 401
- **원인:** `router.beforeEach`가 토큰 **존재 여부**만 체크, 만료 여부 미확인
- **해결:** JWT payload의 `exp` claim을 클라이언트에서 디코딩해 만료 여부 사전 체크

```js
// frontend/src/router/index.js
function isTokenValid(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 > Date.now()
  } catch { return false }
}
```

---

## 코드 패턴 & 컨벤션

### 로그 태그 규칙
모든 로그 메시지는 `[서비스명]` 태그로 시작합니다:

```
[AUTH]        인증/토큰 관련
[CHAT]        1:1 채팅 스트리밍
[GROUP]       그룹 채팅
[CHARACTER]   캐릭터 CRUD
[OLLAMA]      LLM 호출
[CONTEXT]     개인화 컨텍스트 분석
[RATE_LIMIT]  속도 제한
[REFRESH]     토큰 갱신
[BIZ]         비즈니스 예외
[FRONTEND]    프론트엔드 JS 에러
[UNHANDLED]   미처리 예외
```

### MDC 필드 (모든 요청에 자동 주입)
```json
{
  "requestId": "a1b2c3d4",    // 요청 단위 추적 (8자)
  "userId":    "uuid...",      // 인증된 사용자 ID (JwtFilter에서 설정)
  "method":    "POST",         // HTTP 메서드
  "uri":       "/api/chat/stream"  // 요청 경로
}
```

### 예외 처리 계층
```
BusinessException(ErrorCode)  → GlobalExceptionHandler → 표준 JSON 응답
IllegalArgumentException      → GlobalExceptionHandler → INVALID_INPUT (400)
ResourceNotFoundException     → GlobalExceptionHandler → RESOURCE_NOT_FOUND (404)
RateLimitExceededException    → GlobalExceptionHandler → RATE_LIMIT_EXCEEDED (429)
```

### 새 API 엔드포인트 추가 시 체크리스트
1. `SecurityConfig.java`에서 public/auth 여부 설정
2. `@Operation(summary = "...")` Swagger 문서 추가
3. 서비스 레이어에 `log.info("[TAG] ...")` 추가
4. `GlobalExceptionHandler`에서 처리되지 않는 새 예외 유형이 있으면 추가
5. `make check` 실행 — ArchUnit이 레이어 위반을 감지함

### 새 DB 마이그레이션 추가 시 체크리스트
1. `backend/src/main/resources/db/migration/V{N}__설명.sql` 파일 작성
2. `make test-integration` 실행 — `FlywayMigrationTest`가 SQL 오류를 감지함
3. CLAUDE.md의 "디렉토리 구조" 섹션 업데이트 불필요 (자동 감지)

### 성능 기준
- Ollama 응답: 30초 이상이면 `[OLLAMA] 응답 지연` WARN 로그 발생 (`SLOW_THRESHOLD_MS = 30_000`)
- Rate Limit: 분당 24건 이상(80%)이면 `[RATE_LIMIT] 한도 근접` WARN 로그 발생

---

## 인증 플로우 전체 흐름

```
[로그인]
POST /api/auth/login
→ AuthService.login()
→ buildAuthResponse() → accessToken(1h) + refreshToken(7d)
→ Redis: SET refresh:{userId} {refreshToken} TTL 7d
→ 응답: { accessToken, refreshToken, userId, username, email }
→ 프론트: localStorage.setItem('soulpal_token', ...) + localStorage.setItem('soulpal_refresh', ...)

[API 요청]
모든 요청에 axios 인터셉터가 Authorization: Bearer {soulpal_token} 주입
JwtFilter: 토큰 파싱 → SecurityContext 설정 + MDC.put("userId", ...)

[토큰 만료 (1시간 후)]
API → 401 응답
axios 인터셉터: soulpal_refresh로 POST /api/auth/refresh
→ 성공: 새 accessToken → localStorage 갱신 → 원 요청 재시도
→ 실패: redirectLogin() → localStorage 초기화 → /login 리다이렉트

[라우터 가드]
페이지 진입 시 soulpal_token의 exp claim 검사
→ 만료: localStorage 초기화 → /login 리다이렉트 (API 호출 없이 즉시)

[로그아웃]
POST /api/auth/logout
→ Redis에서 refreshToken 삭제
→ Redis 블랙리스트에 accessToken 등록 (잔여 TTL만큼)
```

---

## 구조화 로그 분석 가이드

### 에러 발생 시 분석 순서

```bash
# 1. 최근 에러 확인
docker logs soulpal-backend 2>&1 | grep '"level":"ERROR\|WARN"' | tail -20

# 2. requestId로 요청 전체 흐름 추적
docker logs soulpal-backend 2>&1 | grep '"requestId":"<여기에 ID>"'

# 3. 특정 유저 활동 추적
docker logs soulpal-backend 2>&1 | grep '"userId":"<여기에 userId>"'

# 4. Ollama 성능 이슈
docker logs soulpal-backend 2>&1 | grep '\[OLLAMA\]'

# 5. 인증 이슈
docker logs soulpal-backend 2>&1 | grep '\[AUTH\]\|\[REFRESH\]'

# 6. Rate limit 상황
docker logs soulpal-backend 2>&1 | grep '\[RATE_LIMIT\]'
```

### 정상 요청의 로그 패턴 (채팅 스트림 예시)
```json
{"level":"INFO",  "message":"POST /api/chat/stream → 200 (0ms)",  "requestId":"a1b2c3d4"}  // MdcLoggingFilter (요청 수신)
{"level":"INFO",  "message":"[CHAT] 스트림 시작: characterId=..., characterName=...", "requestId":"a1b2c3d4"}
{"level":"DEBUG", "message":"[CONTEXT] 히스토리: characterId=..., total=15",         "requestId":"a1b2c3d4"}
{"level":"INFO",  "message":"[OLLAMA] streamChat 시작: model=llama3, historySize=15", "requestId":"a1b2c3d4"}
{"level":"INFO",  "message":"[OLLAMA] streamChat 완료: duration=8234ms, tokens=142",  "requestId":"a1b2c3d4"}
{"level":"INFO",  "message":"[CHAT] 스트림 완료: characterId=..., duration=8412ms",   "requestId":"a1b2c3d4"}
```

> 참고: SSE 스트림은 연결이 살아있는 동안 MdcLoggingFilter의 최종 로그(`→ 200`)가 늦게 찍힐 수 있습니다.

---

## 에러 코드 레지스트리

새 에러 추가 시 `ErrorCode.java`에 정의하고 이 표를 업데이트합니다.

| 코드 | HTTP | 의미 | 발생 위치 |
|------|------|------|----------|
| C001 | 400 | 입력값 오류 | `@Valid` 실패, `IllegalArgumentException` |
| C002 | 404 | 리소스 없음 | `ResourceNotFoundException` |
| C003 | 401 | 인증 필요 | JwtFilter 토큰 없음/만료 |
| C004 | 403 | 권한 없음 | 타인 리소스 접근 |
| C005 | 500 | 서버 내부 오류 | 미처리 예외 |
| A001 | 400 | 이메일 중복 | 회원가입 |
| A002 | 400 | 사용자명 중복 | 회원가입 |
| A003 | 401 | 이메일/비밀번호 불일치 | 로그인 |
| A004 | 401 | 유효하지 않은 토큰 | JwtFilter |
| A005 | 401 | 블랙리스트 토큰 | 로그아웃 후 재사용 |
| A006 | 401 | 리프레시 토큰 무효 | `/api/auth/refresh` |
| CH001 | 404 | 캐릭터 없음 | CharacterService |
| CH002 | 403 | 캐릭터 권한 없음 | CharacterService |
| CH003 | 400 | 메시지 2000자 초과 | ChatController |
| P001 | 404 | 프로젝트 없음 | ProjectService |
| P002 | 403 | 프로젝트 권한 없음 | ProjectService |
| R001 | 429 | Rate Limit 초과 (분당 30건) | RateLimitService |
| AI001 | 503 | Ollama 응답 실패 | OllamaService |

---

## API 엔드포인트 목록

전체 스펙: `http://localhost:9090/swagger-ui.html`

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/register` | 없음 | 회원가입 |
| POST | `/api/auth/login` | 없음 | 로그인 |
| POST | `/api/auth/refresh` | 없음 | 액세스 토큰 갱신 |
| POST | `/api/auth/logout` | Bearer | 로그아웃 |
| GET | `/api/auth/me` | Bearer | 내 정보 조회 |
| DELETE | `/api/auth/me` | Bearer | 회원 탈퇴 |
| GET | `/api/characters` | Bearer | 캐릭터 목록 (페이징) |
| POST | `/api/characters` | Bearer | 캐릭터 생성 |
| GET | `/api/characters/{id}` | Bearer | 캐릭터 단건 조회 |
| PUT | `/api/characters/{id}` | Bearer | 캐릭터 수정 |
| DELETE | `/api/characters/{id}` | Bearer | 캐릭터 삭제 |
| POST | `/api/chat/stream` | Bearer | SSE 채팅 스트리밍 |
| GET | `/api/chat/messages/{charId}` | Bearer | 채팅 이력 조회 |
| GET | `/api/projects` | Bearer | 프로젝트 목록 |
| POST | `/api/projects` | Bearer | 프로젝트 생성 |
| DELETE | `/api/projects/{id}` | Bearer | 프로젝트 삭제 |
| POST | `/api/group-rooms` | Bearer | 그룹 대화방 생성 |
| GET | `/api/group-rooms` | Bearer | 그룹 대화방 목록 |
| GET | `/api/group-rooms/{id}` | Bearer | 그룹 대화방 조회 |
| DELETE | `/api/group-rooms/{id}` | Bearer | 그룹 대화방 삭제 |
| POST | `/api/group-chat/stream` | Bearer | 그룹 SSE 채팅 |
| GET | `/api/group-chat/messages/{roomId}` | Bearer | 그룹 채팅 이력 |
| POST | `/api/logs/frontend-error` | 없음 | 프론트엔드 JS 에러 전송 |
| GET | `/actuator/health` | 없음 | 헬스 체크 |
| GET | `/actuator/metrics` | Bearer | 메트릭 |
| POST | `/actuator/loggers/{name}` | Bearer | 런타임 로그 레벨 변경 |

---

## 절대 하면 안 되는 것

| 금지 사항 | 이유 |
|-----------|------|
| `JWT_SECRET`을 코드에 하드코딩 | `StartupValidator`가 prod에서 차단하지만, dev에서도 `.env` 사용 권장 |
| Redis `appendonly` 설정 제거 | 재시작 시 refresh token, blacklist 소실 → 모든 세션 강제 로그아웃 |
| SSE를 WebSocket으로 교체 | ADR-001 참고. 단방향 스트림에 WebSocket은 불필요한 복잡성 |
| `executor.execute()` 내부에서 MDC 복원 코드 생략 | BUG-003 재발 — requestId/userId 로그 소실 |
| `original.url === '/auth/refresh'` 인터셉터 조건 제거 | BUG-001 재발 — 프론트 deadlock |
| `ContextBuilderService` 캐시 제거 | DB 쿼리 부하 폭증 (매 채팅마다 120개 메시지 분석) |
| `docker compose down -v` | 볼륨 삭제로 DB, Redis, Ollama 모델 데이터 전체 손실 |
| 로컬(`./gradlew bootRun`)과 Docker를 혼용해서 사용 | BUG-002 재발 — JWT secret 불일치 |
| `executor.execute()` 내부에서 SecurityContext 복원 코드 생략 | BUG-005 재발 — CharacterService.currentUserId() NPE, 모든 SSE 채팅 실패 |
| ArchUnit 테스트 실패를 무시하고 코드 통합 | 레이어 위반 누적 — Controller가 Repository를 직접 호출하게 됨 |
| `make check` 없이 PR 생성 | lint + 단위 테스트 미검증 상태로 코드 통합 |

---

## 현재 알려진 한계 & 개선 필요 사항

### 🔴 우선순위 높음
- ✅ **통합 테스트**: TestContainers 도입 완료 (`AuthIntegrationTest`, `FlywayMigrationTest`)
- ✅ **CI/CD**: GitHub Actions 완료 (`.github/workflows/ci.yml`)

### 🟡 우선순위 중간
- **프론트 컴포넌트 테스트 없음**: `CharacterCard`, `ChatInput`, `MessageBubble` 미검증
- **E2E 테스트 없음**: 로그인 → 캐릭터 생성 → 채팅 전체 흐름 자동 검증 불가 (Playwright 고려)
- **Actuator Prometheus 미연동**: metrics 엔드포인트 노출은 완료. Prometheus + Grafana 연동 필요
- **단일 세션 정책**: 다중 기기 동시 로그인 지원 안 됨 (ADR-004)

### 🟢 우선순위 낮음
- HTTPS/TLS 미설정 (Nginx 레벨)
- Kubernetes 배포 매니페스트 없음
- 의존성 자동 업데이트 없음 (Dependabot/Renovate)

---

## 디렉토리 구조 요약

```
SoulPal/
├── backend/
│   ├── src/main/java/com/soulpal/
│   │   ├── config/          JwtFilter, JwtUtil, SecurityConfig, MdcLoggingFilter, StartupValidator
│   │   ├── controller/      AuthController, ChatController, GroupChatController, ...
│   │   ├── service/         AuthService, CharacterService, OllamaService, TokenService, ...
│   │   ├── model/           User, Character, Message, GroupRoom, GroupMessage, Project
│   │   ├── repository/      Spring Data JPA repositories
│   │   ├── dto/             Request/Response DTOs
│   │   └── exception/       BusinessException, ErrorCode, GlobalExceptionHandler
│   └── src/test/
│       ├── service/         단위 테스트 7개 (Mock 기반)
│       ├── exception/       GlobalExceptionHandlerTest
│       ├── architecture/    ArchitectureTest (ArchUnit 레이어 규칙)
│       └── integration/     AuthIntegrationTest, FlywayMigrationTest (TestContainers)
├── frontend/
│   ├── src/
│   │   ├── api/             http.js (axios + 인터셉터), auth.js, chat.js, ...
│   │   ├── stores/          auth.js, character.js, chat.js, project.js (Pinia)
│   │   ├── router/          index.js (토큰 만료 체크 포함)
│   │   ├── views/           LoginView, ChatView, GroupChatView, ...
│   │   └── components/      CharacterCard, ChatInput, MessageBubble, OptionCard
│   ├── .eslintrc.cjs        ESLint 설정 (vue3-recommended)
│   └── nginx.conf           /api/* → backend:9090 프록시, SSE 지원
├── .github/
│   └── workflows/ci.yml     GitHub Actions CI (단위·통합·프론트·Docker 빌드)
├── scripts/
│   └── dev-doctor.sh        환경 진단 스크립트 (make doctor)
├── Makefile                 모든 개발 명령어 단일 진입점
├── docker-compose.yml       5개 서비스 오케스트레이션
├── .env                     시크릿 (git-ignore됨)
├── .env.example             환경변수 템플릿
├── README.md                프로젝트 개요
└── CLAUDE.md                ← 이 파일 (AI 개발 컨텍스트)
```

---

*최종 업데이트: 2026-03-25 (자율 개발 인프라 전면 구축)*
*작성: Claude (Sonnet 4.6) + 사용자 협업*
