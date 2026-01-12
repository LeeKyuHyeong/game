# Multiplayer Music Guessing Game Test Plan

## Application Overview

This test plan covers the multiplayer music guessing game functionality of Song Quiz. The application is a real-time multiplayer game where players compete to guess song titles by typing answers in chat. The game supports multiple players (2-10), configurable rounds (5-20), and different game modes (Random/Fixed Genre). The scoring system awards 100 points to the first player who correctly guesses the song title. The test plan covers the complete player journey from authentication through room creation, game play, and final results.

## Test Scenarios

### 1. Authentication and Initial Setup

**Seed:** `seed.spec.ts`

#### 1.1. User Registration

**File:** `tests/auth/user-registration.spec.ts`

**Steps:**
  1. Navigate to http://localhost:8082
  2. Click on '로그인' or '회원가입' link if available, otherwise navigate directly to '/auth/register'
  3. Enter a unique email address (e.g., testuser{timestamp}@example.com)
  4. Verify email availability indicator shows '✓' (available)
  5. Enter username/성명 (e.g., 'Test User')
  6. Enter password (minimum 4 characters, e.g., 'test1234')
  7. Enter matching password confirmation
  8. Verify password match indicator shows success message
  9. Enter nickname (2-20 characters, e.g., 'Player1')
  10. Click '회원가입' button
  11. Wait for success message '회원가입이 완료되었습니다'

**Expected Results:**
  - Registration form displays all required fields
  - Email availability check works in real-time
  - Password confirmation validation works correctly
  - Success message appears after registration
  - User is redirected to login page within 1.5 seconds

#### 1.2. User Login

**File:** `tests/auth/user-login.spec.ts`

**Steps:**
  1. Navigate to http://localhost:8082/auth/login
  2. Enter registered email address
  3. Enter correct password
  4. Click '로그인' button
  5. Wait for redirect to home page

**Expected Results:**
  - Login form accepts valid credentials
  - User is successfully authenticated
  - User is redirected to home page ('/')
  - User info appears in header (nickname visible)
  - Session is established

#### 1.3. Guest Mode Access

**File:** `tests/auth/guest-access.spec.ts`

**Steps:**
  1. Navigate to http://localhost:8082
  2. Look for '게스트로 플레이' button or link
  3. Click guest access option
  4. Verify access to game features

**Expected Results:**
  - Guest mode is accessible without registration
  - Home page displays game mode options
  - Multiplayer lobby is accessible to guests

### 2. Room Creation and Configuration

**Seed:** `seed.spec.ts`

#### 2.1. Create Public Room with Default Settings

**File:** `tests/room-creation/create-public-room-default.spec.ts`

