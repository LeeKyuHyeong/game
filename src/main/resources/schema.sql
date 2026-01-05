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