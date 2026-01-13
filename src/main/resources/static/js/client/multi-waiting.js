let pollingInterval;
let chatPollingInterval;
let lastStatus = null;
let lastChatId = 0;

// 페이지 로드 시 폴링 시작
document.addEventListener('DOMContentLoaded', function() {
    startPolling();
    startChatPolling();
});

// 페이지 떠날 때 폴링 중지 및 방 나가기
window.addEventListener('beforeunload', function() {
    stopPolling();
    stopChatPolling();
    // sendBeacon으로 방 나가기 요청 (페이지 언로드되어도 전송 보장)
    navigator.sendBeacon(`/game/multi/room/${roomCode}/leave`);
});

// 뒤로가기/앞으로가기 시에도 나가기 처리
window.addEventListener('pagehide', function() {
    stopPolling();
    stopChatPolling();
    navigator.sendBeacon(`/game/multi/room/${roomCode}/leave`);
});

// 폴링 시작
function startPolling() {
    fetchRoomStatus();
    pollingInterval = setInterval(fetchRoomStatus, 2000);
}

// 폴링 중지
function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

// 채팅 폴링 시작
function startChatPolling() {
    fetchChats();
    chatPollingInterval = setInterval(fetchChats, 1000);
}

// 채팅 폴링 중지
function stopChatPolling() {
    if (chatPollingInterval) {
        clearInterval(chatPollingInterval);
        chatPollingInterval = null;
    }
}

// 방 상태 조회
async function fetchRoomStatus() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/status`);
        const result = await response.json();

        if (!result.success) {
            alert('방이 종료되었습니다.');
            window.location.href = '/game/multi';
            return;
        }

        if (result.status === 'PLAYING') {
            stopPolling();
            stopChatPolling();
            window.location.href = `/game/multi/room/${roomCode}/play`;
            return;
        }

        updateParticipantsList(result.participants, result.hostId);

        if (isHost) {
            updateStartButton(result.allReady, result.participants.length);
        }

    } catch (error) {
        console.error('상태 조회 오류:', error);
    }
}

// 채팅 목록 조회
async function fetchChats() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/chats?lastId=${lastChatId}`);
        const result = await response.json();

        if (result.success && result.chats && result.chats.length > 0) {
            appendChats(result.chats);
        }
    } catch (error) {
        console.error('채팅 조회 오류:', error);
    }
}

// 채팅 메시지 추가
function appendChats(chats) {
    const container = document.getElementById('chatMessages');
    if (!container) return;

    chats.forEach(chat => {
        if (chat.id > lastChatId) {
            lastChatId = chat.id;

            const msgDiv = document.createElement('div');
            msgDiv.className = 'chat-message';

            if (chat.messageType === 'SYSTEM') {
                msgDiv.classList.add('system');
                msgDiv.innerHTML = `<span class="system-text">${escapeHtml(chat.message)}</span>`;
            } else {
                const isMe = chat.memberId === myMemberId;
                if (isMe) msgDiv.classList.add('mine');

                msgDiv.innerHTML = `
                    <span class="chat-nickname ${isMe ? 'me' : ''}">${escapeHtml(chat.nickname)}</span>
                    <span class="chat-text">${escapeHtml(chat.message)}</span>
                `;
            }

            container.appendChild(msgDiv);
        }
    });

    container.scrollTop = container.scrollHeight;
}

