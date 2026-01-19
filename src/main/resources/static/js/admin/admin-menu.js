/**
 * admin/menu/list.html - 메뉴 관리
 */

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

async function toggleMenu(menuId) {
    try {
        const response = await fetch(`/admin/menu/toggle/${menuId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            showToast(result.message);
            // 페이지 새로고침으로 상태 반영
            location.reload();
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        // console.error('메뉴 상태 변경 오류:', error);
        showToast('메뉴 상태 변경 중 오류가 발생했습니다.', 'error');
    }
}
