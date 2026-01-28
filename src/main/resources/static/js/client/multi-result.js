/**
 * client/game/multi/result.html - 멀티게임 결과
 *
 * 주의: roomCode, isHost 변수는 HTML에서 Thymeleaf로 설정해야 함
 */

let pollingInterval = null;

// 페이지 로드 시 폴링 시작 (비방장만)
document.addEventListener('DOMContentLoaded', function() {
    if (!isHost) {
        startRestartPolling();
    }
});

// 재시작 감지 폴링 시작
function startRestartPolling() {
    pollingInterval = setInterval(checkRoomStatus, 2000);
}

// 폴링 중지
function stopRestartPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

// 방 상태 확인
async function checkRoomStatus() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/status`);
        const result = await response.json();

        // 방이 WAITING 상태면 재시작됨 → 대기실로 이동
        if (result.status === 'WAITING') {
            stopRestartPolling();
            showRestartNotice();
            setTimeout(() => {
                window.location.href = `/game/multi/room/${roomCode}`;
            }, 1500);
        }

        // 방이 없거나 강퇴됨 → 로비로 이동
        if (!result.success || result.kicked) {
            stopRestartPolling();
            window.location.href = '/game/multi';
        }
    } catch (error) {
        // console.error('상태 확인 오류:', error);
    }
}

// 재시작 알림 표시
function showRestartNotice() {
    const notice = document.getElementById('restartNotice');
    if (notice) {
        notice.style.display = 'block';
    }
}

async function restartGame() {
    if (!isHost) {
        showToast('방장만 게임을 재시작할 수 있습니다.');
        return;
    }

    if (!confirm('같은 설정으로 한번 더 게임을 진행하시겠습니까?')) {
        return;
    }

    try {
        const response = await fetch(`/game/multi/room/${roomCode}/restart`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        const result = await response.json();

        if (result.success) {
            // 대기실로 이동
            window.location.href = `/game/multi/room/${roomCode}`;
        } else {
            showToast(result.message || '게임 재시작에 실패했습니다.');
        }
    } catch (error) {
        // console.error('재시작 오류:', error);
        showToast('게임 재시작 중 오류가 발생했습니다.');
    }
}

async function goToLobby() {
    stopRestartPolling();
    // 명시적 로비 이동 - 재시작 대상에서 제외하기 위해 별도 엔드포인트 사용
    try {
        await fetch(`/game/multi/room/${roomCode}/leave-to-lobby`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
    } catch (error) {
        // 나가기 실패해도 로비로 이동
        Debug.log('나가기 처리 중 오류 (무시됨):', error);
    }
    window.location.href = '/game/multi';
}
