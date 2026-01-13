package com.kh.game.service;

import com.kh.game.entity.Genre;
import com.kh.game.entity.Song;
import com.kh.game.repository.GenreRepository;
import com.kh.game.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 장르 체계 마이그레이션 서비스
 * 기존 K-POP 기반 장르를 새로운 간결한 체계로 마이그레이션
 *
 * 방안 C: 한국 대중음악 기준 재설계
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GenreMigrationService {

    private final GenreRepository genreRepository;
    private final SongRepository songRepository;

    // 새 장르 체계 정의 (순서 포함)
    private static final List<Map<String, String>> NEW_GENRE_DEFINITIONS = List.of(
        Map.of("code", "IDOL", "name", "아이돌"),
        Map.of("code", "BALLAD", "name", "발라드"),
        Map.of("code", "HIPHOP", "name", "힙합/랩"),
        Map.of("code", "RNB", "name", "R&B/소울"),
        Map.of("code", "INDIE", "name", "인디/어쿠스틱"),
        Map.of("code", "TROT", "name", "트로트"),
        Map.of("code", "BAND", "name", "밴드/록"),
        Map.of("code", "OST", "name", "OST"),
        Map.of("code", "EDM", "name", "EDM/댄스"),
        Map.of("code", "RETRO", "name", "레트로/가요")
    );

    // 기존 장르 코드 → 새 장르 코드 매핑
    private static final Map<String, String> MIGRATION_MAPPING = Map.ofEntries(
        // K-POP 계열
        Map.entry("KPOP", "IDOL"),           // 광범위한 K-POP → 아이돌로 기본 매핑
        Map.entry("KPOP_IDOL", "IDOL"),      // K-POP 아이돌 → 아이돌
        Map.entry("KPOP_BALLAD", "BALLAD"),  // K-POP 발라드 → 발라드
        Map.entry("KPOP_HIPHOP", "HIPHOP"),  // K-POP 힙합 → 힙합/랩
        Map.entry("KPOP_RNB", "RNB"),        // K-POP R&B → R&B/소울
        Map.entry("KPOP_INDIE", "INDIE"),    // K-POP 인디 → 인디/어쿠스틱
        Map.entry("KPOP_TROT", "TROT"),      // 트로트 → 트로트

        // 기타 장르 통합
        Map.entry("HIPHOP", "HIPHOP"),       // 힙합 → 힙합/랩
        Map.entry("RAP", "HIPHOP"),          // 랩 → 힙합/랩
        Map.entry("RNB", "RNB"),             // R&B → R&B/소울
        Map.entry("SOUL", "RNB"),            // 소울 → R&B/소울
        Map.entry("ROCK", "BAND"),           // 록 → 밴드/록
        Map.entry("ROCK_CLASSIC", "BAND"),   // 클래식 록 → 밴드/록
        Map.entry("ROCK_INDIE", "INDIE"),    // 인디 록 → 인디/어쿠스틱
        Map.entry("BAND", "BAND"),           // 밴드 → 밴드/록
        Map.entry("METAL", "BAND"),          // 메탈 → 밴드/록
        Map.entry("PUNK", "BAND"),           // 펑크 → 밴드/록
        Map.entry("OST", "OST"),             // OST → OST
        Map.entry("EDM", "EDM"),             // EDM → EDM/댄스
        Map.entry("HOUSE", "EDM"),           // 하우스 → EDM/댄스
        Map.entry("TECHNO", "EDM"),          // 테크노 → EDM/댄스
        Map.entry("DISCO", "EDM"),           // 디스코 → EDM/댄스
        Map.entry("JAZZ", "INDIE"),          // 재즈 → 인디/어쿠스틱
        Map.entry("BLUES", "INDIE"),         // 블루스 → 인디/어쿠스틱
        Map.entry("CCM", "BALLAD")           // CCM → 발라드
    );

    /**
     * 새 장르 체계로 마이그레이션 실행
     */
    @Transactional
    public void migrateToNewGenreSystem() {
        log.info("=== 장르 마이그레이션 시작 ===");

        // 1. 새 장르 생성
        Map<String, Genre> newGenres = createNewGenres();
        log.info("새 장르 {}개 생성 완료", newGenres.size());

        // 2. 기존 곡들의 장르 매핑 변경
        migrateSongsToNewGenres(newGenres);

        // 3. 기존 장르 비활성화
        deactivateOldGenres(newGenres.keySet());

        log.info("=== 장르 마이그레이션 완료 ===");
    }

    /**
     * 새 장르들을 생성
     */
    private Map<String, Genre> createNewGenres() {
        Map<String, Genre> newGenres = new HashMap<>();
        int displayOrder = 1;

        for (Map<String, String> definition : NEW_GENRE_DEFINITIONS) {
            String code = definition.get("code");
            String name = definition.get("name");

            // 이미 존재하는지 확인
            Genre genre = genreRepository.findByCode(code).orElse(null);

            if (genre == null) {
                genre = new Genre();
                genre.setCode(code);
                genre.setName(name);
            } else {
                // 기존 장르가 있으면 이름 업데이트
                genre.setName(name);
            }

            genre.setDisplayOrder(displayOrder++);
            genre.setUseYn("Y");
            genre = genreRepository.save(genre);

            newGenres.put(code, genre);
            log.debug("장르 생성/업데이트: {} - {}", code, name);
        }

        return newGenres;
    }

    /**
     * 기존 곡들의 장르를 새 장르로 매핑
     */
    private void migrateSongsToNewGenres(Map<String, Genre> newGenres) {
        List<Song> allSongs = songRepository.findAll();
        int migratedCount = 0;

        for (Song song : allSongs) {
            Genre oldGenre = song.getGenre();
            if (oldGenre == null) {
                log.warn("곡 ID {} ({})에 장르가 없습니다", song.getId(), song.getTitle());
                continue;
            }

            String oldCode = oldGenre.getCode();
            String newCode = MIGRATION_MAPPING.get(oldCode);

            if (newCode == null) {
                // 매핑이 없으면 기존 코드가 새 장르 코드인지 확인
                if (newGenres.containsKey(oldCode)) {
                    newCode = oldCode;
                } else {
                    log.warn("곡 ID {} ({}): 매핑되지 않은 장르 코드 '{}'",
                            song.getId(), song.getTitle(), oldCode);
                    continue;
                }
            }

            Genre newGenre = newGenres.get(newCode);
            if (newGenre != null) {
                song.setGenre(newGenre);
                songRepository.save(song);
                migratedCount++;
                log.debug("곡 '{}' 장르 변경: {} → {}", song.getTitle(), oldCode, newCode);
            }
        }

        log.info("총 {} 곡 마이그레이션 완료", migratedCount);
    }

    /**
     * 기존 장르들을 비활성화
     */
    private void deactivateOldGenres(Set<String> newGenreCodes) {
        List<Genre> allGenres = genreRepository.findAll();
        int deactivatedCount = 0;

        for (Genre genre : allGenres) {
            // 새 장르 코드가 아니면 비활성화
            if (!newGenreCodes.contains(genre.getCode())) {
                genre.setUseYn("N");
                genreRepository.save(genre);
                deactivatedCount++;
                log.debug("장르 비활성화: {} - {}", genre.getCode(), genre.getName());
            }
        }

        log.info("기존 장르 {} 개 비활성화 완료", deactivatedCount);
    }

    /**
     * 기존 장르 코드 → 새 장르 코드 매핑 정보 반환
     */
    public Map<String, String> getMigrationMapping() {
        return new HashMap<>(MIGRATION_MAPPING);
    }

    /**
     * 새 장르 정의 목록 반환
     */
    public List<Map<String, String>> getNewGenreDefinitions() {
        return new ArrayList<>(NEW_GENRE_DEFINITIONS);
    }
}
