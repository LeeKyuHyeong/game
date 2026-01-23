/**
 * 시스템 설정 통합 페이지 JavaScript
 */

var currentTab = 'batch';
var currentParams = {};

// 탭 전환
function switchTab(tab) {
    currentTab = tab;
    currentParams = {};

    // 탭 버튼 활성화 상태 변경
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector('[data-tab="' + tab + '"]').classList.add('active');

    // URL 업데이트
    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    history.pushState({tab: tab}, '', url);

    // 콘텐츠 로드
    loadTabContent(tab);
}

// 탭 콘텐츠 로드
function loadTabContent(tab, params) {
    const container = document.getElementById('tabContent');
    container.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    let url = '';
    switch (tab) {
        case 'batch':
            url = '/admin/batch/content';
            break;
        case 'menu':
            url = '/admin/menu/content';
            break;
        default:
            url = '/admin/batch/content';
    }

    // 파라미터 추가
    if (params) {
        const searchParams = new URLSearchParams(params);
        url += '?' + searchParams.toString();
    }

    fetch(url)
        .then(response => response.text())
        .then(html => {
            container.innerHTML = html;
            initializeScripts();
        })
        .catch(error => {
            container.innerHTML = '<div class="error-message">콘텐츠를 불러오는데 실패했습니다.</div>';
            console.error('Error loading tab content:', error);
        });
}

// 스크립트 초기화
function initializeScripts() {
    // 인라인 스크립트만 실행 (src 있는 스크립트는 이미 로드됨)
    const scripts = document.querySelectorAll('#tabContent script');
    scripts.forEach(script => {
        if (!script.src && script.textContent) {
            const newScript = document.createElement('script');
            newScript.textContent = script.textContent;
            document.body.appendChild(newScript);
        }
    });

    // 탭별 초기화 함수 호출
    if (currentTab === 'batch' && typeof initBatchTab === 'function') {
        initBatchTab();
    } else if (currentTab === 'menu' && typeof initMenuTab === 'function') {
        initMenuTab();
    }
}

// 배치 관리 함수들 (admin-batch.js 에서 재정의)
function refreshBatchSchedules() {
    if (typeof refreshSchedules === 'function') {
        refreshSchedules();
    }
}

function closeBatchModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const tab = urlParams.get('tab') || currentTab;

    loadTabContent(tab);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.querySelector('[data-tab="' + tab + '"]');
    if (activeBtn) {
        activeBtn.classList.add('active');
    }
});

// 브라우저 뒤로가기/앞으로가기 처리
window.addEventListener('popstate', function(event) {
    if (event.state && event.state.tab) {
        loadTabContent(event.state.tab);
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        document.querySelector('[data-tab="' + event.state.tab + '"]').classList.add('active');
    }
});
