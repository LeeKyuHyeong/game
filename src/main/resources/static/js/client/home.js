// Client home page scripts
let isUserLoggedIn = false;

document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
});

async function checkLoginStatus() {
    try {
        const response = await fetch('/auth/status');
        const result = await response.json();

        const userInfo = document.getElementById('userInfo');

        if (result.isLoggedIn) {
            isUserLoggedIn = true;
            const adminBtn = result.role === 'ADMIN' ? '<a href="/admin/login" class="btn btn-admin">관리자</a>' : '';
            userInfo.innerHTML = `
                <span class="user-greeting">안녕하세요, <strong>${result.nickname}</strong>님!</span>
                <button class="btn btn-logout" onclick="logout()">로그아웃</button>
                ${adminBtn}
            `;
            // 내 순위 섹션 표시
            loadMyRanking();
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

async function loadMyRanking() {
    try {
        const response = await fetch('/api/ranking/my');
        const data = await response.json();

        const section = document.getElementById('myRankSection');
        const content = document.getElementById('myRankContent');

        if (!data.loggedIn) {
            return;
        }

        section.style.display = 'block';

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