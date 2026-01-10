package com.kh.game.repository;

import com.kh.game.entity.BadWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadWordRepository extends JpaRepository<BadWord, Long> {

    // 활성화된 금지어 목록
    List<BadWord> findByIsActiveTrue();

    // 단어로 조회
    Optional<BadWord> findByWord(String word);

    // 단어 존재 여부
    boolean existsByWord(String word);

    // 검색
    Page<BadWord> findByWordContaining(String keyword, Pageable pageable);

    // 활성화 상태별 조회
    Page<BadWord> findByIsActive(Boolean isActive, Pageable pageable);

    // 전체 (페이징)
    @Query("SELECT b FROM BadWord b ORDER BY b.createdAt DESC")
    Page<BadWord> findAllOrderByCreatedAtDesc(Pageable pageable);
}