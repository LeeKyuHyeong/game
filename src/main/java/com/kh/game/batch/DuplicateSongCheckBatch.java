package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Song;
import com.kh.game.repository.SongRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateSongCheckBatch {

    private final SongRepository songRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_DUPLICATE_SONG_CHECK";

    /**
     * 같은 YouTube video ID를 가진 중복 곡을 찾아
     * 더 높은 ID(나중에 생성된 데이터)를 비활성화합니다.
     */
    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 중복된 YouTube video ID 목록 조회 (2개 이상 있는 것만)
            List<String> duplicateVideoIds = songRepository.findDuplicateYoutubeVideoIds();

            if (duplicateVideoIds.isEmpty()) {
                resultMessage.append("중복된 YouTube ID가 없습니다.");
            } else {
                int deactivatedCount = 0;

                for (String videoId : duplicateVideoIds) {
                    // 해당 YouTube ID를 가진 활성 곡들 조회 (ID 오름차순)
                    List<Song> songs = songRepository.findByYoutubeVideoIdAndUseYnOrderByIdAsc(videoId, "Y");

                    if (songs.size() > 1) {
                        // 첫 번째(가장 낮은 ID)를 제외하고 나머지 비활성화
                        for (int i = 1; i < songs.size(); i++) {
                            Song duplicate = songs.get(i);
                            duplicate.setUseYn("N");
                            deactivatedCount++;
                            log.info("중복 곡 비활성화: ID={}, title={}, artist={}, youtubeId={}",
                                    duplicate.getId(), duplicate.getTitle(), duplicate.getArtist(), videoId);
                        }
                    }
                }

                if (deactivatedCount > 0) {
                    resultMessage.append(String.format("중복 YouTube ID %d개 발견, %d곡 비활성화.",
                            duplicateVideoIds.size(), deactivatedCount));
                } else {
                    resultMessage.append(String.format("중복 YouTube ID %d개 발견, 이미 처리됨.",
                            duplicateVideoIds.size()));
                }
                totalAffected = deactivatedCount;
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

            log.info("[{}] 배치 실행 완료 - 비활성화: {}건, 소요시간: {}ms",
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
