package com.kh.game.controller.admin;

import com.kh.game.entity.Song;
import com.kh.game.service.GenreService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin/song")
@RequiredArgsConstructor
public class AdminSongController {

    private final SongService songService;
    private final GenreService genreService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) List<String> artists,
                       @RequestParam(required = false) Long genreId,
                       @RequestParam(required = false) String useYn,
                       @RequestParam(required = false) Boolean isSolo,
                       @RequestParam(required = false) Integer releaseYear,
                       @RequestParam(defaultValue = "id") String sort,
                       @RequestParam(defaultValue = "desc") String direction,
                       @RequestParam(defaultValue = "table") String viewMode,
                       Model model) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // 빈 문자열을 null로 변환
        String keywordParam = (keyword != null && keyword.trim().isEmpty()) ? null : keyword;
        String useYnParam = (useYn != null && useYn.trim().isEmpty()) ? null : useYn;

        Page<Song> songPage = songService.searchWithFilters(keywordParam, artists, genreId, useYnParam, isSolo, releaseYear, pageable);

        model.addAttribute("songs", songPage.getContent());
        model.addAttribute("genres", genreService.findActiveGenres());
        model.addAttribute("artists", songService.getArtistsWithCount());
        model.addAttribute("years", songService.getAllYears());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("totalItems", songPage.getTotalElements());
        model.addAttribute("menu", "song");

        // Filter parameters
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedArtists", artists);
        model.addAttribute("selectedGenreId", genreId);
        model.addAttribute("selectedUseYn", useYn);
        model.addAttribute("selectedIsSolo", isSolo);
        model.addAttribute("selectedReleaseYear", releaseYear);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("viewMode", viewMode);

        return "admin/song/list";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        return songService.findById(id)
                .map(song -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", song.getId());
                    result.put("title", song.getTitle());
                    result.put("artist", song.getArtist());
                    result.put("filePath", song.getFilePath());
                    result.put("youtubeVideoId", song.getYoutubeVideoId());
                    result.put("startTime", song.getStartTime());
                    result.put("playDuration", song.getPlayDuration());
                    result.put("genreId", song.getGenre() != null ? song.getGenre().getId() : null);
                    result.put("releaseYear", song.getReleaseYear());
                    result.put("isSolo", song.getIsSolo());
                    result.put("isPopular", song.getIsPopular());
                    result.put("useYn", song.getUseYn());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(
            @RequestParam(required = false) Long id,
            @RequestParam String title,
            @RequestParam String artist,
            @RequestParam(required = false) String youtubeUrl,
            @RequestParam(required = false) Integer startTime,
            @RequestParam(required = false) Integer playDuration,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer releaseYear,
            @RequestParam(required = false) Boolean isSolo,
            @RequestParam(required = false) Boolean isPopular,
            @RequestParam String useYn) {

        Map<String, Object> result = new HashMap<>();
        try {
            Song song;
            if (id != null) {
                song = songService.findById(id).orElse(new Song());
            } else {
                song = new Song();
            }

            song.setTitle(title);
            song.setArtist(artist);
            song.setStartTime(startTime != null ? startTime : 0);
            song.setPlayDuration(playDuration != null ? playDuration : 10);
            song.setReleaseYear(releaseYear);
            song.setIsSolo(isSolo);
            song.setIsPopular(isPopular != null ? isPopular : true);
            song.setUseYn(useYn);

            if (genreId != null) {
                genreService.findById(genreId).ifPresent(song::setGenre);
            } else {
                song.setGenre(null);
            }

            // YouTube URL 처리
            if (youtubeUrl != null && !youtubeUrl.trim().isEmpty()) {
                String videoId = extractYoutubeVideoId(youtubeUrl.trim());
                if (videoId != null) {
                    song.setYoutubeVideoId(videoId);
                } else {
                    result.put("success", false);
                    result.put("message", "유효하지 않은 YouTube URL입니다.");
                    return ResponseEntity.ok(result);
                }
            }

            songService.save(song);
            result.put("success", true);
            result.put("message", "저장되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * YouTube URL에서 Video ID 추출
     * 지원 형식:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     * - https://www.youtube.com/shorts/VIDEO_ID
     */
    private String extractYoutubeVideoId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // 이미 Video ID 형식인 경우 (11자리 영문숫자)
        if (url.matches("^[a-zA-Z0-9_-]{11}$")) {
            return url;
        }

        Pattern pattern = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        );
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            songService.findById(id).ifPresent(song -> {
                if (song.getFilePath() != null) {
                    songService.deleteFile(song.getFilePath());
                }
            });
            songService.deleteById(id);
            result.put("success", true);
            result.put("message", "삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleUseYn(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            songService.toggleUseYn(id);
            result.put("success", true);
            result.put("message", "상태가 변경되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "상태 변경 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/togglePopular/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> togglePopular(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            songService.togglePopular(id);
            Song song = songService.findById(id).orElse(null);
            result.put("success", true);
            result.put("isPopular", song != null ? song.getIsPopular() : null);
            result.put("message", song != null && Boolean.TRUE.equals(song.getIsPopular()) ? "대중곡으로 변경되었습니다." : "매니악으로 변경되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "상태 변경 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }
}