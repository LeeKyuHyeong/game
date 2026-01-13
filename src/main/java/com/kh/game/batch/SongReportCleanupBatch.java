package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.SongReport;
import com.kh.game.repository.SongReportRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongReportCleanupBatch {

    private final SongReportRepository songReportRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_SONG_REPORT_CLEANUP";
    private static final int RETENTION_DAYS = 90;

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);

            // 1. 해결 신고 삭제: 90일 이상 된 RESOLVED 신고 삭제
            int resolvedCount = songReportRepository.deleteOldReportsByStatus(
                    SongReport.ReportStatus.RESOLVED,
                    threshold
            );
            if (resolvedCount > 0) {
                resultMessage.append(String.format("해결 신고 %d건 삭제. ", resolvedCount));
                log.info("해결 신고 삭제: {}건", resolvedCount);
            }
            totalAffected += resolvedCount;

            // 2. 반려 신고 삭제: 90일 이상 된 REJECTED 신고 삭제
            int rejectedCount = songReportRepository.deleteOldReportsByStatus(
                    SongReport.ReportStatus.REJECTED,
                    threshold
            );
            if (rejectedCount > 0) {
                resultMessage.append(String.format("반려 신고 %d건 삭제.", rejectedCount));
                log.info("반려 신고 삭제: {}건", rejectedCount);
            }
            totalAffected += rejectedCount;

            // PENDING, CONFIRMED 상태는 처리 중인 신고이므로 유지

            if (totalAffected == 0) {
                resultMessage.append("삭제할 신고가 없습니다.");
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

            log.info("[{}] 배치 실행 완료 - 해결: {}건, 반려: {}건, 소요시간: {}ms",
                    BATCH_ID, resolvedCount, rejectedCount, executionTime);

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
