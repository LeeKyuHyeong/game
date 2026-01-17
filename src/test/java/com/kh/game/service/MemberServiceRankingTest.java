package com.kh.game.service;

import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MemberService 최다 참여 랭킹 테스트")
class MemberServiceRankingTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();

        // 테스트 회원 생성 - 다양한 게임수와 라운드수
        createMember("게임왕", 200, 400, 1000);      // 게임수 최다
        createMember("라운드왕", 50, 1500, 500);     // 라운드수 최다
        createMember("균형플레이어", 100, 600, 750); // 중간
        createMember("초보자", 5, 20, 50);           // 적은 플레이
        createMember("미플레이", 0, 0, 0);           // 플레이 이력 없음
    }

    private Member createMember(String nickname, int guessGames, int guessRounds, int guessScore) {
        Member member = new Member();
        member.setEmail(nickname + "@test.com");
        member.setPassword("test1234");
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
    @DisplayName("getGuessRankingByGames - 게임수 기준 랭킹")
    class GamesRankingTests {

        @Test
        @DisplayName("게임수 내림차순 정렬")
        void orderByGamesDescending() {
            List<Member> ranking = memberService.getGuessRankingByGames(10);

            assertThat(ranking).hasSize(4);  // 게임수 0인 회원 제외
            assertThat(ranking.get(0).getNickname()).isEqualTo("게임왕");       // 200
            assertThat(ranking.get(1).getNickname()).isEqualTo("균형플레이어"); // 100
            assertThat(ranking.get(2).getNickname()).isEqualTo("라운드왕");     // 50
            assertThat(ranking.get(3).getNickname()).isEqualTo("초보자");       // 5
        }

        @Test
        @DisplayName("게임수 0인 회원 제외")
        void excludeZeroGames() {
            List<Member> ranking = memberService.getGuessRankingByGames(10);

            assertThat(ranking)
                    .extracting(Member::getNickname)
                    .doesNotContain("미플레이");
        }

        @Test
        @DisplayName("limit 적용 확인")
        void respectsLimit() {
            List<Member> ranking = memberService.getGuessRankingByGames(2);

            assertThat(ranking).hasSize(2);
            assertThat(ranking.get(0).getNickname()).isEqualTo("게임왕");
            assertThat(ranking.get(1).getNickname()).isEqualTo("균형플레이어");
        }
    }

    @Nested
    @DisplayName("getGuessRankingByRounds - 라운드수 기준 랭킹")
    class RoundsRankingTests {

        @Test
        @DisplayName("라운드수 내림차순 정렬")
        void orderByRoundsDescending() {
            List<Member> ranking = memberService.getGuessRankingByRounds(10);

            assertThat(ranking).hasSize(4);  // 라운드수 0인 회원 제외
            assertThat(ranking.get(0).getNickname()).isEqualTo("라운드왕");     // 1500
            assertThat(ranking.get(1).getNickname()).isEqualTo("균형플레이어"); // 600
            assertThat(ranking.get(2).getNickname()).isEqualTo("게임왕");       // 400
            assertThat(ranking.get(3).getNickname()).isEqualTo("초보자");       // 20
        }

        @Test
        @DisplayName("라운드수 0인 회원 제외")
        void excludeZeroRounds() {
            List<Member> ranking = memberService.getGuessRankingByRounds(10);

            assertThat(ranking)
                    .extracting(Member::getNickname)
                    .doesNotContain("미플레이");
        }

        @Test
        @DisplayName("비활성 회원 제외")
        void excludeInactiveMembers() {
            // INACTIVE 상태 회원 추가
            Member inactiveMember = new Member();
            inactiveMember.setEmail("inactive@test.com");
            inactiveMember.setPassword("test1234");
            inactiveMember.setNickname("비활성회원");
            inactiveMember.setUsername("inactive_user");
            inactiveMember.setStatus(Member.MemberStatus.INACTIVE);
            inactiveMember.setRole(Member.MemberRole.USER);
            inactiveMember.setGuessGames(999);
            inactiveMember.setGuessRounds(99999);  // 매우 높은 라운드수
            inactiveMember.setGuessScore(9999);
            memberRepository.save(inactiveMember);

            List<Member> ranking = memberService.getGuessRankingByRounds(10);

            assertThat(ranking)
                    .extracting(Member::getNickname)
                    .doesNotContain("비활성회원");
            // 1등은 여전히 라운드왕
            assertThat(ranking.get(0).getNickname()).isEqualTo("라운드왕");
        }
    }
}
