/**
 * Admin Song Popularity Page - Vote Statistics Management
 */

let currentTab = 'songs';
let currentSort = '';
let currentDirection = 'desc';

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
    const tabIndex = tab === 'songs' ? 1 : tab === 'artists' ? 2 : 3;
    document.querySelector(`.tab-btn:nth-child(${tabIndex})`).classList.add('active');

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
        let url;
        if (tab === 'songs') {
            url = `/admin/song-popularity/songs/content${params ? '?' + params : ''}`;
        } else if (tab === 'artists') {
            url = `/admin/song-popularity/artists/content${params ? '?' + params : ''}`;
        } else {
            url = `/admin/song-popularity/votes/content${params ? '?' + params : ''}`;
        }

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
    // 검색 폼 이벤트
    const searchForm = document.querySelector('.tab-content .search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const formData = new FormData(searchForm);
            const params = new URLSearchParams(formData).toString();
            loadTabContent(currentTab, params);
        });
    }

    // 초기화 버튼
    const resetBtn = document.querySelector('.tab-content .btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', (e) => {
            e.preventDefault();
            loadTabContent(currentTab);
        });
    }

    // 정렬 헤더 클릭 이벤트
    document.querySelectorAll('.sortable').forEach(th => {
        th.addEventListener('click', () => {
            const sortField = th.dataset.sort;
            if (sortField) {
                sortBy(sortField);
            }
        });
    });
}

// ========== Utility Functions ==========

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

function sortBy(column) {
    const params = new URLSearchParams();
    const form = document.querySelector('.tab-content .search-form');

    if (form) {
        new FormData(form).forEach((value, key) => {
            if (value) params.set(key, value);
        });
    }

    // 현재 정렬 상태 확인
    const sortedTh = document.querySelector(`.sortable[data-sort="${column}"]`);
    const isCurrentlySorted = sortedTh && sortedTh.classList.contains('sorted-desc');

    if (currentSort === column) {
        currentDirection = currentDirection === 'asc' ? 'desc' : 'asc';
    } else {
        currentSort = column;
        currentDirection = 'desc';
    }

    params.set('sort', column);
    params.set('direction', currentDirection);
    params.set('page', '0');

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

function resetSearch() {
    loadTabContent(currentTab);
}

// ========== Song Detail Functions ==========

async function viewSongDetail(button) {
    const songId = button.dataset.songId;
    const title = button.dataset.title;
    const artist = button.dataset.artist;

    document.getElementById('songDetailTitle').textContent = `${title} - ${artist}`;

    const detailContent = document.getElementById('songDetailContent');
    detailContent.innerHTML = `
        <div class="loading-spinner">
            <div class="spinner"></div>
            <span>로딩 중...</span>
        </div>
    `;

    openModal('songDetailModal');

    try {
        const response = await fetch(`/admin/song-popularity/song/${songId}/votes`);
        if (!response.ok) throw new Error('데이터를 불러올 수 없습니다.');

        const data = await response.json();
        renderSongVotes(data, detailContent);
    } catch (error) {
        detailContent.innerHTML = `
            <div class="error-message">
                <p>${error.message}</p>
            </div>
        `;
    }
}

function renderSongVotes(data, container) {
    if (data.votes.length === 0) {
        container.innerHTML = '<p class="empty-message">투표 데이터가 없습니다.</p>';
        return;
    }

    const table = document.createElement('table');
    table.className = 'data-table vote-detail-table';

    const thead = document.createElement('thead');
    thead.innerHTML = `
        <tr>
            <th>회원</th>
            <th>평점</th>
            <th>평가</th>
            <th>투표일</th>
        </tr>
    `;
    table.appendChild(thead);

    const tbody = document.createElement('tbody');
    data.votes.forEach(vote => {
        const tr = document.createElement('tr');

        const ratingStars = '★'.repeat(vote.rating) + '☆'.repeat(5 - vote.rating);
        const createdAt = new Date(vote.createdAt).toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });

        tr.innerHTML = `
            <td>${escapeHtml(vote.memberNickname)}</td>
            <td class="rating-stars">${ratingStars}</td>
            <td><span class="rating-label rating-${vote.rating}">${vote.ratingLabel}</span></td>
            <td>${createdAt}</td>
        `;
        tbody.appendChild(tr);
    });
    table.appendChild(tbody);

    container.textContent = '';
    container.appendChild(table);

    // 페이지 정보
    if (data.totalPages > 1) {
        const pageInfo = document.createElement('p');
        pageInfo.className = 'page-info';
        pageInfo.textContent = `총 ${data.totalElements}건 중 ${data.votes.length}건 표시`;
        container.appendChild(pageInfo);
    }
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

// ESC 키로 모달 닫기
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const openModal = document.querySelector('.modal.show');
        if (openModal) {
            closeModal(openModal.id);
        }
    }
});

// 모달 바깥 클릭 시 닫기
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        closeModal(e.target.id);
    }
});

// ========== Helper Functions ==========

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, type = 'info') {
    if (typeof window.showToast === 'function') {
        window.showToast(message, type);
    } else {
        alert(message);
    }
}
