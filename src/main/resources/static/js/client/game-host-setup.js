let playerCount = 2;
let totalRounds = 10;
let maxAvailableSongs = 999;

// 초기 노래 개수 로딩
document.addEventListener('DOMContentLoaded', function() {
    updateSongCount();
});

// 라운드 버튼 이벤트
document.querySelectorAll('.round-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        const value = parseInt(this.dataset.value);
        if (value > maxAvailableSongs) {
            alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
            return;
        }
        document.querySelectorAll('.round-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        totalRounds = value;
    });
});

// 게임 모드 변경 이벤트
document.querySelectorAll('input[name="gameMode"]').forEach(radio => {
    radio.addEventListener('change', function() {
        const genreSelect = document.getElementById('genreSelect');
        const genreRoundSetting = document.getElementById('genreRoundSetting');

        if (this.value === 'FIXED_GENRE') {
            genreSelect.style.display = 'block';
            genreRoundSetting.style.display = 'none';
        } else if (this.value === 'GENRE_PER_ROUND') {
            genreSelect.style.display = 'none';
            genreRoundSetting.style.display = 'block';
        } else {
            genreSelect.style.display = 'none';
            genreRoundSetting.style.display = 'none';
        }
        updateSongCount();
    });
});

// 장르 선택 변경 이벤트
document.getElementById('fixedGenreId').addEventListener('change', function() {
    updateSongCount();
});

// 연도 범위 변경 이벤트
document.getElementById('yearFrom').addEventListener('change', updateSongCount);
document.getElementById('yearTo').addEventListener('change', updateSongCount);

// 아티스트 유형 변경 이벤트
document.querySelectorAll('input[name="artistType"]').forEach(radio => {
    radio.addEventListener('change', updateSongCount);
});

async function updateSongCount() {
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    // GENRE_PER_ROUND 모드는 전체 노래 개수로 계산
    let genreId = null;
    if (gameMode === 'FIXED_GENRE') {
        genreId = document.getElementById('fixedGenreId').value || null;
    }

    const yearFrom = document.getElementById('yearFrom').value || null;
    const yearTo = document.getElementById('yearTo').value || null;

    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    let soloOnly = null;
    let groupOnly = null;
    if (artistType === 'solo') soloOnly = true;
    if (artistType === 'group') groupOnly = true;

    try {
        const params = new URLSearchParams();
        if (genreId) params.append('genreId', genreId);
        if (yearFrom) params.append('yearFrom', yearFrom);
        if (yearTo) params.append('yearTo', yearTo);
        if (soloOnly) params.append('soloOnly', soloOnly);
        if (groupOnly) params.append('groupOnly', groupOnly);

        const response = await fetch(`/game/solo/host/song-count?${params.toString()}`);
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

        // 라운드 버튼 활성화/비활성화
        document.querySelectorAll('.round-btn').forEach(btn => {
            const value = parseInt(btn.dataset.value);
            if (value > maxAvailableSongs) {
                btn.classList.add('disabled');
                btn.disabled = true;
            } else {
                btn.classList.remove('disabled');
                btn.disabled = false;
            }
        });

        // 현재 선택된 라운드가 최대치를 초과하면 조정
        if (totalRounds > maxAvailableSongs && maxAvailableSongs > 0) {
            const availableButtons = document.querySelectorAll('.round-btn:not(.disabled)');
            if (availableButtons.length > 0) {
                document.querySelectorAll('.round-btn').forEach(b => b.classList.remove('active'));
                const lastAvailable = availableButtons[availableButtons.length - 1];
                lastAvailable.classList.add('active');
                totalRounds = parseInt(lastAvailable.dataset.value);
            }
        }

    } catch (error) {
        console.error('노래 개수 조회 오류:', error);
    }
}

function changePlayerCount(delta) {
    playerCount = Math.max(2, Math.min(10, playerCount + delta));
    document.getElementById('playerCount').textContent = playerCount;
}

function goToStep1() {
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step1').style.display = 'block';
}

function goToStep2() {
    // 게임 모드 검증
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            return;
        }
    }

    if (maxAvailableSongs === 0) {
        alert('현재 조건에 맞는 노래가 없습니다. 조건을 변경해주세요.');
        return;
    }

    if (totalRounds > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }

    document.getElementById('step1').style.display = 'none';
    document.getElementById('step3').style.display = 'none';
    document.getElementById('step2').style.display = 'block';
}

function goToStep3() {
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step3').style.display = 'block';

    // 플레이어 입력 필드 생성
    const container = document.getElementById('playersInput');
    container.innerHTML = '';

    for (let i = 1; i <= playerCount; i++) {
        const div = document.createElement('div');
        div.className = 'player-input-item';
        div.innerHTML = `
            <label>플레이어 ${i}</label>
            <input type="text" class="player-name-input" placeholder="별명 입력" maxlength="10" required>
        `;
        container.appendChild(div);
    }

    // 첫번째 입력에 포커스
    container.querySelector('input').focus();
}

async function startGame() {
    // 모든 플레이어 이름 수집
    const inputs = document.querySelectorAll('.player-name-input');
    const players = [];

    for (const input of inputs) {
        const name = input.value.trim();
        if (!name) {
            alert('모든 플레이어의 별명을 입력해주세요.');
            input.focus();
            return;
        }
        if (players.includes(name)) {
            alert('중복된 별명이 있습니다.');
            input.focus();
            return;
        }
        players.push(name);
    }

    // 게임 모드
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    // 장르 체크 (FIXED_GENRE 모드일 때만)
    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            return;
        }
    }

    // 최종 라운드 수 검증
    if (totalRounds > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }

    // 설정 수집
    const settings = {};

    const yearFrom = document.getElementById('yearFrom').value;
    const yearTo = document.getElementById('yearTo').value;
    if (yearFrom) settings.yearFrom = parseInt(yearFrom);
    if (yearTo) settings.yearTo = parseInt(yearTo);

    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    if (artistType === 'solo') settings.soloOnly = true;
    if (artistType === 'group') settings.groupOnly = true;

    if (gameMode === 'FIXED_GENRE') {
        settings.fixedGenreId = parseInt(document.getElementById('fixedGenreId').value);
    }

    if (gameMode === 'GENRE_PER_ROUND') {
        settings.hideEmptyGenres = document.getElementById('hideEmptyGenres').checked;
    }

    // 서버로 전송
    try {
        const response = await fetch('/game/solo/host/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                players: players,
                totalRounds: totalRounds,
                gameMode: gameMode,
                settings: settings
            })
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = '/game/solo/host/play';
        } else {
            alert(result.message || '게임 시작에 실패했습니다.');
        }
    } catch (error) {
        alert('게임 시작 중 오류가 발생했습니다.');
        console.error(error);
    }
}

// Enter 키로 다음 입력으로 이동
document.addEventListener('keydown', function(e) {
    if (e.key === 'Enter' && e.target.classList.contains('player-name-input')) {
        const inputs = document.querySelectorAll('.player-name-input');
        const currentIndex = Array.from(inputs).indexOf(e.target);

        if (currentIndex < inputs.length - 1) {
            inputs[currentIndex + 1].focus();
        } else {
            startGame();
        }
    }
});