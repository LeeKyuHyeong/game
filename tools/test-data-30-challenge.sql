-- 30개 챌린지 테스트 데이터
-- 실행 전 비밀번호 해시: BCrypt('1234') 사용

-- 테스트 회원 추가 (이미 존재하면 UPDATE)
INSERT INTO member (email, password, nickname, username, role, status, created_at, updated_at,
    weekly_best_30_score, weekly_best_30_at,
    monthly_best_30_score, monthly_best_30_at,
    all_time_best_30_score, all_time_best_30_at,
    guess_games, guess_score, guess_correct, guess_rounds)
VALUES
-- 1위: 음악천재 (280점)
('test1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '음악천재', 'musicgenius', 'USER', 'ACTIVE', NOW(), NOW(),
    280, DATE_SUB(NOW(), INTERVAL 1 DAY),
    280, DATE_SUB(NOW(), INTERVAL 2 DAY),
    280, DATE_SUB(NOW(), INTERVAL 3 DAY),
    15, 3200, 290, 400),

-- 2위: 노래박사 (265점)
('test2@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '노래박사', 'songdoctor', 'USER', 'ACTIVE', NOW(), NOW(),
    265, DATE_SUB(NOW(), INTERVAL 2 DAY),
    265, DATE_SUB(NOW(), INTERVAL 3 DAY),
    265, DATE_SUB(NOW(), INTERVAL 5 DAY),
    12, 2800, 260, 350),

-- 3위: 퀴즈왕 (258점)
('test3@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '퀴즈왕', 'quizking', 'USER', 'ACTIVE', NOW(), NOW(),
    258, DATE_SUB(NOW(), INTERVAL 1 DAY),
    258, DATE_SUB(NOW(), INTERVAL 4 DAY),
    258, DATE_SUB(NOW(), INTERVAL 10 DAY),
    10, 2500, 240, 320),

-- 4위: 음감러버 (245점)
('test4@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '음감러버', 'musiclover', 'USER', 'ACTIVE', NOW(), NOW(),
    245, DATE_SUB(NOW(), INTERVAL 3 DAY),
    245, DATE_SUB(NOW(), INTERVAL 5 DAY),
    245, DATE_SUB(NOW(), INTERVAL 15 DAY),
    8, 2100, 200, 280),

-- 5위: 송마스터 (238점)
('test5@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '송마스터', 'songmaster', 'USER', 'ACTIVE', NOW(), NOW(),
    238, DATE_SUB(NOW(), INTERVAL 2 DAY),
    238, DATE_SUB(NOW(), INTERVAL 6 DAY),
    238, DATE_SUB(NOW(), INTERVAL 20 DAY),
    7, 1900, 180, 250),

-- 6위: 음악덕후 (225점)
('test6@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '음악덕후', 'musicnerd', 'USER', 'ACTIVE', NOW(), NOW(),
    225, DATE_SUB(NOW(), INTERVAL 4 DAY),
    225, DATE_SUB(NOW(), INTERVAL 7 DAY),
    225, DATE_SUB(NOW(), INTERVAL 25 DAY),
    6, 1700, 160, 220),

-- 7위: 리듬감왕 (218점)
('test7@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '리듬감왕', 'rhythmking', 'USER', 'ACTIVE', NOW(), NOW(),
    218, DATE_SUB(NOW(), INTERVAL 1 DAY),
    218, DATE_SUB(NOW(), INTERVAL 8 DAY),
    218, DATE_SUB(NOW(), INTERVAL 30 DAY),
    5, 1500, 140, 200),

-- 8위: 멜로디러버 (205점)
('test8@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '멜로디러버', 'melodylover', 'USER', 'ACTIVE', NOW(), NOW(),
    205, DATE_SUB(NOW(), INTERVAL 5 DAY),
    205, DATE_SUB(NOW(), INTERVAL 10 DAY),
    205, DATE_SUB(NOW(), INTERVAL 35 DAY),
    4, 1300, 120, 180),

-- 9위: 음치탈출 (198점)
('test9@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '음치탈출', 'notonedeaf', 'USER', 'ACTIVE', NOW(), NOW(),
    198, DATE_SUB(NOW(), INTERVAL 3 DAY),
    198, DATE_SUB(NOW(), INTERVAL 12 DAY),
    198, DATE_SUB(NOW(), INTERVAL 40 DAY),
    3, 1100, 100, 150),

-- 10위: 노래초보 (185점)
('test10@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '노래초보', 'songnewbie', 'USER', 'ACTIVE', NOW(), NOW(),
    185, DATE_SUB(NOW(), INTERVAL 6 DAY),
    185, DATE_SUB(NOW(), INTERVAL 14 DAY),
    185, DATE_SUB(NOW(), INTERVAL 45 DAY),
    2, 900, 80, 120),

-- 11-15위: 추가 테스트 유저
('test11@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '음악고수', 'musicpro', 'USER', 'ACTIVE', NOW(), NOW(),
    175, DATE_SUB(NOW(), INTERVAL 2 DAY),
    175, DATE_SUB(NOW(), INTERVAL 15 DAY),
    175, DATE_SUB(NOW(), INTERVAL 50 DAY),
    2, 800, 70, 100),

('test12@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '퀴즈도전자', 'quizchallenger', 'USER', 'ACTIVE', NOW(), NOW(),
    168, DATE_SUB(NOW(), INTERVAL 4 DAY),
    168, DATE_SUB(NOW(), INTERVAL 18 DAY),
    168, DATE_SUB(NOW(), INTERVAL 55 DAY),
    2, 700, 60, 90),

('test13@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '노래사랑', 'songlove', 'USER', 'ACTIVE', NOW(), NOW(),
    155, DATE_SUB(NOW(), INTERVAL 5 DAY),
    155, DATE_SUB(NOW(), INTERVAL 20 DAY),
    155, DATE_SUB(NOW(), INTERVAL 60 DAY),
    1, 600, 50, 80),

('test14@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '음악여행자', 'musictraveler', 'USER', 'ACTIVE', NOW(), NOW(),
    142, DATE_SUB(NOW(), INTERVAL 6 DAY),
    142, DATE_SUB(NOW(), INTERVAL 22 DAY),
    142, DATE_SUB(NOW(), INTERVAL 65 DAY),
    1, 500, 40, 70),

('test15@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBH.ZPSG3VwzL5x6FQVAFpavKQa', '퀴즈러너', 'quizrunner', 'USER', 'ACTIVE', NOW(), NOW(),
    130, DATE_SUB(NOW(), INTERVAL 7 DAY),
    130, DATE_SUB(NOW(), INTERVAL 25 DAY),
    130, DATE_SUB(NOW(), INTERVAL 70 DAY),
    1, 400, 30, 60)

ON DUPLICATE KEY UPDATE
    weekly_best_30_score = VALUES(weekly_best_30_score),
    weekly_best_30_at = VALUES(weekly_best_30_at),
    monthly_best_30_score = VALUES(monthly_best_30_score),
    monthly_best_30_at = VALUES(monthly_best_30_at),
    all_time_best_30_score = VALUES(all_time_best_30_score),
    all_time_best_30_at = VALUES(all_time_best_30_at),
    guess_games = VALUES(guess_games),
    guess_score = VALUES(guess_score),
    guess_correct = VALUES(guess_correct),
    guess_rounds = VALUES(guess_rounds);

-- 확인 쿼리
SELECT id, nickname,
    weekly_best_30_score,
    monthly_best_30_score,
    all_time_best_30_score
FROM member
WHERE weekly_best_30_score IS NOT NULL
ORDER BY weekly_best_30_score DESC;
