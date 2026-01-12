package com.kh.game.controller.client;

import com.kh.game.entity.Member;
import com.kh.game.service.WrongAnswerStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class StatsController {

    private final WrongAnswerStatsService wrongAnswerStatsService;

    @GetMapping("/stats")
    public String statsPage(
            @SessionAttribute(value = "member", required = false) Member member,
            Model model) {

        // 가장 어려운 곡 TOP 10
        List<Map<String, Object>> hardestSongs = wrongAnswerStatsService.getHardestSongs(5, 10);

        // 자주 틀리는 답변 TOP 10
        List<Map<String, Object>> commonWrongAnswers = wrongAnswerStatsService.getMostCommonWrongAnswers(10);

        model.addAttribute("hardestSongs", hardestSongs);
        model.addAttribute("commonWrongAnswers", commonWrongAnswers);
        model.addAttribute("member", member);

        return "client/stats";
    }
}
