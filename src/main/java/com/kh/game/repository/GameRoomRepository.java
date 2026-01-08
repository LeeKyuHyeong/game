package com.kh.game.repository;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    // 방 코드로 조회
    Optional<GameRoom> findByRoomCode(String roomCode);

    // 방 코드 존재 여부
    boolean existsByRoomCode(String roomCode);

    // 대기중인 공개 방 목록 (참가 가능한 방)
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND r.isPrivate = false " +
            "AND SIZE(r.participants) < r.maxPlayers ORDER BY r.createdAt DESC")
    List<GameRoom> findAvailableRooms();

    // 대기중인 공개 방 목록 (페이징)
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND r.isPrivate = false " +
            "AND SIZE(r.participants) < r.maxPlayers ORDER BY r.createdAt DESC")
    Page<GameRoom> findAvailableRooms(Pageable pageable);

    // 특정 회원이 방장인 방 조회
    List<GameRoom> findByHostAndStatus(Member host, GameRoom.RoomStatus status);

    // 특정 회원이 참가중인 방 조회
    @Query("SELECT r FROM GameRoom r JOIN r.participants p " +
            "WHERE p.member = :member AND p.status = 'JOINED' AND r.status IN ('WAITING', 'PLAYING')")
    Optional<GameRoom> findActiveRoomByMember(@Param("member") Member member);

    // 방 이름으로 검색
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND r.isPrivate = false " +
            "AND r.roomName LIKE %:keyword% ORDER BY r.createdAt DESC")
    List<GameRoom> searchByRoomName(@Param("keyword") String keyword);

    // 상태별 방 조회
    List<GameRoom> findByStatus(GameRoom.RoomStatus status);

    // 오래된 대기 방 조회 (정리용)
    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' " +
            "AND r.updatedAt < :threshold")
    List<GameRoom> findStaleWaitingRooms(@Param("threshold") java.time.LocalDateTime threshold);
}