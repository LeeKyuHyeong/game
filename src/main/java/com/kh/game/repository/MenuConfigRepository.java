package com.kh.game.repository;

import com.kh.game.entity.MenuConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuConfigRepository extends JpaRepository<MenuConfig, String> {

    List<MenuConfig> findByEnabledTrueOrderByDisplayOrderAsc();

    List<MenuConfig> findByCategoryOrderByDisplayOrderAsc(String category);

    List<MenuConfig> findAllByOrderByDisplayOrderAsc();

    List<MenuConfig> findByCategoryAndEnabledTrueOrderByDisplayOrderAsc(String category);

    long countByEnabledTrue();
}
