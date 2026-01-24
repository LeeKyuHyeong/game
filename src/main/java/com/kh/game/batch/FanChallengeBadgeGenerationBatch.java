package com.kh.game.batch;

import com.kh.game.entity.Badge;
import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.FanChallengeDifficulty;
import com.kh.game.entity.FanChallengeStageConfig;
import com.kh.game.repository.BadgeRepository;
import com.kh.game.repository.FanChallengeStageConfigRepository;
import com.kh.game.service.BadgeService;
import com.kh.game.service.BatchService;
import com.kh.game.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 팬 챌린지 배지 사전 생성 배치
 * - 모든 아티스트(20곡 이상)에 대해 난이도/단계별 배지를 미리 생성
 * - 주로 수동 실행 용도 (배지 시스템 도입 또는 새 아티스트 추가 시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FanChallengeBadgeGenerationBatch {

    private final BadgeRepository badgeRepository;
    private final FanChallengeStageConfigRepository stageConfigRepository;
    private final SongService songService;
    private final BadgeService badgeService;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_FAN_CHALLENGE_BADGE_GENERATION";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int createdCount = 0;
        int skippedCount = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 팬 챌린지 배지 사전 생성", BATCH_ID);

            // 1. 팬 챌린지 대상 아티스트 조회 (20곡 이상)
            List<Map<String, Object>> artists = songService.getArtistsWithCountForFanChallenge();
            log.info("팬 챌린지 대상 아티스트 수: {}명", artists.size());

            // 2. 단계 설정 조회
            List<FanChallengeStageConfig> stageConfigs = stageConfigRepository.findAllByOrderByStageLevelAsc();
            log.info("단계 설정 수: {}개", stageConfigs.size());

            // 3. 아티스트별 배지 생성
            for (Map<String, Object> artistInfo : artists) {
                String artistName = (String) artistInfo.get("name");
                int songCount = (Integer) artistInfo.get("count");

                // NORMAL 1단계 배지 생성
                if (createBadgeIfNotExists(artistName, FanChallengeDifficulty.NORMAL, 1, songCount, 20)) {
                    createdCount++;
                } else {
                    skippedCount++;
                }

                // HARDCORE 단계별 배지 생성
                for (FanChallengeStageConfig config : stageConfigs) {
                    int stageLevel = config.getStageLevel();
                    int requiredSongs = config.getRequiredSongs();

                    // 해당 아티스트가 이 단계에 도전 가능한지 확인
                    if (songCount >= requiredSongs) {
                        if (createBadgeIfNotExists(artistName, FanChallengeDifficulty.HARDCORE, stageLevel, songCount, requiredSongs)) {
                            createdCount++;
                        } else {
                            skippedCount++;
                        }
                    }
                }
            }

            resultMessage.append(String.format(
                "배지 사전 생성 완료. 대상 아티스트: %d명, 생성: %d개, 스킵(이미존재): %d개",
                artists.size(), createdCount, skippedCount
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                BATCH_ID,
                executionType,
                BatchConfig.ExecutionResult.SUCCESS,
                resultMessage.toString(),
                createdCount,
                executionTime
            );

            log.info("[{}] 배치 실행 완료 - 생성: {}개, 스킵: {}개, 소요시간: {}ms",
                BATCH_ID, createdCount, skippedCount, executionTime);

            return createdCount;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                BATCH_ID,
                executionType,
                BatchConfig.ExecutionResult.FAIL,
                "오류 발생: " + e.getMessage(),
                createdCount,
                executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 배지가 없으면 생성
     * @return 생성 여부 (true: 생성됨, false: 이미 존재)
     */
    private boolean createBadgeIfNotExists(String artistName, FanChallengeDifficulty difficulty,
                                            int stageLevel, int artistSongCount, int requiredSongs) {
        String normalizedArtist = normalizeArtistCode(artistName);
        String badgeCode = badgeService.buildStageBadgeCode(normalizedArtist, difficulty, stageLevel);

        // 이미 존재하는지 확인
        if (badgeRepository.findByCode(badgeCode).isPresent()) {
            log.debug("배지 이미 존재: {}", badgeCode);
            return false;
        }

        // 배지 생성
        Badge badge = new Badge();
        badge.setCode(badgeCode);
        badge.setBadgeType("FAN_STAGE");
        badge.setArtistName(artistName);
        badge.setFanStageLevel(stageLevel);
        badge.setFanChallengeDifficulty(difficulty);
        badge.setCategory(Badge.BadgeCategory.SPECIAL);
        badge.setRarity(badgeService.determineStageBadgeRarity(difficulty, stageLevel));
        badge.setIsActive(true);
        badge.setSortOrder(difficulty == FanChallengeDifficulty.NORMAL ? 100 : 100 + stageLevel);

        // 이름과 설명 설정
        if (difficulty == FanChallengeDifficulty.NORMAL) {
            badge.setName(artistName + " 노말");
            badge.setDescription(artistName + " 팬 챌린지 노말 모드 퍼펙트 클리어");
            badge.setEmoji("⭐");
        } else {
            FanChallengeStageConfig config = stageConfigRepository.findByStageLevel(stageLevel).orElse(null);
            String stageName = config != null ? config.getStageName() : stageLevel + "단계";
            String stageEmoji = config != null ? config.getStageEmoji() : "🏆";

            badge.setName(artistName + " " + stageName);
            badge.setDescription(artistName + " 팬 챌린지 하드코어 " + stageName + " 퍼펙트 클리어");
            badge.setEmoji(stageEmoji);
        }

        badgeRepository.save(badge);
        log.debug("배지 생성: {} ({}, {})", badge.getName(), badgeCode, badge.getRarity().getDisplayName());
        return true;
    }

    /**
     * 아티스트명을 배지 코드용으로 정규화
     */
    private String normalizeArtistCode(String artist) {
        if (artist == null) return "UNKNOWN";
        return artist.toUpperCase()
            .replaceAll("[^A-Z0-9가-힣]", "")
            .replace(" ", "");
    }
}
