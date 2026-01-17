# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

**IMPORTANT:**
- Use `./mvnw` (Maven Wrapper) instead of `mvn` for all commands
- Set JAVA_HOME to Java 17 before running commands

```bash
# Set Java 17 (required for all commands) - adjust path to your JDK location
export JAVA_HOME="/c/Users/rbgud/.jdks/corretto-17.0.12"  # Windows/Git Bash (집 PC)
# export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.12-amzn"  # Linux/macOS with SDKMAN

# Run application (dev profile, port 8082)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build WAR package
./mvnw clean package -DskipTests

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=GameApplicationTests

# Run a single test method
./mvnw test -Dtest=GameApplicationTests#testMethodName

# Docker deployment (production)
docker-compose up -d

# View logs
docker-compose logs -f app
```

## Architecture Overview

This is a **multiplayer music guessing game** built with Spring Boot 3.4.1 + Java 17 + MariaDB.

### Layered Architecture

```
Controller (MVC + REST) → Service (Business Logic) → Repository (JPA) → MariaDB
         ↓
    Thymeleaf Templates (server-side rendering)
```

### Package Structure (`com.kh.game`)

- **controller/client/** - User-facing: `HomeController`, `AuthController`, `GameGuessController`, `GameHostController`, `GameFanChallengeController`, `MultiGameController`, `RankingController`, `SongReportController`, `BoardController`, `StatsController`, `MyPageController`
- **controller/admin/** - Admin panel: `AdminController` (dashboard), `AdminSongController`, `AdminGenreController`, `AdminBatchController`, `AdminBadWordController`, `AdminRoomController`, `AdminChatController`, `AdminSongReportController`, `AdminMemberController`, `AdminGameHistoryController`, `AdminStatsController`, `AdminAnswerController`, `AdminLoginHistoryController`
- **service/** - Business logic: `GameSessionService`, `MultiGameService`, `SongService`, `MemberService`, `GameRoomService`, `AnswerValidationService`, `YouTubeValidationService`, `BoardService`, `WrongAnswerStatsService`, `BatchService`, `GenreMigrationService`, `GenreService`, `MultiTierService`, `FanChallengeService`, `BadgeService`
- **entity/** - JPA entities: `Member`, `MemberLoginHistory`, `Song`, `SongAnswer`, `Genre`, `GameSession`, `GameRound`, `GameRoundAttempt`, `GameRoom`, `GameRoomParticipant`, `GameRoomChat`, `BadWord`, `SongReport`, `BatchConfig`, `BatchExecutionHistory`, `DailyStats`, `Board`, `BoardComment`, `BoardLike`, `Badge`, `MemberBadge`, `MultiTier`, `FanChallengeDifficulty`, `FanChallengeRecord`
- **repository/** - Spring Data JPA repositories
- **batch/** - 23 scheduled batch jobs managed by `BatchScheduler`
- **config/** - `SecurityConfig` (BCrypt), `WebConfig` (interceptors, file upload), `SchedulerConfig`, `DataInitializer`
- **util/** - `AnswerGeneratorUtil` (English→Korean phonetic conversion for song titles)
- **dto/** - `GameSettings` (multiplayer room configuration)
- **interceptor/** - `AdminInterceptor`, `SessionValidationInterceptor`

### Game Modes

1. **Solo Guess** - User guesses songs with 3 attempts. Supports various modes (RANDOM, FIXED_GENRE, FIXED_ARTIST, FIXED_YEAR, per-round selection). Includes "30-song Challenge" for ranked play.
2. **Solo Host** - User reads clues for others to guess (100/70/50 points)
3. **Fan Challenge** - Artist-focused survival mode with difficulty levels (BEGINNER/NORMAL/HARDCORE). Lives system, timed gameplay, artist-specific rankings.
4. **Multiplayer** - Room-based game with real-time chat polling, first correct answer scores 100 points. LP-based tier system (Bronze→Challenger).

### Key Data Flow

- `GameSession` → contains `GameRound` → tracks `GameRoundAttempt` (solo mode)
- `GameRoom` → has `GameRoomParticipant` → stores `GameRoomChat` (multiplayer mode)
- `Song` → has multiple `SongAnswer` for fuzzy matching validation
- `Board` → has `BoardComment` and `BoardLike` (community board)
- `Member` → has `MemberBadge` → links to `Badge` (achievement system)
- `FanChallengeRecord` → tracks artist challenge attempts with difficulty and score

### Multiplayer Flow

1. Create room → join room → toggle ready
2. Host starts game → `PREPARING` phase (all participants load song)
3. Each participant calls `/round-ready` when ready
4. Host starts round → `PLAYING` phase (song plays, chat for answers)
5. First correct answer wins → show answer → next round or game end

### Key Services

- **AnswerValidationService** - Validates user answers with normalization (lowercase, strip spaces/special chars, keep only alphanumeric + Korean), checks both `Song.title` and `SongAnswer` table
- **AnswerGeneratorUtil** - Generates answer variants including English→Korean phonetic conversion using word/phoneme mapping tables (~700 common words)
- **YouTubeValidationService** - Two-phase validation: oEmbed API check → thumbnail size check (detects deleted videos)
- **BadWordService** - Profanity filtering with ConcurrentHashMap cache, auto-reloads on changes
- **SongReportService** - Handles user reports for problematic songs
- **DataInitializer** - Seeds initial bad words (~50 profanities) on startup via CommandLineRunner
- **BoardService** - Community board CRUD with category filtering, comments, and likes
- **MultiTierService** - LP and tier management for multiplayer with ELO-based rating calculations
- **FanChallengeService** - Artist challenge game logic with difficulty-based scoring
- **BadgeService** - Achievement badge management with automatic and manual award conditions

### Tier System (Multiplayer)

LP-based ranking system similar to League of Legends:
- **Tiers:** Bronze → Silver → Gold → Platinum → Diamond → Master → Challenger
- **LP Range:** 0-100 per tier, promotion/demotion at boundaries
- **ELO Rating:** Combined tier+LP rating for matchmaking calculations
- **LP Changes:** Based on game placement, player count, and opponent tier differential

### Badge System

Achievement badges with categories and rarities:
- **Categories:** BEGINNER, SCORE, VICTORY, STREAK, TIER, SPECIAL
- **Rarities:** COMMON (gray), RARE (blue), EPIC (purple), LEGENDARY (gold)
- Awarded via `BadgeAwardBatch` or directly through `BadgeService`

### Batch Jobs (managed by BatchScheduler)

All batches are DB-configurable via `BatchConfig` table with cron expressions:
- **Cleanup:** `SessionCleanupBatch`, `GameSessionCleanupBatch`, `RoomCleanupBatch`, `ChatCleanupBatch`, `BoardCleanupBatch`, `LoginHistoryCleanupBatch`, `BatchExecutionHistoryCleanupBatch`, `GameRoundAttemptCleanupBatch`, `SongReportCleanupBatch`
- **Stats & Rankings:** `DailyStatsBatch`, `RankingUpdateBatch`, `RankingSnapshotBatch`, `WeeklyRankingResetBatch`, `MonthlyRankingResetBatch`
- **Member Management:** `InactiveMemberBatch`, `BadgeAwardBatch`
- **Song Integrity:** `SongFileCheckBatch`, `SongAnalyticsBatch`, `YouTubeVideoCheckBatch`, `DuplicateSongCheckBatch`
- **Fan Challenge:** `FanChallengePerfectCheckBatch`, `WeeklyPerfectRefreshBatch`
- **System:** `SystemReportBatch`

### Scoring System

- **Solo Guess:** Time-based scoring in challenge mode (100/90/80/70/60 based on answer speed)
- **Solo Guess (casual):** 10 → 7 → 5 points (3 attempts)
- **Solo Host:** 100 → 70 → 50 points (3 attempts, host reads clues)
- **Fan Challenge:** BEGINNER (7s play), NORMAL (5s play), HARDCORE (3s play, ranked)
- **Multiplayer:** 100 points for first correct answer

### Community Board

- Categories: REQUEST (곡 추천/요청), OPINION (의견/후기), QUESTION (질문), FREE (자유)
- Features: Comments, likes, view count tracking
- Status: ACTIVE, DELETED, HIDDEN

## Configuration

- **Dev profile:** Port 8082, MariaDB localhost:3306/song (root/1234), JPA ddl-auto=update
- **Prod profile:** Uses environment variables for DB credentials, Docker volumes for persistence
- **Admin auth:** DB-based via `Member` table with `role=ADMIN`
- **File uploads:** `uploads/songs/`, max 50MB
- **Session timeout:** 30 minutes
- **Admin routes:** Protected by `AdminInterceptor` (/admin/**)
- **Docker memory:** App 512MB, DB 256MB

## CI/CD

GitHub Actions workflow at `.github/workflows/deploy.yml`:
- Triggers on push to main or manual dispatch (ignores *.md, .claude/**, .gitignore, LICENSE)
- Builds Docker image → pushes to Docker Hub → deploys to server via SSH
- Requires secrets: `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`, `SERVER_PORT`, `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`

## CSS Style Guide

**⚠️ CRITICAL: 색상 하드코딩 금지!**

### 필수 규칙
- **색상값을 직접 쓰지 말고 반드시 CSS 변수 사용** (`#1e293b` ❌ → `var(--text-primary)` ✅)
- 새로운 색상이 필요하면 `common.css`의 `:root`에 변수 추가 후 사용
- 라이트/다크 모드 모두 지원해야 함

### 테마 시스템 구조
```
common.css
├── :root { }                    → 라이트 모드 기본값
├── [data-theme="dark"] { }      → 다크 모드 오버라이드
└── .game-page { }               → 게임 페이지 전용 (항상 다크)
```

### 주요 CSS 변수
| 용도 | 변수명 |
|------|--------|
| 기본 텍스트 | `--text-primary` |
| 보조 텍스트 | `--text-secondary` |
| 흐린 텍스트 | `--text-muted` |
| 기본 배경 | `--bg-base` |
| 카드 배경 | `--bg-surface` |
| 강조 배경 | `--bg-elevated` |
| 테두리 | `--border-color` |

### 주의사항: `.game-page` 클래스
- `.game-page`는 CSS 변수를 다크 모드로 강제 오버라이드함
- **흰색 배경 요소**에서 `var(--text-primary)`를 쓰면 흰 글씨가 됨!
- 해결법: 해당 CSS 파일의 다크 테마 섹션(`[data-theme="dark"]`)에서 별도 처리

### 예시: 흰색 배경 모달 처리
```css
/* 기본 스타일 */
.modal-content {
    background: white;
    color: var(--text-primary);  /* 라이트 모드에서 정상 작동 */
}

