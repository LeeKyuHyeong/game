package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.DailyStats;
import com.kh.game.entity.GameRoom;
import com.kh.game.entity.Member;
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

            // 게임 방 통계
            long totalRooms = gameRoomRepository.findAll().stream()
                    .filter(r -> r.getCreatedAt() != null &&
                            !r.getCreatedAt().isBefore(startOfDay) &&
                            r.getCreatedAt().isBefore(endOfDay))
                    .count();
            stats.setTotalRooms((int) totalRooms);

            // 종료된 게임 수
            long finishedGames = gameRoomRepository.findByStatus(GameRoom.RoomStatus.FINISHED).stream()
                    .filter(r -> r.getUpdatedAt() != null &&
                            !r.getUpdatedAt().isBefore(startOfDay) &&
                            r.getUpdatedAt().isBefore(endOfDay))
                    .count();
            stats.setTotalGames((int) finishedGames);

            // 채팅 수 (전일)
            long chatCount = chatRepository.findAll().stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            !c.getCreatedAt().isBefore(startOfDay) &&
                            c.getCreatedAt().isBefore(endOfDay))
                    .count();
            stats.setTotalChats((int) chatCount);

            // 신규 가입자 수
            long newMembers = memberRepository.findAll().stream()
                    .filter(m -> m.getCreatedAt() != null &&
                            !m.getCreatedAt().isBefore(startOfDay) &&
                            m.getCreatedAt().isBefore(endOfDay))
                    .count();
            stats.setNewMembers((int) newMembers);

            // 활성 사용자 (전일 로그인)
            long activeMembers = loginHistoryRepository.findAll().stream()
                    .filter(h -> h.getCreatedAt() != null &&
                            !h.getCreatedAt().isBefore(startOfDay) &&
                            h.getCreatedAt().isBefore(endOfDay))
                    .filter(h -> h.getResult() == com.kh.game.entity.MemberLoginHistory.LoginResult.SUCCESS)
                    .map(h -> h.getMember() != null ? h.getMember().getId() : null)
                    .distinct()
                    .count();
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
