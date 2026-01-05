package com.kh.game.service;

import com.kh.game.entity.Song;
import com.kh.game.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;

    @Value("${file.upload-dir:uploads/songs}")
    private String uploadDir;

    public Page<Song> findAll(Pageable pageable) {
        return songRepository.findAll(pageable);
    }

    public Page<Song> search(String keyword, Pageable pageable) {
        return songRepository.findByTitleContainingOrArtistContaining(keyword, keyword, pageable);
    }

    public Optional<Song> findById(Long id) {
        return songRepository.findById(id);
    }

    @Transactional
    public Song save(Song song) {
        return songRepository.save(song);
    }

    @Transactional
    public void deleteById(Long id) {
        songRepository.deleteById(id);
    }

    @Transactional
    public void toggleUseYn(Long id) {
        songRepository.findById(id).ifPresent(song -> {
            song.setUseYn("Y".equals(song.getUseYn()) ? "N" : "Y");
            songRepository.save(song);
        });
    }

    public String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        return newFilename;
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 로그만 남기고 예외는 무시
        }
    }
}