package com.kh.game.repository;

import com.kh.game.entity.Board;
import com.kh.game.entity.BoardComment;
import com.kh.game.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    // 게시글별 활성 댓글 (작성순)
    List<BoardComment> findByBoardAndStatusOrderByCreatedAtAsc(
            Board board, BoardComment.CommentStatus status);

    // 게시글별 활성 댓글 (페이징)
    Page<BoardComment> findByBoardAndStatusOrderByCreatedAtAsc(
            Board board, BoardComment.CommentStatus status, Pageable pageable);

    // 회원별 댓글
    Page<BoardComment> findByMemberAndStatusOrderByCreatedAtDesc(
            Member member, BoardComment.CommentStatus status, Pageable pageable);

    // 게시글별 활성 댓글 수
    long countByBoardAndStatus(Board board, BoardComment.CommentStatus status);

    // 회원별 활성 댓글 수
    long countByMemberAndStatus(Member member, BoardComment.CommentStatus status);
}
