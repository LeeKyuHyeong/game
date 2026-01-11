package com.kh.game.controller.client;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.*;
import com.kh.game.service.GameSessionService;
import com.kh.game.service.GenreService;
import com.kh.game.service.MemberService;
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
    private final MemberService memberService;

    @GetMapping
    public String setup(Model model, HttpSession httpSession) {
        model.addAttribute("genres", genreService.findActiveGenres());
        // 로그인 상태 전달
        Boolean isLoggedIn = (Boolean) httpSession.getAttribute("isLoggedIn");
        String memberNickname = (String) httpSession.getAttribute("memberNickname");
        model.addAttribute("isLoggedIn", isLoggedIn != null && isLoggedIn);
        model.addAttribute("memberNickname", memberNickname);
        return "client/game/guess/setup";
    }

    @GetMapping("/song-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSongCount(
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String artistName,
            @RequestParam(required = false) List<String> artists,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) List<Integer> years,
            @RequestParam(required = false) Boolean soloOnly,
            @RequestParam(required = false) Boolean groupOnly) {

        Map<String, Object> result = new HashMap<>();

        GameSettings settings = new GameSettings();
        settings.setFixedGenreId(genreId);
        settings.setFixedArtistName(artistName);
        settings.setYearFrom(yearFrom);
        settings.setYearTo(yearTo);
        settings.setSoloOnly(soloOnly);
        settings.setGroupOnly(groupOnly);

        // 복수 선택 (새로운 방식)
        if (years != null && !years.isEmpty()) {
            settings.setSelectedYears(years);
        }
        if (artists != null && !artists.isEmpty()) {
            settings.setSelectedArtists(artists);
        }

        int count = songService.getAvailableSongCount(settings);
        result.put("count", count);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/years")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getYears() {
        List<Map<String, Object>> years = songService.getYearsWithCount();
        return ResponseEntity.ok(years);
    }

    @GetMapping("/artists")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtists() {
        List<Map<String, Object>> artists = songService.getArtistsWithCount();
        return ResponseEntity.ok(artists);
    }

    @GetMapping("/artists/search")
    @ResponseBody
    public ResponseEntity<List<String>> searchArtists(@RequestParam String keyword) {
        List<String> artists = songService.searchArtists(keyword);
        return ResponseEntity.ok(artists);
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

    @GetMapping("/artists-with-count")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtistsWithCount(HttpSession httpSession) {
        @SuppressWarnings("unchecked")
        List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("guessPlayedSongIds");
        if (playedSongIds == null) {
            playedSongIds = new ArrayList<>();
        }

        List<Map<String, Object>> artists = songService.getArtistsWithCountExcluding(playedSongIds);
        return ResponseEntity.ok(artists);
    }

    @GetMapping("/years-with-count")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getYearsWithCount(HttpSession httpSession) {
        @SuppressWarnings("unchecked")
        List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("guessPlayedSongIds");
        if (playedSongIds == null) {
            playedSongIds = new ArrayList<>();
        }

        List<Map<String, Object>> years = songService.getYearsWithCountExcluding(playedSongIds);
        return ResponseEntity.ok(years);
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

            // 로그인한 회원인 경우 연결
            Long memberId = (Long) httpSession.getAttribute("memberId");
            if (memberId != null) {
                memberService.findById(memberId).ifPresent(session::setMember);
            }

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
                // 복수 연도 선택
                if (settingsMap.get("selectedYears") != null) {
                    @SuppressWarnings("unchecked")
                    List<Integer> selectedYears = (List<Integer>) settingsMap.get("selectedYears");
                    settings.setSelectedYears(selectedYears);
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
                if (settingsMap.get("fixedArtistName") != null) {
                    settings.setFixedArtistName((String) settingsMap.get("fixedArtistName"));
                }
                // 복수 아티스트 선택
                if (settingsMap.get("selectedArtists") != null) {
                    @SuppressWarnings("unchecked")
                    List<String> selectedArtists = (List<String>) settingsMap.get("selectedArtists");
                    settings.setSelectedArtists(selectedArtists);
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

            // 매 라운드 선택 모드가 아닌 경우에만 미리 라운드 생성
            if (!gameMode.contains("PER_ROUND")) {
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

        GameSession session = gameSessionService.findById(sessionId).orElse(null);
        if (session == null) {
            return "redirect:/game/solo/guess";
        }

        String nickname = (String) httpSession.getAttribute("guessNickname");
        String gameMode = (String) httpSession.getAttribute("guessGameMode");

        model.addAttribute("gameSession", session);
        model.addAttribute("nickname", nickname);
        model.addAttribute("settings", gameSessionService.parseSettings(session.getSettings()));
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

    @PostMapping("/select-artist")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectArtistForRound(
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
            String artist = (String) request.get("artist");
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

            Song song = songService.getRandomSongByArtistExcluding(artist, playedSongIds);

            if (song == null) {
                result.put("success", false);
                result.put("message", "해당 아티스트의 사용 가능한 노래가 없습니다.");
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
            result.put("message", "아티스트 선택 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/select-year")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectYearForRound(
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
            Integer year = (Integer) request.get("year");
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

            Song song = songService.getRandomSongByYearExcluding(year, playedSongIds);

            if (song == null) {
                result.put("success", false);
                result.put("message", "해당 연도의 사용 가능한 노래가 없습니다.");
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
            result.put("message", "연도 선택 중 오류가 발생했습니다: " + e.getMessage());
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

            // 시간 기반 점수 계산 (초 단위)
            // 3초 이내: 100점, 5초 이내: 90점, 10초 이내: 80점, 이후: 70점
            Double answerTimeSec = request.get("answerTime") != null ?
                    Double.parseDouble(request.get("answerTime").toString()) : null;
            int maxAttempts = 3;

            if (isSkip) {
                // 스킵 처리
                round.setStatus(GameRound.RoundStatus.SKIPPED);
                round.setIsCorrect(false);
                session.setSkipCount(session.getSkipCount() + 1);
                session.setCompletedRounds(session.getCompletedRounds() + 1);

                result.put("isCorrect", false);
                result.put("isRoundOver", true);
                result.put("remainingAttempts", 0);

            } else {
                // 시도 횟수 증가
                int currentAttempt = (round.getAttemptCount() == null ? 0 : round.getAttemptCount()) + 1;
                round.setAttemptCount(currentAttempt);

                // 정답 체크
                boolean isCorrect = songService.checkAnswer(song.getId(), userAnswer);

                // 시도 기록 저장
                GameRoundAttempt attempt = new GameRoundAttempt(round, currentAttempt, userAnswer, isCorrect);
                round.getAttempts().add(attempt);

                if (isCorrect) {
                    // 정답!
                    round.setStatus(GameRound.RoundStatus.ANSWERED);
                    round.setUserAnswer(userAnswer);
                    round.setIsCorrect(true);

                    // 시간 기반 점수 계산
                    int earnedScore = calculateScoreByTime(answerTimeSec);
                    round.setScore(earnedScore);
                    if (answerTimeSec != null) {
                        round.setAnswerTimeMs((long) (answerTimeSec * 1000));
                    }
                    session.setCorrectCount(session.getCorrectCount() + 1);
                    session.setTotalScore(session.getTotalScore() + earnedScore);
                    session.setCompletedRounds(session.getCompletedRounds() + 1);

                    result.put("isCorrect", true);
                    result.put("isRoundOver", true);
                    result.put("earnedScore", earnedScore);
                    result.put("answerTime", answerTimeSec);
                    result.put("attemptCount", currentAttempt);

                } else if (currentAttempt >= maxAttempts) {
                    // 3번 모두 실패
                    round.setStatus(GameRound.RoundStatus.ANSWERED);
                    round.setUserAnswer(userAnswer);
                    round.setIsCorrect(false);
                    round.setScore(0);
                    session.setCompletedRounds(session.getCompletedRounds() + 1);

                    result.put("isCorrect", false);
                    result.put("isRoundOver", true);
                    result.put("remainingAttempts", 0);
                    result.put("attemptCount", currentAttempt);

                } else {
                    // 아직 기회 남음
                    result.put("isCorrect", false);
                    result.put("isRoundOver", false);
                    result.put("remainingAttempts", maxAttempts - currentAttempt);
                    result.put("attemptCount", currentAttempt);
                }
            }

            // 게임 종료 체크
            if (session.getCompletedRounds() >= session.getTotalRounds()) {
                session.setStatus(GameSession.GameStatus.COMPLETED);
                session.setEndedAt(LocalDateTime.now());

                // 회원인 경우 Solo Guess 통계 업데이트
                if (session.getMember() != null) {
                    // 최고기록 랭킹 대상 여부 확인
                    // 조건: 전체랜덤 모드 + 아티스트유형 전체
                    GameSettings settings = gameSessionService.parseSettings(session.getSettings());
                    boolean isRandomMode = session.getGameMode() == GameSession.GameMode.RANDOM;
                    boolean allArtistTypes = !Boolean.TRUE.equals(settings.getSoloOnly())
                                           && !Boolean.TRUE.equals(settings.getGroupOnly());
                    boolean isEligibleForBestScore = isRandomMode && allArtistTypes;

                    memberService.addGuessGameResult(
                            session.getMember().getId(),
                            session.getTotalScore(),
                            session.getCorrectCount(),
                            session.getCompletedRounds(),
                            session.getSkipCount(),
                            isEligibleForBestScore
                    );
                }
            }

            gameSessionService.save(session);

            result.put("success", true);
            result.put("isGameOver", session.getStatus() == GameSession.GameStatus.COMPLETED);
            result.put("completedRounds", session.getCompletedRounds());
            result.put("totalScore", session.getTotalScore());

            // 라운드 종료 시 정답 정보 반환
            boolean isRoundOver = (boolean) result.get("isRoundOver");
            if (isRoundOver) {
                Map<String, Object> answerInfo = new HashMap<>();
                answerInfo.put("title", song.getTitle());
                answerInfo.put("artist", song.getArtist());
                answerInfo.put("releaseYear", song.getReleaseYear());
                if (song.getGenre() != null) {
                    answerInfo.put("genre", song.getGenre().getName());
                }
                result.put("answer", answerInfo);
            }

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

        GameSession session = gameSessionService.findById(sessionId).orElse(null);
        if (session == null) {
            return "redirect:/game/solo/guess";
        }

        String nickname = (String) httpSession.getAttribute("guessNickname");

        model.addAttribute("gameSession", session);
        model.addAttribute("rounds", session.getRounds());
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

    /**
     * 시간 기반 점수 계산
     * 3초 이내: 100점, 5초 이내: 90점, 10초 이내: 80점, 이후: 70점
     */
    private int calculateScoreByTime(Double answerTimeSec) {
        if (answerTimeSec == null) {
            return 70; // 시간 정보가 없으면 기본 점수
        }
        if (answerTimeSec <= 3.0) {
            return 100;
        } else if (answerTimeSec <= 5.0) {
            return 90;
        } else if (answerTimeSec <= 10.0) {
            return 80;
        } else {
            return 70;
        }
    }
}