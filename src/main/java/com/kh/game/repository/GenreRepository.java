package com.kh.game.repository;

import com.kh.game.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    List<Genre> findByUseYnOrderByDisplayOrderAsc(String useYn);

    List<Genre> findAllByOrderByDisplayOrderAsc();

    Page<Genre> findByNameContainingOrCodeContaining(String name, String code, Pageable pageable);

    Optional<Genre> findByCode(String code);

    boolean existsByCode(String code);
}