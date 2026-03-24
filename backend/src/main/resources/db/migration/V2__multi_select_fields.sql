-- V2: 성격·말투·분위기 단일 컬럼 → ElementCollection 테이블로 변환
-- 기존 personality_id, speech_style_id, appearance_id 컬럼의 데이터를 마이그레이션

-- character_personalities 테이블 생성
CREATE TABLE IF NOT EXISTS character_personalities (
    character_id VARCHAR(36) NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    personality_id VARCHAR(500) NOT NULL
);

-- character_speech_styles 테이블 생성
CREATE TABLE IF NOT EXISTS character_speech_styles (
    character_id VARCHAR(36) NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    speech_style_id VARCHAR(500) NOT NULL
);

-- character_appearances 테이블 생성
CREATE TABLE IF NOT EXISTS character_appearances (
    character_id VARCHAR(36) NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    appearance_id VARCHAR(500) NOT NULL
);

-- 기존 단일 값 컬럼에서 데이터 마이그레이션
INSERT INTO character_personalities (character_id, personality_id)
    SELECT id, personality_id FROM characters WHERE personality_id IS NOT NULL;

INSERT INTO character_speech_styles (character_id, speech_style_id)
    SELECT id, speech_style_id FROM characters WHERE speech_style_id IS NOT NULL;

INSERT INTO character_appearances (character_id, appearance_id)
    SELECT id, appearance_id FROM characters WHERE appearance_id IS NOT NULL;

-- 기존 단일 컬럼 제거
ALTER TABLE characters DROP COLUMN IF EXISTS personality_id;
ALTER TABLE characters DROP COLUMN IF EXISTS speech_style_id;
ALTER TABLE characters DROP COLUMN IF EXISTS appearance_id;
