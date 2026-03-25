# SoulPal

AI 캐릭터와 대화하는 웹 애플리케이션.

> **AI 자율 개발 프로젝트입니다.**
> 이 프로젝트는 Claude(AI)가 인간의 개입 없이 스스로 개발할 수 있도록 설계되었습니다.
> AI 개발 컨텍스트 문서 → **[CLAUDE.md](CLAUDE.md)**

---

## AI 활용 방식

이 프로젝트는 **Claude (Sonnet 4.6)** 를 단순 코드 생성 도구가 아닌 **자율 개발자**로 활용합니다.

### 핵심 철학: 인간은 방향을 정하고, AI가 실행한다

```
사람: "그룹 채팅 기능 만들어줘"
 AI: 설계 → 구현 → 테스트 → 디버깅 → 문서화 (자율 수행)
```

사람이 결과를 확인하거나 에러를 직접 분석할 필요 없이, AI가 스스로 로그를 읽고, 테스트를 돌리고, 버그를 수정합니다.

### 이를 가능하게 하는 인프라

| 레이어 | 도구 | 역할 |
|--------|------|------|
| **컨텍스트** | [CLAUDE.md](CLAUDE.md) | ADR·버그기록·금지사항·에러코드·API목록 — AI의 장기 기억 |
| **진단** | `make doctor` | 환경 상태를 한 번에 출력 → AI에게 붙여넣으면 즉시 파악 |
| **아키텍처 보호** | ArchUnit | Controller→Repository 직접 호출 등 레이어 위반을 테스트로 강제 차단 |
| **통합 검증** | TestContainers | 실제 PostgreSQL + Redis 컨테이너로 인증·마이그레이션 플로우 검증 |
| **코드 품질** | ESLint + ArchUnit | AI가 작성한 코드를 자동 검증, 사람 리뷰 의존도 감소 |
| **CI/CD** | GitHub Actions | push마다 단위·통합·프론트 테스트 + Docker 빌드 자동 실행 |
| **구조화 로그** | Logstash JSON + MDC | `requestId`·`userId` 포함 — AI가 로그만으로 버그 원인 추적 가능 |

### AI가 혼자 해결한 문제들

모두 사람이 "이런 에러 났어" 라고 붙여넣으면 AI가 로그 분석부터 수정·검증까지 완료한 케이스입니다.

- **Auth 401 Deadlock** — `/api/auth/refresh` 자체가 401 반환 시 axios 인터셉터가 무한 대기에 빠지는 문제. 로그 패턴으로 원인 파악 → axios 인터셉터 조건 수정
- **JWT Secret 불일치** — 로컬(`bootRun`) ↔ Docker 환경 전환 시 기존 토큰 전부 무효화. `StartupValidator`로 재발 방지
- **MDC 컨텍스트 소실** — SSE executor 스레드에서 `requestId`/`userId`가 로그에서 사라지는 문제. MDC 캡처·복원 패턴 적용
- **라우터 가드 만료 토큰 허용** — 만료된 토큰이 있어도 보호 페이지 진입 허용. JWT `exp` claim 클라이언트 사전 검증으로 해결

---

## 아키텍처

```
┌────────────────────────────────────────────────────────────────┐
│                        Docker Compose                          │
│                                                                │
│  ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ postgres │  │  redis  │  │  ollama  │  │   backend     │  │
│  │  :5432   │  │  :6379  │  │  :11434  │  │   :9090       │  │
│  │ PostgreSQL│ │JWT 블랙리│  │ LLM 서버 │  │ Spring Boot   │  │
│  │ 영구 저장 │  │스트/세션 │  │ llama3   │  │ Tomcat        │  │
│  └──────────┘  └─────────┘  └──────────┘  └───────────────┘  │
│                                                                │
│  ┌───────────────┐                                             │
│  │   frontend    │  → /api/* 요청을 backend:9090 으로 프록시   │
│  │   :8080       │                                             │
│  │ Nginx + Vue   │                                             │
│  └───────────────┘                                             │
│                                                                │
│  Volumes: postgres_data / ollama_data / redis_data             │
└────────────────────────────────────────────────────────────────┘
```

## 서비스 명세

| 서비스 | 이미지 | 포트 | 설명 |
|--------|--------|------|------|
| `db` | `postgres:16-alpine` | 5432 | 메인 데이터베이스 |
| `redis` | `redis:7-alpine` | 6379 | JWT 블랙리스트 / 리프레시 토큰 / Rate Limit 카운터 |
| `ollama` | `ollama/ollama:latest` | 11434 | LLM 추론 서버 (llama3) |
| `backend` | 빌드 (Gradle) | 9090 | Spring Boot REST API |
| `frontend` | 빌드 (Node → Nginx) | 8080 | Vue SPA — `/api/*` 역방향 프록시 |

## 환경 설정 (.env)

프로젝트 루트의 `.env.example`을 복사해 `.env`를 생성합니다.

```bash
cp .env.example .env
```

| 변수 | 설명 |
|------|------|
| `POSTGRES_DB` | DB 이름 |
| `POSTGRES_USER` | DB 사용자 |
| `POSTGRES_PASSWORD` | DB 비밀번호 |
| `REDIS_PORT` | Redis 포트 (기본 6379) |
| `OLLAMA_MODEL` | 사용할 LLM 모델 (기본 llama3) |
| `SERVER_PORT` | 백엔드 포트 (기본 9090) |
| `JWT_SECRET` | JWT 서명 키 — **64자 이상 랜덤 문자열 필수** |

