// admin-chat.js

document.addEventListener('DOMContentLoaded', function() {
    initCheckboxEvents();
});

// 체크박스 이벤트 초기화
function initCheckboxEvents() {
    const selectAll = document.getElementById('selectAll');
    const checkboxes = document.querySelectorAll('.chat-checkbox');
    const deleteBtn = document.getElementById('deleteSelectedBtn');
    const countSpan = document.getElementById('selectedCount');

    // 전체 선택
    selectAll.addEventListener('change', function() {
        checkboxes.forEach(cb => cb.checked = this.checked);
        updateSelectedCount();
    });

    // 개별 체크박스
    checkboxes.forEach(cb => {
        cb.addEventListener('change', function() {
            updateSelectedCount();
            selectAll.checked = Array.from(checkboxes).every(c => c.checked);
        });
    });

    // 선택 삭제 버튼
    deleteBtn.addEventListener('click', deleteSelected);

    function updateSelectedCount() {
        const count = document.querySelectorAll('.chat-checkbox:checked').length;
        countSpan.textContent = count;
        deleteBtn.disabled = count === 0;
    }
}

// 단일 채팅 삭제
function deleteChat(id) {
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return;

    fetch(`/admin/chat/delete/${id}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                location.reload();
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('삭제 중 오류가 발생했습니다.');
        });
}

// 선택 삭제
function deleteSelected() {
    const checkboxes = document.querySelectorAll('.chat-checkbox:checked');
    const ids = Array.from(checkboxes).map(cb => parseInt(cb.value));

    if (ids.length === 0) {
        alert('삭제할 채팅을 선택해주세요.');
        return;
    }

    if (!confirm(`${ids.length}개의 채팅을 삭제하시겠습니까?`)) return;

    fetch('/admin/chat/delete-selected', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ids)
    })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.success) {
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('삭제 중 오류가 발생했습니다.');
        });
}