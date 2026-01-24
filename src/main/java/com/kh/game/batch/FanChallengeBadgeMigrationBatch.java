package com.kh.game.batch;

import com.kh.game.entity.Badge;
import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.FanChallengeDifficulty;
import com.kh.game.repository.BadgeRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 팬 챌린지 배지 마이그레이션 배치
 * - 기존 FAN_STAGE_BTS_1 형식을 FAN_STAGE_BTS_HARDCORE_1 형식으로 변환
 * - fanChallengeDifficulty 필드 설정 (기존 배지는 모두 HARDCORE로 간주)
 * - 주로 수동 실행 용도 (배지 시스템 업그레이드 시 1회 실행)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FanChallengeBadgeMigrationBatch {

    private final BadgeRepository badgeRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_FAN_CHALLENGE_BADGE_MIGRATION";

    // 기존 형식: FAN_STAGE_아티스트_단계 (예: FAN_STAGE_BTS_1, FAN_STAGE_아이유_2)
    private static final Pattern OLD_CODE_PATTERN = Pattern.compile("^FAN_STAGE_(.+)_(\\d+)$");

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int migratedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 기존 FAN_STAGE 배지 마이그레이션", BATCH_ID);

            // 1. fanChallengeDifficulty가 null인 FAN_STAGE 배지 조회
            List<Badge> targetBadges = badgeRepository.findFanStageBadgesWithoutDifficulty();
            log.info("마이그레이션 대상 배지 수: {}개", targetBadges.size());

            if (targetBadges.isEmpty()) {
                resultMessage.append("마이그레이션 대상 배지 없음 (이미 마이그레이션 완료됨)");
                log.info("[{}] 마이그레이션 대상 배지 없음", BATCH_ID);

                batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    0,
                    System.currentTimeMillis() - startTime
                );
                return 0;
            }

            // 2. 각 배지 마이그레이션
            for (Badge badge : targetBadges) {
                try {
                    if (migrateBadge(badge)) {
                        migratedCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.warn("배지 마이그레이션 실패: {} - {}", badge.getCode(), e.getMessage());
                    errorCount++;
                }
            }

            resultMessage.append(String.format(
                "배지 마이그레이션 완료. 대상: %d개, 마이그레이션: %d개, 스킵: %d개, 오류: %d개",
                targetBadges.size(), migratedCount, skippedCount, errorCount
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                BATCH_ID,
                executionType,
                errorCount > 0 ? BatchConfig.ExecutionResult.FAIL : BatchConfig.ExecutionResult.SUCCESS,
                resultMessage.toString(),
                migratedCount,
                executionTime
            );

            log.info("[{}] 배치 실행 완료 - 마이그레이션: {}개, 스킵: {}개, 오류: {}개, 소요시간: {}ms",
                BATCH_ID, migratedCount, skippedCount, errorCount, executionTime);

            return migratedCount;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                BATCH_ID,
                executionType,
                BatchConfig.ExecutionResult.FAIL,
                "오류 발생: " + e.getMessage(),
                migratedCount,
                executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 개별 배지 마이그레이션
     * @return 마이그레이션 성공 여부
     */
    private boolean migrateBadge(Badge badge) {
        String oldCode = badge.getCode();

        // 이미 새 형식인지 확인 (NORMAL 또는 HARDCORE가 포함되어 있으면 스킵)
        if (oldCode.contains("_NORMAL_") || oldCode.contains("_HARDCORE_")) {
            log.debug("이미 새 형식 배지: {}", oldCode);
            // 그래도 difficulty가 null이면 설정
            if (badge.getFanChallengeDifficulty() == null) {
                badge.setFanChallengeDifficulty(
                    oldCode.contains("_NORMAL_") ? FanChallengeDifficulty.NORMAL : FanChallengeDifficulty.HARDCORE
                );
                badgeRepository.save(badge);
                return true;
            }
            return false;
        }

        // 기존 형식 파싱: FAN_STAGE_아티스트_단계
        Matcher matcher = OLD_CODE_PATTERN.matcher(oldCode);
        if (!matcher.matches()) {
            log.warn("알 수 없는 배지 코드 형식: {}", oldCode);
            return false;
        }

        String artistCode = matcher.group(1);
        int stageLevel = Integer.parseInt(matcher.group(2));

        // 새 코드 생성: FAN_STAGE_아티스트_HARDCORE_단계
        String newCode = "FAN_STAGE_" + artistCode + "_HARDCORE_" + stageLevel;

        // 새 코드가 이미 존재하는지 확인
        if (badgeRepository.findByCode(newCode).isPresent()) {
            log.warn("새 코드가 이미 존재: {} -> {} (스킵)", oldCode, newCode);
            return false;
        }

        // 배지 업데이트
        badge.setCode(newCode);
        badge.setFanChallengeDifficulty(FanChallengeDifficulty.HARDCORE);

        // 설명 업데이트 (하드코어 명시)
        if (badge.getDescription() != null && !badge.getDescription().contains("하드코어")) {
            badge.setDescription(badge.getDescription().replace("퍼펙트 클리어", "하드코어 퍼펙트 클리어"));
        }

        badgeRepository.save(badge);
        log.info("배지 마이그레이션 완료: {} -> {}", oldCode, newCode);
        return true;
    }
}
