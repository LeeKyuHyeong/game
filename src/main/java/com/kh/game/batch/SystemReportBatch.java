package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.GameRoom;
import com.kh.game.entity.Member;
import com.kh.game.repository.*;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemReportBatch {

    private final MemberRepository memberRepository;
    private final SongRepository songRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomChatRepository chatRepository;
    private final MemberLoginHistoryRepository loginHistoryRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_SYSTEM_REPORT";

    @Transactional(readOnly = true)
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 회원 통계
            long totalMembers = memberRepository.count();
            long activeMembers = memberRepository.findAll().stream()
                    .filter(m -> m.getStatus() == Member.MemberStatus.ACTIVE)
                    .count();

            // 노래 통계
            long totalSongs = songRepository.count();
            long activeSongs = songRepository.findByUseYn("Y").size();

            // 방 통계
            long totalRooms = gameRoomRepository.count();
            long activeRooms = gameRoomRepository.countActiveRooms();
            long waitingRooms = gameRoomRepository.countByStatus(GameRoom.RoomStatus.WAITING);
            long playingRooms = gameRoomRepository.countByStatus(GameRoom.RoomStatus.PLAYING);

            // 채팅 통계
            long totalChats = chatRepository.count();
            long todayChats = chatRepository.countTodayChats();

            // 로그인 통계 (최근 24시간)
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            long recentLogins = loginHistoryRepository.findAll().stream()
                    .filter(h -> h.getCreatedAt() != null && h.getCreatedAt().isAfter(yesterday))
                    .count();

            // 디스크 공간 (uploads 폴더)
            File uploadsDir = new File("uploads/songs");
            long usedSpace = 0;
            if (uploadsDir.exists()) {
                usedSpace = getFolderSize(uploadsDir);
            }

            // 시스템 메모리
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            long maxMemory = runtime.maxMemory() / (1024 * 1024);

            // 결과 메시지 생성
            resultMessage.append(String.format(
                    "회원: %d/%d(활성), 노래: %d/%d(활성), 방: %d(대기:%d,게임:%d), 채팅: %d(오늘:%d), 메모리: %dMB/%dMB",
                    activeMembers, totalMembers,
                    activeSongs, totalSongs,
                    activeRooms, waitingRooms, playingRooms,
                    totalChats, todayChats,
                    usedMemory, maxMemory
            ));

            // 상세 로그
            log.info("===== 시스템 상태 리포트 =====");
            log.info("회원: 전체 {}, 활성 {}", totalMembers, activeMembers);
            log.info("노래: 전체 {}, 활성 {}", totalSongs, activeSongs);
            log.info("방: 전체 {}, 대기 {}, 게임중 {}", totalRooms, waitingRooms, playingRooms);
            log.info("채팅: 전체 {}, 오늘 {}", totalChats, todayChats);
            log.info("로그인(24h): {}", recentLogins);
            log.info("업로드 폴더: {}MB", usedSpace / (1024 * 1024));
            log.info("메모리: {}MB / {}MB", usedMemory, maxMemory);
            log.info("==============================");

            totalAffected = 1;

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

    private long getFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getFolderSize(file);
                }
            }
        }
        return size;
    }
}
