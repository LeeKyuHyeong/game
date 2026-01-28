package com.kh.game.repository;

import com.kh.game.entity.Badge;
import com.kh.game.entity.FanChallengeDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByCode(String code);

    List<Badge> findByIsActiveTrueOrderBySortOrderAsc();

    List<Badge> findByCategoryAndIsActiveTrueOrderBySortOrderAsc(Badge.BadgeCategory category);

    // ========== FAN_STAGE 배지 조회 ==========

    /**
     * 아티스트 + 난이도 + 단계로 FAN_STAGE 배지 조회
     */
    Optional<Badge> findByBadgeTypeAndArtistNameAndFanChallengeDifficultyAndFanStageLevel(
            String badgeType, String artistName, FanChallengeDifficulty difficulty, Integer stageLevel);

    /**
     * 특정 아티스트의 모든 FAN_STAGE 배지 조회
     */
    List<Badge> findByBadgeTypeAndArtistNameOrderByFanStageLevelAsc(String badgeType, String artistName);

    /**
     * 모든 FAN_STAGE 배지 조회 (마이그레이션용)
     */
    @Query("SELECT b FROM Badge b WHERE b.badgeType = 'FAN_STAGE' ORDER BY b.artistName, b.fanStageLevel")
    List<Badge> findAllFanStageBadges();

    /**
     * fanChallengeDifficulty가 null인 FAN_STAGE 배지 조회 (마이그레이션 대상)
     */
    @Query("SELECT b FROM Badge b WHERE b.badgeType = 'FAN_STAGE' AND b.fanChallengeDifficulty IS NULL")
    List<Badge> findFanStageBadgesWithoutDifficulty();

    /**
     * 특정 아티스트의 특정 난이도 배지 존재 여부
     */
    boolean existsByBadgeTypeAndArtistNameAndFanChallengeDifficultyAndFanStageLevel(
            String badgeType, String artistName, FanChallengeDifficulty difficulty, Integer stageLevel);

    /**
     * 배지 코드 패턴으로 조회 (Like 검색)
     */
    @Query("SELECT b FROM Badge b WHERE b.code LIKE :pattern")
    List<Badge> findByCodePattern(@Param("pattern") String pattern);

    // ========== 아티스트 관리 (병합) ==========

    /**
     * 아티스트명 일괄 변경 (병합)
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Badge b SET b.artistName = :toArtist WHERE b.artistName = :fromArtist")
    int updateArtistName(@Param("fromArtist") String fromArtist, @Param("toArtist") String toArtist);
}
