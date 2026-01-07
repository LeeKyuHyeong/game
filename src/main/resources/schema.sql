-- Database: song_quiz_dev (for development)
-- Database: song_quiz_prod (for production)

CREATE DATABASE IF NOT EXISTS song_quiz_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS song_quiz_prod DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE song_quiz_dev;

-- Genre Table
CREATE TABLE IF NOT EXISTS genre (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE COMMENT '장르 코드',
    name VARCHAR(50) NOT NULL COMMENT '장르명',
    display_order INT DEFAULT 0 COMMENT '정렬순서',
    use_yn CHAR(1) DEFAULT 'Y' COMMENT '사용 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_use_yn (use_yn),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='장르 정보';

-- Song Table
CREATE TABLE IF NOT EXISTS song (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '노래 제목',
    artist VARCHAR(255) NOT NULL COMMENT '아티스트',
    file_path VARCHAR(500) COMMENT 'MP3 파일 경로',
    start_time INT DEFAULT 0 COMMENT '시작 시간(초)',
    play_duration INT DEFAULT 10 COMMENT '재생 시간(초)',
    genre_id BIGINT COMMENT '장르 ID',
    release_year INT COMMENT '발매연도',
    is_solo BOOLEAN DEFAULT FALSE COMMENT '솔로여부',
    use_yn CHAR(1) DEFAULT 'Y' COMMENT '사용 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_use_yn (use_yn),
    INDEX idx_genre_id (genre_id),
    INDEX idx_release_year (release_year),
    INDEX idx_is_solo (is_solo),
    FOREIGN KEY (genre_id) REFERENCES genre(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='노래 정보';

-- Genre Sample Data (30개)
INSERT INTO genre (code, name, display_order, use_yn) VALUES
('KPOP', 'K-POP', 1, 'Y'),
('KPOP_IDOL', 'K-POP 아이돌', 2, 'Y'),
('KPOP_HIPHOP', 'K-POP 힙합', 3, 'Y'),
('KPOP_RNB', 'K-POP R&B', 4, 'Y'),
('KPOP_BALLAD', 'K-POP 발라드', 5, 'Y'),
('KPOP_INDIE', 'K-POP 인디', 6, 'Y'),
('KPOP_TROT', '트로트', 7, 'Y'),
('POP', 'POP', 10, 'Y'),
('POP_DANCE', 'POP 댄스', 11, 'Y'),
('POP_BALLAD', 'POP 발라드', 12, 'Y'),
('JPOP', 'J-POP', 15, 'Y'),
('CPOP', 'C-POP', 16, 'Y'),
('HIPHOP', '힙합', 20, 'Y'),
('RAP', '랩', 21, 'Y'),
('RNB', 'R&B', 22, 'Y'),
('SOUL', '소울', 23, 'Y'),
('ROCK', '록', 30, 'Y'),
('ROCK_CLASSIC', '클래식 록', 31, 'Y'),
('ROCK_INDIE', '인디 록', 32, 'Y'),
('METAL', '메탈', 33, 'Y'),
('PUNK', '펑크', 34, 'Y'),
('EDM', 'EDM', 40, 'Y'),
('HOUSE', '하우스', 41, 'Y'),
('TECHNO', '테크노', 42, 'Y'),
('DISCO', '디스코', 43, 'Y'),
('JAZZ', '재즈', 50, 'Y'),
('BLUES', '블루스', 51, 'Y'),
('CLASSIC', '클래식', 60, 'Y'),
('OST', 'OST/영화음악', 70, 'Y'),
('CCM', 'CCM/가스펠', 80, 'Y');

-- Song Sample Data
INSERT INTO song (title, artist, file_path, start_time, play_duration, genre_id, release_year, is_solo, use_yn) VALUES
('Dynamite', 'BTS', NULL, 30, 10, 1, 2020, FALSE, 'Y'),
('Butter', 'BTS', NULL, 25, 10, 1, 2021, FALSE, 'Y'),
('Shape of You', 'Ed Sheeran', NULL, 45, 10, 8, 2017, TRUE, 'Y'),
('Blinding Lights', 'The Weeknd', NULL, 50, 10, 8, 2019, TRUE, 'Y'),
('Bohemian Rhapsody', 'Queen', NULL, 120, 10, 17, 1975, FALSE, 'Y');

-- Song Answer Table (노래별 여러 정답)
CREATE TABLE IF NOT EXISTS song_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    song_id BIGINT NOT NULL COMMENT '노래 ID',
    answer VARCHAR(255) NOT NULL COMMENT '정답',
    is_primary BOOLEAN DEFAULT FALSE COMMENT '대표 정답 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    INDEX idx_song_id (song_id),
    INDEX idx_answer (answer),
    FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='노래 정답';

-- Song Answer Sample Data
INSERT INTO song_answer (song_id, answer, is_primary) VALUES
(1, 'Dynamite', TRUE),
(1, '다이너마이트', FALSE),
(2, 'Butter', TRUE),
(2, '버터', FALSE),
(3, 'Shape of You', TRUE),
(3, '쉐이프오브유', FALSE),
(4, 'Blinding Lights', TRUE),
(4, '블라인딩라이츠', FALSE),
(5, 'Bohemian Rhapsody', TRUE),
(5, '보헤미안랩소디', FALSE);

-- Game Session Table
CREATE TABLE IF NOT EXISTS game_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_uuid VARCHAR(36) COMMENT '브라우저 세션 UUID',
    nickname VARCHAR(50) NOT NULL COMMENT '별명',
    game_type VARCHAR(20) NOT NULL COMMENT '게임유형 (SOLO_HOST, SOLO_GUESS)',
    game_mode VARCHAR(20) NOT NULL COMMENT '게임모드 (RANDOM, GENRE_PER_ROUND, FIXED_GENRE)',
    total_rounds INT DEFAULT 10 COMMENT '총 라운드 수',
    completed_rounds INT DEFAULT 0 COMMENT '완료된 라운드 수',
    total_score INT DEFAULT 0 COMMENT '총점',
    correct_count INT DEFAULT 0 COMMENT '맞춘 개수',
    skip_count INT DEFAULT 0 COMMENT '스킵 횟수',
    status VARCHAR(20) NOT NULL DEFAULT 'PLAYING' COMMENT '상태 (PLAYING, COMPLETED, ABANDONED)',
    settings JSON COMMENT '게임 설정',
    started_at DATETIME COMMENT '게임 시작시간',
    ended_at DATETIME COMMENT '게임 종료시간',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    INDEX idx_session_uuid (session_uuid),
    INDEX idx_nickname (nickname),
    INDEX idx_game_type (game_type),
    INDEX idx_status (status),
    INDEX idx_total_score (total_score),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게임 세션';

-- Game Round Table
CREATE TABLE IF NOT EXISTS game_round (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_session_id BIGINT NOT NULL COMMENT '게임 세션 ID',
    round_number INT NOT NULL COMMENT '라운드 번호',
    song_id BIGINT COMMENT '출제된 노래 ID',
    genre_id BIGINT COMMENT '해당 라운드 장르 ID',
    play_start_time INT COMMENT '노래 재생 시작 위치(초)',
    play_duration INT COMMENT '재생 시간(초)',
    user_answer VARCHAR(255) COMMENT '사용자 입력 답',
    is_correct BOOLEAN COMMENT '정답 여부',
    answer_time_ms BIGINT COMMENT '답변까지 걸린 시간(ms)',
    hint_used BOOLEAN DEFAULT FALSE COMMENT '힌트 사용 여부',
    hint_type VARCHAR(20) COMMENT '사용한 힌트 타입',
    score INT DEFAULT 0 COMMENT '획득 점수',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '상태 (WAITING, PLAYING, ANSWERED, SKIPPED, TIMEOUT)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    INDEX idx_game_session_id (game_session_id),
    INDEX idx_song_id (song_id),
    INDEX idx_genre_id (genre_id),
    INDEX idx_is_correct (is_correct),
    INDEX idx_status (status),
    FOREIGN KEY (game_session_id) REFERENCES game_session(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE SET NULL,
    FOREIGN KEY (genre_id) REFERENCES genre(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게임 라운드';

-- 테스트 데이터
INSERT INTO game_session (session_uuid, nickname, game_type, game_mode, total_rounds, completed_rounds, total_score, correct_count, skip_count, status, settings, started_at, ended_at) VALUES
('550e8400-e29b-41d4-a716-446655440001', '음악매니아', 'SOLO_GUESS', 'RANDOM', 10, 10, 850, 8, 1, 'COMPLETED', '{"timeLimit":30,"hintEnabled":true,"hintType":"INITIAL","skipAllowed":true,"maxSkips":3,"scorePerCorrect":100,"timeBonusRate":10}', '2024-01-15 14:30:00', '2024-01-15 14:45:00'),
('550e8400-e29b-41d4-a716-446655440002', '노래왕', 'SOLO_GUESS', 'GENRE_PER_ROUND', 10, 10, 920, 9, 0, 'COMPLETED', '{"timeLimit":30,"hintEnabled":true,"hintType":"INITIAL","skipAllowed":true,"maxSkips":3,"scorePerCorrect":100,"timeBonusRate":10}', '2024-01-15 15:00:00', '2024-01-15 15:12:00'),
('550e8400-e29b-41d4-a716-446655440003', '퀴즈마스터', 'SOLO_HOST', 'FIXED_GENRE', 5, 5, 500, 5, 0, 'COMPLETED', '{"timeLimit":30,"hintEnabled":false,"skipAllowed":false,"fixedGenreId":1,"scorePerCorrect":100}', '2024-01-15 16:00:00', '2024-01-15 16:08:00');