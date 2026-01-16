package com.kh.game.service;

import com.kh.game.entity.*;
import com.kh.game.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 멀티게임 재시작(한번 더 하기) 기능 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("멀티게임 재시작 테스트")
class GameRoomRestartTest {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private GameRoomParticipantRepository participantRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member host;
    private Member player1;
    private Member player2;

    @BeforeEach
    void setUp() {
        // 테스트용 멤버 생성
        host = createMember("host_" + System.currentTimeMillis());
        player1 = createMember("player1_" + System.currentTimeMillis());
        player2 = createMember("player2_" + System.currentTimeMillis());
    }

    private Member createMember(String nickname) {
        Member member = new Member();
        member.setUsername(nickname);
        member.setNickname(nickname);
        member.setEmail(nickname + "@test.com");
        member.setPassword(passwordEncoder.encode("test1234"));
        member.setRole(Member.MemberRole.USER);
        return memberRepository.save(member);
    }

    @Test
    @DisplayName("게임 종료 후 재시작 - 방 상태가 WAITING으로 변경되어야 함")
    void testRestartRoom_RoomStatusChangesToWaiting() {
        // Given: 방 생성 및 게임 종료 상태로 설정
        GameRoom room = gameRoomService.createRoom(host, "테스트방", 8, 10, false, "{}");
        room.setStatus(GameRoom.RoomStatus.FINISHED);
        gameRoomRepository.save(room);

        // When: 재시작
        gameRoomService.restartRoom(room, host);

        // Then: 방 상태 확인
        GameRoom updatedRoom = gameRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(updatedRoom.getStatus()).isEqualTo(GameRoom.RoomStatus.WAITING);
        assertThat(updatedRoom.getCurrentRound()).isEqualTo(0);
    }

