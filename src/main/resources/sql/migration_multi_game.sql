-- =====================================================
-- Multi Game Tables Migration
-- 실행: 기존 DB에 아래 SQL을 실행하세요
-- =====================================================

-- Game Room Table (멀티게임 방)
CREATE TABLE IF NOT EXISTS game_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_code VARCHAR(6) NOT NULL UNIQUE COMMENT '참가 코드 (6자리)',
    room_name VARCHAR(50) NOT NULL COMMENT '방 이름',
    host_id BIGINT NOT NULL COMMENT '방장 회원 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '상태 (WAITING, PLAYING, FINISHED)',
    max_players INT NOT NULL DEFAULT 8 COMMENT '최대 인원',
    total_rounds INT NOT NULL DEFAULT 10 COMMENT '총 라운드',
    settings TEXT COMMENT '게임 설정 (JSON)',
    is_private BOOLEAN NOT NULL DEFAULT FALSE COMMENT '비공개 여부',
    current_round INT NOT NULL DEFAULT 0 COMMENT '현재 라운드',
    current_song_id BIGINT COMMENT '현재 출제 노래 ID',
    round_phase VARCHAR(20) COMMENT '라운드 단계 (PLAYING, RESULT)',
    round_start_time DATETIME COMMENT '라운드 시작 시간',
    audio_playing BOOLEAN NOT NULL DEFAULT FALSE COMMENT '오디오 재생 상태',
    audio_played_at BIGINT COMMENT '오디오 재생 시작 시각 (epoch millis)',
    winner_id BIGINT COMMENT '현재 라운드 정답자 ID',
    game_session_id BIGINT COMMENT '연결된 게임 세션 ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_room_code (room_code),
    INDEX idx_host_id (host_id),
    INDEX idx_status (status),
    INDEX idx_is_private (is_private),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (host_id) REFERENCES member(id) ON DELETE CASCADE,
    FOREIGN KEY (current_song_id) REFERENCES song(id) ON DELETE SET NULL,
    FOREIGN KEY (winner_id) REFERENCES member(id) ON DELETE SET NULL,
    FOREIGN KEY (game_session_id) REFERENCES game_session(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='멀티게임 방';

-- 기존 테이블에 컬럼 추가 (테이블이 이미 있는 경우)
-- ALTER TABLE game_room ADD COLUMN IF NOT EXISTS audio_playing BOOLEAN NOT NULL DEFAULT FALSE;
-- ALTER TABLE game_room ADD COLUMN IF NOT EXISTS audio_played_at BIGINT;
-- ALTER TABLE game_room ADD COLUMN IF NOT EXISTS winner_id BIGINT;

-- Game Room Participant Table (방 참가자)
CREATE TABLE IF NOT EXISTS game_room_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_room_id BIGINT NOT NULL COMMENT '방 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    is_ready BOOLEAN NOT NULL DEFAULT FALSE COMMENT '준비 상태',
    score INT NOT NULL DEFAULT 0 COMMENT '현재 점수',
    correct_count INT NOT NULL DEFAULT 0 COMMENT '정답 수',
    status VARCHAR(20) NOT NULL DEFAULT 'JOINED' COMMENT '상태 (JOINED, PLAYING, LEFT)',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '참가일시',
    INDEX idx_game_room_id (game_room_id),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status),
    INDEX idx_score (score),
    UNIQUE KEY uk_room_member (game_room_id, member_id),
    FOREIGN KEY (game_room_id) REFERENCES game_room(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방 참가자';

-- Game Room Chat Table (채팅)
CREATE TABLE IF NOT EXISTS game_room_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_room_id BIGINT NOT NULL COMMENT '방 ID',
    member_id BIGINT NOT NULL COMMENT '회원 ID',
    message VARCHAR(500) NOT NULL COMMENT '메시지',
    message_type VARCHAR(20) NOT NULL DEFAULT 'CHAT' COMMENT '메시지 타입 (CHAT, CORRECT_ANSWER, SYSTEM)',
    round_number INT COMMENT '라운드 번호 (정답일 경우)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    INDEX idx_game_room_id (game_room_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (game_room_id) REFERENCES game_room(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='방 채팅';