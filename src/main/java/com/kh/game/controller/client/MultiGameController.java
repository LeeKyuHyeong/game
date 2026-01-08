package com.kh.game.controller.client;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomParticipant;
import com.kh.game.entity.Member;
import com.kh.game.entity.Song;
import com.kh.game.service.GameRoomService;
import com.kh.game.service.GenreService;
import com.kh.game.service.MemberService;
import com.kh.game.service.MultiGameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/game/multi")
@RequiredArgsConstructor
public class MultiGameController {

    private final GameRoomService gameRoomService;
    private final MultiGameService multiGameService;
    private final MemberService memberService;
    private final GenreService genreService;
    private final ObjectMapper objectMapper;

    /**
     * 멀티게임 로비 페이지
     */
    @GetMapping
    public String lobby(HttpSession httpSession, Model model) {
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            return "redirect:/auth/login?redirect=/game/multi";
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            httpSession.invalidate();
            return "redirect:/auth/login?redirect=/game/multi";
        }

        // 이미 참가중인 방이 있는지 확인
        gameRoomService.findActiveRoomByMember(member).ifPresent(room -> {
            model.addAttribute("activeRoom", room);
        });

        // 참가 가능한 방 목록
        List<GameRoom> availableRooms = gameRoomService.getAvailableRooms();
        model.addAttribute("rooms", availableRooms);
        model.addAttribute("member", member);
        model.addAttribute("genres", genreService.findActiveGenres());

