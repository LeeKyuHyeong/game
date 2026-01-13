package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Board;
import com.kh.game.entity.BoardComment;
import com.kh.game.repository.BoardCommentRepository;
import com.kh.game.repository.BoardRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardCleanupBatch {

    private final BoardRepository boardRepository;
    private final BoardCommentRepository commentRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_BOARD_CLEANUP";
    private static final int RETENTION_DAYS = 30;

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);

            // 1. 30일 지난 DELETED 게시글 삭제 (cascade로 댓글/좋아요 함께 삭제)
            int deletedBoards = boardRepository.deleteOldDeletedBoards(
                    Board.BoardStatus.DELETED, threshold);

            // 2. 30일 지난 DELETED 댓글 삭제 (게시글은 살아있지만 댓글만 삭제된 경우)
            int deletedComments = commentRepository.deleteOldDeletedComments(
                    BoardComment.CommentStatus.DELETED, threshold);

            totalAffected = deletedBoards + deletedComments;

            if (totalAffected > 0) {
                if (deletedBoards > 0) {
                    resultMessage.append(String.format("게시글 %d건 삭제. ", deletedBoards));
                }
                if (deletedComments > 0) {
                    resultMessage.append(String.format("댓글 %d건 삭제.", deletedComments));
                }
            } else {
                resultMessage.append("삭제할 데이터가 없습니다.");
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

            log.info("[{}] 배치 실행 완료 - 게시글: {}건, 댓글: {}건, 소요시간: {}ms",
                    BATCH_ID, deletedBoards, deletedComments, executionTime);

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
