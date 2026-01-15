# 노래 맞추기 게임 - 기능 명세서

> Spring Boot 3.4.1 + Java 17 + MariaDB 기반 멀티플레이어 음악 맞추기 게임

---

## 1. 게임 모드

### 1.1 솔로 게임 - 내가 맞추기 (`/game/solo/guess`)

YouTube 영상에서 노래를 듣고 제목을 맞추는 모드

**게임 설정:**
- 라운드 수: 5, 10, 15, 20, 30곡
- 게임 모드:
  - `RANDOM` - 전체 랜덤
  - `FIXED_GENRE` - 장르 고정
  - `FIXED_ARTIST` - 아티스트 고정
  - `FIXED_YEAR` - 연도 고정
  - `GENRE_PER_ROUND` - 매 라운드 장르 선택
  - `ARTIST_PER_ROUND` - 매 라운드 아티스트 선택
  - `YEAR_PER_ROUND` - 매 라운드 연도 선택

**점수 시스템 (시간 기반):**
| 답변 시간 | 점수 |
|----------|------|
| 0-5초 | 100점 |
| 5-8초 | 90점 |
| 8-12초 | 80점 |
| 12-15초 | 70점 |
| 15초+ | 60점 |
| 3번 실패 | 0점 |

**특별 기능:**
- 30곡 도전 모드 (점수 → 소요시간 순 랭킹)
- YouTube 사전 검증으로 Error 2 방지
- 게임 완료 시 뱃지 자동 검증

---

### 1.2 솔로 게임 - 문제내기 (`/game/solo/host`)

호스트가 노래를 틀고 플레이어들이 맞추는 모드

**게임 흐름:**
1. 호스트가 플레이어 이름 입력
2. 매 라운드 장르/아티스트/연도 선택
3. 호스트가 곡을 재생
4. 플레이어들 중 정답자 선택

**점수 시스템:**
| 순서 | 점수 |
|------|------|
| 1번째 정답 | 100점 |
| 2번째 정답 | 70점 |
| 3번째 정답 | 50점 |
| 스킵 | 0점 |

---

### 1.3 멀티플레이어 게임 (`/game/multi`)

실시간 방 기반 대전 모드 (최대 8인)

**게임 흐름:**
1. 로비에서 방 목록 확인
2. 방 생성 또는 참가 (비밀번호 지원)
3. 대기실에서 준비 상태 토글
4. 방장이 게임 시작
5. PREPARING → 모든 플레이어 로드 대기
6. PLAYING → 채팅으로 정답 입력
7. 첫 정답자 100점 획득
8. 모든 라운드 완료 후 결과

**특별 기능:**
- 실시간 채팅 (정답 자동 검증)
- 스킵 투표 (과반 동의 시)
- 방장 강퇴 기능
- 재생 오류 보고

**LP 티어 시스템:**
```
Bronze → Silver → Gold → Platinum → Diamond → Master → Challenger
```
- 100 LP 도달 시 승격
- 0 LP 미만 시 강등
- 게임 결과/플레이어 수/상대 티어에 따라 LP 변동

---

### 1.4 팬 챌린지 (`/game/fan-challenge`)

특정 아티스트의 모든 곡을 맞추는 서바이벌 모드

**난이도:**
| 설정 | BEGINNER | NORMAL | HARDCORE |
|------|----------|--------|----------|
| 노래 길이 | 7초 | 5초 | 3초 |
| 답변 시간 | 10초 | 8초 | 5초 |
| 생명 | 5개 | 3개 | 3개 |
| 초성 힌트 | O | X | X |
| 랭크 기록 | X | X | O |

**게임 종료:**
- 생명 0: 패배
- 모든 곡 정답: 퍼펙트 클리어 (뱃지 획득)

**랭킹:**
- HARDCORE 모드만 공식 기록
- 점수 → 소요시간 순 정렬
- 아티스트별 랭킹

---

## 2. 회원 시스템

### 2.1 인증 (`/auth`)
- 이메일/비밀번호 로그인
- 회원가입 (이메일, 닉네임 중복 확인)
- 세션 토큰 기반 중복 로그인 방지
- IP/User-Agent 기록

### 2.2 마이페이지 (`/mypage`)
- 개인 통계 조회
- 보유 뱃지 목록
- 대표 뱃지 선택
- 신규 뱃지 알림

