package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Member;
import com.kh.game.entity.MultiTier;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LP Decay 배치
 * 장기간 게임을 하지 않은 유저의 LP를 자동으로 감소시킵니다.
 * - 대상: 30일 이상 멀티게임을 하지 않은 활성 회원
 * - 감소량: 7 LP (브론즈 0 LP는 보호)
 * - 실행 주기: 매주 월요일 05:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LpDecayBatch {

    private final MemberRepository memberRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_LP_DECAY";

    // 설정값
    private static final int INACTIVE_DAYS_THRESHOLD = 30;  // 30일 이상 미접속
    private static final int LP_DECAY_AMOUNT = 7;           // 감소 LP

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int decayedCount = 0;
        int demotedCount = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - {}일 이상 미접속 회원 LP 감소", BATCH_ID, INACTIVE_DAYS_THRESHOLD);

            LocalDateTime threshold = LocalDateTime.now().minusDays(INACTIVE_DAYS_THRESHOLD);
            List<Member> inactiveMembers = memberRepository.findMembersForLpDecay(threshold);

            log.info("LP Decay 대상 회원 수: {}명", inactiveMembers.size());

            for (Member member : inactiveMembers) {
                try {
                    // 브론즈 0 LP는 건너뜀
                    if (member.getMultiTier() == MultiTier.BRONZE &&
                        (member.getMultiLp() == null || member.getMultiLp() == 0)) {
                        continue;
                    }

                    int beforeLp = member.getMultiLp() != null ? member.getMultiLp() : 0;
                    MultiTier beforeTier = member.getMultiTier();

                    String tierChange = member.applyLpDecay(LP_DECAY_AMOUNT);
                    decayedCount++;

                    if ("DEMOTED".equals(tierChange)) {
                        demotedCount++;
                        log.info("회원 {} LP Decay: {} {} LP -> {} {} LP (강등)",
                                member.getNickname(),
                                beforeTier.getDisplayName(), beforeLp,
                                member.getMultiTier().getDisplayName(), member.getMultiLp());
                    } else {
                        log.debug("회원 {} LP Decay: {} -> {} LP",
                                member.getNickname(), beforeLp, member.getMultiLp());
                    }

                } catch (Exception e) {
                    log.warn("회원 {} LP Decay 처리 중 오류: {}", member.getId(), e.getMessage());
                }
            }

            resultMessage.append(String.format(
                    "LP Decay 완료. 대상: %d명, 처리: %d명, 강등: %d명 (-%d LP/인)",
                    inactiveMembers.size(), decayedCount, demotedCount, LP_DECAY_AMOUNT
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    decayedCount,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 처리: {}명, 강등: {}명, 소요시간: {}ms",
                    BATCH_ID, decayedCount, demotedCount, executionTime);

            return decayedCount;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    decayedCount,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
