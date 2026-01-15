/**
 * Theme Manager - 다크/라이트 테마 전환 관리
 *
 * 기능:
 * - localStorage 기반 테마 저장/복원
 * - 시스템 설정 (prefers-color-scheme) 감지
 * - FOUC (Flash of Unstyled Content) 방지
 */

// FOUC 방지: 페이지 로드 전 즉시 실행 (이 코드는 head에서 인라인으로도 실행됨)
(function() {
    try {
        const saved = localStorage.getItem('theme-preference');
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        const theme = saved || (prefersDark ? 'dark' : 'light');
        document.documentElement.setAttribute('data-theme', theme);
    } catch (e) {
        // localStorage 접근 실패 시 기본 라이트 모드
        document.documentElement.setAttribute('data-theme', 'light');
    }
})();

/**
 * ThemeManager 클래스
 */
class ThemeManager {
    constructor() {
        this.STORAGE_KEY = 'theme-preference';
        this.init();
    }

    /**
     * 초기화
     */
    init() {
        // 현재 테마 확인 (이미 FOUC 방지 코드에서 설정됨)
        const currentTheme = document.documentElement.getAttribute('data-theme');
        this.updateToggleButton(currentTheme);

        // 시스템 설정 변경 감지
        this.watchSystemTheme();
    }

    /**
     * 시스템 테마 변경 감지
     */
    watchSystemTheme() {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

        mediaQuery.addEventListener('change', (e) => {
            // 사용자가 명시적으로 테마를 선택하지 않은 경우에만 시스템 설정 따르기
            if (!localStorage.getItem(this.STORAGE_KEY)) {
                const theme = e.matches ? 'dark' : 'light';
                this.setTheme(theme, false);
            }
        });
    }

    /**
     * 테마 설정
     * @param {string} theme - 'light' 또는 'dark'
     * @param {boolean} save - localStorage에 저장할지 여부
     */
    setTheme(theme, save = true) {
        document.documentElement.setAttribute('data-theme', theme);

        if (save) {
            try {
                localStorage.setItem(this.STORAGE_KEY, theme);
            } catch (e) {
                console.warn('테마 저장 실패:', e);
            }
        }

        this.updateToggleButton(theme);

        // 커스텀 이벤트 발생 (다른 스크립트에서 활용 가능)
        window.dispatchEvent(new CustomEvent('themechange', { detail: { theme } }));
    }

    /**
     * 테마 토글
     */
    toggle() {
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = current === 'dark' ? 'light' : 'dark';
        this.setTheme(newTheme);
    }

    /**
     * 현재 테마 반환
     * @returns {string}
     */
    getTheme() {
        return document.documentElement.getAttribute('data-theme') || 'light';
    }

    /**
     * 토글 버튼 aria-label 업데이트
     * @param {string} theme
     */
    updateToggleButton(theme) {
        const buttons = document.querySelectorAll('.theme-toggle');
        buttons.forEach(btn => {
            const label = theme === 'dark' ? '라이트 모드로 전환' : '다크 모드로 전환';
            btn.setAttribute('aria-label', label);
            btn.setAttribute('title', label);
        });
    }

    /**
     * 저장된 테마 설정 초기화 (시스템 설정으로 리셋)
     */
    reset() {
        try {
            localStorage.removeItem(this.STORAGE_KEY);
        } catch (e) {
            console.warn('테마 설정 초기화 실패:', e);
        }

        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        this.setTheme(prefersDark ? 'dark' : 'light', false);
    }
}

// DOM 로드 후 ThemeManager 초기화
document.addEventListener('DOMContentLoaded', () => {
    window.themeManager = new ThemeManager();
});

// 전역 함수로도 토글 가능 (onclick에서 사용)
function toggleTheme() {
    if (window.themeManager) {
        window.themeManager.toggle();
    } else {
        // ThemeManager가 아직 초기화되지 않은 경우 직접 토글
        const current = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', newTheme);
        try {
            localStorage.setItem('theme-preference', newTheme);
        } catch (e) {}
    }
}
