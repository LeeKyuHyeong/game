package com.kh.game.controller.client;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomParticipant;
import com.kh.game.entity.Member;
import com.kh.game.service.GameRoomService;
import com.kh.game.service.GenreService;
import com.kh.game.service.MemberService;
import com.kh.game.service.MultiGameService;
import com.kh.game.service.SongService;
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
    private final SongService songService;
    private final ObjectMapper objectMapper;

    // ========== 페이지 ==========

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

        return "client/game/multi/lobby";
    }

    /**
     * 방 목록 조회 API (Ajax용)
     */
    @GetMapping("/rooms")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getRooms(
            @RequestParam(required = false) String keyword) {

        List<GameRoom> rooms;
        if (keyword != null && !keyword.trim().isEmpty()) {
            rooms = gameRoomService.searchRooms(keyword);
        } else {
            rooms = gameRoomService.getAvailableRooms();
        }

        List<Map<String, Object>> result = rooms.stream().map(room -> {
            Map<String, Object> roomInfo = new HashMap<>();
            roomInfo.put("roomCode", room.getRoomCode());
            roomInfo.put("roomName", room.getRoomName());
            roomInfo.put("hostNickname", room.getHost().getNickname());
            roomInfo.put("currentPlayers", room.getCurrentPlayerCount());
            roomInfo.put("maxPlayers", room.getMaxPlayers());
            roomInfo.put("totalRounds", room.getTotalRounds());
            return roomInfo;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 방 참가 페이지
     */
    @GetMapping("/join")
    public String joinPage(@RequestParam(required = false) String code,
                           HttpSession httpSession, Model model) {
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            return "redirect:/auth/login?redirect=/game/multi/join" + (code != null ? "?code=" + code : "");
        }

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("member", member);
        model.addAttribute("roomCode", code != null ? code : "");

        return "client/game/multi/join";
    }

    /**
     * 방 참가 API
     */
    @PostMapping("/join/{roomCode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinRoomApi(
            @PathVariable String roomCode,
            @RequestBody(required = false) Map<String, String> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        // roomCode 대문자 변환
        roomCode = roomCode.toUpperCase().trim();

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

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        // 비공개 방 비밀번호 확인
        if (room.getIsPrivate()) {
            String password = request != null ? request.get("password") : null;
            if (password == null || !password.equals(room.getPassword())) {
                result.put("success", false);
                result.put("message", "비밀번호가 일치하지 않습니다.");
                return ResponseEntity.ok(result);
            }
        }

        try {
            gameRoomService.joinRoom(roomCode, member);
            result.put("success", true);
            result.put("roomCode", roomCode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
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

        Member member = memberService.findById(memberId).orElse(null);
        if (member == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("genres", genreService.findActiveGenresForGame());
        model.addAttribute("member", member);

        return "client/game/multi/create";
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

        // 게임중이면 플레이 페이지로
        if (room.getStatus() == GameRoom.RoomStatus.PLAYING) {
            return "redirect:/game/multi/room/" + roomCode + "/play";
        }

        // 이미 참가중인지 확인
        GameRoomParticipant participant = gameRoomService.getParticipant(room, member).orElse(null);
        boolean isParticipant = participant != null && participant.getStatus() != GameRoomParticipant.ParticipantStatus.LEFT;

        // 참가자 목록
        List<GameRoomParticipant> participants = gameRoomService.getParticipants(room);

        model.addAttribute("room", room);
        model.addAttribute("member", member);
        model.addAttribute("isHost", room.isHost(member));
        model.addAttribute("isParticipant", isParticipant);
        model.addAttribute("participants", participants);
        model.addAttribute("myParticipant", participant);

        // 게임 설정 정보 파싱
        String gameModeDisplay = "전체 랜덤";
        String filterInfo = null;
        try {
            if (room.getSettings() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> settings = objectMapper.readValue(room.getSettings(), Map.class);
                String gameMode = (String) settings.getOrDefault("gameMode", "RANDOM");

                switch (gameMode) {
                    case "FIXED_GENRE":
                        gameModeDisplay = "장르 고정";
                        Object genreId = settings.get("fixedGenreId");
                        if (genreId != null) {
                            genreService.findById(((Number) genreId).longValue())
                                    .ifPresent(g -> model.addAttribute("filterInfo", g.getName()));
                        }
                        break;
                    case "FIXED_ARTIST":
                        gameModeDisplay = "아티스트 고정";
                        @SuppressWarnings("unchecked")
                        List<String> artists = (List<String>) settings.get("selectedArtists");
                        if (artists != null && !artists.isEmpty()) {
                            filterInfo = artists.size() == 1 ? artists.get(0) : artists.get(0) + " 외 " + (artists.size() - 1) + "명";
                        }
                        break;
                    case "FIXED_YEAR":
                        gameModeDisplay = "연도 고정";
                        @SuppressWarnings("unchecked")
                        List<Integer> years = (List<Integer>) settings.get("selectedYears");
                        if (years != null && !years.isEmpty()) {
                            filterInfo = years.size() == 1 ? years.get(0) + "년" : years.get(0) + "년 외 " + (years.size() - 1) + "개";
                        }
                        break;
                }

                // 솔로/그룹 필터
                Boolean soloOnly = (Boolean) settings.get("soloOnly");
                Boolean groupOnly = (Boolean) settings.get("groupOnly");
                if (Boolean.TRUE.equals(soloOnly)) {
                    model.addAttribute("artistType", "솔로만");
                } else if (Boolean.TRUE.equals(groupOnly)) {
                    model.addAttribute("artistType", "그룹만");
                }
            }
        } catch (Exception e) {
            // 파싱 실패 시 기본값 사용
        }
        model.addAttribute("gameModeDisplay", gameModeDisplay);
        if (filterInfo != null) {
            model.addAttribute("filterInfo", filterInfo);
        }

        return "client/game/multi/waiting";
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

        return "client/game/multi/play";
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
        model.addAttribute("isHost", room.isHost(member));

        return "client/game/multi/result";
    }

    // ========== 필터링 데이터 API ==========

    /**
     * 연도 목록 조회 API
     */
    @GetMapping("/years")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getYears() {
        List<Map<String, Object>> years = songService.getYearsWithCount();
        return ResponseEntity.ok(years);
    }

    /**
     * 아티스트 목록 조회 API
     */
    @GetMapping("/artists")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getArtists() {
        List<Map<String, Object>> artists = songService.getArtistsWithCount();
        return ResponseEntity.ok(artists);
    }

    /**
     * 노래 개수 조회 API (필터링 조건에 따라)
     */
    @PostMapping("/song-count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSongCount(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> result = new HashMap<>();

        GameSettings settings = new GameSettings();

        // 장르 필터
        if (request.get("genreId") != null) {
            settings.setFixedGenreId(Long.valueOf(request.get("genreId").toString()));
        }

        // 솔로/그룹 필터
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

    // ========== 방 관리 API ==========

    /**
     * 방 생성 API
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRoom(
            @RequestBody GameSettings settings,
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
            String settingsJson = objectMapper.writeValueAsString(settings);
            GameRoom room = gameRoomService.createRoom(
                    member,
                    settings.getRoomName(),
                    settings.getMaxPlayers(),
                    settings.getTotalRounds(),
                    settings.isPrivateRoom(),
                    settingsJson
            );

            result.put("success", true);
            result.put("roomCode", room.getRoomCode());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 방 참가 API
     */
    @PostMapping("/room/{roomCode}/join")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> joinRoom(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();

        // roomCode 대문자 변환
        roomCode = roomCode.toUpperCase().trim();

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

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            gameRoomService.joinRoom(roomCode, member);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
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
            // 종료된 게임에서는 나가기 무시 (sendBeacon으로 인한 자동 호출 방지)
            // 참가자 상태를 LEFT로 바꾸면 결과 화면에 안 나오고, 재시작도 불가능해짐
            // 실제 나가기는 cleanupStaleParticipations()에서 자동 처리됨
            if (room.getStatus() != GameRoom.RoomStatus.FINISHED) {
                gameRoomService.leaveRoom(room, member);
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 결과 화면에서 명시적 로비 이동 API
     * "로비로 돌아가기" 버튼 전용 - 재시작 대상에서 제외하기 위해 LEFT 처리
     */
    @PostMapping("/room/{roomCode}/leave-to-lobby")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> leaveToLobby(
            @PathVariable String roomCode,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", true);  // 로그인 안 되어있어도 로비 이동은 허용
            return ResponseEntity.ok(result);
        }

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", true);  // 방 정보 없어도 로비 이동은 허용
            return ResponseEntity.ok(result);
        }

        try {
            // 명시적 로비 이동 - FINISHED 상태에서도 LEFT 처리
            gameRoomService.leaveFinishedRoom(room, member);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", true);  // 오류가 나도 로비 이동은 허용
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
     * 강퇴 API (방장만)
     */
    @PostMapping("/room/{roomCode}/kick/{targetMemberId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> kickParticipant(
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

        Member member = memberService.findById(memberId).orElse(null);
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        Member targetMember = memberService.findById(targetMemberId).orElse(null);
        if (targetMember == null) {
            result.put("success", false);
            result.put("message", "강퇴 대상을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        try {
            gameRoomService.kickParticipant(room, member, targetMember);
            result.put("success", true);
            result.put("message", targetMember.getNickname() + "님이 강퇴되었습니다.");
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
    public ResponseEntity<Map<String, Object>> getRoomStatus(
            @PathVariable String roomCode,
            HttpSession httpSession) {
        Map<String, Object> result = new HashMap<>();

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        // 요청자의 참가 상태 확인 (강퇴 감지용)
        Long memberId = (Long) httpSession.getAttribute("memberId");
        if (memberId != null) {
            Member member = memberService.findById(memberId).orElse(null);
            if (member != null) {
                GameRoomParticipant myParticipant = gameRoomService.getParticipant(room, member).orElse(null);
                boolean isKicked = myParticipant == null || myParticipant.getStatus() == GameRoomParticipant.ParticipantStatus.LEFT;
                result.put("kicked", isKicked);
            }
        }

        result.put("success", true);
        result.put("status", room.getStatus().name());
        result.put("roomName", room.getRoomName());
        result.put("hostId", room.getHost().getId());
        result.put("hostNickname", room.getHost().getNickname());
        result.put("maxPlayers", room.getMaxPlayers());
        result.put("totalRounds", room.getTotalRounds());
        result.put("isPrivate", room.getIsPrivate());

        List<Map<String, Object>> participants = room.getParticipants().stream()
                .filter(p -> p.getStatus() != GameRoomParticipant.ParticipantStatus.LEFT)
                .map(p -> {
                    Map<String, Object> pInfo = new HashMap<>();
                    pInfo.put("memberId", p.getMember().getId());
                    pInfo.put("nickname", p.getMember().getNickname());
                    pInfo.put("isReady", p.getIsReady());
                    pInfo.put("isHost", room.isHost(p.getMember()));
                    return pInfo;
                })
                .collect(Collectors.toList());

        result.put("participants", participants);

        // 모두 준비 완료 여부
        boolean allReady = gameRoomService.isAllReady(room);
        result.put("allReady", allReady);

        return ResponseEntity.ok(result);
    }

    /**
     * 게임 재시작 API (한번 더 하기) - 방장만
     */
    @PostMapping("/room/{roomCode}/restart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restartGame(
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
            gameRoomService.restartRoom(room, member);
            result.put("success", true);
            result.put("roomCode", roomCode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    // ========== 게임 진행 API ==========

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
     * 라운드 시작 API (방장만) - 노래 재생 시작
     */
    @PostMapping("/room/{roomCode}/start-round")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startRound(
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

        Map<String, Object> roundResult = multiGameService.startRound(room, member);
        result.putAll(roundResult);

        return ResponseEntity.ok(result);
    }

    /**
     * 다음 라운드 API (방장만)
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

        Map<String, Object> roundResult = multiGameService.nextRound(room, member);
        result.putAll(roundResult);

        return ResponseEntity.ok(result);
    }

    /**
     * 라운드 준비 완료 API (참가자) - PREPARING 단계에서 호출
     */
    @PostMapping("/room/{roomCode}/round-ready")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setRoundReady(
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

        Map<String, Object> readyResult = multiGameService.setRoundReady(room, member);
        result.putAll(readyResult);

        return ResponseEntity.ok(result);
    }

    /**
     * 곡 스킵 API (방장만) - 재생 오류 시 다른 곡으로 변경
     */
    @PostMapping("/room/{roomCode}/skip-song")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> skipSong(
            @PathVariable String roomCode,
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
        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);

        if (member == null || room == null) {
            result.put("success", false);
            result.put("message", "정보를 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        Long songId = request.get("songId") != null ?
                Long.valueOf(request.get("songId").toString()) : null;

        Map<String, Object> skipResult = multiGameService.skipCurrentSong(room, member, songId);
        result.putAll(skipResult);

        return ResponseEntity.ok(result);
    }

    /**
     * 라운드 스킵 투표 API (참가자)
     */
    @PostMapping("/room/{roomCode}/skip-vote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> voteSkipRound(
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

        Map<String, Object> voteResult = multiGameService.voteSkipRound(room, member);
        result.putAll(voteResult);

        return ResponseEntity.ok(result);
    }

    /**
     * 라운드 정보 조회 API (폴링용)
     */
    @GetMapping("/room/{roomCode}/round")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoundInfo(@PathVariable String roomCode) {
        Map<String, Object> result = new HashMap<>();

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

    // ========== 채팅 API ==========

    /**
     * 채팅 전송 API (정답 체크 포함)
     */
    @PostMapping("/room/{roomCode}/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendChat(
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

        String message = request.get("message");
        Map<String, Object> chatResult = multiGameService.sendChat(room, member, message);
        result.putAll(chatResult);

        return ResponseEntity.ok(result);
    }

    /**
     * 채팅 목록 조회 API (폴링용)
     */
    @GetMapping("/room/{roomCode}/chats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChats(
            @PathVariable String roomCode,
            @RequestParam(defaultValue = "0") Long lastId) {

        Map<String, Object> result = new HashMap<>();

        GameRoom room = gameRoomService.findByRoomCode(roomCode).orElse(null);
        if (room == null) {
            result.put("success", false);
            result.put("message", "방을 찾을 수 없습니다.");
            return ResponseEntity.ok(result);
        }

        result.put("success", true);
        result.put("chats", multiGameService.getChats(room, lastId));

        return ResponseEntity.ok(result);
    }

    // ========== 참가 정보 관리 API ==========

    /**
     * 내 방 참가 정보 초기화 API
     * 오류로 인해 방에 들어갈 수 없을 때 사용
     */
    @PostMapping("/reset-participation")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetParticipation(HttpSession httpSession) {
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
            int count = gameRoomService.resetMyParticipation(member);
            result.put("success", true);
            result.put("message", count > 0
                    ? count + "개의 방 참가 정보가 초기화되었습니다."
                    : "초기화할 참가 정보가 없습니다.");
            result.put("count", count);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "초기화 중 오류가 발생했습니다: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 재생 오류 보고 API (Error 2 등)
     * 참가자가 재생 오류를 보고하면 서버에서 곡 무효화 처리
     */
    @PostMapping("/room/{roomCode}/playback-error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reportPlaybackError(
            @PathVariable String roomCode,
            @RequestBody Map<String, Object> request,
            HttpSession httpSession) {

        Map<String, Object> result = new HashMap<>();
        Long memberId = (Long) httpSession.getAttribute("memberId");

        if (memberId == null) {
            result.put("success", false);
            result.put("message", "로그인이 필요합니다.");
            return ResponseEntity.ok(result);
        }

        try {
            Long songId = request.get("songId") != null ? ((Number) request.get("songId")).longValue() : null;
            Object errorCodeObj = request.get("errorCode");
            final Integer finalErrorCode = (errorCodeObj instanceof Number) ? ((Number) errorCodeObj).intValue() : null;

            if (songId == null) {
                result.put("success", false);
                result.put("message", "곡 정보가 없습니다.");
                return ResponseEntity.ok(result);
            }

            // 곡의 YouTube 유효성 플래그 업데이트
            songService.findById(songId).ifPresent(song -> {
                song.setIsYoutubeValid(false);
                song.setYoutubeCheckedAt(java.time.LocalDateTime.now());
                song.setYoutubeErrorCode(finalErrorCode);
                songService.save(song);
            });

            result.put("success", true);
            result.put("message", "재생 오류가 보고되었습니다.");
            result.put("songId", songId);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "오류 보고 실패: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}