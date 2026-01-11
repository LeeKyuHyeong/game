package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InactiveMemberBatch {

    private final MemberRepository memberRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_INACTIVE_MEMBER";

    @Transactional
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 6개월 이상 미접속 회원을 휴면 상태로 전환
            LocalDateTime threshold = LocalDateTime.now().minusMonths(6);
            List<Member> inactiveMembers = memberRepository.findAll().stream()
                    .filter(m -> m.getStatus() == Member.MemberStatus.ACTIVE)
                    .filter(m -> m.getRole() != Member.MemberRole.ADMIN) // 관리자는 제외
                    .filter(m -> {
                        LocalDateTime lastLogin = m.getLastLoginAt();
                        if (lastLogin == null) {
                            // 로그인 기록이 없으면 가입일 기준
                            return m.getCreatedAt() != null && m.getCreatedAt().isBefore(threshold);
                        }
                        return lastLogin.isBefore(threshold);
                    })
                    .toList();

            for (Member member : inactiveMembers) {
                member.setStatus(Member.MemberStatus.INACTIVE);
                log.debug("휴면 전환: {} ({})", member.getEmail(), member.getNickname());
            }

            totalAffected = inactiveMembers.size();

            if (totalAffected > 0) {
                resultMessage.append(String.format("6개월 미접속 회원 %d명 휴면 전환.", totalAffected));
            } else {
                resultMessage.append("휴면 전환할 회원이 없습니다.");
            }

            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.SUCCESS,
                    resultMessage.toString().trim(),
                    totalAffected,
                    executionTime
            );

            log.info("[{}] 배치 실행 완료 - 휴면 전환: {}명, 소요시간: {}ms",
                    BATCH_ID, totalAffected, executionTime);

            return totalAffected;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            batchService.recordExecution(
                    BATCH_ID,
                    executionType,
                    BatchConfig.ExecutionResult.FAIL,
                    "오류 발생: " + e.getMessage(),
                    totalAffected,
                    executionTime
            );

            log.error("[{}] 배치 실행 실패", BATCH_ID, e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage(), e);
        }
    }
}
