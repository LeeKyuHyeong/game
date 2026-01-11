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
public class SongAnalyticsBatch {

    private final SongRepository songRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_SONG_ANALYTICS";

    @Transactional(readOnly = true)
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 활성 노래 통계 분석
            List<Song> activeSongs = songRepository.findByUseYn("Y");
            int totalSongs = activeSongs.size();

            // 파일이 있는 노래
            long songsWithFile = activeSongs.stream()
                    .filter(s -> s.getFilePath() != null && !s.getFilePath().isEmpty())
                    .count();

            // 장르별 통계
            long genreCount = activeSongs.stream()
                    .filter(s -> s.getGenre() != null)
                    .map(s -> s.getGenre().getId())
                    .distinct()
                    .count();

            // 연도별 통계
            long yearCount = activeSongs.stream()
                    .filter(s -> s.getReleaseYear() != null)
                    .map(Song::getReleaseYear)
                    .distinct()
                    .count();

            totalAffected = totalSongs;

            resultMessage.append(String.format(
                    "노래 분석 완료. 전체: %d곡, 파일있음: %d곡, 장르: %d개, 연도: %d개",
                    totalSongs, songsWithFile, genreCount, yearCount
            ));

            log.info("노래 분석 결과: 전체 {}곡, 파일 {}곡, 장르 {}개",
                    totalSongs, songsWithFile, genreCount);

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString().trim(),
                    totalAffected,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 소요시간: {}ms", BATCH_ID, executionTime);

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
