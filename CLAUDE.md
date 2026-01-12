# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run application (dev profile, port 8082)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build WAR package
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=GameApplicationTests

# Run a single test method
mvn test -Dtest=GameApplicationTests#testMethodName

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

- **controller/client/** - User-facing: `AuthController`, `GameGuessController`, `GameHostController`, `MultiGameController`, `RankingController`, `SongReportController`
- **controller/admin/** - Admin panel: `AdminSongController`, `AdminGenreController`, `AdminBatchController`, `AdminBadWordController`, `AdminRoomController`, `AdminChatController`, `AdminSongReportController`
- **service/** - Business logic: `GameSessionService`, `MultiGameService`, `SongService`, `MemberService`, `GameRoomService`, `AnswerValidationService`, `YouTubeValidationService`
- **entity/** - JPA entities: `Member`, `Song`, `SongAnswer`, `Genre`, `GameSession`, `GameRound`, `GameRoundAttempt`, `GameRoom`, `GameRoomParticipant`, `GameRoomChat`, `BadWord`, `SongReport`, `BatchConfig`, `DailyStats`
- **repository/** - Spring Data JPA repositories
- **batch/** - 12 scheduled batch jobs managed by `BatchScheduler`
- **config/** - `SecurityConfig` (BCrypt), `WebConfig` (interceptors, file upload), `SchedulerConfig`, `DataInitializer`
- **util/** - `AnswerGeneratorUtil` (English→Korean phonetic conversion for song titles)
- **dto/** - `GameSettings` (multiplayer room configuration)
- **interceptor/** - `AdminInterceptor`, `SessionValidationInterceptor`

### Game Modes

1. **Solo Guess** - User guesses songs with 3 attempts (10/7/5 points)
2. **Solo Host** - User reads clues for others to guess (100/70/50 points)
3. **Multiplayer** - Room-based game with real-time chat polling, first correct answer scores 100 points

### Key Data Flow

- `GameSession` → contains `GameRound` → tracks `GameRoundAttempt` (solo mode)
- `GameRoom` → has `GameRoomParticipant` → stores `GameRoomChat` (multiplayer mode)
- `Song` → has multiple `SongAnswer` for fuzzy matching validation

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

### Batch Jobs (managed by BatchScheduler)

All batches are DB-configurable via `BatchConfig` table with cron expressions:
- `SessionCleanupBatch`, `RoomCleanupBatch`, `ChatCleanupBatch` - Cleanup expired data
- `DailyStatsBatch`, `RankingUpdateBatch`, `WeeklyRankingResetBatch` - Stats & rankings
- `LoginHistoryCleanupBatch`, `InactiveMemberBatch` - Member management
- `SongFileCheckBatch`, `SongAnalyticsBatch`, `YouTubeVideoCheckBatch` - Song integrity
- `SystemReportBatch` - System health reports

### Scoring System

- **Solo Guess:** 10 → 7 → 5 points (3 attempts)
- **Solo Host:** 100 → 70 → 50 points (3 attempts, host reads clues)
- **Multiplayer:** 100 points for first correct answer

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
