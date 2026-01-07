package com.kh.game.controller.client;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.*;
import com.kh.game.service.GameSessionService;
import com.kh.game.service.GenreService;
import com.kh.game.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/game/solo/guess")
@RequiredArgsConstructor
public class GameGuessController {

    private final SongService songService;
    private final GenreService genreService;
    private final GameSessionService gameSessionService;

    @GetMapping
    public String setup(Model model) {
        model.addAttribute("genres", genreService.findActiveGenres());
        return "client/game/guess/setup";
    }

    @GetMapping("/song-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSongCount(
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Boolean soloOnly,
            @RequestParam(required = false) Boolean groupOnly) {

        Map<String, Object> result = new HashMap<>();

        GameSettings settings = new GameSettings();
        settings.setFixedGenreId(genreId);
        settings.setYearFrom(yearFrom);
        settings.setYearTo(yearTo);
        settings.setSoloOnly(soloOnly);
        settings.setGroupOnly(groupOnly);

        int count = songService.getAvailableSongCount(settings);
        result.put("count", count);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/genres-with-count")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getGenresWithCount(HttpSession httpSession) {
        List<Map<String, Object>> result = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("guessPlayedSongIds");
        if (playedSongIds == null) {
            playedSongIds = new ArrayList<>();
        }

        for (var genre : genreService.findActiveGenres()) {
            Map<String, Object> genreInfo = new HashMap<>();
            genreInfo.put("id", genre.getId());
            genreInfo.put("name", genre.getName());

            int availableCount = songService.getAvailableSongCountByGenreExcluding(genre.getId(), playedSongIds);
            genreInfo.put("availableCount", availableCount);

            result.add(genreInfo);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startGame(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        try {
            String nickname = (String) request.get("nickname");
            int totalRounds = (int) request.get("totalRounds");
            String gameMode = (String) request.get("gameMode");

            @SuppressWarnings("unchecked")
            Map<String, Object> settingsMap = (Map<String, Object>) request.get("settings");

            // GameSession 생성
            GameSession session = new GameSession();
            session.setSessionUuid(UUID.randomUUID().toString());
            session.setNickname(nickname);
            session.setGameType(GameSession.GameType.SOLO_GUESS);
            session.setGameMode(GameSession.GameMode.valueOf(gameMode));
            session.setTotalRounds(totalRounds);
            session.setStatus(GameSession.GameStatus.PLAYING);

            // Settings JSON 생성
            GameSettings settings = new GameSettings();
            if (settingsMap != null) {
                if (settingsMap.get("timeLimit") != null) {
                    settings.setTimeLimit((Integer) settingsMap.get("timeLimit"));
                }
                if (settingsMap.get("yearFrom") != null) {
                    settings.setYearFrom((Integer) settingsMap.get("yearFrom"));
                }
                if (settingsMap.get("yearTo") != null) {
                    settings.setYearTo((Integer) settingsMap.get("yearTo"));
                }
                if (settingsMap.get("soloOnly") != null) {
                    settings.setSoloOnly((Boolean) settingsMap.get("soloOnly"));
                }
                if (settingsMap.get("groupOnly") != null) {
                    settings.setGroupOnly((Boolean) settingsMap.get("groupOnly"));
                }
                if (settingsMap.get("fixedGenreId") != null) {
                    settings.setFixedGenreId(Long.valueOf(settingsMap.get("fixedGenreId").toString()));
                }
            }
            session.setSettings(gameSessionService.toSettingsJson(settings));

            GameSession savedSession = gameSessionService.save(session);

            // 세션에 정보 저장
            httpSession.setAttribute("guessSessionId", savedSession.getId());
            httpSession.setAttribute("guessNickname", nickname);
            httpSession.setAttribute("guessPlayedSongIds", new ArrayList<Long>());
            httpSession.setAttribute("guessGameMode", gameMode);
            httpSession.setAttribute("guessScore", 0);

            // GENRE_PER_ROUND 모드가 아닌 경우에만 미리 라운드 생성
            if (!"GENRE_PER_ROUND".equals(gameMode)) {
                List<Song> songs = songService.getRandomSongs(totalRounds, settings);

                // 실제 생성된 라운드 수로 업데이트
                int actualRounds = songs.size();
                savedSession.setTotalRounds(actualRounds);

                for (int i = 0; i < songs.size(); i++) {
                    GameRound round = new GameRound();
                    round.setGameSession(savedSession);
                    round.setRoundNumber(i + 1);
                    round.setSong(songs.get(i));
                    round.setGenre(songs.get(i).getGenre());
                    round.setPlayStartTime(songs.get(i).getStartTime());
                    round.setPlayDuration(songs.get(i).getPlayDuration());
                    round.setStatus(GameRound.RoundStatus.WAITING);
                    savedSession.getRounds().add(round);
                }

                gameSessionService.save(savedSession);
            }

            result.put("success", true);
            result.put("sessionId", savedSession.getId());
            result.put("gameMode", gameMode);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "게임 시작 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/play")
    public String play(HttpSession httpSession, Model model) {
        Long sessionId = (Long) httpSession.getAttribute("guessSessionId");
        if (sessionId == null) {
            return "redirect:/game/solo/guess";
        }

        GameSession sessions = gameSessionService.findById(sessionId).orElse(null);
        if (sessions == null) {
            return "redirect:/game/solo/guess";
        }

        String nickname = (String) httpSession.getAttribute("guessNickname");
        String gameMode = (String) httpSession.getAttribute("guessGameMode");

        model.addAttribute("sessions", sessions);
        model.addAttribute("nickname", nickname);
        model.addAttribute("settings", gameSessionService.parseSettings(sessions.getSettings()));
        model.addAttribute("gameMode", gameMode);
        model.addAttribute("genres", genreService.findActiveGenres());

        return "client/game/guess/play";
    }

    @PostMapping("/select-genre")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectGenreForRound(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long sessionId = (Long) httpSession.getAttribute("guessSessionId");

        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            Long genreId = Long.valueOf(request.get("genreId").toString());
            int roundNumber = (int) request.get("roundNumber");

            GameSession session = gameSessionService.findById(sessionId).orElse(null);
            if (session == null) {
                result.put("success", false);
                result.put("message", "게임을 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            @SuppressWarnings("unchecked")
            List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("guessPlayedSongIds");
            if (playedSongIds == null) {
                playedSongIds = new ArrayList<>();
            }

            Song song = songService.getRandomSongByGenreExcluding(genreId, playedSongIds);

            if (song == null) {
                result.put("success", false);
                result.put("message", "해당 장르에 사용 가능한 노래가 없습니다.");
                return ResponseEntity.ok(result);
            }

            GameRound round = new GameRound();
            round.setGameSession(session);
            round.setRoundNumber(roundNumber);
            round.setSong(song);
            round.setGenre(song.getGenre());
            round.setPlayStartTime(song.getStartTime());
            round.setPlayDuration(song.getPlayDuration());
            round.setStatus(GameRound.RoundStatus.WAITING);
            session.getRounds().add(round);

            gameSessionService.save(session);

            playedSongIds.add(song.getId());
            httpSession.setAttribute("guessPlayedSongIds", playedSongIds);

            result.put("success", true);
            result.put("roundNumber", roundNumber);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "장르 선택 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/round/{roundNumber}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRound(
            @PathVariable int roundNumber,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long sessionId = (Long) httpSession.getAttribute("guessSessionId");

        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 없습니다.");
            return ResponseEntity.ok(result);
        }

        GameSession session = gameSessionService.findById(sessionId).orElse(null);
        if (session == null) {
            result.put("success", false);
            result.put("message", "게임을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        GameRound round = session.getRounds().stream()
                .filter(r -> r.getRoundNumber() == roundNumber)
                .findFirst()
                .orElse(null);

        if (round == null) {
            result.put("success", false);
            result.put("message", "라운드를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        result.put("roundNumber", round.getRoundNumber());
        result.put("totalRounds", session.getTotalRounds());

        if (round.getSong() != null) {
            Map<String, Object> songInfo = new HashMap<>();
            songInfo.put("id", round.getSong().getId());
            songInfo.put("filePath", round.getSong().getFilePath());
            songInfo.put("playDuration", round.getPlayDuration());
            if (round.getSong().getGenre() != null) {
                songInfo.put("genre", round.getSong().getGenre().getName());
            }
            result.put("song", songInfo);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long sessionId = (Long) httpSession.getAttribute("guessSessionId");

        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            int roundNumber = (int) request.get("roundNumber");
            String userAnswer = (String) request.get("answer");
            boolean isSkip = request.get("isSkip") != null && (boolean) request.get("isSkip");

            GameSession session = gameSessionService.findById(sessionId).orElse(null);
            if (session == null) {
                result.put("success", false);
                result.put("message", "게임을 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            GameRound round = session.getRounds().stream()
                    .filter(r -> r.getRoundNumber() == roundNumber)
                    .findFirst()
                    .orElse(null);

            if (round == null) {
                result.put("success", false);
                result.put("message", "라운드를 찾을 수 없습니다.");
                return ResponseEntity.ok(result);
            }

            Song song = round.getSong();
            boolean isCorrect = false;

            if (isSkip) {
                round.setStatus(GameRound.RoundStatus.SKIPPED);
                round.setIsCorrect(false);
                session.setSkipCount(session.getSkipCount() + 1);
            } else {
                // 정답 체크
                isCorrect = songService.checkAnswer(song.getId(), userAnswer);

                round.setStatus(GameRound.RoundStatus.ANSWERED);
                round.setUserAnswer(userAnswer);
                round.setIsCorrect(isCorrect);

                if (isCorrect) {
                    round.setScore(100);
                    session.setCorrectCount(session.getCorrectCount() + 1);
                    session.setTotalScore(session.getTotalScore() + 100);
                }
            }

            session.setCompletedRounds(session.getCompletedRounds() + 1);

            // 게임 종료 체크
            if (session.getCompletedRounds() >= session.getTotalRounds()) {
                session.setStatus(GameSession.GameStatus.COMPLETED);
                session.setEndedAt(LocalDateTime.now());
            }

            gameSessionService.save(session);

            result.put("success", true);
            result.put("isCorrect", isCorrect);
            result.put("isGameOver", session.getStatus() == GameSession.GameStatus.COMPLETED);
            result.put("completedRounds", session.getCompletedRounds());

            // 정답 정보 반환
            Map<String, Object> answerInfo = new HashMap<>();
            answerInfo.put("title", song.getTitle());
            answerInfo.put("artist", song.getArtist());
            answerInfo.put("releaseYear", song.getReleaseYear());
            if (song.getGenre() != null) {
                answerInfo.put("genre", song.getGenre().getName());
            }
            result.put("answer", answerInfo);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/result")
    public String result(HttpSession httpSession, Model model) {
        Long sessionId = (Long) httpSession.getAttribute("guessSessionId");
        if (sessionId == null) {
            return "redirect:/game/solo/guess";
        }

        GameSession sessions = gameSessionService.findById(sessionId).orElse(null);
        if (sessions == null) {
            return "redirect:/game/solo/guess";
        }

        String nickname = (String) httpSession.getAttribute("guessNickname");

        model.addAttribute("sessions", sessions);
        model.addAttribute("rounds", sessions.getRounds());
        model.addAttribute("nickname", nickname);

        return "client/game/guess/result";
    }

    @PostMapping("/end")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endGame(HttpSession httpSession) {
        Map<String, Object> result = new HashMap<>();
        Long sessionId = (Long) httpSession.getAttribute("guessSessionId");

        if (sessionId != null) {
            GameSession session = gameSessionService.findById(sessionId).orElse(null);
            if (session != null && session.getStatus() == GameSession.GameStatus.PLAYING) {
                session.setStatus(GameSession.GameStatus.ABANDONED);
                session.setEndedAt(LocalDateTime.now());
                gameSessionService.save(session);
            }
        }

        httpSession.removeAttribute("guessSessionId");
        httpSession.removeAttribute("guessNickname");
        httpSession.removeAttribute("guessPlayedSongIds");
        httpSession.removeAttribute("guessGameMode");
        httpSession.removeAttribute("guessScore");

        result.put("success", true);
        return ResponseEntity.ok(result);
    }
}