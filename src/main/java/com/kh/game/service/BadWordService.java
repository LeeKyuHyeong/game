package com.kh.game.service;

import com.kh.game.entity.BadWord;
import com.kh.game.repository.BadWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadWordService {

    private final BadWordRepository badWordRepository;

    // 캐시된 금지어 목록 (성능 최적화)
    private final Map<String, String> badWordCache = new ConcurrentHashMap<>();
    private Pattern badWordPattern = null;

    @PostConstruct
    public void init() {
        reloadCache();
    }

    /**
     * 캐시 리로드
     */
    public void reloadCache() {
        badWordCache.clear();
        List<BadWord> badWords = badWordRepository.findByIsActiveTrue();

        if (badWords.isEmpty()) {
            badWordPattern = null;
            return;
        }

        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < badWords.size(); i++) {
            BadWord bw = badWords.get(i);
            String word = bw.getWord().toLowerCase();
            String replacement = bw.getReplacement() != null ? bw.getReplacement() : "***";
            badWordCache.put(word, replacement);

            if (i > 0) patternBuilder.append("|");
            patternBuilder.append(Pattern.quote(word));
        }

        badWordPattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
        log.info("금지어 캐시 리로드 완료: {}개", badWords.size());
    }

    /**
     * 메시지에서 금지어 필터링
     */
    public String filterMessage(String message) {
        if (message == null || message.isEmpty() || badWordPattern == null) {
            return message;
        }

        String result = message;
        String lowerMessage = message.toLowerCase();

        for (Map.Entry<String, String> entry : badWordCache.entrySet()) {
            String word = entry.getKey();
            String replacement = entry.getValue();

            int index = lowerMessage.indexOf(word);
            while (index >= 0) {
                // 원본 메시지의 해당 부분을 대체
                result = result.substring(0, index) + replacement + result.substring(index + word.length());
                lowerMessage = result.toLowerCase();
                index = lowerMessage.indexOf(word, index + replacement.length());
            }
        }

        return result;
    }

    /**
     * 금지어 포함 여부 확인
     */
    public boolean containsBadWord(String message) {
        if (message == null || message.isEmpty() || badWordPattern == null) {
            return false;
        }
        return badWordPattern.matcher(message).find();
    }

    /**
     * 메시지에서 발견된 금지어 목록 반환
     */
    public List<String> findBadWords(String message) {
        List<String> found = new ArrayList<>();
        if (message == null || message.isEmpty()) {
            return found;
        }

        String lowerMessage = message.toLowerCase();
        for (String word : badWordCache.keySet()) {
            if (lowerMessage.contains(word)) {
                found.add(word);
            }
        }
        return found;
    }

    // ========== CRUD ==========

    @Transactional
    public BadWord addBadWord(String word, String replacement) {
        if (badWordRepository.existsByWord(word.toLowerCase())) {
            throw new IllegalArgumentException("이미 등록된 금지어입니다.");
        }

        BadWord badWord = new BadWord(word.toLowerCase(), replacement);
        BadWord saved = badWordRepository.save(badWord);
        reloadCache();
        return saved;
    }

    @Transactional
    public void updateBadWord(Long id, String word, String replacement, Boolean isActive) {
        BadWord badWord = badWordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("금지어를 찾을 수 없습니다."));

        if (word != null && !word.equals(badWord.getWord())) {
            if (badWordRepository.existsByWord(word.toLowerCase())) {
                throw new IllegalArgumentException("이미 등록된 금지어입니다.");
            }
            badWord.setWord(word.toLowerCase());
        }

        if (replacement != null) {
            badWord.setReplacement(replacement.isEmpty() ? null : replacement);
        }

        if (isActive != null) {
            badWord.setIsActive(isActive);
        }

        reloadCache();
    }

    @Transactional
    public void deleteBadWord(Long id) {
        badWordRepository.deleteById(id);
        reloadCache();
    }

    @Transactional
    public void toggleActive(Long id) {
        BadWord badWord = badWordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("금지어를 찾을 수 없습니다."));
        badWord.setIsActive(!badWord.getIsActive());
        reloadCache();
    }

    public Page<BadWord> findAll(Pageable pageable) {
        return badWordRepository.findAllOrderByCreatedAtDesc(pageable);
    }

    public Page<BadWord> search(String keyword, Pageable pageable) {
        return badWordRepository.findByWordContaining(keyword, pageable);
    }

    public Page<BadWord> findByActive(Boolean isActive, Pageable pageable) {
        return badWordRepository.findByIsActive(isActive, pageable);
    }

    public Optional<BadWord> findById(Long id) {
        return badWordRepository.findById(id);
    }

    public long countActive() {
        return badWordRepository.findByIsActiveTrue().size();
    }
}