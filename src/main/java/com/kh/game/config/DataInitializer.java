package com.kh.game.config;

import com.kh.game.entity.Badge;
import com.kh.game.entity.BadWord;
import com.kh.game.entity.Member;
import com.kh.game.repository.BadgeRepository;
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
    private final BadgeRepository badgeRepository;

    @Override
    public void run(String... args) {
        initAdminAccount();
        initBadWords();
        initBadges();
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

    /**
     * 초기 뱃지 데이터 등록 (23개)
     */
    private void initBadges() {
        if (badgeRepository.count() > 0) {
            log.info("뱃지 데이터가 이미 존재합니다. 초기화 건너뜀.");
            return;
        }

        log.info("뱃지 초기 데이터 등록 시작...");

        // 뱃지 데이터 정의 (code, name, description, emoji, category, rarity, sortOrder)
        List<Object[]> badges = Arrays.asList(
            // 입문 (BEGINNER) - 3개
            new Object[]{"FIRST_GUESS_GAME", "첫 발걸음", "첫 솔로 게임 플레이", "👣", Badge.BadgeCategory.BEGINNER, Badge.BadgeRarity.COMMON, 1},
            new Object[]{"FIRST_CORRECT", "첫 정답", "첫 번째 정답 맞추기", "🎯", Badge.BadgeCategory.BEGINNER, Badge.BadgeRarity.COMMON, 2},
            new Object[]{"MULTI_SPROUT", "멀티게임 새싹", "첫 멀티게임 참여", "🌱", Badge.BadgeCategory.BEGINNER, Badge.BadgeRarity.COMMON, 3},

            // 점수 (SCORE) - 4개
            new Object[]{"SCORE_100", "100점 돌파", "누적 100점 달성", "💯", Badge.BadgeCategory.SCORE, Badge.BadgeRarity.COMMON, 10},
            new Object[]{"SCORE_1000", "1000점 클럽", "누적 1,000점 달성", "🏅", Badge.BadgeCategory.SCORE, Badge.BadgeRarity.RARE, 11},
            new Object[]{"SCORE_5000", "5000점 마스터", "누적 5,000점 달성", "🏆", Badge.BadgeCategory.SCORE, Badge.BadgeRarity.EPIC, 12},
            new Object[]{"SCORE_10000", "만점왕", "누적 10,000점 달성", "👑", Badge.BadgeCategory.SCORE, Badge.BadgeRarity.LEGENDARY, 13},

            // 승리 (VICTORY) - 4개
            new Object[]{"FIRST_MULTI_WIN", "첫 승리", "멀티게임 첫 1등", "🥇", Badge.BadgeCategory.VICTORY, Badge.BadgeRarity.COMMON, 20},
            new Object[]{"MULTI_WINNER_10", "10승 달성", "멀티게임 10회 1등", "🎖️", Badge.BadgeCategory.VICTORY, Badge.BadgeRarity.RARE, 21},
            new Object[]{"MULTI_WINNER_50", "50승 전사", "멀티게임 50회 1등", "⚔️", Badge.BadgeCategory.VICTORY, Badge.BadgeRarity.EPIC, 22},
            new Object[]{"MULTI_WINNER_100", "백전백승", "멀티게임 100회 1등", "🏰", Badge.BadgeCategory.VICTORY, Badge.BadgeRarity.LEGENDARY, 23},

            // 연속 (STREAK) - 4개
            new Object[]{"STREAK_5", "5연속 정답", "5문제 연속 정답", "🔥", Badge.BadgeCategory.STREAK, Badge.BadgeRarity.COMMON, 30},
            new Object[]{"STREAK_10", "10연속 정답", "10문제 연속 정답", "💥", Badge.BadgeCategory.STREAK, Badge.BadgeRarity.RARE, 31},
            new Object[]{"STREAK_20", "음악 천재", "20문제 연속 정답", "🧠", Badge.BadgeCategory.STREAK, Badge.BadgeRarity.EPIC, 32},
            new Object[]{"PERFECT_GAME", "퍼펙트 게임", "10라운드 이상 100% 정답률", "✨", Badge.BadgeCategory.STREAK, Badge.BadgeRarity.EPIC, 33},

            // 티어 (TIER) - 8개
            new Object[]{"TIER_SILVER", "실버 달성", "통합 티어 실버 달성", "🥈", Badge.BadgeCategory.TIER, Badge.BadgeRarity.COMMON, 40},
            new Object[]{"TIER_GOLD", "골드 달성", "통합 티어 골드 달성", "🥇", Badge.BadgeCategory.TIER, Badge.BadgeRarity.RARE, 41},
            new Object[]{"TIER_PLATINUM", "플래티넘 달성", "통합 티어 플래티넘 달성", "💎", Badge.BadgeCategory.TIER, Badge.BadgeRarity.RARE, 42},
            new Object[]{"TIER_DIAMOND", "다이아몬드 달성", "통합 티어 다이아몬드 달성", "💠", Badge.BadgeCategory.TIER, Badge.BadgeRarity.EPIC, 43},
            new Object[]{"TIER_MASTER", "마스터 달성", "통합 티어 마스터 달성", "🔱", Badge.BadgeCategory.TIER, Badge.BadgeRarity.LEGENDARY, 44},
            new Object[]{"MULTI_TIER_GOLD", "멀티 골드", "멀티 티어 골드 달성", "🏅", Badge.BadgeCategory.TIER, Badge.BadgeRarity.RARE, 50},
            new Object[]{"MULTI_TIER_DIAMOND", "멀티 다이아", "멀티 티어 다이아몬드 달성", "💎", Badge.BadgeCategory.TIER, Badge.BadgeRarity.EPIC, 51},
            new Object[]{"MULTI_TIER_CHALLENGER", "챌린저", "멀티 티어 챌린저 달성", "⚡", Badge.BadgeCategory.TIER, Badge.BadgeRarity.LEGENDARY, 52}
        );

        int count = 0;
        for (Object[] badgeData : badges) {
            try {
                Badge badge = new Badge();
                badge.setCode((String) badgeData[0]);
                badge.setName((String) badgeData[1]);
                badge.setDescription((String) badgeData[2]);
                badge.setEmoji((String) badgeData[3]);
                badge.setCategory((Badge.BadgeCategory) badgeData[4]);
                badge.setRarity((Badge.BadgeRarity) badgeData[5]);
                badge.setSortOrder((Integer) badgeData[6]);
                badge.setIsActive(true);

                badgeRepository.save(badge);
                count++;
            } catch (Exception e) {
                log.warn("뱃지 등록 실패: {} - {}", badgeData[0], e.getMessage());
            }
        }

        log.info("뱃지 초기 데이터 등록 완료: {}개", count);
    }
}
