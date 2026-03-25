#!/usr/bin/env bash
# SoulPal 개발 환경 진단 스크립트
# 실행: bash scripts/dev-doctor.sh  또는  make doctor
#
# AI 지침: 사용자가 이 스크립트 출력을 붙여넣으면
#           환경 상태(Docker, 포트, 로그, .env)를 완전히 파악할 수 있습니다.

set -uo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ok()   { echo -e "${GREEN}[OK]${NC}    $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $1"; }
fail() { echo -e "${RED}[FAIL]${NC}  $1"; }

echo "================================================================"
echo "  SoulPal 개발 환경 진단 — $(date '+%Y-%m-%d %H:%M:%S')"
echo "================================================================"
echo ""

# ── 필수 도구 확인 ──────────────────────────────────────────────────
echo "▶ 필수 도구"

if command -v java &>/dev/null; then
  JAVA_VER=$(java -version 2>&1 | head -1)
  ok "Java: $JAVA_VER"
else
  fail "Java 미설치 (Java 21 필요)"
fi

if command -v docker &>/dev/null; then
  DOCKER_VER=$(docker --version)
  ok "Docker: $DOCKER_VER"
  if docker info &>/dev/null; then
    ok "Docker 데몬: 실행 중"
  else
    fail "Docker 데몬: 중지됨 (Docker Desktop을 시작하세요)"
  fi
else
  fail "Docker 미설치"
fi

if command -v node &>/dev/null; then
  NODE_VER=$(node --version)
  ok "Node.js: $NODE_VER"
else
  fail "Node.js 미설치 (v20 필요)"
fi

echo ""

# ── Docker 컨테이너 상태 ────────────────────────────────────────────
echo "▶ Docker 컨테이너 상태"

for service in soulpal-backend soulpal-frontend soulpal-db soulpal-redis soulpal-ollama; do
  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${service}$"; then
    STATUS=$(docker inspect --format='{{.State.Status}}' "$service" 2>/dev/null || echo "unknown")
    ok "$service: UP ($STATUS)"
  else
    warn "$service: STOPPED"
  fi
done

echo ""

# ── 포트 응답 확인 ──────────────────────────────────────────────────
echo "▶ 서비스 포트 응답"

check_port() {
  local name=$1 host=$2 port=$3
  if command -v curl &>/dev/null; then
    if curl -s --connect-timeout 2 "http://${host}:${port}" &>/dev/null; then
      ok "$name ($host:$port): 응답 있음"
    else
      warn "$name ($host:$port): 응답 없음"
    fi
  else
    warn "$name 포트 확인 불가 (curl 미설치)"
  fi
}

check_port "Backend API"  localhost 9090
check_port "Frontend"     localhost 8080
check_port "Ollama"       localhost 11434

echo ""

# ── 백엔드 헬스 체크 ────────────────────────────────────────────────
echo "▶ 백엔드 헬스 체크"

if command -v curl &>/dev/null; then
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    --connect-timeout 3 http://localhost:9090/actuator/health 2>/dev/null || echo "000")
  if [ "$HTTP_STATUS" = "200" ]; then
    HEALTH_BODY=$(curl -s --connect-timeout 3 http://localhost:9090/actuator/health 2>/dev/null)
    ok "Actuator Health: 200 — $HEALTH_BODY"
  else
    warn "Actuator Health: HTTP $HTTP_STATUS"
  fi
fi

echo ""

# ── 최근 에러 로그 ──────────────────────────────────────────────────
echo "▶ 최근 에러 로그 (백엔드, ERROR 레벨)"

if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^soulpal-backend$"; then
  ERROR_LOGS=$(docker logs soulpal-backend --tail 200 2>&1 | grep '"level":"ERROR"' | tail -5)
  if [ -z "$ERROR_LOGS" ]; then
    ok "최근 ERROR 로그 없음"
  else
    fail "ERROR 로그 발견 (최근 5건):"
    echo "$ERROR_LOGS"
  fi

  WARN_COUNT=$(docker logs soulpal-backend --tail 500 2>&1 | grep -c '"level":"WARN"' || true)
  if [ "$WARN_COUNT" -gt 0 ]; then
    warn "최근 WARN 로그: ${WARN_COUNT}건 (docker logs soulpal-backend 2>&1 | grep '\"level\":\"WARN\"' 로 확인)"
  else
    ok "최근 WARN 로그 없음"
  fi
else
  warn "soulpal-backend 미실행 — 로그 확인 불가"
fi

echo ""

# ── .env 파일 확인 ──────────────────────────────────────────────────
echo "▶ 환경변수 파일"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

if [ -f "$PROJECT_ROOT/.env" ]; then
  ok ".env 파일 존재"
  JWT_SECRET_LINE=$(grep "^JWT_SECRET=" "$PROJECT_ROOT/.env" 2>/dev/null || echo "")
  if [ -n "$JWT_SECRET_LINE" ]; then
    JWT_SECRET_VAL="${JWT_SECRET_LINE#JWT_SECRET=}"
    SECRET_LEN=${#JWT_SECRET_VAL}
    if [ "$SECRET_LEN" -ge 64 ]; then
      ok "JWT_SECRET 길이: ${SECRET_LEN}자 (≥64 OK)"
    else
      fail "JWT_SECRET 길이: ${SECRET_LEN}자 (64자 이상 필요!)"
    fi
  else
    warn "JWT_SECRET 항목 없음 (.env 파일 확인 필요)"
  fi
else
  fail ".env 파일 없음 → cp .env.example .env 실행 후 JWT_SECRET 설정 필요"
fi

echo ""

# ── Redis 상태 ──────────────────────────────────────────────────────
echo "▶ Redis 상태"

if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^soulpal-redis$"; then
  REFRESH_COUNT=$(docker exec soulpal-redis redis-cli KEYS "refresh:*" 2>/dev/null | wc -l || echo "0")
  BLACKLIST_COUNT=$(docker exec soulpal-redis redis-cli KEYS "blacklist:*" 2>/dev/null | wc -l || echo "0")
  ok "Redis 실행 중 — 리프레시 토큰: ${REFRESH_COUNT}개, 블랙리스트: ${BLACKLIST_COUNT}개"
else
  warn "soulpal-redis 미실행"
fi

echo ""
echo "================================================================"
echo "  진단 완료. [FAIL]/[WARN] 항목을 확인하세요."
echo "  AI에게 이 출력 전체를 붙여넣으면 환경 상태를 파악할 수 있습니다."
echo "================================================================"