### 2.3 뱃지 시스템

**카테고리:**
- `BEGINNER` - 첫 게임, 첫 정답
- `SCORE` - 점수 마일스톤 (1000, 5000, 10000...)
- `VICTORY` - 승리 횟수
- `STREAK` - 연속 정답
- `TIER` - 멀티 티어 달성
- `SPECIAL` - 팬챌린지 퍼펙트 클리어

**레어도:**
- COMMON (회색) → RARE (파랑) → EPIC (보라) → LEGENDARY (금색)

---

## 3. 랭킹 시스템 (`/ranking`)

**솔로 게임 랭킹:**
- 총 점수
- 정확도
- 평균 점수
- 게임 수
- 정답 수

**멀티게임 랭킹:**
- 총 점수
- 정확도
- 1등 횟수
- LP 티어

**기간별:**
- 주간 (매주 월요일 초기화)
- 월간 (매월 1일 초기화)
- 역대 최고

**30곡 도전 랭킹:**
- 점수 → 소요시간 순
- 주간/월간/역대

---

## 4. 커뮤니티 (`/board`)

**게시판 카테고리:**
- `REQUEST` - 곡 추천/요청
- `OPINION` - 의견/후기
- `QUESTION` - 질문
- `FREE` - 자유글

**기능:**
- 게시글 작성/수정/삭제
- 댓글
- 좋아요
- 조회수 카운팅
- 검색 및 페이징

---

## 5. 통계 (`/stats`)

- 가장 어려운 곡 TOP 10
- 자주 틀리는 답변 TOP 10
- 개인별 게임 통계
  - 총 게임 수
  - 정답률
  - 최고 점수
  - 평균 점수

---

## 6. 관리자 기능 (`/admin`)

### 6.1 대시보드
- 일일 게임 수
- 활동 회원 수
- 시스템 상태

### 6.2 곡 관리 (`/admin/song`)
- 곡 등록/수정/삭제
- YouTube 검증
- 장르/아티스트/연도 필터링
- 사용 여부 토글

### 6.3 정답 관리 (`/admin/answer`)
- 곡별 정답 변형 추가
- 대체 정답 관리

### 6.4 장르 관리 (`/admin/genre`)
- 장르 CRUD
- 레트로 장르 마이그레이션

### 6.5 회원 관리 (`/admin/member`)
- 회원 목록/상세
- 뱃지 수동 부여
- 통계 조회

### 6.6 게임 기록 (`/admin/game-history`)
- 게임 세션 기록
- 라운드별 상세 정보
- 필터링 (타입, 상태, 기간)

### 6.7 방 모니터링 (`/admin/room`)
- 활성 방 목록
- 참가자 정보
- 게임 진행 상태

### 6.8 채팅 모니터링 (`/admin/chat`)
- 멀티게임 채팅 기록
- 금지어 필터링 확인

### 6.9 금지어 관리 (`/admin/badword`)
- 금지어 CRUD
- 실시간 캐시 리로드

### 6.10 신고 관리 (`/admin/song-report`)
- 사용자 곡 신고 처리

### 6.11 로그인 기록 (`/admin/login-history`)
- 회원별 로그인 히스토리
- IP/User-Agent 추적

### 6.12 배치 관리 (`/admin/batch`)
- 배치 설정 (Cron 표현식)
- 수동 실행
- 실행 이력

---

## 7. 배치 작업 (23개)

### 정리 배치
| 배치 | 기능 | 보존 기간 |
|------|------|----------|
| SessionCleanupBatch | 게임 세션 정리 | 7일 |
| GameSessionCleanupBatch | 완료된 세션 정리 | 30일 |
| RoomCleanupBatch | 비활성 방 정리 | 2시간 |
| ChatCleanupBatch | 채팅 메시지 정리 | 30일 |
| BoardCleanupBatch | 삭제된 게시글 정리 | 90일 |
| LoginHistoryCleanupBatch | 로그인 기록 정리 | 6개월 |
| GameRoundAttemptCleanupBatch | 시도 기록 정리 | 90일 |
| SongReportCleanupBatch | 신고 정리 | 6개월 |
| BatchExecutionHistoryCleanupBatch | 배치 기록 정리 | 6개월 |

