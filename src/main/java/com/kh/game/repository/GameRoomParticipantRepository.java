package com.kh.game.repository;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomParticipant;
import com.kh.game.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoomParticipantRepository extends JpaRepository<GameRoomParticipant, Long> {

    // 방의 참가자 목록
    List<GameRoomParticipant> findByGameRoomOrderByJoinedAtAsc(GameRoom gameRoom);

    // 방의 활성 참가자 목록
    @Query("SELECT p FROM GameRoomParticipant p WHERE p.gameRoom = :room AND p.status = 'JOINED' ORDER BY p.joinedAt ASC")
    List<GameRoomParticipant> findActiveParticipants(@Param("room") GameRoom room);

    // 특정 회원의 특정 방 참가 정보
    Optional<GameRoomParticipant> findByGameRoomAndMember(GameRoom gameRoom, Member member);

    // 특정 회원이 참가중인 방 (활성 상태)
    @Query("SELECT p FROM GameRoomParticipant p WHERE p.member = :member AND p.status = 'JOINED'")
    Optional<GameRoomParticipant> findActiveParticipation(@Param("member") Member member);

    // 방의 준비된 참가자 수
    @Query("SELECT COUNT(p) FROM GameRoomParticipant p WHERE p.gameRoom = :room AND p.isReady = true AND p.status = 'JOINED'")
    int countReadyParticipants(@Param("room") GameRoom room);

    // 방의 참가자 수
    @Query("SELECT COUNT(p) FROM GameRoomParticipant p WHERE p.gameRoom = :room AND p.status = 'JOINED'")
    int countActiveParticipants(@Param("room") GameRoom room);

    // 점수 순 정렬
    @Query("SELECT p FROM GameRoomParticipant p WHERE p.gameRoom = :room AND p.status = 'JOINED' ORDER BY p.score DESC")
    List<GameRoomParticipant> findByGameRoomOrderByScoreDesc(@Param("room") GameRoom room);

    // 회원이 해당 방에 참가중인지
    boolean existsByGameRoomAndMemberAndStatus(GameRoom gameRoom, Member member, GameRoomParticipant.ParticipantStatus status);
}