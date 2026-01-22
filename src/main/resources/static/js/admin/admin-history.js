// admin-history.js - 게임 관리 페이지

let currentTab = 'history';

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    // activeTab은 HTML에서 설정됨
    if (typeof activeTab !== 'undefined') {
        currentTab = activeTab;
    }
    loadTabContent(currentTab);
});

// 탭 전환
function switchTab(tab) {
    if (currentTab === tab) return;
    currentTab = tab;

    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    window.history.pushState({}, '', url);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector(`.tab-btn:nth-child(${tab === 'history' ? 1 : 2})`).classList.add('active');

    loadTabContent(tab);
}

// 탭 콘텐츠 로드
async function loadTabContent(tab, params = '') {
    const tabContent = document.getElementById('tabContent');
    tabContent.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    try {
        const url = tab === 'history'
            ? `/admin/history/content${params ? '?' + params : ''}`
            : `/admin/history/ranking/content${params ? '?' + params : ''}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load content');

        tabContent.innerHTML = await response.text();
        initTabScripts();
    } catch (error) {
        tabContent.innerHTML = `<div class="error-message"><p>콘텐츠를 불러오는데 실패했습니다.</p><button class="btn btn-primary" onclick="loadTabContent('${tab}')">다시 시도</button></div>`;
    }
}

// 탭 내 스크립트 초기화
function initTabScripts() {
    const filterForm = document.querySelector('.tab-content .filter-form');
    if (filterForm) {
        filterForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const params = new URLSearchParams(new FormData(filterForm)).toString();
            loadTabContent(currentTab, params);
        });
    }

    const resetBtn = document.querySelector('.tab-content .btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', (e) => {
            e.preventDefault();
            loadTabContent(currentTab);
        });
    }
}

// 행 펼치기/접기 (모바일)
function toggleRowExpand(row) {
    if (window.innerWidth <= 768) row.classList.toggle('expanded');
}

// 페이지 이동
function goToPage(page) {
    const form = document.querySelector('.tab-content .filter-form');
    const params = new URLSearchParams();
    if (form) new FormData(form).forEach((v, k) => { if (v) params.set(k, v); });
    params.set('page', page);
    loadTabContent(currentTab, params.toString());
}

// 랭킹 타입 변경
function changeRankType(rankType, artist = '') {
    const params = new URLSearchParams();
    params.set('rankType', rankType);
    if (artist) params.set('artist', artist);
    loadTabContent('ranking', params.toString());
}

// 세션 삭제
async function deleteSession(id) {
    if (!confirm('정말 삭제하시겠습니까?\n해당 게임의 모든 라운드 정보도 함께 삭제됩니다.')) return;

    try {
        const response = await fetch(`/admin/history/delete/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast(result.message);
            location.reload();
        } else {
            showToast(result.message);
        }
    } catch (error) {
        showToast('삭제 중 오류가 발생했습니다.');
    }
}