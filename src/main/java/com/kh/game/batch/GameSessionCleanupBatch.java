package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.GameSession;
import com.kh.game.repository.GameSessionRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionCleanupBatch {

    private final GameSessionRepository gameSessionRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_GAME_SESSION_CLEANUP";
    private static final int ZOMBIE_HOURS = 24;
    private static final int RETENTION_DAYS = 7;

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 1. 좀비 세션 처리: 24시간 이상 PLAYING 상태 → ABANDONED 변경
            LocalDateTime zombieThreshold = LocalDateTime.now().minusHours(ZOMBIE_HOURS);
            int zombieCount = gameSessionRepository.markZombieSessionsAsAbandoned(
                    GameSession.GameStatus.PLAYING,
                    GameSession.GameStatus.ABANDONED,
                    zombieThreshold
            );
            if (zombieCount > 0) {
                resultMessage.append(String.format("좀비 세션 %d건 ABANDONED 처리. ", zombieCount));
                log.info("좀비 세션 처리: {}건", zombieCount);
            }
            totalAffected += zombieCount;

            // 2. 완료 세션 삭제: 7일 이상 된 COMPLETED 세션 삭제 (cascade로 GameRound, GameRoundAttempt 함께 삭제)
            LocalDateTime deleteThreshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int completedCount = gameSessionRepository.deleteOldSessionsByStatus(
                    GameSession.GameStatus.COMPLETED,
                    deleteThreshold
            );
            if (completedCount > 0) {
                resultMessage.append(String.format("완료 세션 %d건 삭제. ", completedCount));
                log.info("완료 세션 삭제: {}건", completedCount);
            }
            totalAffected += completedCount;

            // 3. 포기 세션 삭제: 7일 이상 된 ABANDONED 세션 삭제
            int abandonedCount = gameSessionRepository.deleteOldSessionsByStatus(
                    GameSession.GameStatus.ABANDONED,
                    deleteThreshold
            );
            if (abandonedCount > 0) {
                resultMessage.append(String.format("포기 세션 %d건 삭제.", abandonedCount));
                log.info("포기 세션 삭제: {}건", abandonedCount);
            }
            totalAffected += abandonedCount;

            if (totalAffected == 0) {
                resultMessage.append("정리할 세션이 없습니다.");
            }

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString().trim(),
                    totalAffected,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 좀비: {}건, 완료삭제: {}건, 포기삭제: {}건, 소요시간: {}ms",
                    BATCH_ID, zombieCount, completedCount, abandonedCount, executionTime);

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
