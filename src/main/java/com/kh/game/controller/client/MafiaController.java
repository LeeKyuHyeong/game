package com.kh.game.controller.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mafia")
public class MafiaController {

    @GetMapping
    public String mafia() {
        return "client/mafia";
    }
}
