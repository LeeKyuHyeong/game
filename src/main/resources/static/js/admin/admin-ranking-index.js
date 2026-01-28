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

// ========== 회원 상세보기 ==========

async function viewMemberDetail(id) {
    if (!id) return;

    try {
        const response = await fetch('/admin/member/detail/' + id);
        if (!response.ok) throw new Error('Failed to load member detail');

        const data = await response.json();
        const content = document.getElementById('memberDetailContent');

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
        openModal('memberDetailModal');
    } catch (error) {
        console.error('Error loading member detail:', error);
        if (typeof showToast === 'function') {
            showToast('회원 정보를 불러오는데 실패했습니다.', 'error');
        } else {
            alert('회원 정보를 불러오는데 실패했습니다.');
        }
    }
}

// ========== 랭킹 리셋 함수 ==========

function confirmResetWeekly() {
    if (!confirm('주간 랭킹을 리셋하시겠습니까?\n\n현재 주간 랭킹이 스냅샷으로 저장된 후 초기화됩니다.')) return;
    executeRankingReset('weekly');
}

function confirmResetMonthly() {
    if (!confirm('월간 랭킹을 리셋하시겠습니까?\n\n현재 월간 랭킹이 스냅샷으로 저장된 후 초기화됩니다.')) return;
    executeRankingReset('monthly');
}

function executeRankingReset(type) {
    fetch('/admin/ranking/reset/' + type, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast(result.message, 'success');
                loadTabContent(currentTab);
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function() {
            showToast('랭킹 리셋 중 오류가 발생했습니다.', 'error');
        });
}

// ========== 과거 기간 조회 함수 ==========

function loadAvailablePeriods() {
    var select = document.getElementById('historyPeriodSelect');
    if (!select) return;

    fetch('/admin/ranking/history/periods')
        .then(function(response) { return response.json(); })
        .then(function(periods) {
            // 기존 옵션 유지 (첫 번째 "실시간" 옵션)
            while (select.options.length > 1) {
                select.remove(1);
            }
            periods.forEach(function(p) {
                var option = document.createElement('option');
                option.value = p.periodStart + '|' + p.periodEnd;
                option.textContent = p.label;
                select.appendChild(option);
            });
        })
        .catch(function() {
            console.error('Failed to load ranking history periods');
        });
}

function loadRankingHistoryPeriod(value) {
    if (!value) {
        // 실시간으로 돌아가기
        loadTabContent(currentTab);
        return;
    }

    var parts = value.split('|');
    var periodStart = parts[0];
    var periodEnd = parts[1];

    var container = document.getElementById('tabContent');
    container.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    fetch('/admin/ranking/history/content?periodStart=' + periodStart + '&periodEnd=' + periodEnd)
        .then(function(response) { return response.text(); })
        .then(function(html) {
            container.innerHTML = html;
        })
        .catch(function() {
            container.innerHTML = '<div class="error-message">스냅샷 데이터를 불러오는데 실패했습니다.</div>';
        });
}

// ========== 모달 함수 ==========

function openModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.add('show');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

// 모달 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('show');
        document.body.style.overflow = '';
    }
});
