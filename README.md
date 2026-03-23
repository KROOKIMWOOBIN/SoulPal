# SoulPal Web

AI 캐릭터와 대화하는 웹 애플리케이션.

## 아키텍처

```
┌──────────────────────────────────────────────────────┐
│                    Docker Compose                     │
│                                                      │
│  ┌──────────┐   ┌────────────┐   ┌───────────────┐  │
│  │ postgres │   │   ollama   │   │    backend    │  │
│  │  :5432   │   │   :11434   │   │    :8080      │  │
│  │          │   │            │   │ Spring Boot   │  │
│  │ PostgreSQL│  │ LLM 서버   │   │ + Vue SPA     │  │
│  └──────────┘   └────────────┘   └───────────────┘  │
│                                                      │
│  Volume: postgres_data  /  ollama_data               │
└──────────────────────────────────────────────────────┘
```

## 서비스 명세

| 서비스 | 이미지 | 포트 | 설명 |
|--------|--------|------|------|
| `db` | `postgres:16-alpine` | 5432 | 메인 데이터베이스 |
| `ollama` | `ollama/ollama:latest` | 11434 | LLM 추론 서버 |
| `ollama-init` | `ollama/ollama:latest` | - | 최초 모델 pull (1회성) |
| `backend` | 빌드 | 8080 | Spring Boot API + Vue SPA |

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_URL` | `jdbc:postgresql://db:5432/soulpal` | DB 연결 URL |
| `DB_USER` | `soulpal` | DB 사용자 |
| `DB_PASSWORD` | `soulpal1234` | DB 비밀번호 |
| `OLLAMA_BASE_URL` | `http://ollama:11434` | Ollama 서버 주소 |
| `OLLAMA_MODEL` | `llama3` | 사용할 LLM 모델 |
| `JWT_SECRET` | (기본값) | JWT 서명 키 (**배포 시 반드시 변경**) |

## 실행 방법

### Docker (권장)

```bash
cd web/

# 전체 서비스 시작
docker compose up -d

# 로그 확인
docker compose logs -f backend
docker compose logs -f ollama

# 종료
docker compose down
```

브라우저에서 http://localhost:8080 접속

> **최초 실행 시**: `ollama-init` 컨테이너가 자동으로 `llama3` 모델을 pull합니다 (~4.7GB).
> 모델 데이터는 `ollama_data` 볼륨에 영구 저장되므로 재시작 시 재다운로드 없음.

### 로컬 개발

**백엔드** (IntelliJ 기준):
```bash
cd web/backend/
./gradlew bootRun
```
- Java 21 toolchain 자동 설정
- H2 인메모리 DB 사용 (기본값)
- Ollama는 localhost:11434 사용

**프론트엔드** (별도 dev 서버):
```bash
cd web/frontend/
npm install
npm run dev   # http://localhost:5173 (API → :8080 프록시)
```

## 기술 스택

- **Backend**: Spring Boot 3.2, Java 21, Spring Security (JWT), JPA
- **Database**: PostgreSQL 16 (Docker) / H2 (로컬 개발)
- **AI**: Ollama (llama3 모델)
- **Frontend**: Vue 3, Pinia, Vue Router, Vite
- **빌드**: Gradle 8.7 (node-gradle 플러그인으로 Vue 빌드 통합)
- **인프라**: Docker Compose

## API 명세

### 인증
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/register` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| GET | `/api/auth/me` | 내 정보 |

### 프로젝트
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/projects` | 프로젝트 목록 |
| POST | `/api/projects` | 프로젝트 생성 |
| PUT | `/api/projects/:id` | 프로젝트 수정 |
| DELETE | `/api/projects/:id` | 프로젝트 삭제 |

### 캐릭터
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/characters?projectId=` | 캐릭터 목록 (프로젝트 필터) |
| POST | `/api/characters` | 캐릭터 생성 |
| PUT | `/api/characters/:id` | 캐릭터 수정 |
| DELETE | `/api/characters/:id` | 캐릭터 삭제 |
| POST | `/api/characters/:id/favorite` | 즐겨찾기 토글 |

### 채팅
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/chat/messages/:characterId` | 메시지 목록 (페이징) |
| POST | `/api/chat/stream` | SSE 스트리밍 채팅 |
| POST | `/api/chat/send` | 일반 채팅 |
| DELETE | `/api/chat/messages/:characterId` | 대화 전체 삭제 |
