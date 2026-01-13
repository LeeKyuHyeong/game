// 최고 팬 챌린지 게임 진행 JavaScript

const TIME_LIMIT_MS = 5000; // 5초
let currentRound = 1;
let totalRounds = 0;
let remainingLives = 3;
let correctCount = 0;
let currentSong = null;
let timerInterval = null;
let startTime = null;
let isPlaying = false;
let youtubePlayer = null;
let youtubePlayerReady = false;

// YouTube API 준비 완료 콜백
function onYouTubeIframeAPIReady() {
    youtubePlayer = new YT.Player('youtubePlayer', {
        height: '1',
        width: '1',
        playerVars: {
            'autoplay': 0,
            'controls': 0,
            'disablekb': 1,
            'modestbranding': 1,
            'rel': 0
        },
        events: {
            'onReady': onPlayerReady,
            'onStateChange': onPlayerStateChange
        }
    });
}

function onPlayerReady(event) {
    youtubePlayerReady = true;
}

function onPlayerStateChange(event) {
    if (event.data === YT.PlayerState.PLAYING && !isPlaying) {
        isPlaying = true;
        startTimer();
    }
}

document.addEventListener('DOMContentLoaded', function() {
    // 엔터 키로 정답 제출
    document.getElementById('answerInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            submitAnswer();
        }
    });

    // 초기 라운드 정보 로드
    loadRoundInfo();
});

async function loadRoundInfo() {
    try {
        const response = await fetch(`/game/fan-challenge/round/${currentRound}`);
        const data = await response.json();

        if (data.success) {
            totalRounds = data.totalRounds;
            remainingLives = data.remainingLives;
            correctCount = data.correctCount;
            currentSong = data.song;

            document.getElementById('totalRounds').textContent = totalRounds;
            updateUI();
        } else {
            alert(data.message || '라운드 정보를 불러올 수 없습니다');
            window.location.href = '/game/fan-challenge';
        }
    } catch (error) {
        console.error('라운드 정보 로드 오류:', error);
        alert('게임 정보를 불러올 수 없습니다');
        window.location.href = '/game/fan-challenge';
    }
}

function updateUI() {
    document.getElementById('currentRound').textContent = currentRound;
    document.getElementById('correctCount').textContent = correctCount;

    // 라이프 표시 업데이트
    for (let i = 1; i <= 3; i++) {
        const lifeEl = document.getElementById(`life${i}`);
        if (i <= remainingLives) {
            lifeEl.classList.add('active');
            lifeEl.classList.remove('lost');
        } else {
            lifeEl.classList.remove('active');
            lifeEl.classList.add('lost');
        }
    }
}

function startRound() {
    document.getElementById('readyScreen').style.display = 'none';
    document.getElementById('gameScreen').style.display = 'flex';
    document.getElementById('answerInput').value = '';
    document.getElementById('answerInput').focus();

    // 타이머 바 초기화
    document.getElementById('timerBar').style.width = '100%';
    document.getElementById('timerValue').textContent = '5.0';

    // 음악 재생
    playSong();
}

function playSong() {
    if (!currentSong) return;

    if (currentSong.youtubeVideoId) {
        playYouTube(currentSong.youtubeVideoId, currentSong.startTime || 0);
    } else if (currentSong.filePath) {
        playMP3(currentSong.filePath, currentSong.startTime || 0);
    }
}

function playYouTube(videoId, startTime) {
    if (youtubePlayer && youtubePlayerReady) {
        youtubePlayer.loadVideoById({
            videoId: videoId,
            startSeconds: startTime
        });
        // onPlayerStateChange에서 타이머 시작
    } else {
        // YouTube 플레이어가 준비되지 않은 경우 직접 시작
        setTimeout(() => {
            isPlaying = true;
            startTimer();
        }, 500);
    }
}

function playMP3(filePath, startTime) {
    const audio = document.getElementById('audioPlayer');
    audio.src = '/uploads/songs/' + filePath;
    audio.currentTime = startTime;
    audio.play().then(() => {
        isPlaying = true;
        startTimer();
    }).catch(error => {
        console.error('MP3 재생 오류:', error);
        isPlaying = true;
        startTimer();
    });
}

function startTimer() {
    startTime = Date.now();

    timerInterval = setInterval(() => {
        const elapsed = Date.now() - startTime;
        const remaining = Math.max(0, TIME_LIMIT_MS - elapsed);
        const seconds = (remaining / 1000).toFixed(1);

        document.getElementById('timerValue').textContent = seconds;
        document.getElementById('timerBar').style.width = (remaining / TIME_LIMIT_MS * 100) + '%';

        // 타이머 색상 변경
        const timerBar = document.getElementById('timerBar');
        if (remaining <= 1000) {
            timerBar.classList.add('critical');
        } else if (remaining <= 2000) {
            timerBar.classList.add('warning');
            timerBar.classList.remove('critical');
        } else {
            timerBar.classList.remove('warning', 'critical');
        }

        if (remaining <= 0) {
            handleTimeout();
        }
    }, 100);
}

