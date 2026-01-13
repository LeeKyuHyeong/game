package com.kh.game.controller.admin;

import com.kh.game.entity.Song;
import com.kh.game.entity.SongAnswer;
import com.kh.game.repository.SongAnswerRepository;
import com.kh.game.service.SongService;
import com.kh.game.util.AnswerGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/answer")
@RequiredArgsConstructor
public class AdminAnswerController {

    private final SongService songService;
    private final SongAnswerRepository songAnswerRepository;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Song> songs;

        if (keyword != null && !keyword.trim().isEmpty()) {
            songs = songService.search(keyword, pageable);
        } else {
            songs = songService.findAll(pageable);
        }

        Map<Long, Long> answerCounts = new HashMap<>();
        Map<Long, List<SongAnswer>> songAnswersMap = new HashMap<>();
        Map<Long, SongAnswer> primaryAnswerMap = new HashMap<>();
        for (Song song : songs.getContent()) {
            List<SongAnswer> answers = songAnswerRepository.findBySongIdOrderByIsPrimaryDesc(song.getId());
            answerCounts.put(song.getId(), (long) answers.size());
            songAnswersMap.put(song.getId(), answers);
            SongAnswer primary = answers.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsPrimary()))
                    .findFirst()
                    .orElse(answers.isEmpty() ? null : answers.get(0));
            primaryAnswerMap.put(song.getId(), primary);
        }

        model.addAttribute("songs", songs);
        model.addAttribute("answerCounts", answerCounts);
        model.addAttribute("songAnswersMap", songAnswersMap);
        model.addAttribute("primaryAnswerMap", primaryAnswerMap);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", songs.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("menu", "answer");

        return "admin/answer/list";
    }

    @GetMapping("/song/{songId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAnswers(@PathVariable Long songId) {
        Map<String, Object> result = new HashMap<>();

        Song song = songService.findById(songId).orElse(null);
        if (song == null) {
            result.put("success", false);
            result.put("message", "노래를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        List<SongAnswer> answers = songAnswerRepository.findBySongIdOrderByIsPrimaryDesc(songId);

        result.put("success", true);

        Map<String, Object> songInfo = new HashMap<>();
        songInfo.put("id", song.getId());
        songInfo.put("title", song.getTitle() != null ? song.getTitle() : "");
        songInfo.put("artist", song.getArtist() != null ? song.getArtist() : "");
        result.put("song", songInfo);

        result.put("answers", answers.stream().map(a -> {
            Map<String, Object> answerInfo = new HashMap<>();
            answerInfo.put("id", a.getId());
            answerInfo.put("answer", a.getAnswer() != null ? a.getAnswer() : "");
            answerInfo.put("isPrimary", Boolean.TRUE.equals(a.getIsPrimary()));
            return answerInfo;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addAnswer(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            Long songId = Long.valueOf(request.get("songId").toString());
            String answerText = (String) request.get("answer");
            Boolean isPrimary = request.get("isPrimary") != null && (Boolean) request.get("isPrimary");

            if (answerText == null || answerText.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "정답을 입력해주세요.");
                return ResponseEntity.ok(result);
            }

            Song song = songService.findById(songId).orElse(null);
            if (song == null) {
                result.put("success", false);
                result.put("message", "노래를 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            if (isPrimary) {
                List<SongAnswer> existingAnswers = songAnswerRepository.findBySongId(songId);
                for (SongAnswer existing : existingAnswers) {
                    if (existing.getIsPrimary()) {
                        existing.setIsPrimary(false);
                        songAnswerRepository.save(existing);
                    }
                }
            }

            SongAnswer answer = new SongAnswer(song, answerText.trim(), isPrimary);
            songAnswerRepository.save(answer);

            result.put("success", true);
            result.put("message", "정답이 추가되었습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "정답 추가 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/update/{answerId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateAnswer(
            @PathVariable Long answerId,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> result = new HashMap<>();

        try {
            String answerText = (String) request.get("answer");
            Boolean isPrimary = request.get("isPrimary") != null && (Boolean) request.get("isPrimary");

            SongAnswer answer = songAnswerRepository.findById(answerId).orElse(null);
            if (answer == null) {
                result.put("success", false);
                result.put("message", "정답을 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            if (answerText != null && !answerText.trim().isEmpty()) {
                answer.setAnswer(answerText.trim());
            }

            if (isPrimary && !answer.getIsPrimary()) {
                List<SongAnswer> existingAnswers = songAnswerRepository.findBySongId(answer.getSong().getId());
                for (SongAnswer existing : existingAnswers) {
                    if (existing.getIsPrimary()) {
                        existing.setIsPrimary(false);
                        songAnswerRepository.save(existing);
                    }
                }
            }
            answer.setIsPrimary(isPrimary);

            songAnswerRepository.save(answer);

            result.put("success", true);
            result.put("message", "정답이 수정되었습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "정답 수정 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete/{answerId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAnswer(@PathVariable Long answerId) {
        Map<String, Object> result = new HashMap<>();

        try {
            SongAnswer answer = songAnswerRepository.findById(answerId).orElse(null);
            if (answer == null) {
                result.put("success", false);
                result.put("message", "정답을 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            songAnswerRepository.delete(answer);

            result.put("success", true);
            result.put("message", "정답이 삭제되었습니다.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "정답 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 개선된 자동 생성 - 영어→한글 발음 포함 다양한 변형 생성
     */
    @PostMapping("/auto-generate/{songId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> autoGenerateAnswers(@PathVariable Long songId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Song song = songService.findById(songId).orElse(null);
            if (song == null) {
                result.put("success", false);
                result.put("message", "노래를 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            // 이미 등록된 정답들 조회
            List<SongAnswer> existingAnswers = songAnswerRepository.findBySongId(songId);
            Set<String> existingNormalized = existingAnswers.stream()
                    .map(a -> normalizeForComparison(a.getAnswer()))
                    .collect(Collectors.toSet());

            // 정답 변형 생성
            Set<String> variants = AnswerGeneratorUtil.generateAnswerVariants(song.getTitle());

            int count = 0;
            boolean isFirst = existingAnswers.isEmpty();

            for (String variant : variants) {
                String normalized = normalizeForComparison(variant);

                // 중복 체크
                if (existingNormalized.contains(normalized)) {
                    continue;
                }

                // 저장
                SongAnswer answer = new SongAnswer(song, variant, isFirst);
                songAnswerRepository.save(answer);
                existingNormalized.add(normalized);

                count++;
                isFirst = false;
            }

            if (count == 0) {
                result.put("success", true);
                result.put("message", "추가할 새로운 정답 변형이 없습니다.");
            } else {
                result.put("success", true);
                result.put("message", count + "개의 정답이 자동 생성되었습니다.");
            }
            result.put("count", count);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "자동 생성 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 비교용 정규화 (중복 체크용)
     */
    private String normalizeForComparison(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9가-힣]", "");
    }
}