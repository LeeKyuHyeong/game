/**
 * admin/report/list.html - 곡 신고 관리
 */

function filterByStatus(status) {
    if (status) {
        window.location.href = '/admin/report?status=' + status;
    } else {
        window.location.href = '/admin/report';
    }
}

function openProcessModal(btn) {
    const id = btn.dataset.id;
    const songTitle = btn.dataset.songTitle;
    const songArtist = btn.dataset.songArtist;
    const reportType = btn.dataset.reportType;
    const description = btn.dataset.description || '(없음)';
    const status = btn.dataset.status;

    document.getElementById('processReportId').value = id;
    document.getElementById('modalSongTitle').textContent = songTitle;
    document.getElementById('modalSongArtist').textContent = songArtist;
    document.getElementById('modalReportType').textContent = reportType;
    document.getElementById('modalDescription').textContent = description;
    document.getElementById('processStatus').value = status;
    document.getElementById('adminNote').value = '';

    document.getElementById('processModal').classList.add('show');
}

function closeProcessModal() {
    document.getElementById('processModal').classList.remove('show');
}

async function processReport() {
    const id = document.getElementById('processReportId').value;
    const status = document.getElementById('processStatus').value;
    const adminNote = document.getElementById('adminNote').value;

    try {
        const response = await fetch('/admin/report/process/' + id, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status, adminNote })
        });

        const result = await response.json();
        showToast(result.message, result.success ? 'success' : 'error');

        if (result.success) {
            setTimeout(() => window.location.reload(), 1000);
        }
    } catch (error) {
        showToast('처리 중 오류가 발생했습니다.', 'error');
    }
}

async function disableSong(reportId) {
    if (!confirm('해당 곡을 비활성화하시겠습니까?\n게임에서 더 이상 출제되지 않습니다.')) {
        return;
    }

    try {
        const response = await fetch('/admin/report/disable-song/' + reportId, {
            method: 'POST'
        });

        const result = await response.json();
        showToast(result.message, result.success ? 'success' : 'error');

        if (result.success) {
            setTimeout(() => window.location.reload(), 1000);
        }
    } catch (error) {
        showToast('비활성화 중 오류가 발생했습니다.', 'error');
    }
}

function toggleRowExpand(row) {
    row.classList.toggle('expanded');
}
