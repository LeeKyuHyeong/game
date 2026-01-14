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
 * 월간 랭킹 리셋 배치
 * 매월 1일 00:00에 실행하여 월간 30곡 최고점 통계를 초기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyRankingResetBatch {

    private final MemberRepository memberRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_MONTHLY_RANKING_RESET";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 월간 랭킹 리셋", BATCH_ID);

            // 활성 회원 조회
            List<Member> activeMembers = memberRepository.findByStatus(Member.MemberStatus.ACTIVE);
            log.info("활성 회원 수: {}명", activeMembers.size());

            for (Member member : activeMembers) {
                boolean hasMonthlyStats = member.getMonthlyBest30Score() != null && member.getMonthlyBest30Score() > 0;

                if (hasMonthlyStats) {
                    // 리셋 전 통계 로깅 (상위 10명만)
                    if (totalAffected < 10) {
                        log.debug("리셋: {} - 월간 30곡 최고점: {}점 (달성: {})",
                                member.getNickname(),
                                member.getMonthlyBest30Score(),
                                member.getMonthlyBest30At());
                    }

                    member.resetMonthlyStats();
                    memberRepository.save(member);
                    totalAffected++;
                }
            }

            resultMessage.append(String.format(
                    "월간 랭킹 리셋 완료. 총 리셋: %d명",
                    totalAffected
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
