package com.kh.game.integration;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import com.kh.game.service.*;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("레트로 랭킹 통합 테스트")
class RetroRankingIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private MemberRepository memberRepository;

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

    private Genre retroGenre;
    private Genre kpopGenre;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        gameSessionRepository.deleteAll();
        songAnswerRepository.deleteAll();
        songRepository.deleteAll();
        genreRepository.deleteAll();
        memberRepository.deleteAll();

        // YouTube Mock
        YouTubeValidationService.ValidationResult validResult = YouTubeValidationService.ValidationResult.valid();
        Mockito.when(youTubeValidationService.validateVideo(anyString())).thenReturn(validResult);

        // 장르 생성
        retroGenre = createGenre("RETRO", "Retro/Oldies");
        kpopGenre = createGenre("KPOP", "K-POP");

        // 레트로 곡 생성
        createRetroSongsWithAnswers(30);
    }

    private Genre createGenre(String code, String name) {
        Genre genre = new Genre();
        genre.setCode(code);
        genre.setName(name);
        genre.setUseYn("Y");
        return genreRepository.save(genre);
    }

    private Member createMember(String email, String nickname) {
        Member member = new Member();
        member.setEmail(email);
        member.setPassword("password123");
        member.setNickname(nickname);
        member.setUsername(email.split("@")[0]);  // email에서 username 생성
        return memberRepository.save(member);
    }

    private void createRetroSongsWithAnswers(int count) {
        for (int i = 1; i <= count; i++) {
            Song song = new Song();
            song.setTitle("레트로 곡 " + i);
            song.setArtist("레트로 가수");
            song.setGenre(retroGenre);
            song.setReleaseYear(1990 + (i % 10));
            song.setUseYn("Y");
            song.setYoutubeVideoId("retro" + i);
            song.setStartTime(0);
            song.setPlayDuration(30);
            song.setIsPopular(true);
            Song saved = songRepository.save(song);

            SongAnswer answer = new SongAnswer();
            answer.setSong(saved);
            answer.setAnswer("레트로 곡 " + i);
            answer.setIsPrimary(true);
            songAnswerRepository.save(answer);
        }
    }

    private void playRetroGame(Member member, int rounds, int score) {
        playRetroGame(member, rounds, score, true);
    }

    private void playRetroGame(Member member, int rounds, int score, boolean isEligibleForBestScore) {
        // 게임 결과 시뮬레이션
        memberService.addRetroGameResult(
            member.getId(),
            score,
            (int) (score / 100.0),  // 대략적인 정답 수
            rounds,
            0,  // 스킵 수
            isEligibleForBestScore
        );
    }

    // =====================================================
    // Scenario 1: 레트로 랭킹 분리 (SOLO_GUESS와 독립)
    // =====================================================
    @Nested
    @DisplayName("레트로 랭킹 분리 (SOLO_GUESS와 독립)")
    class RetroRankingSeparation {

        @Test
        @DisplayName("레트로 게임은 SOLO_GUESS 랭킹에 영향 없음")
        void retroGame_shouldNotAffectGuessRanking() {
            // Given
            Member member = createMember("player1@test.com", "플레이어1");
            int initialGuessScore = member.getGuessScore();

            // When: 레트로 게임 플레이
            playRetroGame(member, 10, 800);

            // Then
            Member updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getGuessScore()).isEqualTo(initialGuessScore);  // 변경 없음
            assertThat(updated.getRetroScore()).isEqualTo(800);  // 레트로 점수만 증가
        }

        @Test
        @DisplayName("SOLO_GUESS 게임은 레트로 랭킹에 영향 없음")
        void guessGame_shouldNotAffectRetroRanking() {
            // Given
            Member member = createMember("player2@test.com", "플레이어2");

            // 먼저 레트로 게임 플레이
            playRetroGame(member, 10, 500);
            int initialRetroScore = memberRepository.findById(member.getId()).orElseThrow().getRetroScore();

            // When: SOLO_GUESS 게임 결과 추가
            memberService.addGuessGameResult(member.getId(), 1000, 10, 10, 0, true);

            // Then
            Member updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getRetroScore()).isEqualTo(initialRetroScore);  // 변경 없음
            assertThat(updated.getGuessScore()).isEqualTo(1000);  // GUESS 점수만 증가
        }

        @Test
        @DisplayName("두 게임을 모두 플레이해도 각각 독립적")
        void bothGames_shouldBeIndependent() {
            // Given
            Member member = createMember("player3@test.com", "플레이어3");

            // When
            playRetroGame(member, 10, 600);
            memberService.addGuessGameResult(member.getId(), 800, 10, 8, 2, true);

            // Then
            Member updated = memberRepository.findById(member.getId()).orElseThrow();
            assertThat(updated.getRetroScore()).isEqualTo(600);
            assertThat(updated.getGuessScore()).isEqualTo(800);
            assertThat(updated.getRetroGames()).isEqualTo(1);
            assertThat(updated.getGuessGames()).isEqualTo(1);
        }
    }

    // =====================================================
    // Scenario 2: 레트로 랭킹 조회
    // =====================================================
    @Nested
    @DisplayName("레트로 랭킹 조회")
    class RetroRankingQueries {

        @Test
        @DisplayName("점수순 레트로 랭킹 조회")
        void getRetroRankingByScore_shouldReturnTopPlayers() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");
            Member player3 = createMember("player3@test.com", "플레이어3");

            playRetroGame(player1, 10, 800);
            playRetroGame(player2, 10, 1000);  // 1위
            playRetroGame(player3, 10, 600);

            // When
            List<Member> ranking = memberService.getRetroRankingByScore(10);

            // Then
            assertThat(ranking).hasSize(3);
            assertThat(ranking.get(0).getEmail()).isEqualTo("player2@test.com");
            assertThat(ranking.get(1).getEmail()).isEqualTo("player1@test.com");
            assertThat(ranking.get(2).getEmail()).isEqualTo("player3@test.com");
        }

        @Test
        @DisplayName("정확도순 레트로 랭킹 조회")
        void getRetroRankingByAccuracy_shouldReturnByCorrectRate() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");

            // Player1: 80% 정확도
            memberService.addRetroGameResult(player1.getId(), 800, 8, 10, 2, true);
            // Player2: 90% 정확도
            memberService.addRetroGameResult(player2.getId(), 700, 9, 10, 1, true);

            // When
            List<Member> ranking = memberService.getRetroRankingByAccuracy(10);

            // Then
            assertThat(ranking.get(0).getEmail()).isEqualTo("player2@test.com");  // 90%
            assertThat(ranking.get(1).getEmail()).isEqualTo("player1@test.com");  // 80%
        }

        @Test
        @DisplayName("게임 수순 레트로 랭킹 조회")
        void getRetroRankingByGames_shouldReturnByGameCount() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");

            playRetroGame(player1, 10, 500);
            playRetroGame(player1, 10, 600);  // 2게임
            playRetroGame(player2, 10, 900);  // 1게임

            // When
            List<Member> ranking = memberService.getRetroRankingByGames(10);

            // Then
            assertThat(ranking.get(0).getEmail()).isEqualTo("player1@test.com");
            assertThat(ranking.get(0).getRetroGames()).isEqualTo(2);
        }

        @Test
        @DisplayName("레트로 게임 안한 회원은 랭킹에 없음")
        void noRetroGames_shouldNotAppearInRanking() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member noGamePlayer = createMember("player2@test.com", "플레이어2");

            playRetroGame(player1, 10, 800);
            // player2는 레트로 게임 안함

            // When
            List<Member> ranking = memberService.getRetroRankingByScore(10);

            // Then
            assertThat(ranking).hasSize(1);
            assertThat(ranking.get(0).getEmail()).isEqualTo("player1@test.com");
        }
    }

    // =====================================================
    // Scenario 3: 주간 레트로 랭킹
    // =====================================================
    @Nested
    @DisplayName("주간 레트로 랭킹")
    class WeeklyRetroRanking {

        @Test
        @DisplayName("주간 레트로 통계 누적")
        void weeklyRetroStats_shouldAccumulate() {
            // Given
            Member player = createMember("player@test.com", "플레이어");

            // When: 여러 게임 플레이
            playRetroGame(player, 10, 500);
            playRetroGame(player, 10, 600);
            playRetroGame(player, 10, 700);

            // Then
            Member updated = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(updated.getWeeklyRetroScore()).isEqualTo(1800);  // 합계
            assertThat(updated.getWeeklyRetroGames()).isEqualTo(3);
        }

        @Test
        @DisplayName("주간 점수순 레트로 랭킹")
        void getWeeklyRetroRankingByScore_shouldUseWeeklyStats() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");

            playRetroGame(player1, 10, 1200);
            playRetroGame(player2, 10, 900);

            // When
            List<Member> ranking = memberService.getWeeklyRetroRankingByScore(10);

            // Then
            assertThat(ranking.get(0).getEmail()).isEqualTo("player1@test.com");
            assertThat(ranking.get(0).getWeeklyRetroScore()).isEqualTo(1200);
        }

        @Test
        @DisplayName("주간 초기화 시 주간 레트로 통계도 초기화")
        void weeklyReset_shouldClearWeeklyRetroStats() {
            // Given
            Member player = createMember("player@test.com", "플레이어");
            playRetroGame(player, 10, 1000);

            // When
            Member beforeReset = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(beforeReset.getWeeklyRetroScore()).isEqualTo(1000);

            beforeReset.resetWeeklyStats();  // 주간 통계 초기화
            memberRepository.save(beforeReset);

            // Then
            Member afterReset = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(afterReset.getWeeklyRetroScore()).isEqualTo(0);
            assertThat(afterReset.getRetroScore()).isEqualTo(1000);  // 전체 통계는 유지
        }
    }

    // =====================================================
    // Scenario 4: 30곡 도전 베스트 점수
    // =====================================================
    @Nested
    @DisplayName("30곡 도전 베스트 점수")
    class RetroBestScore {

        @Test
        @DisplayName("30곡 레트로 게임은 베스트 점수 업데이트")
        void thirtyRoundRetroGame_shouldUpdateBestScore() {
            // Given
            Member player = createMember("player@test.com", "플레이어");

            // When: 30곡 게임
            memberService.addRetroGameResult(player.getId(), 2500, 25, 30, 5, true);

            // Then
            Member updated = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(updated.getRetroBest30Score()).isEqualTo(2500);
            assertThat(updated.getWeeklyRetroBest30Score()).isEqualTo(2500);
        }

        @Test
        @DisplayName("더 높은 점수는 베스트 점수 갱신")
        void higherScore_shouldUpdateBestScore() {
            // Given
            Member player = createMember("player@test.com", "플레이어");
            memberService.addRetroGameResult(player.getId(), 2000, 20, 30, 10, true);

            // When: 더 높은 점수로 다시 플레이
            memberService.addRetroGameResult(player.getId(), 2800, 28, 30, 2, true);

            // Then
            Member updated = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(updated.getRetroBest30Score()).isEqualTo(2800);
        }

        @Test
        @DisplayName("더 낮은 점수는 베스트 점수 갱신 안함")
        void lowerScore_shouldNotUpdateBestScore() {
            // Given
            Member player = createMember("player@test.com", "플레이어");
            memberService.addRetroGameResult(player.getId(), 2500, 25, 30, 5, true);

            // When: 더 낮은 점수로 다시 플레이
            memberService.addRetroGameResult(player.getId(), 2000, 20, 30, 10, true);

            // Then
            Member updated = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(updated.getRetroBest30Score()).isEqualTo(2500);  // 변경 없음
        }

        @Test
        @DisplayName("10곡 게임은 베스트 점수에 영향 없음")
        void nonThirtyRoundGame_shouldNotUpdateBestScore() {
            // Given
            Member player = createMember("player@test.com", "플레이어");

            // When: 10곡 게임 (높은 점수) - 10곡이므로 베스트 점수 업데이트 안됨
            memberService.addRetroGameResult(player.getId(), 1000, 10, 10, 0, true);

            // Then
            Member updated = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(updated.getRetroBest30Score()).isNull();  // 설정 안됨
        }
    }

    // =====================================================
    // Scenario 5: 내 레트로 랭킹 조회
    // =====================================================
    @Nested
    @DisplayName("내 레트로 랭킹 조회")
    class MyRetroRankingApi {

        @Test
        @DisplayName("내 레트로 순위 조회")
        void getMyRetroRank_shouldReturnCorrectRank() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");
            Member player3 = createMember("player3@test.com", "플레이어3");

            playRetroGame(player1, 10, 800);   // 2위
            playRetroGame(player2, 10, 1000);  // 1위
            playRetroGame(player3, 10, 600);   // 3위

            // When
            long rank = memberService.getMyRetroRank(player1.getRetroScore());

            // Then
            assertThat(rank).isEqualTo(2);
        }

        @Test
        @DisplayName("동점자 순위 처리")
        void sameScore_shouldHandleTieCorrectly() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");
            Member player3 = createMember("player3@test.com", "플레이어3");

            playRetroGame(player1, 10, 800);
            playRetroGame(player2, 10, 800);  // 동점
            playRetroGame(player3, 10, 600);

            // When & Then: 800점은 공동 1위
            long rank = memberService.getMyRetroRank(800);
            assertThat(rank).isEqualTo(1);
        }

        @Test
        @DisplayName("레트로 참가자 수 조회")
        void getRetroParticipantCount_shouldReturnCorrectCount() {
            // Given
            Member player1 = createMember("player1@test.com", "플레이어1");
            Member player2 = createMember("player2@test.com", "플레이어2");
            createMember("player3@test.com", "플레이어3");  // 게임 안함

            playRetroGame(player1, 10, 500);
            playRetroGame(player2, 10, 600);

            // When
            long count = memberService.getRetroParticipantCount();

            // Then
            assertThat(count).isEqualTo(2);  // 게임한 사람만
        }
    }

    // =====================================================
    // Scenario 6: 통계 정확성
    // =====================================================
    @Nested
    @DisplayName("통계 정확성")
    class StatsAccuracy {

        @Test
        @DisplayName("여러 게임 후 총계 정확성")
        void multipleGames_totalsShouldBeAccurate() {
            // Given
            Member player = createMember("player@test.com", "플레이어");

            // When: 3게임 플레이
            memberService.addRetroGameResult(player.getId(), 500, 5, 10, 5, true);
            memberService.addRetroGameResult(player.getId(), 700, 7, 10, 3, true);
            memberService.addRetroGameResult(player.getId(), 600, 6, 10, 4, true);

            // Then
            Member updated = memberRepository.findById(player.getId()).orElseThrow();
            assertThat(updated.getRetroGames()).isEqualTo(3);
            assertThat(updated.getRetroScore()).isEqualTo(1800);  // 500 + 700 + 600
            assertThat(updated.getRetroCorrect()).isEqualTo(18);  // 5 + 7 + 6
            assertThat(updated.getRetroRounds()).isEqualTo(30);   // 10 + 10 + 10
            assertThat(updated.getRetroSkip()).isEqualTo(12);     // 5 + 3 + 4
        }

        @Test
        @DisplayName("정확도 계산 정확성")
        void accuracyCalculation_shouldBeCorrect() {
            // Given
            Member player = createMember("player@test.com", "플레이어");
            memberService.addRetroGameResult(player.getId(), 800, 8, 10, 2, true);

            // When
            Member updated = memberRepository.findById(player.getId()).orElseThrow();

            // Then: 8/10 = 80%
            double accuracy = (double) updated.getRetroCorrect() / updated.getRetroRounds() * 100;
            assertThat(accuracy).isEqualTo(80.0);
        }
    }
}
