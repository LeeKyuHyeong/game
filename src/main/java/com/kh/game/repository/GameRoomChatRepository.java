package com.kh.game.repository;

import com.kh.game.entity.GameRoom;
import com.kh.game.entity.GameRoomChat;
import com.kh.game.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameRoomChatRepository extends JpaRepository<GameRoomChat, Long> {

    // 특정 방의 모든 채팅 조회 (생성순)
    List<GameRoomChat> findByGameRoomOrderByCreatedAtAsc(GameRoom gameRoom);

    // 특정 ID 이후의 채팅 조회 (폴링용)
    @Query("SELECT c FROM GameRoomChat c WHERE c.gameRoom = :room AND c.id > :lastId ORDER BY c.createdAt ASC")
    List<GameRoomChat> findByGameRoomAndIdGreaterThan(@Param("room") GameRoom room, @Param("lastId") Long lastId);

    // 방의 채팅 삭제
    @Modifying
    @Transactional
    void deleteByGameRoom(GameRoom gameRoom);

    // 최근 N개 채팅 조회
    @Query("SELECT c FROM GameRoomChat c WHERE c.gameRoom = :room ORDER BY c.createdAt DESC LIMIT :limit")
    List<GameRoomChat> findRecentChats(@Param("room") GameRoom room, @Param("limit") int limit);

    // 회원별 채팅 삭제
    @Modifying
    @Transactional
    void deleteByMember(Member member);

    // 전체 채팅 조회 (페이징)
    Page<GameRoomChat> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 필터링 조회
    @Query("SELECT c FROM GameRoomChat c " +
            "WHERE (:keyword IS NULL OR c.message LIKE %:keyword%) " +
            "AND (:roomCode IS NULL OR c.gameRoom.roomCode = :roomCode) " +
            "AND (:nickname IS NULL OR c.member.nickname LIKE %:nickname%) " +
            "AND (:messageType IS NULL OR CAST(c.messageType AS string) = :messageType) " +
            "AND (:startDate IS NULL OR c.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR c.createdAt <= :endDate) " +
            "ORDER BY c.createdAt DESC")
    Page<GameRoomChat> findAllWithFilters(
            @Param("keyword") String keyword,
            @Param("roomCode") String roomCode,
            @Param("nickname") String nickname,
            @Param("messageType") String messageType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // 오늘 채팅 수
    @Query("SELECT COUNT(c) FROM GameRoomChat c WHERE c.createdAt >= CURRENT_DATE")
    long countTodayChats();

    // 메시지 타입별 통계
    @Query("SELECT c.messageType, COUNT(c) FROM GameRoomChat c GROUP BY c.messageType")
    List<Object[]> countByMessageType();

    // 특정 기간 채팅 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM GameRoomChat c WHERE c.createdAt < :before")
    int deleteOldChats(@Param("before") LocalDateTime before);
}