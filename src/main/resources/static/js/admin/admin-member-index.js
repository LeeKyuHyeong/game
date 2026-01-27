/**
 * Admin Member Index Page - Tab Management & Member Functions
 */

var currentTab = currentTab || 'member';
var currentMemberId = currentMemberId || null;

// ========== Initialization ==========

document.addEventListener('DOMContentLoaded', () => {
    loadTabContent(currentTab);
    initForms();
});

// ========== Tab Management ==========

function switchTab(tab) {
    if (currentTab === tab) return;
    currentTab = tab;

    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    window.history.pushState({}, '', url);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    const tabIndex = tab === 'member' ? 1 : tab === 'login' ? 2 : 3;
    document.querySelector(`.tab-btn:nth-child(${tabIndex})`).classList.add('active');

    loadTabContent(tab);
}

async function loadTabContent(tab, params = '') {
    const tabContent = document.getElementById('tabContent');
    tabContent.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    try {
        let url;
        if (tab === 'member') {
            url = `/admin/member/content${params ? '?' + params : ''}`;
        } else if (tab === 'login') {
            url = `/admin/login-history/content${params ? '?' + params : ''}`;
        } else {
            url = `/admin/badword/content${params ? '?' + params : ''}`;
        }

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load content');

        tabContent.innerHTML = await response.text();
        initTabScripts();
    } catch (error) {
        tabContent.innerHTML = `<div class="error-message"><p>콘텐츠를 불러오는데 실패했습니다.</p><button class="btn btn-primary" onclick="loadTabContent('${tab}')">다시 시도</button></div>`;
    }
}

function initTabScripts() {
    // 동적으로 로드된 인라인 스크립트만 실행
    // 주의: 외부 스크립트(script.src)는 로드하지 않음 - 함수 충돌 방지
    const scripts = document.querySelectorAll('#tabContent script');

    scripts.forEach(script => {
        // 인라인 스크립트만 실행 (외부 스크립트는 무시)
        if (!script.src && script.textContent) {
            const newScript = document.createElement('script');
            newScript.textContent = script.textContent;
            document.body.appendChild(newScript);
        }
    });

    // 폼 이벤트 바인딩
    const searchForm = document.querySelector('.tab-content .search-form, .tab-content .filter-form');
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const params = new URLSearchParams(new FormData(searchForm)).toString();
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

// 외부 스크립트 순차 로드
function loadScriptsSequentially(urls, callback) {
    if (urls.length === 0) {
        callback();
        return;
    }

    const url = urls.shift();

    // 이미 로드된 스크립트인지 확인
    if (document.querySelector(`script[src="${url}"]`)) {
        loadScriptsSequentially(urls, callback);
        return;
    }

    const script = document.createElement('script');
    script.src = url;
    script.onload = () => loadScriptsSequentially(urls, callback);
    script.onerror = () => {
        console.error('Failed to load script:', url);
        loadScriptsSequentially(urls, callback);
    };
    document.body.appendChild(script);
}

// ========== Form Initialization ==========

function initForms() {
    const statusForm = document.getElementById('statusForm');
    if (statusForm) {
        statusForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('statusMemberId').value;
            const status = document.querySelector('input[name="status"]:checked')?.value;
            if (!status) { showToast('상태를 선택해주세요.', 'error'); return; }

            try {
                const response = await fetch(`/admin/member/update-status/${id}?status=${status}`, { method: 'POST' });
                const result = await response.json();
                showToast(result.message, result.success ? 'success' : 'error');
                if (result.success) { closeModal('statusModal'); loadTabContent('member'); }
            } catch (error) { showToast('오류가 발생했습니다.', 'error'); }
        });
    }

    const roleForm = document.getElementById('roleForm');
    if (roleForm) {
        roleForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const id = document.getElementById('roleMemberId').value;
            const role = document.querySelector('input[name="role"]:checked')?.value;
            if (!role) { showToast('권한을 선택해주세요.', 'error'); return; }

            try {
                const response = await fetch(`/admin/member/update-role/${id}?role=${role}`, { method: 'POST' });
                const result = await response.json();
                showToast(result.message, result.success ? 'success' : 'error');
                if (result.success) { closeModal('roleModal'); loadTabContent('member'); }
            } catch (error) { showToast('오류가 발생했습니다.', 'error'); }
        });
    }
}

// ========== Row & Pagination Functions ==========

