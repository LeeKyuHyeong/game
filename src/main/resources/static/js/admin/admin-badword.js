// admin-badword.js

document.addEventListener('DOMContentLoaded', function() {
    // 폼 제출 이벤트
    document.getElementById('badWordForm').addEventListener('submit', function(e) {
        e.preventDefault();
        saveBadWord();
    });
});

// 모달 열기 (추가)
function openModal() {
    document.getElementById('modalTitle').textContent = '비속어 추가';
    document.getElementById('badWordForm').reset();
    document.getElementById('badWordId').value = '';
    document.getElementById('badWordModal').classList.add('show');
}

// 모달 닫기
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// 수정 모달 열기
function editBadWord(id) {
    fetch(`/admin/badword/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            document.getElementById('modalTitle').textContent = '비속어 수정';
            document.getElementById('badWordId').value = data.id;
            document.getElementById('word').value = data.word;
            document.getElementById('replacement').value = data.replacement || '';

            // 라디오 버튼 설정
            const activeRadios = document.querySelectorAll('input[name="isActive"]');
            activeRadios.forEach(radio => {
                radio.checked = (radio.value === String(data.isActive));
            });

            document.getElementById('badWordModal').classList.add('show');
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('정보를 불러오는데 실패했습니다.');
        });
}

// 저장
function saveBadWord() {
    const form = document.getElementById('badWordForm');
    const formData = new FormData(form);

    fetch('/admin/badword/save', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            showToast(data.message);
            if (data.success) {
                location.reload();
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('저장 중 오류가 발생했습니다.');
        });
}

// 삭제
function deleteBadWord(id) {
    if (!confirm('이 비속어를 삭제하시겠습니까?')) return;

    fetch(`/admin/badword/delete/${id}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            showToast(data.message);
            if (data.success) {
                location.reload();
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('삭제 중 오류가 발생했습니다.');
        });
}

// 상태 토글
function toggleStatus(id) {
    fetch(`/admin/badword/toggle/${id}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                location.reload();
            } else {
                showToast(data.message);
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('상태 변경 중 오류가 발생했습니다.');
        });
}

// 테스트 모달 열기
function openTestModal() {
    document.getElementById('testMessage').value = '';
    document.getElementById('testResult').style.display = 'none';
    document.getElementById('testModal').classList.add('show');
}

// 필터 테스트
function testFilter() {
    const message = document.getElementById('testMessage').value.trim();

    if (!message) {
        showToast('테스트할 메시지를 입력해주세요.');
        return;
    }

    const formData = new FormData();
    formData.append('message', message);

    fetch('/admin/badword/test', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                document.getElementById('originalText').textContent = data.original;
                document.getElementById('filteredText').textContent = data.filtered;
                document.getElementById('foundWords').textContent =
                    data.foundWords.length > 0 ? data.foundWords.join(', ') : '없음';
                document.getElementById('testResult').style.display = 'block';
            } else {
                showToast(data.message);
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('테스트 중 오류가 발생했습니다.');
        });
}

// 일괄 등록 모달 열기
function openBulkModal() {
    document.getElementById('bulkWords').value = '';
    document.getElementById('bulkModal').classList.add('show');
}

// 일괄 등록
function bulkAdd() {
    const words = document.getElementById('bulkWords').value.trim();

    if (!words) {
        showToast('비속어를 입력해주세요.');
        return;
    }

    const formData = new FormData();
    formData.append('words', words);

    fetch('/admin/badword/bulk-add', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            showToast(data.message);
            if (data.success) {
                location.reload();
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('일괄 등록 중 오류가 발생했습니다.');
        });
}

// 모달 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    if (e.target.classList.contains('modal')) {
        e.target.classList.remove('show');
    }
});

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal.show').forEach(modal => {
            modal.classList.remove('show');
        });
    }
});

// 행 펼치기/접기 (모바일)
function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

// 정렬
function sortBy(column) {
    const params = new URLSearchParams(window.location.search);
    const currentSort = params.get('sort');
    const currentDirection = params.get('direction') || 'desc';

    if (currentSort === column) {
        params.set('direction', currentDirection === 'asc' ? 'desc' : 'asc');
    } else {
        params.set('sort', column);
        params.set('direction', 'desc');
    }
    params.set('page', '0');
    window.location.href = '/admin/badword?' + params.toString();
}