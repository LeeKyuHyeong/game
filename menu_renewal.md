# 관리자 메뉴 통합 계획 (12개 → 6개)

## 목표
기존 12개 관리자 메뉴를 6개로 통합하여 탭 기반 UI로 재구성

## 최종 구조

| 메뉴 | URL | 탭 구성 |
|------|-----|--------|
| 📁 콘텐츠 관리 | `/admin/content` | 노래, 정답, 장르, 대중성 |
| 🎮 게임 관리 | `/admin/game` | 게임이력, 멀티게임, 챌린지 |
| 👥 회원 관리 | `/admin/member` | 회원, 로그인이력, 비속어 |
| 📈 통계 분석 | `/admin/stats` | 오답통계 (기존 유지) |
| ⚙️ 시스템 설정 | `/admin/system` | 배치, 메뉴 |
| 🏆 랭킹 관리 | `/admin/ranking` | 솔로, 멀티, 레트로, 챌린지 |

---

## 작업 단계

### Phase 1: 통합 컨트롤러 생성 (4개)

**1.1 AdminContentController.java** - `/admin/content`
```
- 탭: song, answer, genre, popularity
- 의존성: SongService, GenreService, SongPopularityVoteService
- 통계: 총 노래 수, 장르 수, 투표 수
```

**1.2 AdminGameController.java** - `/admin/game`
```
- 탭: history, multi, challenge
- 의존성: GameSessionService, GameRoomRepository, FanChallengeRecordRepository
- 통계: 오늘 게임, 활성 방, 챌린지 기록
```

**1.3 AdminSystemController.java** - `/admin/system`
```
- 탭: batch, menu
- 의존성: BatchService, MenuConfigService
- 통계: 배치 수, 활성 메뉴
```

**1.4 AdminRankingController.java** - `/admin/ranking`
```
- 탭: solo, multi, retro, challenge
- 의존성: MemberService, FanChallengeRecordRepository
- 기존 랭킹 로직 재사용
```

### Phase 2: 기존 컨트롤러에 /content 엔드포인트 추가 (5개)

| 컨트롤러 | 추가 엔드포인트 | Fragment 경로 |
|---------|---------------|--------------|
| AdminAnswerController | GET `/content` | `admin/answer/fragments/answer.html` |
| AdminGenreController | GET `/content` | `admin/genre/fragments/genre.html` |
| AdminBadWordController | GET `/content` | `admin/badword/fragments/badword.html` |
| AdminBatchController | GET `/content` | `admin/batch/fragments/batch.html` |
| AdminMenuController | GET `/content` | `admin/menu/fragments/menu.html` |

### Phase 3: 기존 컨트롤러에 리다이렉트 추가

| 기존 URL | 리다이렉트 대상 |
|---------|---------------|
| `/admin/song` | `/admin/content?tab=song` |
| `/admin/answer` | `/admin/content?tab=answer` |
| `/admin/genre` | `/admin/content?tab=genre` |
| `/admin/song-popularity` | `/admin/content?tab=popularity` |
| `/admin/history` | `/admin/game?tab=history` |
| `/admin/multi` | `/admin/game?tab=multi` |
| `/admin/challenge` | `/admin/game?tab=challenge` |
| `/admin/badword` | `/admin/member?tab=badword` |
| `/admin/batch` | `/admin/system?tab=batch` |
| `/admin/menu` | `/admin/system?tab=menu` |

### Phase 4: HTML 템플릿 생성

**4.1 통합 index.html (4개 신규)**
- `templates/admin/content/index.html`
- `templates/admin/game/index.html`
- `templates/admin/system/index.html`
- `templates/admin/ranking/index.html`

**4.2 Fragment 템플릿 (5개 신규)**
- `templates/admin/answer/fragments/answer.html`
- `templates/admin/genre/fragments/genre.html`
- `templates/admin/badword/fragments/badword.html`
- `templates/admin/batch/fragments/batch.html`
- `templates/admin/menu/fragments/menu.html`

**4.3 기존 템플릿 수정**
- `templates/admin/member/index.html` - 비속어 탭 추가
- `templates/admin/layout/sidebar.html` - 메뉴 6개로 변경

### Phase 5: JavaScript 파일 생성 (4개)
- `static/js/admin/admin-content-index.js`
- `static/js/admin/admin-game-index.js`
- `static/js/admin/admin-system-index.js`
- `static/js/admin/admin-ranking-index.js`