function goToPage(page) {
    const form = document.querySelector('.tab-content .search-form, .tab-content .filter-form');
    const params = new URLSearchParams();
    if (form) new FormData(form).forEach((v, k) => { if (v) params.set(k, v); });
    params.set('page', page);
    loadTabContent(currentTab, params.toString());
}

function sortBy(column) {
    const params = new URLSearchParams();
    const form = document.querySelector('.tab-content .search-form');
    if (form) new FormData(form).forEach((v, k) => { if (v) params.set(k, v); });

    const currentSort = params.get('sort');
    const currentDir = params.get('direction') || 'desc';
    params.set('sort', column);
    params.set('direction', currentSort === column && currentDir === 'desc' ? 'asc' : 'desc');
    params.set('page', '0');
    loadTabContent(currentTab, params.toString());
}

// ========== Member Detail Functions ==========

async function viewDetail(id) {
    console.log('viewDetail called with id:', id);
    currentMemberId = id;
    try {
        const response = await fetch(`/admin/member/detail/${id}`);
        const data = await response.json();

        const content = document.getElementById('detailContent');
        content.innerHTML = `
            <div class="detail-grid">
                <div class="detail-item"><div class="detail-label">ID</div><div class="detail-value">${data.id}</div></div>
                <div class="detail-item"><div class="detail-label">이메일</div><div class="detail-value">${escapeHtml(data.email)}</div></div>
                <div class="detail-item"><div class="detail-label">닉네임</div><div class="detail-value">${escapeHtml(data.nickname)}</div></div>
                <div class="detail-item"><div class="detail-label">권한</div><div class="detail-value">${data.role === 'ADMIN' ? '관리자' : '사용자'}</div></div>
                <div class="detail-item"><div class="detail-label">상태</div><div class="detail-value">${data.status}</div></div>
                <div class="detail-item"><div class="detail-label">티어</div><div class="detail-value"><span class="tier-badge" style="background-color:${data.multiTierColor}">${escapeHtml(data.multiTierDisplayName)}</span> (LP: ${data.multiLp})</div></div>
                <div class="detail-item"><div class="detail-label">총 게임</div><div class="detail-value">${data.totalGames}회</div></div>
                <div class="detail-item"><div class="detail-label">총 점수</div><div class="detail-value">${data.totalScore}점</div></div>
                <div class="detail-item"><div class="detail-label">마지막 로그인</div><div class="detail-value">${data.lastLoginAt ? new Date(data.lastLoginAt).toLocaleString('ko-KR') : '-'}</div></div>
                <div class="detail-item"><div class="detail-label">가입일</div><div class="detail-value">${new Date(data.createdAt).toLocaleString('ko-KR')}</div></div>
            </div>
            ${data.badges && data.badges.length > 0 ? `
                <div class="badges-section">
                    <h4>보유 뱃지 (${data.badgeCount}개)</h4>
                    <div class="badges-list">${data.badges.map(b => `<span class="badge-item" style="border-color:${b.rarityColor}" title="${escapeHtml(b.description)}">${b.emoji} ${escapeHtml(b.name)}</span>`).join('')}</div>
                </div>
            ` : ''}
        `;
        openModal('detailModal');
    } catch (error) { showToast('상세 정보를 불러오는데 실패했습니다.', 'error'); }
}

// ========== Modal Functions ==========

function openStatusModal(id) {
    currentMemberId = id;
    document.getElementById('statusMemberId').value = id;
    openModal('statusModal');
}

function openRoleModal(id) {
    currentMemberId = id;
    document.getElementById('roleMemberId').value = id;
    openModal('roleModal');
}

function openModal(id) {
    document.getElementById(id).classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeModal(id) {
    document.getElementById(id).classList.remove('show');
    document.body.style.overflow = '';
}

// ========== Member Actions ==========

async function resetPassword() {
    if (!confirm('비밀번호를 초기화하시겠습니까?')) return;
    try {
        const response = await fetch(`/admin/member/reset-password/${currentMemberId}`, { method: 'POST' });
        const result = await response.json();
        showToast(result.message, result.success ? 'success' : 'error');
    } catch (error) { showToast('오류가 발생했습니다.', 'error'); }
}

async function kickSession() {
    if (!confirm('세션을 강제 종료하시겠습니까?')) return;
    try {
        const response = await fetch(`/admin/member/kick-session/${currentMemberId}`, { method: 'POST' });
        const result = await response.json();
        showToast(result.message, result.success ? 'success' : 'error');
    } catch (error) { showToast('오류가 발생했습니다.', 'error'); }
}
