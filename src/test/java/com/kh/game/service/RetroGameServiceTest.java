package com.kh.game.service;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.*;
import com.kh.game.repository.*;
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
@DisplayName("레트로 게임 Service 테스트")
class RetroGameServiceTest {

    @Autowired
    private SongService songService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private SongAnswerRepository songAnswerRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @MockitoBean
    private YouTubeValidationService youTubeValidationService;

    private Genre kpopGenre;
    private Genre retroGenre;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        gameSessionRepository.deleteAll();
        songAnswerRepository.deleteAll();
        songRepository.deleteAll();
        genreRepository.deleteAll();
        memberRepository.deleteAll();

        // YouTube 검증 항상 성공하도록 Mock 설정
        YouTubeValidationService.ValidationResult validResult = YouTubeValidationService.ValidationResult.valid();
        Mockito.when(youTubeValidationService.validateVideo(anyString())).thenReturn(validResult);

        // 장르 생성
        kpopGenre = createGenre("KPOP", "K-POP");
        retroGenre = createGenre("RETRO", "Retro/Oldies");

        // 테스트 회원 생성
        testMember = createTestMember();
    }

    private Genre createGenre(String code, String name) {
        Genre genre = new Genre();
        genre.setCode(code);
        genre.setName(name);
        genre.setUseYn("Y");
        genre.setDisplayOrder(1);
        return genreRepository.save(genre);
    }

    private Song createSong(String title, String artist, Integer releaseYear, Genre genre) {
        Song song = new Song();
        song.setTitle(title);
        song.setArtist(artist);
        song.setReleaseYear(releaseYear);
        song.setGenre(genre);
        song.setYoutubeVideoId("abc" + System.nanoTime());  // 고유한 ID
        song.setPlayDuration(180);
        song.setStartTime(0);
        song.setUseYn("Y");
        Song saved = songRepository.save(song);

        // 정답 추가
        SongAnswer answer = new SongAnswer(saved, title, true);
        songAnswerRepository.save(answer);

        return saved;
    }

    private Member createTestMember() {
        Member member = new Member();
        member.setEmail("test@test.com");
        member.setPassword("password123");
        member.setNickname("테스트유저");
        member.setUsername("testuser");
        member.setStatus(Member.MemberStatus.ACTIVE);
        member.setRole(Member.MemberRole.USER);
        return memberRepository.save(member);
    }

    // =====================================================
    // Scenario 1: 레트로 노래 선택 로직
    // =====================================================
    @Nested
    @DisplayName("레트로 노래 선택 로직")
    class RetroSongSelectionLogic {

        @Test
        @DisplayName("findRetroSongsForGame - releaseYear < 2000 인 곡 포함")
        void findRetroSongsForGame_shouldIncludeOldSongs() {
            // Given
            createSong("1999년 노래", "아티스트A", 1999, kpopGenre);
            createSong("2000년 노래", "아티스트B", 2000, kpopGenre);  // 제외됨
            createSong("2020년 노래", "아티스트C", 2020, kpopGenre);  // 제외됨

            // When
            List<Song> retroSongs = songService.findRetroSongsForGame();

            // Then
            assertThat(retroSongs).hasSize(1);
            assertThat(retroSongs.get(0).getTitle()).isEqualTo("1999년 노래");
        }

        @Test
        @DisplayName("findRetroSongsForGame - RETRO 장르 곡 포함")
        void findRetroSongsForGame_shouldIncludeRetroGenreSongs() {
            // Given
            createSong("2020년 레트로 장르", "아티스트A", 2020, retroGenre);  // 2020년이지만 RETRO 장르
            createSong("2020년 일반", "아티스트B", 2020, kpopGenre);  // 제외됨

            // When
            List<Song> retroSongs = songService.findRetroSongsForGame();

            // Then
            assertThat(retroSongs).hasSize(1);
            assertThat(retroSongs.get(0).getTitle()).isEqualTo("2020년 레트로 장르");
        }

        @Test
        @DisplayName("findRetroSongsForGame - OR 로직 검증 (연도 < 2000 OR 장르 = RETRO)")
        void findRetroSongsForGame_shouldUseOrLogic() {
            // Given
            createSong("1999년 K-POP", "아티스트A", 1999, kpopGenre);  // 연도로 포함
            createSong("2020년 RETRO", "아티스트B", 2020, retroGenre);  // 장르로 포함
            createSong("2020년 K-POP", "아티스트C", 2020, kpopGenre);  // 둘 다 아님

            // When
            List<Song> retroSongs = songService.findRetroSongsForGame();

            // Then
            assertThat(retroSongs).hasSize(2);
        }

        @Test
        @DisplayName("getRandomRetroSongs - 요청한 개수만큼 반환")
        void getRandomRetroSongs_shouldReturnRequestedCount() {
            // Given
            for (int i = 0; i < 10; i++) {
                createSong("노래" + i, "아티스트" + i, 1990 + i, kpopGenre);
            }

            // When
            List<Song> songs = songService.getRandomRetroSongs(5, new GameSettings());

            // Then
            assertThat(songs).hasSize(5);
        }

        @Test
        @DisplayName("getAvailableRetroSongCount - 레트로 곡 수 정확히 카운트")
        void getAvailableRetroSongCount_shouldCountCorrectly() {
            // Given
            createSong("1995년", "A", 1995, kpopGenre);
            createSong("1998년", "B", 1998, kpopGenre);
            createSong("2020년 레트로", "C", 2020, retroGenre);
            createSong("2020년 일반", "D", 2020, kpopGenre);  // 제외

            // When
            int count = songService.getAvailableRetroSongCount(new GameSettings());

            // Then
            assertThat(count).isEqualTo(3);
        }
    }

    // =====================================================
    // Scenario 2: 레트로 게임 통계 업데이트
    // =====================================================
    @Nested
    @DisplayName("레트로 게임 통계 업데이트")
    class RetroGameStatsUpdate {

        @Test
        @DisplayName("레트로 게임 결과가 레트로 통계에만 반영됨")
        void addRetroGameResult_shouldOnlyUpdateRetroStats() {
            // Given
            int initialGuessScore = testMember.getGuessScore() != null ? testMember.getGuessScore() : 0;
            int initialRetroScore = testMember.getRetroScore() != null ? testMember.getRetroScore() : 0;

            // When
            memberService.addRetroGameResult(testMember.getId(), 500, 5, 10, 0, true);

            // Then
            Member updated = memberRepository.findById(testMember.getId()).orElseThrow();
            assertThat(updated.getRetroScore()).isEqualTo(initialRetroScore + 500);
            assertThat(updated.getGuessScore()).isEqualTo(initialGuessScore);  // 변경 없음
        }

        @Test
        @DisplayName("레트로 게임 수와 라운드 수 증가")
        void addRetroGameResult_shouldIncrementGamesAndRounds() {
            // Given
            int initialGames = testMember.getRetroGames() != null ? testMember.getRetroGames() : 0;
            int initialRounds = testMember.getRetroRounds() != null ? testMember.getRetroRounds() : 0;

            // When
            memberService.addRetroGameResult(testMember.getId(), 500, 5, 10, 0, true);

            // Then
            Member updated = memberRepository.findById(testMember.getId()).orElseThrow();
            assertThat(updated.getRetroGames()).isEqualTo(initialGames + 1);
            assertThat(updated.getRetroRounds()).isEqualTo(initialRounds + 10);
        }

        @Test
        @DisplayName("레트로 30곡 최고점 갱신")
        void updateRetro30SongBestScore_shouldUpdateWhenHigher() {
            // Given - 첫 30곡 게임
            memberService.addRetroGameResult(testMember.getId(), 2000, 25, 30, 5, true);

            // When - 더 높은 점수
            boolean updated = memberService.updateRetro30SongBestScore(testMember.getId(), 2500);

            // Then
            assertThat(updated).isTrue();
            Member member = memberRepository.findById(testMember.getId()).orElseThrow();
            assertThat(member.getRetroBest30Score()).isEqualTo(2500);
        }

        @Test
        @DisplayName("레트로 30곡 최고점 - 낮은 점수는 갱신 안됨")
        void updateRetro30SongBestScore_shouldNotUpdateWhenLower() {
            // Given - 첫 30곡 게임으로 높은 점수
            memberService.updateRetro30SongBestScore(testMember.getId(), 2500);

            // When - 더 낮은 점수
            boolean updated = memberService.updateRetro30SongBestScore(testMember.getId(), 2000);

            // Then
            assertThat(updated).isFalse();
            Member member = memberRepository.findById(testMember.getId()).orElseThrow();
            assertThat(member.getRetroBest30Score()).isEqualTo(2500);
        }
    }

    // =====================================================
    // Scenario 3: 레트로 랭킹 조회
    // =====================================================
    @Nested
    @DisplayName("레트로 랭킹 조회")
    class RetroRankingQueries {

        @Test
        @DisplayName("레트로 총점 랭킹 조회")
        void getRetroRankingByScore_shouldReturnSorted() {
            // Given
            Member member1 = createMemberWithRetroScore("player1@test.com", "플레이어1", 1000);
            Member member2 = createMemberWithRetroScore("player2@test.com", "플레이어2", 2000);
            Member member3 = createMemberWithRetroScore("player3@test.com", "플레이어3", 1500);

            // When
            List<Member> ranking = memberService.getRetroRankingByScore(10);

            // Then
            assertThat(ranking).hasSize(3);
            assertThat(ranking.get(0).getNickname()).isEqualTo("플레이어2");
            assertThat(ranking.get(1).getNickname()).isEqualTo("플레이어3");
            assertThat(ranking.get(2).getNickname()).isEqualTo("플레이어1");
        }

        @Test
        @DisplayName("레트로 참가자 수 조회")
        void getRetroParticipantCount_shouldReturnCorrectCount() {
            // Given
            createMemberWithRetroScore("player1@test.com", "플레이어1", 1000);
            createMemberWithRetroScore("player2@test.com", "플레이어2", 2000);

            // When
            long count = memberService.getRetroParticipantCount();

            // Then
            assertThat(count).isEqualTo(2);
        }

        private Member createMemberWithRetroScore(String email, String nickname, int score) {
            Member member = new Member();
            member.setEmail(email);
            member.setPassword("password123");
            member.setNickname(nickname);
            member.setUsername(email.split("@")[0]);
            member.setStatus(Member.MemberStatus.ACTIVE);
            member.setRole(Member.MemberRole.USER);
            member.setRetroGames(1);
            member.setRetroScore(score);
            member.setRetroCorrect(score / 100);
            member.setRetroRounds(10);
            return memberRepository.save(member);
        }
    }

    // =====================================================
    // Scenario 4: 경계값 테스트
    // =====================================================
    @Nested
    @DisplayName("경계값 테스트")
    class EdgeCases {

        @Test
        @DisplayName("releaseYear = 1999는 레트로에 포함")
        void releaseYear1999_shouldBeIncluded() {
            // Given
            createSong("1999년", "A", 1999, kpopGenre);

            // When
            List<Song> songs = songService.findRetroSongsForGame();

            // Then
            assertThat(songs).hasSize(1);
        }

        @Test
        @DisplayName("releaseYear = 2000은 레트로에서 제외")
        void releaseYear2000_shouldBeExcluded() {
            // Given
            createSong("2000년", "A", 2000, kpopGenre);

            // When
            List<Song> songs = songService.findRetroSongsForGame();

            // Then
            assertThat(songs).isEmpty();
        }

        @Test
        @DisplayName("releaseYear = null + RETRO 장르는 포함")
        void nullReleaseYear_withRetroGenre_shouldBeIncluded() {
            // Given
            createSong("연도없는 레트로", "A", null, retroGenre);

            // When
            List<Song> songs = songService.findRetroSongsForGame();

            // Then
            assertThat(songs).hasSize(1);
        }

        @Test
        @DisplayName("useYn = N 인 곡은 제외")
        void inactiveSong_shouldBeExcluded() {
            // Given
            Song song = createSong("비활성 곡", "A", 1990, kpopGenre);
            song.setUseYn("N");
            songRepository.save(song);

            // When
            List<Song> songs = songService.findRetroSongsForGame();

            // Then
            assertThat(songs).isEmpty();
        }

        @Test
        @DisplayName("곡이 부족한 경우 가능한 만큼만 반환")
        void notEnoughSongs_shouldReturnAvailable() {
            // Given - 3곡만 생성
            createSong("노래1", "A", 1990, kpopGenre);
            createSong("노래2", "B", 1991, kpopGenre);
            createSong("노래3", "C", 1992, kpopGenre);

            // When - 10곡 요청
            List<Song> songs = songService.getRandomRetroSongs(10, new GameSettings());

            // Then - 3곡만 반환
            assertThat(songs).hasSize(3);
        }
    }
}
