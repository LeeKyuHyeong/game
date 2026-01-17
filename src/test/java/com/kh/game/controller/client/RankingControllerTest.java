package com.kh.game.controller.client;

import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("RankingController 최다 참여 서브탭 테스트")
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // 기존 테스트 데이터 정리
        memberRepository.deleteAll();

        // 테스트 회원 생성 - 게임수와 라운드수가 다른 회원들
        createMember("플레이어A", 100, 500, 10);   // 게임수 100, 라운드 500
        createMember("플레이어B", 50, 800, 200);   // 게임수 50, 라운드 800
        createMember("플레이어C", 150, 300, 50);   // 게임수 150, 라운드 300
        createMember("플레이어D", 0, 0, 0);        // 비활성 회원 (게임 0)
    }

    private Member createMember(String nickname, int guessGames, int guessRounds, int guessScore) {
        Member member = new Member();
        member.setEmail(nickname.toLowerCase() + "@test.com");
        member.setPassword("password123");
        member.setNickname(nickname);
        member.setUsername("user_" + nickname.hashCode());
        member.setStatus(Member.MemberStatus.ACTIVE);
        member.setRole(Member.MemberRole.USER);
        member.setGuessGames(guessGames);
        member.setGuessRounds(guessRounds);
        member.setGuessScore(guessScore);
        member.setGuessCorrect(guessScore / 10);
        return memberRepository.save(member);
    }

    @Nested
    @DisplayName("게임수 랭킹 API")
    class GamesRankingTests {

        @Test
        @DisplayName("GET /api/ranking?type=games - 게임수 기준 내림차순 정렬")
        void getGuessRankingByGames_orderByGamesDesc() throws Exception {
            mockMvc.perform(get("/api/ranking")
                            .param("mode", "guess")
                            .param("type", "games")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))  // 게임 0인 회원 제외
                    .andExpect(jsonPath("$[0].nickname").value("플레이어C"))  // 150 게임
                    .andExpect(jsonPath("$[1].nickname").value("플레이어A"))  // 100 게임
                    .andExpect(jsonPath("$[2].nickname").value("플레이어B"))  // 50 게임
                    .andExpect(jsonPath("$[0].totalGames").value(150))
                    .andExpect(jsonPath("$[1].totalGames").value(100))
                    .andExpect(jsonPath("$[2].totalGames").value(50));
        }

        @Test
        @DisplayName("limit 파라미터 적용 확인")
        void getGuessRankingByGames_respectsLimit() throws Exception {
            mockMvc.perform(get("/api/ranking")
                            .param("mode", "guess")
                            .param("type", "games")
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].nickname").value("플레이어C"))
                    .andExpect(jsonPath("$[1].nickname").value("플레이어A"));
        }
    }

    @Nested
    @DisplayName("라운드수 랭킹 API")
    class RoundsRankingTests {

        @Test
        @DisplayName("GET /api/ranking?type=rounds - 라운드수 기준 내림차순 정렬")
        void getGuessRankingByRounds_orderByRoundsDesc() throws Exception {
            mockMvc.perform(get("/api/ranking")
                            .param("mode", "guess")
                            .param("type", "rounds")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))  // 라운드 0인 회원 제외
                    .andExpect(jsonPath("$[0].nickname").value("플레이어B"))  // 800 라운드
                    .andExpect(jsonPath("$[1].nickname").value("플레이어A"))  // 500 라운드
                    .andExpect(jsonPath("$[2].nickname").value("플레이어C"))  // 300 라운드
                    .andExpect(jsonPath("$[0].totalRounds").value(800))
                    .andExpect(jsonPath("$[1].totalRounds").value(500))
                    .andExpect(jsonPath("$[2].totalRounds").value(300));
        }

        @Test
        @DisplayName("비활성 회원 제외 확인")
        void getGuessRankingByRounds_excludesInactive() throws Exception {
            // INACTIVE 상태 회원 추가
            Member inactiveMember = new Member();
            inactiveMember.setEmail("inactive@test.com");
            inactiveMember.setPassword("password123");
            inactiveMember.setNickname("비활성회원");
            inactiveMember.setUsername("inactive_user");
            inactiveMember.setStatus(Member.MemberStatus.INACTIVE);
            inactiveMember.setRole(Member.MemberRole.USER);
            inactiveMember.setGuessGames(1000);
            inactiveMember.setGuessRounds(9999);  // 매우 높은 라운드수
            inactiveMember.setGuessScore(5000);
            memberRepository.save(inactiveMember);

            mockMvc.perform(get("/api/ranking")
                            .param("mode", "guess")
                            .param("type", "rounds")
                            .param("limit", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))  // INACTIVE 회원 제외
                    .andExpect(jsonPath("$[0].nickname").value("플레이어B"));  // 여전히 B가 1등
        }
    }
}
