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
- **batch/** - 20 scheduled batch jobs managed by `BatchScheduler`
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
- **Stats & Rankings:** `DailyStatsBatch`, `RankingUpdateBatch`, `WeeklyRankingResetBatch`, `MonthlyRankingResetBatch`
- **Member Management:** `InactiveMemberBatch`, `BadgeAwardBatch`
- **Song Integrity:** `SongFileCheckBatch`, `SongAnalyticsBatch`, `YouTubeVideoCheckBatch`, `DuplicateSongCheckBatch`
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
