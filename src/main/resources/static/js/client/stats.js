/**
 * client/stats.html - 통계 페이지
 */

document.addEventListener('DOMContentLoaded', function() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabId = this.dataset.tab;

            // 탭 버튼 활성화
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // 탭 콘텐츠 활성화
            tabContents.forEach(content => {
                content.classList.remove('active');
                if (content.id === 'tab-' + tabId) {
                    content.classList.add('active');
                }
            });
        });
    });
});
