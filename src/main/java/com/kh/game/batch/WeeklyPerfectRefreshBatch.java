package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.FanChallengeRecord;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.service.BatchService;
import com.kh.game.service.SongService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 주간 퍼펙트 갱신 배치
 *
 * 정책:
 * - 1주일마다 실행
 * - 곡 수 변경 시 isCurrentPerfect 무효화 (추가/삭제 모두)
 * - isPerfectClear (달성시점 퍼펙트)는 전곡 삭제 시에만 무효화
 * - 달성시점 + 현재시점 둘 다 표시
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyPerfectRefreshBatch {

    public static final String BATCH_ID = "BATCH_WEEKLY_PERFECT_REFRESH";

    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final SongService songService;
    private final BatchService batchService;

    @Transactional
    public BatchResult execute() {
        return execute(BatchExecutionHistory.ExecutionType.SCHEDULED);
    }

    @Transactional
    public BatchResult execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int processedCount = 0;
        int invalidatedCount = 0;
        int allDeletedCount = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 주간 퍼펙트 갱신 배치 시작", BATCH_ID);

            // 1. 현재 아티스트별 곡 수 조회
            List<Map<String, Object>> artistsWithCount = songService.getArtistsWithCount();
            Map<String, Integer> currentSongCounts = new java.util.HashMap<>();
            for (Map<String, Object> artistInfo : artistsWithCount) {
                String artist = (String) artistInfo.get("name");
                int count = ((Number) artistInfo.get("count")).intValue();
                currentSongCounts.put(artist, count);
            }
            log.info("현재 아티스트 수: {}개", currentSongCounts.size());

            // 2. 모든 퍼펙트 기록 조회 (isPerfectClear 또는 isCurrentPerfect가 true인 것)
            List<FanChallengeRecord> allRecords = fanChallengeRecordRepository.findAll();
            log.info("전체 기록 수: {}개", allRecords.size());

            // 3. 각 기록 검사 및 갱신
            for (FanChallengeRecord record : allRecords) {
                processedCount++;
                String artist = record.getArtist();
                Integer currentCount = currentSongCounts.get(artist);
                Integer recordedTotalSongs = record.getTotalSongs();

                // 케이스 1: 아티스트 곡이 모두 삭제됨
                if (currentCount == null || currentCount == 0) {
                    if (Boolean.TRUE.equals(record.getIsPerfectClear()) ||
                        Boolean.TRUE.equals(record.getIsCurrentPerfect())) {

                        log.debug("아티스트 곡 없음 - 퍼펙트 완전 무효화: {} (회원: {})",
                                artist, record.getMember().getNickname());
                        record.setIsPerfectClear(false);
                        record.setIsCurrentPerfect(false);
                        record.setLastCheckedAt(LocalDateTime.now());
                        fanChallengeRecordRepository.save(record);
                        invalidatedCount++;
                        allDeletedCount++;
                    }
                    continue;
                }

                // 케이스 2: 곡 수가 변경됨 (추가 또는 삭제)
                if (!currentCount.equals(recordedTotalSongs)) {
                    if (Boolean.TRUE.equals(record.getIsCurrentPerfect())) {
                        log.debug("곡 수 변경으로 현재시점 퍼펙트 무효화: {} (기존: {}곡, 현재: {}곡, 회원: {})",
                                artist, recordedTotalSongs, currentCount, record.getMember().getNickname());
                        record.setIsCurrentPerfect(false);
                        record.setLastCheckedAt(LocalDateTime.now());
                        fanChallengeRecordRepository.save(record);
                        invalidatedCount++;
                    }
                } else {
                    // 곡 수가 같으면 lastCheckedAt만 갱신
                    record.setLastCheckedAt(LocalDateTime.now());
                    fanChallengeRecordRepository.save(record);
                }
            }

            resultMessage.append(String.format(
                    "주간 퍼펙트 갱신 완료. 처리: %d건, 무효화: %d건 (전곡삭제: %d건)",
                    processedCount, invalidatedCount, allDeletedCount
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    invalidatedCount,
                    executionTime
            );

            log.info("[{}] 배치 완료 - 처리: {}건, 무효화: {}건, 소요시간: {}ms",
                    BATCH_ID, processedCount, invalidatedCount, executionTime);

            return new BatchResult(processedCount, invalidatedCount, allDeletedCount);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    invalidatedCount,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 배치 실행 결과
     */
    @Getter
    public static class BatchResult {
        private final int processedCount;
        private final int invalidatedCount;
        private final int allDeletedCount;

        public BatchResult(int processedCount, int invalidatedCount, int allDeletedCount) {
            this.processedCount = processedCount;
            this.invalidatedCount = invalidatedCount;
            this.allDeletedCount = allDeletedCount;
        }
    }
}
