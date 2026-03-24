-- SoulPal 초기 스키마 (V1)
-- 이 파일은 신규 설치 시 실행됩니다.
-- 기존 운영 DB는 flyway.baseline-on-migrate=true 로 baseline=V1 처리됩니다.

CREATE TABLE IF NOT EXISTS users (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS projects (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_project_user ON projects(user_id);

CREATE TABLE IF NOT EXISTS characters (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36)  NOT NULL,
    project_id      VARCHAR(36),
    name            VARCHAR(50)  NOT NULL,
    relationship_id VARCHAR(50),
    personality_id  VARCHAR(50),
    speech_style_id VARCHAR(50),
    appearance_id   VARCHAR(50),
    last_message    TEXT,
    last_message_at TIMESTAMP,
    is_favorite     BOOLEAN      DEFAULT FALSE,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_char_user_project      ON characters(user_id, project_id);
CREATE INDEX IF NOT EXISTS idx_char_user_project_time ON characters(user_id, project_id, last_message_at);

CREATE TABLE IF NOT EXISTS character_interests (
    character_id VARCHAR(36) NOT NULL,
    interest_id  VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id           VARCHAR(36) NOT NULL PRIMARY KEY,
    character_id VARCHAR(36) NOT NULL,
    content      TEXT        NOT NULL,
    is_user      BOOLEAN,
    created_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_msg_char_time ON messages(character_id, created_at);
CREATE INDEX IF NOT EXISTS idx_msg_char_user ON messages(character_id, is_user);
