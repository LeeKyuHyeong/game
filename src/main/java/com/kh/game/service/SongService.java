package com.kh.game.service;

import com.kh.game.dto.GameSettings;
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
import java.util.*;

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

    public List<Song> findActiveSongs() {
        return songRepository.findByUseYn("Y");
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

    public List<Song> getRandomSongs(int count, GameSettings settings) {
        List<Song> allSongs = songRepository.findByUseYnAndFilePathIsNotNull("Y");

        // 필터링
        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            // 연도 필터
            if (settings.getYearFrom() != null && song.getReleaseYear() != null) {
                if (song.getReleaseYear() < settings.getYearFrom()) continue;
            }
            if (settings.getYearTo() != null && song.getReleaseYear() != null) {
                if (song.getReleaseYear() > settings.getYearTo()) continue;
            }

            // 솔로/그룹 필터
            if (settings.getSoloOnly() != null && settings.getSoloOnly()) {
                if (song.getIsSolo() == null || !song.getIsSolo()) continue;
            }
            if (settings.getGroupOnly() != null && settings.getGroupOnly()) {
                if (song.getIsSolo() != null && song.getIsSolo()) continue;
            }

            // 장르 필터
            if (settings.getFixedGenreId() != null) {
                if (song.getGenre() == null || !song.getGenre().getId().equals(settings.getFixedGenreId())) continue;
            }

            filtered.add(song);
        }

        // 셔플 후 필요한 만큼 반환
        Collections.shuffle(filtered);
        return filtered.subList(0, Math.min(count, filtered.size()));
    }

    public int getAvailableSongCount(GameSettings settings) {
        List<Song> allSongs = songRepository.findByUseYnAndFilePathIsNotNull("Y");

        int count = 0;
        for (Song song : allSongs) {
            // 연도 필터
            if (settings.getYearFrom() != null && song.getReleaseYear() != null) {
                if (song.getReleaseYear() < settings.getYearFrom()) continue;
            }
            if (settings.getYearTo() != null && song.getReleaseYear() != null) {
                if (song.getReleaseYear() > settings.getYearTo()) continue;
            }

            // 솔로/그룹 필터
            if (settings.getSoloOnly() != null && settings.getSoloOnly()) {
                if (song.getIsSolo() == null || !song.getIsSolo()) continue;
            }
            if (settings.getGroupOnly() != null && settings.getGroupOnly()) {
                if (song.getIsSolo() != null && song.getIsSolo()) continue;
            }

            // 장르 필터
            if (settings.getFixedGenreId() != null) {
                if (song.getGenre() == null || !song.getGenre().getId().equals(settings.getFixedGenreId())) continue;
            }

            count++;
        }

        return count;
    }

    public int getAvailableSongCountByGenreExcluding(Long genreId, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndFilePathIsNotNull("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            count++;
        }

        return count;
    }

    public Song getRandomSongByGenreExcluding(Long genreId, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndFilePathIsNotNull("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        if (filtered.isEmpty()) {
            return null;
        }

        Collections.shuffle(filtered);
        return filtered.get(0);
    }
}