        return "client/game/multi/lobby";
    }

    /**
     * 방 만들기 페이지
     */
    @GetMapping("/create")
    public String createRoomPage(HttpSession httpSession, Model model) {
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            return "redirect:/auth/login?redirect=/game/multi/create";
        }

        model.addAttribute("genres", genreService.findActiveGenres());
        return "client/game/multi/create";
    }

    /**
     * 방 생성 API
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            result.put("success", false);
            result.put("message", "회원 정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            String roomName = (String) request.get("roomName");
            int maxPlayers = (int) request.getOrDefault("maxPlayers", 8);
            int totalRounds = (int) request.getOrDefault("totalRounds", 10);
            boolean isPrivate = (boolean) request.getOrDefault("isPrivate", false);

            @SuppressWarnings("unchecked")
            Map<String, Object> settings = (Map<String, Object>) request.get("settings");
            String settingsJson = settings != null ? objectMapper.writeValueAsString(settings) : null;

            GameRoom room = gameRoomService.createRoom(member, roomName, maxPlayers, totalRounds, isPrivate, settingsJson);

            result.put("success", true);
            result.put("roomCode", room.getRoomCode());
            result.put("roomId", room.getId());

        } catch (IllegalStateException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "방 생성에 실패했습니다.");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 방 참가 API
     */
    @PostMapping("/join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinRoom(
            @RequestBody Map<String, String> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            result.put("success", false);
            result.put("message", "회원 정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            String roomCode = request.get("roomCode").toUpperCase().trim();
            GameRoomParticipant participant = gameRoomService.joinRoom(roomCode, member);

            result.put("success", true);
            result.put("roomCode", participant.getGameRoom().getRoomCode());
            result.put("roomId", participant.getGameRoom().getId());

        } catch (IllegalArgumentException | IllegalStateException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 대기실 페이지
     */
    @GetMapping("/room/{roomCode}")
    public String waitingRoom(@PathVariable String roomCode, HttpSession httpSession, Model model) {
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            return "redirect:/auth/login?redirect=/game/multi/room/" + roomCode;
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            return "redirect:/auth/login";
        }

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            return "redirect:/game/multi?error=notfound";
        }

        // 참가자인지 확인, 아니면 자동 참가 시도
        GameRoomParticipant participant = gameRoomService.getParticipant(room, member).orElse(null);
        if (participant == null || participant.getStatus() == GameRoomParticipant.ParticipantStatus.LEFT) {
            try {
                participant = gameRoomService.joinRoom(roomCode, member);
            } catch (Exception e) {
                return "redirect:/game/multi?error=" + e.getMessage();
            }
        }

        List<GameRoomParticipant> participants = gameRoomService.getParticipants(room);

        model.addAttribute("room", room);
        model.addAttribute("participants", participants);
        model.addAttribute("member", member);
        model.addAttribute("isHost", room.isHost(member));
        model.addAttribute("myParticipant", participant);

        return "client/game/multi/waiting";
    }

    /**
     * 방 나가기 API
     */
    @PostMapping("/room/{roomCode}/leave")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> leaveRoom(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            gameRoomService.leaveRoom(room, member);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 준비 상태 토글 API
     */
    @PostMapping("/room/{roomCode}/ready")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleReady(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            boolean isReady = gameRoomService.toggleReady(room, member);
            result.put("success", true);
            result.put("isReady", isReady);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 방 상태 조회 API (폴링용)
     */
    @GetMapping("/room/{roomCode}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomStatus(@PathVariable String roomCode) {
        Map<String, Object> result = new HashMap<>();

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        List<GameRoomParticipant> participants = gameRoomService.getParticipants(room);

        result.put("success", true);
        result.put("status", room.getStatus().name());
        result.put("hostId", room.getHost().getId());
        result.put("hostNickname", room.getHost().getNickname());
        result.put("allReady", gameRoomService.isAllReady(room));
        result.put("participants", participants.stream().map(p -> {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("id", p.getId());
            pMap.put("memberId", p.getMember().getId());
            pMap.put("nickname", p.getMember().getNickname());
            pMap.put("isReady", p.getIsReady());
            pMap.put("isHost", room.isHost(p.getMember()));
            return pMap;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(result);
    }

    /**
     * 방 목록 API
     */
    @GetMapping("/rooms")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRooms(
            @RequestParam(required = false) String keyword) {

        List<GameRoom> rooms;
        if (keyword != null && !keyword.isBlank()) {
            rooms = gameRoomService.searchRooms(keyword);
        } else {
            rooms = gameRoomService.getAvailableRooms();
        }

        List<Map<String, Object>> roomList = rooms.stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getId());
            map.put("roomCode", room.getRoomCode());
            map.put("roomName", room.getRoomName());
            map.put("hostNickname", room.getHost().getNickname());
            map.put("currentPlayers", room.getCurrentPlayerCount());
            map.put("maxPlayers", room.getMaxPlayers());
            map.put("totalRounds", room.getTotalRounds());
            map.put("isPrivate", room.getIsPrivate());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(roomList);
    }

    /**
     * 강퇴 API
     */
    @PostMapping("/room/{roomCode}/kick/{targetMemberId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> kickPlayer(
            @PathVariable String roomCode,
            @PathVariable Long targetMemberId,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member host = memberService.findById(memberId).orElse(null);
        Member target = memberService.findById(targetMemberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (host == null || target == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            gameRoomService.kickParticipant(room, host, target);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    // ==================== 게임 진행 API ====================

    /**
     * 게임 시작 API (방장만)
     */
    @PostMapping("/room/{roomCode}/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startGame(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            multiGameService.startGame(room, member);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 게임 플레이 페이지
     */
    @GetMapping("/room/{roomCode}/play")
    public String playPage(@PathVariable String roomCode, HttpSession httpSession, Model model) {
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            return "redirect:/auth/login?redirect=/game/multi/room/" + roomCode;
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            return "redirect:/auth/login";
        }

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            return "redirect:/game/multi?error=notfound";
        }

        // 게임중이 아니면 대기실로
        if (room.getStatus() != GameRoom.RoomStatus.PLAYING) {
            return "redirect:/game/multi/room/" + roomCode;
        }

        // 참가자 확인
        GameRoomParticipant participant = gameRoomService.getParticipant(room, member).orElse(null);
        if (participant == null || participant.getStatus() == GameRoomParticipant.ParticipantStatus.LEFT) {
            return "redirect:/game/multi?error=notparticipant";
        }

        model.addAttribute("room", room);
        model.addAttribute("member", member);
        model.addAttribute("isHost", room.isHost(member));
        model.addAttribute("genres", genreService.findActiveGenres());

        return "client/game/multi/play";
    }

    /**
     * 현재 라운드 정보 조회 API (폴링용)
     */
    @GetMapping("/room/{roomCode}/round")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoundInfo(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        result.putAll(multiGameService.getCurrentRoundInfo(room));

        return ResponseEntity.ok(result);
    }

    /**
     * 장르 선택 API (GENRE_PER_ROUND 모드, 방장만)
     */
    @PostMapping("/room/{roomCode}/select-genre")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectGenre(
            @PathVariable String roomCode,
            @RequestBody Map<String, Long> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            Long genreId = request.get("genreId");
            Song song = multiGameService.selectGenre(room, member, genreId);

            if (song == null) {
                result.put("success", false);
                result.put("message", "선택 가능한 노래가 없습니다.");
            } else {
                result.put("success", true);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 장르 목록 (남은 곡 수 포함) API
     */
    @GetMapping("/room/{roomCode}/genres")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getGenres(@PathVariable String roomCode) {
        Map<String, Object> result = new HashMap<>();

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        result.put("genres", multiGameService.getGenresWithCount(room));

        return ResponseEntity.ok(result);
    }

    /**
     * 답변 제출 API
     */
    @PostMapping("/room/{roomCode}/answer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @PathVariable String roomCode,
            @RequestBody Map<String, String> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            String answer = request.get("answer");
            Map<String, Object> answerResult = multiGameService.submitAnswer(room, member, answer);
            result.putAll(answerResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 라운드 결과 보기 (방장이 호출)
     */
    @PostMapping("/room/{roomCode}/show-result")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> showRoundResult(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        if (!room.isHost(member)) {
            result.put("success", false);
            result.put("message", "방장만 결과를 공개할 수 있습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            multiGameService.showRoundResult(room);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 다음 라운드 진행 (방장이 호출)
     */
    @PostMapping("/room/{roomCode}/next-round")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> nextRound(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        if (!room.isHost(member)) {
            result.put("success", false);
            result.put("message", "방장만 다음 라운드를 진행할 수 있습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            Map<String, Object> nextResult = multiGameService.proceedToNext(room);
            result.put("success", true);
            result.putAll(nextResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 오디오 재생 API (방장만)
     */
    @PostMapping("/room/{roomCode}/audio/play")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> playAudio(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            multiGameService.playAudio(room, member);
            result.put("success", true);
            result.put("audioPlayedAt", room.getAudioPlayedAt());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 오디오 일시정지 API (방장만)
     */
    @PostMapping("/room/{roomCode}/audio/pause")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pauseAudio(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            multiGameService.pauseAudio(room, member);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 게임 결과 페이지
     */
    @GetMapping("/room/{roomCode}/result")
    public String resultPage(@PathVariable String roomCode, HttpSession httpSession, Model model) {
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            return "redirect:/auth/login";
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            return "redirect:/auth/login";
        }

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            return "redirect:/game/multi";
        }

        List<Map<String, Object>> finalResult = multiGameService.getFinalResult(room);

        model.addAttribute("room", room);
        model.addAttribute("member", member);
        model.addAttribute("results", finalResult);

        return "client/game/multi/result";
    }
}