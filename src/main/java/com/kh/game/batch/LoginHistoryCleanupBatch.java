package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.MemberLoginHistory;
import com.kh.game.repository.MemberLoginHistoryRepository;
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
public class LoginHistoryCleanupBatch {

    private final MemberLoginHistoryRepository loginHistoryRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_LOGIN_HISTORY_CLEANUP";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 90일이 지난 로그인 이력 삭제
            LocalDateTime threshold = LocalDateTime.now().minusDays(90);
            List<MemberLoginHistory> oldHistories = loginHistoryRepository.findAll().stream()
                    .filter(h -> h.getCreatedAt() != null && h.getCreatedAt().isBefore(threshold))
                    .toList();

            if (!oldHistories.isEmpty()) {
                loginHistoryRepository.deleteAll(oldHistories);
                totalAffected = oldHistories.size();
                resultMessage.append(String.format("90일 지난 로그인 이력 %d건 삭제.", totalAffected));
            } else {
                resultMessage.append("삭제할 로그인 이력이 없습니다.");
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
