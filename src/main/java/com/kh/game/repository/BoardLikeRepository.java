package com.kh.game.repository;

import com.kh.game.entity.Board;
import com.kh.game.entity.BoardLike;
import com.kh.game.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

    // 특정 회원이 특정 게시글에 좋아요 했는지 확인
    Optional<BoardLike> findByBoardAndMember(Board board, Member member);

    // 좋아요 존재 여부
    boolean existsByBoardAndMember(Board board, Member member);

    // 게시글별 좋아요 수
    long countByBoard(Board board);

    // 회원별 좋아요 수
    long countByMember(Member member);

    // 좋아요 삭제
    void deleteByBoardAndMember(Board board, Member member);
}