/* 다크 모드 또는 .game-page에서 오버라이드 */
[data-theme="dark"] .modal-content,
.game-page .modal-content {
    background: var(--bg-surface);  /* 다크 배경으로 변경 */
    color: var(--text-primary);     /* 이제 흰 글씨가 맞음 */
}
```

### 반응형 브레이크포인트

| 구분 | 브레이크포인트 | 용도 |
|------|---------------|------|
| **모바일** | `max-width: 480px` | 스마트폰 세로 |
| **태블릿** | `max-width: 768px` | 태블릿/스마트폰 가로 |
| **데스크탑** | `min-width: 769px` | PC (기본) |
| **대형** | `min-width: 1200px` | 대형 모니터 (선택적) |

```css
/* 기본: 데스크탑 스타일 */
.container { padding: 2rem; }

/* 태블릿 이하 */
@media (max-width: 768px) {
    .container { padding: 1.5rem; }
}

/* 모바일 */
@media (max-width: 480px) {
    .container { padding: 1rem; }
}
```

**⚠️ 금지:** 임의의 브레이크포인트 사용 (450px, 375px 등)

**예외:** `game-multi.css`는 채팅+스코어보드 레이아웃 특성상 `900px` 브레이크포인트 허용

### Z-Index 계층

| 계층 | 값 | 용도 |
|------|-----|------|
| 기본 | `1-10` | 로컬 스태킹 (카드 내 요소) |
| 고정 | `100` | 사이드바, 네비게이션 |
| 드롭다운 | `500` | 드롭다운, 팝오버 |
| 모달 배경 | `900` | 모달 오버레이 |
| 모달 | `1000` | 모달 콘텐츠 |
| 토스트 | `5000` | 알림 토스트 |
| 최상위 | `10000` | 뱃지 토스트 (특수) |

**⚠️ 금지:** 임의의 z-index 값 사용 (9999, 99999 등)

### 단위 & 값 규칙

- **길이:** `rem` 사용 (px 금지, 예외: `1px` 보더)
- **Border-radius:** `rem` 단위만 사용
  - 작음: `0.25rem` / 중간: `0.5rem` / 큼: `0.75rem` / 매우 큼: `1rem` / 원형: `50%`
- **Spacing:** `0.25rem` 단위로 증가 (0.5rem, 0.75rem, 1rem, 1.5rem, 2rem)
- **Transition:** `0.2s` (빠름) / `0.3s` (기본) / `0.5s` (느림)

```css
/* ❌ 잘못된 예 */
border-radius: 6px;      /* px 사용 */
border-radius: 0.375rem; /* 비표준 값 */
padding: 13px;           /* px 사용 */

/* ✅ 올바른 예 */
border-radius: 0.25rem;  /* 표준 값 */
border-radius: 0.5rem;   /* 표준 값 */
padding: 0.75rem;        /* rem 사용 */
```

### RGBA 투명도 처리

투명도가 필요한 색상도 변수 사용 권장. `common.css`에 정의된 `--overlay-*` 변수 활용:

```css
/* ❌ 하드코딩 */
background: rgba(0, 0, 0, 0.5);
color: rgba(255, 255, 255, 0.7);

/* ✅ 변수 사용 */
background: var(--overlay-medium);
color: var(--text-secondary);
```
