package com.kh.game.service;

import com.kh.game.repository.GameRoundAttemptRepository;
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

    /**
     * 가장 흔한 오답 목록 (전체)
     */
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

    /**
     * 특정 곡에 대한 흔한 오답 목록
     */
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

    /**
     * 가장 어려운 곡 목록 (오답률 높은 순)
     */
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

    /**
     * 최근 오답 목록
     */
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

    /**
     * 오답 통계 요약
     */
    public Map<String, Object> getStatsSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 총 오답 수
        List<Object[]> allWrong = attemptRepository.findMostCommonWrongAnswers();
        long totalWrongCount = allWrong.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        // 고유 오답 종류 수
        int uniqueWrongAnswers = allWrong.size();

        summary.put("totalWrongCount", totalWrongCount);
        summary.put("uniqueWrongAnswers", uniqueWrongAnswers);

        return summary;
    }
}
