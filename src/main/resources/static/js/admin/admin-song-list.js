/**
 * Admin Song List Page - Form handling, sorting, pagination, artist filter, YouTube preview
 */

let ytPlayer = null;
let ytApiReady = false;

// ========== Row & Pagination Functions ==========

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

function sortBy(column) {
    const form = document.getElementById('filterForm');
    const sortInput = form.querySelector('input[name="sort"]');
    const directionInput = form.querySelector('input[name="direction"]');
    const pageInput = document.getElementById('pageInput');

    if (currentSort === column) {
        directionInput.value = currentDirection === 'asc' ? 'desc' : 'asc';
    } else {
        sortInput.value = column;
        directionInput.value = 'desc';
    }
    pageInput.value = '0';
    form.submit();
}

function goToPage(page) {
    const pageInput = document.getElementById('pageInput');
    pageInput.value = page;
    document.getElementById('filterForm').submit();
}

function setViewMode(mode) {
    const form = document.getElementById('filterForm');
    form.querySelector('input[name="viewMode"]').value = mode;
    document.getElementById('pageInput').value = '0';
    form.submit();
}

// ========== Artist Filter Functions ==========

function updateSelectedArtists() {
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    const tagsContainer = document.getElementById('selectedArtistsTags');
    const countSpan = document.getElementById('selectedCount');

    let selectedArtists = [];
    checkboxes.forEach(cb => {
        if (cb.checked) {
            const item = cb.closest('.artist-checkbox-item');
            selectedArtists.push({
                name: item.dataset.name,
                count: item.dataset.count
            });
        }
    });

    countSpan.textContent = selectedArtists.length;
    updateSelectAllCheckboxState();

    tagsContainer.replaceChildren();

    if (selectedArtists.length === 0) {
        const noSelectionSpan = document.createElement('span');
        noSelectionSpan.className = 'no-selection-text';
        noSelectionSpan.textContent = '선택된 아티스트가 없습니다';
        tagsContainer.appendChild(noSelectionSpan);
    } else {
        selectedArtists.forEach(a => {
            const tag = document.createElement('span');
            tag.className = 'selected-artist-tag';

            const nameSpan = document.createElement('span');
            nameSpan.className = 'tag-name';
            nameSpan.textContent = a.name;

            const countSpanEl = document.createElement('span');
            countSpanEl.className = 'tag-count';
            countSpanEl.textContent = `(${a.count})`;

            const removeBtn = document.createElement('button');
            removeBtn.type = 'button';
            removeBtn.className = 'tag-remove';
            removeBtn.textContent = '×';
            removeBtn.onclick = () => removeArtist(a.name);

            tag.appendChild(nameSpan);
            tag.appendChild(countSpanEl);
            tag.appendChild(removeBtn);
            tagsContainer.appendChild(tag);
        });
    }
}

function removeArtist(name) {
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    checkboxes.forEach(cb => {
        if (cb.value === name) {
            cb.checked = false;
        }
    });
    updateSelectedArtists();
}

function handleFormSubmit() {
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    const allCheckboxes = Array.from(checkboxes);
    const checkedCount = allCheckboxes.filter(cb => cb.checked).length;

    if (checkedCount === allCheckboxes.length && allCheckboxes.length > 0) {
        allCheckboxes.forEach(cb => {
            cb.checked = false;
        });
    }
}

function toggleSelectAllArtists() {
    const selectAllCheckbox = document.getElementById('selectAllArtists');
    const checkboxes = document.querySelectorAll('#artistCheckboxGrid input[type="checkbox"]');
    const visibleCheckboxes = Array.from(checkboxes).filter(cb => !cb.closest('.artist-checkbox-item').classList.contains('hidden'));

    visibleCheckboxes.forEach(cb => {
        cb.checked = selectAllCheckbox.checked;
    });
    updateSelectedArtists();
}

function updateSelectAllCheckboxState() {
    const selectAllCheckbox = document.getElementById('selectAllArtists');
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
        if (keyword === '' || name.includes(keyword)) {
            item.classList.remove('hidden');
        } else {
            item.classList.add('hidden');
        }
    });
    updateSelectAllCheckboxState();
}

function sortArtistList() {
    const sortOrder = document.getElementById('artistSortOrder').value;
    const grid = document.getElementById('artistCheckboxGrid');
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
        if (ytPlayer) {
            ytPlayer.destroy();
        }
        ytPlayer = new YT.Player('youtubePlayer', {
            height: '360',
            width: '640',
            videoId: videoId,
            playerVars: {
                'start': startTime,
                'autoplay': 1,
                'controls': 1,
                'rel': 0
            },
            events: {
                'onReady': function(event) {
                    document.getElementById('previewStatus').textContent = '재생 중';
                    document.getElementById('previewStatus').style.color = '#10b981';
                },
                'onError': function(event) {
                    let errorMsg = '알 수 없는 오류';
                    switch(event.data) {
                        case 2: errorMsg = '잘못된 Video ID'; break;
                        case 5: errorMsg = 'HTML5 플레이어 오류'; break;
                        case 100: errorMsg = '영상을 찾을 수 없음 (삭제됨)'; break;
                        case 101:
                        case 150: errorMsg = '소유자가 임베드를 허용하지 않음'; break;
                    }
                    document.getElementById('previewStatus').textContent = '오류: ' + errorMsg;
                    document.getElementById('previewStatus').style.color = '#ef4444';
                },
                'onStateChange': function(event) {
                    if (event.data === YT.PlayerState.PLAYING) {
                        document.getElementById('previewStatus').textContent = '재생 중';
                        document.getElementById('previewStatus').style.color = '#10b981';
                    } else if (event.data === YT.PlayerState.PAUSED) {
                        document.getElementById('previewStatus').textContent = '일시정지';
                        document.getElementById('previewStatus').style.color = '#f59e0b';
                    } else if (event.data === YT.PlayerState.ENDED) {
                        document.getElementById('previewStatus').textContent = '재생 완료';
                        document.getElementById('previewStatus').style.color = '#6b7280';
                    }
                }
            }
        });
    } else {
        document.getElementById('previewStatus').textContent = 'YouTube API 로딩 중... 잠시 후 다시 시도해주세요.';
        document.getElementById('previewStatus').style.color = '#f59e0b';
    }
}

function closeYoutubePreview() {
    if (ytPlayer) {
        ytPlayer.destroy();
        ytPlayer = null;
    }
    document.getElementById('youtubePlayer').replaceChildren();
    document.getElementById('youtubePreviewModal').classList.remove('show');
}

// ========== Initialization ==========

document.addEventListener('DOMContentLoaded', function() {
    updateSelectedArtists();
    sortArtistList();
    loadYouTubeAPI();
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        const youtubeModal = document.getElementById('youtubePreviewModal');
        if (youtubeModal && youtubeModal.classList.contains('show')) {
            closeYoutubePreview();
        }
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const youtubeModal = document.getElementById('youtubePreviewModal');
    if (youtubeModal) {
        youtubeModal.addEventListener('click', function(e) {
            if (e.target === this) {
                closeYoutubePreview();
            }
        });
    }
});
