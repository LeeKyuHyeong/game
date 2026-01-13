package com.kh.game.repository;

import com.kh.game.entity.Board;
import com.kh.game.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 활성 게시글 목록 (최신순)
    Page<Board> findByStatusOrderByCreatedAtDesc(Board.BoardStatus status, Pageable pageable);

    // 카테고리별 활성 게시글
    Page<Board> findByCategoryAndStatusOrderByCreatedAtDesc(
            Board.BoardCategory category, Board.BoardStatus status, Pageable pageable);

    // 회원별 게시글
    Page<Board> findByMemberAndStatusOrderByCreatedAtDesc(
            Member member, Board.BoardStatus status, Pageable pageable);

    // 키워드 검색 (제목 + 내용)
    @Query("SELECT b FROM Board b WHERE b.status = :status " +
           "AND (b.title LIKE %:keyword% OR b.content LIKE %:keyword%) " +
           "ORDER BY b.createdAt DESC")
    Page<Board> searchByKeyword(@Param("keyword") String keyword,
                                @Param("status") Board.BoardStatus status,
                                Pageable pageable);

    // 인기 게시글 (좋아요 순)
    @Query("SELECT b FROM Board b WHERE b.status = :status " +
           "ORDER BY b.likeCount DESC, b.createdAt DESC")
    Page<Board> findPopular(@Param("status") Board.BoardStatus status, Pageable pageable);

    // 카테고리별 개수
    @Query("SELECT b.category, COUNT(b) FROM Board b WHERE b.status = :status GROUP BY b.category")
    List<Object[]> countByCategory(@Param("status") Board.BoardStatus status);

    // 전체 활성 게시글 수
    long countByStatus(Board.BoardStatus status);

    // 회원별 게시글 수
    long countByMemberAndStatus(Member member, Board.BoardStatus status);

    // 오래된 삭제 게시글 영구 삭제 (배치용)
    @Modifying
    @Query("DELETE FROM Board b WHERE b.status = :status AND b.updatedAt < :threshold")
    int deleteOldDeletedBoards(@Param("status") Board.BoardStatus status,
                               @Param("threshold") LocalDateTime threshold);
}
