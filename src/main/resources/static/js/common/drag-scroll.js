/**
 * PC Drag Scroll Utility
 *
 * PC에서 마우스 드래그로 가로 스크롤 가능하게 하는 유틸리티
 * 브라우저는 기본적으로 터치 스와이프만 지원하므로, PC에서는 JavaScript가 필요
 *
 * @example
 * const element = document.querySelector('.horizontal-scroll');
 * enableDragScroll(element);
 */

/**
 * 요소에 PC 드래그 스크롤 기능을 활성화합니다.
 * @param {HTMLElement} element - 드래그 스크롤을 적용할 요소
 */
function enableDragScroll(element) {
    if (!element) return;

    let isDown = false;
    let startX;
    let scrollLeft;

    // 마우스 버튼을 누를 때
    element.addEventListener('mousedown', (e) => {
        // 링크나 버튼 클릭은 무시
        if (e.target.tagName === 'A' || e.target.tagName === 'BUTTON') return;

        isDown = true;
        element.classList.add('dragging');
        startX = e.pageX - element.offsetLeft;
        scrollLeft = element.scrollLeft;
    });

    // 마우스가 요소 밖으로 나갈 때
    element.addEventListener('mouseleave', () => {
        isDown = false;
        element.classList.remove('dragging');
    });

    // 마우스 버튼을 놓을 때
    element.addEventListener('mouseup', () => {
        isDown = false;
        element.classList.remove('dragging');
    });

    // 마우스를 움직일 때
    element.addEventListener('mousemove', (e) => {
        if (!isDown) return;
        e.preventDefault();
        const x = e.pageX - element.offsetLeft;
        const walk = (x - startX) * 2; // 스크롤 속도 배율
        element.scrollLeft = scrollLeft - walk;
    });
}
