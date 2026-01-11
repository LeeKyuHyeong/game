package com.kh.game.service;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.repository.BatchConfigRepository;
import com.kh.game.repository.BatchExecutionHistoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchService {

    private final BatchConfigRepository batchConfigRepository;
    private final BatchExecutionHistoryRepository historyRepository;

    /**
     * 초기 배치 설정 데이터 생성 및 업데이트
     */
    @PostConstruct
    @Transactional
    public void initBatchConfigs() {
        // 기존 데이터가 있으면 implemented 플래그 및 설정 업데이트
        if (batchConfigRepository.count() > 0) {
            updateImplementedFlags();
            updateBatchConfigs();
            return;
        }

        log.info("배치 설정 초기 데이터 생성");

        // 1. 방 정리
        batchConfigRepository.save(new BatchConfig(
                "BATCH_ROOM_CLEANUP",
                "방 정리",
                "종료된 방 및 24시간 이상 대기 상태인 방을 자동 삭제합니다.",
                "0 0 * * * *",
                "매시간",
                "GameRoom",
                BatchConfig.Priority.HIGH,
                true  // 구현됨
        ));

        // 2. 채팅 정리
        batchConfigRepository.save(new BatchConfig(
                "BATCH_CHAT_CLEANUP",
                "채팅 로그 정리",
                "30일이 지난 채팅 로그를 자동 삭제하여 DB 용량을 관리합니다.",
                "0 0 3 * * *",
                "매일 03:00",
                "GameRoomChat",
                BatchConfig.Priority.MEDIUM,
                true  // 구현됨
        ));

        // 3. 통계 집계
        batchConfigRepository.save(new BatchConfig(
                "BATCH_DAILY_STATS",
                "일일 통계 집계",
                "전일 게임 수, 참여자 수, 정답률 등 통계를 집계합니다.",
                "0 0 4 * * *",
                "매일 04:00",
                "DailyStats",
                BatchConfig.Priority.HIGH,
                true  // 구현됨
        ));

        // 4. 랭킹 갱신
        batchConfigRepository.save(new BatchConfig(
                "BATCH_RANKING_UPDATE",
                "랭킹 스냅샷 갱신",
                "주간/월간 랭킹 스냅샷을 생성하여 랭킹 조회 성능을 개선합니다.",
                "0 0 * * * *",
                "매시간",
                "MemberRanking",
                BatchConfig.Priority.MEDIUM,
                true  // 구현됨
        ));

        // 5. 로그인 이력 정리
        batchConfigRepository.save(new BatchConfig(
                "BATCH_LOGIN_HISTORY_CLEANUP",
                "로그인 이력 정리",
                "90일이 지난 로그인 이력을 삭제합니다.",
                "0 0 5 * * SUN",
                "매주 일요일 05:00",
                "MemberLoginHistory",
                BatchConfig.Priority.LOW,
                true  // 구현됨
        ));

        // 6. 비활성 회원 처리
        batchConfigRepository.save(new BatchConfig(
                "BATCH_INACTIVE_MEMBER",
                "비활성 회원 처리",
                "6개월 이상 미접속 회원을 휴면 상태로 전환합니다.",
                "0 0 6 1 * *",
                "매월 1일 06:00",
                "Member",
                BatchConfig.Priority.LOW,
                true  // 구현됨
        ));

        // 7. 노래 파일 정합성 검사 (MP3 유효성 검증 포함)
        batchConfigRepository.save(new BatchConfig(
                "BATCH_SONG_FILE_CHECK",
                "MP3 파일 유효성 검사",
                "MP3 파일 존재 및 형식 유효성을 검사하고, 문제 있는 노래를 비활성화합니다.",
                "0 0 * * * *",
                "매시간",
                "Song",
                BatchConfig.Priority.HIGH,
                true  // 구현됨
        ));

        // 8. 게임 세션 정리 (구현됨)
        batchConfigRepository.save(new BatchConfig(
                "BATCH_SESSION_CLEANUP",
                "게임 세션 정리",
                "비정상 종료된 게임 세션과 오래된 PLAYING 상태 방을 정리합니다.",
                "0 30 * * * *",
                "매시간 30분",
                "GameSession",
                BatchConfig.Priority.MEDIUM,
                true  // 구현됨
        ));

        // 9. 인기 노래 분석
        batchConfigRepository.save(new BatchConfig(
                "BATCH_SONG_ANALYTICS",
                "인기 노래 분석",
                "주간 노래별 출제 횟수, 정답률, 스킵률을 분석합니다.",
                "0 0 7 * * MON",
                "매주 월요일 07:00",
                "SongAnalytics",
                BatchConfig.Priority.LOW,
                true  // 구현됨
        ));

        // 10. 시스템 상태 리포트
        batchConfigRepository.save(new BatchConfig(
                "BATCH_SYSTEM_REPORT",
                "시스템 상태 리포트",
                "DB 용량, 활성 사용자 수, 서버 상태 등 일일 리포트를 생성합니다.",
                "0 0 8 * * *",
                "매일 08:00",
                "SystemReport",
                BatchConfig.Priority.MEDIUM,
                true  // 구현됨
        ));

        log.info("배치 설정 초기 데이터 생성 완료: 10개");
    }

    /**
     * 모든 배치의 implemented 플래그를 true로 업데이트
     * (모든 배치가 구현되었으므로)
     */
    @Transactional
    public void updateImplementedFlags() {
        // 구현된 배치 ID 목록
        java.util.Set<String> implementedBatchIds = java.util.Set.of(
                "BATCH_SESSION_CLEANUP",
                "BATCH_ROOM_CLEANUP",
                "BATCH_CHAT_CLEANUP",
                "BATCH_DAILY_STATS",
                "BATCH_RANKING_UPDATE",
                "BATCH_LOGIN_HISTORY_CLEANUP",
                "BATCH_INACTIVE_MEMBER",
                "BATCH_SONG_FILE_CHECK",
                "BATCH_SONG_ANALYTICS",
                "BATCH_SYSTEM_REPORT"
        );

        int updatedCount = 0;
        for (BatchConfig config : batchConfigRepository.findAll()) {
            if (implementedBatchIds.contains(config.getBatchId()) && !config.getImplemented()) {
                config.setImplemented(true);
                updatedCount++;
                log.info("배치 구현 상태 업데이트: {} -> implemented=true", config.getBatchId());
            }
        }

        if (updatedCount > 0) {
            log.info("배치 구현 상태 업데이트 완료: {}개", updatedCount);
        }
    }

    /**
     * 배치 설정 업데이트 (기존 데이터 마이그레이션)
     */
    @Transactional
    public void updateBatchConfigs() {
        // BATCH_SONG_FILE_CHECK: 매일 02:00 -> 매시간으로 변경
        batchConfigRepository.findById("BATCH_SONG_FILE_CHECK").ifPresent(config -> {
            boolean updated = false;

            // 이름 업데이트
            if (!"MP3 파일 유효성 검사".equals(config.getName())) {
                config.setName("MP3 파일 유효성 검사");
                updated = true;
            }

            // 설명 업데이트
            String newDesc = "MP3 파일 존재 및 형식 유효성을 검사하고, 문제 있는 노래를 비활성화합니다.";
            if (!newDesc.equals(config.getDescription())) {
                config.setDescription(newDesc);
                updated = true;
            }

            // cron 표현식 업데이트 (매시간)
            if (!"0 0 * * * *".equals(config.getCronExpression())) {
                config.setCronExpression("0 0 * * * *");
                config.setScheduleText("매시간");
                updated = true;
            }

            if (updated) {
                log.info("BATCH_SONG_FILE_CHECK 설정 업데이트 완료 (매시간 실행)");
            }
        });
    }

    /**
     * 전체 배치 목록 조회
     */
    public List<BatchConfig> findAll() {
        return batchConfigRepository.findAllByOrderByPriorityAscNameAsc();
    }

    /**
     * 배치 조회
     */
    public Optional<BatchConfig> findById(String batchId) {
        return batchConfigRepository.findById(batchId);
    }

    /**
     * 활성화된 & 구현된 배치 목록
     */
    public List<BatchConfig> findEnabledAndImplemented() {
        return batchConfigRepository.findByImplementedTrueAndEnabledTrue();
    }

    /**
     * 배치 설정 업데이트
     */
    @Transactional
    public void updateConfig(String batchId, String cronExpression, String scheduleText, Boolean enabled) {
        BatchConfig config = batchConfigRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("배치를 찾을 수 없습니다: " + batchId));

        if (cronExpression != null && !cronExpression.isEmpty()) {
            config.setCronExpression(cronExpression);
        }
        if (scheduleText != null) {
            config.setScheduleText(scheduleText);
        }
        if (enabled != null) {
            config.setEnabled(enabled);
        }

        log.info("배치 설정 업데이트: {} - cron={}, enabled={}", batchId, cronExpression, enabled);
    }

    /**
     * 배치 활성화/비활성화 토글
     */
    @Transactional
    public boolean toggleEnabled(String batchId) {
        BatchConfig config = batchConfigRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("배치를 찾을 수 없습니다: " + batchId));

        config.setEnabled(!config.getEnabled());
        log.info("배치 활성화 토글: {} -> {}", batchId, config.getEnabled());
        return config.getEnabled();
    }

    /**
     * 실행 결과 기록
     */
    @Transactional
    public void recordExecution(String batchId, BatchExecutionHistory.ExecutionType executionType,
                                BatchConfig.ExecutionResult result, String message,
                                int affectedCount, long executionTimeMs) {
        // BatchConfig 업데이트
        batchConfigRepository.findById(batchId).ifPresent(config -> {
            config.recordExecution(result, message, affectedCount, executionTimeMs);
        });

        // 실행 이력 저장
        BatchConfig config = batchConfigRepository.findById(batchId).orElse(null);
        String batchName = config != null ? config.getName() : batchId;

        BatchExecutionHistory history = new BatchExecutionHistory(batchId, batchName, executionType);
        history.complete(result, message, affectedCount, executionTimeMs);
        historyRepository.save(history);

        log.info("배치 실행 기록: {} - result={}, affected={}, time={}ms",
                batchId, result, affectedCount, executionTimeMs);
    }

    /**
     * 실행 이력 조회
     */
    public List<BatchExecutionHistory> getRecentHistory(String batchId) {
        return historyRepository.findTop10ByBatchIdOrderByExecutedAtDesc(batchId);
    }

    /**
     * 통계
     */
    public long countImplemented() {
        return batchConfigRepository.findAll().stream()
                .filter(BatchConfig::getImplemented)
                .count();
    }

    public long countEnabled() {
        return batchConfigRepository.findByEnabledTrue().size();
    }
}
