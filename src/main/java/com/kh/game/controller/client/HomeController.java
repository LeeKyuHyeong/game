package com.kh.game.controller.client;

import com.kh.game.service.MenuConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MenuConfigService menuConfigService;

    @GetMapping("/")
    public String home(Model model) {
        // 메뉴 활성화 상태를 Map으로 전달 (menuId -> enabled)
        model.addAttribute("menuStatus", menuConfigService.getMenuStatusMap());
        return "client/home";
    }
}