    @Test
    @DisplayName("게임 종료 후 재시작 - 참가자 상태가 JOINED로 변경되어야 함")
    void testRestartRoom_ParticipantStatusChangesToJoined() {
        // Given: 방 생성 및 참가자 추가
        GameRoom room = gameRoomService.createRoom(host, "테스트방", 8, 10, false, "{}");
        GameRoomParticipant p1 = gameRoomService.joinRoom(room.getRoomCode(), player1);
        GameRoomParticipant p2 = gameRoomService.joinRoom(room.getRoomCode(), player2);

        // 게임 진행 상태로 변경
        room.setStatus(GameRoom.RoomStatus.PLAYING);
        List<GameRoomParticipant> participants = participantRepository.findActiveParticipants(room);
        for (GameRoomParticipant p : participants) {
            p.setStatus(GameRoomParticipant.ParticipantStatus.PLAYING);
            p.setScore(100);
            p.setCorrectCount(5);
        }
        gameRoomRepository.save(room);

        // 게임 종료
        room.setStatus(GameRoom.RoomStatus.FINISHED);
        gameRoomRepository.save(room);

        System.out.println("=== 재시작 전 참가자 상태 ===");
        for (GameRoomParticipant p : participantRepository.findByGameRoomOrderByJoinedAtAsc(room)) {
            System.out.println(p.getMember().getNickname() + ": " + p.getStatus() + ", score=" + p.getScore());
        }

        // When: 재시작
        gameRoomService.restartRoom(room, host);

        // Then: 참가자 상태 확인
        System.out.println("=== 재시작 후 참가자 상태 ===");
        List<GameRoomParticipant> updatedParticipants = participantRepository.findActiveParticipants(room);
        for (GameRoomParticipant p : updatedParticipants) {
            System.out.println(p.getMember().getNickname() + ": " + p.getStatus() + ", score=" + p.getScore());
        }

        assertThat(updatedParticipants).hasSize(3); // 방장 + 참가자 2명
        for (GameRoomParticipant p : updatedParticipants) {
            assertThat(p.getStatus()).isEqualTo(GameRoomParticipant.ParticipantStatus.JOINED);
            assertThat(p.getScore()).isEqualTo(0);
            assertThat(p.getCorrectCount()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("FINISHED 상태에서 leaveRoom 호출 시 무시되어야 함")
    void testLeaveRoom_IgnoredWhenFinished() {
        // Given: 방 생성 및 게임 종료 상태
        GameRoom room = gameRoomService.createRoom(host, "테스트방", 8, 10, false, "{}");
        gameRoomService.joinRoom(room.getRoomCode(), player1);

        room.setStatus(GameRoom.RoomStatus.FINISHED);
        gameRoomRepository.save(room);

        // 참가자 상태를 PLAYING으로 변경
        List<GameRoomParticipant> participants = participantRepository.findByGameRoomOrderByJoinedAtAsc(room);
        for (GameRoomParticipant p : participants) {
            p.setStatus(GameRoomParticipant.ParticipantStatus.PLAYING);
        }

        System.out.println("=== leaveRoom 전 참가자 상태 ===");
        for (GameRoomParticipant p : participantRepository.findByGameRoomOrderByJoinedAtAsc(room)) {
            System.out.println(p.getMember().getNickname() + ": " + p.getStatus());
        }

        // When: leaveRoom 호출 (sendBeacon 시뮬레이션)
        gameRoomService.leaveRoom(room, host);

        // Then: 참가자 상태가 변경되지 않아야 함
        System.out.println("=== leaveRoom 후 참가자 상태 ===");
        List<GameRoomParticipant> afterLeave = participantRepository.findByGameRoomOrderByJoinedAtAsc(room);
        for (GameRoomParticipant p : afterLeave) {
            System.out.println(p.getMember().getNickname() + ": " + p.getStatus());
        }

        GameRoomParticipant hostParticipant = participantRepository.findByGameRoomAndMember(room, host).orElseThrow();
        assertThat(hostParticipant.getStatus()).isEqualTo(GameRoomParticipant.ParticipantStatus.PLAYING);
    }

    @Test
    @DisplayName("FINISHED 상태에서 leaveFinishedRoom 호출 시 LEFT로 변경되어야 함")
    void testLeaveFinishedRoom_StatusChangesToLeft() {
        // Given: 방 생성 및 게임 종료 상태
        GameRoom room = gameRoomService.createRoom(host, "테스트방", 8, 10, false, "{}");

        room.setStatus(GameRoom.RoomStatus.FINISHED);
        gameRoomRepository.save(room);

        GameRoomParticipant hostParticipant = participantRepository.findByGameRoomAndMember(room, host).orElseThrow();
        hostParticipant.setStatus(GameRoomParticipant.ParticipantStatus.PLAYING);

        System.out.println("=== leaveFinishedRoom 전 ===");
        System.out.println(host.getNickname() + ": " + hostParticipant.getStatus());

        // When: leaveFinishedRoom 호출 (명시적 로비로 돌아가기)
        gameRoomService.leaveFinishedRoom(room, host);

        // Then: LEFT로 변경
        System.out.println("=== leaveFinishedRoom 후 ===");
        GameRoomParticipant afterLeave = participantRepository.findByGameRoomAndMember(room, host).orElseThrow();
        System.out.println(host.getNickname() + ": " + afterLeave.getStatus());

        assertThat(afterLeave.getStatus()).isEqualTo(GameRoomParticipant.ParticipantStatus.LEFT);
    }

    @Test
    @DisplayName("전체 시나리오: 게임 종료 → 한번 더 하기 → 대기실")
    void testFullScenario_FinishThenRestart() {
        // Given: 방 생성 및 참가
        GameRoom room = gameRoomService.createRoom(host, "테스트방", 8, 10, false, "{}");
        gameRoomService.joinRoom(room.getRoomCode(), player1);

        // 게임 시작 (PLAYING 상태)
        room.setStatus(GameRoom.RoomStatus.PLAYING);
        room.setCurrentRound(5);
        List<GameRoomParticipant> participants = participantRepository.findActiveParticipants(room);
        for (GameRoomParticipant p : participants) {
            p.setStatus(GameRoomParticipant.ParticipantStatus.PLAYING);
            p.setScore(300);
        }
        gameRoomRepository.save(room);

        System.out.println("=== 1. 게임 진행 중 ===");
        System.out.println("방 상태: " + room.getStatus());

        // 게임 종료
        room.setStatus(GameRoom.RoomStatus.FINISHED);
        gameRoomRepository.save(room);

        System.out.println("=== 2. 게임 종료 ===");
        System.out.println("방 상태: " + room.getStatus());

        // sendBeacon으로 leaveRoom 호출 (무시되어야 함)
        gameRoomService.leaveRoom(room, host);
        gameRoomService.leaveRoom(room, player1);

        System.out.println("=== 3. sendBeacon leaveRoom 후 ===");
        participants = participantRepository.findByGameRoomOrderByJoinedAtAsc(room);
        for (GameRoomParticipant p : participants) {
            System.out.println(p.getMember().getNickname() + ": " + p.getStatus());
            assertThat(p.getStatus()).isNotEqualTo(GameRoomParticipant.ParticipantStatus.LEFT);
        }

        // 한번 더 하기 (재시작)
        gameRoomService.restartRoom(room, host);

        System.out.println("=== 4. 재시작 후 ===");
        GameRoom updatedRoom = gameRoomRepository.findById(room.getId()).orElseThrow();
        System.out.println("방 상태: " + updatedRoom.getStatus());

        List<GameRoomParticipant> afterRestart = participantRepository.findActiveParticipants(updatedRoom);
        System.out.println("활성 참가자 수: " + afterRestart.size());
        for (GameRoomParticipant p : afterRestart) {
            System.out.println(p.getMember().getNickname() + ": " + p.getStatus() + ", ready=" + p.getIsReady());
        }

        // Then: 검증
        assertThat(updatedRoom.getStatus()).isEqualTo(GameRoom.RoomStatus.WAITING);
        assertThat(afterRestart).hasSize(2); // 방장 + player1

        for (GameRoomParticipant p : afterRestart) {
            assertThat(p.getStatus()).isEqualTo(GameRoomParticipant.ParticipantStatus.JOINED);
            assertThat(p.getScore()).isEqualTo(0);

            // 방장은 자동 준비
            if (p.getMember().getId().equals(host.getId())) {
                assertThat(p.getIsReady()).isTrue();
            } else {
                assertThat(p.getIsReady()).isFalse();
            }
        }
    }
}
