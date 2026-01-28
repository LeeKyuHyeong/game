package com.kh.game.controller.admin;

import com.kh.game.batch.BatchScheduler;
import com.kh.game.entity.BatchConfig;
import com.kh.game.service.BadWordService;
import com.kh.game.service.BatchService;
import com.kh.game.service.MenuConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 시스템 설정 통합 페이지 컨트롤러
 * 배치 관리, 메뉴 관리, 금칙어 관리를 하나의 페이지에서 탭으로 관리
 */
@Controller
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final BatchService batchService;
    private final BatchScheduler batchScheduler;
    private final MenuConfigService menuConfigService;
    private final BadWordService badWordService;

    /**
     * 통합 시스템 설정 페이지
     */
    @GetMapping({"", "/"})
    public String systemIndex(@RequestParam(defaultValue = "batch") String tab, Model model) {
        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "system");

        // 배치 통계
        List<BatchConfig> batches = batchService.findAll();
        long totalBatches = batches.size();
        long implementedCount = batchService.countImplemented();
        long enabledCount = batchService.countEnabled();
        int scheduledCount = batchScheduler.getScheduledCount();

        model.addAttribute("totalBatches", totalBatches);
        model.addAttribute("implementedCount", implementedCount);
        model.addAttribute("enabledCount", enabledCount);
        model.addAttribute("scheduledCount", scheduledCount);

        // 메뉴 통계
        long totalMenus = menuConfigService.count();
        long activeMenus = menuConfigService.countEnabled();

        model.addAttribute("totalMenus", totalMenus);
        model.addAttribute("activeMenus", activeMenus);

        // 금칙어 통계
        long totalBadWords = badWordService.count();
        long activeBadWords = badWordService.countActive();

        model.addAttribute("totalBadWords", totalBadWords);
        model.addAttribute("activeBadWords", activeBadWords);

        return "admin/system/index";
    }
}
