/**
 * 랭킹 관리 통합 페이지 JavaScript
 */

var currentTab = 'multi';
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

    let url = '/admin/ranking/content';
    let rankType = '';

    switch (tab) {
        case 'multi':
            rankType = 'multi';
            break;
        case 'challenge30':
            rankType = 'weeklyBest30';
            break;
        case 'artist':
            rankType = 'fan';
            break;
        case 'genre':
            rankType = 'genreTotal';
            break;
        case 'retro':
            rankType = 'retro';
            break;
        default:
            rankType = 'multi';
    }

    // 파라미터 설정
    const queryParams = params || {};
    queryParams.rankType = rankType;
    queryParams.tab = tab;
    const searchParams = new URLSearchParams(queryParams);
    url += '?' + searchParams.toString();

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
