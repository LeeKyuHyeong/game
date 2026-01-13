package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.repository.GameRoundAttemptRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoundAttemptCleanupBatch {

    private final GameRoundAttemptRepository gameRoundAttemptRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_GAME_ROUND_ATTEMPT_CLEANUP";

    /**
     * 30일이 지난 게임 라운드 시도 기록을 삭제합니다.
     * 통계 분석에는 집계된 데이터를 사용하므로 원본 시도 기록은 일정 기간 후 정리합니다.
     */
    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 30일 이전 데이터 삭제
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            int deletedCount = gameRoundAttemptRepository.deleteByCreatedAtBefore(threshold);

            if (deletedCount > 0) {
                resultMessage.append(String.format("30일 지난 시도 기록 %d건 삭제.", deletedCount));
            } else {
                resultMessage.append("정리할 시도 기록이 없습니다.");
            }
            totalAffected = deletedCount;

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString().trim(),
                    totalAffected,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 삭제: {}건, 소요시간: {}ms",
                    BATCH_ID, totalAffected, executionTime);

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
