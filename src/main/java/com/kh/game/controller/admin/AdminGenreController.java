package com.kh.game.controller.admin;

import com.kh.game.entity.Genre;
import com.kh.game.service.GenreService;
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
@RequestMapping("/admin/genre")
@RequiredArgsConstructor
public class AdminGenreController {

    private final GenreService genreService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "displayOrder") String sort,
                       @RequestParam(defaultValue = "asc") String direction,
                       Model model) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Genre> genrePage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            genrePage = genreService.search(keyword, pageable);
        } else {
            genrePage = genreService.findAll(pageable);
        }

        model.addAttribute("genres", genrePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", genrePage.getTotalPages());
        model.addAttribute("totalItems", genrePage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("menu", "genre");

        return "admin/genre/list";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<Genre> detail(@PathVariable Long id) {
        return genreService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    @ResponseBody
    public ResponseEntity<List<Genre>> getActiveGenres() {
        return ResponseEntity.ok(genreService.findActiveGenres());
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(@RequestBody Genre genre) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (genre.getId() == null && genreService.existsByCode(genre.getCode())) {
                result.put("success", false);
                result.put("message", "이미 존재하는 코드입니다.");
                return ResponseEntity.ok(result);
            }

            genreService.save(genre);
            result.put("success", true);
            result.put("message", "저장되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "저장 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            genreService.deleteById(id);
            result.put("success", true);
            result.put("message", "삭제되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "삭제 중 오류가 발생했습니다. 해당 장르를 사용하는 노래가 있을 수 있습니다.");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleUseYn(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            genreService.toggleUseYn(id);
            result.put("success", true);
            result.put("message", "상태가 변경되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "상태 변경 중 오류가 발생했습니다.");
        }
        return ResponseEntity.ok(result);
    }
}