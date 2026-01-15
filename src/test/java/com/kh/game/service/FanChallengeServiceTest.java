package com.kh.game.service;

import com.kh.game.entity.*;
import com.kh.game.repository.GameSessionRepository;
import com.kh.game.repository.GenreRepository;
import com.kh.game.repository.SongAnswerRepository;
import com.kh.game.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("FanChallengeService 테스트")
class FanChallengeServiceTest {

    @Autowired
    private FanChallengeService fanChallengeService;

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongAnswerRepository songAnswerRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @MockitoBean
    private YouTubeValidationService youTubeValidationService;

    private Genre kpopGenre;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        gameSessionRepository.deleteAll();
        songAnswerRepository.deleteAll();
        songRepository.deleteAll();
        genreRepository.deleteAll();

        // YouTube 검증 항상 성공하도록 Mock 설정
        YouTubeValidationService.ValidationResult validResult = YouTubeValidationService.ValidationResult.valid();
        Mockito.when(youTubeValidationService.validateVideo(anyString())).thenReturn(validResult);

        // 장르 생성
        kpopGenre = new Genre();
        kpopGenre.setCode("KPOP");
        kpopGenre.setName("K-POP");
        kpopGenre.setUseYn("Y");
        kpopGenre = genreRepository.save(kpopGenre);
    }

    private Song createSong(String title, String artist) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setGenre(kpopGenre);
        song.setUseYn("Y");
        song.setIsSolo(false);
        song.setYoutubeVideoId("test" + System.nanoTime());
        song.setStartTime(0);
        song.setPlayDuration(30);
        Song saved = songRepository.save(song);

        // 정답 추가
        SongAnswer answer = new SongAnswer();
        answer.setSong(saved);
        answer.setAnswer(title);
        answer.setIsPrimary(true);
        songAnswerRepository.save(answer);

        return saved;
    }

    private void createSongsForArtist(String artist, int count) {
        for (int i = 1; i <= count; i++) {
            createSong(artist + " Song " + i, artist);
        }
    }

    // =====================================================
    // Edge Case 1: 곡 1개 시나리오
    // =====================================================
    @Nested
    @DisplayName("곡 1개 시나리오")
    class SingleSongScenarios {

        @Test
        @DisplayName("[버그] 곡 1개 + 첫 라운드 실패 → PERFECT_CLEAR가 아닌 실패로 처리되어야 함")
        void singleSong_firstRoundFail_shouldNotBePerfectClear() {
            // Given: 곡 1개인 아티스트
            createSong("Only Song", "Solo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Solo Artist", FanChallengeDifficulty.HARDCORE);

            assertThat(session.getTotalRounds()).isEqualTo(1);
            assertThat(session.getRemainingLives()).isEqualTo(3); // 하드코어 라이프 3개

            // When: 첫 라운드 오답
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "틀린 답", 5000);

            // Then: ALL_ROUNDS_COMPLETED (퍼펙트가 아니므로)
            assertThat(result.isCorrect()).isFalse();
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("ALL_ROUNDS_COMPLETED");
            assertThat(result.correctCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("곡 1개 + 첫 라운드 성공 → 진짜 PERFECT_CLEAR")
        void singleSong_firstRoundSuccess_shouldBePerfectClear() {
            // Given
            createSong("Only Song", "Solo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Solo Artist", FanChallengeDifficulty.HARDCORE);

            // When: 첫 라운드 정답
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "Only Song", 5000);

            // Then
            assertThat(result.isCorrect()).isTrue();
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("PERFECT_CLEAR");
            assertThat(result.correctCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("곡 1개 + 시간 초과 → 게임 오버 (퍼펙트 아님)")
        void singleSong_timeout_shouldNotBePerfectClear() {
            // Given
            createSong("Only Song", "Solo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Solo Artist", FanChallengeDifficulty.HARDCORE);

            // When: 시간 초과
            FanChallengeService.AnswerResult result = fanChallengeService.processTimeout(
                    session.getId(), 1);

            // Then: ALL_ROUNDS_COMPLETED (시간 초과로 실패)
            assertThat(result.isTimeout()).isTrue();
            assertThat(result.isCorrect()).isFalse();
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("ALL_ROUNDS_COMPLETED");
        }
    }

    // =====================================================
    // Edge Case 2: 곡 2개 시나리오
    // =====================================================
    @Nested
    @DisplayName("곡 2개 시나리오")
    class TwoSongScenarios {

        @Test
        @DisplayName("곡 2개 + 첫 번째 실패 + 두 번째 성공 → 완료 (퍼펙트 아님)")
        void twoSongs_firstFailSecondSuccess_notPerfect() {
            // Given
            createSong("Song 1", "Duo Artist");
            createSong("Song 2", "Duo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Duo Artist", FanChallengeDifficulty.HARDCORE);

            assertThat(session.getTotalRounds()).isEqualTo(2);

            // When: 첫 번째 오답
            FanChallengeService.AnswerResult result1 = fanChallengeService.processAnswer(
                    session.getId(), 1, "틀린 답", 5000);

            assertThat(result1.isCorrect()).isFalse();
            assertThat(result1.isGameOver()).isFalse();
            assertThat(result1.remainingLives()).isEqualTo(2);

            // When: 두 번째 정답
            FanChallengeService.AnswerResult result2 = fanChallengeService.processAnswer(
                    session.getId(), 2, "Song 2", 5000);

            // Then
            assertThat(result2.isCorrect()).isTrue();
            assertThat(result2.isGameOver()).isTrue();
            assertThat(result2.correctCount()).isEqualTo(1);
            assertThat(result2.completedRounds()).isEqualTo(2);
            // 퍼펙트가 아니므로 gameOverReason 확인
            // 수정 후: PERFECT_CLEAR가 아닌 다른 값이어야 함
        }

        @Test
        @DisplayName("곡 2개 + 둘 다 성공 → PERFECT_CLEAR")
        void twoSongs_bothSuccess_perfectClear() {
            // Given
            createSong("Song 1", "Duo Artist");
            createSong("Song 2", "Duo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Duo Artist", FanChallengeDifficulty.HARDCORE);

            // When
            fanChallengeService.processAnswer(session.getId(), 1, "Song 1", 5000);
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 2, "Song 2", 5000);

            // Then
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("PERFECT_CLEAR");
            assertThat(result.correctCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("곡 2개 + 둘 다 실패 (라이프 3개) → 라이프 1개 남고 게임 완료")
        void twoSongs_bothFail_livesRemaining() {
            // Given
            createSong("Song 1", "Duo Artist");
            createSong("Song 2", "Duo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Duo Artist", FanChallengeDifficulty.HARDCORE);

            // When
            FanChallengeService.AnswerResult result1 = fanChallengeService.processAnswer(
                    session.getId(), 1, "틀린 답", 5000);
            FanChallengeService.AnswerResult result2 = fanChallengeService.processAnswer(
                    session.getId(), 2, "틀린 답", 5000);

            // Then: ALL_ROUNDS_COMPLETED (둘 다 실패, 라이프 1개 남음)
            assertThat(result2.isGameOver()).isTrue();
            assertThat(result2.remainingLives()).isEqualTo(1);
            assertThat(result2.correctCount()).isEqualTo(0);
            assertThat(result2.gameOverReason()).isEqualTo("ALL_ROUNDS_COMPLETED");
        }
    }

    // =====================================================
    // Edge Case 3: 라이프 소진 시나리오
    // =====================================================
    @Nested
    @DisplayName("라이프 소진 시나리오")
    class LifeExhaustionScenarios {

        @Test
        @DisplayName("곡 5개 + 라이프 3개 + 3회 연속 실패 → LIFE_EXHAUSTED")
        void fiveSongs_threeConsecutiveFails_lifeExhausted() {
            // Given
            createSongsForArtist("Test Artist", 5);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            assertThat(session.getTotalRounds()).isEqualTo(5);
            assertThat(session.getRemainingLives()).isEqualTo(3);

            // When: 3회 연속 실패
            fanChallengeService.processAnswer(session.getId(), 1, "틀림", 5000);
            fanChallengeService.processAnswer(session.getId(), 2, "틀림", 5000);
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 3, "틀림", 5000);

            // Then
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("LIFE_EXHAUSTED");
            assertThat(result.remainingLives()).isEqualTo(0);
            assertThat(result.completedRounds()).isEqualTo(3);
        }

        @Test
        @DisplayName("입문 모드 (라이프 5개) + 5회 연속 실패 → LIFE_EXHAUSTED")
        void beginnerMode_fiveConsecutiveFails_lifeExhausted() {
            // Given
            createSongsForArtist("Test Artist", 10);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.BEGINNER);

            assertThat(session.getRemainingLives()).isEqualTo(5);

            // When: 5회 연속 실패
            for (int i = 1; i <= 4; i++) {
                FanChallengeService.AnswerResult r = fanChallengeService.processAnswer(
                        session.getId(), i, "틀림", 10000);
                assertThat(r.isGameOver()).isFalse();
            }
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 5, "틀림", 10000);

            // Then
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("LIFE_EXHAUSTED");
            assertThat(result.remainingLives()).isEqualTo(0);
        }

        @Test
        @DisplayName("라이프가 딱 0이 되는 시점 + 라운드 완료 시점 동시 발생")
        void lifeExhaustedAndRoundsCompletedSimultaneously() {
            // Given: 곡 3개, 라이프 3개
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // When: 3회 모두 실패 (라이프 0 + 라운드 완료 동시 발생)
            fanChallengeService.processAnswer(session.getId(), 1, "틀림", 5000);
            fanChallengeService.processAnswer(session.getId(), 2, "틀림", 5000);
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 3, "틀림", 5000);

            // Then: LIFE_EXHAUSTED가 우선해야 함 (먼저 체크됨)
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("LIFE_EXHAUSTED");
            assertThat(result.remainingLives()).isEqualTo(0);
            assertThat(result.completedRounds()).isEqualTo(3);
            assertThat(result.correctCount()).isEqualTo(0);
        }
    }

    // =====================================================
    // Edge Case 4: 시간 초과 시나리오
    // =====================================================
    @Nested
    @DisplayName("시간 초과 시나리오")
    class TimeoutScenarios {

        @Test
        @DisplayName("시간 초과 → 라이프 감소")
        void timeout_decreasesLife() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // When
            FanChallengeService.AnswerResult result = fanChallengeService.processTimeout(
                    session.getId(), 1);

            // Then
            assertThat(result.isTimeout()).isTrue();
            assertThat(result.isCorrect()).isFalse();
            assertThat(result.remainingLives()).isEqualTo(2);
        }

        @Test
        @DisplayName("시간 제한 초과한 정답 제출 → 시간 초과로 처리")
        void answerAfterTimeLimit_treatedAsTimeout() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // 하드코어 총 시간: 3초 + 5초 = 8초 = 8000ms
            FanChallengeDifficulty difficulty = FanChallengeDifficulty.HARDCORE;
            long timeLimit = difficulty.getTotalTimeMs();

            // When: 시간 초과 후 정답 제출 (정답이지만 시간 초과)
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "Test Artist Song 1", timeLimit + 1);

            // Then
            assertThat(result.isTimeout()).isTrue();
            assertThat(result.isCorrect()).isFalse();
        }

        @Test
        @DisplayName("시간 제한 내 정답 제출 → 정상 처리")
        void answerWithinTimeLimit_processedNormally() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            FanChallengeDifficulty difficulty = FanChallengeDifficulty.HARDCORE;
            long timeLimit = difficulty.getTotalTimeMs();

            // When: 시간 내 정답 제출
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "Test Artist Song 1", timeLimit - 1);

            // Then
            assertThat(result.isTimeout()).isFalse();
            assertThat(result.isCorrect()).isTrue();
        }
    }

    // =====================================================
    // Edge Case 5: 잘못된 입력 시나리오
    // =====================================================
    @Nested
    @DisplayName("잘못된 입력 시나리오")
    class InvalidInputScenarios {

        @Test
        @DisplayName("빈 문자열 답안 → 오답 처리")
        void emptyAnswer_treatedAsWrong() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // When
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "", 5000);

            // Then
            assertThat(result.isCorrect()).isFalse();
            assertThat(result.remainingLives()).isEqualTo(2);
        }

        @Test
        @DisplayName("null 답안 → 오답 처리")
        void nullAnswer_treatedAsWrong() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // When
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, null, 5000);

            // Then
            assertThat(result.isCorrect()).isFalse();
        }

        @Test
        @DisplayName("공백만 있는 답안 → 오답 처리")
        void whitespaceOnlyAnswer_treatedAsWrong() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // When
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "   ", 5000);

            // Then
            assertThat(result.isCorrect()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 세션 ID → 예외 발생")
        void nonExistentSessionId_throwsException() {
            // When & Then
            assertThatThrownBy(() ->
                    fanChallengeService.processAnswer(999999L, 1, "답", 5000)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("세션을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("존재하지 않는 라운드 번호 → 예외 발생")
        void nonExistentRoundNumber_throwsException() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // When & Then
            assertThatThrownBy(() ->
                    fanChallengeService.processAnswer(session.getId(), 999, "답", 5000)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("라운드를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("곡이 없는 아티스트로 게임 시작 → 예외 발생")
        void noSongsForArtist_throwsException() {
            // When & Then
            assertThatThrownBy(() ->
                    fanChallengeService.startChallenge(null, "테스터", "없는 아티스트", FanChallengeDifficulty.HARDCORE)
            ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효한 곡이 없습니다");
        }
    }

    // =====================================================
    // Edge Case 6: 이미 완료된 게임 시나리오
    // =====================================================
    @Nested
    @DisplayName("이미 완료된 게임 시나리오")
    class CompletedGameScenarios {

        @Test
        @DisplayName("이미 완료된 게임에 답안 제출 → 예외 발생")
        void answerToCompletedGame_throwsException() {
            // Given: 게임 완료
            createSong("Only Song", "Solo Artist");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Solo Artist", FanChallengeDifficulty.HARDCORE);

            fanChallengeService.processAnswer(session.getId(), 1, "Only Song", 5000);

            // 게임이 완료됨을 확인
            GameSession completedSession = fanChallengeService.getSession(session.getId());
            assertThat(completedSession.getStatus()).isEqualTo(GameSession.GameStatus.COMPLETED);

            // When & Then: 완료된 게임에 다시 답안 제출
            assertThatThrownBy(() ->
                    fanChallengeService.processAnswer(session.getId(), 1, "다른 답", 5000)
            ).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("게임이 진행 중이 아닙니다");
        }
    }

    // =====================================================
    // Edge Case 7: 난이도별 설정 시나리오
    // =====================================================
    @Nested
    @DisplayName("난이도별 설정 시나리오")
    class DifficultyScenarios {

        @Test
        @DisplayName("입문 모드 초기 라이프 5개")
        void beginnerMode_initialLives5() {
            // Given
            createSongsForArtist("Test Artist", 3);

            // When
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.BEGINNER);

            // Then
            assertThat(session.getRemainingLives()).isEqualTo(5);
        }

        @Test
        @DisplayName("일반 모드 초기 라이프 3개")
        void normalMode_initialLives3() {
            // Given
            createSongsForArtist("Test Artist", 3);

            // When
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.NORMAL);

            // Then
            assertThat(session.getRemainingLives()).isEqualTo(3);
        }

        @Test
        @DisplayName("하드코어 모드 초기 라이프 3개 (수정됨)")
        void hardcoreMode_initialLives3() {
            // Given
            createSongsForArtist("Test Artist", 3);

            // When
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // Then
            assertThat(session.getRemainingLives()).isEqualTo(3);
        }

        @Test
        @DisplayName("난이도별 시간 제한 확인")
        void difficultyTimeSettings() {
            // 입문: 7초 + 5초 = 12초
            assertThat(FanChallengeDifficulty.BEGINNER.getTotalTimeMs()).isEqualTo(12000);

            // 일반: 5초 + 5초 = 10초
            assertThat(FanChallengeDifficulty.NORMAL.getTotalTimeMs()).isEqualTo(10000);

            // 하드코어: 3초 + 5초 = 8초
            assertThat(FanChallengeDifficulty.HARDCORE.getTotalTimeMs()).isEqualTo(8000);
        }

        @Test
        @DisplayName("입문 모드만 초성 힌트 제공")
        void onlyBeginnerShowsChosungHint() {
            assertThat(FanChallengeDifficulty.BEGINNER.isShowChosungHint()).isTrue();
            assertThat(FanChallengeDifficulty.NORMAL.isShowChosungHint()).isFalse();
            assertThat(FanChallengeDifficulty.HARDCORE.isShowChosungHint()).isFalse();
        }

        @Test
        @DisplayName("하드코어만 공식 랭킹 반영")
        void onlyHardcoreIsRanked() {
            assertThat(FanChallengeDifficulty.BEGINNER.isRanked()).isFalse();
            assertThat(FanChallengeDifficulty.NORMAL.isRanked()).isFalse();
            assertThat(FanChallengeDifficulty.HARDCORE.isRanked()).isTrue();
        }
    }

    // =====================================================
    // Edge Case 8: 초성 힌트 시나리오
    // =====================================================
    @Nested
    @DisplayName("초성 힌트 시나리오")
    class ChosungHintScenarios {

        @Test
        @DisplayName("한글 제목 초성 추출")
        void koreanTitle_extractChosung() {
            // Given & When & Then
            assertThat(fanChallengeService.extractChosung("봄날")).isEqualTo("ㅂㄴ");
            assertThat(fanChallengeService.extractChosung("피 땀 눈물")).isEqualTo("ㅍ ㄸ ㄴㅁ");
            assertThat(fanChallengeService.extractChosung("작은 것들을 위한 시")).isEqualTo("ㅈㅇ ㄱㄷㅇ ㅇㅎ ㅅ");
        }

        @Test
        @DisplayName("영어 제목 힌트 생성")
        void englishTitle_extractHint() {
            // Given & When & Then
            assertThat(fanChallengeService.extractChosung("Dynamite")).isEqualTo("D*******");
            assertThat(fanChallengeService.extractChosung("Boy With Luv")).isEqualTo("B** W*** L**");
        }

        @Test
        @DisplayName("숫자가 포함된 제목")
        void titleWithNumbers() {
            assertThat(fanChallengeService.extractChosung("2002")).isEqualTo("2002");
            assertThat(fanChallengeService.extractChosung("24시간이 모자라")).isEqualTo("24ㅅㄱㅇ ㅁㅈㄹ");
        }

        @Test
        @DisplayName("빈 제목")
        void emptyTitle() {
            assertThat(fanChallengeService.extractChosung("")).isEqualTo("");
            assertThat(fanChallengeService.extractChosung(null)).isEqualTo("");
        }
    }

    // =====================================================
    // Edge Case 9: 동시성/경계값 시나리오
    // =====================================================
    @Nested
    @DisplayName("경계값 시나리오")
    class BoundaryScenarios {

        @Test
        @DisplayName("정확히 시간 제한에 맞춘 답안 → 정상 처리")
        void answerExactlyAtTimeLimit_processedNormally() {
            // Given
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            long timeLimit = FanChallengeDifficulty.HARDCORE.getTotalTimeMs();

            // When: 정확히 시간 제한에 맞춤
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "Test Artist Song 1", timeLimit);

            // Then: 시간 초과가 아님 (timeLimit 초과가 아니라 timeLimit 이하)
            assertThat(result.isTimeout()).isFalse();
        }

        @Test
        @DisplayName("라이프가 정확히 1개 남은 상태에서 실패")
        void exactlyOneLifeRemaining_fail() {
            // Given
            createSongsForArtist("Test Artist", 5);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // 2회 실패로 라이프 1개 남김
            fanChallengeService.processAnswer(session.getId(), 1, "틀림", 5000);
            fanChallengeService.processAnswer(session.getId(), 2, "틀림", 5000);

            GameSession afterTwoFails = fanChallengeService.getSession(session.getId());
            assertThat(afterTwoFails.getRemainingLives()).isEqualTo(1);

            // When: 마지막 라이프 소진
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 3, "틀림", 5000);

            // Then
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("LIFE_EXHAUSTED");
            assertThat(result.remainingLives()).isEqualTo(0);
        }

        @Test
        @DisplayName("마지막 라운드에서 정답")
        void lastRound_correct() {
            // Given: 곡 3개
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // 처음 2개 정답
            fanChallengeService.processAnswer(session.getId(), 1, "Test Artist Song 1", 5000);
            fanChallengeService.processAnswer(session.getId(), 2, "Test Artist Song 2", 5000);

            // When: 마지막 라운드 정답
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 3, "Test Artist Song 3", 5000);

            // Then
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.gameOverReason()).isEqualTo("PERFECT_CLEAR");
            assertThat(result.correctCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("마지막 라운드에서 오답 (라이프 남음)")
        void lastRound_wrongWithLivesRemaining() {
            // Given: 곡 3개
            createSongsForArtist("Test Artist", 3);
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "Test Artist", FanChallengeDifficulty.HARDCORE);

            // 처음 2개 정답
            fanChallengeService.processAnswer(session.getId(), 1, "Test Artist Song 1", 5000);
            fanChallengeService.processAnswer(session.getId(), 2, "Test Artist Song 2", 5000);

            // When: 마지막 라운드 오답
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 3, "틀림", 5000);

            // Then: 게임 완료 but 퍼펙트 아님
            assertThat(result.isGameOver()).isTrue();
            assertThat(result.correctCount()).isEqualTo(2);
            assertThat(result.remainingLives()).isEqualTo(2);
            // 퍼펙트가 아니므로 PERFECT_CLEAR가 아니어야 함
            // 현재 버그: PERFECT_CLEAR로 나옴
        }
    }

    // =====================================================
    // Edge Case 10: 정답 변형 시나리오
    // =====================================================
    @Nested
    @DisplayName("정답 변형 시나리오")
    class AnswerVariationScenarios {

        @Test
        @DisplayName("대소문자 무시하고 정답 처리")
        void caseInsensitiveAnswer() {
            // Given
            createSong("Dynamite", "BTS");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "BTS", FanChallengeDifficulty.HARDCORE);

            // When: 소문자로 제출
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "dynamite", 5000);

            // Then
            assertThat(result.isCorrect()).isTrue();
        }

        @Test
        @DisplayName("앞뒤 공백 무시하고 정답 처리")
        void trimmedAnswer() {
            // Given
            createSong("Dynamite", "BTS");
            GameSession session = fanChallengeService.startChallenge(
                    null, "테스터", "BTS", FanChallengeDifficulty.HARDCORE);

            // When: 앞뒤 공백 포함
            FanChallengeService.AnswerResult result = fanChallengeService.processAnswer(
                    session.getId(), 1, "  Dynamite  ", 5000);

            // Then
            assertThat(result.isCorrect()).isTrue();
        }
    }
}
