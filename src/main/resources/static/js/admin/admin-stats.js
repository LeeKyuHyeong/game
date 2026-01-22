/**
 * admin/stats/index.html - 통합 통계 페이지
 */

document.addEventListener('DOMContentLoaded', function() {
    // 오답 통계 2차 탭 전환
    const wrongAnswersContent = document.getElementById('wrong-answers-content');
    if (wrongAnswersContent) {
        wrongAnswersContent.querySelectorAll('.stats-tabs .stats-tab').forEach(tab => {
            tab.addEventListener('click', function() {
                wrongAnswersContent.querySelectorAll('.stats-tabs .stats-tab').forEach(t => t.classList.remove('active'));
                this.classList.add('active');

                const tabName = this.dataset.tab;
                wrongAnswersContent.querySelectorAll('.stats-panel').forEach(panel => {
                    panel.style.display = 'none';
                });
                document.getElementById(tabName + 'Panel').style.display = 'block';
            });
        });
    }

    // 대중성 통계 2차 탭 전환
    const popularityContent = document.getElementById('popularity-content');
    if (popularityContent) {
        popularityContent.querySelectorAll('.stats-tabs .stats-tab').forEach(tab => {
            tab.addEventListener('click', function() {
                popularityContent.querySelectorAll('.stats-tabs .stats-tab').forEach(t => t.classList.remove('active'));
                this.classList.add('active');

                const tabName = this.dataset.tab;
                popularityContent.querySelectorAll('.stats-panel').forEach(panel => {
                    panel.style.display = 'none';
                });
                document.getElementById(tabName + 'Panel').style.display = 'block';
            });
        });
    }
});

/**
 * 1차 탭 전환 (오답 통계 / 대중성 통계)
 */
function switchPrimaryTab(tab) {
    // URL 파라미터 업데이트
    const url = new URL(window.location);
    url.searchParams.set('tab', tab);

    // 기본값이면 파라미터 제거
    if (tab === 'wrong-answers') {
        url.searchParams.delete('minPlays');
        url.searchParams.delete('filterJunk');
    }

    // 페이지 새로고침 없이 URL 업데이트
    window.history.pushState({}, '', url);

    // 탭 버튼 활성화
    document.querySelectorAll('.primary-tab').forEach(t => t.classList.remove('active'));
    document.querySelector(`.primary-tab[data-tab="${tab}"]`).classList.add('active');

    // 콘텐츠 표시/숨김
    document.querySelectorAll('.primary-content').forEach(content => {
        content.style.display = 'none';
    });
    document.getElementById(tab + '-content').style.display = 'block';
}

/**
 * 대중성 설정 업데이트
 */
function updatePopularity(songId, isPopular) {
    if (!confirm(isPopular ? '대중적으로 변경하시겠습니까?' : '매니악으로 변경하시겠습니까?')) {
        return;
    }

    fetch('/admin/stats/popularity/update/' + songId + '?isPopular=' + isPopular, {
        method: 'POST'
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('변경되었습니다.');
            location.reload();
        } else {
            alert('변경 실패: ' + data.message);
        }
    })
    .catch(error => {
        alert('오류가 발생했습니다.');
        console.error(error);
    });
}
