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
