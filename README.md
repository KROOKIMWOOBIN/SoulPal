# SoulPal

AI 캐릭터와 대화하는 웹 애플리케이션.

---

## 변경 이력

| 날짜 | 내용 | 핵심 포인트 |
|------|------|-------------|
| 2026-03-25 (화) | **보안·성능·아키텍처 전면 고도화** | JWT Secret 강제 검증 / 비밀번호 8자 이상 / H2 콘솔 dev 전용 / Redis Rate Limit / 페이지네이션 / ExecutorService Bean 분리 / Spring Actuator / .env 시크릿 분리 / 프론트 API 모듈화 / Vitest 테스트 / 404 페이지 / SSE 401 자동 리프레시 |
| 2026-03-24 (월) | **그룹 대화방 기능** | 여러 캐릭터를 한 방에 초대해 그룹 채팅, 각 캐릭터가 순서대로 SSE 스트리밍 응답, char-start/token/char-done 이벤트 프로토콜, 캐릭터들이 서로의 발언을 컨텍스트로 참고, HomeView 그룹 탭 추가, Flyway V3 마이그레이션 |
| 2026-03-24 (월) | **캐릭터 생성 복수선택 + 직접입력 커스텀** | 성격·말투·분위기·관심사 복수선택, 모든 단계 "직접 입력" 옵션 추가, custom: 프리픽스 방식으로 Ollama 프롬프트 자동 주입, Flyway V2 마이그레이션 (ElementCollection 테이블 분리) |
| 2026-03-24 (월) | **단위 테스트 전면 작성 + Ollama 로그 제한 + Router lazy loading** | AuthService·CharacterService·RateLimitService·ContextBuilderService·GlobalExceptionHandler 테스트 (JUnit5+Mockito) / Ollama 컨테이너 json-file 로그 드라이버 10MB×3파일 제한 / Vue Router 전 뷰 lazy import 전환 (초기 번들 감소) |
| 2026-03-24 (월) | **CTO 관점 백엔드 고도화** | GlobalExceptionHandler + ErrorCode Enum / Bucket4j Rate Limit / ThreadPoolExecutor bounded / Java11 HttpClient 커넥션 풀 / Caffeine @Cacheable / Flyway 마이그레이션 / Swagger springdoc / verifyOwnership 소유권 검증 / deleteAccount 회원탈퇴 |
| 2026-03-24 (월) | **전면 UI/UX 리디자인** | 그라디언트 디자인 시스템, 글래스모피즘 카드, 캐릭터별 컬러 아바타, 채팅 뒤로가기 프로젝트 복귀 수정, 메시지 입/출력 애니메이션, 생성 플로우 단계 인디케이터 개선 |
| 2026-03-24 (월) | **DB 기반 개인화 Ollama 응답 알고리즘** | ContextBuilderService: 키워드 빈도 추출 / 감정 톤 감지 / 관련성 기반 히스토리 선택 |
| 2026-03-24 (월) | **DB 인덱스 / 웹 RAG / Redis JWT** | 복합 인덱스, DuckDuckGo 크롤링 RAG, 액세스+리프레시 토큰 이중화 |
| 2026-03-24 (월) | **프로젝트 구조 웹 전용 재편** | Flutter 제거, Spring Boot(Tomcat :9090) + Vue(Nginx :8080) Docker 분리 구성 |

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
cd backend/
./gradlew bootRun
```
- Java 21 toolchain 자동 설정
- H2 파일 DB 사용 (기본값, PostgreSQL 없이 동작)
- `spring.h2.console.enabled=true` 시 H2 콘솔 활성화 (`/h2-console`)
- Ollama는 `localhost:11434` 연결

**프론트엔드** (별도 dev 서버):
```bash
cd frontend/
npm install
npm run dev    # http://localhost:5173 (API → backend:9090 프록시)
npm run test   # Vitest 단위 테스트 실행
```

## 기술 스택

| 분류 | 내용 |
|------|------|
| **Backend** | Spring Boot 3.2, Java 21, Spring Security (Stateless JWT), JPA/Hibernate, Spring Actuator |
| **Database** | PostgreSQL 16 (Docker) / H2 파일 DB (로컬 개발), Flyway 마이그레이션 |
| **Cache / 보안** | Redis 7 — JWT 블랙리스트, 리프레시 토큰, Redis 기반 Rate Limit |
| **AI** | Ollama llama3 — SSE 스트리밍 + 개인화 컨텍스트 주입 |
| **RAG** | DuckDuckGo HTML 검색 + JSoup 크롤링 — 실시간 웹 지식 주입 |
| **Frontend** | Vue 3, Pinia, Vue Router 4, Vite, Vitest |
| **Infra** | Docker Compose, Nginx (리버스 프록시 + SSE), Gradle 8.7 |

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

## API 명세

### 인증
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/register` | 회원가입 (비밀번호 8자 이상) |
| POST | `/api/auth/login` | 로그인 |
| POST | `/api/auth/logout` | 로그아웃 (토큰 무효화) |
| POST | `/api/auth/refresh` | 액세스 토큰 재발급 |
| GET | `/api/auth/me` | 내 정보 조회 |

### 프로젝트
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/projects` | 프로젝트 목록 |
| GET | `/api/projects/:id` | 프로젝트 단건 조회 |
| POST | `/api/projects` | 프로젝트 생성 |
| PUT | `/api/projects/:id` | 프로젝트 수정 |
| DELETE | `/api/projects/:id` | 프로젝트 삭제 |

### 캐릭터
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/characters?projectId=&sort=&page=&size=` | 캐릭터 목록 (페이지네이션, 정렬: recent/name/favorite) |
| GET | `/api/characters/:id` | 캐릭터 단건 조회 |
| POST | `/api/characters` | 캐릭터 생성 |
| PUT | `/api/characters/:id` | 캐릭터 수정 |
| DELETE | `/api/characters/:id` | 캐릭터 삭제 |
| POST | `/api/characters/:id/favorite` | 즐겨찾기 토글 |
| GET | `/api/categories` | 카테고리 목록 (성격/관계 등) |

### 채팅
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/chat/messages/:characterId?page=&size=` | 메시지 목록 (페이징) |
| GET | `/api/chat/messages/:characterId/search?q=` | 메시지 검색 |
| POST | `/api/chat/stream` | SSE 스트리밍 채팅 (권장) |
| POST | `/api/chat/send` | 일반 채팅 (동기) |
| POST | `/api/chat/messages/save` | AI 메시지 저장 |
| DELETE | `/api/chat/messages/:characterId/last-ai` | 마지막 AI 메시지 삭제 |
| DELETE | `/api/chat/messages/:characterId` | 대화 전체 삭제 |

### 그룹 채팅
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/group-rooms?projectId=` | 그룹 대화방 목록 |
| GET | `/api/group-rooms/:id` | 대화방 단건 조회 |
| POST | `/api/group-rooms` | 대화방 생성 |
| DELETE | `/api/group-rooms/:id` | 대화방 삭제 |
| GET | `/api/group-chat/messages/:roomId?page=&size=` | 그룹 메시지 목록 |
| POST | `/api/group-chat/stream` | 그룹 SSE 스트리밍 채팅 |

### 운영
| Method | Path | 설명 |
|--------|------|------|
| GET | `/actuator/health` | 헬스체크 (인증 불필요) |
| POST | `/api/logs/error` | 프론트엔드 에러 수집 (IP 분당 20건 제한) |
