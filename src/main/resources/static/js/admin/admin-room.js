// admin-room.js

let currentRoomId = null;

// 방 상세 조회
function viewRoom(id) {
    currentRoomId = id;

    fetch(`/admin/room/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            const content = document.getElementById('roomDetailContent');
            content.innerHTML = `
                <div class="detail-section">
                    <h3>기본 정보</h3>
                    <div class="info-grid">
                        <div class="info-item">
                            <label>방 코드</label>
                            <span><code>${data.roomCode}</code></span>
                        </div>
                        <div class="info-item">
                            <label>방 이름</label>
                            <span>${data.roomName}</span>
                        </div>
                        <div class="info-item">
                            <label>방장</label>
                            <span>${data.hostNickname}</span>
                        </div>
                        <div class="info-item">
                            <label>상태</label>
                            <span class="status-badge ${getStatusClass(data.status)}">${getStatusText(data.status)}</span>
                        </div>
                        <div class="info-item">
                            <label>인원</label>
                            <span>${data.currentPlayers} / ${data.maxPlayers}</span>
                        </div>
                        <div class="info-item">
                            <label>라운드</label>
                            <span>${data.currentRound} / ${data.totalRounds}</span>
                        </div>
                        <div class="info-item">
                            <label>비공개</label>
                            <span>${data.isPrivate ? '예' : '아니오'}</span>
                        </div>
                        <div class="info-item">
                            <label>생성일</label>
                            <span>${formatDateTime(data.createdAt)}</span>
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>참가자 목록</h3>
                    <table class="data-table mini-table">
                        <thead>
                            <tr>
                                <th>닉네임</th>
                                <th>점수</th>
                                <th>준비</th>
                                <th>상태</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${data.participants.map(p => `
                                <tr>
                                    <td>${p.nickname}</td>
                                    <td>${p.score}</td>
                                    <td>${p.isReady ? '✓' : '-'}</td>
                                    <td>${p.status}</td>
                                </tr>
                            `).join('')}
                            ${data.participants.length === 0 ? '<tr><td colspan="4" class="empty-row">참가자 없음</td></tr>' : ''}
                        </tbody>
                    </table>
                </div>

                ${data.settings ? `
                <div class="detail-section">
                    <h3>게임 설정</h3>
                    <pre class="settings-json">${JSON.stringify(JSON.parse(data.settings), null, 2)}</pre>
                </div>
                ` : ''}
            `;

            openModalById('roomDetailModal');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('방 정보를 불러오는데 실패했습니다.');
        });
}

// 채팅 내역 조회
function viewChat(id) {
    currentRoomId = id;

    fetch(`/admin/room/chat/${id}`)
        .then(response => response.json())
        .then(data => {
            if (!data.success) {
                alert(data.message);
                return;
            }

            document.getElementById('chatModalTitle').textContent =
                `채팅 내역 - ${data.roomName} (${data.roomCode})`;

            const content = document.getElementById('chatContent');

            if (data.chats.length === 0) {
                content.innerHTML = '<div class="empty-chat">채팅 내역이 없습니다.</div>';
            } else {
                content.innerHTML = data.chats.map(chat => `
                    <div class="chat-item ${chat.messageType.toLowerCase()}">
                        <div class="chat-header">
                            <span class="chat-nickname">${chat.nickname}</span>
                            <span class="chat-time">${formatDateTime(chat.createdAt)}</span>
                            <button class="btn btn-xs btn-delete" onclick="deleteSingleChat(${chat.id})">삭제</button>
                        </div>
                        <div class="chat-message">${escapeHtml(chat.message)}</div>
                        ${chat.messageType === 'CORRECT_ANSWER' ? `<span class="chat-badge correct">정답 (R${chat.roundNumber})</span>` : ''}
                        ${chat.messageType === 'SYSTEM' ? '<span class="chat-badge system">시스템</span>' : ''}
                    </div>
                `).join('');
            }

            document.getElementById('deleteAllChatsBtn').onclick = () => deleteAllChats(id);

            openModalById('chatModal');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('채팅 내역을 불러오는데 실패했습니다.');
        });
}

// 방 종료
function closeRoom(id) {
    if (!confirm('이 방을 종료하시겠습니까?')) return;

    fetch(`/admin/room/close/${id}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.success) {
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('방 종료 중 오류가 발생했습니다.');
        });
}

// 방 삭제
function deleteRoom(id) {
    if (!confirm('이 방을 삭제하시겠습니까?\n채팅 내역도 함께 삭제됩니다.')) return;

    fetch(`/admin/room/delete/${id}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.success) {
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('방 삭제 중 오류가 발생했습니다.');
        });
}

// 단일 채팅 삭제
function deleteSingleChat(chatId) {
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return;

    fetch(`/admin/room/chat/delete/${chatId}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                viewChat(currentRoomId); // 채팅 목록 새로고침
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('채팅 삭제 중 오류가 발생했습니다.');
        });
}

// 모든 채팅 삭제
function deleteAllChats(roomId) {
    if (!confirm('이 방의 모든 채팅을 삭제하시겠습니까?')) return;

    fetch(`/admin/room/chat/delete-all/${roomId}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => {
            alert(data.message);
            if (data.success) {
                viewChat(roomId); // 채팅 목록 새로고침
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('채팅 삭제 중 오류가 발생했습니다.');
        });
}

// 모달 열기
function openModalById(modalId) {
    document.getElementById(modalId).classList.add('show');
}

// 모달 닫기
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// 상태 클래스 반환
function getStatusClass(status) {
    switch (status) {
        case 'WAITING': return 'warning';
        case 'PLAYING': return 'success';
        case 'FINISHED': return 'info';
        default: return '';
    }
}

// 상태 텍스트 반환
function getStatusText(status) {
    switch (status) {
        case 'WAITING': return '대기중';
        case 'PLAYING': return '게임중';
        case 'FINISHED': return '종료';
        default: return status;
    }
}

// 날짜 포맷
function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR');
}

// HTML 이스케이프
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
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