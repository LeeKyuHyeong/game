package com.kh.game.controller.admin;

import com.kh.game.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 아티스트 관리 컨트롤러
 * - 아티스트 목록 조회
 * - 아티스트 병합 (중복 아티스트 통합)
 */
@Controller
@RequestMapping("/admin/artist")
@RequiredArgsConstructor
@Slf4j
public class AdminArtistController {

    private final SongService songService;

    /**
     * 아티스트 관리 페이지
     */
    @GetMapping
    public String artistManagement(Model model) {
        List<Map<String, Object>> artists = songService.getArtistsWithCountForAdmin();
        model.addAttribute("artists", artists);
        model.addAttribute("totalArtists", artists.size());
        model.addAttribute("menu", "artist");
        return "admin/artist";
    }

    /**
     * 아티스트 검색 (AJAX)
     */
    @GetMapping("/search")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchArtists(
            @RequestParam(required = false) String keyword) {
        List<Map<String, Object>> artists = songService.getArtistsWithCountForAdmin();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            artists = artists.stream()
                    .filter(a -> ((String) a.get("name")).toLowerCase().contains(lowerKeyword))
                    .toList();
        }

        return ResponseEntity.ok(artists);
    }

    /**
     * 아티스트 곡 수 조회 (AJAX)
     */
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getArtistCount(@RequestParam String artist) {
        Map<String, Object> result = new HashMap<>();
        result.put("artist", artist);
        result.put("count", songService.countSongsByArtist(artist));
        result.put("exists", songService.artistExists(artist));
        return ResponseEntity.ok(result);
    }

    /**
     * 아티스트 병합 (POST)
     */
    @PostMapping("/merge")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> mergeArtist(
            @RequestParam String fromArtist,
            @RequestParam String toArtist) {
        Map<String, Object> result = new HashMap<>();

        try {
            SongService.ArtistMergeResult mergeResult = songService.mergeArtist(fromArtist, toArtist);

            result.put("success", true);
            result.put("message", String.format("'%s' → '%s' 병합 완료", fromArtist, toArtist));
            result.put("fromArtist", mergeResult.getFromArtist());
            result.put("toArtist", mergeResult.getToArtist());
            result.put("songCount", mergeResult.getSongCount());
            result.put("fanChallengeCount", mergeResult.getFanChallengeCount());
            result.put("badgeCount", mergeResult.getBadgeCount());
            result.put("totalCount", mergeResult.getTotalCount());

            log.info("아티스트 병합 성공: {} → {} (총 {}건)",
                    fromArtist, toArtist, mergeResult.getTotalCount());

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("아티스트 병합 실패: {} → {}", fromArtist, toArtist, e);
            result.put("success", false);
            result.put("message", "병합 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 중복 의심 아티스트 목록 (비슷한 이름 그룹화)
     */
    @GetMapping("/duplicates")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> findDuplicateCandidates() {
        List<Map<String, Object>> artists = songService.getArtistsWithCountForAdmin();

        // 비슷한 이름 그룹 찾기 (간단한 휴리스틱)
        // - 공백 제거 후 동일
        // - 대소문자만 다름
        // - 띄어쓰기만 다름

        Map<String, List<Map<String, Object>>> groups = new HashMap<>();

        for (Map<String, Object> artist : artists) {
            String name = (String) artist.get("name");
            String normalized = name.toLowerCase()
                    .replaceAll("\\s+", "")
                    .replaceAll("[^a-z0-9가-힣]", "");

            groups.computeIfAbsent(normalized, k -> new java.util.ArrayList<>()).add(artist);
        }

        // 2개 이상 있는 그룹만 반환
        List<Map<String, Object>> duplicates = new java.util.ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            if (entry.getValue().size() > 1) {
                Map<String, Object> group = new HashMap<>();
                group.put("normalized", entry.getKey());
                group.put("artists", entry.getValue());
                group.put("count", entry.getValue().size());
                duplicates.add(group);
            }
        }

        // 그룹 수 기준 내림차순 정렬
        duplicates.sort((a, b) -> ((Integer) b.get("count")).compareTo((Integer) a.get("count")));

        return ResponseEntity.ok(duplicates);
    }
}