### 통계/랭킹 배치
| 배치 | 기능 | 주기 |
|------|------|------|
| DailyStatsBatch | 일일 통계 | 매일 02:00 |
| RankingUpdateBatch | 랭킹 업데이트 | 매시간 |
| RankingSnapshotBatch | 랭킹 스냅샷 | 매일 03:00 |
| WeeklyRankingResetBatch | 주간 초기화 | 매주 월요일 |
| MonthlyRankingResetBatch | 월간 초기화 | 매월 1일 |
| WeeklyPerfectRefreshBatch | 퍼펙트 클리어 갱신 | 매일 |

### 검증 배치
| 배치 | 기능 |
|------|------|
| YouTubeVideoCheckBatch | YouTube 영상 유효성 검사 |
| SongFileCheckBatch | 로컬 파일 존재 여부 |
| DuplicateSongCheckBatch | 중복 곡 확인 |
| SongAnalyticsBatch | 곡 분석 통계 |
| BadgeAwardBatch | 뱃지 자동 부여 |
| FanChallengePerfectCheckBatch | 퍼펙트 클리어 검증 |

### 기타 배치
| 배치 | 기능 |
|------|------|
| InactiveMemberBatch | 휴면 회원 표시 |
| SystemReportBatch | 시스템 보고서 |

---

## 8. 핵심 기술 기능

### 8.1 정답 검증 (AnswerValidationService)

**정규화 프로세스:**
1. 소문자 변환
2. 공백 제거
3. 특수문자 제거 (한글/영문/숫자만 보존)
4. 영문→한글 음운 변환

**검색 범위:**
- `Song.title` (DB 곡 제목)
- `SongAnswer` 테이블 (대체 정답)

**음운 변환 예시:**
- "dynamite" → "다이너마이트"
- "butter" → "버터"

### 8.2 YouTube 검증 (YouTubeValidationService)

**2단계 검증:**
1. oEmbed API로 영상 존재 확인
2. 썸네일 크기 검사 (삭제 영상 감지)

**Error 2 방지:**
- 게임 중 재생 오류 보고 시 곡 무효화
- 배치로 매일 자동 재검증

### 8.3 금지어 필터링 (BadWordService)

- ConcurrentHashMap 캐싱
- 채팅, 댓글 자동 필터링
- 관리자 수정 시 즉시 리로드

### 8.4 세션 관리

- UUID 기반 세션 토큰
- 중복 로그인 감지 및 차단
- forceLogin 옵션 지원

---

## 9. 데이터 모델

### 핵심 엔티티
```
Member (회원)
├─ 인증 정보 (email, password, nickname)
├─ 게임 통계 (총/솔로/멀티/주간)
├─ MultiTier (LP 티어)
└─ MemberBadge (뱃지)

Song (곡)
├─ 기본 정보 (title, artist, youtubeVideoId)
├─ 재생 설정 (startTime, playDuration)
├─ Genre (장르)
└─ SongAnswer (정답 변형)

GameSession (게임 세션)
├─ 설정 (gameType, gameMode, totalRounds)
├─ 진행 (status, currentRound, score)
└─ GameRound (라운드)

GameRoom (멀티 방)
├─ 설정 (roomCode, maxPlayers, isPrivate)
├─ 진행 (currentRound, roundPhase)
├─ GameRoomParticipant (참가자)
└─ GameRoomChat (채팅)

FanChallengeRecord (팬챌린지 기록)
├─ 설정 (artist, difficulty)
└─ 결과 (correctCount, isPerfectClear, bestTimeMs)
```

---

## 10. 프로젝트 규모

| 분류 | 수량 |
|------|------|
| 컨트롤러 | 24개 |
| 서비스 | 17개 |
| 엔티티 | 26개 |
| 배치 작업 | 23개 |
| API 엔드포인트 | 150+ |
| 템플릿 (HTML) | 42개 |

---

## 11. 기술 스택

- **Backend:** Spring Boot 3.4.1, Spring Data JPA, Spring Security
- **Frontend:** Thymeleaf, jQuery, CSS3
- **Database:** MariaDB (JSON 지원)
- **Build:** Maven Wrapper
- **Deploy:** Docker, Docker Compose
- **CI/CD:** GitHub Actions
