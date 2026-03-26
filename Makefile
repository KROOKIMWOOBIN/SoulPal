.PHONY: help dev-backend dev-frontend embed-frontend test test-unit test-integration test-frontend lint lint-fix check docker-up docker-down docker-rebuild doctor

help:
	@echo "SoulPal 개발 명령어"
	@echo ""
	@echo "  make test-unit       백엔드 단위 테스트 (빠름, Docker 불필요)"
	@echo "  make test-integration 백엔드 통합 테스트 (Docker 필요)"
	@echo "  make test-frontend   프론트엔드 Vitest"
	@echo "  make test            단위 + 통합 + 프론트 전체"
	@echo "  make lint            프론트엔드 ESLint 검사"
	@echo "  make lint-fix        ESLint 자동 수정"
	@echo "  make check           lint + test-unit + test-frontend (PR 전 필수)"
	@echo "  make doctor          환경 진단 — 출력을 AI에게 붙여넣으면 상태 파악 완료"
	@echo "  make docker-up       전체 서비스 시작"
	@echo "  make docker-down     서비스 중지"
	@echo "  make docker-rebuild  이미지 재빌드 후 재시작
  make embed-frontend  프론트 빌드 → 백엔드 내장 (최초 1회 또는 배포 빌드 시)"

test: test-unit test-integration test-frontend

test-unit:
	cd backend && ./gradlew test \n		--tests "com.soulpal.service.*" \n		--tests "com.soulpal.exception.*" \n		--tests "com.soulpal.architecture.*" \n		-x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend \n		--no-daemon

test-integration:
	cd backend && ./gradlew test \n		--tests "com.soulpal.integration.*" \n		-x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend \n		--no-daemon

test-frontend:
	cd frontend && npm install --silent && npm run test

lint:
	cd frontend && npm run lint

lint-fix:
	cd frontend && npm run lint:fix

check: lint test-unit test-frontend

doctor:
	@bash scripts/dev-doctor.sh

docker-up:
	docker compose up -d

docker-down:
	docker compose down

docker-rebuild:
	docker compose build backend frontend
	docker compose up -d backend frontend

dev-backend:
	cd backend && ./gradlew bootRun -x buildFrontend -x copyFrontendToBuild -x npmInstallFrontend

dev-frontend:
	cd frontend && npm install --silent && npm run dev

embed-frontend:
	cd backend && ./gradlew copyFrontendToBuild
