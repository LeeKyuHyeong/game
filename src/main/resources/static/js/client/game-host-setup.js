let playerCount = 2;
let totalRounds = 10;

// 라운드 버튼 이벤트
document.querySelectorAll('.round-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        document.querySelectorAll('.round-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        totalRounds = parseInt(this.dataset.value);
    });
});

// 게임 모드 변경 이벤트
document.querySelectorAll('input[name="gameMode"]').forEach(radio => {
    radio.addEventListener('change', function() {
        const genreSelect = document.getElementById('genreSelect');
        if (this.value === 'FIXED_GENRE') {
            genreSelect.style.display = 'block';
        } else {
            genreSelect.style.display = 'none';
        }
    });
});

function changePlayerCount(delta) {
    playerCount = Math.max(2, Math.min(10, playerCount + delta));
    document.getElementById('playerCount').textContent = playerCount;
}

function goToStep1() {
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step1').style.display = 'block';
}

function goToStep2() {
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

    // 장르 체크
    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            return;
        }
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