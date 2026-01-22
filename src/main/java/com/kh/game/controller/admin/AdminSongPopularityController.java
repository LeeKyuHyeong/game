package com.kh.game.controller.admin;

import com.kh.game.entity.SongPopularityVote;
import com.kh.game.service.SongPopularityVoteService;
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
@RequestMapping("/admin/song-popularity")
@RequiredArgsConstructor
public class AdminSongPopularityController {

    private final SongPopularityVoteService voteService;

    /**
     * 메인 페이지 (탭 구조)
     */
    @GetMapping
    public String index(@RequestParam(defaultValue = "songs") String tab,
                        Model model) {
        // 전체 통계
        Map<String, Object> stats = voteService.getTotalStats();
        model.addAttribute("totalVotes", stats.get("totalVotes"));
        model.addAttribute("todayVotes", stats.get("todayVotes"));
        model.addAttribute("avgRating", stats.get("avgRating"));
        model.addAttribute("votedSongCount", stats.get("votedSongCount"));

        model.addAttribute("activeTab", tab);
        model.addAttribute("menu", "song-popularity");

        return "admin/song-popularity/index";
    }

    /**
     * 곡별 통계 탭 콘텐츠 (AJAX 로딩용)
     */
    @GetMapping("/songs/content")
    public String songsContent(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "voteCount") String sort,
                               @RequestParam(defaultValue = "desc") String direction,
                               Model model) {
        // 곡별 통계
        List<Map<String, Object>> songStats = voteService.getSongStatistics();

        // 검색 필터링
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = keyword.toLowerCase();
            songStats = songStats.stream()
                    .filter(s -> {
                        String title = ((String) s.get("title")).toLowerCase();
                        String artist = ((String) s.get("artist")).toLowerCase();
                        return title.contains(searchKeyword) || artist.contains(searchKeyword);
                    })
                    .toList();
        }

        // 정렬
        songStats = sortSongStats(songStats, sort, direction);

        // 페이징 처리
        int totalItems = songStats.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int startIdx = page * size;
        int endIdx = Math.min(startIdx + size, totalItems);

        List<Map<String, Object>> pagedStats = songStats.subList(
                Math.min(startIdx, totalItems),
                Math.min(endIdx, totalItems)
        );

        model.addAttribute("songStats", pagedStats);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "admin/song-popularity/fragments/song-stats";
    }

    /**
     * 아티스트별 통계 탭 콘텐츠 (AJAX 로딩용)
     */
    @GetMapping("/artists/content")
    public String artistsContent(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "totalVotes") String sort,
                                 @RequestParam(defaultValue = "desc") String direction,
                                 Model model) {
        // 아티스트별 통계
        List<Map<String, Object>> artistStats = voteService.getArtistStatistics();

        // 검색 필터링
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = keyword.toLowerCase();
            artistStats = artistStats.stream()
                    .filter(s -> ((String) s.get("artist")).toLowerCase().contains(searchKeyword))
                    .toList();
        }

        // 정렬
        artistStats = sortArtistStats(artistStats, sort, direction);

        // 페이징 처리
        int totalItems = artistStats.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int startIdx = page * size;
        int endIdx = Math.min(startIdx + size, totalItems);

        List<Map<String, Object>> pagedStats = artistStats.subList(
                Math.min(startIdx, totalItems),
                Math.min(endIdx, totalItems)
        );

        model.addAttribute("artistStats", pagedStats);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "admin/song-popularity/fragments/artist-stats";
    }

    /**
     * 투표 내역 탭 콘텐츠 (AJAX 로딩용)
     */
    @GetMapping("/votes/content")
    public String votesContent(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "createdAt") String sort,
                               @RequestParam(defaultValue = "desc") String direction,
                               Model model) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<SongPopularityVote> votePage = voteService.searchVotes(keyword, pageable);

        model.addAttribute("votes", votePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", votePage.getTotalPages());
        model.addAttribute("totalItems", votePage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        return "admin/song-popularity/fragments/vote-list";
    }

    /**
     * 특정 곡의 투표자 목록 (JSON)
     */
    @GetMapping("/song/{songId}/votes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSongVotes(
            @PathVariable Long songId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SongPopularityVote> votePage = voteService.getVotesBySongId(songId, pageable);

        List<Map<String, Object>> votes = votePage.getContent().stream()
                .map(vote -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", vote.getId());
                    item.put("memberId", vote.getMember().getId());
                    item.put("memberNickname", vote.getMember().getNickname());
                    item.put("rating", vote.getRating());
                    item.put("ratingLabel", SongPopularityVoteService.getRatingLabel(vote.getRating()));
                    item.put("createdAt", vote.getCreatedAt());
                    return item;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("votes", votes);
        result.put("totalPages", votePage.getTotalPages());
        result.put("totalElements", votePage.getTotalElements());
        result.put("currentPage", page);

        return ResponseEntity.ok(result);
    }

    /**
     * 곡별 통계 정렬
     */
    private List<Map<String, Object>> sortSongStats(List<Map<String, Object>> stats,
                                                     String sortField, String direction) {
        boolean isDesc = "desc".equalsIgnoreCase(direction);

        return stats.stream()
                .sorted((a, b) -> {
                    int result;
                    switch (sortField) {
                        case "title":
                            result = ((String) a.get("title")).compareTo((String) b.get("title"));
                            break;
                        case "artist":
                            result = ((String) a.get("artist")).compareTo((String) b.get("artist"));
                            break;
                        case "avgRating":
                            result = Double.compare(
                                    ((Number) a.get("avgRating")).doubleValue(),
                                    ((Number) b.get("avgRating")).doubleValue()
                            );
                            break;
                        case "voteCount":
                        default:
                            result = Long.compare(
                                    ((Number) a.get("voteCount")).longValue(),
                                    ((Number) b.get("voteCount")).longValue()
                            );
                            break;
                    }
                    return isDesc ? -result : result;
                })
                .toList();
    }

    /**
     * 아티스트별 통계 정렬
     */
    private List<Map<String, Object>> sortArtistStats(List<Map<String, Object>> stats,
                                                       String sortField, String direction) {
        boolean isDesc = "desc".equalsIgnoreCase(direction);

        return stats.stream()
                .sorted((a, b) -> {
                    int result;
                    switch (sortField) {
                        case "artist":
                            result = ((String) a.get("artist")).compareTo((String) b.get("artist"));
                            break;
                        case "songCount":
                            result = Long.compare(
                                    ((Number) a.get("songCount")).longValue(),
                                    ((Number) b.get("songCount")).longValue()
                            );
                            break;
                        case "avgRating":
                            result = Double.compare(
                                    ((Number) a.get("avgRating")).doubleValue(),
                                    ((Number) b.get("avgRating")).doubleValue()
                            );
                            break;
                        case "totalVotes":
                        default:
                            result = Long.compare(
                                    ((Number) a.get("totalVotes")).longValue(),
                                    ((Number) b.get("totalVotes")).longValue()
                            );
                            break;
                    }
                    return isDesc ? -result : result;
                })
                .toList();
    }
}
