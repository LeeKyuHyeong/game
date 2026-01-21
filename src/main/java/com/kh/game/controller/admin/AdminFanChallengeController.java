package com.kh.game.controller.admin;

import com.kh.game.entity.FanChallengeDifficulty;
import com.kh.game.entity.FanChallengeRecord;
import com.kh.game.repository.FanChallengeRecordRepository;
import com.kh.game.service.FanChallengeService;
import com.kh.game.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/fan-challenge")
@RequiredArgsConstructor
public class AdminFanChallengeController {

    private final FanChallengeRecordRepository fanChallengeRecordRepository;
    private final FanChallengeService fanChallengeService;
    private final SongService songService;

    /**
     * 레거시 URL → 통합 페이지로 리다이렉트
     */
    @GetMapping
    public String redirectToChallenge() {
        return "redirect:/admin/challenge?tab=fan";
    }

    /**
     * AJAX 로딩용 팬 챌린지 콘텐츠 (fragment)
     */
    @GetMapping("/content")
    public String listContent(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String difficulty,
                              @RequestParam(required = false) String perfect,
                              @RequestParam(defaultValue = "achievedAt") String sort,
                              @RequestParam(defaultValue = "desc") String direction,
                              Model model) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<FanChallengeRecord> recordPage = getFilteredRecords(keyword, difficulty, perfect, pageable);

        // 통계
        long totalCount = fanChallengeRecordRepository.count();
        long perfectCount = fanChallengeRecordRepository.countPerfectClears();
        long artistCount = fanChallengeRecordRepository.countDistinctArtists();
        long todayCount = fanChallengeRecordRepository.countTodayRecords();

        model.addAttribute("records", recordPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", recordPage.getTotalPages());
        model.addAttribute("totalItems", recordPage.getTotalElements());
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("perfectCount", perfectCount);
        model.addAttribute("artistCount", artistCount);
        model.addAttribute("todayCount", todayCount);
        model.addAttribute("keyword", keyword);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("perfect", perfect);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "admin/challenge/fragments/fan-challenge";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        return fanChallengeRecordRepository.findById(id)
                .map(record -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", record.getId());
                    result.put("memberId", record.getMember().getId());
                    result.put("memberNickname", record.getMember().getNickname());
                    result.put("memberEmail", record.getMember().getEmail());
                    result.put("artist", record.getArtist());
                    result.put("difficulty", record.getDifficulty().name());
                    result.put("difficultyDisplayName", record.getDifficulty().getDisplayName());
                    result.put("correctCount", record.getCorrectCount());
                    result.put("totalSongs", record.getTotalSongs());
                    result.put("clearRate", String.format("%.1f", record.getClearRate()));
                    result.put("isPerfectClear", record.getIsPerfectClear());
                    result.put("isCurrentPerfect", record.getIsCurrentPerfect());
                    result.put("bestTimeMs", record.getBestTimeMs());
                    result.put("achievedAt", record.getAchievedAt());
                    result.put("lastCheckedAt", record.getLastCheckedAt());
                    result.put("createdAt", record.getCreatedAt());

                    // 현재 아티스트 곡 수
                    int currentSongCount = songService.countActiveSongsByArtist(record.getArtist());
                    result.put("currentSongCount", currentSongCount);

                    // 현재 기준 클리어율
                    double currentClearRate = currentSongCount > 0
                            ? Math.min((double) record.getCorrectCount() / currentSongCount * 100, 100.0)
                            : 0.0;
                    result.put("currentClearRate", String.format("%.1f", currentClearRate));

                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtistStats() {
        List<Object[]> statistics = fanChallengeRecordRepository.getArtistStatistics();

        List<Map<String, Object>> result = statistics.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("artist", row[0]);
                    item.put("challengeCount", row[1]);
                    item.put("perfectCount", row[2]);

                    // 현재 곡 수
                    String artist = (String) row[0];
                    int songCount = songService.countActiveSongsByArtist(artist);
                    item.put("songCount", songCount);

                    return item;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/member/{memberId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMemberRecords(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "achievedAt"));
        Page<FanChallengeRecord> recordPage = fanChallengeRecordRepository.findByMemberId(memberId, pageable);

        List<Map<String, Object>> records = recordPage.getContent().stream()
                .map(record -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", record.getId());
                    item.put("artist", record.getArtist());
                    item.put("difficulty", record.getDifficulty().name());
                    item.put("difficultyDisplayName", record.getDifficulty().getDisplayName());
                    item.put("correctCount", record.getCorrectCount());
                    item.put("totalSongs", record.getTotalSongs());
                    item.put("clearRate", String.format("%.1f", record.getClearRate()));
                    item.put("isPerfectClear", record.getIsPerfectClear());
                    item.put("bestTimeMs", record.getBestTimeMs());
                    item.put("achievedAt", record.getAchievedAt());
                    return item;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("totalPages", recordPage.getTotalPages());
        result.put("totalElements", recordPage.getTotalElements());
        result.put("currentPage", page);

        return ResponseEntity.ok(result);
    }

    private Page<FanChallengeRecord> getFilteredRecords(String keyword, String difficulty, String perfect, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasDifficulty = difficulty != null && !difficulty.isEmpty();
        boolean hasPerfect = perfect != null && !perfect.isEmpty();

        FanChallengeDifficulty diff = hasDifficulty ? FanChallengeDifficulty.fromString(difficulty) : null;
        Boolean isPerfect = hasPerfect ? "true".equalsIgnoreCase(perfect) : null;

        // 복합 필터 조합
        if (hasKeyword && hasDifficulty && hasPerfect) {
            return fanChallengeRecordRepository.findByArtistContainingAndDifficultyAndIsPerfectClear(keyword, diff, isPerfect, pageable);
        } else if (hasKeyword && hasDifficulty) {
            return fanChallengeRecordRepository.findByArtistContainingAndDifficulty(keyword, diff, pageable);
        } else if (hasKeyword && hasPerfect) {
            return fanChallengeRecordRepository.findByArtistContainingAndIsPerfectClear(keyword, isPerfect, pageable);
        } else if (hasDifficulty && hasPerfect) {
            return fanChallengeRecordRepository.findByDifficultyAndIsPerfectClear(diff, isPerfect, pageable);
        } else if (hasKeyword) {
            return fanChallengeRecordRepository.findByArtistContaining(keyword, pageable);
        } else if (hasDifficulty) {
            return fanChallengeRecordRepository.findByDifficulty(diff, pageable);
        } else if (hasPerfect) {
            return fanChallengeRecordRepository.findByIsPerfectClear(isPerfect, pageable);
        } else {
            return fanChallengeRecordRepository.findAllWithMember(pageable);
        }
    }
}
