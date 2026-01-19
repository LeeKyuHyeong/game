let maxAvailableSongs = 999;

// ========== 30곡 챌린지 ==========

function openChallengeModal() {
    document.getElementById('challengeModal').classList.add('active');
}

function closeChallengeModal() {
    document.getElementById('challengeModal').classList.remove('active');
}

// 모달 외부 클릭 시 닫기
document.addEventListener('click', function(e) {
    const modal = document.getElementById('challengeModal');
    if (e.target === modal) {
        closeChallengeModal();
    }
});

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeChallengeModal();
    }
});

async function startChallenge() {
    // 로그인 사용자의 닉네임 사용
    const nickname = memberNickname || '';
    if (!nickname) {
        showToast('로그인이 필요합니다.');
        window.location.href = '/auth/login';
        return;
    }

    try {
        const response = await fetch('/game/retro/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                nickname: nickname,
                totalRounds: 30,
                settings: {}
            })
        });

        const result = await response.json();

        if (result.success) {
            sessionStorage.setItem('challengeMode', 'true');
            window.location.href = '/game/retro/play';
        } else {
            showToast(result.message || '게임 시작에 실패했습니다.');
        }
    } catch (error) {
        showToast('게임 시작 중 오류가 발생했습니다.');
        // console.error(error);
    }
}

// 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 로그인한 경우 닉네임 자동 입력
    if (typeof isLoggedIn !== 'undefined' && isLoggedIn && memberNickname) {
        document.getElementById('nickname').value = memberNickname;
    }

    // 초기 노래 개수 업데이트
    updateSongCount();

    // 라운드 입력 이벤트
    document.getElementById('totalRounds').addEventListener('change', function() {
        validateRounds();
        updatePresetButtons();
    });

    document.getElementById('totalRounds').addEventListener('input', function() {
        updatePresetButtons();
    });

    // Enter 키 이벤트
    document.getElementById('nickname').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            startGame();
        }
    });

    // 시작 버튼 이벤트
    document.getElementById('startBtn').addEventListener('click', startGame);
});

// ========== 라운드 설정 ==========

function adjustRounds(delta) {
    const input = document.getElementById('totalRounds');
    let value = parseInt(input.value) || 10;
    value = Math.max(1, Math.min(100, value + delta));

    if (value > maxAvailableSongs) {
        value = maxAvailableSongs;
    }

    input.value = value;
    updatePresetButtons();
}

function setRounds(value) {
    if (value > maxAvailableSongs) {
        showToast(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }
    document.getElementById('totalRounds').value = value;
    updatePresetButtons();
}

function updatePresetButtons() {
    const currentValue = parseInt(document.getElementById('totalRounds').value) || 10;

    document.querySelectorAll('.preset-btn').forEach(btn => {
        const btnValue = parseInt(btn.textContent);
        btn.classList.remove('active');

        if (btnValue > maxAvailableSongs) {
            btn.classList.add('disabled');
            btn.disabled = true;
        } else {
            btn.classList.remove('disabled');
            btn.disabled = false;
            if (btnValue === currentValue) {
                btn.classList.add('active');
            }
        }
    });
}

function validateRounds() {
    const input = document.getElementById('totalRounds');
    let value = parseInt(input.value) || 10;

    if (value < 1) value = 1;
    if (value > 100) value = 100;
    if (value > maxAvailableSongs && maxAvailableSongs > 0) {
        value = maxAvailableSongs;
    }

    input.value = value;
}

function getTotalRounds() {
    return parseInt(document.getElementById('totalRounds').value) || 10;
}

// ========== 노래 개수 조회 ==========

async function updateSongCount() {
    try {
        const response = await fetch('/game/retro/song-count', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });
        const result = await response.json();

        maxAvailableSongs = result.count;

        const infoEl = document.getElementById('songCountInfo');
        if (maxAvailableSongs === 0) {
            infoEl.textContent = '(사용 가능한 노래 없음)';
            infoEl.style.color = '#ef4444';
        } else {
            infoEl.textContent = `(최대 ${maxAvailableSongs}라운드)`;
            infoEl.style.color = '#22c55e';
        }

        updatePresetButtons();

        const currentRounds = getTotalRounds();
        if (maxAvailableSongs > 0 && currentRounds > maxAvailableSongs) {
            document.getElementById('totalRounds').value = maxAvailableSongs;
            updatePresetButtons();
        }

    } catch (error) {
        // console.error('노래 개수 조회 오류:', error);
    }
}

// ========== 게임 시작 ==========

async function startGame() {
    const nickname = document.getElementById('nickname').value.trim();
    if (!nickname) {
        showToast('닉네임을 입력해주세요.');
        document.getElementById('nickname').focus();
        return;
    }

    if (maxAvailableSongs === 0) {
        showToast('레트로 노래가 없습니다.');
        return;
    }

    const totalRounds = getTotalRounds();

    if (totalRounds > maxAvailableSongs) {
        showToast(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }

    try {
        const response = await fetch('/game/retro/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                nickname: nickname,
                totalRounds: totalRounds,
                settings: {}
            })
        });

        const result = await response.json();

        if (result.success) {
            if (result.reducedCount && result.reducedCount > 0) {
                sessionStorage.setItem('roundsReduced', JSON.stringify({
                    requested: result.requestedRounds,
                    actual: result.actualRounds,
                    reduced: result.reducedCount
                }));
            }
            window.location.href = '/game/retro/play';
        } else {
            showToast(result.message || '게임 시작에 실패했습니다.');
        }
    } catch (error) {
        showToast('게임 시작 중 오류가 발생했습니다.');
        // console.error(error);
    }
}
