/**
 * Admin Challenge Index Page - Fan & Genre Challenge Management
 */

let currentTab = 'fan';
let genres = [];

// ========== Initialization ==========

document.addEventListener('DOMContentLoaded', () => {
    loadTabContent(currentTab);
});

// ========== Tab Management ==========

function switchTab(tab) {
    if (currentTab === tab) return;

    currentTab = tab;

    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    window.history.pushState({}, '', url);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector(`.tab-btn:nth-child(${tab === 'fan' ? 1 : 2})`).classList.add('active');

    loadTabContent(tab);
}

async function loadTabContent(tab, params = '') {
    const tabContent = document.getElementById('tabContent');
    tabContent.innerHTML = `
        <div class="loading-spinner">
            <div class="spinner"></div>
            <span>로딩 중...</span>
        </div>
    `;

    try {
        const url = tab === 'fan'
            ? `/admin/fan-challenge/content${params ? '?' + params : ''}`
            : `/admin/genre-challenge/content${params ? '?' + params : ''}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load content');

        const html = await response.text();
        tabContent.innerHTML = html;

        initTabScripts();
    } catch (error) {
        tabContent.innerHTML = `
            <div class="error-message">
                <p>콘텐츠를 불러오는데 실패했습니다.</p>
                <button class="btn btn-primary" onclick="loadTabContent('${tab}')">다시 시도</button>
            </div>
        `;
    }
}

function initTabScripts() {
    const searchForm = document.querySelector('.tab-content .search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const formData = new FormData(searchForm);
            const params = new URLSearchParams(formData).toString();
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

// ========== Utility Functions ==========

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

function sortBy(column, baseUrl) {
    const params = new URLSearchParams(window.location.search);
    const currentSort = params.get('sort');
    const currentDirection = params.get('direction') || 'desc';

    if (currentSort === column) {
        params.set('direction', currentDirection === 'asc' ? 'desc' : 'asc');
    } else {
        params.set('sort', column);
        params.set('direction', 'desc');
    }
    params.set('page', '0');
    params.delete('tab');

    loadTabContent(currentTab, params.toString());
}

function goToPage(page) {
    const params = new URLSearchParams();
    const form = document.querySelector('.tab-content .search-form');
    if (form) {
        new FormData(form).forEach((value, key) => {
            if (value) params.set(key, value);
        });
    }
    params.set('page', page);
    loadTabContent(currentTab, params.toString());
}

// ========== Detail Functions ==========

async function viewDetail(id, type) {
    try {
        const url = type === 'fan'
            ? `/admin/fan-challenge/detail/${id}`
            : `/admin/genre-challenge/detail/${id}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('기록을 찾을 수 없습니다.');

        const data = await response.json();

        document.getElementById('detailModalTitle').textContent =
            type === 'fan' ? '팬 챌린지 기록 상세' : '장르 챌린지 기록 상세';

        renderDetailContent(data, type);
        openModal('detailModal');
    } catch (error) {
        showToast('기록을 불러오는데 실패했습니다.', 'error');
    }
}

function renderDetailContent(data, type) {
    const detailContent = document.getElementById('detailContent');
    detailContent.textContent = '';

    const grid = document.createElement('div');
    grid.className = 'detail-grid';

    function createDetailItem(label, value) {
        const item = document.createElement('div');
        item.className = 'detail-item';

        const labelDiv = document.createElement('div');
        labelDiv.className = 'detail-label';
        labelDiv.textContent = label;

        const valueDiv = document.createElement('div');
        valueDiv.className = 'detail-value';
        valueDiv.textContent = value;

        item.appendChild(labelDiv);
        item.appendChild(valueDiv);
        return item;
    }

    const bestTime = data.bestTimeMs ? `${(data.bestTimeMs / 1000).toFixed(1)}초` : '-';
    const achievedAt = data.achievedAt ? new Date(data.achievedAt).toLocaleString('ko-KR') : '-';

    grid.appendChild(createDetailItem('회원', data.memberNickname));
    grid.appendChild(createDetailItem('이메일', data.memberEmail));

    if (type === 'fan') {
        grid.appendChild(createDetailItem('아티스트', data.artist));
    } else {
        grid.appendChild(createDetailItem('장르', data.genreName));
    }

    grid.appendChild(createDetailItem('난이도', data.difficultyDisplayName));
    grid.appendChild(createDetailItem('달성 시점 정답/전체', `${data.correctCount}/${data.totalSongs} (${data.clearRate}%)`));
    grid.appendChild(createDetailItem('현재 시점 정답/전체', `${data.correctCount}/${data.currentSongCount} (${data.currentClearRate}%)`));

    if (type === 'fan') {
        const perfectItem = document.createElement('div');
        perfectItem.className = 'detail-item';

        const perfectLabel = document.createElement('div');
        perfectLabel.className = 'detail-label';
        perfectLabel.textContent = '퍼펙트 상태';

        const perfectValue = document.createElement('div');
        perfectValue.className = 'detail-value';

        if (data.isPerfectClear) {
            if (data.isCurrentPerfect) {
                perfectValue.innerHTML = '<span class="perfect-badge achieved">✅ 현재 유효</span>';
            } else {
                perfectValue.innerHTML = '<span class="perfect-badge invalid">⭐ 곡 추가로 무효화</span>';
            }
        } else {
            perfectValue.textContent = '미달성';
        }

        perfectItem.appendChild(perfectLabel);
        perfectItem.appendChild(perfectValue);
        grid.appendChild(perfectItem);
    } else {
        grid.appendChild(createDetailItem('최대 콤보', data.maxCombo));
    }

    grid.appendChild(createDetailItem('최고 클리어 시간', bestTime));
    grid.appendChild(createDetailItem('달성일', achievedAt));

    detailContent.appendChild(grid);
}

// ========== Modal Functions ==========

function openModal(modalId) {
    document.getElementById(modalId).classList.add('show');
    document.body.style.overflow = 'hidden';
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
    document.body.style.overflow = '';
}
