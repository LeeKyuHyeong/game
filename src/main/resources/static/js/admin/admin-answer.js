// admin-answer.js

let currentSongId = null;

// 정답 관리 모달 열기
function openAnswerModal(btn) {
    const songId = btn.dataset.songId;
    const title = btn.dataset.songTitle;
    const artist = btn.dataset.songArtist;
    
    currentSongId = songId;
    document.getElementById('currentSongId').value = songId;
    document.getElementById('answerModalTitle').textContent = '정답 관리 - ' + title;
    document.getElementById('modalSongTitle').textContent = title;
    document.getElementById('modalSongArtist').textContent = artist;
    
    // 정답 목록 로드
    loadAnswers(songId);
    
    // 입력 필드 초기화
    document.getElementById('newAnswer').value = '';
    document.getElementById('newAnswerPrimary').checked = false;
    
    document.getElementById('answerModal').classList.add('show');
}

// 모달 닫기
function closeAnswerModal() {
    document.getElementById('answerModal').classList.remove('show');
    currentSongId = null;
}

// 정답 목록 로드
async function loadAnswers(songId) {
    const listContainer = document.getElementById('answerList');
    listContainer.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch(`/admin/answer/song/${songId}`);
        const data = await response.json();
        
        if (!data.success) {
            listContainer.innerHTML = `<div class="error">${data.message}</div>`;
            return;
        }
        
        if (data.answers && data.answers.length > 0) {
            listContainer.innerHTML = data.answers.map(answer => `
                <div class="answer-item ${answer.isPrimary ? 'primary' : ''}">
                    <span class="answer-text">${escapeHtml(answer.answer)}</span>
                    ${answer.isPrimary ? '<span class="primary-badge">대표</span>' : ''}
                    <div class="answer-actions">
                        <button class="btn btn-sm btn-edit" onclick="openEditModal(${answer.id}, '${escapeJs(answer.answer)}', ${answer.isPrimary})">수정</button>
                        <button class="btn btn-sm btn-delete" onclick="deleteAnswer(${answer.id})">삭제</button>
                    </div>
                </div>
            `).join('');
        } else {
            listContainer.innerHTML = `
                <div class="empty-answers">
                    <p>등록된 정답이 없습니다.</p>
                    <p class="hint">정답이 없으면 노래 제목으로 정답 체크됩니다.</p>
                </div>
            `;
        }
    } catch (error) {
        // console.error('Error:', error);
        listContainer.innerHTML = '<div class="error">정답을 불러오는데 실패했습니다.</div>';
    }
}

// 정답 추가
async function addAnswer() {
    const answer = document.getElementById('newAnswer').value.trim();
    const isPrimary = document.getElementById('newAnswerPrimary').checked;
    
    if (!answer) {
        showToast('정답을 입력해주세요.');
        return;
    }
    
    try {
        const response = await fetch('/admin/answer/add', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                songId: currentSongId,
                answer: answer,
                isPrimary: isPrimary
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            document.getElementById('newAnswer').value = '';
            document.getElementById('newAnswerPrimary').checked = false;
            loadAnswers(currentSongId);
        } else {
            showToast(result.message);
        }
    } catch (error) {
        // console.error('Error:', error);
        showToast('정답 추가 중 오류가 발생했습니다.');
    }
}

// 정답 삭제
async function deleteAnswer(answerId) {
    if (!confirm('이 정답을 삭제하시겠습니까?')) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/answer/delete/${answerId}`, {
            method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.success) {
            loadAnswers(currentSongId);
        } else {
            showToast(result.message);
        }
    } catch (error) {
        // console.error('Error:', error);
        showToast('정답 삭제 중 오류가 발생했습니다.');
    }
}

// 정답 수정 모달 열기
function openEditModal(answerId, answerText, isPrimary) {
    document.getElementById('editAnswerId').value = answerId;
    document.getElementById('editAnswerText').value = answerText;
    document.getElementById('editAnswerPrimary').checked = isPrimary;
    document.getElementById('editAnswerModal').classList.add('show');
}

// 정답 수정 모달 닫기
function closeEditModal() {
    document.getElementById('editAnswerModal').classList.remove('show');
}

// 정답 수정 저장
async function saveEditAnswer() {
    const answerId = document.getElementById('editAnswerId').value;
    const answer = document.getElementById('editAnswerText').value.trim();
    const isPrimary = document.getElementById('editAnswerPrimary').checked;
    
    if (!answer) {
        showToast('정답을 입력해주세요.');
        return;
    }
    
    try {
        const response = await fetch(`/admin/answer/update/${answerId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                answer: answer,
                isPrimary: isPrimary
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            closeEditModal();
            loadAnswers(currentSongId);
        } else {
            showToast(result.message);
        }
    } catch (error) {
        // console.error('Error:', error);
        showToast('정답 수정 중 오류가 발생했습니다.');
    }
}

// 정답 자동 생성 (개별 노래)
async function autoGenerateAnswers() {
    if (!confirm('제목을 기반으로 정답을 자동 생성하시겠습니까?')) {
        return;
    }
    
    try {
        const response = await fetch(`/admin/answer/auto-generate/${currentSongId}`, {
            method: 'POST'
        });
        
        const result = await response.json();
        showToast(result.message);
        
        if (result.success) {
            loadAnswers(currentSongId);
        }
    } catch (error) {
        // console.error('Error:', error);
        showToast('자동 생성 중 오류가 발생했습니다.');
    }
}

// HTML 이스케이프
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// JavaScript 문자열 이스케이프
function escapeJs(text) {
    return text.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"');
}

// Enter 키로 정답 추가
document.addEventListener('DOMContentLoaded', function() {
    const newAnswerInput = document.getElementById('newAnswer');
    if (newAnswerInput) {
        newAnswerInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                addAnswer();
            }
        });
    }
    
    const editAnswerInput = document.getElementById('editAnswerText');
    if (editAnswerInput) {
        editAnswerInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                saveEditAnswer();
            }
        });
    }
});

// 모달 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    if (e.target.id === 'answerModal') {
        closeAnswerModal();
    }
    if (e.target.id === 'editAnswerModal') {
        closeEditModal();
    }
});

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        const answerModal = document.getElementById('answerModal');
        const editModal = document.getElementById('editAnswerModal');

        if (editModal && editModal.classList.contains('show')) {
            closeEditModal();
        } else if (answerModal && answerModal.classList.contains('show')) {
            closeAnswerModal();
        }
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
    if (typeof currentSort !== 'undefined' && currentSort === column) {
        params.set('direction', (typeof currentDirection !== 'undefined' && currentDirection === 'asc') ? 'desc' : 'asc');
    } else {
        params.set('sort', column);
        params.set('direction', 'desc');
    }
    params.set('page', '0');
    window.location.href = '/admin/answer?' + params.toString();
}