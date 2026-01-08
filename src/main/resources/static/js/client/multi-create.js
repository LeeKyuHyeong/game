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

// 방 만들기
async function createRoom() {
    const roomName = document.getElementById('roomName').value.trim();

    if (!roomName) {
        alert('방 이름을 입력해주세요.');
        document.getElementById('roomName').focus();
        return;
    }

    if (roomName.length < 2) {
        alert('방 이름은 2자 이상 입력해주세요.');
        document.getElementById('roomName').focus();
        return;
    }

    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    // FIXED_GENRE 모드면 장르 선택 확인
    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            return;
        }
    }

    const maxPlayers = parseInt(document.getElementById('maxPlayers').value);
    const totalRounds = parseInt(document.getElementById('totalRounds').value);
    const isPrivate = document.getElementById('isPrivate').checked;

    // 설정 구성
    const settings = {
        gameMode: gameMode
    };

    if (gameMode === 'FIXED_GENRE') {
        settings.fixedGenreId = parseInt(document.getElementById('fixedGenreId').value);
    }

    try {
        const response = await fetch('/game/multi/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roomName: roomName,
                maxPlayers: maxPlayers,
                totalRounds: totalRounds,
                isPrivate: isPrivate,
                settings: settings
            })
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = `/game/multi/room/${result.roomCode}`;
        } else {
            alert(result.message || '방 생성에 실패했습니다.');
        }
    } catch (error) {
        console.error('방 생성 오류:', error);
        alert('방 생성 중 오류가 발생했습니다.');
    }
}

// Enter 키로 방 만들기
document.getElementById('roomName').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        createRoom();
    }
});