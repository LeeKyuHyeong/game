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

import java.time.LocalDate;
import java.util.List;

/**
 * 연속 로그인 스트릭 리셋 배치
 * 어제 로그인하지 않은 회원의 연속 로그인 스트릭을 리셋합니다.
 * - 대상: lastLoginDate가 어제가 아닌 회원 중 loginStreak > 0인 회원
 * - 실행 주기: 매일 00:30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginStreakBatch {

    private final MemberRepository memberRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_LOGIN_STREAK";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int resetCount = 0;
        int maxStreakBroken = 0;  // 10일 이상 스트릭이 끊긴 경우
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 연속 로그인 스트릭 리셋", BATCH_ID);

            // 어제 날짜 (오늘 기준 어제 로그인 안 했으면 스트릭 끊김)
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<Member> membersToReset = memberRepository.findMembersToResetLoginStreak(yesterday);

            log.info("스트릭 리셋 대상 회원 수: {}명", membersToReset.size());

            for (Member member : membersToReset) {
                try {
                    int previousStreak = member.getLoginStreak() != null ? member.getLoginStreak() : 0;

                    if (previousStreak >= 10) {
                        maxStreakBroken++;
                        log.info("회원 {} 연속 로그인 {}일 스트릭 끊김", member.getNickname(), previousStreak);
                    }

                    member.resetLoginStreak();
                    resetCount++;

                    log.debug("회원 {} 스트릭 리셋: {} -> 0", member.getNickname(), previousStreak);

                } catch (Exception e) {
                    log.warn("회원 {} 스트릭 리셋 중 오류: {}", member.getId(), e.getMessage());
                }
            }

            resultMessage.append(String.format(
                    "연속 로그인 스트릭 리셋 완료. 대상: %d명, 리셋: %d명, 10일+ 끊김: %d명",
                    membersToReset.size(), resetCount, maxStreakBroken
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    resetCount,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 리셋: {}명, 10일+ 끊김: {}명, 소요시간: {}ms",
                    BATCH_ID, resetCount, maxStreakBroken, executionTime);

            return resetCount;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    resetCount,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
