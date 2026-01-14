package com.kh.game.service;

import com.kh.game.entity.*;
import com.kh.game.repository.GameRoundAttemptRepository;
import com.kh.game.repository.GameRoundRepository;
import com.kh.game.repository.GameSessionRepository;
import com.kh.game.repository.MemberRepository;
import com.kh.game.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class WrongAnswerStatsServiceTest {

    @Autowired
    private WrongAnswerStatsService wrongAnswerStatsService;

    @Autowired
    private GameRoundAttemptRepository attemptRepository;

    @Autowired
    private GameRoundRepository gameRoundRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Song song1;
    private Song song2;
    private Member member;
    private GameSession session;

    @BeforeEach
    void setUp() {
        // 테스트용 멤버 생성
        member = new Member();
        member.setEmail("test@test.com");
        member.setUsername("testuser");
        member.setPassword("password");
        member.setNickname("테스터");
        member = memberRepository.save(member);

        // 테스트용 곡 생성
        song1 = new Song();
        song1.setTitle("Celebrity");
        song1.setArtist("아이유");
        song1 = songRepository.save(song1);

        song2 = new Song();
        song2.setTitle("좋은 날");
        song2.setArtist("아이유");
        song2 = songRepository.save(song2);

        // 테스트용 게임 세션 생성
        session = new GameSession();
        session.setMember(member);
        session.setNickname("테스터");
        session.setGameType(GameSession.GameType.SOLO_GUESS);
        session.setGameMode(GameSession.GameMode.RANDOM);
        session.setStatus(GameSession.GameStatus.COMPLETED);
        session = gameSessionRepository.save(session);
    }

    @Test
    @DisplayName("오답+정답 쌍 그룹핑 - 같은 오답이 다른 곡에서 나오면 별도로 집계")
    void getMostCommonWrongAnswersWithSong_shouldGroupByAnswerAndSong() {
        // Given: Celebrity에서 "celebrit" 오답 3회
        createWrongAttempt(song1, "celebrit");
        createWrongAttempt(song1, "celebrit");
        createWrongAttempt(song1, "celebrit");

        // Given: 좋은 날에서 "celebrit" 오답 1회 (같은 오답, 다른 곡)
        createWrongAttempt(song2, "celebrit");

        // Given: Celebrity에서 "셀럽" 오답 2회
        createWrongAttempt(song1, "셀럽");
        createWrongAttempt(song1, "셀럽");

        // When
        List<Map<String, Object>> result = wrongAnswerStatsService.getMostCommonWrongAnswersWithSong(10);

        // Then: 3개의 그룹이 나와야 함 (celebrit+Celebrity, celebrit+좋은날, 셀럽+Celebrity)
        assertThat(result).hasSize(3);

        // 첫 번째: celebrit + Celebrity = 3회 (가장 많음)
        assertThat(result.get(0).get("answer")).isEqualTo("celebrit");
        assertThat(result.get(0).get("songTitle")).isEqualTo("Celebrity");
        assertThat(result.get(0).get("count")).isEqualTo(3L);

        // 두 번째: 셀럽 + Celebrity = 2회
        assertThat(result.get(1).get("answer")).isEqualTo("셀럽");
        assertThat(result.get(1).get("songTitle")).isEqualTo("Celebrity");
        assertThat(result.get(1).get("count")).isEqualTo(2L);

        // 세 번째: celebrit + 좋은 날 = 1회
        assertThat(result.get(2).get("answer")).isEqualTo("celebrit");
        assertThat(result.get(2).get("songTitle")).isEqualTo("좋은 날");
        assertThat(result.get(2).get("count")).isEqualTo(1L);
    }

    @Test
    @DisplayName("오답+정답 쌍 그룹핑 - 정답은 제외되어야 함")
    void getMostCommonWrongAnswersWithSong_shouldExcludeCorrectAnswers() {
        // Given: 정답 시도
        createCorrectAttempt(song1, "Celebrity");

        // Given: 오답 시도
        createWrongAttempt(song1, "celebrit");

        // When
        List<Map<String, Object>> result = wrongAnswerStatsService.getMostCommonWrongAnswersWithSong(10);

        // Then: 오답만 1개 있어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("answer")).isEqualTo("celebrit");
    }

    @Test
    @DisplayName("오답+정답 쌍 그룹핑 - 데이터 없으면 빈 리스트")
    void getMostCommonWrongAnswersWithSong_shouldReturnEmptyWhenNoData() {
        // When
        List<Map<String, Object>> result = wrongAnswerStatsService.getMostCommonWrongAnswersWithSong(10);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("오답+정답 쌍 그룹핑 - limit 적용")
    void getMostCommonWrongAnswersWithSong_shouldRespectLimit() {
        // Given: 5개의 다른 오답+곡 조합
        createWrongAttempt(song1, "오답1");
        createWrongAttempt(song1, "오답2");
        createWrongAttempt(song1, "오답3");
        createWrongAttempt(song2, "오답4");
        createWrongAttempt(song2, "오답5");

        // When: limit 3
        List<Map<String, Object>> result = wrongAnswerStatsService.getMostCommonWrongAnswersWithSong(3);

        // Then
        assertThat(result).hasSize(3);
    }

    private void createWrongAttempt(Song song, String userAnswer) {
        GameRound round = new GameRound();
        round.setGameSession(session);
        round.setSong(song);
        round.setRoundNumber(1);
        round.setStatus(GameRound.RoundStatus.ANSWERED);
        round.setIsCorrect(false);
        round = gameRoundRepository.save(round);

        GameRoundAttempt attempt = new GameRoundAttempt(round, 1, userAnswer, false);
        attemptRepository.save(attempt);
    }

    private void createCorrectAttempt(Song song, String userAnswer) {
        GameRound round = new GameRound();
        round.setGameSession(session);
        round.setSong(song);
        round.setRoundNumber(1);
        round.setStatus(GameRound.RoundStatus.ANSWERED);
        round.setIsCorrect(true);
        round = gameRoundRepository.save(round);

        GameRoundAttempt attempt = new GameRoundAttempt(round, 1, userAnswer, true);
        attemptRepository.save(attempt);
    }
}
