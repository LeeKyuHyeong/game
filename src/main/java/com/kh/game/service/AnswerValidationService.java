package com.kh.game.service;

import com.kh.game.entity.Song;
import com.kh.game.entity.SongAnswer;
import com.kh.game.repository.SongAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnswerValidationService {

    private final SongAnswerRepository songAnswerRepository;

    /**
     * 사용자 답변이 정답인지 검증
     * @param userAnswer 사용자가 입력한 답
     * @param correctTitle 정답 노래 제목
     * @return 정답 여부
     */
    public boolean validateAnswer(String userAnswer, String correctTitle) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String normalizedUserAnswer = normalize(userAnswer);
        String normalizedCorrectTitle = normalize(correctTitle);

        // 정규화된 정답과 비교
        return normalizedUserAnswer.equals(normalizedCorrectTitle);
    }

    /**
     * Song 객체로 정답 검증 (SongAnswer 테이블도 확인)
     */
    public boolean validateAnswer(String userAnswer, Song song) {
        if (userAnswer == null || userAnswer.trim().isEmpty() || song == null) {
            return false;
        }

        String normalizedUserAnswer = normalize(userAnswer);

        // 1. 기본 제목과 비교
        if (normalizedUserAnswer.equals(normalize(song.getTitle()))) {
            return true;
        }

        // 2. SongAnswer 테이블의 대체 정답들과 비교
        List<SongAnswer> answers = songAnswerRepository.findBySongId(song.getId());
        for (SongAnswer answer : answers) {
            if (normalizedUserAnswer.equals(normalize(answer.getAnswer()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 문자열 정규화
     * - 공백 제거
     * - 소문자 변환
     * - 특수문자 제거
     */
    private String normalize(String text) {
        if (text == null) return "";

        return text.toLowerCase()
                .replaceAll("\\s+", "")           // 공백 제거
                .replaceAll("[^a-z0-9가-힣]", ""); // 특수문자 제거 (영문, 숫자, 한글만 유지)
    }
}