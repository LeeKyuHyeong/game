// Client home page scripts
let isUserLoggedIn = false;

document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    setupRankingAccordion();
});

// 모바일에서 랭킹 섹션 아코디언 토글
function setupRankingAccordion() {
    const rankingHeader = document.getElementById('rankingHeader');
    const rankingSection = document.getElementById('rankingSection');

    if (rankingHeader && rankingSection) {
        // 모바일에서 기본 접힘 상태
        if (window.innerWidth <= 768) {
            rankingSection.classList.add('collapsed');
        }

        rankingHeader.addEventListener('click', function() {
            if (window.innerWidth <= 768) {
                rankingSection.classList.toggle('collapsed');
            }
        });

        // 화면 크기 변경 시 처리
        window.addEventListener('resize', function() {
            if (window.innerWidth > 768) {
                rankingSection.classList.remove('collapsed');
            }
        });
    }
}

async function checkLoginStatus() {
    try {
        const response = await fetch('/auth/status');
        const result = await response.json();

        const userInfo = document.getElementById('userInfo');

        if (result.isLoggedIn) {
            isUserLoggedIn = true;
            userInfo.innerHTML = `
                <span class="user-greeting">안녕하세요, <strong>${result.nickname}</strong>님!</span>
                <button class="btn btn-logout" onclick="logout()">로그아웃</button>
            `;
            // 랭킹 섹션 표시
            document.getElementById('rankingSection').style.display = 'block';
            loadRanking('score');
            setupRankingTabs();
        } else {
            isUserLoggedIn = false;
            userInfo.innerHTML = `
                <a href="/auth/login" class="btn btn-login">로그인</a>
                <a href="/auth/register" class="btn btn-register">회원가입</a>
            `;
        }
    } catch (error) {
        console.error('로그인 상태 확인 오류:', error);
    }
}

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

function setupRankingTabs() {
    document.querySelectorAll('.ranking-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('.ranking-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            loadRanking(this.dataset.type);
        });
    });
}

async function loadRanking(type) {
    try {
        const response = await fetch(`/api/ranking?type=${type}&limit=5`);
        const rankings = await response.json();

        const list = document.getElementById('rankingList');

        if (rankings.length === 0) {
            list.innerHTML = '<div class="empty-ranking">아직 랭킹 데이터가 없습니다.</div>';
            return;
        }

        list.innerHTML = rankings.map((member, index) => `
            <div class="ranking-item">
                <span class="rank">${index + 1}</span>
                <span class="nickname">${member.nickname}</span>
                <span class="value">${formatRankValue(type, member)}</span>
            </div>
        `).join('');
    } catch (error) {
        console.error('랭킹 로딩 오류:', error);
    }
}

function formatRankValue(type, member) {
    switch(type) {
        case 'score':
            return member.totalScore.toLocaleString() + '점';
        case 'accuracy':
            return member.accuracyRate.toFixed(1) + '%';
        case 'games':
            return member.totalGames + '게임';
        default:
            return '';
    }
}