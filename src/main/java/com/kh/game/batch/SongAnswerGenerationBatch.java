package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongAnswer;
import com.kh.game.repository.SongAnswerRepository;
import com.kh.game.repository.SongRepository;
import com.kh.game.service.BatchService;
import com.kh.game.util.AnswerGeneratorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 곡 정답 자동 생성 배치
 * SongAnswer가 없는 곡에 대해 정답 변형을 자동으로 생성합니다.
 * - AnswerGeneratorUtil을 활용하여 원본, 괄호제거, 특수문자제거, 한글발음, feat제거 등 생성
 * - 실행 주기: 매일 06:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SongAnswerGenerationBatch {

    private final SongRepository songRepository;
    private final SongAnswerRepository songAnswerRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_SONG_ANSWER_GENERATION";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int processedSongs = 0;
        int totalAnswersCreated = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 정답 미생성 곡에 정답 변형 자동 생성", BATCH_ID);

            // 활성화된 곡 중 SongAnswer가 없는 곡 조회
            List<Song> songsWithoutAnswers = songRepository.findAll().stream()
                    .filter(song -> "Y".equals(song.getUseYn()))
                    .filter(song -> !songAnswerRepository.existsBySongId(song.getId()))
                    .toList();

            log.info("정답 미생성 곡 수: {}개", songsWithoutAnswers.size());

            for (Song song : songsWithoutAnswers) {
                try {
                    Set<String> variants = AnswerGeneratorUtil.generateAnswerVariants(song.getTitle());

                    if (variants.isEmpty()) {
                        log.warn("곡 {} (ID: {}) 정답 변형 생성 실패 - 제목이 비어있음", song.getTitle(), song.getId());
                        continue;
                    }

                    boolean isFirst = true;
                    int answersForSong = 0;

                    for (String variant : variants) {
                        SongAnswer answer = new SongAnswer();
                        answer.setSong(song);
                        answer.setAnswer(variant);
                        answer.setIsPrimary(isFirst);  // 첫 번째만 primary
                        songAnswerRepository.save(answer);

                        isFirst = false;
                        answersForSong++;
                        totalAnswersCreated++;
                    }

                    processedSongs++;
                    log.debug("곡 '{}' (ID: {}) 정답 {}개 생성: {}",
                            song.getTitle(), song.getId(), answersForSong, variants);

                } catch (Exception e) {
                    log.warn("곡 {} (ID: {}) 정답 생성 중 오류: {}",
                            song.getTitle(), song.getId(), e.getMessage());
                }
            }

            resultMessage.append(String.format(
                    "정답 자동 생성 완료. 처리 곡: %d개, 생성 정답: %d개",
                    processedSongs, totalAnswersCreated
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    totalAnswersCreated,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 처리 곡: {}개, 생성 정답: {}개, 소요시간: {}ms",
                    BATCH_ID, processedSongs, totalAnswersCreated, executionTime);

            return totalAnswersCreated;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    totalAnswersCreated,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
