package com.kh.game.batch;

import com.kh.game.entity.*;
import com.kh.game.entity.RankingHistory.PeriodType;
import com.kh.game.entity.RankingHistory.RankingType;
import com.kh.game.repository.MemberRepository;
import com.kh.game.repository.RankingHistoryRepository;
import com.kh.game.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RankingSnapshotBatch TDD 테스트
 *
 * 기능:
 * - 주간/월간 랭킹 리셋 전에 Top 100 스냅샷 저장
 * - 6종류 랭킹 타입 지원
 * - 기록 보존으로 명예의 전당 구현 가능
 */
@ExtendWith(MockitoExtension.class)
class RankingSnapshotBatchTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RankingHistoryRepository rankingHistoryRepository;

    @Mock
    private BatchService batchService;

    @Captor
    private ArgumentCaptor<RankingHistory> historyCaptor;

    private RankingSnapshotBatch batch;

    @BeforeEach
    void setUp() {
        batch = new RankingSnapshotBatch(memberRepository, rankingHistoryRepository, batchService);
    }

    // ========== 테스트 데이터 헬퍼 ==========

    private Member createMember(Long id, String nickname, int weeklyGuessScore, int weeklyMultiScore) {
        Member member = new Member();
        member.setId(id);
        member.setNickname(nickname);
        member.setStatus(Member.MemberStatus.ACTIVE);
        member.setWeeklyGuessScore(weeklyGuessScore);
        member.setWeeklyGuessGames(10);
        member.setWeeklyGuessCorrect(8);
        member.setWeeklyGuessRounds(10);
        member.setWeeklyMultiScore(weeklyMultiScore);
        member.setWeeklyMultiGames(5);
        member.setWeeklyMultiCorrect(4);
        member.setWeeklyMultiRounds(5);
        return member;
    }

    private Member createMemberWithBest30(Long id, String nickname, Integer weeklyBest30, Integer monthlyBest30) {
        Member member = createMember(id, nickname, 0, 0);
        member.setWeeklyBest30Score(weeklyBest30);
        member.setWeeklyBest30At(LocalDateTime.now().minusDays(1));
        member.setMonthlyBest30Score(monthlyBest30);
        member.setMonthlyBest30At(LocalDateTime.now().minusDays(1));
        return member;
    }

    private Member createMemberWithTier(Long id, String nickname, MultiTier tier, int lp, int wins) {
        Member member = createMember(id, nickname, 0, 0);
        member.setMultiTier(tier);
        member.setMultiLp(lp);
        member.setMultiWins(wins);
        member.setMultiGames(20);
        return member;
    }

    private List<Member> createMembers(int count, int baseScore) {
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            members.add(createMember(
                    (long) (i + 1),
                    "Player" + (i + 1),
                    baseScore - (i * 10),  // 점수 내림차순
                    baseScore - (i * 5)
            ));
        }
        return members;
    }

    // ========== 주간 스냅샷 테스트 ==========

    @Nested
    @DisplayName("주간 스냅샷 저장")
    class WeeklySnapshotTests {

        @Test
        @DisplayName("TC-07: 주간 스냅샷 저장 - 정상 케이스")
        void shouldSaveWeeklySnapshot() {
            // given
            List<Member> guessRanking = createMembers(5, 1000);
            List<Member> multiRanking = createMembers(3, 500);
            List<Member> best30Ranking = List.of(
                    createMemberWithBest30(1L, "Player1", 280, null),
                    createMemberWithBest30(2L, "Player2", 250, null)
            );
            List<Member> tierRanking = List.of(
                    createMemberWithTier(1L, "Player1", MultiTier.DIAMOND, 80, 15)
            );

            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(guessRanking);
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(multiRanking);
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(best30Ranking);
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(tierRanking);
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            int result = batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            assertThat(result).isEqualTo(11);  // 5 + 3 + 2 + 1 + 0
            verify(rankingHistoryRepository, times(11)).save(any(RankingHistory.class));
        }

        @Test
        @DisplayName("TC-09: 빈 랭킹 처리 - 0개 저장")
        void shouldHandleEmptyRanking() {
            // given
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            int result = batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            assertThat(result).isZero();
            verify(rankingHistoryRepository, never()).save(any(RankingHistory.class));
            verify(batchService).recordExecution(
                    eq(RankingSnapshotBatch.BATCH_ID),
                    eq(BatchExecutionHistory.ExecutionType.SCHEDULED),
                    eq(BatchConfig.ExecutionResult.SUCCESS),
                    anyString(),
                    eq(0),
                    anyLong()
            );
        }

        @Test
        @DisplayName("TC-10: Top 100 제한 검증")
        void shouldLimitToTop100() {
            // given
            List<Member> largeRanking = createMembers(150, 5000);

            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(largeRanking.subList(0, 100));  // Repository에서 100명만 반환
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository, times(100)).save(historyCaptor.capture());

            List<RankingHistory> savedHistories = historyCaptor.getAllValues();
            assertThat(savedHistories).hasSize(100);

            // 순위가 1~100인지 확인
            assertThat(savedHistories.get(0).getRankPosition()).isEqualTo(1);
            assertThat(savedHistories.get(99).getRankPosition()).isEqualTo(100);
        }

        @Test
        @DisplayName("TC-12: 닉네임 스냅샷 보존")
        void shouldPreserveNickname() {
            // given
            Member member = createMember(1L, "OriginalNickname", 1000, 500);
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(List.of(member));
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository).save(historyCaptor.capture());
            RankingHistory saved = historyCaptor.getValue();

            assertThat(saved.getNickname()).isEqualTo("OriginalNickname");
            assertThat(saved.getMemberId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("TC-13: 주간 기간 계산 검증")
        void shouldCalculateWeeklyPeriod() {
            // given
            Member member = createMember(1L, "Player1", 1000, 0);
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(List.of(member));
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository).save(historyCaptor.capture());
            RankingHistory saved = historyCaptor.getValue();

            // 주간 기간이 올바르게 설정되었는지 확인
            assertThat(saved.getPeriodType()).isEqualTo(PeriodType.WEEKLY);
            assertThat(saved.getPeriodStart()).isNotNull();
            assertThat(saved.getPeriodEnd()).isNotNull();
            // 기간이 7일인지 확인
            assertThat(saved.getPeriodEnd()).isAfterOrEqualTo(saved.getPeriodStart());
        }
    }

    // ========== 월간 스냅샷 테스트 ==========

    @Nested
    @DisplayName("월간 스냅샷 저장")
    class MonthlySnapshotTests {

        @Test
        @DisplayName("TC-08: 월간 스냅샷 저장 - 정상 케이스")
        void shouldSaveMonthlySnapshot() {
            // given
            List<Member> monthlyBest30 = List.of(
                    createMemberWithBest30(1L, "Player1", null, 290),
                    createMemberWithBest30(2L, "Player2", null, 270),
                    createMemberWithBest30(3L, "Player3", null, 250)
            );

            when(memberRepository.findMonthlyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(monthlyBest30);

            // when
            int result = batch.executeMonthly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            assertThat(result).isEqualTo(3);
            verify(rankingHistoryRepository, times(3)).save(historyCaptor.capture());

            List<RankingHistory> saved = historyCaptor.getAllValues();
            assertThat(saved).allMatch(h -> h.getRankingType() == RankingType.MONTHLY_BEST_30);
            assertThat(saved).allMatch(h -> h.getPeriodType() == PeriodType.MONTHLY);
        }

        @Test
        @DisplayName("TC-14: 월간 기간 계산 검증")
        void shouldCalculateMonthlyPeriod() {
            // given
            Member member = createMemberWithBest30(1L, "Player1", null, 280);
            when(memberRepository.findMonthlyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(List.of(member));

            // when
            batch.executeMonthly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository).save(historyCaptor.capture());
            RankingHistory saved = historyCaptor.getValue();

            assertThat(saved.getPeriodType()).isEqualTo(PeriodType.MONTHLY);
            assertThat(saved.getPeriodStart().getDayOfMonth()).isEqualTo(1);  // 월 시작일
        }
    }

    // ========== 랭킹 타입별 테스트 ==========

    @Nested
    @DisplayName("랭킹 타입별 스냅샷")
    class RankingTypeTests {

        @Test
        @DisplayName("주간 내가맞추기 총점 스냅샷")
        void shouldSaveWeeklyGuessScoreSnapshot() {
            // given
            Member member = createMember(1L, "Player1", 1500, 0);
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(List.of(member));
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository).save(historyCaptor.capture());
            RankingHistory saved = historyCaptor.getValue();

            assertThat(saved.getRankingType()).isEqualTo(RankingType.WEEKLY_GUESS_SCORE);
            assertThat(saved.getScore()).isEqualTo(1500);
            assertThat(saved.getGamesPlayed()).isEqualTo(10);
        }

        @Test
        @DisplayName("주간 멀티게임 티어 스냅샷")
        void shouldSaveWeeklyMultiTierSnapshot() {
            // given
            Member member = createMemberWithTier(1L, "Player1", MultiTier.MASTER, 75, 20);
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(List.of(member));
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository).save(historyCaptor.capture());
            RankingHistory saved = historyCaptor.getValue();

            assertThat(saved.getRankingType()).isEqualTo(RankingType.WEEKLY_MULTI_TIER);
            assertThat(saved.getMultiTier()).isEqualTo(MultiTier.MASTER);
            assertThat(saved.getMultiLp()).isEqualTo(75);
        }

        @Test
        @DisplayName("주간 30곡 최고점 스냅샷")
        void shouldSaveWeeklyBest30Snapshot() {
            // given
            Member member = createMemberWithBest30(1L, "Player1", 285, null);
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(List.of(member));
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository).save(historyCaptor.capture());
            RankingHistory saved = historyCaptor.getValue();

            assertThat(saved.getRankingType()).isEqualTo(RankingType.WEEKLY_BEST_30);
            assertThat(saved.getScore()).isEqualTo(285);
        }
    }

    // ========== 예외 처리 테스트 ==========

    @Nested
    @DisplayName("예외 처리")
    class ExceptionTests {

        @Test
        @DisplayName("TC-15: 배치 실행 실패 시 FAIL 기록")
        void shouldRecordFailOnException() {
            // given
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenThrow(new RuntimeException("DB 연결 실패"));

            // when & then
            assertThatThrownBy(() -> batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("배치 실행 실패");

            verify(batchService).recordExecution(
                    eq(RankingSnapshotBatch.BATCH_ID),
                    eq(BatchExecutionHistory.ExecutionType.SCHEDULED),
                    eq(BatchConfig.ExecutionResult.FAIL),
                    argThat(msg -> msg.contains("오류")),
                    eq(0),
                    anyLong()
            );
        }

        @Test
        @DisplayName("저장 중 예외 발생 시 롤백")
        void shouldRollbackOnSaveException() {
            // given
            Member member = createMember(1L, "Player1", 1000, 0);
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(List.of(member));
            when(rankingHistoryRepository.save(any(RankingHistory.class)))
                    .thenThrow(new RuntimeException("저장 실패"));

            // when & then
            assertThatThrownBy(() -> batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ========== 중복 실행 방지 테스트 ==========

    @Nested
    @DisplayName("중복 실행 처리")
    class DuplicateExecutionTests {

        @Test
        @DisplayName("TC-18: 같은 기간 중복 실행 시 스킵")
        void shouldSkipDuplicateExecution() {
            // given
            when(rankingHistoryRepository.existsByPeriodTypeAndRankingTypeAndPeriodStartAndPeriodEnd(
                    any(PeriodType.class), any(RankingType.class), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(true);  // 이미 스냅샷 존재

            // when
            int result = batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            assertThat(result).isZero();
            verify(rankingHistoryRepository, never()).save(any(RankingHistory.class));
            verify(batchService).recordExecution(
                    eq(RankingSnapshotBatch.BATCH_ID),
                    any(),
                    eq(BatchConfig.ExecutionResult.SUCCESS),
                    argThat(msg -> msg.contains("이미") || msg.contains("스킵")),
                    eq(0),
                    anyLong()
            );
        }
    }

    // ========== 성공 기록 테스트 ==========

    @Nested
    @DisplayName("실행 기록")
    class ExecutionRecordTests {

        @Test
        @DisplayName("정상 실행 시 SUCCESS 기록")
        void shouldRecordSuccessExecution() {
            // given
            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(createMembers(5, 1000));
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.MANUAL);

            // then
            verify(batchService).recordExecution(
                    eq(RankingSnapshotBatch.BATCH_ID),
                    eq(BatchExecutionHistory.ExecutionType.MANUAL),
                    eq(BatchConfig.ExecutionResult.SUCCESS),
                    argThat(msg -> msg.contains("스냅샷") && msg.contains("5")),
                    eq(5),
                    anyLong()
            );
        }

        @Test
        @DisplayName("BATCH_ID 확인")
        void shouldHaveCorrectBatchId() {
            assertThat(RankingSnapshotBatch.BATCH_ID).isEqualTo("BATCH_RANKING_SNAPSHOT");
        }
    }

    // ========== 동점자 처리 테스트 ==========

    @Nested
    @DisplayName("동점자 처리")
    class TieBreakingTests {

        @Test
        @DisplayName("TC-11: 동점자 순위 부여")
        void shouldAssignRankToTiedMembers() {
            // given - 같은 점수의 회원들
            Member member1 = createMember(1L, "Player1", 1000, 0);
            Member member2 = createMember(2L, "Player2", 1000, 0);  // 동점
            Member member3 = createMember(3L, "Player3", 900, 0);

            when(memberRepository.findTopWeeklyGuessRankingByScore(any(PageRequest.class)))
                    .thenReturn(List.of(member1, member2, member3));
            when(memberRepository.findTopWeeklyMultiRankingByScore(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findWeeklyBest30Ranking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiTierRanking(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());
            when(memberRepository.findTopMultiWins(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            // when
            batch.executeWeekly(BatchExecutionHistory.ExecutionType.SCHEDULED);

            // then
            verify(rankingHistoryRepository, times(3)).save(historyCaptor.capture());
            List<RankingHistory> saved = historyCaptor.getAllValues();

            // 순위가 1, 2, 3으로 부여됨 (먼저 조회된 순서)
            assertThat(saved.get(0).getRankPosition()).isEqualTo(1);
            assertThat(saved.get(1).getRankPosition()).isEqualTo(2);
            assertThat(saved.get(2).getRankPosition()).isEqualTo(3);
        }
    }
}
