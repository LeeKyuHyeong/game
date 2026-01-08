package com.kh.game.service;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongAnswer;
import com.kh.game.repository.SongAnswerRepository;
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
    private final SongAnswerRepository songAnswerRepository;

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

    // ========== 정답 관련 메서드 ==========

    public List<SongAnswer> getAnswers(Long songId) {
        return songAnswerRepository.findBySongIdOrderByIsPrimaryDesc(songId);
    }

    @Transactional
    public SongAnswer addAnswer(Long songId, String answer, Boolean isPrimary) {
        Song song = songRepository.findById(songId).orElseThrow();
        SongAnswer songAnswer = new SongAnswer(song, answer, isPrimary);
        return songAnswerRepository.save(songAnswer);
    }

    @Transactional
    public void deleteAnswer(Long answerId) {
        songAnswerRepository.deleteById(answerId);
    }

    @Transactional
    public void updateAnswers(Long songId, List<String> answers) {
        songAnswerRepository.deleteBySongId(songId);
        Song song = songRepository.findById(songId).orElseThrow();

        boolean isFirst = true;
        for (String answer : answers) {
            if (answer != null && !answer.trim().isEmpty()) {
                SongAnswer songAnswer = new SongAnswer(song, answer.trim(), isFirst);
                songAnswerRepository.save(songAnswer);
                isFirst = false;
            }
        }
    }

    public boolean checkAnswer(Long songId, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String normalizedUserAnswer = normalizeAnswer(userAnswer);

        // 1. song_answer 테이블에서 정답 확인
        List<SongAnswer> answers = songAnswerRepository.findBySongId(songId);
        for (SongAnswer answer : answers) {
            if (normalizeAnswer(answer.getAnswer()).equals(normalizedUserAnswer)) {
                return true;
            }
        }

        // 2. song_answer가 없으면 title로 체크
        if (answers.isEmpty()) {
            Song song = songRepository.findById(songId).orElse(null);
            if (song != null) {
                return normalizeAnswer(song.getTitle()).equals(normalizedUserAnswer);
            }
        }

        return false;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) return "";
        // 공백 제거, 소문자 변환, 특수문자 제거
        return answer.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9가-힣]", "");
    }

    // ========== 멀티게임용 메서드 ==========

    /**
     * 랜덤 노래 가져오기 (excludeSongIds 제외)
     */
    public Song getRandomSongExcluding(Long genreId, Set<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndFilePathIsNotNull("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            // 장르 필터 (genreId가 null이면 전체)
            if (genreId != null) {
                if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            }
            // 제외 목록 체크
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        if (filtered.isEmpty()) {
            return null;
        }

        Collections.shuffle(filtered);
        return filtered.get(0);
    }

    /**
     * 장르별 사용 가능한 노래 수 (excludeSongIds 제외)
     */
    public int getAvailableCountByGenreExcluding(Long genreId, Set<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndFilePathIsNotNull("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            count++;
        }

        return count;
    }
}