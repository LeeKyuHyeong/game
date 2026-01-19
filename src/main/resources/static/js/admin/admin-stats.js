/**
 * admin/stats/wrong-answers.html - 오답 통계
 */

document.addEventListener('DOMContentLoaded', function() {
    // 탭 전환
    document.querySelectorAll('.stats-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('.stats-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            const tabName = this.dataset.tab;
            document.querySelectorAll('.stats-panel').forEach(panel => {
                panel.style.display = 'none';
            });
            document.getElementById(tabName + 'Panel').style.display = 'block';
        });
    });
});
