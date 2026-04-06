package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.DailyStats;
import com.kh.game.entity.GameRoom;
import com.kh.game.repository.*;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStatsBatch {

    private final DailyStatsRepository dailyStatsRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomChatRepository chatRepository;
    private final MemberRepository memberRepository;
    private final MemberLoginHistoryRepository loginHistoryRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_DAILY_STATS";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 전일 통계 집계
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startOfDay = yesterday.atStartOfDay();
            LocalDateTime endOfDay = yesterday.plusDays(1).atStartOfDay();

            // 이미 집계된 데이터가 있는지 확인
            DailyStats stats = dailyStatsRepository.findByStatDate(yesterday)
                    .orElse(new DailyStats(yesterday));

            // 게임 방 통계 (DB 레벨 카운트)
            long totalRooms = gameRoomRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            stats.setTotalRooms((int) totalRooms);

            // 종료된 게임 수 (DB 레벨 카운트)
            long finishedGames = gameRoomRepository.countByStatusAndUpdatedAtBetween(
                    GameRoom.RoomStatus.FINISHED, startOfDay, endOfDay);
            stats.setTotalGames((int) finishedGames);

            // 채팅 수 (DB 레벨 카운트)
            long chatCount = chatRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            stats.setTotalChats((int) chatCount);

            // 신규 가입자 수 (DB 레벨 카운트)
            long newMembers = memberRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            stats.setNewMembers((int) newMembers);

            // 활성 사용자 - 전일 로그인 성공한 고유 회원 수 (DB 레벨 카운트)
            long activeMembers = loginHistoryRepository.countDistinctActiveMembersBetween(startOfDay, endOfDay);
            stats.setActiveMembers((int) activeMembers);

            dailyStatsRepository.save(stats);
            totalAffected = 1;

            resultMessage.append(String.format(
                    "%s 통계 집계 완료. 게임: %d, 방: %d, 채팅: %d, 신규: %d, 활성: %d",
                    yesterday, stats.getTotalGames(), stats.getTotalRooms(),
                    stats.getTotalChats(), stats.getNewMembers(), stats.getActiveMembers()
            ));

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
