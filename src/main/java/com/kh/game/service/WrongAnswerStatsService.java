package com.kh.game.service;

import com.kh.game.repository.GameRoundAttemptRepository;
import com.kh.game.repository.GameRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WrongAnswerStatsService {

    private final GameRoundAttemptRepository attemptRepository;
    private final GameRoundRepository gameRoundRepository;

    public List<Map<String, Object>> getMostCommonWrongAnswers(int limit) {
        List<Object[]> results = attemptRepository.findMostCommonWrongAnswers();
        List<Map<String, Object>> wrongAnswers = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("answer", row[0]);
            item.put("count", row[1]);
            wrongAnswers.add(item);
            count++;
        }

        return wrongAnswers;
    }

    public List<Map<String, Object>> getWrongAnswersForSong(Long songId, int limit) {
        List<Object[]> results = attemptRepository.findMostCommonWrongAnswersBySong(songId);
        List<Map<String, Object>> wrongAnswers = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("answer", row[0]);
            item.put("count", row[1]);
            wrongAnswers.add(item);
            count++;
        }

        return wrongAnswers;
    }

    public List<Map<String, Object>> getHardestSongs(int minPlays, int limit) {
        List<Object[]> results = attemptRepository.findHardestSongs(minPlays);
        List<Map<String, Object>> hardestSongs = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("songId", row[0]);
            item.put("title", row[1]);
            item.put("artist", row[2]);

            long correct = ((Number) row[3]).longValue();
            long total = ((Number) row[4]).longValue();
            double correctRate = total > 0 ? (correct * 100.0 / total) : 0;
            double wrongRate = 100 - correctRate;

            item.put("correctCount", correct);
            item.put("totalPlays", total);
            item.put("correctRate", Math.round(correctRate * 10) / 10.0);
            item.put("wrongRate", Math.round(wrongRate * 10) / 10.0);

            hardestSongs.add(item);
            count++;
        }

        return hardestSongs;
    }

    public List<Map<String, Object>> getRecentWrongAnswers(int limit) {
        List<Object[]> results = attemptRepository.findRecentWrongAnswers();
        List<Map<String, Object>> recentWrong = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("userAnswer", row[0]);
            item.put("songTitle", row[1]);
            item.put("artist", row[2]);
            item.put("createdAt", row[3]);

            recentWrong.add(item);
            count++;
        }

        return recentWrong;
    }

    public List<Map<String, Object>> getMostCommonWrongAnswersWithSong(int limit) {
        List<Object[]> results = attemptRepository.findMostCommonWrongAnswersWithSong();
        List<Map<String, Object>> wrongAnswers = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("answer", row[0]);
            item.put("songId", row[1]);
            item.put("songTitle", row[2]);
            item.put("artist", row[3]);
            item.put("count", row[4]);
            wrongAnswers.add(item);
            count++;
        }

        return wrongAnswers;
    }

    public Map<String, Object> getStatsSummary() {
        Map<String, Object> summary = new HashMap<>();

        List<Object[]> allWrong = attemptRepository.findMostCommonWrongAnswers();
        long totalWrongCount = allWrong.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        int uniqueWrongAnswers = allWrong.size();

        summary.put("totalWrongCount", totalWrongCount);
        summary.put("uniqueWrongAnswers", uniqueWrongAnswers);

        return summary;
    }

    public List<Map<String, Object>> getMemberSongStats(Long memberId, int limit) {
        List<Object[]> results = gameRoundRepository.findMemberSongStatistics(memberId);
        List<Map<String, Object>> stats = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("songId", row[0]);
            item.put("title", row[1]);
            item.put("artist", row[2]);

            long correctCount = ((Number) row[3]).longValue();
            long totalCount = ((Number) row[4]).longValue();
            double correctRate = totalCount > 0 ? (correctCount * 100.0 / totalCount) : 0;

            item.put("correctCount", correctCount);
            item.put("totalCount", totalCount);
            item.put("correctRate", Math.round(correctRate * 10) / 10.0);

            stats.add(item);
            count++;
        }

        return stats;
    }

    public List<Map<String, Object>> getMemberMostCorrectSongs(Long memberId, int limit) {
        List<Object[]> results = gameRoundRepository.findMemberMostCorrectSongs(memberId);
        List<Map<String, Object>> stats = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("songId", row[0]);
            item.put("title", row[1]);
            item.put("artist", row[2]);

            long correctCount = ((Number) row[3]).longValue();
            long totalCount = ((Number) row[4]).longValue();
            double correctRate = totalCount > 0 ? (correctCount * 100.0 / totalCount) : 0;

            item.put("correctCount", correctCount);
            item.put("totalCount", totalCount);
            item.put("correctRate", Math.round(correctRate * 10) / 10.0);

            stats.add(item);
            count++;
        }

        return stats;
    }

    public List<Map<String, Object>> getMemberHardestSongs(Long memberId, int limit) {
        List<Object[]> results = gameRoundRepository.findMemberHardestSongs(memberId);
        List<Map<String, Object>> stats = new ArrayList<>();

        int count = 0;
        for (Object[] row : results) {
            if (count >= limit) break;

            Map<String, Object> item = new HashMap<>();
            item.put("songId", row[0]);
            item.put("title", row[1]);
            item.put("artist", row[2]);

            long correctCount = ((Number) row[3]).longValue();
            long totalCount = ((Number) row[4]).longValue();
            double correctRate = totalCount > 0 ? (correctCount * 100.0 / totalCount) : 0;

            item.put("correctCount", correctCount);
            item.put("totalCount", totalCount);
            item.put("correctRate", Math.round(correctRate * 10) / 10.0);

            stats.add(item);
            count++;
        }

        return stats;
    }
}
