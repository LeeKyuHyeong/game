package com.kh.game.service;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongAnswer;
import com.kh.game.repository.GameRoomRepository;
import com.kh.game.repository.GameRoundRepository;
import com.kh.game.repository.SongAnswerRepository;
import com.kh.game.repository.SongReportRepository;
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
    private final SongReportRepository songReportRepository;
    private final GameRoundRepository gameRoundRepository;
    private final GameRoomRepository gameRoomRepository;
    private final YouTubeValidationService youTubeValidationService;

    @Value("${file.upload-dir:uploads/songs}")
    private String uploadDir;

    public Page<Song> findAll(Pageable pageable) {
        return songRepository.findAll(pageable);
    }

    public Page<Song> search(String keyword, Pageable pageable) {
        return songRepository.findByTitleContainingOrArtistContaining(keyword, keyword, pageable);
    }

    public Page<Song> findByArtist(String artist, Pageable pageable) {
        return songRepository.findByArtist(artist, pageable);
    }

    public Page<Song> searchWithFilters(String keyword, String artist, Long genreId, String useYn, Boolean isSolo, Pageable pageable) {
        return songRepository.searchWithFilters(keyword, artist, genreId, useYn, isSolo, pageable);
    }

    public Page<Song> searchWithFilters(String keyword, List<String> artists, Long genreId, String useYn, Boolean isSolo, Pageable pageable) {
        if (artists != null && !artists.isEmpty()) {
            return songRepository.searchWithFiltersMultipleArtists(keyword, artists, genreId, useYn, isSolo, pageable);
        }
        return songRepository.searchWithFilters(keyword, null, genreId, useYn, isSolo, pageable);
    }

    public List<String> getAllArtists() {
        return songRepository.findAllDistinctArtists();
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
        // 외래 키 제약조건 해결을 위해 관련 데이터 먼저 정리
        // 1. SongReport 삭제 (nullable=false이므로 삭제 필요)
        songReportRepository.deleteBySongId(id);
        // 2. GameRound의 song 참조를 null로 설정
        gameRoundRepository.clearSongReference(id);
        // 3. GameRoom의 currentSong 참조를 null로 설정
        gameRoomRepository.clearCurrentSongReference(id);
        // 4. Song 삭제 (SongAnswer는 cascade로 자동 삭제)
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
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        // 필터링
        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (!matchesSettings(song, settings)) continue;
            filtered.add(song);
        }

        // 셔플 후 필요한 만큼 반환
        Collections.shuffle(filtered);
        return filtered.subList(0, Math.min(count, filtered.size()));
    }

    /**
     * 노래가 설정 조건에 맞는지 확인
     */
    private boolean matchesSettings(Song song, GameSettings settings) {
        // 연도 필터 (복수 선택)
        if (settings.getSelectedYears() != null && !settings.getSelectedYears().isEmpty()) {
            if (song.getReleaseYear() == null || !settings.getSelectedYears().contains(song.getReleaseYear())) {
                return false;
            }
        } else {
            // 레거시: 연도 범위 필터
            if (settings.getYearFrom() != null && song.getReleaseYear() != null) {
                if (song.getReleaseYear() < settings.getYearFrom()) return false;
            }
            if (settings.getYearTo() != null && song.getReleaseYear() != null) {
                if (song.getReleaseYear() > settings.getYearTo()) return false;
            }
        }

        // 솔로/그룹 필터
        if (settings.getSoloOnly() != null && settings.getSoloOnly()) {
            if (song.getIsSolo() == null || !song.getIsSolo()) return false;
        }
        if (settings.getGroupOnly() != null && settings.getGroupOnly()) {
            if (song.getIsSolo() != null && song.getIsSolo()) return false;
        }

        // 장르 필터
        if (settings.getFixedGenreId() != null) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(settings.getFixedGenreId())) return false;
        }

        // 아티스트 필터 (복수 선택)
        if (settings.getSelectedArtists() != null && !settings.getSelectedArtists().isEmpty()) {
            if (song.getArtist() == null || !settings.getSelectedArtists().contains(song.getArtist())) {
                return false;
            }
        } else if (settings.getFixedArtistName() != null && !settings.getFixedArtistName().isEmpty()) {
            // 레거시: 단일 아티스트 필터
            if (song.getArtist() == null || !song.getArtist().equals(settings.getFixedArtistName())) return false;
        }

        return true;
    }

    public int getAvailableSongCount(GameSettings settings) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (!matchesSettings(song, settings)) continue;
            count++;
        }

        return count;
    }

    // 아티스트 목록 조회 (곡 수 포함)
    public List<Map<String, Object>> getArtistsWithCount() {
        List<Object[]> results = songRepository.findDistinctArtistsWithCount();
        List<Map<String, Object>> artists = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> artist = new HashMap<>();
            artist.put("name", row[0]);
            artist.put("count", ((Number) row[1]).intValue());
            artists.add(artist);
        }
        return artists;
    }

    // 연도 목록 조회 (곡 수 포함)
    public List<Map<String, Object>> getYearsWithCount() {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");
        Map<Integer, Integer> yearCountMap = new TreeMap<>(Collections.reverseOrder()); // 최신 연도부터

        for (Song song : allSongs) {
            if (song.getReleaseYear() != null) {
                yearCountMap.merge(song.getReleaseYear(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> years = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : yearCountMap.entrySet()) {
            Map<String, Object> yearInfo = new HashMap<>();
            yearInfo.put("year", entry.getKey());
            yearInfo.put("count", entry.getValue());
            years.add(yearInfo);
        }
        return years;
    }

    // 아티스트 검색 (자동완성용)
    public List<String> searchArtists(String keyword) {
        return songRepository.findArtistsByKeyword(keyword);
    }

    public int getAvailableSongCountByGenreExcluding(Long genreId, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            count++;
        }

        return count;
    }

    public Song getRandomSongByGenreExcluding(Long genreId, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

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

    // ========== 매 라운드 선택 모드용 메서드 ==========

    // 아티스트별 사용 가능한 곡 수 (제외 목록 적용)
    public int getAvailableSongCountByArtistExcluding(String artist, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (song.getArtist() == null || !song.getArtist().equals(artist)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            count++;
        }

        return count;
    }

    // 아티스트로 랜덤 노래 가져오기 (제외 목록 적용)
    public Song getRandomSongByArtistExcluding(String artist, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getArtist() == null || !song.getArtist().equals(artist)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        if (filtered.isEmpty()) {
            return null;
        }

        Collections.shuffle(filtered);
        return filtered.get(0);
    }

    // 연도별 사용 가능한 곡 수 (제외 목록 적용)
    public int getAvailableSongCountByYearExcluding(Integer year, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (song.getReleaseYear() == null || !song.getReleaseYear().equals(year)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            count++;
        }

        return count;
    }

    // 연도로 랜덤 노래 가져오기 (제외 목록 적용)
    public Song getRandomSongByYearExcluding(Integer year, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getReleaseYear() == null || !song.getReleaseYear().equals(year)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        if (filtered.isEmpty()) {
            return null;
        }

        Collections.shuffle(filtered);
        return filtered.get(0);
    }

    // 아티스트 목록 조회 (곡 수 포함, 제외 목록 적용)
    public List<Map<String, Object>> getArtistsWithCountExcluding(List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");
        Map<String, Integer> artistCountMap = new HashMap<>();

        for (Song song : allSongs) {
            if (song.getArtist() == null) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            artistCountMap.merge(song.getArtist(), 1, Integer::sum);
        }

        List<Map<String, Object>> artists = new ArrayList<>();
        artistCountMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Map<String, Object> artist = new HashMap<>();
                    artist.put("name", entry.getKey());
                    artist.put("count", entry.getValue());
                    artists.add(artist);
                });

        return artists;
    }

    // 연도 목록 조회 (곡 수 포함, 제외 목록 적용)
    public List<Map<String, Object>> getYearsWithCountExcluding(List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");
        Map<Integer, Integer> yearCountMap = new TreeMap<>(Collections.reverseOrder());

        for (Song song : allSongs) {
            if (song.getReleaseYear() == null) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            yearCountMap.merge(song.getReleaseYear(), 1, Integer::sum);
        }

        List<Map<String, Object>> years = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : yearCountMap.entrySet()) {
            Map<String, Object> yearInfo = new HashMap<>();
            yearInfo.put("year", entry.getKey());
            yearInfo.put("count", entry.getValue());
            years.add(yearInfo);
        }

        return years;
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
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

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
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        int count = 0;
        for (Song song : allSongs) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            count++;
        }

        return count;
    }

    /**
     * 랜덤 노래 가져오기 (YouTube 검증 포함, excludeSongIds 제외)
     * 멀티플레이어용
     */
    public Song getValidatedRandomSongExcluding(Long genreId, Set<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

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

        Collections.shuffle(filtered);

        // YouTube 검증하면서 유효한 곡 반환
        for (Song song : filtered) {
            if (song.getYoutubeVideoId() != null && !song.getYoutubeVideoId().isEmpty()) {
                YouTubeValidationService.ValidationResult result =
                        youTubeValidationService.validateVideo(song.getYoutubeVideoId());

                if (!result.isValid()) {
                    continue; // 다음 곡 시도
                }
            }
            return song;
        }

        return null;
    }

    // ========== YouTube 사전 검증 메서드 ==========

    /**
     * YouTube 검증 결과를 포함한 랜덤 노래 목록 가져오기
     * 무효한 곡은 자동으로 대체됨
     *
     * @return ValidatedSongsResult (노래 목록 + 대체된 곡 수)
     */
    public ValidatedSongsResult getRandomSongsWithValidation(int count, GameSettings settings) {
        // 여유분 포함해서 후보 가져오기 (대체용으로 2배 요청)
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        // 필터링
        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (!matchesSettings(song, settings)) continue;
            filtered.add(song);
        }

        Collections.shuffle(filtered);

        List<Song> validSongs = new ArrayList<>();
        Set<Long> usedSongIds = new HashSet<>();
        int replacedCount = 0;
        int validationFailCount = 0;

        for (Song song : filtered) {
            if (validSongs.size() >= count) break;
            if (usedSongIds.contains(song.getId())) continue;

            // YouTube 검증
            if (song.getYoutubeVideoId() != null && !song.getYoutubeVideoId().isEmpty()) {
                YouTubeValidationService.ValidationResult result =
                        youTubeValidationService.validateVideo(song.getYoutubeVideoId());

                if (!result.isValid()) {
                    validationFailCount++;
                    continue; // 다음 곡으로 (대체됨)
                }
            }

            validSongs.add(song);
            usedSongIds.add(song.getId());
        }

        // 검증 실패로 인해 대체된 곡 수 계산
        // count 요청했는데 실제로 validationFailCount개가 스킵됨
        replacedCount = Math.min(validationFailCount, count);

        return new ValidatedSongsResult(validSongs, replacedCount);
    }

    /**
     * 단일 곡 YouTube 검증 후 유효한 곡 반환 (장르 기준)
     * 무효시 같은 장르의 다른 곡으로 대체
     */
    public ValidatedSongResult getValidatedSongByGenre(Long genreId, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getGenre() == null || !song.getGenre().getId().equals(genreId)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        Collections.shuffle(filtered);

        for (Song song : filtered) {
            if (song.getYoutubeVideoId() != null && !song.getYoutubeVideoId().isEmpty()) {
                YouTubeValidationService.ValidationResult result =
                        youTubeValidationService.validateVideo(song.getYoutubeVideoId());

                if (!result.isValid()) {
                    continue; // 다음 곡 시도
                }
            }
            return new ValidatedSongResult(song, false);
        }

        return new ValidatedSongResult(null, false);
    }

    /**
     * 아티스트의 모든 곡을 YouTube 검증 후 반환 (팬 챌린지용)
     * 무효한 곡은 제외됨
     */
    public List<Song> getAllValidatedSongsByArtist(String artist) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getArtist() == null || !song.getArtist().equals(artist)) continue;
            filtered.add(song);
        }

        // YouTube 검증
        List<Song> validSongs = new ArrayList<>();
        for (Song song : filtered) {
            if (song.getYoutubeVideoId() != null && !song.getYoutubeVideoId().isEmpty()) {
                YouTubeValidationService.ValidationResult result =
                        youTubeValidationService.validateVideo(song.getYoutubeVideoId());
                if (!result.isValid()) {
                    continue;
                }
            }
            validSongs.add(song);
        }

        // 셔플 후 반환
        Collections.shuffle(validSongs);
        return validSongs;
    }

    /**
     * 아티스트의 전체 곡 수 조회 (팬 챌린지 설정용)
     */
    public int getSongCountByArtist(String artist) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");
        int count = 0;
        for (Song song : allSongs) {
            if (song.getArtist() != null && song.getArtist().equals(artist)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 단일 곡 YouTube 검증 후 유효한 곡 반환 (아티스트 기준)
     */
    public ValidatedSongResult getValidatedSongByArtist(String artist, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getArtist() == null || !song.getArtist().equals(artist)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        Collections.shuffle(filtered);

        for (Song song : filtered) {
            if (song.getYoutubeVideoId() != null && !song.getYoutubeVideoId().isEmpty()) {
                YouTubeValidationService.ValidationResult result =
                        youTubeValidationService.validateVideo(song.getYoutubeVideoId());

                if (!result.isValid()) {
                    continue;
                }
            }
            return new ValidatedSongResult(song, false);
        }

        return new ValidatedSongResult(null, false);
    }

    /**
     * 단일 곡 YouTube 검증 후 유효한 곡 반환 (연도 기준)
     */
    public ValidatedSongResult getValidatedSongByYear(Integer year, List<Long> excludeSongIds) {
        List<Song> allSongs = songRepository.findByUseYnAndHasAudioSource("Y");

        List<Song> filtered = new ArrayList<>();
        for (Song song : allSongs) {
            if (song.getReleaseYear() == null || !song.getReleaseYear().equals(year)) continue;
            if (excludeSongIds != null && excludeSongIds.contains(song.getId())) continue;
            filtered.add(song);
        }

        Collections.shuffle(filtered);

        for (Song song : filtered) {
            if (song.getYoutubeVideoId() != null && !song.getYoutubeVideoId().isEmpty()) {
                YouTubeValidationService.ValidationResult result =
                        youTubeValidationService.validateVideo(song.getYoutubeVideoId());

                if (!result.isValid()) {
                    continue;
                }
            }
            return new ValidatedSongResult(song, false);
        }

        return new ValidatedSongResult(null, false);
    }

    /**
     * 검증된 노래 목록 결과
     */
    public static class ValidatedSongsResult {
        private final List<Song> songs;
        private final int replacedCount;

        public ValidatedSongsResult(List<Song> songs, int replacedCount) {
            this.songs = songs;
            this.replacedCount = replacedCount;
        }

        public List<Song> getSongs() {
            return songs;
        }

        public int getReplacedCount() {
            return replacedCount;
        }
    }

    /**
     * 검증된 단일 노래 결과
     */
    public static class ValidatedSongResult {
        private final Song song;
        private final boolean wasReplaced;

        public ValidatedSongResult(Song song, boolean wasReplaced) {
            this.song = song;
            this.wasReplaced = wasReplaced;
        }

        public Song getSong() {
            return song;
        }

        public boolean wasReplaced() {
            return wasReplaced;
        }
    }
}