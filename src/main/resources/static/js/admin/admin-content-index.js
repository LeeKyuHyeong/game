/**
 * 콘텐츠 관리 통합 페이지 JavaScript
 */

var currentTab = 'song';
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
        case 'song':
            url = '/admin/song/content';
            break;
        case 'answer':
            url = '/admin/answer/content';
            break;
        case 'genre':
            url = '/admin/genre/content';
            break;
        case 'popularity':
            url = '/admin/song-popularity/content';
            break;
        default:
            url = '/admin/song/content';
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
    // 동적으로 로드된 스크립트 실행 (인라인 스크립트만)
    const scripts = document.querySelectorAll('#tabContent script');
    scripts.forEach(script => {
        // src가 있는 스크립트는 이미 페이지에 로드되어 있으므로 건너뜀
        if (!script.src && script.textContent) {
            const newScript = document.createElement('script');
            newScript.textContent = script.textContent;
            document.body.appendChild(newScript);
        }
    });

    // 탭별 초기화 함수 호출
    if (currentTab === 'song' && typeof initSongTabScripts === 'function') {
        initSongTabScripts();
    }
}

// 정답 관리 탭 전용 함수들
function resetAnswerFilter() {
    document.querySelector('[name="keyword"]').value = '';
    loadTabContent('answer');
}

function sortAnswerBy(field) {
    var direction = (answerSort === field && answerDirection === 'asc') ? 'desc' : 'asc';
    loadTabContent('answer', {sort: field, direction: direction});
}

function loadAnswerPage(page) {
    loadTabContent('answer', {page: page, sort: answerSort || 'id', direction: answerDirection || 'desc'});
}

// 장르 관리 탭 전용 함수들
function resetGenreFilter() {
    document.querySelector('[name="keyword"]').value = '';
    loadTabContent('genre');
}

function sortGenreBy(field) {
    var direction = (genreSort === field && genreDirection === 'asc') ? 'desc' : 'asc';
    loadTabContent('genre', {sort: field, direction: direction});
}

function loadGenrePage(page) {
    loadTabContent('genre', {page: page, sort: genreSort || 'id', direction: genreDirection || 'desc'});
}

// 장르 모달 함수들
function openGenreModal() {
    const modal = document.getElementById('genreModal');
    if (modal) {
        modal.classList.add('show');
        document.getElementById('genreModalTitle').textContent = '장르 추가';
        document.getElementById('genreForm').reset();
        const genreIdInput = document.getElementById('genreIdInput');
        if (genreIdInput) genreIdInput.value = '';
    }
}

function closeGenreModal() {
    const modal = document.getElementById('genreModal');
    if (modal) modal.classList.remove('show');
}

// 노래 관리 탭에서 사용하는 함수
function refreshSongList() {
    const form = document.getElementById('filterForm');
    if (form) {
        const params = new URLSearchParams(new FormData(form)).toString();
        loadTabContent('song', params);
    } else {
        loadTabContent('song');
    }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // URL에서 탭 파라미터 확인
    const urlParams = new URLSearchParams(window.location.search);
    const tab = urlParams.get('tab') || currentTab;

    // 초기 탭 콘텐츠 로드
    loadTabContent(tab);

    // 탭 버튼 활성화
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
