/**
 * client/game/genre-challenge/result.html - 장르 챌린지 결과
 */

document.addEventListener('DOMContentLoaded', function() {
    // 랭킹 섹션 드래그 스크롤 활성화
    const rankingList = document.querySelector('.ranking-list');
    if (rankingList && typeof enableDragScroll === 'function') {
        enableDragScroll(rankingList);
    }

    // 라운드 그리드 드래그 스크롤 활성화
    const roundsGrid = document.querySelector('.rounds-grid');
    if (roundsGrid && typeof enableDragScroll === 'function') {
        enableDragScroll(roundsGrid);
    }
});

/**
 * 같은 장르로 다시 도전
 */
async function restartWithSameGenre() {
    const restartData = document.getElementById('restartData');
    if (!restartData) {
        alert('재시작 정보를 찾을 수 없습니다.');
        window.location.href = '/game/genre-challenge';
        return;
    }

    const sessionId = restartData.dataset.sessionId;

    try {
        const response = await fetch('/game/genre-challenge/restart', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ previousSessionId: sessionId })
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = '/game/genre-challenge/play';
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
