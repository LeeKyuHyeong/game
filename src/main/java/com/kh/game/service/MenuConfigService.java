package com.kh.game.service;

import com.kh.game.entity.MenuConfig;
import com.kh.game.repository.MenuConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuConfigService {

    private final MenuConfigRepository menuConfigRepository;

    /**
     * 모든 메뉴 설정 조회 (관리자용)
     */
    public List<MenuConfig> findAll() {
        return menuConfigRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * 활성화된 메뉴만 조회 (클라이언트용)
     */
    public List<MenuConfig> findEnabledMenus() {
        return menuConfigRepository.findByEnabledTrueOrderByDisplayOrderAsc();
    }

    /**
     * 카테고리별 메뉴 조회
     */
    public List<MenuConfig> findByCategory(String category) {
        return menuConfigRepository.findByCategoryOrderByDisplayOrderAsc(category);
    }

    /**
     * 카테고리별 활성화된 메뉴 조회
     */
    public List<MenuConfig> findEnabledByCategory(String category) {
        return menuConfigRepository.findByCategoryAndEnabledTrueOrderByDisplayOrderAsc(category);
    }

    /**
     * 메뉴 ID로 조회
     */
    public Optional<MenuConfig> findById(String menuId) {
        return menuConfigRepository.findById(menuId);
    }

    /**
     * 특정 메뉴가 활성화되어 있는지 확인
     */
    public boolean isEnabled(String menuId) {
        return menuConfigRepository.findById(menuId)
                .map(MenuConfig::getEnabled)
                .orElse(false);
    }

    /**
     * 메뉴 활성화 상태를 Map으로 반환 (Thymeleaf용)
     */
    public Map<String, Boolean> getMenuStatusMap() {
        return menuConfigRepository.findAll().stream()
                .collect(Collectors.toMap(MenuConfig::getMenuId, MenuConfig::getEnabled));
    }

    /**
     * 메뉴 저장
     */
    @Transactional
    public MenuConfig save(MenuConfig menuConfig) {
        return menuConfigRepository.save(menuConfig);
    }

    /**
     * 메뉴 활성화/비활성화 토글
     */
    @Transactional
    public void toggleEnabled(String menuId) {
        menuConfigRepository.findById(menuId).ifPresent(menu -> {
            menu.setEnabled(!menu.getEnabled());
            menuConfigRepository.save(menu);
        });
    }

    /**
     * 메뉴 활성화 상태 직접 설정
     */
    @Transactional
    public void setEnabled(String menuId, boolean enabled) {
        menuConfigRepository.findById(menuId).ifPresent(menu -> {
            menu.setEnabled(enabled);
            menuConfigRepository.save(menu);
        });
    }

    /**
     * 초기 데이터가 없을 때 기본 메뉴 생성
     */
    @Transactional
    public void initializeDefaultMenus() {
        if (menuConfigRepository.count() == 0) {
            // 솔로 게임 메뉴
            save(new MenuConfig("SOLO_HOST", "게임 진행용", "TV/모니터에 띄워두고 다른 사람들이 맞추는 모드",
                    "SOLO", "📺", "/game/solo/host", 1, true));
            save(new MenuConfig("SOLO_GUESS", "내가 맞추기", "혼자서 노래를 듣고 맞추는 모드",
                    "SOLO", "🎯", "/game/solo/guess", 2, true));
            save(new MenuConfig("FAN_CHALLENGE", "아티스트 챌린지", "특정 아티스트의 20곡을 맞추는 도전 모드",
                    "SOLO", "🏆", "/game/fan-challenge", 3, true));
            save(new MenuConfig("RETRO", "레트로 챌린지", "추억의 노래를 맞추는 모드",
                    "SOLO", "📻", "/game/retro", 4, true));

            // 멀티 게임 메뉴
            save(new MenuConfig("MULTI_LOBBY", "로비 입장", "친구들과 함께 대결하는 멀티플레이어 모드",
                    "MULTI", "🚪", "/game/multi", 1, true));
        }
    }
}
