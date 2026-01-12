/**
 * 곡 신고 기능
 */

let reportSongId = null;

/**
 * 신고 모달 열기
 */
function openReportModal(songId) {
    reportSongId = songId;
    document.getElementById('reportDescription').value = '';
    document.querySelectorAll('.report-option').forEach(btn => btn.classList.remove('selected'));
    document.getElementById('reportModal').classList.add('show');
}

/**
 * 신고 모달 닫기
 */
function closeReportModal() {
    reportSongId = null;
    document.getElementById('reportModal').classList.remove('show');
}

/**
 * 신고 제출
 */
async function submitReport(reportType) {
    if (!reportSongId) {
        alert('신고할 곡을 선택해주세요.');
        return;
    }

    const description = document.getElementById('reportDescription').value.trim();

    // 기타 선택 시 설명 필수
    if (reportType === 'OTHER' && !description) {
        alert('기타 선택 시 상세 내용을 입력해주세요.');
        document.getElementById('reportDescription').focus();
        return;
    }

    try {
        const response = await fetch('/api/song-report', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                songId: reportSongId,
                reportType: reportType,
                description: description
            })
        });

        const result = await response.json();
        alert(result.message);

        if (result.success) {
            closeReportModal();
        }
    } catch (error) {
        console.error('신고 오류:', error);
        alert('신고 중 오류가 발생했습니다.');
    }
}

/**
 * 신고 옵션 선택 시 스타일 변경
 */
function selectReportOption(btn, reportType) {
    document.querySelectorAll('.report-option').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    submitReport(reportType);
}

// 모달 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    const modal = document.getElementById('reportModal');
    if (e.target === modal) {
        closeReportModal();
    }
});

// ESC 키로 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeReportModal();
    }
});
