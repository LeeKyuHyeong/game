/**
 * 배치 영향 곡 관리 JavaScript
 */

// 현재 필터 상태
let currentPage = 0;
let currentSize = 20;
let currentFilters = {
    batchId: '',
    isRestored: '',
    keyword: ''
};

// 초기화
document.addEventListener('DOMContentLoaded', function() {
    loadContent(0);
});

/**
 * 콘텐츠 로드 (AJAX)
 */
function loadContent(page = 0) {
    currentPage = parseInt(page, 10) || 0;

    const params = new URLSearchParams({
        page: page,
        size: currentSize,
        batchId: currentFilters.batchId,
        isRestored: currentFilters.isRestored,
        keyword: currentFilters.keyword
    });

    const wrapper = document.getElementById('contentWrapper');
    wrapper.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    fetch(`/admin/batch-affected/content?${params}`)
        .then(response => response.text())
        .then(html => {
            wrapper.innerHTML = html;
        })
        .catch(error => {
            console.error('로딩 실패:', error);
            wrapper.innerHTML = '<div class="error-message">데이터를 불러오는 중 오류가 발생했습니다.</div>';
        });
}

/**
 * 필터 적용
 */
function applyFilters() {
    currentFilters.batchId = document.getElementById('filterBatchId').value;
    currentFilters.isRestored = document.getElementById('filterRestored').value;
    currentFilters.keyword = document.getElementById('filterKeyword').value.trim();

    loadContent(0);
}

/**
 * 필터 초기화
 */
function clearFilters() {
    document.getElementById('filterBatchId').value = '';
    document.getElementById('filterRestored').value = '';
    document.getElementById('filterKeyword').value = '';

    currentFilters = {
        batchId: '',
        isRestored: '',
        keyword: ''
    };

    loadContent(0);
}

/**
 * 새로고침
 */
function refreshContent() {
    loadContent(currentPage);
    refreshStats();
}

/**
 * 통계 새로고침
 */
function refreshStats() {
    fetch('/admin/batch-affected/api/stats')
        .then(response => response.json())
        .then(stats => {
            // 통계 카드 업데이트
            const cards = document.querySelectorAll('.stat-card .stat-value');
            if (cards.length >= 5) {
                cards[0].textContent = stats.total || 0;
                cards[1].textContent = stats.unrestored || 0;
                cards[2].textContent = stats.restored || 0;
                cards[3].textContent = stats.youtubeUnrestored || 0;
                cards[4].textContent = stats.duplicateUnrestored || 0;
            }
        })
        .catch(error => {
            console.error('통계 로딩 실패:', error);
        });
}

/**
 * 전체 선택/해제
 */
function toggleSelectAll(checkbox) {
    const checkboxes = document.querySelectorAll('.row-checkbox:not(:disabled)');
    checkboxes.forEach(cb => {
        cb.checked = checkbox.checked;
    });
}

/**
 * 개별 곡 복구
 */
function restoreSong(id) {
    if (!confirm('이 곡을 복구하시겠습니까?')) {
        return;
    }

    fetch(`/admin/batch-affected/restore/${id}`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast(data.message, 'success');
            refreshContent();
        } else {
            showToast(data.message, 'error');
        }
    })
    .catch(error => {
        console.error('복구 실패:', error);
        showToast('복구 중 오류가 발생했습니다.', 'error');
    });
}

/**
 * 선택 복구
 */
function restoreSelected() {
    const checkboxes = document.querySelectorAll('.row-checkbox:checked');
    if (checkboxes.length === 0) {
        showToast('복구할 곡을 선택해주세요.', 'warning');
        return;
    }

    if (!confirm(`선택한 ${checkboxes.length}곡을 복구하시겠습니까?`)) {
        return;
    }

    const ids = Array.from(checkboxes).map(cb => parseInt(cb.value));

    fetch('/admin/batch-affected/restore-selected', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ ids: ids })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast(data.message, 'success');
            refreshContent();
        } else {
            showToast(data.message, 'error');
        }
    })
    .catch(error => {
        console.error('선택 복구 실패:', error);
        showToast('복구 중 오류가 발생했습니다.', 'error');
    });
}

/**
 * 배치 ID 기준 전체 복구
 */
function restoreAllByBatch(batchId) {
    const batchName = batchId === 'BATCH_YOUTUBE_VIDEO_CHECK' ? 'YouTube 검증' : '중복 검사';

    if (!confirm(`${batchName} 배치의 미복구 곡을 전부 복구하시겠습니까?`)) {
        return;
    }

    fetch(`/admin/batch-affected/restore-all/batch/${batchId}`, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast(data.message, 'success');
            refreshContent();
        } else {
            showToast(data.message, 'error');
        }
    })
    .catch(error => {
        console.error('전체 복구 실패:', error);
        showToast('복구 중 오류가 발생했습니다.', 'error');
    });
}

/**
 * 곡 상세 보기
 */
function viewSongDetail(songId) {
    // 간단한 상세 정보 표시
    const modal = document.getElementById('songDetailModal');
    const title = document.getElementById('songDetailTitle');
    const content = document.getElementById('songDetailContent');

    title.textContent = '곡 상세 정보';
    content.innerHTML = `
        <div class="loading-spinner">
            <div class="spinner"></div>
            <span>로딩 중...</span>
        </div>
    `;

    modal.classList.add('show');

    // 실제 데이터 로드 (곡 관리 페이지 링크로 대체)
    content.innerHTML = `
        <div class="song-detail-info">
            <p>곡 ID: <strong>${songId}</strong></p>
            <p>
                <a href="/admin/song?keyword=${songId}" class="btn btn-primary" target="_blank">
                    노래 관리에서 보기
                </a>
            </p>
        </div>
    `;
}

/**
 * 모달 닫기
 */
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
    }
}

/**
 * 토스트 메시지 표시
 */
function showToast(message, type = 'info') {
    // 기존 토스트 제거
    const existingToast = document.querySelector('.toast-notification');
    if (existingToast) {
        existingToast.remove();
    }

    const toast = document.createElement('div');
    toast.className = `toast-notification toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);

    // 애니메이션
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal.show').forEach(modal => {
            modal.classList.remove('show');
        });
    }
});
