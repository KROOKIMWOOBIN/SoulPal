# SoulPal — AI 개발 컨텍스트

> AI가 확인 없이 자율 개발할 수 있도록 작성된 컨텍스트 파일.
> 새로운 결정이나 패턴 추가 시 이 문서를 업데이트.

---

## 아키텍처

```
브라우저 → Nginx:8080 → Spring Boot:9090 → PostgreSQL:5432
                                          → Redis:6379
                                          → Ollama:11434 (llama3)
```

**핵심 원칙:** 외부 AI API 사용 금지 (프라이버시, 비용 0)

---

## 명령어 (Makefile 단일 진입점)

```bash
make test-unit        # 단위 + 아키텍처 테스트 (Docker 불필요)
make test-integration # TestContainers 통합 테스트 (Docker 필요)
make test-frontend    # Vitest
make check            # PR 전 필수: lint + test-unit + test-frontend
make lint-fix         # ESLint 자동 수정
make doctor           # 환경 진단
make docker-rebuild   # 이미지 재빌드 + 재시작
```

---

## 핵심 ADR (변경 금지)

| 결정 | 이유 | 금지 |
|------|------|------|
| SSE 스트리밍 | 단방향 스트림에 WebSocket 불필요 | WebSocket 교체 |
| Ollama 로컬 | 프라이버시, API 비용 없음 | 외부 AI API 기본 경로 추가 |
| Redis JWT 블랙리스트 | stateless JWT 즉시 무효화 보완 | appendonly 설정 제거 |
| 단일 세션 (userId 키) | 설계 단순성 | `userId:deviceId` 키 변경 |
| Caffeine 컨텍스트 캐시 5분 | 매 요청마다 120개 메시지 분석 방지 | 캐시 제거 또는 TTL=0 |
| 액세스 1h / 리프레시 7d | 보안 + UX 균형 | - |

---

## 알려진 버그 패턴 (재발 방지)

- **BUG-001** axios 인터셉터에 `original.url === '/auth/refresh'` 예외 필수 → deadlock 방지 (`frontend/src/api/http.js`)
- **BUG-003** `executor.execute()` 내 MDC 캡처·복원·finally clear 필수 → requestId/userId 로그 소실 방지 (`ChatController`, `GroupChatController`)
- **BUG-005** `executor.execute()` 내 SecurityContext 캡처·복원·finally clear 필수 → NPE 방지 (BUG-003과 항상 세트)
- **BUG-004** 라우터 가드에서 JWT `exp` claim 검증 필수 → 만료 토큰으로 페이지 진입 방지 (`frontend/src/router/index.js`)
- **BUG-002** 로컬+Docker 혼용 실행 금지 → JWT secret 불일치

---

## 코드 컨벤션

**로그 태그:** `[AUTH]` `[CHAT]` `[GROUP]` `[CHARACTER]` `[OLLAMA]` `[CONTEXT]` `[RATE_LIMIT]` `[REFRESH]` `[BIZ]` `[FRONTEND]` `[UNHANDLED]`

**MDC 자동 주입 (모든 요청):** `requestId`(8자), `userId`, `method`, `uri`

**예외 계층:**
```
BusinessException(ErrorCode)    → GlobalExceptionHandler → 표준 JSON
ResourceNotFoundException       → RESOURCE_NOT_FOUND (404)
RateLimitExceededException      → RATE_LIMIT_EXCEEDED (429)
RejectedExecutionException      → R002 서버 혼잡 (503)
Exception (미처리)               → C005 서버 오류 (500)
```
주의: `IllegalArgumentException`은 더 이상 별도 처리하지 않음 — 모든 비즈니스 오류는 `BusinessException(ErrorCode)` 사용

---

## 새 API 엔드포인트 체크리스트

