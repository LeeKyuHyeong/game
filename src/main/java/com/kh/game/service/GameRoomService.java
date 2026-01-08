package com.kh.game.service;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomParticipant;
import com.kh.game.entity.Member;
import com.kh.game.repository.GameRoomRepository;
import com.kh.game.repository.GameRoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameRoomService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomParticipantRepository participantRepository;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    /**
     * 방 생성
     */
    @Transactional
    public GameRoom createRoom(Member host, String roomName, int maxPlayers, int totalRounds,
                               boolean isPrivate, String settings) {
        // 이미 다른 방에 참가중인지 확인
        Optional<GameRoomParticipant> existingParticipation = participantRepository.findActiveParticipation(host);
        if (existingParticipation.isPresent()) {
            throw new IllegalStateException("이미 다른 방에 참가중입니다. 먼저 나가주세요.");
        }

        GameRoom room = new GameRoom();
        room.setRoomCode(generateUniqueCode());
        room.setRoomName(roomName);
        room.setHost(host);
        room.setMaxPlayers(maxPlayers);
        room.setTotalRounds(totalRounds);
        room.setIsPrivate(isPrivate);
        room.setSettings(settings);
        room.setStatus(GameRoom.RoomStatus.WAITING);

        gameRoomRepository.save(room);

        // 방장을 참가자로 추가 (자동 준비 완료)
        GameRoomParticipant hostParticipant = new GameRoomParticipant(room, host);
        hostParticipant.setIsReady(true);
        participantRepository.save(hostParticipant);
        room.getParticipants().add(hostParticipant);

        return room;
    }

    /**
     * 방 참가
     */
    @Transactional
    public GameRoomParticipant joinRoom(String roomCode, Member member) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 참가 가능 여부 확인
        if (!room.canJoin()) {
            if (room.getStatus() != GameRoom.RoomStatus.WAITING) {
                throw new IllegalStateException("이미 게임이 시작된 방입니다.");
            }
            throw new IllegalStateException("방이 가득 찼습니다.");
        }

        // 이미 다른 방에 참가중인지 확인
        Optional<GameRoomParticipant> existingParticipation = participantRepository.findActiveParticipation(member);
        if (existingParticipation.isPresent()) {
            GameRoom existingRoom = existingParticipation.get().getGameRoom();
            if (existingRoom.getId().equals(room.getId())) {
                // 같은 방이면 기존 참가 정보 반환
                return existingParticipation.get();
            }
            throw new IllegalStateException("이미 다른 방에 참가중입니다.");
        }

        // 이전에 나갔다가 다시 들어온 경우
        Optional<GameRoomParticipant> previousParticipation = participantRepository.findByGameRoomAndMember(room, member);
        if (previousParticipation.isPresent()) {
            GameRoomParticipant participant = previousParticipation.get();
            participant.setStatus(GameRoomParticipant.ParticipantStatus.JOINED);
            participant.setIsReady(false);
            participant.resetScore();
            return participant;
        }

        // 새 참가자 추가
        GameRoomParticipant participant = new GameRoomParticipant(room, member);
        participantRepository.save(participant);
        room.getParticipants().add(participant);

        return participant;
    }

    /**
     * 방 나가기
     */
    @Transactional
    public void leaveRoom(GameRoom room, Member member) {
        GameRoomParticipant participant = participantRepository.findByGameRoomAndMember(room, member)
                .orElseThrow(() -> new IllegalArgumentException("참가 정보를 찾을 수 없습니다."));

        if (room.isHost(member)) {
            // 방장이 나가면 방 삭제 또는 다음 사람에게 방장 위임
            List<GameRoomParticipant> activeParticipants = participantRepository.findActiveParticipants(room);

            if (activeParticipants.size() <= 1) {
                // 방장 혼자면 방 삭제
                room.setStatus(GameRoom.RoomStatus.FINISHED);
            } else {
                // 다음 참가자에게 방장 위임
                for (GameRoomParticipant p : activeParticipants) {
                    if (!p.getMember().getId().equals(member.getId())) {
                        room.setHost(p.getMember());
                        p.setIsReady(true);  // 새 방장은 자동 준비
                        break;
                    }
                }
            }
        }

        participant.setStatus(GameRoomParticipant.ParticipantStatus.LEFT);
    }

    /**
     * 준비 상태 토글
     */
    @Transactional
    public boolean toggleReady(GameRoom room, Member member) {
        // 방장은 항상 준비 상태
        if (room.isHost(member)) {
            return true;
        }

        GameRoomParticipant participant = participantRepository.findByGameRoomAndMember(room, member)
                .orElseThrow(() -> new IllegalArgumentException("참가 정보를 찾을 수 없습니다."));

        participant.toggleReady();
        return participant.getIsReady();
    }

    /**
     * 모두 준비됐는지 확인
     */
    public boolean isAllReady(GameRoom room) {
        List<GameRoomParticipant> activeParticipants = participantRepository.findActiveParticipants(room);
        if (activeParticipants.size() < 2) {
            return false;  // 최소 2명 필요
        }
        return activeParticipants.stream().allMatch(GameRoomParticipant::getIsReady);
    }

    /**
     * 방 코드로 조회
     */
    public Optional<GameRoom> findByRoomCode(String roomCode) {
        return gameRoomRepository.findByRoomCode(roomCode);
    }

    /**
     * ID로 조회
     */
    public Optional<GameRoom> findById(Long id) {
        return gameRoomRepository.findById(id);
    }

    /**
     * 참가 가능한 방 목록
     */
    public List<GameRoom> getAvailableRooms() {
        return gameRoomRepository.findAvailableRooms();
    }

    /**
     * 방 검색
     */
    public List<GameRoom> searchRooms(String keyword) {
        return gameRoomRepository.searchByRoomName(keyword);
    }

    /**
     * 회원의 현재 참가 방 조회
     */
    public Optional<GameRoom> findActiveRoomByMember(Member member) {
        return gameRoomRepository.findActiveRoomByMember(member);
    }

    /**
     * 참가자 목록 조회
     */
    public List<GameRoomParticipant> getParticipants(GameRoom room) {
        return participantRepository.findActiveParticipants(room);
    }

    /**
     * 참가자 정보 조회
     */
    public Optional<GameRoomParticipant> getParticipant(GameRoom room, Member member) {
        return participantRepository.findByGameRoomAndMember(room, member);
    }

    /**
     * 방 설정 업데이트 (방장만)
     */
    @Transactional
    public void updateRoomSettings(GameRoom room, Member member, String roomName,
                                   int maxPlayers, int totalRounds, String settings) {
        if (!room.isHost(member)) {
            throw new IllegalStateException("방장만 설정을 변경할 수 있습니다.");
        }
        if (room.getStatus() != GameRoom.RoomStatus.WAITING) {
            throw new IllegalStateException("게임중에는 설정을 변경할 수 없습니다.");
        }

        room.setRoomName(roomName);
        room.setMaxPlayers(maxPlayers);
        room.setTotalRounds(totalRounds);
        room.setSettings(settings);
    }

    /**
     * 강퇴 (방장만)
     */
    @Transactional
    public void kickParticipant(GameRoom room, Member host, Member target) {
        if (!room.isHost(host)) {
            throw new IllegalStateException("방장만 강퇴할 수 있습니다.");
        }
        if (host.getId().equals(target.getId())) {
            throw new IllegalStateException("자기 자신은 강퇴할 수 없습니다.");
        }

        GameRoomParticipant participant = participantRepository.findByGameRoomAndMember(room, target)
                .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

        participant.setStatus(GameRoomParticipant.ParticipantStatus.LEFT);
    }

    /**
     * 고유 방 코드 생성
     */
    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = generateRandomCode();
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("방 코드 생성에 실패했습니다.");
            }
        } while (gameRoomRepository.existsByRoomCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}