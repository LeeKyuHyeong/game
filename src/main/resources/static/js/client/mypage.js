// 마이페이지 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // 페이지 로드 시 새 뱃지 체크
    checkNewBadges();

    // 뱃지 클릭 이벤트 (이벤트 위임)
    const badgeGrid = document.querySelector('.badge-grid');
    if (badgeGrid) {
        badgeGrid.addEventListener('click', function(e) {
            const badgeItem = e.target.closest('.badge-item');
            if (badgeItem && badgeItem.dataset.owned === 'true') {
                const badgeId = badgeItem.dataset.badgeId;
                selectBadge(badgeId);
            }
        });
    }
});

// 뱃지 선택
async function selectBadge(badgeId) {
    try {
        const response = await fetch('/mypage/badge/select', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ badgeId: badgeId })
        });

        const result = await response.json();

        if (result.success) {
            // 페이지 새로고침으로 UI 업데이트
            window.location.reload();
        } else {
            alert(result.message || '뱃지 선택에 실패했습니다.');
        }
    } catch (error) {
        console.error('뱃지 선택 오류:', error);
        alert('뱃지 선택 중 오류가 발생했습니다.');
    }
}

// 뱃지 선택 해제
async function clearBadge() {
    try {
        const response = await fetch('/mypage/badge/select', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ badgeId: null })
        });

        const result = await response.json();

        if (result.success) {
            window.location.reload();
        } else {
            alert(result.message || '뱃지 해제에 실패했습니다.');
        }
    } catch (error) {
        console.error('뱃지 해제 오류:', error);
        alert('뱃지 해제 중 오류가 발생했습니다.');
    }
}

// 새 뱃지 체크 및 읽음 처리
async function checkNewBadges() {
    try {
        const response = await fetch('/mypage/badges/new');
        const newBadges = await response.json();

        if (newBadges.length > 0) {
            // 새 뱃지가 있으면 읽음 처리
            await fetch('/mypage/badges/mark-read', { method: 'POST' });
        }
    } catch (error) {
        console.error('새 뱃지 체크 오류:', error);
    }
}
