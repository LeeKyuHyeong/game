package com.kh.game.service;

import com.kh.game.entity.FanChallengeStageConfig;
import com.kh.game.repository.FanChallengeStageConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 팬 챌린지 단계 설정 서비스
 * - 단계별 곡 수, 활성화 상태 관리
 * - HARDCORE 모드에서만 단계 시스템 적용
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FanChallengeStageService {

    private final FanChallengeStageConfigRepository stageConfigRepository;

    /**
     * 전체 단계 목록 조회 (관리자용)
     */
    public List<FanChallengeStageConfig> getAllStages() {
        return stageConfigRepository.findAllByOrderByStageLevelAsc();
    }

    /**
     * 활성화된 단계 목록 조회
     */
    public List<FanChallengeStageConfig> getActiveStages() {
        return stageConfigRepository.findByIsActiveTrueOrderByStageLevelAsc();
    }

    /**
     * 특정 단계 설정 조회
     */
    public Optional<FanChallengeStageConfig> getStageConfig(int stageLevel) {
        return stageConfigRepository.findByStageLevel(stageLevel);
    }

    /**
     * 특정 단계의 필요 곡 수 조회
     */
    public int getRequiredSongsForStage(int stageLevel) {
        return stageConfigRepository.findByStageLevel(stageLevel)
                .map(FanChallengeStageConfig::getRequiredSongs)
                .orElse(20); // 기본값 20곡
    }

    /**
     * 아티스트별 도전 가능한 단계 목록 조회
     * (해당 아티스트의 곡 수 기준으로 활성화된 단계 중 도전 가능한 것만)
     */
    public List<FanChallengeStageConfig> getAvailableStagesForArtist(int artistSongCount) {
        return stageConfigRepository.findAvailableStagesBySongCount(artistSongCount);
    }

    /**
     * 단계 활성화
     */
    @Transactional
    public FanChallengeStageConfig activateStage(int stageLevel) {
        FanChallengeStageConfig config = stageConfigRepository.findByStageLevel(stageLevel)
                .orElseThrow(() -> new IllegalArgumentException("단계를 찾을 수 없습니다: " + stageLevel));

        // 이전 단계가 활성화되어 있는지 확인
        if (stageLevel > 1) {
            boolean previousActive = stageConfigRepository.findByStageLevel(stageLevel - 1)
                    .map(FanChallengeStageConfig::getIsActive)
                    .orElse(false);
            if (!previousActive) {
                throw new IllegalStateException("이전 단계(" + (stageLevel - 1) + "단계)를 먼저 활성화해야 합니다.");
            }
        }

        config.activate();
        log.info("팬 챌린지 {}단계 활성화", stageLevel);
        return stageConfigRepository.save(config);
    }

    /**
     * 단계 비활성화
     */
    @Transactional
    public FanChallengeStageConfig deactivateStage(int stageLevel) {
        FanChallengeStageConfig config = stageConfigRepository.findByStageLevel(stageLevel)
                .orElseThrow(() -> new IllegalArgumentException("단계를 찾을 수 없습니다: " + stageLevel));

        // 1단계는 비활성화 불가
        if (stageLevel == 1) {
            throw new IllegalStateException("1단계는 비활성화할 수 없습니다.");
        }

        // 다음 단계가 활성화되어 있는지 확인
        boolean nextActive = stageConfigRepository.findByStageLevel(stageLevel + 1)
                .map(FanChallengeStageConfig::getIsActive)
                .orElse(false);
        if (nextActive) {
            throw new IllegalStateException("다음 단계(" + (stageLevel + 1) + "단계)를 먼저 비활성화해야 합니다.");
        }

        config.deactivate();
        log.info("팬 챌린지 {}단계 비활성화", stageLevel);
        return stageConfigRepository.save(config);
    }

    /**
     * 단계 활성화 토글
     */
    @Transactional
    public FanChallengeStageConfig toggleStageActive(int stageLevel) {
        FanChallengeStageConfig config = stageConfigRepository.findByStageLevel(stageLevel)
                .orElseThrow(() -> new IllegalArgumentException("단계를 찾을 수 없습니다: " + stageLevel));

        if (Boolean.TRUE.equals(config.getIsActive())) {
            return deactivateStage(stageLevel);
        } else {
            return activateStage(stageLevel);
        }
    }

    /**
     * 단계 설정 수정
     */
    @Transactional
    public FanChallengeStageConfig updateStageConfig(int stageLevel, Integer requiredSongs, String stageName, String emoji) {
        FanChallengeStageConfig config = stageConfigRepository.findByStageLevel(stageLevel)
                .orElseThrow(() -> new IllegalArgumentException("단계를 찾을 수 없습니다: " + stageLevel));

        if (requiredSongs != null && requiredSongs > 0) {
            // 이전 단계보다 작거나 같을 수 없음
            if (stageLevel > 1) {
                int prevRequired = getRequiredSongsForStage(stageLevel - 1);
                if (requiredSongs <= prevRequired) {
                    throw new IllegalArgumentException("필요 곡 수는 이전 단계(" + prevRequired + "곡)보다 많아야 합니다.");
                }
            }
            // 다음 단계보다 크거나 같을 수 없음
            Optional<FanChallengeStageConfig> nextStage = stageConfigRepository.findByStageLevel(stageLevel + 1);
            if (nextStage.isPresent() && requiredSongs >= nextStage.get().getRequiredSongs()) {
                throw new IllegalArgumentException("필요 곡 수는 다음 단계(" + nextStage.get().getRequiredSongs() + "곡)보다 적어야 합니다.");
            }
            config.setRequiredSongs(requiredSongs);
        }

        if (stageName != null && !stageName.trim().isEmpty()) {
            config.setStageName(stageName.trim());
        }

        if (emoji != null && !emoji.trim().isEmpty()) {
            config.setStageEmoji(emoji.trim());
        }

        log.info("팬 챌린지 {}단계 설정 수정: 곡수={}, 이름={}, 이모지={}",
                stageLevel, config.getRequiredSongs(), config.getStageName(), config.getStageEmoji());
        return stageConfigRepository.save(config);
    }

    /**
     * 새 단계 추가
     */
    @Transactional
    public FanChallengeStageConfig addStage(int requiredSongs, String stageName, String emoji) {
        Integer maxLevel = stageConfigRepository.findMaxStageLevel();
        int newLevel = (maxLevel != null ? maxLevel : 0) + 1;

        // 이전 단계보다 곡 수가 많아야 함
        if (maxLevel != null) {
            int prevRequired = getRequiredSongsForStage(maxLevel);
            if (requiredSongs <= prevRequired) {
                throw new IllegalArgumentException("필요 곡 수는 이전 단계(" + prevRequired + "곡)보다 많아야 합니다.");
            }
        }

        FanChallengeStageConfig config = new FanChallengeStageConfig(newLevel, requiredSongs, stageName, emoji);
        log.info("팬 챌린지 새 단계 추가: {}단계 ({}곡)", newLevel, requiredSongs);
        return stageConfigRepository.save(config);
    }

    /**
     * 단계가 존재하고 활성화되어 있는지 확인
     */
    public boolean isStageAvailable(int stageLevel) {
        return stageConfigRepository.findByStageLevel(stageLevel)
                .map(config -> Boolean.TRUE.equals(config.getIsActive()))
                .orElse(false);
    }

    /**
     * 특정 아티스트의 곡 수로 도전 가능한 최대 단계 조회
     */
    public int getMaxAvailableStage(int artistSongCount) {
        List<FanChallengeStageConfig> available = getAvailableStagesForArtist(artistSongCount);
        if (available.isEmpty()) {
            return 0;
        }
        return available.get(available.size() - 1).getStageLevel();
    }
}
