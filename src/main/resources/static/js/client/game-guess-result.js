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

/**
 * 같은 설정으로 다시하기
 */
async function restartWithSameSettings() {
    const restartData = document.getElementById('restartData');
    const sessionId = restartData.dataset.sessionId;

    try {
        const response = await fetch('/game/solo/guess/restart', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ previousSessionId: sessionId })
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = '/game/solo/guess/play';
        } else {
            alert(result.message || '재시작에 실패했습니다.');
            if (result.redirectUrl) {
                window.location.href = result.redirectUrl;
            }
        }
    } catch (error) {
        console.error('Restart error:', error);
        alert('재시작 중 오류가 발생했습니다.');
    }
}