### Phase 6: CSS 파일 (기존 재사용)
- 기존 `member-index.css`, `history-index.css` 패턴 재사용
- 필요시 `content-index.css` 등 추가

### Phase 7: 사이드바 메뉴 업데이트
`sidebar.html`에서 12개 메뉴 → 6개 메뉴로 변경

---

## 파일 목록

### 신규 생성 (17개)
```
Controller (4개):
  src/main/java/com/kh/game/controller/admin/AdminContentController.java
  src/main/java/com/kh/game/controller/admin/AdminGameController.java
  src/main/java/com/kh/game/controller/admin/AdminSystemController.java
  src/main/java/com/kh/game/controller/admin/AdminRankingController.java

Template - Index (4개):
  src/main/resources/templates/admin/content/index.html
  src/main/resources/templates/admin/game/index.html
  src/main/resources/templates/admin/system/index.html
  src/main/resources/templates/admin/ranking/index.html

Template - Fragment (5개):
  src/main/resources/templates/admin/answer/fragments/answer.html
  src/main/resources/templates/admin/genre/fragments/genre.html
  src/main/resources/templates/admin/badword/fragments/badword.html
  src/main/resources/templates/admin/batch/fragments/batch.html
  src/main/resources/templates/admin/menu/fragments/menu.html

JavaScript (4개):
  src/main/resources/static/js/admin/admin-content-index.js
  src/main/resources/static/js/admin/admin-game-index.js
  src/main/resources/static/js/admin/admin-system-index.js
  src/main/resources/static/js/admin/admin-ranking-index.js
```

### 수정 (12개)
```
Controller (10개):
  AdminSongController.java - 리다이렉트 추가
  AdminAnswerController.java - 리다이렉트 + /content
  AdminGenreController.java - 리다이렉트 + /content
  AdminSongPopularityController.java - 리다이렉트
  AdminGameHistoryController.java - 리다이렉트
  AdminMultiController.java - 리다이렉트
  AdminChallengeController.java - 리다이렉트
  AdminBadWordController.java - /content
  AdminBatchController.java - /content
  AdminMenuController.java - /content

Template (1개):
  sidebar.html - 메뉴 6개로 변경

JavaScript (1개):
  admin-member-index.js - 비속어 탭 로직 추가 (선택)
```

---

## 참조 파일 (기존 패턴)

| 용도 | 파일 |
|-----|------|
| 탭 UI 패턴 | `admin-member-index.js` |
| 통합 index 구조 | `admin/member/index.html` |
| Fragment 구조 | `admin/member/fragments/member.html` |
| CSS 스타일 | `member-index.css` |
| 컨트롤러 패턴 | `AdminMemberController.java` |

---

## 검증 방법

1. **빌드 확인**
   ```bash
   ./mvnw clean compile
   ```

2. **애플리케이션 실행**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **URL 테스트**
   - `/admin/content` - 콘텐츠 관리 페이지 로드
   - `/admin/game` - 게임 관리 페이지 로드
   - `/admin/system` - 시스템 설정 페이지 로드
   - `/admin/ranking` - 랭킹 관리 페이지 로드

4. **리다이렉트 테스트**
   - `/admin/song` → `/admin/content?tab=song`
   - `/admin/batch` → `/admin/system?tab=batch`

5. **탭 전환 테스트**
   - 각 탭 클릭 시 콘텐츠 AJAX 로드 확인
   - URL 파라미터 업데이트 확인 (?tab=xxx)
   - 브라우저 뒤로가기 동작 확인

6. **반응형 테스트**
   - PC (1200px+)
   - 태블릿 (768px)
   - 모바일 (480px)

---

## 주의사항

### CSS 규칙 (CLAUDE.md)
- 색상: CSS 변수 사용 (`var(--text-primary)` 등)
- 반응형: 3단계 필수 (PC/태블릿/모바일)
- 다크 모드: `[data-theme="dark"]` 정의 필수

### 하위 호환성
- 기존 URL은 리다이렉트로 유지 (북마크 동작)
- 기존 API 엔드포인트 변경 없음

### 보안
- `/admin/**` 경로는 AdminInterceptor로 자동 보호
- 새 컨트롤러도 동일하게 보호됨
