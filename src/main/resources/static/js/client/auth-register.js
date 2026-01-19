/**
 * client/auth/register.html - 회원가입
 */

let emailAvailable = false;

// 이메일 중복 체크
document.getElementById('email').addEventListener('blur', async function() {
    const email = this.value;
    const emailCheck = document.getElementById('emailCheck');
    const emailHint = document.getElementById('emailHint');

    if (!email || !email.match(/^[A-Za-z0-9+_.-]+@(.+)$/)) {
        emailCheck.textContent = '';
        emailHint.textContent = '';
        emailAvailable = false;
        return;
    }

    try {
        const response = await fetch(`/auth/check-email?email=${encodeURIComponent(email)}`);
        const result = await response.json();

        if (result.available) {
            emailCheck.textContent = '✓';
            emailCheck.className = 'check-icon available';
            emailHint.textContent = '사용 가능한 이메일입니다.';
            emailHint.className = 'field-hint success';
            emailAvailable = true;
        } else {
            emailCheck.textContent = '✗';
            emailCheck.className = 'check-icon unavailable';
            emailHint.textContent = '이미 사용 중인 이메일입니다.';
            emailHint.className = 'field-hint error';
            emailAvailable = false;
        }
    } catch (error) {
        // console.error(error);
    }
});

// 비밀번호 확인
document.getElementById('passwordConfirm').addEventListener('input', function() {
    const password = document.getElementById('password').value;
    const passwordHint = document.getElementById('passwordHint');

    if (this.value && this.value !== password) {
        passwordHint.textContent = '비밀번호가 일치하지 않습니다.';
        passwordHint.className = 'field-hint error';
    } else if (this.value && this.value === password) {
        passwordHint.textContent = '비밀번호가 일치합니다.';
        passwordHint.className = 'field-hint success';
    } else {
        passwordHint.textContent = '';
    }
});

// 회원가입 폼 제출
document.getElementById('registerForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const passwordConfirm = document.getElementById('passwordConfirm').value;
    const nickname = document.getElementById('nickname').value;
    const username = document.getElementById('username').value;
    const errorMessage = document.getElementById('errorMessage');
    const successMessage = document.getElementById('successMessage');

    errorMessage.style.display = 'none';
    successMessage.style.display = 'none';

    // 유효성 검사
    if (!emailAvailable) {
        errorMessage.textContent = '사용 가능한 이메일을 입력해주세요.';
        errorMessage.style.display = 'block';
        return;
    }

    if (password !== passwordConfirm) {
        errorMessage.textContent = '비밀번호가 일치하지 않습니다.';
        errorMessage.style.display = 'block';
        return;
    }

    try {
        const response = await fetch('/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password, nickname, username })
        });

        const result = await response.json();

        if (result.success) {
            successMessage.textContent = '회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.';
            successMessage.style.display = 'block';
            setTimeout(() => {
                window.location.href = '/auth/login';
            }, 1500);
        } else {
            errorMessage.textContent = result.message;
            errorMessage.style.display = 'block';
        }
    } catch (error) {
        errorMessage.textContent = '회원가입 처리 중 오류가 발생했습니다.';
        errorMessage.style.display = 'block';
    }
});
