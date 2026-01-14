package com.kh.game.service;

import com.kh.game.entity.Badge;
import com.kh.game.entity.Member;
import com.kh.game.entity.MemberBadge;
import com.kh.game.entity.MultiTier;
import com.kh.game.repository.BadgeRepository;
import com.kh.game.repository.MemberBadgeRepository;
import com.kh.game.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final MemberBadgeRepository memberBadgeRepository;
    private final MemberRepository memberRepository;

    // ========== 뱃지 획득 체크 메서드 ==========

    /**
     * 기존 기록 기반 모든 뱃지 체크 (마이페이지 접속 시 또는 배치용)
     * 이미 획득한 뱃지는 건너뛰고, 새로 획득한 뱃지만 반환
     */
    @Transactional
    public List<Badge> checkAllBadgesForMember(Member member) {
        List<Badge> newBadges = new ArrayList<>();

        // 1. 입문 뱃지
        if (member.getGuessGames() != null && member.getGuessGames() >= 1) {
            awardBadge(member, "FIRST_GUESS_GAME").ifPresent(newBadges::add);
        }
        if (member.getGuessCorrect() != null && member.getGuessCorrect() >= 1) {
            awardBadge(member, "FIRST_CORRECT").ifPresent(newBadges::add);
        }
        if (member.getMultiGames() != null && member.getMultiGames() >= 1) {
            awardBadge(member, "MULTI_SPROUT").ifPresent(newBadges::add);
        }

        // 2. 점수 마일스톤
        checkScoreBadges(member, newBadges);

        // 3. 승리 마일스톤
        Integer wins = member.getMultiWins();
        if (wins != null) {
            if (wins >= 1) {
                awardBadge(member, "FIRST_MULTI_WIN").ifPresent(newBadges::add);
            }
            if (wins >= 10) {
                awardBadge(member, "MULTI_WINNER_10").ifPresent(newBadges::add);
            }
            if (wins >= 50) {
                awardBadge(member, "MULTI_WINNER_50").ifPresent(newBadges::add);
            }
            if (wins >= 100) {
                awardBadge(member, "MULTI_WINNER_100").ifPresent(newBadges::add);
            }
        }

        // 4. 연속 정답 뱃지 (maxCorrectStreak 기반)
        Integer maxStreak = member.getMaxCorrectStreak();
        if (maxStreak != null) {
            if (maxStreak >= 5) {
                awardBadge(member, "STREAK_5").ifPresent(newBadges::add);
            }
            if (maxStreak >= 10) {
                awardBadge(member, "STREAK_10").ifPresent(newBadges::add);
            }
            if (maxStreak >= 20) {
                awardBadge(member, "STREAK_20").ifPresent(newBadges::add);
            }
        }

        // 5. 멀티 티어 뱃지
        checkMultiTierBadges(member, newBadges);

        return newBadges;
    }

    /**
     * 솔로 게임 완료 후 뱃지 체크
     */
    @Transactional
    public List<Badge> checkBadgesAfterGuessGame(Member member, int score, int correct, int rounds) {
        List<Badge> newBadges = new ArrayList<>();

        // 입문 뱃지
        if (member.getGuessGames() != null && member.getGuessGames() == 1) {
            awardBadge(member, "FIRST_GUESS_GAME").ifPresent(newBadges::add);
        }

        // 첫 정답
        if (member.getGuessCorrect() != null && member.getGuessCorrect() >= 1) {
            awardBadge(member, "FIRST_CORRECT").ifPresent(newBadges::add);
        }

        // 점수 마일스톤
        checkScoreBadges(member, newBadges);

        // 퍼펙트 게임 (10라운드 이상 + 100% 정답률)
        if (rounds >= 10 && correct == rounds) {
            awardBadge(member, "PERFECT_GAME").ifPresent(newBadges::add);
        }

        // 연속 정답 뱃지
        checkStreakBadges(member, newBadges);

        return newBadges;
    }

    /**
     * 멀티게임 완료 후 뱃지 체크
     */
    @Transactional
    public List<Badge> checkBadgesAfterMultiGame(Member member, int rank, int totalPlayers) {
        List<Badge> newBadges = new ArrayList<>();

        // 첫 멀티게임 참여
        if (member.getMultiGames() != null && member.getMultiGames() == 1) {
            awardBadge(member, "MULTI_SPROUT").ifPresent(newBadges::add);
        }

        // 첫 승리
        if (rank == 1 && member.getMultiWins() != null && member.getMultiWins() == 1) {
            awardBadge(member, "FIRST_MULTI_WIN").ifPresent(newBadges::add);
        }

        // 승리 마일스톤
        Integer wins = member.getMultiWins();
        if (wins != null) {
            if (wins >= 10) {
                awardBadge(member, "MULTI_WINNER_10").ifPresent(newBadges::add);
            }
            if (wins >= 50) {
                awardBadge(member, "MULTI_WINNER_50").ifPresent(newBadges::add);
            }
            if (wins >= 100) {
                awardBadge(member, "MULTI_WINNER_100").ifPresent(newBadges::add);
            }
        }

        // 점수 마일스톤
        checkScoreBadges(member, newBadges);

        // 멀티 티어 뱃지
        checkMultiTierBadges(member, newBadges);

        return newBadges;
    }

    /**
     * 점수 마일스톤 뱃지 체크
     */
    private void checkScoreBadges(Member member, List<Badge> newBadges) {
        int totalScore = (member.getGuessScore() != null ? member.getGuessScore() : 0)
                       + (member.getMultiScore() != null ? member.getMultiScore() : 0);

        if (totalScore >= 100) {
            awardBadge(member, "SCORE_100").ifPresent(newBadges::add);
        }
        if (totalScore >= 1000) {
            awardBadge(member, "SCORE_1000").ifPresent(newBadges::add);
        }
        if (totalScore >= 5000) {
            awardBadge(member, "SCORE_5000").ifPresent(newBadges::add);
        }
        if (totalScore >= 10000) {
            awardBadge(member, "SCORE_10000").ifPresent(newBadges::add);
        }
    }

    /**
     * 연속 정답 뱃지 체크
     */
    private void checkStreakBadges(Member member, List<Badge> newBadges) {
        Integer streak = member.getCurrentCorrectStreak();
        if (streak == null) return;

        if (streak >= 5) {
            awardBadge(member, "STREAK_5").ifPresent(newBadges::add);
        }
        if (streak >= 10) {
            awardBadge(member, "STREAK_10").ifPresent(newBadges::add);
        }
        if (streak >= 20) {
            awardBadge(member, "STREAK_20").ifPresent(newBadges::add);
        }
    }

    /**
     * 멀티게임 티어 뱃지 체크
     */
    private void checkMultiTierBadges(Member member, List<Badge> newBadges) {
        if (member.getMultiTier() == null) return;

        MultiTier tier = member.getMultiTier();
        if (tier == MultiTier.GOLD || tier.ordinal() > MultiTier.GOLD.ordinal()) {
            awardBadge(member, "MULTI_TIER_GOLD").ifPresent(newBadges::add);
        }
        if (tier == MultiTier.DIAMOND || tier.ordinal() > MultiTier.DIAMOND.ordinal()) {
            awardBadge(member, "MULTI_TIER_DIAMOND").ifPresent(newBadges::add);
        }
        if (tier == MultiTier.CHALLENGER) {
            awardBadge(member, "MULTI_TIER_CHALLENGER").ifPresent(newBadges::add);
        }
    }

    /**
     * 뱃지 지급 (이미 보유한 경우 무시)
     */
    @Transactional
    public Optional<Badge> awardBadge(Member member, String badgeCode) {
        Badge badge = badgeRepository.findByCode(badgeCode).orElse(null);
        if (badge == null || !Boolean.TRUE.equals(badge.getIsActive())) {
            return Optional.empty();
        }

        if (memberBadgeRepository.existsByMemberAndBadge(member, badge)) {
            return Optional.empty();  // 이미 보유
        }

        MemberBadge memberBadge = new MemberBadge(member, badge);
        memberBadgeRepository.save(memberBadge);

        log.info("뱃지 획득: {} -> {} ({})", member.getNickname(), badge.getName(), badgeCode);
        return Optional.of(badge);
    }

    // ========== 뱃지 선택 ==========

    @Transactional
    public void selectBadge(Long memberId, Long badgeId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (badgeId == null) {
            member.setSelectedBadge(null);
        } else {
            Badge badge = badgeRepository.findById(badgeId)
                    .orElseThrow(() -> new IllegalArgumentException("뱃지를 찾을 수 없습니다."));

            // 회원이 해당 뱃지를 보유하고 있는지 확인
            if (!memberBadgeRepository.existsByMemberAndBadge(member, badge)) {
                throw new IllegalArgumentException("보유하지 않은 뱃지입니다.");
            }

            member.setSelectedBadge(badge);
        }
        memberRepository.save(member);
    }

    // ========== 뱃지 조회 ==========

    public List<MemberBadge> getMemberBadges(Member member) {
        return memberBadgeRepository.findByMemberWithBadge(member);
    }

    public List<Badge> getAllActiveBadges() {
        return badgeRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    public List<MemberBadge> getNewBadges(Member member) {
        return memberBadgeRepository.findByMemberAndIsNewTrue(member);
    }

    @Transactional
    public void markBadgesAsRead(Member member) {
        memberBadgeRepository.markAllAsRead(member);
    }

    public long getBadgeCount(Member member) {
        return memberBadgeRepository.countByMember(member);
    }

    public List<Long> getMemberBadgeIds(Member member) {
        return getMemberBadges(member).stream()
                .map(mb -> mb.getBadge().getId())
                .toList();
    }
}
