/**
 * 뱃지 획득 토스트 알림 시스템
 */
class BadgeToast {
    static show(badge) {
        const toast = document.createElement('div');
        toast.className = `badge-toast badge-rarity-${badge.rarity.toLowerCase()}`;
        toast.innerHTML = `
            <div class="badge-toast-icon">${badge.emoji}</div>
            <div class="badge-toast-content">
                <div class="badge-toast-title">새로운 뱃지 획득!</div>
                <div class="badge-toast-name">${badge.name}</div>
            </div>
        `;

        document.body.appendChild(toast);

        // 애니메이션 시작
        setTimeout(() => toast.classList.add('show'), 10);

        // 4초 후 사라짐
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    static showMultiple(badges) {
        badges.forEach((badge, index) => {
            setTimeout(() => {
                this.show(badge);
            }, index * 500);
        });
    }

    static async checkNewBadges() {
        try {
            const response = await fetch('/mypage/badges/new');
            const newBadges = await response.json();

            if (newBadges.length > 0) {
                this.showMultiple(newBadges);

                // 읽음 처리
                await fetch('/mypage/badges/mark-read', { method: 'POST' });
            }
        } catch (error) {
            // console.error('뱃지 체크 오류:', error);
        }
    }
}

// 게임 결과에서 받은 뱃지 표시 (게임 종료 시 사용)
function showNewBadgesFromResult(newBadges) {
    if (newBadges && newBadges.length > 0) {
        BadgeToast.showMultiple(newBadges);
    }
}

// 페이지 로드 시 로그인 상태면 새 뱃지 체크 (선택적)
// 게임 페이지에서는 게임 종료 시 직접 표시하므로 비활성화
// document.addEventListener('DOMContentLoaded', () => {
//     const isLoggedIn = document.body.dataset.loggedIn === 'true';
//     if (isLoggedIn) {
//         setTimeout(() => BadgeToast.checkNewBadges(), 1000);
//     }
// });
