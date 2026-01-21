package com.kh.game.controller.admin;

import com.kh.game.entity.Genre;
import com.kh.game.entity.GenreChallengeDifficulty;
import com.kh.game.entity.GenreChallengeRecord;
import com.kh.game.repository.GenreChallengeRecordRepository;
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

@Controller
@RequestMapping("/admin/genre-challenge")
@RequiredArgsConstructor
public class AdminGenreChallengeController {

    private final GenreChallengeRecordRepository genreChallengeRecordRepository;
    private final GenreService genreService;
    private final SongService songService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String genreCode,
                       @RequestParam(required = false) String difficulty,
                       @RequestParam(defaultValue = "achievedAt") String sort,
                       @RequestParam(defaultValue = "desc") String direction,
                       Model model) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<GenreChallengeRecord> recordPage = getFilteredRecords(keyword, genreCode, difficulty, pageable);

        // 통계
        long totalCount = genreChallengeRecordRepository.count();
        long activeGenreCount = genreChallengeRecordRepository.countDistinctGenres();
        long todayCount = genreChallengeRecordRepository.countTodayRecords();

        // 평균 클리어율 계산
        double avgClearRate = 0.0;
        List<GenreChallengeRecord> allRecords = genreChallengeRecordRepository.findAll();
        if (!allRecords.isEmpty()) {
            avgClearRate = allRecords.stream()
                    .mapToDouble(GenreChallengeRecord::getClearRate)
                    .average()
                    .orElse(0.0);
        }

        // 장르 목록 (드롭다운용)
        List<Genre> genres = genreService.findActiveGenres();

        model.addAttribute("records", recordPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", recordPage.getTotalPages());
        model.addAttribute("totalItems", recordPage.getTotalElements());
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("activeGenreCount", activeGenreCount);
        model.addAttribute("todayCount", todayCount);
        model.addAttribute("avgClearRate", avgClearRate);
        model.addAttribute("genres", genres);
        model.addAttribute("keyword", keyword);
        model.addAttribute("genreCode", genreCode);
        model.addAttribute("difficulty", difficulty);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("menu", "genre-challenge");

        return "admin/genre-challenge/list";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        return genreChallengeRecordRepository.findById(id)
                .map(record -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", record.getId());
                    result.put("memberId", record.getMember().getId());
                    result.put("memberNickname", record.getMember().getNickname());
                    result.put("memberEmail", record.getMember().getEmail());
                    result.put("genreCode", record.getGenre().getCode());
                    result.put("genreName", record.getGenre().getName());
                    result.put("difficulty", record.getDifficulty().name());
                    result.put("difficultyDisplayName", record.getDifficulty().getDisplayName());
                    result.put("correctCount", record.getCorrectCount());
                    result.put("totalSongs", record.getTotalSongs());
                    result.put("clearRate", String.format("%.1f", record.getClearRate()));
                    result.put("maxCombo", record.getMaxCombo());
                    result.put("bestTimeMs", record.getBestTimeMs());
                    result.put("achievedAt", record.getAchievedAt());
                    result.put("createdAt", record.getCreatedAt());

                    // 현재 장르 곡 수
                    int currentSongCount = songService.getSongCountByGenreCode(record.getGenre().getCode());
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
    public ResponseEntity<List<Map<String, Object>>> getGenreStats() {
        List<Object[]> statistics = genreChallengeRecordRepository.getGenreStatistics();

        List<Map<String, Object>> result = statistics.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("genreCode", row[0]);
                    item.put("genreName", row[1]);
                    item.put("challengeCount", row[2]);
                    item.put("maxCorrectCount", row[3]);

                    // 현재 곡 수
                    String genreCode = (String) row[0];
                    int songCount = songService.getSongCountByGenreCode(genreCode);
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
        Page<GenreChallengeRecord> recordPage = genreChallengeRecordRepository.findByMemberId(memberId, pageable);

        List<Map<String, Object>> records = recordPage.getContent().stream()
                .map(record -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", record.getId());
                    item.put("genreCode", record.getGenre().getCode());
                    item.put("genreName", record.getGenre().getName());
                    item.put("difficulty", record.getDifficulty().name());
                    item.put("difficultyDisplayName", record.getDifficulty().getDisplayName());
                    item.put("correctCount", record.getCorrectCount());
                    item.put("totalSongs", record.getTotalSongs());
                    item.put("clearRate", String.format("%.1f", record.getClearRate()));
                    item.put("maxCombo", record.getMaxCombo());
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

    private Page<GenreChallengeRecord> getFilteredRecords(String keyword, String genreCode, String difficulty, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasGenre = genreCode != null && !genreCode.isEmpty();
        boolean hasDifficulty = difficulty != null && !difficulty.isEmpty();

        Genre genre = hasGenre ? genreService.findByCode(genreCode).orElse(null) : null;
        GenreChallengeDifficulty diff = hasDifficulty ? GenreChallengeDifficulty.fromString(difficulty) : null;

        // 장르가 지정되었지만 찾을 수 없는 경우
        if (hasGenre && genre == null) {
            return Page.empty(pageable);
        }

        // 복합 필터 조합
        if (hasKeyword && hasGenre && hasDifficulty) {
            return genreChallengeRecordRepository.findByMemberNicknameContainingAndGenreAndDifficulty(keyword, genre, diff, pageable);
        } else if (hasKeyword && hasGenre) {
            return genreChallengeRecordRepository.findByMemberNicknameContainingAndGenre(keyword, genre, pageable);
        } else if (hasKeyword && hasDifficulty) {
            return genreChallengeRecordRepository.findByMemberNicknameContainingAndDifficulty(keyword, diff, pageable);
        } else if (hasGenre && hasDifficulty) {
            return genreChallengeRecordRepository.findByGenreAndDifficulty(genre, diff, pageable);
        } else if (hasKeyword) {
            return genreChallengeRecordRepository.findByMemberNicknameContaining(keyword, pageable);
        } else if (hasGenre) {
            return genreChallengeRecordRepository.findByGenre(genre, pageable);
        } else if (hasDifficulty) {
            return genreChallengeRecordRepository.findByDifficulty(diff, pageable);
        } else {
            return genreChallengeRecordRepository.findAllWithMemberAndGenre(pageable);
        }
    }
}