function stopTimer() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
}

function stopMusic() {
    // YouTube 중지
    if (youtubePlayer && youtubePlayerReady) {
        try {
            youtubePlayer.stopVideo();
        } catch (e) {}
    }

    // MP3 중지
    const audio = document.getElementById('audioPlayer');
    audio.pause();
    audio.currentTime = 0;

    isPlaying = false;
}

async function submitAnswer() {
    if (!isPlaying) return;

    const answerInput = document.getElementById('answerInput');
    const answer = answerInput.value.trim();

    if (!answer) {
        answerInput.focus();
        return;
    }

    const answerTimeMs = Date.now() - startTime;

    stopTimer();
    stopMusic();

    try {
        const response = await fetch('/game/fan-challenge/answer', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                roundNumber: currentRound,
                answer: answer,
                answerTimeMs: answerTimeMs
            })
        });

        const result = await response.json();

        if (result.success) {
            showAnswerResult(result);
        } else {
            alert(result.message || '오류가 발생했습니다');
        }
    } catch (error) {
        console.error('정답 제출 오류:', error);
        alert('정답 제출 중 오류가 발생했습니다');
    }
}

async function handleTimeout() {
    stopTimer();
    stopMusic();

    try {
        const response = await fetch('/game/fan-challenge/timeout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                roundNumber: currentRound
            })
        });

        const result = await response.json();

        if (result.success) {
            showAnswerResult(result);
        }
    } catch (error) {
        console.error('시간 초과 처리 오류:', error);
    }
}

function showAnswerResult(result) {
    remainingLives = result.remainingLives;
    correctCount = result.correctCount;
    updateUI();

    const resultEl = document.getElementById('answerResult');
    const correctAnswerEl = document.getElementById('correctAnswer');

    if (result.isTimeout) {
        resultEl.innerHTML = '<span class="timeout">시간 초과!</span>';
        resultEl.className = 'answer-result timeout';
    } else if (result.isCorrect) {
        resultEl.innerHTML = '<span class="correct">정답!</span>';
        resultEl.className = 'answer-result correct';
    } else {
        resultEl.innerHTML = '<span class="wrong">오답!</span>';
        resultEl.className = 'answer-result wrong';
    }

    correctAnswerEl.textContent = result.correctAnswer;

    // 라이프 표시
    let livesHtml = '';
    for (let i = 0; i < 3; i++) {
        if (i < result.remainingLives) {
            livesHtml += '<span class="life active">&#10084;</span>';
        } else {
            livesHtml += '<span class="life lost">&#10084;</span>';
        }
    }
    document.getElementById('modalLives').innerHTML = livesHtml;
    document.getElementById('modalCorrect').textContent = result.correctCount;

    // 게임 오버 체크
    if (result.isGameOver) {
        document.getElementById('nextBtn').style.display = 'none';

        setTimeout(() => {
            document.getElementById('answerModal').style.display = 'none';
            showGameOver(result);
        }, 1500);
    } else {
        document.getElementById('nextBtn').style.display = 'block';
        document.getElementById('nextBtn').textContent =
            `다음 곡 (${result.completedRounds}/${result.totalRounds})`;
    }

    document.getElementById('answerModal').style.display = 'flex';
}

function showGameOver(result) {
    const titleEl = document.getElementById('gameOverTitle');
    const messageEl = document.getElementById('gameOverMessage');

    if (result.gameOverReason === 'PERFECT_CLEAR') {
        titleEl.innerHTML = '&#127942; PERFECT CLEAR! &#127942;';
        titleEl.className = 'gameover-title perfect';
        messageEl.textContent = `${artist}의 모든 곡을 맞췄습니다!`;
    } else {
        titleEl.innerHTML = '&#128148; GAME OVER';
        titleEl.className = 'gameover-title failed';
        messageEl.textContent = `${result.correctCount}/${result.totalRounds}곡 정답`;
    }

    document.getElementById('gameOverModal').style.display = 'flex';
}

async function nextRound() {
    document.getElementById('answerModal').style.display = 'none';
    document.getElementById('gameScreen').style.display = 'none';
    document.getElementById('readyScreen').style.display = 'flex';

    currentRound++;
    await loadRoundInfo();
}

function goToResult() {
    window.location.href = '/game/fan-challenge/result';
}

async function quitGame() {
    if (!confirm('정말 포기하시겠습니까?')) return;

    stopTimer();
    stopMusic();

    try {
        await fetch('/game/fan-challenge/end', { method: 'POST' });
    } catch (e) {}

    window.location.href = '/game/fan-challenge';
}
