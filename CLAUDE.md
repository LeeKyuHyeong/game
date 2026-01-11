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

# Docker deployment
docker-compose up -d
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

- **controller/client/** - User-facing: `AuthController`, `GameGuessController`, `GameHostController`, `MultiGameController`
- **controller/admin/** - Admin panel: song/genre/user management, batch operations
- **service/** - Business logic: `GameSessionService`, `MultiGameService`, `SongService`, `MemberService`
- **entity/** - JPA entities: `Member`, `Song`, `GameSession`, `GameRoom`, `GameRoomParticipant`
- **repository/** - Spring Data JPA repositories
- **batch/** - Scheduled tasks: `BatchScheduler`, `SessionCleanupBatch`
- **config/** - `SecurityConfig` (BCrypt), `WebConfig` (interceptors, file upload)

### Game Modes

1. **Solo Guess** - User guesses songs with 3 attempts (10/7/5 points)
2. **Solo Host** - User reads clues for others to guess
3. **Multiplayer** - Room-based game with real-time chat, first correct answer scores 100 points

### Key Data Flow

- `GameSession` → contains `GameRound` → tracks `GameRoundAttempt` (solo mode)
- `GameRoom` → has `GameRoomParticipant` → stores `GameRoomChat` (multiplayer mode)
- `Song` → has multiple `SongAnswer` for fuzzy matching validation

## Configuration

- **Dev profile:** Port 8082, MariaDB localhost:3306/song (root/1234)
- **Admin login (dev):** admin / 1234
- **File uploads:** `uploads/songs/`, max 50MB
- **Session timeout:** 30 minutes
- **Admin routes:** Protected by `AdminInterceptor` (/admin/**)

## CI/CD

GitHub Actions workflow at `.github/workflows/deploy.yml`:
- Triggers on push to main or manual dispatch
- Builds Docker image → pushes to Docker Hub → deploys to server via SSH
- Requires secrets: `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`, `SERVER_PORT`, `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `DB_USERNAME`, `DB_PASSWORD`
