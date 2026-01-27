// 토스트 알림 표시 (전역 함수)
function showToast(message, type = 'info') {
    // 기존 토스트 제거
    const existingToast = document.querySelector('.toast-notification');
    if (existingToast) {
        existingToast.remove();
    }

    const toast = document.createElement('div');
    toast.className = `toast-notification toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    // 애니메이션을 위해 약간의 딜레이 후 show 클래스 추가
    requestAnimationFrame(() => {
        toast.classList.add('show');
    });

    // 3초 후 자동 제거
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// HTML 이스케이프 (XSS 방지)
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Common utilities
const Utils = {
    formatDate(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleDateString('ko-KR');
    },
    
    formatDateTime(dateStr) {
        if (!dateStr) return '-';
        const date = new Date(dateStr);
        return date.toLocaleString('ko-KR');
    },
    
    formatNumber(num) {
        if (num === null || num === undefined) return '0';
        return num.toLocaleString('ko-KR');
    },
    
    debounce(func, wait) {
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
};

// Global error handler (운영 환경에서는 로깅 서비스로 전송 권장)
window.addEventListener('unhandledrejection', function(event) {
    // 운영 환경에서는 console.error 대신 로깅 서비스 사용 권장
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        console.error('Unhandled promise rejection:', event.reason);
    }
});

// ========== 중복 로그인 감지 (세션 유효성 체크) ==========
const SessionManager = {
    checkInterval: null,
    isChecking: false,

    // 세션 체크 시작 (30초 간격)
    startSessionCheck() {
        // 인증 페이지에서는 체크하지 않음
        if (window.location.pathname.startsWith('/auth/')) {
            return;
        }

        // 이미 체크 중이면 중복 실행 방지
        if (this.checkInterval) {
            return;
        }

        // 30초마다 세션 유효성 체크
        this.checkInterval = setInterval(() => this.validateSession(), 30000);

        // 페이지 로드 시 즉시 1회 체크
        setTimeout(() => this.validateSession(), 1000);
    },

    // 세션 체크 중지
    stopSessionCheck() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
        }
    },

    // 세션 유효성 검증
    async validateSession() {
        if (this.isChecking) return;
        this.isChecking = true;

        try {
            const response = await fetch('/auth/validate-session', {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });

            const result = await response.json();

            if (!result.valid && result.reason === 'SESSION_INVALIDATED') {
                this.handleSessionInvalidated(result.message);
            }
        } catch (error) {
            // 네트워크 오류는 무시 (오프라인 등)
            // 운영 환경에서는 로깅 서비스 사용 권장
        } finally {
            this.isChecking = false;
        }
    },

    // 세션 무효화 처리 (다른 기기에서 로그인됨)
    handleSessionInvalidated(message) {
        this.stopSessionCheck();

        // 토스트 알림 표시
        const msg = message || '다른 기기에서 로그인하여 현재 세션이 종료되었습니다.';
        showToast(msg, 'warning');

        // 잠시 후 로그인 페이지로 이동 (토스트 확인 시간)
        setTimeout(() => {
            window.location.href = '/auth/login';
        }, 1500);
    }
};

// 전역 fetch 래퍼 - AJAX 응답에서 세션 무효화 감지
const originalFetch = window.fetch;
window.fetch = async function(...args) {
    const response = await originalFetch.apply(this, args);

    // 401 응답에서 세션 무효화 감지
    if (response.status === 401) {
        try {
            const clonedResponse = response.clone();
            const result = await clonedResponse.json();

            if (result.error === 'SESSION_INVALIDATED') {
                SessionManager.handleSessionInvalidated(result.message);
            }
        } catch (e) {
            // JSON 파싱 실패 시 무시
        }
    }

    return response;
};

// 페이지 로드 시 세션 체크 시작
document.addEventListener('DOMContentLoaded', function() {
    SessionManager.startSessionCheck();
});