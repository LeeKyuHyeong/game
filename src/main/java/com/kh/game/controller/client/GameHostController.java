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
@RequestMapping("/game/solo/host")
@RequiredArgsConstructor
public class GameHostController {

    private final SongService songService;
    private final GenreService genreService;
    private final GameSessionService gameSessionService;

    @GetMapping
    public String setup(Model model) {
        model.addAttribute("genres", genreService.findActiveGenresForGame());
        return "client/game/host/setup";
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

    @PostMapping("/song-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSongCountPost(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> result = new HashMap<>();

        GameSettings settings = new GameSettings();

        if (request.get("genreId") != null) {
            settings.setFixedGenreId(Long.valueOf(request.get("genreId").toString()));
        }
        if (request.get("soloOnly") != null) {
            settings.setSoloOnly((Boolean) request.get("soloOnly"));
        }
        if (request.get("groupOnly") != null) {
            settings.setGroupOnly((Boolean) request.get("groupOnly"));
        }

        // 복수 연도 선택
        if (request.get("years") != null) {
            @SuppressWarnings("unchecked")
            List<Integer> years = (List<Integer>) request.get("years");
            settings.setSelectedYears(years);
        }

        // 복수 아티스트 선택
        if (request.get("artists") != null) {
            @SuppressWarnings("unchecked")
            List<String> artists = (List<String>) request.get("artists");
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
        List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("playedSongIds");
        if (playedSongIds == null) {
            playedSongIds = new ArrayList<>();
        }

        for (var genre : genreService.findActiveGenresForGame()) {
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
        List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("playedSongIds");
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
        List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("playedSongIds");
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
            @SuppressWarnings("unchecked")
            List<String> players = (List<String>) request.get("players");
            int totalRounds = (int) request.get("totalRounds");
            String gameMode = (String) request.get("gameMode");

            @SuppressWarnings("unchecked")
            Map<String, Object> settingsMap = (Map<String, Object>) request.get("settings");

            // GameSession 생성
            GameSession session = new GameSession();
            session.setSessionUuid(UUID.randomUUID().toString());
            session.setNickname(players.get(0)); // 첫번째 플레이어를 대표로
            session.setGameType(GameSession.GameType.SOLO_HOST);
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
                // 복수 연도 선택
                if (settingsMap.get("selectedYears") != null) {
                    @SuppressWarnings("unchecked")
                    List<Integer> selectedYears = (List<Integer>) settingsMap.get("selectedYears");
                    settings.setSelectedYears(selectedYears);
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

            // 세션에 플레이어 정보 저장
            httpSession.setAttribute("gameSessionId", savedSession.getId());
            httpSession.setAttribute("players", players);
            httpSession.setAttribute("playerScores", new HashMap<String, Integer>());
            httpSession.setAttribute("playedSongIds", new ArrayList<Long>());
            httpSession.setAttribute("gameMode", gameMode);

            // 매 라운드 선택 모드가 아닌 경우에만 미리 라운드 생성
            int replacedCount = 0;
            boolean isPerRoundMode = "GENRE_PER_ROUND".equals(gameMode)
                    || "YEAR_PER_ROUND".equals(gameMode)
                    || "ARTIST_PER_ROUND".equals(gameMode);
            if (!isPerRoundMode) {
                // YouTube 사전 검증 포함된 노래 목록 가져오기
                SongService.ValidatedSongsResult validatedResult =
                        songService.getRandomSongsWithValidation(totalRounds, settings);
                List<Song> songs = validatedResult.getSongs();
                replacedCount = validatedResult.getReplacedCount();

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
            result.put("requestedRounds", totalRounds);
            result.put("actualRounds", savedSession.getTotalRounds());
            result.put("reducedCount", totalRounds - savedSession.getTotalRounds());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "게임 시작 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/play")
    public String play(HttpSession httpSession, Model model) {
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");
        if (sessionId == null) {
            return "redirect:/game/solo/host";
        }

        GameSession session = gameSessionService.findById(sessionId).orElse(null);
        if (session == null) {
            return "redirect:/game/solo/host";
        }

        @SuppressWarnings("unchecked")
        List<String> players = (List<String>) httpSession.getAttribute("players");

        String gameMode = (String) httpSession.getAttribute("gameMode");

        model.addAttribute("gameSession", session);
        model.addAttribute("players", players);
        model.addAttribute("settings", gameSessionService.parseSettings(session.getSettings()));
        model.addAttribute("gameMode", gameMode);
        model.addAttribute("genres", genreService.findActiveGenresForGame());

        return "client/game/host/play";
    }

    @PostMapping("/select-genre")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectGenreForRound(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");

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
            List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("playedSongIds");
            if (playedSongIds == null) {
                playedSongIds = new ArrayList<>();
            }

            // YouTube 사전 검증 포함
            SongService.ValidatedSongResult validatedResult =
                    songService.getValidatedSongByGenre(genreId, playedSongIds);
            Song song = validatedResult.getSong();

            if (song == null) {
                result.put("success", false);
                result.put("message", "해당 장르에 사용 가능한 노래가 없습니다.");
                return ResponseEntity.ok(result);
            }

            // 라운드 생성
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

            // 플레이한 노래 ID 추가
            playedSongIds.add(song.getId());
            httpSession.setAttribute("playedSongIds", playedSongIds);

            result.put("success", true);
            result.put("roundNumber", roundNumber);
            result.put("totalRounds", session.getTotalRounds());

            // ★ 노래 정보를 직접 반환 (loadRound 호출 불필요)
            Map<String, Object> songInfo = new HashMap<>();
            songInfo.put("id", song.getId());
            songInfo.put("title", song.getTitle());
            songInfo.put("artist", song.getArtist());
            songInfo.put("filePath", song.getFilePath());
            songInfo.put("youtubeVideoId", song.getYoutubeVideoId());
            songInfo.put("startTime", song.getStartTime() != null ? song.getStartTime() : 0);
            songInfo.put("playDuration", round.getPlayDuration());
            songInfo.put("releaseYear", song.getReleaseYear());
            if (song.getGenre() != null) {
                songInfo.put("genre", song.getGenre().getName());
            }
            result.put("song", songInfo);

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
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");

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
            List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("playedSongIds");
            if (playedSongIds == null) {
                playedSongIds = new ArrayList<>();
            }

            // YouTube 사전 검증 포함
            SongService.ValidatedSongResult validatedResult =
                    songService.getValidatedSongByArtist(artist, playedSongIds);
            Song song = validatedResult.getSong();

            if (song == null) {
                result.put("success", false);
                result.put("message", "해당 아티스트의 사용 가능한 노래가 없습니다.");
                return ResponseEntity.ok(result);
            }

            // 라운드 생성
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

            // 플레이한 노래 ID 추가
            playedSongIds.add(song.getId());
            httpSession.setAttribute("playedSongIds", playedSongIds);

            result.put("success", true);
            result.put("roundNumber", roundNumber);
            result.put("totalRounds", session.getTotalRounds());

            // 노래 정보를 직접 반환 (loadRound 호출 불필요)
            Map<String, Object> songInfo = new HashMap<>();
            songInfo.put("id", song.getId());
            songInfo.put("title", song.getTitle());
            songInfo.put("artist", song.getArtist());
            songInfo.put("filePath", song.getFilePath());
            songInfo.put("youtubeVideoId", song.getYoutubeVideoId());
            songInfo.put("startTime", song.getStartTime() != null ? song.getStartTime() : 0);
            songInfo.put("playDuration", round.getPlayDuration());
            songInfo.put("releaseYear", song.getReleaseYear());
            if (song.getGenre() != null) {
                songInfo.put("genre", song.getGenre().getName());
            }
            result.put("song", songInfo);

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
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");

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
            List<Long> playedSongIds = (List<Long>) httpSession.getAttribute("playedSongIds");
            if (playedSongIds == null) {
                playedSongIds = new ArrayList<>();
            }

            // YouTube 사전 검증 포함
            SongService.ValidatedSongResult validatedResult =
                    songService.getValidatedSongByYear(year, playedSongIds);
            Song song = validatedResult.getSong();

            if (song == null) {
                result.put("success", false);
                result.put("message", "해당 연도의 사용 가능한 노래가 없습니다.");
                return ResponseEntity.ok(result);
            }

            // 라운드 생성
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

            // 플레이한 노래 ID 추가
            playedSongIds.add(song.getId());
            httpSession.setAttribute("playedSongIds", playedSongIds);

            result.put("success", true);
            result.put("roundNumber", roundNumber);
            result.put("totalRounds", session.getTotalRounds());

            // 노래 정보를 직접 반환 (loadRound 호출 불필요)
            Map<String, Object> songInfo = new HashMap<>();
            songInfo.put("id", song.getId());
            songInfo.put("title", song.getTitle());
            songInfo.put("artist", song.getArtist());
            songInfo.put("filePath", song.getFilePath());
            songInfo.put("youtubeVideoId", song.getYoutubeVideoId());
            songInfo.put("startTime", song.getStartTime() != null ? song.getStartTime() : 0);
            songInfo.put("playDuration", round.getPlayDuration());
            songInfo.put("releaseYear", song.getReleaseYear());
            if (song.getGenre() != null) {
                songInfo.put("genre", song.getGenre().getName());
            }
            result.put("song", songInfo);

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
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");

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
            songInfo.put("title", round.getSong().getTitle());
            songInfo.put("artist", round.getSong().getArtist());
            songInfo.put("filePath", round.getSong().getFilePath());
            songInfo.put("youtubeVideoId", round.getSong().getYoutubeVideoId());
            songInfo.put("startTime", round.getSong().getStartTime() != null ? round.getSong().getStartTime() : 0);
            songInfo.put("playDuration", round.getPlayDuration());
            songInfo.put("releaseYear", round.getSong().getReleaseYear());
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
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");

        if (sessionId == null) {
            result.put("success", false);
            result.put("message", "세션이 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            int roundNumber = (int) request.get("roundNumber");
            String winner = (String) request.get("winner"); // null이면 스킵
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

            // 라운드 업데이트
            if (isSkip) {
                round.setStatus(GameRound.RoundStatus.SKIPPED);
                round.setIsCorrect(false);
                session.setSkipCount(session.getSkipCount() + 1);
            } else {
                round.setStatus(GameRound.RoundStatus.ANSWERED);
                round.setUserAnswer(winner);
                round.setIsCorrect(true);
                round.setScore(100);
                session.setCorrectCount(session.getCorrectCount() + 1);
                session.setTotalScore(session.getTotalScore() + 100);

                // 플레이어 점수 업데이트
                @SuppressWarnings("unchecked")
                Map<String, Integer> playerScores = (Map<String, Integer>) httpSession.getAttribute("playerScores");
                playerScores.put(winner, playerScores.getOrDefault(winner, 0) + 100);
                httpSession.setAttribute("playerScores", playerScores);
            }

            session.setCompletedRounds(session.getCompletedRounds() + 1);

            // 게임 종료 체크
            if (session.getCompletedRounds() >= session.getTotalRounds()) {
                session.setStatus(GameSession.GameStatus.COMPLETED);
                session.setEndedAt(LocalDateTime.now());
            }

            gameSessionService.save(session);

            result.put("success", true);
            result.put("isGameOver", session.getStatus() == GameSession.GameStatus.COMPLETED);
            result.put("completedRounds", session.getCompletedRounds());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/result")
    public String result(HttpSession httpSession, Model model) {
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");
        if (sessionId == null) {
            return "redirect:/game/solo/host";
        }

        GameSession session = gameSessionService.findById(sessionId).orElse(null);
        if (session == null) {
            return "redirect:/game/solo/host";
        }

        @SuppressWarnings("unchecked")
        List<String> players = (List<String>) httpSession.getAttribute("players");
        @SuppressWarnings("unchecked")
        Map<String, Integer> playerScores = (Map<String, Integer>) httpSession.getAttribute("playerScores");

        // 점수순 정렬 및 동점자 순위 계산
        List<Map<String, Object>> sortedScores = new ArrayList<>();
        if (playerScores != null && players != null) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>();
            for (String player : players) {
                int score = playerScores.getOrDefault(player, 0);
                entries.add(new AbstractMap.SimpleEntry<>(player, score));
            }
            entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            // 동점자 순위 계산
            int rank = 1;
            int prevScore = -1;
            int sameScoreCount = 0;

            for (Map.Entry<String, Integer> entry : entries) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", entry.getKey());
                playerData.put("score", entry.getValue());

                if (entry.getValue() == prevScore) {
                    // 동점이면 이전 순위 유지
                    sameScoreCount++;
                } else {
                    // 점수가 다르면 순위 갱신 (이전 동점자 수만큼 건너뜀)
                    rank += sameScoreCount;
                    sameScoreCount = 1;
                    prevScore = entry.getValue();
                }
                playerData.put("rank", rank);
                sortedScores.add(playerData);
            }
        }

        model.addAttribute("gameSession", session);
        model.addAttribute("rounds", session.getRounds());
        model.addAttribute("players", players);
        model.addAttribute("playerScores", sortedScores);

        return "client/game/host/result";
    }

    @PostMapping("/end")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> endGame(HttpSession httpSession) {
        Map<String, Object> result = new HashMap<>();
        Long sessionId = (Long) httpSession.getAttribute("gameSessionId");

        if (sessionId != null) {
            GameSession session = gameSessionService.findById(sessionId).orElse(null);
            if (session != null && session.getStatus() == GameSession.GameStatus.PLAYING) {
                session.setStatus(GameSession.GameStatus.ABANDONED);
                session.setEndedAt(LocalDateTime.now());
                gameSessionService.save(session);
            }
        }

        httpSession.removeAttribute("gameSessionId");
        httpSession.removeAttribute("players");
        httpSession.removeAttribute("playerScores");

        result.put("success", true);
        return ResponseEntity.ok(result);
    }
}