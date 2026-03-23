# ── Stage 1: 빌드 ─────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

# 의존성 레이어 캐싱
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -q || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test \
    -x npmInstallFrontend -x buildFrontend -x copyFrontendToBuild

# ── Stage 2: 런타임 ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
