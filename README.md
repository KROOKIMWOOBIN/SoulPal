# SoulPal

로컬 LLM 기반 AI 캐릭터 대화 웹 애플리케이션.
외부 AI API 없이 Ollama(llama3)를 사용해 사용자 대화 데이터가 외부로 나가지 않습니다.

> AI 자율 개발 컨텍스트 → **[CLAUDE.md](CLAUDE.md)**

---

## 아키텍처

```
브라우저 → Nginx:8080 → Spring Boot:9090 → PostgreSQL:5432
                                          → Redis:6379
                                          → Ollama:11434
```

| 서비스 | 이미지 | 포트 | 역할 |
|--------|--------|------|------|
| `frontend` | Node → Nginx | 8080 | Vue SPA, `/api/*` 역방향 프록시 |
| `backend` | Gradle 빌드 | 9090 | Spring Boot REST API |
| `db` | `postgres:16-alpine` | 5432 | 메인 데이터베이스 |
| `redis` | `redis:7-alpine` | 6379 | JWT 블랙리스트 / 리프레시 토큰 / Rate Limit |
| `ollama` | `ollama/ollama:latest` | 11434 | LLM 추론 서버 (llama3) |

---

## 기술 스택

| 분류 | 내용 |
|------|------|
| **Backend** | Spring Boot 3.2, Java 21, Spring Security (Stateless JWT), JPA/Hibernate |
| **Database** | PostgreSQL 16 / H2 (로컬 개발), Flyway 마이그레이션 |
| **Cache / 보안** | Redis 7 — JWT 블랙리스트, 리프레시 토큰, Rate Limit |
| **AI** | Ollama llama3 — SSE 스트리밍 + 개인화 컨텍스트 주입 |
| **RAG** | DuckDuckGo 검색 + JSoup 크롤링 — 실시간 웹 지식 주입 |
| **Frontend** | Vue 3, Pinia, Vue Router 4, Vite, Vitest, ESLint |
| **Infra** | Docker Compose, Nginx, Gradle 8.7, GitHub Actions CI |

---

## 빠른 시작 (Docker)

```bash
# 1. 환경 파일 준비
cp .env.example .env
# .env에서 JWT_SECRET 등 값 수정 (JWT_SECRET은 64자 이상 필수)

# 2. 전체 서비스 시작
docker compose up -d

# 3. 최초 실행 시 llama3 모델 다운로드 (~4.7GB)
docker exec -it soulpal-ollama ollama pull llama3
```

브라우저에서 **http://localhost:8080** 접속

```bash
# 로그 확인
docker compose logs -f backend

# 종료
docker compose down
```

> `docker compose down -v`는 DB·Redis·Ollama 데이터 볼륨을 삭제하므로 사용 금지.

---

## 로컬 개발

```bash
# 백엔드 (H2 파일 DB 사용, PostgreSQL 불필요)
make dev-backend
# 또는: cd backend && ./gradlew bootRun -x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend

# 프론트엔드 (http://localhost:5173, API → backend:9090 프록시)
make dev-frontend
# 또는: cd frontend && npm install && npm run dev
```

### 개발 명령어

```bash
make check            # lint + test-unit + test-frontend (PR 전 필수)
make test-unit        # 백엔드 단위 + 아키텍처 테스트 (Docker 불필요)
make test-integration # 백엔드 통합 테스트 (TestContainers 자동 실행)
make test-frontend    # 프론트엔드 Vitest
make lint-fix         # ESLint 자동 수정
make doctor           # 환경 진단
make docker-rebuild   # 이미지 재빌드 후 재시작
```

---

## 환경 변수 (.env)

| 변수 | 설명 |
|------|------|
| `POSTGRES_DB` | DB 이름 |
| `POSTGRES_USER` | DB 사용자 |
| `POSTGRES_PASSWORD` | DB 비밀번호 |
| `REDIS_PORT` | Redis 포트 (기본 6379) |
| `OLLAMA_MODEL` | LLM 모델 (기본 llama3) |
| `SERVER_PORT` | 백엔드 포트 (기본 9090) |
| `JWT_SECRET` | JWT 서명 키 — **64자 이상 랜덤 문자열 필수** |

> `.env`는 `.gitignore`에 포함되어 있습니다. 절대 커밋하지 마세요.

---

## 주요 기능

- **인증** — 액세스 토큰(1h) + 리프레시 토큰(7d), 로그아웃 시 블랙리스트 등록, 401 시 자동 갱신
- **AI 채팅** — Ollama SSE 스트리밍, 개인화 컨텍스트(관심사·감정·친밀도) 자동 주입
- **그룹 채팅** — 여러 AI 캐릭터가 동시에 참여하는 대화방
- **웹 RAG** — 질문 감지 시 DuckDuckGo 검색 → 상위 3페이지 크롤링 → 프롬프트 주입
- **Rate Limit** — 채팅 API 분당 30건, 프론트 에러 로그 API IP별 분당 20건 (Redis 기반)
- **에러 모니터링** — 프론트엔드 JS 에러를 백엔드로 전송 → `[FRONTEND]` 태그로 로그 확인
- **API 문서** — Swagger UI: `http://localhost:9090/swagger-ui.html`