// 채팅 전송
async function sendChat() {
    const input = document.getElementById('chatInput');
    const message = input.value.trim();

    if (!message) return;

    input.value = '';
    input.focus();

    try {
        const response = await fetch(`/game/multi/room/${roomCode}/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message })
        });

        const result = await response.json();

        if (!result.success) {
            console.error('채팅 전송 실패:', result.message);
        }
    } catch (error) {
        console.error('채팅 전송 오류:', error);
    }
}

// 참가자 목록 갱신
function updateParticipantsList(participants, hostId) {
    const container = document.getElementById('participantsList');
    if (!container) return;

    container.innerHTML = participants.map(p => {
        const isHostMember = p.memberId === hostId;
        const isMe = p.memberId === myMemberId;

        return `
            <div class="participant-card ${p.isReady ? 'ready' : ''}" data-member-id="${p.memberId}">
                <div class="participant-info">
                    <span class="participant-icon">${isHostMember ? '👑' : '👤'}</span>
                    <span class="participant-name">${escapeHtml(p.nickname)}${isMe ? ' (나)' : ''}</span>
                </div>
                <div class="participant-status">
                    ${isHostMember
                        ? '<span class="status-badge host">방장</span>'
                        : `<span class="status-badge ${p.isReady ? 'ready' : 'waiting'}">${p.isReady ? '준비완료' : '대기중'}</span>`
                    }
                </div>
                ${isHost && !isHostMember ? `<button type="button" class="btn btn-kick" onclick="kickPlayer(${p.memberId})">강퇴</button>` : ''}
            </div>
        `;
    }).join('');

    if (!isHost) {
        const myInfo = participants.find(p => p.memberId === myMemberId);
        if (myInfo) {
            const readyBtn = document.getElementById('readyBtn');
            if (readyBtn) {
                if (myInfo.isReady) {
                    readyBtn.classList.add('is-ready');
                    readyBtn.innerHTML = '✅ 준비 완료';
                } else {
                    readyBtn.classList.remove('is-ready');
                    readyBtn.innerHTML = '🎮 준비하기';
                }
            }
        }
    }
}

// 시작 버튼 상태 갱신
function updateStartButton(allReady, participantCount) {
    const startBtn = document.getElementById('startGameBtn');
    if (!startBtn) return;

    if (participantCount < 2) {
        startBtn.disabled = true;
        startBtn.textContent = '👥 2명 이상 필요';
    } else if (!allReady) {
        startBtn.disabled = true;
        startBtn.textContent = '⏳ 모두 준비 대기중';
    } else {
        startBtn.disabled = false;
        startBtn.textContent = '🚀 게임 시작';
    }
}

// 준비 상태 토글
async function toggleReady() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/ready`, {
            method: 'POST'
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '준비 상태 변경에 실패했습니다.');
        }
    } catch (error) {
        console.error('준비 상태 변경 오류:', error);
    }
}

// 방 나가기
async function leaveRoom() {
    if (!confirm('정말 방을 나가시겠습니까?')) return;

    try {
        const response = await fetch(`/game/multi/room/${roomCode}/leave`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            stopPolling();
            stopChatPolling();
            window.location.href = '/game/multi';
        } else {
            alert(result.message || '나가기에 실패했습니다.');
        }
    } catch (error) {
        console.error('나가기 오류:', error);
        window.location.href = '/game/multi';
    }
}

// 강퇴
async function kickPlayer(memberId) {
    if (!confirm('정말 이 플레이어를 강퇴하시겠습니까?')) return;

    try {
        const response = await fetch(`/game/multi/room/${roomCode}/kick/${memberId}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '강퇴에 실패했습니다.');
        }
    } catch (error) {
        console.error('강퇴 오류:', error);
    }
}

// 게임 시작 (방장용)
async function startGame() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/start`, {
            method: 'POST'
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '게임 시작에 실패했습니다.');
        }
    } catch (error) {
        console.error('게임 시작 오류:', error);
        alert('게임 시작 중 오류가 발생했습니다.');
    }
}

// 참가 코드 복사
function copyRoomCode() {
    // Clipboard API가 지원되는지 확인 (HTTPS 또는 localhost에서만 사용 가능)
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(roomCode).then(() => {
            showCopySuccess();
        }).catch(() => {
            fallbackCopy();
        });
    } else {
        fallbackCopy();
    }
}

// 폴백 복사 (구형 브라우저 또는 HTTP 환경)
function fallbackCopy() {
    const tempInput = document.createElement('textarea');
    tempInput.value = roomCode;
    tempInput.style.position = 'fixed';
    tempInput.style.left = '-9999px';
    tempInput.style.top = '0';
    document.body.appendChild(tempInput);
    tempInput.focus();
    tempInput.select();

    try {
        document.execCommand('copy');
        showCopySuccess();
    } catch (err) {
        // 복사 실패 시 직접 코드 표시
        prompt('참가 코드를 복사하세요:', roomCode);
    }

    document.body.removeChild(tempInput);
}

// 복사 성공 메시지
function showCopySuccess() {
    const copyBtn = document.querySelector('.btn-copy');
    if (copyBtn) {
        const originalText = copyBtn.textContent;
        copyBtn.textContent = '✅';
        copyBtn.disabled = true;
        setTimeout(() => {
            copyBtn.textContent = originalText;
            copyBtn.disabled = false;
        }, 1500);
    }
}

// HTML 이스케이프
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}