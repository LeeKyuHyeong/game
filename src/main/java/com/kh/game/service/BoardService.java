package com.kh.game.service;

import com.kh.game.entity.Board;
import com.kh.game.entity.BoardComment;
import com.kh.game.entity.BoardLike;
import com.kh.game.entity.Member;
import com.kh.game.repository.BoardCommentRepository;
import com.kh.game.repository.BoardLikeRepository;
import com.kh.game.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCommentRepository commentRepository;
    private final BoardLikeRepository likeRepository;
    private final BadWordService badWordService;

    // ========== 게시글 CRUD ==========

    @Transactional
    public Map<String, Object> createBoard(Member member, Board.BoardCategory category,
                                           String title, String content) {
        Map<String, Object> result = new HashMap<>();

        // 비속어 체크
        if (badWordService.containsBadWord(title) || badWordService.containsBadWord(content)) {
            result.put("success", false);
            result.put("message", "부적절한 표현이 포함되어 있습니다.");
            return result;
        }

        Board board = new Board(member, category, title.trim(), content.trim());
        boardRepository.save(board);

        log.info("게시글 작성 - boardId: {}, member: {}, category: {}",
                board.getId(), member.getId(), category);

        result.put("success", true);
        result.put("message", "게시글이 작성되었습니다.");
        result.put("boardId", board.getId());
        return result;
    }

    @Transactional
    public Map<String, Object> updateBoard(Long boardId, Member member, String title, String content) {
        Map<String, Object> result = new HashMap<>();

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            result.put("success", false);
            result.put("message", "게시글을 찾을 수 없습니다.");
            return result;
        }

        if (!board.getMember().getId().equals(member.getId())) {
            result.put("success", false);
            result.put("message", "수정 권한이 없습니다.");
            return result;
        }

        // 비속어 체크
        if (badWordService.containsBadWord(title) || badWordService.containsBadWord(content)) {
            result.put("success", false);
            result.put("message", "부적절한 표현이 포함되어 있습니다.");
            return result;
        }

        board.setTitle(title.trim());
        board.setContent(content.trim());

        log.info("게시글 수정 - boardId: {}, member: {}", boardId, member.getId());

        result.put("success", true);
        result.put("message", "게시글이 수정되었습니다.");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteBoard(Long boardId, Member member, boolean isAdmin) {
        Map<String, Object> result = new HashMap<>();

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            result.put("success", false);
            result.put("message", "게시글을 찾을 수 없습니다.");
            return result;
        }

        if (!isAdmin && !board.getMember().getId().equals(member.getId())) {
            result.put("success", false);
            result.put("message", "삭제 권한이 없습니다.");
            return result;
        }

        board.setStatus(Board.BoardStatus.DELETED);

        log.info("게시글 삭제 - boardId: {}, member: {}, isAdmin: {}",
                boardId, member.getId(), isAdmin);

        result.put("success", true);
        result.put("message", "게시글이 삭제되었습니다.");
        return result;
    }

    public Optional<Board> findById(Long id) {
        return boardRepository.findById(id);
    }

    @Transactional
    public Optional<Board> findByIdAndIncrementView(Long id) {
        Optional<Board> boardOpt = boardRepository.findById(id);
        boardOpt.ifPresent(board -> {
            if (board.getStatus() == Board.BoardStatus.ACTIVE) {
                board.incrementViewCount();
            }
        });
        return boardOpt;
    }

    public Page<Board> getBoards(Board.BoardCategory category, String keyword, Pageable pageable) {
        Board.BoardStatus status = Board.BoardStatus.ACTIVE;

        if (keyword != null && !keyword.trim().isEmpty()) {
            return boardRepository.searchByKeyword(keyword.trim(), status, pageable);
        }
        if (category != null) {
            return boardRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, status, pageable);
        }
        return boardRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    public Page<Board> getPopularBoards(Pageable pageable) {
        return boardRepository.findPopular(Board.BoardStatus.ACTIVE, pageable);
    }

    public Page<Board> getBoardsByMember(Member member, Pageable pageable) {
        return boardRepository.findByMemberAndStatusOrderByCreatedAtDesc(
                member, Board.BoardStatus.ACTIVE, pageable);
    }

    public Map<String, Long> getCategoryStats() {
        Map<String, Long> stats = new HashMap<>();
        for (Board.BoardCategory cat : Board.BoardCategory.values()) {
            stats.put(cat.name(), 0L);
        }

        List<Object[]> counts = boardRepository.countByCategory(Board.BoardStatus.ACTIVE);
        for (Object[] row : counts) {
            Board.BoardCategory cat = (Board.BoardCategory) row[0];
            Long count = (Long) row[1];
            stats.put(cat.name(), count);
        }
        return stats;
    }

    public long getTotalBoardCount() {
        return boardRepository.countByStatus(Board.BoardStatus.ACTIVE);
    }

    // ========== 댓글 CRUD ==========

    @Transactional
    public Map<String, Object> addComment(Long boardId, Member member, String content) {
        Map<String, Object> result = new HashMap<>();

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null || board.getStatus() != Board.BoardStatus.ACTIVE) {
            result.put("success", false);
            result.put("message", "게시글을 찾을 수 없습니다.");
            return result;
        }

        // 비속어 체크
        if (badWordService.containsBadWord(content)) {
            result.put("success", false);
            result.put("message", "부적절한 표현이 포함되어 있습니다.");
            return result;
        }

        BoardComment comment = new BoardComment(board, member, content.trim());
        commentRepository.save(comment);
        board.updateCommentCount(1);

        log.info("댓글 작성 - boardId: {}, commentId: {}, member: {}",
                boardId, comment.getId(), member.getId());

        result.put("success", true);
        result.put("message", "댓글이 작성되었습니다.");
        result.put("commentId", comment.getId());
        return result;
    }

    @Transactional
    public Map<String, Object> deleteComment(Long commentId, Member member, boolean isAdmin) {
        Map<String, Object> result = new HashMap<>();

        BoardComment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            result.put("success", false);
            result.put("message", "댓글을 찾을 수 없습니다.");
            return result;
        }

        if (!isAdmin && !comment.getMember().getId().equals(member.getId())) {
            result.put("success", false);
            result.put("message", "삭제 권한이 없습니다.");
            return result;
        }

        comment.setStatus(BoardComment.CommentStatus.DELETED);
        comment.getBoard().updateCommentCount(-1);

        log.info("댓글 삭제 - commentId: {}, member: {}, isAdmin: {}",
                commentId, member.getId(), isAdmin);

        result.put("success", true);
        result.put("message", "댓글이 삭제되었습니다.");
        return result;
    }

    public List<BoardComment> getComments(Long boardId) {
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) return List.of();
        return commentRepository.findByBoardAndStatusOrderByCreatedAtAsc(
                board, BoardComment.CommentStatus.ACTIVE);
    }

    // ========== 좋아요 ==========

    @Transactional
    public Map<String, Object> toggleLike(Long boardId, Member member) {
        Map<String, Object> result = new HashMap<>();

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null || board.getStatus() != Board.BoardStatus.ACTIVE) {
            result.put("success", false);
            result.put("message", "게시글을 찾을 수 없습니다.");
            return result;
        }

        Optional<BoardLike> existingLike = likeRepository.findByBoardAndMember(board, member);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            board.updateLikeCount(-1);
            result.put("liked", false);
            log.info("좋아요 취소 - boardId: {}, member: {}", boardId, member.getId());
        } else {
            BoardLike like = new BoardLike(board, member);
            likeRepository.save(like);
            board.updateLikeCount(1);
            result.put("liked", true);
            log.info("좋아요 - boardId: {}, member: {}", boardId, member.getId());
        }

        result.put("success", true);
        result.put("likeCount", board.getLikeCount());
        return result;
    }

    public boolean isLikedByMember(Long boardId, Member member) {
        if (member == null) return false;
        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) return false;
        return likeRepository.existsByBoardAndMember(board, member);
    }

    // ========== 관리자 기능 ==========

    public Page<Board> getAllBoards(Board.BoardStatus status, Pageable pageable) {
        if (status != null) {
            return boardRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return boardRepository.findAll(pageable);
    }

    @Transactional
    public Map<String, Object> hideBoard(Long boardId, Member admin) {
        Map<String, Object> result = new HashMap<>();

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            result.put("success", false);
            result.put("message", "게시글을 찾을 수 없습니다.");
            return result;
        }

        board.setStatus(Board.BoardStatus.HIDDEN);

        log.info("게시글 숨김 처리 - boardId: {}, admin: {}", boardId, admin.getId());

        result.put("success", true);
        result.put("message", "게시글이 숨김 처리되었습니다.");
        return result;
    }

    @Transactional
    public Map<String, Object> restoreBoard(Long boardId, Member admin) {
        Map<String, Object> result = new HashMap<>();

        Board board = boardRepository.findById(boardId).orElse(null);
        if (board == null) {
            result.put("success", false);
            result.put("message", "게시글을 찾을 수 없습니다.");
            return result;
        }

        board.setStatus(Board.BoardStatus.ACTIVE);

        log.info("게시글 복구 - boardId: {}, admin: {}", boardId, admin.getId());

        result.put("success", true);
        result.put("message", "게시글이 복구되었습니다.");
        return result;
    }
}