**Steps:**
  1. Login as a registered user or access as guest
  2. Navigate to home page (http://localhost:8082)
  3. Click '멀티게임 로비' button under '멀티 게임' section
  4. Verify redirect to multiplayer lobby (/game/multi)
  5. Click '방 만들기' button (➕ 방 만들기)
  6. Verify redirect to room creation page (/game/multi/create)
  7. Enter room name (e.g., '테스트방')
  8. Keep default settings (8명, 10 라운드, 전체 랜덤 mode)
  9. Ensure '비공개 방' checkbox is unchecked
  10. Click '방 만들기' button
  11. Wait for redirect to waiting room

**Expected Results:**
  - Room creation form displays all configuration options
  - Default values are: maxPlayers=8, totalRounds=10, gameMode=RANDOM
  - Room is created successfully
  - User is redirected to waiting room (/game/multi/room/{roomCode})
  - Room code is displayed and visible
  - User appears as host (👑 icon)
  - User is marked as 'ready' status shows '방장'

#### 2.2. Create Private Room with Custom Settings

**File:** `tests/room-creation/create-private-room-custom.spec.ts`

**Steps:**
  1. Login as a registered user
  2. Navigate to room creation page (/game/multi/create)
  3. Enter room name (e.g., '비공개 테스트')
  4. Change max players to 4명
  5. Change total rounds to 5 라운드
  6. Select '장르 고정' radio button
  7. Wait for genre dropdown to appear
  8. Select a genre from dropdown (e.g., 'K-POP')
  9. Check '🔒 비공개 방' checkbox
  10. Click '방 만들기' button
  11. Verify room is created with private badge

**Expected Results:**
  - Genre selection dropdown appears when '장르 고정' is selected
  - All custom settings are accepted
  - Private room is created successfully
  - Room displays private indicator (🔒 비공개 badge)
  - Room settings reflect custom configuration (4명, 5 라운드)

#### 2.3. Room Creation Validation

**File:** `tests/room-creation/room-creation-validation.spec.ts`

**Steps:**
  1. Navigate to room creation page
  2. Leave room name field empty
  3. Click '방 만들기' button
  4. Verify validation error for empty room name
  5. Enter room name exceeding 30 characters
  6. Verify input is limited to 30 characters
  7. Select '장르 고정' mode without selecting a genre
  8. Click '방 만들기' button
  9. Verify validation error for missing genre selection

**Expected Results:**
  - Room name is required (validation message appears)
  - Room name is limited to 30 characters maximum
  - Genre must be selected when '장르 고정' mode is chosen
  - Appropriate validation messages are displayed
  - Room is not created with invalid data

### 3. Room Discovery and Joining

**Seed:** `seed.spec.ts`

#### 3.1. Browse and Join Public Room from Lobby

**File:** `tests/room-joining/join-from-lobby.spec.ts`

**Steps:**
  1. Have one user create a public room (as per previous test)
  2. Open a second browser/incognito window and login as different user
  3. Navigate to multiplayer lobby (/game/multi)
  4. Verify created room appears in '공개 방 목록'
  5. Check room displays correct information (room name, host nickname, player count, rounds)
  6. Click '입장' button on the desired room card
  7. Wait for redirect to waiting room
  8. Verify user appears in participants list

**Expected Results:**
  - Public rooms are visible in lobby list
  - Room information is accurate (name, host, player count, rounds)
  - Player count shows current/max (e.g., '1/8명')
  - User successfully joins the room
  - User appears in waiting room participant list
  - User status shows '대기중' (not ready)
  - Room player count increments

#### 3.2. Join Room by Code

**File:** `tests/room-joining/join-by-code.spec.ts`

**Steps:**
  1. Have one user create a room and note the room code
  2. Login as a second user
  3. Navigate to multiplayer lobby
  4. Locate '참가 코드 입력' input field
  5. Enter the 6-character room code
  6. Click '참가' button
  7. Wait for redirect to waiting room

**Expected Results:**
  - Room code input accepts 6-character codes
  - User successfully joins room by code
  - User is redirected to correct waiting room
  - User appears in participant list

#### 3.3. Join Private Room with Code Search

**File:** `tests/room-joining/join-private-room.spec.ts`

**Steps:**
  1. Have one user create a private room (비공개 방) and note the room code
  2. Login as a second user
  3. Navigate to /game/multi/join or use join by code feature
  4. Enter the private room code in '방 코드' field
  5. Click '검색' button
  6. Verify room information is displayed
  7. Verify '🔒 비공개' badge is visible
  8. Note that password field should appear (if password was set)
  9. Click '참가하기' button
  10. Wait for successful join or password prompt

**Expected Results:**
  - Private room can be found by code
  - Room displays private indicator
  - Room information is shown after search
  - Join button becomes enabled after successful search
  - User can join private room with correct code

#### 3.4. Room Full Prevention

**File:** `tests/room-joining/room-full-prevention.spec.ts`

**Steps:**
  1. Create a room with maxPlayers=2
  2. Have first user join the room (now 2/2 players)
  3. Attempt to join with a third user from lobby
  4. Verify join is prevented

**Expected Results:**
  - Full rooms are marked as full in lobby
  - Join button is disabled or shows appropriate message
  - User receives error message when attempting to join full room
  - Player count accurately reflects room capacity

#### 3.5. Search Rooms by Name

**File:** `tests/room-joining/search-rooms.spec.ts`

**Steps:**
  1. Create multiple rooms with different names (e.g., 'Alpha Room', 'Beta Room', 'Gamma Room')
  2. Navigate to multiplayer lobby
  3. Locate search input field ('방 이름 검색...')
  4. Enter search term (e.g., 'Alpha')
  5. Trigger search (type and wait or press Enter)
  6. Verify only matching rooms are displayed

**Expected Results:**
  - Search functionality filters room list
  - Only rooms matching search term are visible
  - Search is case-insensitive
  - Empty search shows all rooms
  - Search updates room list dynamically

### 4. Waiting Room and Pre-Game

**Seed:** `seed.spec.ts`

#### 4.1. Toggle Ready Status

**File:** `tests/waiting-room/toggle-ready.spec.ts`

**Steps:**
  1. Join a room as non-host participant
  2. Verify '🎮 준비하기' button is visible
  3. Verify button does not show '✅ 준비 완료' initially
  4. Click '준비하기' button
  5. Wait for ready status to update
  6. Verify button changes to '✅ 준비 완료'
  7. Verify participant card shows '준비완료' badge
  8. Click button again to un-ready
  9. Verify button reverts to '🎮 준비하기'
  10. Verify participant status changes to '대기중'

**Expected Results:**
  - Ready button is visible to non-host participants
  - Clicking ready button toggles ready status
  - Button text changes to reflect current status
  - Participant card shows correct ready/waiting badge
  - Ready status updates in real-time for all participants
  - API endpoint /game/multi/room/{roomCode}/ready is called successfully

#### 4.2. Real-time Participant Updates

**File:** `tests/waiting-room/realtime-participant-updates.spec.ts`

**Steps:**
  1. Have two users in waiting room (different browsers)
  2. In first browser, toggle ready status
  3. Observe second browser (should auto-update via polling)
  4. Verify participant status updates without manual refresh
  5. Have a third user join the room
  6. Verify all browsers show the new participant

**Expected Results:**
  - Participant list updates in real-time (via polling)
  - Ready status changes are visible to all participants
  - New participant joins are detected automatically
  - Player count updates for all users
  - No manual refresh required

#### 4.3. Waiting Room Chat

**File:** `tests/waiting-room/waiting-room-chat.spec.ts`

**Steps:**
  1. Join waiting room with at least 2 participants
  2. Locate chat input field at bottom of chat section
  3. Type a message (e.g., 'Hello everyone!')
  4. Press Enter or click '전송' button
  5. Verify message appears in chat messages area
  6. Verify message shows sender nickname
  7. In second browser window, verify message appears for other participant
  8. Send multiple messages and verify chat scrolling

**Expected Results:**
  - Chat input field accepts text up to 200 characters
  - Messages are sent successfully
  - Messages appear with sender nickname
  - Messages are received by all participants in real-time
  - Chat auto-scrolls to newest messages
  - Enter key sends message

#### 4.4. Host Kick Player

**File:** `tests/waiting-room/host-kick-player.spec.ts`

**Steps:**
  1. Create room as host
  2. Have another user join the room
  3. As host, locate '강퇴' button next to participant (not yourself)
  4. Click '강퇴' button
  5. Confirm action if prompted
  6. Verify kicked participant is removed from list
  7. In kicked user's browser, verify redirect or error message

**Expected Results:**
  - Host sees '강퇴' button for other participants
  - Host does not see '강퇴' button for themselves
  - Kicked player is immediately removed from room
  - Kicked player receives notification and is redirected
  - Player count decrements correctly
  - API endpoint /game/multi/room/{roomCode}/kick/{memberId} works

#### 4.5. Leave Room

**File:** `tests/waiting-room/leave-room.spec.ts`

**Steps:**
  1. Join a room as non-host participant
  2. Click '나가기' button
  3. Verify redirect to lobby (/game/multi)
  4. Navigate back to lobby
  5. Verify player no longer appears in room
  6. Verify player count decreased

**Expected Results:**
  - Leave button is visible and functional
  - User successfully leaves room
  - User is redirected to lobby
  - User is removed from participant list
  - Room player count updates correctly
  - If host leaves, room should close or transfer host (test this separately)

#### 4.6. Copy Room Code

**File:** `tests/waiting-room/copy-room-code.spec.ts`

**Steps:**
  1. Join or create a room
  2. Locate room code display in header (참가 코드 section)
  3. Note the displayed room code
  4. Click '📋' copy button next to room code
  5. Verify clipboard contains room code (if browser allows)

**Expected Results:**
  - Room code is clearly displayed
  - Copy button is visible and clickable
  - Room code is copied to clipboard
  - Visual feedback confirms copy action (if implemented)

### 5. Game Start and Round Preparation

**Seed:** `seed.spec.ts`

#### 5.1. Host Start Game - All Players Ready

**File:** `tests/game-start/host-start-all-ready.spec.ts`

**Steps:**
  1. Create room with at least 2 participants
  2. Have all non-host participants click '준비하기'
  3. Verify all participants show '준비완료' status
  4. As host, verify '🚀 게임 시작' button is visible
  5. Click '게임 시작' button
  6. Wait for redirect to game play page (/game/multi/room/{roomCode}/play)
  7. Verify page loads with game interface

**Expected Results:**
  - Host can see start game button
  - Game starts successfully when all players are ready
  - All participants are redirected to play page
  - Game enters PREPARING phase
  - Round preparation interface is displayed
  - API endpoint /game/multi/room/{roomCode}/start is successful

#### 5.2. Host Start Game - Not All Ready

**File:** `tests/game-start/host-start-not-all-ready.spec.ts`

**Steps:**
  1. Create room with at least 2 participants
  2. Have only some participants ready (not all)
  3. As host, click '🚀 게임 시작' button
  4. Verify either: button is disabled OR warning message appears OR game starts anyway

**Expected Results:**
  - System handles not-all-ready scenario appropriately
  - Either game requires all ready, or starts with warning
  - Clear feedback is provided to host
  - Participants are informed of game state

#### 5.3. Round Preparing Phase - Participant Ready

**File:** `tests/game-start/round-preparing-ready.spec.ts`

**Steps:**
  1. Start a game and enter play page
  2. Verify 'PREPARING' phase UI is displayed
  3. Verify message: '광고가 있다면 먼저 시청해주세요'
  4. Verify '준비 완료' button is visible
  5. Verify ready status count shows '0 / N 명 준비 완료'
  6. Click '준비 완료' button
  7. Verify button becomes disabled or changes appearance
  8. Verify ready count increments (e.g., '1 / 2 명 준비 완료')
  9. Have all other participants click ready
  10. Wait for automatic transition to PLAYING phase

**Expected Results:**
  - PREPARING phase displays correct instructions
  - Participant can signal ready state
  - Ready count updates in real-time for all participants
  - When all participants ready, phase automatically transitions to PLAYING
  - API endpoint /game/multi/room/{roomCode}/round-ready is called successfully
  - Song loading completes before playing starts

#### 5.4. Host Start Round Control

**File:** `tests/game-start/host-start-round.spec.ts`

**Steps:**
  1. Start game as host
  2. Verify host sees '라운드 대기중' state initially
  3. Verify '🎵 라운드 시작' button is visible to host only
  4. Click '라운드 시작' button
  5. Verify transition to PREPARING phase
  6. Wait for all participants to ready
  7. Verify automatic transition to PLAYING phase when all ready

**Expected Results:**
  - Host has exclusive control to start each round
  - Round start button is visible only to host
  - Clicking start initiates PREPARING phase
  - All participants move to preparing together
  - Participants cannot start round (only host can)

### 6. Game Play and Answer Submission

**Seed:** `seed.spec.ts`

#### 6.1. Round Playing - Audio Playback

**File:** `tests/game-play/round-playing-audio.spec.ts`

**Steps:**
  1. Start game and progress to PLAYING phase
  2. Verify '재생 중...' status is displayed
  3. Verify music icon animation (🎶 playing)
  4. Verify progress bar is visible and animating
  5. Verify time display shows current time / total time
  6. Wait for a few seconds
  7. Verify progress bar advances
  8. Verify current time increments

**Expected Results:**
  - Audio player interface is displayed
  - Song plays automatically when PLAYING phase starts
  - Progress bar reflects playback position
  - Time counter updates correctly
  - Visual feedback indicates song is playing
  - Audio element or YouTube player loads successfully

#### 6.2. Submit Correct Answer First

**File:** `tests/game-play/submit-correct-answer-first.spec.ts`

**Steps:**
  1. Start game with 2+ participants and enter PLAYING phase
  2. As first participant, locate chat input field
  3. Type the correct song title (obtain from game state or known test song)
  4. Press Enter or click '전송' button
  5. Wait for answer validation
  6. Verify song stops playing
  7. Verify transition to RESULT phase
  8. Verify winner announcement displays your nickname
  9. Verify '+100점' score is shown
  10. Verify your score in scoreboard increments by 100

**Expected Results:**
  - Correct answer is recognized immediately
  - First correct answer wins the round
  - Song playback stops
  - Winner information is displayed to all participants
  - Winner receives 100 points
  - Score updates in real-time scoreboard
  - Answer validation ignores case, spaces, and special characters

#### 6.3. Submit Incorrect Answer

**File:** `tests/game-play/submit-incorrect-answer.spec.ts`

**Steps:**
  1. During PLAYING phase, type an incorrect answer in chat
  2. Press Enter to send
  3. Verify message appears in chat
  4. Verify no winner announcement
  5. Verify song continues playing
  6. Verify no score change occurs
  7. Wait for another participant to submit correct answer or timeout

**Expected Results:**
  - Incorrect answers appear as regular chat messages
  - Round continues after incorrect answer
  - No points are awarded for incorrect answers
  - Song playback is uninterrupted
  - Participant can submit multiple attempts

#### 6.4. Multiple Answers - First Wins

**File:** `tests/game-play/multiple-answers-first-wins.spec.ts`

**Steps:**
  1. Start game with 3+ participants in PLAYING phase
  2. Have first participant submit correct answer
  3. Immediately have second participant submit same correct answer
  4. Verify only first participant is declared winner
  5. Verify only first participant receives 100 points
  6. Verify second participant does not receive points

**Expected Results:**
  - Only the first correct answer is counted
  - System correctly identifies timing of submissions
  - Second and subsequent correct answers are ignored
  - Only one winner per round (first to answer correctly)
  - Scoring is fair and accurate based on submission order

#### 6.5. Answer Normalization

**File:** `tests/game-play/answer-normalization.spec.ts`

**Steps:**
  1. During PLAYING phase with known song title (e.g., 'Love Me'),
  2. Test answer with different cases: 'LOVE ME', 'love me', 'LoVe Me'
  3. Verify all case variations are accepted
  4. In next round, test with spaces: 'LoveMe', 'Love  Me' (double space)
  5. Verify space variations are accepted
  6. Test with special characters: 'Love-Me', 'Love!Me', 'Love.Me'
  7. Verify special character variations are accepted

**Expected Results:**
  - Answer validation is case-insensitive
  - Answer validation ignores extra spaces
  - Answer validation ignores special characters
  - Answer validation keeps only alphanumeric and Korean characters
  - Flexible validation improves user experience

#### 6.6. Real-time Scoreboard Updates

**File:** `tests/game-play/realtime-scoreboard.spec.ts`

**Steps:**
  1. Start game with 3+ participants
  2. Verify scoreboard displays all participants with 0 points initially
  3. Complete a round with one participant winning
  4. Verify winner's score updates to 100 in scoreboard
  5. Verify scoreboard order reflects highest score first
  6. Complete another round with different winner
  7. Verify both scores are accurate and sorted

**Expected Results:**
  - Scoreboard displays all active participants
  - Scores initialize at 0
  - Scores update immediately after each round
  - Scoreboard sorts by score (highest first)
  - Score changes are visible to all participants in real-time

#### 6.7. Round Result Display

**File:** `tests/game-play/round-result-display.spec.ts`

**Steps:**
  1. Complete a round (someone submits correct answer)
  2. Verify 'RESULT' phase is displayed
  3. Verify '🎉 정답!' heading appears
  4. Verify song title is revealed
  5. Verify artist name is displayed
  6. Verify song metadata (year, genre) is shown
  7. Verify winner nickname is displayed
  8. Verify '정답자' label and '+100점' score
  9. Verify '문제 신고' button is available

**Expected Results:**
  - Result screen shows complete song information
  - Winner is clearly identified
  - Score awarded is displayed
  - Song metadata is accurate
  - Report button allows flagging problematic songs

#### 6.8. Chat During Game Play

**File:** `tests/game-play/chat-during-play.spec.ts`

**Steps:**
  1. During PLAYING phase, type both answers and casual messages
  2. Send correct answer (should trigger win)
  3. Send incorrect answer (should appear as chat)
  4. Send casual message like 'This is fun!' (should appear as chat)
  5. Verify all messages appear in chat area
  6. Verify chat messages from other participants appear
  7. Verify chat scrolls with new messages

**Expected Results:**
  - Chat accepts both answers and general messages
  - Correct answers trigger game logic AND appear in chat
  - Incorrect answers appear as chat messages
  - Real-time chat synchronization works during gameplay
  - Chat history is maintained throughout game

### 7. Multi-Round Game Flow

**Seed:** `seed.spec.ts`

#### 7.1. Progress Through Multiple Rounds

**File:** `tests/multi-round/progress-multiple-rounds.spec.ts`

**Steps:**
  1. Create room with 5 rounds
  2. Start game and complete round 1
  3. Verify round counter shows '라운드 1 / 5'
  4. As host, click '다음 라운드 →' button after result shown
  5. Verify transition back to round waiting state
  6. Start round 2 and complete it
  7. Verify round counter shows '라운드 2 / 5'
  8. Continue through all 5 rounds
  9. Verify final round completes and redirects to results page

**Expected Results:**
  - Round counter accurately displays current round / total rounds
  - Host can advance to next round after each result
  - Each round follows: WAITING → PREPARING → PLAYING → RESULT cycle
  - Scores accumulate across rounds
  - Final round triggers game end and redirect to results
  - All participants progress together through rounds

#### 7.2. Host Next Round Control

**File:** `tests/multi-round/host-next-round.spec.ts`

**Steps:**
  1. Complete a round as host
  2. Verify '다음 라운드 →' button appears only to host
  3. As non-host participant in different browser, verify button is not visible
  4. As host, click '다음 라운드 →'
  5. Verify all participants transition to next round waiting state
  6. Verify round number increments for all participants

**Expected Results:**
  - Only host can advance to next round
  - Next round button visible only to host
  - All participants transition together when host advances
  - Non-host participants wait for host to continue

#### 7.3. Score Accumulation

**File:** `tests/multi-round/score-accumulation.spec.ts`

**Steps:**
  1. Start game with 3 participants
  2. Round 1: Have participant A win (100 points)
  3. Verify participant A score is 100
  4. Round 2: Have participant B win (100 points)
  5. Verify participant A still has 100, participant B has 100
  6. Round 3: Have participant A win again
  7. Verify participant A has 200, participant B has 100
  8. Round 4: Have participant C win
  9. Verify scores: A=200, B=100, C=100
  10. Verify scoreboard maintains correct ordering

**Expected Results:**
  - Scores persist across rounds
  - Each win adds 100 points to participant total
  - Scoreboard accurately reflects cumulative scores
  - Score calculation is correct for all participants
  - Multiple wins by same player accumulate properly

#### 7.4. No Winner Round - Timeout

**File:** `tests/multi-round/no-winner-timeout.spec.ts`

**Steps:**
  1. Start a round in PLAYING phase
  2. Do not submit any correct answers from any participant
  3. Wait for song to complete playing (timeout)
  4. Verify system handles no-winner scenario
  5. Verify result shows 'no winner' or appropriate message
  6. Verify no points are awarded
  7. Verify host can still proceed to next round

**Expected Results:**
  - Round completes even without correct answer
  - System gracefully handles timeout/no winner
  - Result phase shows appropriate message
  - No points awarded when no one answers correctly
  - Game can continue to next round
  - Participants are not stuck in endless round

### 8. Game Completion and Results

**Seed:** `seed.spec.ts`

#### 8.1. Final Results Display

**File:** `tests/game-end/final-results-display.spec.ts`

**Steps:**
  1. Complete all rounds in a game (e.g., 5 rounds)
  2. After final round result, wait for automatic redirect or host action
  3. Verify redirect to results page (/game/multi/room/{roomCode}/result)
  4. Verify '🏆 게임 종료!' heading
  5. Verify room name is displayed
  6. Verify podium section shows top 3 players
  7. Verify 1st place gets 🥇, 2nd gets 🥈, 3rd gets 🥉
  8. Verify each podium player shows nickname, score, correct count
  9. Verify full ranking list shows all participants with rank, nickname, correct count, score
  10. Verify current user row is highlighted (marked with '(나)')

**Expected Results:**
  - Results page displays after final round
  - Podium visualization shows top 3 players
  - Crown (👑) appears on 1st place
  - All participant rankings are accurate
  - Scores and correct counts match game performance
  - Current user is highlighted in ranking list
  - Host indicator (👑) appears next to host's name in full ranking

#### 8.2. Results Ranking Accuracy

**File:** `tests/game-end/results-ranking-accuracy.spec.ts`

**Steps:**
  1. Play game with controlled wins to create specific scores:
  2. Player A wins 3 rounds (300 points)
  3. Player B wins 2 rounds (200 points)
  4. Player C wins 1 round (100 points)
  5. Player D wins 0 rounds (0 points)
  6. Verify final ranking order: A (1st), B (2nd), C (3rd), D (4th)
  7. Verify correct counts: A=3, B=2, C=1, D=0
  8. Verify scores: A=300, B=200, C=100, D=0

**Expected Results:**
  - Ranking is sorted by total score (highest first)
  - Correct count accurately reflects rounds won
  - Final score equals (correct count × 100)
  - Ties are handled appropriately (if scores equal)
  - All participants appear in final ranking

#### 8.3. Return to Lobby from Results

**File:** `tests/game-end/return-to-lobby.spec.ts`

**Steps:**
  1. View final results page
  2. Locate '로비로 돌아가기' button
  3. Click button
  4. Verify redirect to multiplayer lobby (/game/multi)
  5. Verify previous room is no longer active
  6. Verify user can create or join new room

**Expected Results:**
  - Return to lobby button is visible
  - User successfully returns to lobby
  - Room is closed/cleaned up after game completion
  - User can start new game session
  - Previous game state does not interfere

#### 8.4. Ranking with Single Player

**File:** `tests/game-end/single-player-ranking.spec.ts`

**Steps:**
  1. Play game solo (only host, no other participants)
  2. Complete all rounds without winning any (timeout each)
  3. View final results
  4. Verify single player appears in ranking
  5. Verify podium shows only 1st place (no 2nd or 3rd)
  6. Verify results page handles single player gracefully

**Expected Results:**
  - Single player game completes successfully
  - Results display appropriately for one player
  - No errors with missing 2nd/3rd place players
  - Score and correct count are accurate
  - User can return to lobby

### 9. Edge Cases and Error Handling

**Seed:** `seed.spec.ts`

#### 9.1. Quit During Game

**File:** `tests/edge-cases/quit-during-game.spec.ts`

**Steps:**
  1. Start game with 3 participants
  2. Progress to round 2 or 3 (mid-game)
  3. As one non-host participant, click '나가기' button
  4. Verify confirmation dialog or immediate quit
  5. Verify redirect to lobby
  6. In host browser, verify participant is removed from game
  7. Verify game continues for remaining participants
  8. Verify scoreboard updates to remove quit player

**Expected Results:**
  - Participant can quit during active game
  - Quitting participant is redirected to lobby
  - Remaining participants continue game normally
  - Participant list updates to reflect quit
  - Scores for remaining players are preserved
  - Game is not disrupted by mid-game quit

#### 9.2. Host Quit During Game

**File:** `tests/edge-cases/host-quit-during-game.spec.ts`

**Steps:**
  1. Start game as host with at least 2 other participants
  2. Progress to mid-game
  3. As host, click '나가기' button
  4. Verify behavior: either game ends for all OR host is transferred
  5. If game ends, verify all participants redirected with message
  6. If host transfer, verify new host gains control buttons

**Expected Results:**
  - Host quitting is handled gracefully
  - Either game ends or host role transfers to another participant
  - All participants are informed of host change/game end
  - No game state corruption occurs
  - Clear messaging about what happened

#### 9.3. Connection Loss Simulation

**File:** `tests/edge-cases/connection-loss-simulation.spec.ts`

**Steps:**
  1. Start game with multiple participants
  2. In one browser, disable network or close browser tab abruptly
  3. Wait 30-60 seconds
  4. In other browsers, observe participant list
  5. Verify disconnected participant is eventually removed or marked inactive
  6. Verify game can continue

**Expected Results:**
  - System detects disconnected participants (via polling timeout)
  - Disconnected participants are removed after reasonable timeout
  - Game continues for connected participants
  - Session cleanup handles abandoned connections

#### 9.4. Invalid Room Code Access

**File:** `tests/edge-cases/invalid-room-code.spec.ts`

**Steps:**
  1. Navigate directly to /game/multi/room/INVALID123
  2. Verify error handling
  3. Try joining with invalid code in lobby
  4. Verify appropriate error message
  5. Verify user remains on lobby or shown error page

**Expected Results:**
  - Invalid room codes are rejected
  - User receives clear error message
  - User is not stuck on error page
  - System suggests valid actions (return to lobby, create room)

#### 9.5. Concurrent Game Start Attempts

**File:** `tests/edge-cases/concurrent-game-start.spec.ts`

**Steps:**
  1. Open room in two browser tabs as same host (if possible) or simulate
  2. Attempt to start game from both tabs simultaneously
  3. Verify only one start request succeeds
  4. Verify game enters valid state (not double-started)
  5. Verify no duplicate rounds or score issues

**Expected Results:**
  - Concurrent start requests handled safely
  - Game state remains consistent
  - No race condition issues
  - Only one game session initiated

#### 9.6. Song Loading Failure

**File:** `tests/edge-cases/song-loading-failure.spec.ts`

**Steps:**
  1. Start game and enter PREPARING phase
  2. If possible, simulate song loading failure (network block, invalid song)
  3. Verify system handles loading error
  4. Verify error message or skip mechanism
  5. Verify game can recover and continue to next song or round

**Expected Results:**
  - Song loading errors are detected
  - User is informed of loading issue
  - Skip or retry mechanism is available
  - Game does not become unplayable
  - Error is logged for admin review

#### 9.7. Chat Profanity Filter

**File:** `tests/edge-cases/chat-profanity-filter.spec.ts`

**Steps:**
  1. Enter waiting room or game play chat
  2. Attempt to send message with profanity (test words from BadWord table)
  3. Verify message is either blocked, filtered, or flagged
  4. Send normal message to verify chat still works
  5. Verify other participants do not see profanity

**Expected Results:**
  - Profanity filter is active on chat messages
  - Bad words are blocked or replaced
  - User receives feedback about filtered content
  - Normal messages pass through unchanged
  - System uses BadWord table for filtering

#### 9.8. Song Report Functionality

**File:** `tests/edge-cases/song-report.spec.ts`

**Steps:**
  1. Complete a round to see result screen
  2. Click '문제 신고' button
  3. Verify report modal opens
  4. Select report reason (e.g., wrong answer, audio issue, inappropriate)
  5. Enter optional description
  6. Submit report
  7. Verify success confirmation
  8. Verify report is recorded (admin can check)

**Expected Results:**
  - Report modal opens correctly
  - Report reasons are clear and comprehensive
  - Report can be submitted with valid reason
  - Success feedback is provided
  - Report is saved to SongReport table
  - Admin can review reported songs

#### 9.9. Maximum Player Limit Enforcement

**File:** `tests/edge-cases/max-player-limit.spec.ts`

**Steps:**
  1. Create room with maxPlayers=2
  2. Have 2 users join
  3. Verify room shows 2/2 in lobby
  4. Attempt to join with 3rd user
  5. Verify join is prevented with error message
  6. Have one player leave
  7. Verify 3rd user can now join (1/2 available)

**Expected Results:**
  - Room capacity is enforced strictly
  - Cannot exceed maxPlayers limit
  - Error message clearly explains why join failed
  - Room availability updates when player leaves
  - Join becomes possible after slot opens

#### 9.10. Already in Active Room Prevention

**File:** `tests/edge-cases/already-in-active-room.spec.ts`

**Steps:**
  1. Join or create a room
  2. While in room, try to navigate to lobby and join different room
  3. Verify system detects existing participation
  4. Verify alert or warning displayed
  5. Verify option to return to current room or reset participation

**Expected Results:**
  - System detects if user already in active room
  - Warning is displayed with active room info
  - User can click to return to their active room
  - User can reset participation if needed
  - Prevents being in multiple rooms simultaneously

### 10. YouTube Integration Tests

**Seed:** `seed.spec.ts`

#### 10.1. YouTube Video Playback

**File:** `tests/youtube/youtube-playback.spec.ts`

**Steps:**
  1. Start game with song that has YouTube URL (not MP3 file)
  2. Verify YouTube player iframe loads in hidden container
  3. Verify video plays audio when PLAYING phase starts
  4. Verify progress bar reflects YouTube video progress
  5. Verify time counter matches video duration

**Expected Results:**
  - YouTube videos play successfully
  - YouTube player API loads correctly
  - Audio plays without showing video (hidden player)
  - Progress tracking works for YouTube videos
  - Playback controls function properly

#### 10.2. MP3 Fallback Playback

**File:** `tests/youtube/mp3-fallback.spec.ts`

**Steps:**
  1. Start game with song that has MP3 file (local upload)
  2. Verify audio element is used for playback
  3. Verify MP3 plays when PLAYING phase starts
  4. Verify progress bar and time counter work correctly

**Expected Results:**
  - MP3 files play via audio element
  - Local file playback is smooth
  - No YouTube player loaded for MP3 songs
  - Progress and timing accurate for audio element

#### 10.3. Mixed Media in Same Game

**File:** `tests/youtube/mixed-media-game.spec.ts`

**Steps:**
  1. Play game where some rounds use YouTube, others use MP3
  2. Round 1: YouTube video
  3. Round 2: MP3 file
  4. Round 3: YouTube video again
  5. Verify each media type switches correctly
  6. Verify no playback errors during switches

**Expected Results:**
  - System seamlessly switches between YouTube and MP3
  - Both media types work in same game session
  - Player component adapts to media source automatically
  - No memory leaks or performance issues from switching
