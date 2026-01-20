/**
 * client/game/fan-challenge/result.html - 아티스트 챌린지 결과
 */

document.addEventListener('DOMContentLoaded', function() {
    const badgesCollection = document.querySelector('.badges-collection');
    if (badgesCollection) {
        enableDragScroll(badgesCollection);
    }

    // 곡 인기도 평가 버튼 이벤트
    initPopularityVote();
});

/**
 * 곡 인기도 평가 기능 초기화
 */
function initPopularityVote() {
    const voteButtons = document.querySelectorAll('.vote-btn');

    voteButtons.forEach(btn => {
        btn.addEventListener('click', async function() {
            const rating = parseInt(this.dataset.rating);
            const voteSection = this.closest('.popularity-vote-section');
            const songId = voteSection.dataset.songId;

            // 버튼 비활성화 (중복 클릭 방지)
            const allBtns = voteSection.querySelectorAll('.vote-btn');
            allBtns.forEach(b => b.disabled = true);

            try {
                const response = await fetch(`/game/fan-challenge/api/song/${songId}/popularity-vote`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ rating: rating })
                });

                const result = await response.json();

                if (result.success) {
                    // UI 업데이트: 버튼 -> 별점 표시
                    updateVoteDisplay(voteSection, rating, result.ratingLabel);
                } else if (response.status === 409) {
                    // 이미 투표한 경우
                    alert('이미 평가한 곡입니다');
                    // 페이지 새로고침하여 기존 투표 표시
                    location.reload();
                } else if (response.status === 401) {
                    // 로그인 필요
                    if (confirm('로그인이 필요합니다. 로그인 페이지로 이동하시겠습니까?')) {
                        window.location.href = '/login';
                    }
                } else {
                    alert(result.message || '평가 중 오류가 발생했습니다');
                    // 버튼 다시 활성화
                    allBtns.forEach(b => b.disabled = false);
                }
            } catch (error) {
                console.error('평가 제출 오류:', error);
                alert('평가 중 오류가 발생했습니다');
                // 버튼 다시 활성화
                allBtns.forEach(b => b.disabled = false);
            }
        });
    });
}

/**
 * 투표 후 UI 업데이트 (버튼 -> 별점 표시)
 */
function updateVoteDisplay(voteSection, rating, ratingLabel) {
    const voteButtons = voteSection.querySelector('.vote-buttons');
    if (!voteButtons) return;

    // 별점 생성
    let stars = '';
    for (let i = 1; i <= 5; i++) {
        stars += i <= rating
            ? '<span class="star filled">★</span>'
            : '<span class="star empty">☆</span>';
    }

    // HTML 교체
    const voteDisplay = document.createElement('div');
    voteDisplay.className = 'vote-display';
    voteDisplay.dataset.rating = rating;
    voteDisplay.innerHTML = `
        <span class="vote-label">내 평가:</span>
        <span class="vote-stars">${stars}</span>
        <span class="vote-rating-label">${ratingLabel}</span>
    `;

    voteButtons.replaceWith(voteDisplay);

    // 애니메이션 효과
    voteDisplay.classList.add('vote-submitted');
}
