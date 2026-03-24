-- V3: 그룹 대화방 + 그룹 메시지 테이블

CREATE TABLE IF NOT EXISTS group_rooms (
    id               VARCHAR(36)  PRIMARY KEY,
    user_id          VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id       VARCHAR(36),
    name             VARCHAR(100) NOT NULL,
    last_message     TEXT,
    last_message_at  TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_group_room_user ON group_rooms(user_id);

CREATE TABLE IF NOT EXISTS group_room_characters (
    room_id      VARCHAR(36) NOT NULL REFERENCES group_rooms(id) ON DELETE CASCADE,
    character_id VARCHAR(36) NOT NULL
);

CREATE TABLE IF NOT EXISTS group_messages (
    id                  VARCHAR(36)  PRIMARY KEY,
    room_id             VARCHAR(36)  NOT NULL REFERENCES group_rooms(id) ON DELETE CASCADE,
    content             TEXT         NOT NULL,
    sender_character_id VARCHAR(36),
    sender_name         VARCHAR(100),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_group_msg_room_time ON group_messages(room_id, created_at);
