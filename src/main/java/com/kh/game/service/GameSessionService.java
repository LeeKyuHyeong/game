package com.kh.game.service;

import com.kh.game.dto.GameSettings;
import com.kh.game.entity.GameRound;
import com.kh.game.entity.GameSession;
import com.kh.game.repository.GameRoundRepository;
import com.kh.game.repository.GameSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameSessionService {

    private final GameSessionRepository gameSessionRepository;
    private final GameRoundRepository gameRoundRepository;
    private final ObjectMapper objectMapper;

    public Page<GameSession> findAll(Pageable pageable) {
        return gameSessionRepository.findAll(pageable);
    }

    public Page<GameSession> findByStatus(GameSession.GameStatus status, Pageable pageable) {
        return gameSessionRepository.findByStatus(status, pageable);
    }

    public Page<GameSession> findByGameType(GameSession.GameType gameType, Pageable pageable) {
        return gameSessionRepository.findByGameType(gameType, pageable);
    }

    public Page<GameSession> search(String keyword, Pageable pageable) {
        return gameSessionRepository.findByNicknameContaining(keyword, pageable);
    }

    public Optional<GameSession> findById(Long id) {
        return gameSessionRepository.findById(id);
    }

    public List<GameSession> findBySessionUuid(String sessionUuid) {
        return gameSessionRepository.findBySessionUuidOrderByCreatedAtDesc(sessionUuid);
    }

    public List<GameRound> findRoundsBySessionId(Long sessionId) {
        return gameRoundRepository.findByGameSessionIdOrderByRoundNumberAsc(sessionId);
    }

    public List<GameRound> findRoundsWithAttemptsBySessionId(Long sessionId) {
        return gameRoundRepository.findRoundsWithAttemptsBySessionId(sessionId);
    }

    public List<GameSession> getTopScores(int limit) {
        return gameSessionRepository.findTopScores(Pageable.ofSize(limit));
    }

    public List<GameSession> getDailyTopScores(int limit) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gameSessionRepository.findTopScoresSince(startOfDay, Pageable.ofSize(limit));
    }

    public List<GameSession> getWeeklyTopScores(int limit) {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return gameSessionRepository.findTopScoresSince(startOfWeek, Pageable.ofSize(limit));
    }

    // 게임 타입별 랭킹 조회
    public List<GameSession> getTopScoresByGameType(GameSession.GameType gameType, int limit) {
        return gameSessionRepository.findTopScoresByGameType(gameType, Pageable.ofSize(limit));
    }

    public List<GameSession> getDailyTopScoresByGameType(GameSession.GameType gameType, int limit) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gameSessionRepository.findTopScoresByGameTypeSince(gameType, startOfDay, Pageable.ofSize(limit));
    }

    public List<GameSession> getWeeklyTopScoresByGameType(GameSession.GameType gameType, int limit) {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return gameSessionRepository.findTopScoresByGameTypeSince(gameType, startOfWeek, Pageable.ofSize(limit));
    }

    public Long getTodayGameCount() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return gameSessionRepository.countGamesSince(startOfDay);
    }

    public Double getAverageScore() {
        return gameSessionRepository.getAverageScore();
    }

    public GameSettings parseSettings(String settingsJson) {
        if (settingsJson == null || settingsJson.isEmpty()) {
            return new GameSettings();
        }
        try {
            return objectMapper.readValue(settingsJson, GameSettings.class);
        } catch (JsonProcessingException e) {
            return new GameSettings();
        }
    }

    public String toSettingsJson(GameSettings settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Transactional
    public GameSession save(GameSession session) {
        return gameSessionRepository.save(session);
    }

    @Transactional
    public void deleteById(Long id) {
        gameSessionRepository.deleteById(id);
    }
}