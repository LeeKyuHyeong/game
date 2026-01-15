package com.kh.game.batch;

import com.kh.game.entity.BatchConfig;
import com.kh.game.entity.BatchExecutionHistory;
import com.kh.game.entity.Member;
import com.kh.game.repository.MemberRepository;
import com.kh.game.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @deprecated 이 배치는 RankingSnapshotBatch로 대체되었습니다.
 * Top 100을 조회만 하고 저장하지 않는 무의미한 배치였습니다.
 * 실제 랭킹 스냅샷 저장은 {@link RankingSnapshotBatch}를 사용하세요.
 *
 * DB에서 batch_config.enabled = false로 비활성화되어 있어야 합니다.
 * 향후 버전에서 완전히 제거될 예정입니다.
 *
 * @see RankingSnapshotBatch
 */
@Deprecated(since = "2026-01-15", forRemoval = true)
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingUpdateBatch {

    private final MemberRepository memberRepository;
    private final BatchService batchService;

    public static final String BATCH_ID = "BATCH_RANKING_UPDATE";

    @Transactional(readOnly = true)
    public int execute(BatchExecutionHistory.ExecutionType executionType) {
        long startTime = System.currentTimeMillis();
        int totalAffected = 0;
        StringBuilder resultMessage = new StringBuilder();

        try {
            log.info("[{}] 배치 실행 시작", BATCH_ID);

            // 랭킹 집계 (현재는 캐시 갱신 개념으로 로그만 기록)
            // 향후 Redis 캐시나 별도 테이블에 저장 가능

            // 총점 Top 100
            List<Member> topByScore = memberRepository.findTopByTotalScore(PageRequest.of(0, 100));
            log.info("총점 Top 100 조회 완료: {}명", topByScore.size());

            // 정답률 Top 100
            List<Member> topByAccuracy = memberRepository.findTopByAccuracy(PageRequest.of(0, 100));
            log.info("정답률 Top 100 조회 완료: {}명", topByAccuracy.size());

            // 게임 수 Top 100
            List<Member> topByGames = memberRepository.findTopByTotalGames(PageRequest.of(0, 100));
            log.info("게임 수 Top 100 조회 완료: {}명", topByGames.size());

            totalAffected = topByScore.size() + topByAccuracy.size() + topByGames.size();

            resultMessage.append(String.format(
                    "랭킹 갱신 완료. 총점: %d명, 정답률: %d명, 게임수: %d명",
                    topByScore.size(), topByAccuracy.size(), topByGames.size()
            ));

            // 1위 정보 로깅
            if (!topByScore.isEmpty()) {
                Member top1 = topByScore.get(0);
                log.info("총점 1위: {} ({}점)", top1.getNickname(), top1.getTotalScore());
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

            log.info("[{}] 배치 실행 완료 - 소요시간: {}ms", BATCH_ID, executionTime);

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
