package com.kh.game.service;

import com.kh.game.entity.Member;
import com.kh.game.entity.MultiTier;
import com.kh.game.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 멀티게임 LP 티어 시스템 서비스
 * 롤(LoL) 스타일의 LP 기반 승급/강등 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiTierService {

    private final MemberRepository memberRepository;

    /**
     * LP 변화량 계산 결과
     */
    public record LpChangeResult(
            Long memberId,
            String nickname,
            int rank,
            int score,
            MultiTier oldTier,
            int oldLp,
            MultiTier newTier,
            int newLp,
            int lpChange,
            String tierChange  // null, "PROMOTED", "DEMOTED"
    ) {}

    /**
     * 게임 결과에 따른 LP 적용
     *
     * @param memberId 회원 ID
     * @param totalPlayers 총 참가자 수
     * @param rank 순위 (1등, 2등, ...)
     * @param score 게임에서 획득한 점수
     * @return LP 변화 결과
     */
    @Transactional
    public LpChangeResult applyGameResult(Long memberId, int totalPlayers, int rank, int score) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        // 현재 상태 저장
        MultiTier oldTier = member.getMultiTier() != null ? member.getMultiTier() : MultiTier.BRONZE;
        int oldLp = member.getMultiLp() != null ? member.getMultiLp() : 0;

        // LP 변화량 계산
        int lpChange = MultiTier.calculateLpChange(totalPlayers, rank);

        // LP 적용 및 티어 변동 처리
        String tierChange = member.applyLpChange(lpChange);

        // 순위 통계 업데이트
        member.updateMultiRankStats(rank);

        memberRepository.save(member);

        log.info("LP 적용 - 회원: {}, 순위: {}/{}명, LP: {} -> {} ({}), 티어: {} -> {}{}",
                member.getNickname(), rank, totalPlayers,
                oldLp, member.getMultiLp(), lpChange >= 0 ? "+" + lpChange : lpChange,
                oldTier.getDisplayName(), member.getMultiTier().getDisplayName(),
                tierChange != null ? " [" + tierChange + "]" : "");

        return new LpChangeResult(
                memberId,
                member.getNickname(),
                rank,
                score,
                oldTier,
                oldLp,
                member.getMultiTier(),
                member.getMultiLp(),
                lpChange,
                tierChange
        );
    }

    /**
     * LP 변화량만 계산 (적용 없이)
     */
    public int calculateLpChange(int totalPlayers, int rank) {
        return MultiTier.calculateLpChange(totalPlayers, rank);
    }

    /**
     * 회원의 현재 멀티 티어 정보 조회
     */
    @Transactional(readOnly = true)
    public MultiTierInfo getMultiTierInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        return new MultiTierInfo(
                member.getMultiTier() != null ? member.getMultiTier() : MultiTier.BRONZE,
                member.getMultiLp() != null ? member.getMultiLp() : 0,
                member.getMultiWins() != null ? member.getMultiWins() : 0,
                member.getMultiTop3() != null ? member.getMultiTop3() : 0,
                member.getMultiGames() != null ? member.getMultiGames() : 0
        );
    }

    public record MultiTierInfo(
            MultiTier tier,
            int lp,
            int wins,
            int top3,
            int totalGames
    ) {}
}
