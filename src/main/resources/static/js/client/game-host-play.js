let currentRound = 1;
let currentSong = null;
let isPlaying = false;
let audioPlayer = document.getElementById('audioPlayer');
let progressInterval = null;
let playerScores = {};

// 초기화
players.forEach(player => {
    playerScores[player] = 0;
});

// 게임 시작
document.addEventListener('DOMContentLoaded', function() {
    loadRound(1);
});

async function loadRound(roundNumber) {
    try {
        const response = await fetch(`/game/solo/host/round/${roundNumber}`);
        const result = await response.json();

        if (!result.success) {
            alert(result.message);
            return;
        }

        currentRound = roundNumber;
        currentSong = result.song;

        document.getElementById('currentRound').textContent = roundNumber;

        // 오디오 설정
        if (currentSong && currentSong.filePath) {
            audioPlayer.src = `/uploads/songs/${currentSong.filePath}`;
            audioPlayer.currentTime = currentSong.startTime || 0;

            audioPlayer.onloadedmetadata = function() {
                updateTimeDisplay();
            };
        }

        // UI 리셋
        resetPlayerUI();

    } catch (error) {
        console.error('라운드 로딩 오류:', error);
        alert('라운드를 불러오는 중 오류가 발생했습니다.');
    }
}

function togglePlay() {
    if (!currentSong || !currentSong.filePath) {
        alert('재생할 노래가 없습니다.');
        return;
    }

    if (isPlaying) {
        pauseAudio();
    } else {
        playAudio();
    }
}

function playAudio() {
    audioPlayer.play();
    isPlaying = true;

    document.getElementById('playBtn').innerHTML = '<span class="pause-icon">❚❚</span>';
    document.getElementById('musicIcon').textContent = '🎶';
    document.getElementById('musicIcon').classList.add('playing');
    document.getElementById('playerStatus').textContent = '재생 중...';

    // 프로그레스 바 업데이트
    progressInterval = setInterval(updateProgress, 100);
}

function pauseAudio() {
    audioPlayer.pause();
    isPlaying = false;

    document.getElementById('playBtn').innerHTML = '<span class="play-icon">▶</span>';
    document.getElementById('musicIcon').textContent = '🎵';
    document.getElementById('musicIcon').classList.remove('playing');
    document.getElementById('playerStatus').textContent = '일시정지';

    clearInterval(progressInterval);
}

function stopAudio() {
    audioPlayer.pause();
    audioPlayer.currentTime = currentSong ? (currentSong.startTime || 0) : 0;
    isPlaying = false;

    document.getElementById('playBtn').innerHTML = '<span class="play-icon">▶</span>';
    document.getElementById('musicIcon').textContent = '🎵';
    document.getElementById('musicIcon').classList.remove('playing');
    document.getElementById('playerStatus').textContent = '재생 대기중';
    document.getElementById('progressBar').style.width = '0%';

    clearInterval(progressInterval);
}

function updateProgress() {
    if (!currentSong) return;

    const startTime = currentSong.startTime || 0;
    const duration = currentSong.playDuration || 10;
    const currentTime = audioPlayer.currentTime - startTime;
    const progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    updateTimeDisplay();

    // 재생 시간 초과 시 자동 정지
    if (currentTime >= duration) {
        pauseAudio();
    }
}

function updateTimeDisplay() {
    const startTime = currentSong ? (currentSong.startTime || 0) : 0;
    const duration = currentSong ? (currentSong.playDuration || 10) : 0;
    const currentTime = Math.max(0, audioPlayer.currentTime - startTime);

    document.getElementById('currentTime').textContent = formatTime(Math.min(currentTime, duration));
    document.getElementById('totalTime').textContent = formatTime(duration);
}

function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function resetPlayerUI() {
    stopAudio();
    document.querySelectorAll('.player-btn').forEach(btn => {
        btn.classList.remove('selected');
    });
}

async function selectWinner(playerName) {
    if (!currentSong) return;

    // 버튼 하이라이트
    document.querySelectorAll('.player-btn').forEach(btn => {
        if (btn.textContent === playerName) {
            btn.classList.add('selected');
        }
    });

    stopAudio();

    // 서버에 결과 전송
    try {
        const response = await fetch('/game/solo/host/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundNumber: currentRound,
                winner: playerName,
                isSkip: false
            })
        });

        const result = await response.json();

        if (result.success) {
            // 점수 업데이트
            playerScores[playerName] = (playerScores[playerName] || 0) + 100;
            updateScoreboard();

            // 정답 모달 표시
            showAnswerModal(playerName, result.isGameOver);
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('답변 제출 오류:', error);
    }
}

async function skipRound() {
    if (!currentSong) return;

    stopAudio();

    try {
        const response = await fetch('/game/solo/host/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundNumber: currentRound,
                winner: null,
                isSkip: true
            })
        });

        const result = await response.json();

        if (result.success) {
            showAnswerModal(null, result.isGameOver);
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('스킵 오류:', error);
    }
}

function showAnswerModal(winner, isGameOver) {
    const modal = document.getElementById('answerModal');
    const header = document.getElementById('answerHeader');
    const winnerInfo = document.getElementById('winnerInfo');
    const nextBtn = document.getElementById('nextRoundBtn');

    if (winner) {
        header.textContent = '🎉 정답!';
        header.className = 'answer-header correct';
        winnerInfo.innerHTML = `<span class="winner-name">${winner}</span> 정답! +100점`;
    } else {
        header.textContent = '⏭ 스킵';
        header.className = 'answer-header skip';
        winnerInfo.innerHTML = '아쉽게도 스킵되었습니다.';
    }

    // 노래 정보 표시
    document.getElementById('answerTitle').textContent = currentSong.title;
    document.getElementById('answerArtist').textContent = currentSong.artist;

    let meta = [];
    if (currentSong.releaseYear) meta.push(currentSong.releaseYear + '년');
    if (currentSong.genre) meta.push(currentSong.genre);
    document.getElementById('answerMeta').textContent = meta.join(' · ');

    // 버튼 텍스트
    if (isGameOver) {
        nextBtn.textContent = '결과 보기 🏆';
        nextBtn.onclick = function() { window.location.href = '/game/solo/host/result'; };
    } else {
        nextBtn.textContent = '다음 라운드 →';
        nextBtn.onclick = nextRound;
    }

    modal.classList.add('show');
}

function nextRound() {
    document.getElementById('answerModal').classList.remove('show');

    if (currentRound < totalRounds) {
        loadRound(currentRound + 1);
    } else {
        window.location.href = '/game/solo/host/result';
    }
}

function updateScoreboard() {
    const scoreList = document.getElementById('scoreList');

    // 점수순 정렬
    const sorted = Object.entries(playerScores).sort((a, b) => b[1] - a[1]);

    sorted.forEach(([player, score], index) => {
        const item = scoreList.querySelector(`[data-player="${player}"]`);
        if (item) {
            item.querySelector('.player-score').textContent = score;
            item.style.order = index;
        }
    });
}

async function quitGame() {
    if (!confirm('정말 게임을 종료하시겠습니까?')) return;

    try {
        await fetch('/game/solo/host/end', { method: 'POST' });
        window.location.href = '/';
    } catch (error) {
        window.location.href = '/';
    }
}

// 오디오 이벤트
audioPlayer.addEventListener('ended', function() {
    pauseAudio();
});

audioPlayer.addEventListener('error', function() {
    alert('오디오 파일을 재생할 수 없습니다.');
    pauseAudio();
});