> ⚠️ `.env` 파일은 `.gitignore`에 포함되어 있습니다. **절대 커밋하지 마세요.**
> `JWT_SECRET`은 반드시 64자 이상으로 설정해야 합니다 (512비트 ≥ HS512 요구사항).

## 실행 방법

### Docker (권장)

```bash
# .env 파일 준비 (최초 1회)
cp .env.example .env
# .env에서 JWT_SECRET 등 값 변경

# 전체 서비스 시작
docker compose up -d

# 로그 확인 (프론트 에러도 [FRONTEND] 태그로 여기서 확인)
docker compose logs -f backend

# 종료
docker compose down
```

브라우저에서 http://localhost:8080 접속

> **최초 실행 시**: `ollama` 컨테이너 내부에서 llama3 모델을 직접 pull해야 합니다.
> ```bash
> docker exec -it soulpal-ollama ollama pull llama3
> ```
> 모델 데이터는 `ollama_data` 볼륨에 영구 저장되므로 재시작 시 재다운로드 없음 (~4.7GB).

### 로컬 개발

**백엔드** (IntelliJ 기준):
```bash
make dev-backend   # 또는: cd backend && ./gradlew bootRun -x buildFrontend ...
```
- Java 21 toolchain 자동 설정
- H2 파일 DB 사용 (기본값, PostgreSQL 없이 동작)
- Ollama는 `localhost:11434` 연결

**프론트엔드** (별도 dev 서버):
```bash
make dev-frontend  # 또는: cd frontend && npm install && npm run dev
# http://localhost:5173 (API → backend:9090 프록시)
```

### 개발 명령어 (Makefile)

```bash
make test-unit        # 백엔드 단위 + 아키텍처 테스트 (빠름, Docker 불필요)
make test-integration # 백엔드 통합 테스트 (TestContainers: PostgreSQL + Redis 자동 실행)
make test-frontend    # 프론트엔드 Vitest
make check            # lint + test-unit + test-frontend (PR 전 필수)
make doctor           # 환경 진단 — 출력을 AI에게 붙여넣으면 상태 즉시 파악
make docker-rebuild   # 이미지 재빌드 후 재시작
```

## 기술 스택

| 분류 | 내용 |
|------|------|
| **Backend** | Spring Boot 3.2, Java 21, Spring Security (Stateless JWT), JPA/Hibernate, Spring Actuator |
| **Database** | PostgreSQL 16 (Docker) / H2 파일 DB (로컬 개발), Flyway 마이그레이션 |
| **Cache / 보안** | Redis 7 — JWT 블랙리스트, 리프레시 토큰, Redis 기반 Rate Limit |
| **AI** | Ollama llama3 — SSE 스트리밍 + 개인화 컨텍스트 주입 |
| **RAG** | DuckDuckGo HTML 검색 + JSoup 크롤링 — 실시간 웹 지식 주입 |
| **Frontend** | Vue 3, Pinia, Vue Router 4, Vite, Vitest, ESLint |
| **Infra** | Docker Compose, Nginx (리버스 프록시 + SSE), Gradle 8.7, GitHub Actions |
| **AI 개발 인프라** | ArchUnit, TestContainers, Logstash JSON 로깅, MDC 추적, CLAUDE.md |

## 주요 기능

### 인증 (JWT + Redis)
- 액세스 토큰 (1시간) + 리프레시 토큰 (7일) 이중 토큰 방식
- Redis에 리프레시 토큰 저장, 로그아웃 시 액세스 토큰 블랙리스트 등록
- 프론트엔드 401 응답 시 자동 토큰 갱신 (큐 기반 동시 요청 처리)
- **SSE 스트림**도 401 감지 시 자동 리프레시 후 재시도
- prod 프로파일에서 `JWT_SECRET` 기본값 사용 시 시작 차단

### Rate Limiting (Redis)
- 채팅 API: 사용자별 분당 30건 (Redis INCR, 수평 확장 가능)
- 프론트엔드 에러 로그 API: IP별 분당 20건

### 캐릭터 목록 페이지네이션
- `GET /api/characters?page=0&size=20&sort=recent` — Spring Data Page 응답
- 정렬: `recent` (최근 대화순) / `name` (이름순) / `favorite` (즐겨찾기)

### 개인화 AI 응답
- 대화 이력 분석으로 사용자 관심사·감정 상태 추출 → 시스템 프롬프트에 자동 주입
- 현재 메시지와 키워드 유사도가 높은 과거 대화를 선별해 컨텍스트로 전달
- 대화 깊이(총 메시지 수)에 따른 친밀도 단계 자동 조정

### 웹 크롤링 RAG
- 질문 패턴 감지 시 DuckDuckGo 검색 → 상위 3페이지 크롤링
- 추출된 웹 컨텍스트를 Ollama 시스템 프롬프트에 주입해 실시간 지식 보완

### 에러 모니터링
- 프론트엔드 JS 에러(Vue errorHandler, unhandledrejection, window.error)를
  백엔드로 전송 → `docker compose logs backend`에서 `[FRONTEND]` 태그로 확인

### DB 인덱스
- `messages`: `(character_id, created_at)`, `(character_id, is_user)` 복합 인덱스
- `characters`: `(user_id, project_id)`, `(user_id, project_id, last_message_at)` 복합 인덱스
- `projects`: `(user_id)` 인덱스
