package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class BatchScheduler {

    private final TaskScheduler taskScheduler;
    private final BatchService batchService;
    private final SessionCleanupBatch sessionCleanupBatch;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("애플리케이션 시작 완료 - 배치 스케줄 초기화");
        refreshAllSchedules();
    }

    public void refreshAllSchedules() {
        log.info("배치 스케줄 새로고침 시작");

        scheduledTasks.forEach((id, future) -> {
            future.cancel(false);
            log.debug("기존 스케줄 취소: {}", id);
        });
        scheduledTasks.clear();

        batchService.findEnabledAndImplemented().forEach(this::scheduleTask);

        log.info("배치 스케줄 새로고침 완료: {}개 등록", scheduledTasks.size());
    }

    public void refreshSchedule(String batchId) {
        ScheduledFuture<?> existing = scheduledTasks.remove(batchId);
        if (existing != null) {
            existing.cancel(false);
            log.debug("기존 스케줄 취소: {}", batchId);
        }

        batchService.findById(batchId).ifPresent(config -> {
            if (config.getImplemented() && config.getEnabled()) {
                scheduleTask(config);
            }
        });
    }

    private void scheduleTask(BatchConfig config) {
        String batchId = config.getBatchId();
        String cronExpression = config.getCronExpression();

        try {
            Runnable task = createTask(batchId);
            if (task == null) {
                log.warn("배치 작업을 찾을 수 없음: {}", batchId);
                return;
            }

            ScheduledFuture<?> future = taskScheduler.schedule(
                    task,
                    new CronTrigger(cronExpression)
            );

            scheduledTasks.put(batchId, future);
            log.info("배치 스케줄 등록: {} - {}", batchId, cronExpression);

        } catch (Exception e) {
            log.error("배치 스케줄 등록 실패: {} - {}", batchId, e.getMessage());
        }
    }

    private Runnable createTask(String batchId) {
        switch (batchId) {
            case SessionCleanupBatch.BATCH_ID:
                return () -> {
                    try {
                        sessionCleanupBatch.execute(BatchExecutionHistory.ExecutionType.SCHEDULED);
                    } catch (Exception e) {
                        log.error("배치 실행 중 오류: {}", batchId, e);
                    }
                };
            default:
                return null;
        }
    }

    public void executeManually(String batchId) {
        log.info("배치 수동 실행 요청: {}", batchId);

        switch (batchId) {
            case SessionCleanupBatch.BATCH_ID:
                sessionCleanupBatch.execute(BatchExecutionHistory.ExecutionType.MANUAL);
                break;
            default:
                throw new IllegalArgumentException("실행할 수 없는 배치입니다: " + batchId);
        }
    }

    public int getScheduledCount() {
        return scheduledTasks.size();
    }

    public boolean isScheduled(String batchId) {
        return scheduledTasks.containsKey(batchId);
    }
}