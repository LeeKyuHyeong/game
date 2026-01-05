package com.kh.game.repository;

import com.kh.game.entity.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

    Page<Song> findByUseYn(String useYn, Pageable pageable);

    List<Song> findByUseYn(String useYn);

    Page<Song> findByTitleContainingOrArtistContaining(String title, String artist, Pageable pageable);

    Page<Song> findByTitleContainingOrArtistContainingAndUseYn(String title, String artist, String useYn, Pageable pageable);
}
