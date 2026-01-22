# TODO

## 2026-01-22 변경 내역

### 관리자 메뉴 통합
- [O] 챌린지 관리 통합 (팬 챌린지 + 장르 챌린지 → 탭 기반 UI)
- [O] 멀티게임 관리 통합 (방 관리 + 채팅 이력 → 탭 기반 UI)
- [O] 회원 관리 통합 (회원 목록 + 로그인 이력 → 탭 기반 UI)
- [O] 게임 관리 통합 (게임 이력 + 랭킹 → 탭 기반 UI)
- [O] 노래 관리 통합 (노래 목록 + 곡 신고 → 탭 기반 UI)
- [O] 사이드바 메뉴 16개 → 11개로 축소 (31% 감소)

### 버그 수정
- [O] 장르챌린지 상세 모달이 열리지 않는 버그 수정 (CSS 클래스 'active' → 'show' 변경)

---

## 2026-01-20 변경 내역

### 기능 구현
- [O] 랭킹 페이지 6개 탭 추가 (멀티 티어 / 주간 멀티 / 30곡 챌린지 / 레트로 / 팬 챌린지 / 통계)
- [O] 레트로 랭킹 API 추가 (/api/ranking?mode=retro) - 누적 점수, 30곡 최고점, 주간
- [O] 팬 챌린지 글로벌 랭킹 API 추가 (/api/ranking/fan-challenge) - 퍼펙트 클리어 횟수, 도전 아티스트 수
- [O] FanChallengeRecordRepository 글로벌 랭킹 쿼리 추가

### UI/UX 개선
- [O] 랭킹 페이지 6탭 반응형 레이아웃 (PC: 6열, 태블릿: 3열, 모바일: 2열)
- [O] 각 탭별 서브탭 및 안내 문구 추가

---

## 2026-01-19 변경 내역

### 기능 구현
- [O] 아티스트 챌린지 정보 API 추가 (설정 화면용 - 1위 기록, 내 기록)
- [O] Fan Challenge 기록 저장 로직 개선 (모든 점수에 시간 기록, 동점 시간 갱신)
- [O] 세션 조회 Lazy Loading 문제 해결 (rounds/song JOIN FETCH)
- [O] 관리자용 FanChallengeRecord 쿼리 메서드 추가
- [O] WeeklyPerfectRefreshBatch 배치 등록 (주간 퍼펙트 상태 갱신)
- [O] 팬 챌린지 관리자 페이지 추가 (AdminFanChallengeController)

### UI/UX 개선
- [O] 홈 화면 모바일 햄버거 메뉴 추가 (슬라이드 네비게이션)
- [O] 그리드 체커보드 패턴 자동 적용 (열 수 자동 감지)
- [O] 게임 화면 모바일 플로팅 버튼 추가 (홈/포기)
- [O] 모바일 아티스트 유형 선택 버튼화 (select → buttons)
- [O] 정답 모달 스킵 시 메시지 표시
- [O] Admin/Client CSS 테마 변수화 (다크모드 대응)
- [O] 모바일 반응형 레이아웃 개선 (setup, result, play 화면)
- [O] rem 단위 및 CSS 변수 표준화
- [O] 라이트모드 전체랭킹 포디움 내부 별명 및 점수 color
- [] 통계 가시성 Up
- [] 커뮤니티 모바일 버전 탭 디자인 수정 
- [O] 아티스트 챌린지 정답 가시성
- 
### 테스트
- [O] FanChallengeServiceTest 추가
- [O] AdminFanChallengeControllerTest 추가
- [O] WeeklyPerfectRefreshBatchTest 추가

---

## 2026-01-15 (20시 이후) 변경 내역

### 기능 구현
- [O] 아티스트 챌린지 개편: 30곡 고정 포맷, 난이도 단순화
- [O] 호스트 모드 2라운드부터 자동 재생 기능
- [O] 내가맞추기 모드 다음 라운드 자동 재생 기능
- [O] 멀티플레이어 대기실 강퇴 감지 기능
- [O] 멀티플레이어 게임 헤더에 내 점수 표시
- [ ] 2000년대 이전 노래 목록으로 레트로 게임 유형 만들기 

### UI/UX 개선
- [O] 통계 페이지 벤토 그리드 레이아웃 적용
- [O] 아티스트 챌린지 랭킹 PC 드래그 스크롤 지원
- [O] 게임 UI 통일화 (music-controls-bar 컴포넌트)
- [O] 관리자 페이지 헤더 이모지 제거
- [O] 게임 이력 상세 페이지 UI 개선 (목록 버튼화, 힌트 컬럼 제거)
- [O] 라이트/다크 테마 토글 기능

