package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Song;
import com.kh.game.repository.SongRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongFileCheckBatch {

    private final SongRepository songRepository;
    private final BatchService batchService;

    @Value("${file.upload-dir:uploads/songs}")
    private String uploadDir;

    public static final String BATCH_ID = "BATCH_SONG_FILE_CHECK";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            List<Song> allSongs = songRepository.findByUseYn("Y");
            List<String> missingFiles = new ArrayList<>();
            int disabledCount = 0;

            for (Song song : allSongs) {
                String filePath = song.getFilePath();
                if (filePath == null || filePath.isEmpty()) {
                    continue;
                }

                // 파일 경로 처리
                Path absolutePath;
                if (filePath.startsWith("/")) {
                    absolutePath = Paths.get(uploadDir, filePath);
                } else {
                    absolutePath = Paths.get(filePath);
                }

                if (!Files.exists(absolutePath)) {
                    missingFiles.add(String.format("%s - %s (%s)",
                            song.getArtist(), song.getTitle(), filePath));
                    // 파일이 없는 노래는 비활성화
                    song.setUseYn("N");
                    disabledCount++;
                    log.warn("파일 누락으로 비활성화: {} - {} ({})",
                            song.getArtist(), song.getTitle(), filePath);
                }
            }

            totalAffected = missingFiles.size();

            if (disabledCount > 0) {
                resultMessage.append(String.format("파일 누락 노래 %d곡 비활성화.", disabledCount));
            }

            if (missingFiles.isEmpty()) {
                resultMessage.append("모든 노래 파일 정상.");
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

            log.info("[{}] 배치 실행 완료 - 누락 파일: {}개, 소요시간: {}ms",
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
