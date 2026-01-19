// multi-join.js

let currentRoomCode = null;
let isPrivate = false;

document.addEventListener('DOMContentLoaded', function() {
    // URL에서 방 코드 파라미터 확인
    const urlParams = new URLSearchParams(window.location.search);
    const codeParam = urlParams.get('code');

    if (codeParam) {
        document.getElementById('roomCode').value = codeParam;
        searchRoom();
    }

    // 폼 제출
    document.getElementById('joinForm').addEventListener('submit', function(e) {
        e.preventDefault();
        joinRoom();
    });

    // 방 코드 입력 시 자동 검색 (6자 이상)
    document.getElementById('roomCode').addEventListener('input', function(e) {
        const code = e.target.value.trim();
        if (code.length >= 6) {
            searchRoom();
        } else {
            hideRoomInfo();
        }
    });
});

// 방 검색
function searchRoom() {
    const roomCode = document.getElementById('roomCode').value.trim().toUpperCase();

    if (!roomCode || roomCode.length < 4) {
        showToast('방 코드를 입력해주세요.');
        return;
    }

    fetch(`/game/multi/room/${roomCode}/status`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentRoomCode = roomCode;
                showRoomInfo(data);
            } else {
                hideRoomInfo();
                showToast(data.message || '방을 찾을 수 없습니다.');
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            hideRoomInfo();
            showToast('방 정보를 불러오는데 실패했습니다.');
        });
}

// 방 정보 표시
function showRoomInfo(data) {
    const card = document.getElementById('roomInfoCard');
    card.style.display = 'block';

    document.getElementById('roomName').textContent = data.roomName;
    document.getElementById('hostNickname').textContent = data.hostNickname;
    document.getElementById('playerCount').textContent = data.participants.length;
    document.getElementById('maxPlayers').textContent = data.maxPlayers;
    document.getElementById('totalRounds').textContent = data.totalRounds;

    // 상태 배지
    const statusBadge = document.querySelector('.badge-status');
    if (data.status === 'WAITING') {
        statusBadge.textContent = '대기중';
        statusBadge.className = 'badge-status waiting';
    } else if (data.status === 'PLAYING') {
        statusBadge.textContent = '게임중';
        statusBadge.className = 'badge-status playing';
    } else {
        statusBadge.textContent = '종료됨';
        statusBadge.className = 'badge-status finished';
    }

    // 비공개 방
    isPrivate = data.isPrivate;
    document.getElementById('privateBadge').style.display = isPrivate ? 'inline' : 'none';
    document.getElementById('passwordGroup').style.display = isPrivate ? 'block' : 'none';

    // 참가자 목록
    const participantList = document.getElementById('participantList');
    participantList.innerHTML = data.participants.map(p => `
        <span class="participant-tag ${p.isHost ? 'host' : ''}">
            ${p.isHost ? '👑 ' : ''}${p.nickname}
            ${p.isReady ? ' ✓' : ''}
        </span>
    `).join('');

    // 참가 버튼 활성화 여부
    const joinBtn = document.getElementById('joinBtn');
    const memberId = document.getElementById('currentMemberId').value;

    // 이미 참가중인지 확인
    const alreadyJoined = data.participants.some(p => p.memberId == memberId);

    if (alreadyJoined) {
        joinBtn.textContent = '대기실로 이동';
        joinBtn.disabled = false;
    } else if (data.status !== 'WAITING') {
        joinBtn.textContent = '참가 불가 (게임중)';
        joinBtn.disabled = true;
    } else if (data.participants.length >= data.maxPlayers) {
        joinBtn.textContent = '참가 불가 (정원 초과)';
        joinBtn.disabled = true;
    } else {
        joinBtn.textContent = '참가하기';
        joinBtn.disabled = false;
    }
}

// 방 정보 숨기기
function hideRoomInfo() {
    document.getElementById('roomInfoCard').style.display = 'none';
    document.getElementById('joinBtn').disabled = true;
    document.getElementById('joinBtn').textContent = '참가하기';
    currentRoomCode = null;
}

// 방 참가
function joinRoom() {
    if (!currentRoomCode) {
        showToast('먼저 방을 검색해주세요.');
        return;
    }

    const password = document.getElementById('password').value;

    if (isPrivate && !password) {
        showToast('비밀번호를 입력해주세요.');
        return;
    }

    const data = { password: password };

    fetch(`/game/multi/join/${currentRoomCode}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // 대기실로 이동
                window.location.href = `/game/multi/room/${currentRoomCode}`;
            } else {
                showToast(data.message || '참가에 실패했습니다.');
            }
        })
        .catch(error => {
            // console.error('Error:', error);
            showToast('참가 처리 중 오류가 발생했습니다.');
        });
}