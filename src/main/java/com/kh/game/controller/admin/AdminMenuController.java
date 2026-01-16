package com.kh.game.controller.admin;

import com.kh.game.entity.MenuConfig;
import com.kh.game.service.MenuConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/menu")
@RequiredArgsConstructor
public class AdminMenuController {

    private final MenuConfigService menuConfigService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("menus", menuConfigService.findAll());
        model.addAttribute("menu", "menu");
        return "admin/menu/list";
    }

    @GetMapping("/detail/{menuId}")
    @ResponseBody
    public ResponseEntity<MenuConfig> detail(@PathVariable String menuId) {
        return menuConfigService.findById(menuId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/toggle/{menuId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable String menuId) {
        Map<String, Object> result = new HashMap<>();
        try {
            menuConfigService.toggleEnabled(menuId);
            MenuConfig updated = menuConfigService.findById(menuId).orElse(null);
            result.put("success", true);
            result.put("message", "상태가 변경되었습니다.");
            result.put("enabled", updated != null && updated.getEnabled());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "상태 변경 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(@RequestBody MenuConfig menuConfig) {
        Map<String, Object> result = new HashMap<>();
        try {
            menuConfigService.save(menuConfig);
            result.put("success", true);
            result.put("message", "저장되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "저장 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }
}
