package com.kh.game.repository;

import com.kh.game.entity.FanChallengeStageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FanChallengeStageConfigRepository extends JpaRepository<FanChallengeStageConfig, Long> {

    /**
     * 단계 레벨로 조회
     */
    Optional<FanChallengeStageConfig> findByStageLevel(Integer stageLevel);

    /**
     * 활성화된 단계 목록 조회 (레벨 오름차순)
     */
    List<FanChallengeStageConfig> findByIsActiveTrueOrderByStageLevelAsc();

    /**
     * 전체 단계 목록 조회 (레벨 오름차순)
     */
    List<FanChallengeStageConfig> findAllByOrderByStageLevelAsc();

    /**
     * 특정 곡 수 이하로 도전 가능한 활성화된 단계 목록
     */
    @Query("SELECT s FROM FanChallengeStageConfig s WHERE s.isActive = true AND s.requiredSongs <= :songCount ORDER BY s.stageLevel ASC")
    List<FanChallengeStageConfig> findAvailableStagesBySongCount(int songCount);

    /**
     * 최대 단계 레벨 조회
     */
    @Query("SELECT MAX(s.stageLevel) FROM FanChallengeStageConfig s")
    Integer findMaxStageLevel();

    /**
     * 특정 단계 존재 여부
     */
    boolean existsByStageLevel(Integer stageLevel);
}
