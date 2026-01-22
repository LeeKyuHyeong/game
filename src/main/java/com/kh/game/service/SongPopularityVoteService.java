package com.kh.game.service;

import com.kh.game.entity.Member;
import com.kh.game.entity.Song;
import com.kh.game.entity.SongPopularityVote;
import com.kh.game.repository.SongPopularityVoteRepository;
import com.kh.game.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongPopularityVoteService {

    private final SongPopularityVoteRepository voteRepository;
    private final SongRepository songRepository;

    /**
     * 평가 제출 (중복 방지)
     * @param songId 곡 ID
     * @param member 회원
     * @param rating 평가 (1~5)
     * @return 투표 결과 (success, alreadyVoted, vote)
     */
    @Transactional
    public Map<String, Object> submitVote(Long songId, Member member, int rating) {
        Map<String, Object> result = new HashMap<>();

        // 유효성 검사
        if (rating < 1 || rating > 5) {
            result.put("success", false);
            result.put("message", "평가는 1~5 사이여야 합니다");
            return result;
        }

        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "곡을 찾을 수 없습니다");
            return result;
        }

        Song song = songOpt.get();

        // 이미 투표했는지 확인
        if (voteRepository.existsBySongAndMember(song, member)) {
            result.put("success", false);
            result.put("alreadyVoted", true);
            result.put("message", "이미 평가한 곡입니다");
            return result;
        }

        // 투표 저장
        SongPopularityVote vote = new SongPopularityVote(song, member, rating);
        voteRepository.save(vote);

        log.info("곡 인기도 평가 제출: songId={}, memberId={}, rating={}",
                songId, member.getId(), rating);

        result.put("success", true);
        result.put("rating", rating);
        result.put("songId", songId);
        return result;
    }

    /**
     * 회원이 특정 곡 목록에 대해 평가한 기록 조회 (결과 화면용)
     * @param member 회원
     * @param songIds 곡 ID 목록
     * @return Map<songId, rating>
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> getMemberVotesForSongs(Member member, List<Long> songIds) {
        Map<Long, Integer> result = new HashMap<>();

        if (member == null || songIds == null || songIds.isEmpty()) {
            return result;
        }

        List<SongPopularityVote> votes = voteRepository.findByMemberAndSongIdIn(member, songIds);
        for (SongPopularityVote vote : votes) {
            result.put(vote.getSong().getId(), vote.getRating());
        }

        return result;
    }

    /**
     * 특정 곡에 대한 회원의 평가 조회
     */
    @Transactional(readOnly = true)
    public Optional<Integer> getMemberVoteForSong(Member member, Long songId) {
        if (member == null || songId == null) {
            return Optional.empty();
        }

        Optional<Song> songOpt = songRepository.findById(songId);
        if (songOpt.isEmpty()) {
            return Optional.empty();
        }

        return voteRepository.findBySongAndMember(songOpt.get(), member)
                .map(SongPopularityVote::getRating);
    }

    /**
     * 평가 라벨 반환
     */
    public static String getRatingLabel(int rating) {
        return switch (rating) {
            case 1 -> "매우 대중적";
            case 2 -> "대중적";
            case 3 -> "보통";
            case 4 -> "매니악";
            case 5 -> "매우 매니악";
            default -> "알 수 없음";
        };
    }

    // ========== 관리자용 메서드 ==========

    /**
     * 전체 투표 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SongPopularityVote> getAllVotes(Pageable pageable) {
        return voteRepository.findAllWithSongAndMember(pageable);
    }

    /**
     * 키워드로 투표 검색 (곡명/아티스트)
     */
    @Transactional(readOnly = true)
    public Page<SongPopularityVote> searchVotes(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllVotes(pageable);
        }
        return voteRepository.searchByKeyword(keyword.trim(), pageable);
    }

    /**
     * 곡별 통계 조회
     * @return List of maps containing songId, title, artist, avgRating, voteCount, rating distribution
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSongStatistics() {
        List<Object[]> stats = voteRepository.getSongStatistics();
        return stats.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("songId", row[0]);
                    item.put("title", row[1]);
                    item.put("artist", row[2]);
                    item.put("avgRating", row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
                    item.put("voteCount", ((Number) row[4]).longValue());
                    // 평점 분포 (1~5점)
                    item.put("rating1", ((Number) row[5]).longValue());
                    item.put("rating2", ((Number) row[6]).longValue());
                    item.put("rating3", ((Number) row[7]).longValue());
                    item.put("rating4", ((Number) row[8]).longValue());
                    item.put("rating5", ((Number) row[9]).longValue());
                    return item;
                })
                .toList();
    }

    /**
     * 아티스트별 통계 조회
     * @return List of maps containing artist, songCount, avgRating, totalVotes
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArtistStatistics() {
        List<Object[]> stats = voteRepository.getArtistStatistics();
        return stats.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("artist", row[0]);
                    item.put("songCount", ((Number) row[1]).longValue());
                    item.put("avgRating", row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
                    item.put("totalVotes", ((Number) row[3]).longValue());
                    return item;
                })
                .toList();
    }

    /**
     * 특정 곡의 투표 목록 조회
     */
    @Transactional(readOnly = true)
    public List<SongPopularityVote> getVotesBySongId(Long songId) {
        return voteRepository.findBySongIdWithMember(songId);
    }

    /**
     * 특정 곡의 투표 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SongPopularityVote> getVotesBySongId(Long songId, Pageable pageable) {
        return voteRepository.findBySongIdWithMember(songId, pageable);
    }

    /**
     * 오늘 투표 수 조회
     */
    @Transactional(readOnly = true)
    public long getTodayVoteCount() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return voteRepository.countByCreatedAtAfter(startOfDay);
    }

    /**
     * 전체 통계 조회
     * @return Map containing totalVotes, todayVotes, avgRating, votedSongCount
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTotalStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalVotes", voteRepository.count());
        stats.put("todayVotes", getTodayVoteCount());
        Double avgRating = voteRepository.getOverallAverageRating();
        stats.put("avgRating", avgRating != null ? avgRating : 0.0);
        stats.put("votedSongCount", voteRepository.countDistinctSongs());
        return stats;
    }

    /**
     * 대중성 등급 반환 (평균 평점 기준)
     */
    public static String getPopularityGrade(double avgRating) {
        if (avgRating <= 1.5) {
            return "🌟 매우 대중적";
        } else if (avgRating <= 2.5) {
            return "⭐ 대중적";
        } else if (avgRating <= 3.5) {
            return "🎵 보통";
        } else if (avgRating <= 4.5) {
            return "🎸 매니악";
        } else {
            return "💀 매우 매니악";
        }
    }
}
