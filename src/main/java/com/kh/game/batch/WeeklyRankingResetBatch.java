package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주간 랭킹 리셋 배치
 * 매주 월요일 06:00에 실행하여 주간 통계를 초기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyRankingResetBatch {

    private final MemberRepository memberRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_WEEKLY_RANKING_RESET";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 주간 랭킹 리셋", BATCH_ID);

            // 활성 회원 조회
            List<Member> activeMembers = memberRepository.findByStatus(Member.MemberStatus.ACTIVE);
            log.info("활성 회원 수: {}명", activeMembers.size());

            int guessResetCount = 0;
            int multiResetCount = 0;

            for (Member member : activeMembers) {
                boolean hasGuessStats = member.getWeeklyGuessGames() != null && member.getWeeklyGuessGames() > 0;
                boolean hasMultiStats = member.getWeeklyMultiGames() != null && member.getWeeklyMultiGames() > 0;

                if (hasGuessStats || hasMultiStats) {
                    // 리셋 전 통계 로깅 (상위 10명만)
                    if (totalAffected < 10) {
                        log.debug("리셋: {} - 주간 guess: {}점/{}게임, multi: {}점/{}게임",
                                member.getNickname(),
                                member.getWeeklyGuessScore(), member.getWeeklyGuessGames(),
                                member.getWeeklyMultiScore(), member.getWeeklyMultiGames());
                    }

                    if (hasGuessStats) guessResetCount++;
                    if (hasMultiStats) multiResetCount++;

                    member.resetWeeklyStats();
                    memberRepository.save(member);
                    totalAffected++;
                }
            }

            resultMessage.append(String.format(
                    "주간 랭킹 리셋 완료. 내가맞추기: %d명, 멀티게임: %d명, 총 리셋: %d명",
                    guessResetCount, multiResetCount, totalAffected
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    totalAffected,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - {}명 리셋, 소요시간: {}ms", BATCH_ID, totalAffected, executionTime);

            return totalAffected;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    totalAffected,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
