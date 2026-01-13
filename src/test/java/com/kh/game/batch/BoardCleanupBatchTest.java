package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Board;
import com.kh.game.entity.BoardComment;
import com.kh.game.repository.BoardCommentRepository;
import com.kh.game.repository.BoardRepository;
import com.kh.game.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BoardCleanupBatch TDD 테스트
 *
 * 정리 대상:
 * - DELETED 상태 게시글 (30일 경과)
 * - DELETED 상태 댓글 (30일 경과)
 */
@ExtendWith(MockitoExtension.class)
class BoardCleanupBatchTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardCommentRepository commentRepository;

    @Mock
    private BatchService batchService;

    private BoardCleanupBatch batch;

    @BeforeEach
    void setUp() {
        batch = new BoardCleanupBatch(boardRepository, commentRepository, batchService);
    }

    @Test
    @DisplayName("30일 지난 DELETED 게시글 삭제")
    void shouldDeleteOldDeletedBoards() {
        // given
        when(boardRepository.deleteOldDeletedBoards(
                eq(Board.BoardStatus.DELETED),
                any(LocalDateTime.class)))
                .thenReturn(5);
        when(commentRepository.deleteOldDeletedComments(
                eq(BoardComment.CommentStatus.DELETED),
                any(LocalDateTime.class)))
                .thenReturn(3);

        // when
        int result = batch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // then
        assertThat(result).isEqualTo(8);  // 게시글 5 + 댓글 3
        verify(boardRepository).deleteOldDeletedBoards(
                eq(Board.BoardStatus.DELETED),
                argThat(threshold -> threshold.isBefore(LocalDateTime.now().minusDays(29))));
    }

    @Test
    @DisplayName("삭제할 데이터 없으면 0 반환")
    void shouldReturnZeroWhenNoDataToDelete() {
        // given
        when(boardRepository.deleteOldDeletedBoards(any(), any())).thenReturn(0);
        when(commentRepository.deleteOldDeletedComments(any(), any())).thenReturn(0);

        // when
        int result = batch.execute(BatchExecutionHistory.ExecutionType.SCHEDULED);

        // then
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("정상 실행 시 SUCCESS 기록")
    void shouldRecordSuccessExecution() {
        // given
        when(boardRepository.deleteOldDeletedBoards(any(), any())).thenReturn(2);
        when(commentRepository.deleteOldDeletedComments(any(), any())).thenReturn(1);

        // when
        batch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // then
        verify(batchService).recordExecution(
                eq(BoardCleanupBatch.BATCH_ID),
                eq(BatchExecutionHistory.ExecutionType.MANUAL),
                eq(BatchConfig.ExecutionResult.SUCCESS),
                argThat(msg -> msg.contains("게시글") && msg.contains("2")),
                eq(3),
                anyLong()
        );
    }

    @Test
    @DisplayName("예외 발생 시 FAIL 기록 후 예외 재발생")
    void shouldRecordFailExecution() {
        // given
        when(boardRepository.deleteOldDeletedBoards(any(), any()))
                .thenThrow(new RuntimeException("DB 오류"));

        // when & then
        assertThatThrownBy(() -> batch.execute(BatchExecutionHistory.ExecutionType.MANUAL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("배치 실행 실패");

        verify(batchService).recordExecution(
                eq(BoardCleanupBatch.BATCH_ID),
                eq(BatchExecutionHistory.ExecutionType.MANUAL),
                eq(BatchConfig.ExecutionResult.FAIL),
                argThat(msg -> msg.contains("오류")),
                eq(0),
                anyLong()
        );
    }

    @Test
    @DisplayName("DELETED 상태로만 삭제 쿼리 호출")
    void shouldOnlyDeleteDeletedStatus() {
        // given
        when(boardRepository.deleteOldDeletedBoards(any(), any())).thenReturn(0);
        when(commentRepository.deleteOldDeletedComments(any(), any())).thenReturn(0);

        // when
        batch.execute(BatchExecutionHistory.ExecutionType.MANUAL);

        // then - DELETED 상태로만 호출됨
        verify(boardRepository).deleteOldDeletedBoards(
                eq(Board.BoardStatus.DELETED),
                any(LocalDateTime.class));
        verify(boardRepository, never()).deleteOldDeletedBoards(
                eq(Board.BoardStatus.ACTIVE),
                any());
        verify(boardRepository, never()).deleteOldDeletedBoards(
                eq(Board.BoardStatus.HIDDEN),
                any());
    }

    @Test
    @DisplayName("게시글과 댓글 모두 삭제")
    void shouldDeleteBothBoardsAndComments() {
        // given
        when(boardRepository.deleteOldDeletedBoards(any(), any())).thenReturn(10);
        when(commentRepository.deleteOldDeletedComments(any(), any())).thenReturn(25);

        // when
        int result = batch.execute(BatchExecutionHistory.ExecutionType.SCHEDULED);

        // then
        assertThat(result).isEqualTo(35);
        verify(boardRepository).deleteOldDeletedBoards(any(), any());
        verify(commentRepository).deleteOldDeletedComments(any(), any());
    }

    @Test
    @DisplayName("삭제할 데이터 없을 때 적절한 메시지 기록")
    void shouldRecordProperMessageWhenNoData() {
        // given
        when(boardRepository.deleteOldDeletedBoards(any(), any())).thenReturn(0);
        when(commentRepository.deleteOldDeletedComments(any(), any())).thenReturn(0);

        // when
        batch.execute(BatchExecutionHistory.ExecutionType.SCHEDULED);

        // then
        verify(batchService).recordExecution(
                eq(BoardCleanupBatch.BATCH_ID),
                eq(BatchExecutionHistory.ExecutionType.SCHEDULED),
                eq(BatchConfig.ExecutionResult.SUCCESS),
                argThat(msg -> msg.contains("삭제할") && msg.contains("없습니다")),
                eq(0),
                anyLong()
        );
    }
}
