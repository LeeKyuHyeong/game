package com.kh.game.service;

import com.kh.game.entity.Genre;
import com.kh.game.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository genreRepository;

    public Page<Genre> findAll(Pageable pageable) {
        return genreRepository.findAll(pageable);
    }

    public List<Genre> findAllOrderByDisplayOrder() {
        return genreRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Genre> findActiveGenres() {
        return genreRepository.findByUseYnOrderByDisplayOrderAsc("Y");
    }

    public Page<Genre> search(String keyword, Pageable pageable) {
        return genreRepository.findByNameContainingOrCodeContaining(keyword, keyword, pageable);
    }

    public Optional<Genre> findById(Long id) {
        return genreRepository.findById(id);
    }

    public Optional<Genre> findByCode(String code) {
        return genreRepository.findByCode(code);
    }

    public boolean existsByCode(String code) {
        return genreRepository.existsByCode(code);
    }

    @Transactional
    public Genre save(Genre genre) {
        return genreRepository.save(genre);
    }

    @Transactional
    public void deleteById(Long id) {
        genreRepository.deleteById(id);
    }

    @Transactional
    public void toggleUseYn(Long id) {
        genreRepository.findById(id).ifPresent(genre -> {
            genre.setUseYn("Y".equals(genre.getUseYn()) ? "N" : "Y");
            genreRepository.save(genre);
        });
    }
}