package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.GameRoom;
import com.kh.game.repository.GameRoomChatRepository;
import com.kh.game.repository.GameRoomRepository;
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
public class SessionCleanupBatch {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomChatRepository chatRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_SESSION_CLEANUP";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 1. 2시간 이상 PLAYING 상태인 방 종료
            LocalDateTime playingThreshold = LocalDateTime.now().minusHours(2);
            List<GameRoom> stuckPlayingRooms = gameRoomRepository.findByStatus(GameRoom.RoomStatus.PLAYING);
            int stuckCount = 0;
            for (GameRoom room : stuckPlayingRooms) {
                if (room.getUpdatedAt() != null && room.getUpdatedAt().isBefore(playingThreshold)) {
                    room.setStatus(GameRoom.RoomStatus.FINISHED);
                    stuckCount++;
                    log.debug("PLAYING 상태 방 종료: {} ({})", room.getRoomCode(), room.getRoomName());
                }
            }
            if (stuckCount > 0) {
                resultMessage.append(String.format("오래된 PLAYING 방 %d개 종료. ", stuckCount));
            }
            totalAffected += stuckCount;

            // 2. 24시간 이상 대기 상태인 방 종료
            LocalDateTime waitingThreshold = LocalDateTime.now().minusHours(24);
            List<GameRoom> staleWaitingRooms = gameRoomRepository.findStaleWaitingRooms(waitingThreshold);
            for (GameRoom room : staleWaitingRooms) {
                room.setStatus(GameRoom.RoomStatus.FINISHED);
                log.debug("오래된 대기 방 종료: {} ({})", room.getRoomCode(), room.getRoomName());
            }
            if (!staleWaitingRooms.isEmpty()) {
                resultMessage.append(String.format("오래된 대기 방 %d개 종료. ", staleWaitingRooms.size()));
            }
            totalAffected += staleWaitingRooms.size();

            // 3. 7일 이상 된 FINISHED 방 삭제
            LocalDateTime deleteThreshold = LocalDateTime.now().minusDays(7);
            List<GameRoom> oldFinishedRooms = gameRoomRepository.findByStatus(GameRoom.RoomStatus.FINISHED);
            int deletedCount = 0;
            for (GameRoom room : oldFinishedRooms) {
                if (room.getUpdatedAt() != null && room.getUpdatedAt().isBefore(deleteThreshold)) {
                    chatRepository.deleteByGameRoom(room);
                    gameRoomRepository.delete(room);
                    deletedCount++;
                    log.debug("오래된 방 삭제: {} ({})", room.getRoomCode(), room.getRoomName());
                }
            }
            if (deletedCount > 0) {
                resultMessage.append(String.format("7일 지난 방 %d개 삭제.", deletedCount));
            }
            totalAffected += deletedCount;

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

            log.info("[{}] 배치 실행 완료 - 영향: {}건, 소요시간: {}ms",
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
