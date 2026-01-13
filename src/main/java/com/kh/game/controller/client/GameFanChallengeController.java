package com.kh.game.controller.client;

import com.kh.game.entity.FanChallengeRecord;
import com.kh.game.entity.GameRound;
import com.kh.game.entity.GameSession;
import com.kh.game.entity.Member;
import com.kh.game.service.FanChallengeService;
import com.kh.game.service.MemberService;
import com.kh.game.service.SongService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/game/fan-challenge")
@RequiredArgsConstructor
@Slf4j
public class GameFanChallengeController {

    private final FanChallengeService fanChallengeService;
    private final SongService songService;
    private final MemberService memberService;

    /**
     * 설정 페이지
     */
    @GetMapping
    public String setup(HttpSession httpSession, Model model) {
        // 로그인 상태 확인
        Long memberId = (Long) httpSession.getAttribute("memberId");
        if (memberId != null) {
            memberService.findById(memberId).ifPresent(member -> {
                model.addAttribute("member", member);
                model.addAttribute("nickname", member.getNickname());
            });
        }

        return "client/game/fan-challenge/setup";
    }

    /**
     * 아티스트 목록 조회 (곡 수 포함)
     */
    @GetMapping("/artists")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtistsWithCount() {
        List<Map<String, Object>> artists = songService.getArtistsWithCount();
        // 최소 1곡 이상인 아티스트만 필터링
        artists.removeIf(artist -> ((Number) artist.get("count")).intValue() < 1);
        return ResponseEntity.ok(artists);
    }

    /**
     * 아티스트 검색
     */
    @GetMapping("/artists/search")
    @ResponseBody
    public ResponseEntity<List<String>> searchArtists(@RequestParam String keyword) {
        List<String> artists = songService.searchArtists(keyword);
        return ResponseEntity.ok(artists);
    }

    /**
     * 게임 시작
     */
    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startChallenge(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        try {
            String nickname = (String) request.get("nickname");
            String artist = (String) request.get("artist");

            if (nickname == null || nickname.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "닉네임을 입력해주세요");
                return ResponseEntity.badRequest().body(result);
            }

            if (artist == null || artist.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "아티스트를 선택해주세요");
                return ResponseEntity.badRequest().body(result);
            }

            // 곡 수 확인
            int songCount = songService.getSongCountByArtist(artist);
            if (songCount < 1) {
                result.put("success", false);
                result.put("message", "해당 아티스트의 곡이 없습니다");
                return ResponseEntity.badRequest().body(result);
            }

            // 로그인 회원 확인
            Member member = null;
            Long memberId = (Long) httpSession.getAttribute("memberId");
            if (memberId != null) {
                member = memberService.findById(memberId).orElse(null);
            }

            // 게임 세션 생성
            GameSession session = fanChallengeService.startChallenge(member, nickname.trim(), artist);

            // HTTP 세션에 저장
            httpSession.setAttribute("fanChallengeSessionId", session.getId());
            httpSession.setAttribute("fanChallengeNickname", nickname.trim());
            httpSession.setAttribute("fanChallengeArtist", artist);

