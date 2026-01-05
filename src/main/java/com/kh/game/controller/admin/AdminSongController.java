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
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/song")
@RequiredArgsConstructor
public class AdminSongController {

    private final SongService songService;
    private final GenreService genreService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        System.out.println("adminSongController!!");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Song> songPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            songPage = songService.search(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            songPage = songService.findAll(pageable);
        }

        model.addAttribute("songs", songPage.getContent());
        model.addAttribute("genres", genreService.findActiveGenres());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("totalItems", songPage.getTotalElements());
        model.addAttribute("menu", "song");

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
                    result.put("startTime", song.getStartTime());
                    result.put("playDuration", song.getPlayDuration());
                    result.put("genreId", song.getGenre() != null ? song.getGenre().getId() : null);
                    result.put("releaseYear", song.getReleaseYear());
                    result.put("isSolo", song.getIsSolo());
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
            @RequestParam(required = false) Integer startTime,
            @RequestParam(required = false) Integer playDuration,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer releaseYear,
            @RequestParam(required = false) Boolean isSolo,
            @RequestParam String useYn,
            @RequestParam(required = false) MultipartFile mp3File) {

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
            song.setUseYn(useYn);

            if (genreId != null) {
                genreService.findById(genreId).ifPresent(song::setGenre);
            } else {
                song.setGenre(null);
            }

            if (mp3File != null && !mp3File.isEmpty()) {
                String filePath = songService.saveFile(mp3File);
                if (id != null && song.getFilePath() != null) {
                    songService.deleteFile(song.getFilePath());
                }
                song.setFilePath(filePath);
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
}