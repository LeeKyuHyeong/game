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
}
