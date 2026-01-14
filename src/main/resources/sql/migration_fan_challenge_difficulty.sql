-- Fan Challenge 난이도 시스템 마이그레이션
-- 실행 전 백업 권장

-- 1. difficulty 컬럼 추가 (기존 레코드는 HARDCORE로 설정)
ALTER TABLE fan_challenge_record
ADD COLUMN IF NOT EXISTS difficulty VARCHAR(20) NOT NULL DEFAULT 'HARDCORE';

-- 2. 기존 unique constraint 삭제 (존재하는 경우)
-- MariaDB/MySQL에서는 ALTER TABLE DROP INDEX 사용
-- 인덱스 이름을 확인하고 실행: SHOW INDEX FROM fan_challenge_record;
-- ALTER TABLE fan_challenge_record DROP INDEX UK_fan_challenge_member_artist;

-- 3. 새로운 unique constraint 추가 (member_id, artist, difficulty)
-- 기존 인덱스가 있다면 먼저 삭제 후 실행
-- ALTER TABLE fan_challenge_record
-- ADD UNIQUE INDEX UK_fan_challenge_member_artist_difficulty (member_id, artist, difficulty);

-- ============================================
-- 수동 실행용 개별 쿼리
-- ============================================

-- 현재 인덱스 확인
-- SHOW INDEX FROM fan_challenge_record;

-- 기존 unique 인덱스 삭제 (인덱스 이름 확인 후 실행)
-- ALTER TABLE fan_challenge_record DROP INDEX UK_member_artist;
-- 또는
-- ALTER TABLE fan_challenge_record DROP INDEX UK_fan_challenge_member_artist;

-- 새 unique 인덱스 생성
-- CREATE UNIQUE INDEX UK_fan_challenge_member_artist_difficulty
-- ON fan_challenge_record (member_id, artist, difficulty);

-- ============================================
-- 롤백용 쿼리 (필요시)
-- ============================================
-- ALTER TABLE fan_challenge_record DROP COLUMN difficulty;
-- DROP INDEX UK_fan_challenge_member_artist_difficulty ON fan_challenge_record;
-- CREATE UNIQUE INDEX UK_fan_challenge_member_artist ON fan_challenge_record (member_id, artist);
