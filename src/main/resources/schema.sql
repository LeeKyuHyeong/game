-- Database: song_quiz_dev (for development)
-- Database: song_quiz_prod (for production)

CREATE DATABASE IF NOT EXISTS song_quiz_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS song_quiz_prod DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE song_quiz_dev;

-- Song Table
CREATE TABLE IF NOT EXISTS song (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT '노래 제목',
    artist VARCHAR(255) NOT NULL COMMENT '아티스트',
    youtube_url VARCHAR(500) COMMENT 'YouTube URL',
    start_time INT DEFAULT 0 COMMENT '시작 시간(초)',
    play_duration INT DEFAULT 10 COMMENT '재생 시간(초)',
    genre VARCHAR(50) COMMENT '장르',
    difficulty_level INT DEFAULT 2 COMMENT '난이도 (1:쉬움, 2:보통, 3:어려움)',
    use_yn CHAR(1) DEFAULT 'Y' COMMENT '사용 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    INDEX idx_use_yn (use_yn),
    INDEX idx_genre (genre),
    INDEX idx_difficulty (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='노래 정보';

-- Sample Data
INSERT INTO song (title, artist, youtube_url, start_time, play_duration, genre, difficulty_level, use_yn) VALUES
('Dynamite', 'BTS', 'https://www.youtube.com/watch?v=gdZLi9oWNZg', 30, 10, 'KPOP', 1, 'Y'),
('Butter', 'BTS', 'https://www.youtube.com/watch?v=WMweEpGlu_U', 25, 10, 'KPOP', 1, 'Y'),
('Shape of You', 'Ed Sheeran', 'https://www.youtube.com/watch?v=JGwWNGJdvx8', 45, 10, 'POP', 2, 'Y'),
('Blinding Lights', 'The Weeknd', 'https://www.youtube.com/watch?v=4NRXx6U8ABQ', 50, 10, 'POP', 2, 'Y'),
('Bohemian Rhapsody', 'Queen', 'https://www.youtube.com/watch?v=fJ9rUzIMcZQ', 120, 10, 'ROCK', 3, 'Y');
