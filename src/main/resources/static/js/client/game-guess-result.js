/**
 * client/game/guess/result.html - 게임 결과
 */

// 챌린지 모드 완료 처리
document.addEventListener('DOMContentLoaded', function() {
    const isChallengeMode = sessionStorage.getItem('challengeMode') === 'true';

    if (isChallengeMode) {
        // 챌린지 완료 배너 표시
        const banner = document.getElementById('challengeCompleteBanner');
        if (banner) {
            banner.style.display = 'block';
        }

        // 랭킹 보기 버튼 표시
        const rankingBtn = document.getElementById('rankingBtn');
        if (rankingBtn) {
            rankingBtn.style.display = 'inline-flex';
        }

        // 세션스토리지 정리
        sessionStorage.removeItem('challengeMode');
    }
});
