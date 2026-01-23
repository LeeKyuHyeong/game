/**
 * Admin Song Index Page - Tab Management & YouTube Preview
 */

var currentTab = currentTab || 'song';
var ytPlayer = ytPlayer || null;
var ytApiReady = ytApiReady || false;

// ========== Initialization ==========

document.addEventListener('DOMContentLoaded', () => {
    // admin/content 페이지에서는 admin-content-index.js가 초기화를 담당
    if (!window.location.pathname.includes('/admin/content')) {
        loadTabContent(currentTab);
    }
    loadYouTubeAPI();
});

// ========== Tab Management ==========

function switchTab(tab) {
    if (currentTab === tab) return;
    currentTab = tab;

    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    window.history.pushState({}, '', url);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector(`.tab-btn:nth-child(${tab === 'song' ? 1 : 2})`).classList.add('active');

    loadTabContent(tab);
}

async function loadTabContent(tab, params = '') {
    const tabContent = document.getElementById('tabContent');
    tabContent.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    try {
        const url = tab === 'song'
            ? `/admin/song/content${params ? '?' + params : ''}`
            : `/admin/report/content${params ? '?' + params : ''}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load content');

        tabContent.innerHTML = await response.text();
        initTabScripts(tab);
    } catch (error) {
        tabContent.innerHTML = `<div class="error-message"><p>콘텐츠를 불러오는데 실패했습니다.</p><button class="btn btn-primary" onclick="loadTabContent('${tab}')">다시 시도</button></div>`;
    }
}

function initTabScripts(tab) {
    if (tab === 'song') {
        initSongTabScripts();
    } else {
        initReportTabScripts();
    }
}

// ========== Song Tab Functions ==========

function initSongTabScripts() {
    const filterForm = document.getElementById('filterForm');
    if (filterForm) {
        filterForm.addEventListener('submit', (e) => {
            e.preventDefault();
            handleFormSubmit();
            const params = new URLSearchParams(new FormData(filterForm)).toString();
            loadTabContent('song', params);
        });
    }

    const resetBtn = document.querySelector('.tab-content .btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', (e) => {
            e.preventDefault();
            loadTabContent('song');
        });
    }

    updateSelectedArtists();
    sortArtistList();
}

function refreshSongList() {
    const form = document.getElementById('filterForm');
    if (form) {
        const params = new URLSearchParams(new FormData(form)).toString();
        loadTabContent('song', params);
    } else {
        loadTabContent('song');
    }
}

// ========== Report Tab Functions ==========

function initReportTabScripts() {
    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) {
        statusFilter.onchange = function() {
            const params = this.value ? `status=${this.value}` : '';
            loadTabContent('report', params);
        };
    }

    const resetBtn = document.querySelector('.tab-content .btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', (e) => {
            e.preventDefault();
            loadTabContent('report');
        });
    }

    updatePendingBadge();
}

function updatePendingBadge() {
    const pendingCount = document.querySelector('.pending-badge');
    const badge = document.getElementById('pendingBadge');
    if (pendingCount && badge) {
        const count = pendingCount.textContent.match(/\d+/);
        if (count && parseInt(count[0]) > 0) {
            badge.textContent = count[0];
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    }
}

// ========== Row & Pagination Functions ==========

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) row.classList.toggle('expanded');
}

function goToPage(page) {
    const form = document.getElementById('filterForm');
    const params = new URLSearchParams();
    if (form) {
        new FormData(form).forEach((v, k) => { if (v) params.set(k, v); });
    }
    params.set('page', page);
    loadTabContent(currentTab, params.toString());
}

function goToReportPage(page) {
    const statusFilter = document.getElementById('statusFilter');
    const params = new URLSearchParams();
    if (statusFilter && statusFilter.value) params.set('status', statusFilter.value);
    params.set('page', page);
    loadTabContent('report', params.toString());
}

// ========== Sort & View Mode ==========

function sortBy(column) {
    const form = document.getElementById('filterForm');
    const sortInput = form.querySelector('input[name="sort"]');
    const directionInput = form.querySelector('input[name="direction"]');
    const currentSort = sortInput.value;
    const currentDirection = directionInput.value;

    if (currentSort === column) {
        directionInput.value = currentDirection === 'asc' ? 'desc' : 'asc';
    } else {
        sortInput.value = column;
        directionInput.value = 'desc';
    }
    form.querySelector('input[name="page"]').value = '0';

    const params = new URLSearchParams(new FormData(form)).toString();
    loadTabContent('song', params);
}

function setViewMode(mode) {
    const form = document.getElementById('filterForm');
    form.querySelector('input[name="viewMode"]').value = mode;
    form.querySelector('input[name="page"]').value = '0';

    const params = new URLSearchParams(new FormData(form)).toString();
    loadTabContent('song', params);
}

// ========== Artist Filter Functions ==========

function updateSelectedArtists() {
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    if (!checkboxes.length) return;

    const tagsContainer = document.getElementById('selectedArtistsTags');
    const countSpan = document.getElementById('selectedCount');

    let selectedArtists = [];
    checkboxes.forEach(cb => {
        if (cb.checked) {
            const item = cb.closest('.artist-checkbox-item');
            selectedArtists.push({ name: item.dataset.name, count: item.dataset.count });
        }
    });

    if (countSpan) countSpan.textContent = selectedArtists.length;
    updateSelectAllCheckboxState();

    if (tagsContainer) {
        tagsContainer.replaceChildren();
        if (selectedArtists.length === 0) {
            const span = document.createElement('span');
            span.className = 'no-selection-text';
            span.textContent = '선택된 아티스트가 없습니다';
            tagsContainer.appendChild(span);
        } else {
            selectedArtists.forEach(a => {
                const tag = document.createElement('span');
                tag.className = 'selected-artist-tag';
                tag.innerHTML = `<span class="tag-name">${escapeHtml(a.name)}</span><span class="tag-count">(${a.count})</span>`;
                const removeBtn = document.createElement('button');
                removeBtn.type = 'button';
                removeBtn.className = 'tag-remove';
                removeBtn.textContent = '×';
                removeBtn.onclick = () => removeArtist(a.name);
                tag.appendChild(removeBtn);
                tagsContainer.appendChild(tag);
            });
        }
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function removeArtist(name) {
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    checkboxes.forEach(cb => { if (cb.value === name) cb.checked = false; });
    updateSelectedArtists();
}

function handleFormSubmit() {
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    const allCheckboxes = Array.from(checkboxes);
    const checkedCount = allCheckboxes.filter(cb => cb.checked).length;
    if (checkedCount === allCheckboxes.length && allCheckboxes.length > 0) {
        allCheckboxes.forEach(cb => { cb.checked = false; });
    }
}

function toggleSelectAllArtists() {
    const selectAllCheckbox = document.getElementById('selectAllArtists');
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    const visibleCheckboxes = Array.from(checkboxes).filter(cb => !cb.closest('.artist-checkbox-item').classList.contains('hidden'));
    visibleCheckboxes.forEach(cb => { cb.checked = selectAllCheckbox.checked; });
    updateSelectedArtists();
}

function updateSelectAllCheckboxState() {
    const selectAllCheckbox = document.getElementById('selectAllArtists');
    if (!selectAllCheckbox) return;
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    const visibleCheckboxes = Array.from(checkboxes).filter(cb => !cb.closest('.artist-checkbox-item').classList.contains('hidden'));
    if (visibleCheckboxes.length === 0) {
        selectAllCheckbox.checked = false;
        selectAllCheckbox.indeterminate = false;
    } else {
        const checkedCount = visibleCheckboxes.filter(cb => cb.checked).length;
        selectAllCheckbox.checked = checkedCount === visibleCheckboxes.length;
        selectAllCheckbox.indeterminate = checkedCount > 0 && checkedCount < visibleCheckboxes.length;
    }
}

function filterArtistList() {
    const searchInput = document.getElementById('artistSearchInput');
    const keyword = searchInput.value.toLowerCase().trim();
    const items = document.querySelectorAll('#artistCheckboxGrid .artist-checkbox-item');
    items.forEach(item => {
        const name = item.dataset.name.toLowerCase();
        item.classList.toggle('hidden', keyword !== '' && !name.includes(keyword));
    });
    updateSelectAllCheckboxState();
}

function sortArtistList() {
    const sortSelect = document.getElementById('artistSortOrder');
    const grid = document.getElementById('artistCheckboxGrid');
    if (!sortSelect || !grid) return;

    const sortOrder = sortSelect.value;
    const items = Array.from(grid.querySelectorAll('.artist-checkbox-item'));
    items.sort((a, b) => {
        if (sortOrder === 'count') {
            return parseInt(b.dataset.count) - parseInt(a.dataset.count);
        } else {
            return a.dataset.name.localeCompare(b.dataset.name, 'ko');
        }
    });
    items.forEach(item => grid.appendChild(item));
}

function toggleShowMore() {
    const grid = document.getElementById('artistCheckboxGrid');
    const btn = document.getElementById('showMoreBtn');
    if (grid.classList.contains('expanded')) {
        grid.classList.remove('expanded');
        btn.textContent = '더보기 ▼';
    } else {
        grid.classList.add('expanded');
        btn.textContent = '접기 ▲';
    }
}

// ========== YouTube Preview ==========

function loadYouTubeAPI() {
    const tag = document.createElement('script');
    tag.src = "https://www.youtube.com/iframe_api";
    const firstScriptTag = document.getElementsByTagName('script')[0];
    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
}

function onYouTubeIframeAPIReady() {
    ytApiReady = true;
}

function openYoutubePreview(btn) {
    const videoId = btn.dataset.videoId;
    const startTime = parseInt(btn.dataset.startTime) || 0;
    const duration = parseInt(btn.dataset.duration) || 10;
    const title = btn.dataset.title;
    const artist = btn.dataset.artist;

    document.getElementById('youtubePreviewTitle').textContent = 'YouTube 미리보기';
    document.getElementById('previewSongInfo').textContent = `${title} - ${artist}`;
    document.getElementById('previewTimeInfo').textContent = `시작: ${startTime}초 / 재생: ${duration}초`;
    document.getElementById('previewStatus').textContent = '로딩 중...';

    document.getElementById('youtubePreviewModal').classList.add('show');

    if (ytApiReady) {
        if (ytPlayer) ytPlayer.destroy();
        ytPlayer = new YT.Player('youtubePlayer', {
            height: '360', width: '640', videoId: videoId,
            playerVars: { 'start': startTime, 'autoplay': 1, 'controls': 1, 'rel': 0 },
            events: {
                'onReady': () => {
                    document.getElementById('previewStatus').textContent = '재생 중';
                    document.getElementById('previewStatus').style.color = '#10b981';
                },
                'onError': (event) => {
                    let errorMsg = '알 수 없는 오류';
                    switch(event.data) {
                        case 2: errorMsg = '잘못된 Video ID'; break;
                        case 5: errorMsg = 'HTML5 플레이어 오류'; break;
                        case 100: errorMsg = '영상을 찾을 수 없음 (삭제됨)'; break;
                        case 101: case 150: errorMsg = '소유자가 임베드를 허용하지 않음'; break;
                    }
                    document.getElementById('previewStatus').textContent = '오류: ' + errorMsg;
                    document.getElementById('previewStatus').style.color = '#ef4444';
                },
                'onStateChange': (event) => {
                    const statusEl = document.getElementById('previewStatus');
                    if (event.data === YT.PlayerState.PLAYING) { statusEl.textContent = '재생 중'; statusEl.style.color = '#10b981'; }
                    else if (event.data === YT.PlayerState.PAUSED) { statusEl.textContent = '일시정지'; statusEl.style.color = '#f59e0b'; }
                    else if (event.data === YT.PlayerState.ENDED) { statusEl.textContent = '재생 완료'; statusEl.style.color = '#6b7280'; }
                }
            }
        });
    } else {
        document.getElementById('previewStatus').textContent = 'YouTube API 로딩 중... 잠시 후 다시 시도해주세요.';
        document.getElementById('previewStatus').style.color = '#f59e0b';
    }
}

function closeYoutubePreview() {
    if (ytPlayer) { ytPlayer.destroy(); ytPlayer = null; }
    document.getElementById('youtubePlayer').replaceChildren();
    document.getElementById('youtubePreviewModal').classList.remove('show');
}

// ESC key and modal click handlers
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const youtubeModal = document.getElementById('youtubePreviewModal');
        if (youtubeModal && youtubeModal.classList.contains('show')) closeYoutubePreview();
    }
});

document.addEventListener('DOMContentLoaded', () => {
    const youtubeModal = document.getElementById('youtubePreviewModal');
    if (youtubeModal) {
        youtubeModal.addEventListener('click', function(e) {
            if (e.target === this) closeYoutubePreview();
        });
    }
});
