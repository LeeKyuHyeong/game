/**
 * client/auth/login.html - 로그인
 */

document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    await attemptLogin(false);
});

async function attemptLogin(forceLogin) {
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorMessage = document.getElementById('errorMessage');

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password, forceLogin: forceLogin ? 'true' : 'false' })
        });

        const result = await response.json();

        if (result.success) {
            const redirect = document.getElementById('redirectUrl').value;
            window.location.href = redirect || '/';
        } else if (result.requireConfirm) {
            // 게임 중인 세션 존재 - 확인 필요
            const confirmMsg = result.inGame
                ? '현재 다른 기기에서 게임이 진행 중입니다.\n강제 로그인 시 해당 게임이 중단됩니다.\n\n강제 로그인하시겠습니까?'
                : '다른 기기에서 이미 로그인 중입니다.\n강제 로그인하시겠습니까?';

            if (confirm(confirmMsg)) {
                await attemptLogin(true);
            }
        } else {
            errorMessage.textContent = result.message;
            errorMessage.style.display = 'block';
        }
    } catch (error) {
        errorMessage.textContent = '로그인 처리 중 오류가 발생했습니다.';
        errorMessage.style.display = 'block';
    }
}