1. `SecurityConfig.java` public/auth 여부
2. `@Operation(summary = "...")` Swagger 문서
3. `log.info("[TAG] ...")` 서비스 레이어
4. `GlobalExceptionHandler` 새 예외 타입 처리
5. `make check` 실행 (ArchUnit 레이어 위반 감지)

## 새 DB 마이그레이션 체크리스트

1. `backend/src/main/resources/db/migration/V{N}__설명.sql`
2. `make test-integration` 실행 (FlywayMigrationTest)

---

## API 문서

Swagger UI: `http://localhost:9090/swagger-ui.html`

---

## 에러 코드 레지스트리

| 코드 | HTTP | 의미 |
|------|------|------|
| C001-C005 | 400/404/401/403/500 | 공통 (입력오류/없음/인증/권한/서버) |
| A001-A002 | 400 | 이메일/사용자명 중복 |
| A003 | 401 | 이메일/비밀번호 불일치 |
| A004-A006 | 401 | 토큰 무효/블랙리스트/리프레시 무효 |
| CH001-CH003 | 404/403/400 | 캐릭터 없음/권한없음/메시지 2000자 초과 |
| P001-P002 | 404/403 | 프로젝트 없음/권한없음 |
| R001 | 429 | Rate Limit 초과 (분당 30건) |
| R002 | 503 | SSE 스레드 풀 포화 (AbortPolicy) |
| AI001 | 503 | Ollama 응답 실패 (2회 재시도 후) |

---

## 절대 금지

- `JWT_SECRET` 하드코딩
- Redis `appendonly` 제거
- SSE → WebSocket 교체
- `executor.execute()` 내 MDC/SecurityContext 복원 생략
- `/auth/refresh` 인터셉터 예외 조건 제거
- `ContextBuilderService` 캐시 제거
- `docker compose down -v` (데이터 전체 삭제)
- 로컬+Docker 혼용 실행
- ArchUnit 실패 무시하고 코드 통합
- `make check` 없이 PR 생성

---

## 주요 설계 결정 (2026-03-26 이후)

- **CharacterService**: `currentUserId()` 제거 → 모든 메서드에 `userId` 명시 파라미터. 서비스 레이어에서 SecurityContext 직접 접근 금지.
- **Refresh token rotation**: `TokenService.refresh()` 반환 타입 `Map<String, String>` (accessToken + refreshToken 모두). 프론트도 새 refreshToken 저장 필수.
- **GroupChatService**: 캐릭터 배치 로딩 (`getByIds()`) → 그룹 내 캐릭터 수만큼 쿼리 발생하던 N+1 제거.
- **MessageService**: `save/clearAll/deleteLastAiMessage` → `@CacheEvict(userContext)`. 대화 변경 즉시 컨텍스트 캐시 무효화.
- **OllamaService**: 최대 2회 지수 백오프 재시도 (1s, 2s). 일시적 Ollama 장애에 자동 복구.
- **AsyncConfig**: `CallerRunsPolicy` → `AbortPolicy`. 풀 포화 시 Tomcat 스레드 차단 대신 `RejectedExecutionException` 발생 → 503 응답.
- **Token storage**: `localStorage` → `sessionStorage`. 브라우저 탭 닫으면 세션 만료.
- **CORS**: `WebConfig`에 `${cors.allowed-origins}` 환경변수 (기본값: localhost 3개).

## 현재 상태

- ✅ 통합 테스트 (TestContainers), CI/CD (GitHub Actions)
- ✅ 백엔드 단위 테스트 10/10, 프론트 스토어 테스트 5/5
- 🟡 컴포넌트 테스트 없음 (CharacterCard, ChatInput, MessageBubble)
- 🟡 E2E 테스트 없음 (Playwright 고려)
- 🟡 Prometheus + Grafana 연동 필요
- 🟢 HTTPS/TLS, Kubernetes 배포 (낮은 우선순위)

---

*최종 업데이트: 2026-03-26*
*작성: Claude (Sonnet 4.6) + 사용자 협업*
