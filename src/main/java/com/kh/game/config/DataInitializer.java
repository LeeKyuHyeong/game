package com.kh.game.config;

import com.kh.game.entity.BadWord;
import com.kh.game.entity.Member;
import com.kh.game.repository.BadWordRepository;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.BadWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BadWordRepository badWordRepository;
    private final BadWordService badWordService;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminAccount();
        initBadWords();
    }

    /**
     * 기본 관리자 계정 생성
     */
    private void initAdminAccount() {
        String adminEmail = "a@a.com";

        // 이미 관리자 계정이 있는지 확인
        if (memberRepository.findByEmail(adminEmail).isPresent()) {
            log.info("관리자 계정이 이미 존재합니다: {}", adminEmail);
            return;
        }

        // 기본 관리자 계정 생성
        Member admin = new Member();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("123!@#"));
        admin.setNickname("관리자");
        admin.setUsername("admin");
        admin.setRole(Member.MemberRole.ADMIN);
        admin.setStatus(Member.MemberStatus.ACTIVE);

        memberRepository.save(admin);
        log.info("기본 관리자 계정 생성 완료: {} (비밀번호: admin1234!)", adminEmail);
    }

    private void initBadWords() {
        if (badWordRepository.count() > 0) {
            log.info("금지어 데이터가 이미 존재합니다. 초기화 건너뜀.");
            return;
        }

        log.info("금지어 초기 데이터 등록 시작...");

        // 일반적인 비속어 및 욕설 목록
        List<String> badWords = Arrays.asList(
            // 기본 욕설
            "시발", "씨발", "ㅅㅂ", "ㅆㅂ", "씹", "좆", "ㅈㄹ", "지랄",
            "병신", "ㅂㅅ", "븅신", "빙신",
            "개새끼", "개새", "개색", "개섀끼",
            "미친놈", "미친년", "미친새끼",
            "닥쳐", "꺼져", "죽어",

            // 변형 욕설
            "시바", "씨바", "씨팔", "시팔", "씨빨", "시빨",
            "ㅗ", "ㅗㅗ",

            // 비하/혐오 표현
            "장애인", "찐따", "ㅉㄸ",
            "한남", "한녀", "김치녀", "김치남",

            // 성적 비속어
            "보지", "자지", "섹스", "sex",

            // 영어 욕설
            "fuck", "shit", "damn", "ass", "bitch",
            "f*ck", "sh*t", "b*tch",

            // 기타 부적절한 표현
            "ㄲㅈ", "꺼지", "닥쳐라", "입닥쳐"
        );

        int count = 0;
        for (String word : badWords) {
            try {
                if (!badWordRepository.existsByWord(word.toLowerCase())) {
                    BadWord badWord = new BadWord(word.toLowerCase());
                    badWordRepository.save(badWord);
                    count++;
                }
            } catch (Exception e) {
                log.warn("금지어 등록 실패: {} - {}", word, e.getMessage());
            }
        }

        // 캐시 리로드
        badWordService.reloadCache();

        log.info("금지어 초기 데이터 등록 완료: {}개", count);
    }
}
