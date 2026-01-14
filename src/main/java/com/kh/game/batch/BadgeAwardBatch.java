package com.kh.game.batch;

import com.kh.game.entity.Badge;
import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.BadgeService;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 뱃지 일괄 지급 배치
 * 기존 사용자들의 기록을 기반으로 뱃지를 일괄 지급합니다.
 * 주로 수동 실행 용도 (뱃지 시스템 도입 시 기존 유저 처리)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeAwardBatch {

    private final MemberRepository memberRepository;
    private final BadgeService badgeService;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_BADGE_AWARD";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAwarded = 0;
        int processedMembers = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작 - 기존 회원 뱃지 일괄 지급", BATCH_ID);

            // 모든 활성 회원 조회
            List<Member> members = memberRepository.findByStatus(Member.MemberStatus.ACTIVE);
            log.info("처리 대상 회원 수: {}명", members.size());

            for (Member member : members) {
                try {
                    List<Badge> awarded = badgeService.checkAllBadgesForMember(member);
                    if (!awarded.isEmpty()) {
                        totalAwarded += awarded.size();
                        log.debug("회원 {} 뱃지 지급: {}개", member.getNickname(), awarded.size());
                    }
                    processedMembers++;
                } catch (Exception e) {
                    log.warn("회원 {} 뱃지 지급 중 오류: {}", member.getId(), e.getMessage());
                }
            }

            resultMessage.append(String.format(
                    "뱃지 일괄 지급 완료. 처리 회원: %d명, 지급 뱃지: %d개",
                    processedMembers, totalAwarded
            ));

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString(),
                    totalAwarded,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 처리: {}명, 지급: {}개, 소요시간: {}ms",
                    BATCH_ID, processedMembers, totalAwarded, executionTime);

            return totalAwarded;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    totalAwarded,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
