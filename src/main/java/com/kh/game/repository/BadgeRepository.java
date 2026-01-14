package com.kh.game.repository;

import com.kh.game.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByCode(String code);

    List<Badge> findByIsActiveTrueOrderBySortOrderAsc();

    List<Badge> findByCategoryAndIsActiveTrueOrderBySortOrderAsc(Badge.BadgeCategory category);
}
