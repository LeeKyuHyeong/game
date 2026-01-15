-- 곡 이력 관리 및 주간 퍼펙트 갱신 마이그레이션
-- 실행 전 백업 권장

-- ============================================
-- 1. song_history 테이블 생성
-- ============================================
CREATE TABLE IF NOT EXISTS song_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL,
    artist VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL COMMENT 'ADDED, DELETED, RESTORED',
    action_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_artist_action_at (artist, action_at),
    INDEX idx_song_id (song_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. fan_challenge_record 테이블에 컬럼 추가
-- ============================================

-- 현재시점 퍼펙트 여부
ALTER TABLE fan_challenge_record
ADD COLUMN IF NOT EXISTS is_current_perfect TINYINT(1) DEFAULT 0 COMMENT '현재시점 기준 퍼펙트 여부';

-- 마지막 퍼펙트 검사 시점
ALTER TABLE fan_challenge_record
ADD COLUMN IF NOT EXISTS last_checked_at DATETIME DEFAULT NULL COMMENT '마지막 주간 배치 검사 시점';

-- 기존 isPerfectClear가 true인 레코드는 isCurrentPerfect도 true로 설정
UPDATE fan_challenge_record
SET is_current_perfect = 1
WHERE is_perfect_clear = 1;

-- ============================================
-- 3. batch_config에 주간 퍼펙트 갱신 배치 추가
-- ============================================
INSERT INTO batch_config (batch_id, batch_name, cron_expression, description, is_enabled, created_at)
SELECT 'BATCH_WEEKLY_PERFECT_REFRESH', '주간 퍼펙트 갱신', '0 0 4 * * MON', '매주 월요일 04:00 - 곡 수 변경에 따른 퍼펙트 상태 갱신', TRUE, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM batch_config WHERE batch_id = 'BATCH_WEEKLY_PERFECT_REFRESH'
);

-- ============================================
-- 4. 기존 곡 데이터로 초기 이력 생성 (선택사항)
-- ============================================
-- 현재 활성화된 곡들의 ADDED 이력 생성
-- 주의: 대량 데이터의 경우 시간이 오래 걸릴 수 있음
-- INSERT INTO song_history (song_id, artist, title, action, action_at, created_at)
-- SELECT id, artist, title, 'ADDED', created_at, NOW()
-- FROM song
-- WHERE use_yn = 'Y'
--   AND NOT EXISTS (
--       SELECT 1 FROM song_history sh WHERE sh.song_id = song.id
--   );

-- ============================================
-- 롤백용 쿼리 (필요시)
-- ============================================
-- DROP TABLE IF EXISTS song_history;
-- ALTER TABLE fan_challenge_record DROP COLUMN is_current_perfect;
-- ALTER TABLE fan_challenge_record DROP COLUMN last_checked_at;
-- DELETE FROM batch_config WHERE batch_id = 'BATCH_WEEKLY_PERFECT_REFRESH';

-- ============================================
-- 검증 쿼리
-- ============================================
-- DESCRIBE song_history;
-- DESCRIBE fan_challenge_record;
-- SELECT * FROM batch_config WHERE batch_id = 'BATCH_WEEKLY_PERFECT_REFRESH';
