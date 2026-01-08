let pollingInterval;
let lastStatus = null;

// 페이지 로드 시 폴링 시작
document.addEventListener('DOMContentLoaded', function() {
    startPolling();
});

// 페이지 떠날 때 폴링 중지
window.addEventListener('beforeunload', function() {
    stopPolling();
});

// 폴링 시작
function startPolling() {
    // 즉시 한 번 실행
    fetchRoomStatus();
    // 2초마다 갱신
    pollingInterval = setInterval(fetchRoomStatus, 2000);
}

// 폴링 중지
function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

// 방 상태 조회
async function fetchRoomStatus() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/status`);
        const result = await response.json();

        if (!result.success) {
            // 방이 삭제됨
            alert('방이 종료되었습니다.');
            window.location.href = '/game/multi';
            return;
        }

        // 게임 시작됨
        if (result.status === 'PLAYING') {
            stopPolling();
            window.location.href = `/game/multi/room/${roomCode}/play`;
            return;
        }

        // 참가자 목록 갱신
        updateParticipantsList(result.participants, result.hostId);

        // 시작 버튼 상태 갱신 (방장용)
        if (isHost) {
            updateStartButton(result.allReady, result.participants.length);
        }

    } catch (error) {
        console.error('상태 조회 오류:', error);
    }
}

// 참가자 목록 갱신
function updateParticipantsList(participants, hostId) {
    const container = document.getElementById('participantsList');

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

    // 내 준비 상태 버튼 갱신
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
        // 폴링으로 UI 갱신됨

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
        // 폴링으로 UI 갱신됨

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

        if (result.success) {
            // 폴링이 PLAYING 상태를 감지하면 자동으로 플레이 페이지로 이동
        } else {
            alert(result.message || '게임 시작에 실패했습니다.');
        }

    } catch (error) {
        console.error('게임 시작 오류:', error);
        alert('게임 시작 중 오류가 발생했습니다.');
    }
}

// 참가 코드 복사
function copyRoomCode() {
    navigator.clipboard.writeText(roomCode).then(() => {
        alert('참가 코드가 복사되었습니다: ' + roomCode);
    }).catch(() => {
        // 폴백
        const tempInput = document.createElement('input');
        tempInput.value = roomCode;
        document.body.appendChild(tempInput);
        tempInput.select();
        document.execCommand('copy');
        document.body.removeChild(tempInput);
        alert('참가 코드가 복사되었습니다: ' + roomCode);
    });
}

// HTML 이스케이프
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}