            result.put("success", true);
            result.put("sessionId", session.getId());
            result.put("artist", artist);
            result.put("totalRounds", session.getTotalRounds());
            result.put("remainingLives", session.getRemainingLives());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("팬 챌린지 시작 오류", e);
            result.put("success", false);
            result.put("message", "게임 시작 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 게임 진행 페이지
     */
    @GetMapping("/play")
    public String play(HttpSession httpSession, Model model) {
        Long sessionId = (Long) httpSession.getAttribute("fanChallengeSessionId");
        if (sessionId == null) {
            return "redirect:/game/fan-challenge";
        }

        String nickname = (String) httpSession.getAttribute("fanChallengeNickname");
        String artist = (String) httpSession.getAttribute("fanChallengeArtist");

        model.addAttribute("sessionId", sessionId);
        model.addAttribute("nickname", nickname);
        model.addAttribute("artist", artist);

        return "client/game/fan-challenge/play";
    }

    /**
     * 라운드 정보 조회
     */
    @GetMapping("/round/{roundNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRound(
            @PathVariable int roundNumber,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        Long sessionId = (Long) httpSession.getAttribute("fanChallengeSessionId");
        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 만료되었습니다");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            GameSession session = fanChallengeService.getSession(sessionId);
            if (session == null) {
                result.put("success", false);
                result.put("message", "세션을 찾을 수 없습니다");
                return ResponseEntity.badRequest().body(result);
            }

            GameRound round = session.getRounds().stream()
                    .filter(r -> r.getRoundNumber() == roundNumber)
                    .findFirst()
                    .orElse(null);

            if (round == null) {
                result.put("success", false);
                result.put("message", "라운드를 찾을 수 없습니다");
                return ResponseEntity.badRequest().body(result);
            }

            result.put("success", true);
            result.put("roundNumber", roundNumber);
            result.put("totalRounds", session.getTotalRounds());
            result.put("remainingLives", session.getRemainingLives());
            result.put("correctCount", session.getCorrectCount());

            // 노래 정보
            Map<String, Object> songInfo = new HashMap<>();
            if (round.getSong().getYoutubeVideoId() != null) {
                songInfo.put("youtubeVideoId", round.getSong().getYoutubeVideoId());
            }
            if (round.getSong().getFilePath() != null) {
                songInfo.put("filePath", round.getSong().getFilePath());
            }
            songInfo.put("startTime", round.getPlayStartTime() != null ? round.getPlayStartTime() : 0);
            songInfo.put("playDuration", round.getPlayDuration() != null ? round.getPlayDuration() : 30);
            result.put("song", songInfo);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("라운드 정보 조회 오류", e);
            result.put("success", false);
            result.put("message", "라운드 정보 조회 중 오류가 발생했습니다");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 정답 제출
     */
    @PostMapping("/answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        Long sessionId = (Long) httpSession.getAttribute("fanChallengeSessionId");
        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 만료되었습니다");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            int roundNumber = ((Number) request.get("roundNumber")).intValue();
            String answer = (String) request.get("answer");
            long answerTimeMs = ((Number) request.get("answerTimeMs")).longValue();

            FanChallengeService.AnswerResult answerResult =
                    fanChallengeService.processAnswer(sessionId, roundNumber, answer, answerTimeMs);

            result.put("success", true);
            result.put("isCorrect", answerResult.isCorrect());
            result.put("isTimeout", answerResult.isTimeout());
            result.put("correctAnswer", answerResult.correctAnswer());
            result.put("remainingLives", answerResult.remainingLives());
            result.put("correctCount", answerResult.correctCount());
            result.put("completedRounds", answerResult.completedRounds());
            result.put("totalRounds", answerResult.totalRounds());
            result.put("isGameOver", answerResult.isGameOver());
            result.put("gameOverReason", answerResult.gameOverReason());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("정답 제출 오류", e);
            result.put("success", false);
            result.put("message", "정답 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 시간 초과 처리
     */
    @PostMapping("/timeout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleTimeout(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        Long sessionId = (Long) httpSession.getAttribute("fanChallengeSessionId");
        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 만료되었습니다");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            int roundNumber = ((Number) request.get("roundNumber")).intValue();

            FanChallengeService.AnswerResult answerResult =
                    fanChallengeService.processTimeout(sessionId, roundNumber);

            result.put("success", true);
            result.put("isCorrect", false);
            result.put("isTimeout", true);
            result.put("correctAnswer", answerResult.correctAnswer());
            result.put("remainingLives", answerResult.remainingLives());
            result.put("correctCount", answerResult.correctCount());
            result.put("completedRounds", answerResult.completedRounds());
            result.put("totalRounds", answerResult.totalRounds());
            result.put("isGameOver", answerResult.isGameOver());
            result.put("gameOverReason", answerResult.gameOverReason());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("시간 초과 처리 오류", e);
            result.put("success", false);
            result.put("message", "시간 초과 처리 중 오류가 발생했습니다");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 결과 페이지
     */
    @GetMapping("/result")
    public String result(HttpSession httpSession, Model model) {
        Long sessionId = (Long) httpSession.getAttribute("fanChallengeSessionId");
        if (sessionId == null) {
            return "redirect:/game/fan-challenge";
        }

        GameSession session = fanChallengeService.getSession(sessionId);
        if (session == null) {
            return "redirect:/game/fan-challenge";
        }

        String artist = (String) httpSession.getAttribute("fanChallengeArtist");

        model.addAttribute("session", session);
        model.addAttribute("artist", artist);
        model.addAttribute("correctCount", session.getCorrectCount());
        model.addAttribute("totalRounds", session.getTotalRounds());
        model.addAttribute("isPerfectClear", session.getCorrectCount().equals(session.getTotalRounds()));
        model.addAttribute("playTimeSeconds", session.getPlayTimeSeconds());

        // 아티스트 랭킹
        List<FanChallengeRecord> ranking = fanChallengeService.getArtistRanking(artist, 10);
        model.addAttribute("ranking", ranking);

        // 내 기록
        Long memberId = (Long) httpSession.getAttribute("memberId");
        if (memberId != null) {
            memberService.findById(memberId).ifPresent(member -> {
                fanChallengeService.getMemberRecord(member, artist).ifPresent(record -> {
                    model.addAttribute("myRecord", record);
                });
            });
        }

        // 세션 정리
        httpSession.removeAttribute("fanChallengeSessionId");
        httpSession.removeAttribute("fanChallengeNickname");
        httpSession.removeAttribute("fanChallengeArtist");

        return "client/game/fan-challenge/result";
    }

    /**
     * 아티스트별 랭킹 조회
     */
    @GetMapping("/ranking/{artist}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtistRanking(
            @PathVariable String artist) {

        List<FanChallengeRecord> records = fanChallengeService.getArtistRanking(artist, 20);

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (FanChallengeRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("nickname", record.getMember().getNickname());
            item.put("correctCount", record.getCorrectCount());
            item.put("totalSongs", record.getTotalSongs());
            item.put("isPerfectClear", record.getIsPerfectClear());
            item.put("bestTimeMs", record.getBestTimeMs());
            item.put("achievedAt", record.getAchievedAt());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 게임 포기
     */
    @PostMapping("/end")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endGame(HttpSession httpSession) {
        Map<String, Object> result = new HashMap<>();

        // 세션 정리
        httpSession.removeAttribute("fanChallengeSessionId");
        httpSession.removeAttribute("fanChallengeNickname");
        httpSession.removeAttribute("fanChallengeArtist");

        result.put("success", true);
        return ResponseEntity.ok(result);
    }
}
