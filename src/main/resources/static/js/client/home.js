// Client home page scripts
let isUserLoggedIn = false;

document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    applyCheckerboardPattern();
    loadArtistChallengeRanking();
});

// 화면 리사이즈 시 체커보드 패턴 재적용
window.addEventListener('resize', debounce(applyCheckerboardPattern, 150));

// 체커보드 패턴 자동 적용 (열 수 자동 감지)
function applyCheckerboardPattern() {
    document.querySelectorAll('.mode-grid').forEach(grid => {
        const items = grid.querySelectorAll('.grid-item');
        if (items.length === 0) return;

        // 그리드의 실제 열 수 계산
        const gridStyle = window.getComputedStyle(grid);
        const columns = gridStyle.gridTemplateColumns.split(' ').length;

        items.forEach((item, index) => {
            const row = Math.floor(index / columns);
            const col = index % columns;

            // 체커보드: (row + col) % 2 === 1 이면 대체 색상
            if ((row + col) % 2 === 1) {
                item.classList.add('checker-alt');
            } else {
                item.classList.remove('checker-alt');
            }
        });
    });
}

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

            // 내 순위 섹션 표시
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
        }
    } catch (error) {
        console.error('로그인 상태 확인 오류:', error);
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

// 내가 맞추기 버튼 클릭 핸들러
function handleGuessClick() {
    if (isUserLoggedIn) {
        // 로그인 상태면 바로 게임으로 이동
        window.location.href = '/game/solo/guess';
    } else {
        // 비로그인 상태면 모달 표시
        showLoginPrompt();
    }
}

// 로그인 안내 모달 표시
function showLoginPrompt() {
    const modal = document.getElementById('loginPromptModal');
    if (modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

// 로그인 안내 모달 닫기
function closeLoginPrompt() {
    const modal = document.getElementById('loginPromptModal');
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }
}

// 비로그인으로 진행
function proceedWithoutLogin() {
    closeLoginPrompt();
    window.location.href = '/game/solo/guess';
}

async function logout() {
    try {
        await fetch('/auth/logout', { method: 'POST' });
        window.location.reload();
    } catch (error) {
        console.error('로그아웃 오류:', error);
    }
}

async function loadMyRanking() {
    try {
        const response = await fetch('/api/ranking/my');
        const data = await response.json();

        const section = document.getElementById('myRankSection');
        const content = document.getElementById('myRankContent');

        if (!data.loggedIn) {
            return;
        }

        section.classList.remove('hidden');

        if (data.guessGames > 0) {
            content.innerHTML = `
                <div class="my-rank-info">
                    <span class="tier-badge" style="background: ${data.tierColor}">${data.tierDisplayName}</span>
                    <span class="rank-text">내 순위: <strong>${data.guessRank}위</strong> / ${data.guessTotal}명</span>
                    <span class="score-text">총점 ${data.guessScore.toLocaleString()}점</span>
                </div>
            `;
        } else {
            content.innerHTML = `
                <div class="my-rank-info">
                    <span class="tier-badge" style="background: ${data.tierColor}">${data.tierDisplayName}</span>
                    <span class="rank-text">아직 게임 기록이 없습니다</span>
                </div>
            `;
        }
    } catch (error) {
        console.error('내 순위 로딩 오류:', error);
    }
}

async function loadArtistChallengeRanking() {
    try {
        const response = await fetch('/game/fan-challenge/top-artists');
        const data = await response.json();

        const section = document.getElementById('artistRankingSection');
        const scroll = document.getElementById('artistRankingScroll');

        if (!data || data.length === 0) {
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
        enableDragScroll(scroll);
    } catch (error) {
        console.error('아티스트 챌린지 랭킹 로딩 오류:', error);
    }
}