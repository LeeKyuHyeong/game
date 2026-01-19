async function deleteSession(id) {
    if (!confirm('정말 삭제하시겠습니까?\n해당 게임의 모든 라운드 정보도 함께 삭제됩니다.')) return;

    try {
        const response = await fetch(`/admin/history/delete/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast(result.message);
            location.reload();
        } else {
            showToast(result.message);
        }
    } catch (error) {
        showToast('삭제 중 오류가 발생했습니다.');
    }
}