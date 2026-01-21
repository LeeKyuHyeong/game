// Client home page scripts - Bento Grid Version
let isUserLoggedIn = false;

document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    loadArtistChallengeRanking();
    loadRankingPreview();
});

// 디바운스 유틸리티
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

async function checkLoginStatus() {
    try {
        const response = await fetch('/auth/status');
        const result = await response.json();

        const userInfoDesktop = document.getElementById('userInfoDesktop');
        const userInfoMobile = document.getElementById('userInfoMobile');
        const bentoMyRank = document.getElementById('bentoMyRank');
        const bentoRanking = document.getElementById('bentoRanking');

        if (result.isLoggedIn) {
            isUserLoggedIn = true;
            const adminBtn = result.role === 'ADMIN' ? '<a href="/admin/login" class="btn btn-admin">관리자</a>' : '';
            const adminBtnMobile = result.role === 'ADMIN' ? '<a href="/admin/login" class="mobile-menu-link admin">🛠️ 관리자</a>' : '';

            // 데스크탑 UI
            userInfoDesktop.innerHTML = `
                <span class="user-greeting">안녕하세요, <strong>${result.nickname}</strong>님!</span>
                <a href="/mypage" class="btn btn-mypage">마이페이지</a>
                <button class="btn btn-logout" onclick="logout()">로그아웃</button>
                ${adminBtn}
            `;

            // 모바일 UI
            userInfoMobile.innerHTML = `
                <div class="mobile-user-greeting">안녕하세요, <strong>${result.nickname}</strong>님!</div>
                <a href="/mypage" class="mobile-menu-link">👤 마이페이지</a>
                <button class="mobile-menu-link" onclick="logout()">🚪 로그아웃</button>
                ${adminBtnMobile}
            `;

            // 벤토 카드: 로그인 시 내 순위 표시, 전체 랭킹 숨김
            if (bentoMyRank) bentoMyRank.classList.remove('hidden');
            if (bentoRanking) bentoRanking.classList.add('hidden');

            // 내 순위 로딩
            loadMyRanking();
        } else {
            isUserLoggedIn = false;

            // 데스크탑 UI
            userInfoDesktop.innerHTML = `
                <a href="/auth/login" class="btn btn-login">로그인</a>
                <a href="/auth/register" class="btn btn-register">회원가입</a>
            `;

            // 모바일 UI
            userInfoMobile.innerHTML = `
                <a href="/auth/login" class="mobile-menu-link">🔑 로그인</a>
                <a href="/auth/register" class="mobile-menu-link primary">✨ 회원가입</a>
            `;

            // 벤토 카드: 비로그인 시 내 순위 숨김
            if (bentoMyRank) bentoMyRank.classList.add('hidden');
        }
    } catch (error) {
        // console.error('로그인 상태 확인 오류:', error);
    }
}

// 모바일 메뉴 토글
function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    const btn = document.getElementById('hamburgerBtn');
    const isOpen = menu.classList.toggle('open');

    btn.setAttribute('aria-expanded', isOpen);
    btn.setAttribute('aria-label', isOpen ? '메뉴 닫기' : '메뉴 열기');
}

// 메뉴 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    const menu = document.getElementById('mobileMenu');
    const btn = document.getElementById('hamburgerBtn');

    if (menu && btn && !menu.contains(e.target) && !btn.contains(e.target)) {
        menu.classList.remove('open');
        btn.setAttribute('aria-expanded', 'false');
        btn.setAttribute('aria-label', '메뉴 열기');
    }
});

async function logout() {
    try {
        await fetch('/auth/logout', { method: 'POST' });
        window.location.reload();
    } catch (error) {
        // console.error('로그아웃 오류:', error);
    }
}

async function loadMyRanking() {
    try {
        const response = await fetch('/api/ranking/my');
        const data = await response.json();

        const content = document.getElementById('myRankContent');

        if (!data.loggedIn || !content) {
            return;
        }

        if (data.guessGames > 0) {
            content.innerHTML = `
                <span class="myrank-tier tier-${data.tierName?.toLowerCase() || 'bronze'}">${data.tierDisplayName}</span>
                <div class="myrank-stats">
                    <span class="myrank-rank">#${data.guessRank}</span>
                    <span class="myrank-score">${data.guessScore.toLocaleString()}점</span>
                </div>
            `;
        } else {
            content.innerHTML = `
                <span class="myrank-tier tier-${data.tierName?.toLowerCase() || 'bronze'}">${data.tierDisplayName}</span>
                <div class="myrank-stats">
                    <span class="myrank-score">게임 기록 없음</span>
                </div>
            `;
        }
    } catch (error) {
        // console.error('내 순위 로딩 오류:', error);
    }
}

async function loadArtistChallengeRanking() {
    try {
        const response = await fetch('/game/fan-challenge/top-artists');
        const data = await response.json();

        const section = document.getElementById('bentoArtistTop');
        const scroll = document.getElementById('artistRankingScroll');

        if (!data || data.length === 0 || !section || !scroll) {
            return;
        }

        section.classList.remove('hidden');

        let html = '';
        data.forEach(item => {
            const scoreText = `${item.correctCount}/${item.totalSongs}`;
            const badgeHtml = item.isPerfectClear
                ? '<span class="artist-card-badge">PERFECT</span>'
                : '';
            const timeHtml = item.bestTimeMs
                ? `<div class="artist-card-time">${(item.bestTimeMs / 1000).toFixed(1)}s</div>`
                : '';

            html += `
                <div class="artist-card">
                    <div class="artist-card-icon">🎵</div>
                    <div class="artist-card-name" title="${item.artist}">${item.artist}</div>
                    <div class="artist-card-user">${item.nickname}</div>
                    <div class="artist-card-score">${scoreText}</div>
                    ${timeHtml}
                    ${badgeHtml}
                </div>
            `;
        });

        scroll.innerHTML = html;

        // PC 드래그 스크롤 활성화
        if (typeof enableDragScroll === 'function') {
            enableDragScroll(scroll);
        }
    } catch (error) {
        // console.error('아티스트 챌린지 랭킹 로딩 오류:', error);
    }
}

// 전체 랭킹 미리보기 로딩 (TOP 3)
async function loadRankingPreview() {
    try {
        const response = await fetch('/api/ranking?mode=guess&type=score&period=all&limit=3');
        const data = await response.json();

        const preview = document.getElementById('rankingPreview');
        if (!preview || !Array.isArray(data) || data.length === 0) {
            return;
        }

        const medals = ['🥇', '🥈', '🥉'];
        let html = '';

        data.slice(0, 3).forEach((item, index) => {
            html += `
                <div class="ranking-preview-item">
                    <span class="ranking-preview-rank">${medals[index]}</span>
                    <span class="ranking-preview-name">${item.nickname}</span>
                    <span class="ranking-preview-score">${item.totalScore?.toLocaleString() || 0}</span>
                </div>
            `;
        });

        preview.innerHTML = html;
    } catch (error) {
        // console.error('랭킹 미리보기 로딩 오류:', error);
    }
}