### 테스트
- [O] 30곡 고정 아티스트 챌린지 테스트 업데이트

---

## 기능 구현

### 자동 재생 기능
- [O] '게임 진행' 다음 라운드 노래 자동 재생 기능 구현
- [O] '내가맞추기' 다음 라운드 노래 자동 재생 기능 구현
- [O] '멀티게임' 다음 라운드 노래 자동 재생 기능 구현

### 콘텐츠 관리
- [ ] 아티스트 챌린지 기능 변경

---

## UI/UX 개선 작업

### 통계 페이지
- [O] 로그인 후 통계 - 내 기록 design 확인

### 아티스트 챌린지
- [O] 랭킹 목록 PC화면 드래그 안됨 수정

### 랭킹 페이지
- [O] 전체 랭킹 상단 탭 design 다시 확인

### 버튼 통일
- [O] 멀티게임 방 만들 때 '로비로' 버튼 -> '홈'과 동일하게 변경
- [O] 커뮤니티 디테일에서 '목록으로' 버튼 -> '홈'과 동일하게 변경


[O] 곡신고에서  상태변경 버튼이 안먹힘
[O] 랭킹화면 리뉴얼(client) - 6탭 구성 완료
[O] 아티스트 챌린지 결과화면에서 진행한 곡의 '대중성' / '매니악' 투표(번호 선택 1~5) 가능하게 팝업 + 투표 버튼 추가 (로그인 ID로 체크할지 그냥 매번 cnt올릴지)
[O] 관리자의 노래관리 목록에는 해당 아티스트옆의 숫자는 전체곡을 나타내야될거같아(이게 client쪽 게임 진행할때 뜨는 아티스트 목록의 노래개수에는 영향주면안됨)
[O] 장르 챌린지 추가 구현

[]아티스트 챌린지 결과에서 투표한 대중성 정도 볼 수 있는 관리자화면 필요

[O] 관리자화면 메뉴 통합 가능한 곳 통합하기
[O] css 다크/라이트 가시성, 밝기 전체확인
[O] 노래 관리 화면 검색 파라미터 유지하기
[] 유튜브 및 노래 중복 배치 후 변경된 목록 볼 수 있는 관리자화면 필요
[O] 관리자 랭킹 '팬챌린지' 오류 - tymeleaf 문법 오류일듯
[O] 사용자 관리 접근안됐음
[O] client 쪽 아티스트 챌린지 랭킹 보여줄때, score 높은 순서로 보여줘야겠음
[] 사용자 관리 에서 게임 수 는 뭐로 체크?

0. 몸으로 말해요 (전체) - 친목도모
1. <주크박스 게임>   (3vs3) - 총 3라운드 진행 2라운드를 먼저 모든 인원이 통과하면 승!
2. <인물퀴즈 게임>   (2vs2) - 총 3라운드 진행 2라운드를 먼저 모든 인원이 통과하면 승!
3. <인간 스탑워치 게임> (1vs1) - 상대방이 말하는 1분 내의 초를 스탑워치 멈췄을때 오차가 작은 팀이 승!
   해서 2게임을 승리한 팀이 대박 상품

마피아 3
경찰 1
의사 1
스파이 1
시민 6

-NCT 127-
favorite
simon says
cherry bomb
lemonade
touch
highway to heaven
ay-yo
back 2 u
우산 love song
윤슬 gold dust
소방차
무량적아
superhuman
punch
love on the floor
sit down
road trip
whiplash
angel eyes
summer 127

-NCT DREAM-
Smoothie
Beatbox
yougurt shake
boom
hello future
오르골 life is still going on
ISTJ
고래 dive into you
북극성
Moonlight
버퍼링
GO
We Go Up
Ridin`
like we just met
broken melodies
poison 모래성
주인공
graduation
Drippin`

-태연-
날개
I Got Love
Sweet Love
When I Was Young
수채화
Fire
Eraser
기억을 걷는 시간
Lonely Night
Find Me
Love You Like Crazy
Wine
Blue
내게 들려주고 싶은 말(Dear Me)
월식
Here I Am
그런 밤(Some Night)
어른아이(Toddler)
Siren
Cold As Hell
쌍둥이자리(Gemini)


-블랙핑크-
Typa Girl
The girls
Ready For Love
Tally
Hard to Love
Crazy Over You
Kick It
You Never Know
See U Later
Sour Candy
Yeah Yeah Yeah
Really
아니길(Hope Not)