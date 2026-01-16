let currentRound = 1;
let currentSong = null;
let isPlaying = false;
let audioPlayer = document.getElementById('audioPlayer');
let progressInterval = null;
let score = 0;
let correctCount = 0;
let wrongCount = 0;
let skipCount = 0;
let actualTotalRounds = totalRounds;
let isRoundEnded = false;
let isRoundReady = false;
let totalPlayTime = 0;
let playStartTime = null;
let lastPossibleScore = 100;

// 점수 구간 정의
const SCORE_THRESHOLDS = [
    { maxTime: 5, score: 100, nextAt: 5 },
    { maxTime: 8, score: 90, nextAt: 8 },
    { maxTime: 12, score: 80, nextAt: 12 },
    { maxTime: 15, score: 70, nextAt: 15 },
    { maxTime: Infinity, score: 60, nextAt: null }
];
let youtubePlayerReady = false;
let videoReady = false;
let pendingAutoPlay = false;

// 게임 시작
document.addEventListener('DOMContentLoaded', async function() {
    // YouTube Player 초기화
    try {
        await YouTubePlayerManager.init('youtubePlayerContainer', {
            onStateChange: function(e) {
                console.log('YouTube 상태 변경:', e.data);

                if (e.data === 5) { // CUED
                    videoReady = true;
                    console.log('영상 로드 완료 (CUED)');

                    if (pendingAutoPlay && currentSong) {
                        console.log('자동 재생 시작');
                        pendingAutoPlay = false;
                        playAudio();
                    }
                } else if (e.data === 0) { // ENDED
                    pauseAudio();
                } else if (e.data === 1) { // PLAYING
                    videoReady = true;
                }
            },
            onError: function(e, errorInfo) {
                console.error('YouTube 재생 오류:', e.data);
                videoReady = false;
                pendingAutoPlay = false;
                if (currentSong && currentSong.filePath) {
                    console.log('MP3 fallback 시도');
                    currentSong.youtubeVideoId = null;
                    loadAudioSource();
                } else {
                    handlePlaybackError(errorInfo);
                }
            }
        });
        youtubePlayerReady = true;
    } catch (error) {
        console.warn('YouTube Player 초기화 실패, MP3 모드로 진행:', error);
    }

    // 레트로 모드는 항상 전체 랜덤
    loadRound(1);

    // 라운드 축소 알림 표시
    showReplacedSongsNotice();

    // 챌린지 모드 배너 표시
    initChallengeBanner();
});


// 챌린지 진행 상황 업데이트
function updateChallengeProgress(round) {
    const isChallengeMode = sessionStorage.getItem('challengeMode') === 'true';
    if (!isChallengeMode) return;

    const progressEl = document.getElementById('challengeProgress');
    if (progressEl) {
        progressEl.textContent = `${round - 1}/30 완료`;
    }
}

// 챌린지 모드 배너 초기화
function initChallengeBanner() {
    const isChallengeMode = sessionStorage.getItem('challengeMode') === 'true';
    const banner = document.getElementById('challengeBanner');

    if (isChallengeMode && totalRounds === 30) {
        banner.style.display = 'flex';
        updateChallengeProgress(1);
    }
}


// 라운드 축소 알림 표시
function showReplacedSongsNotice() {
    const roundsReducedJson = sessionStorage.getItem('roundsReduced');
    if (roundsReducedJson) {
        sessionStorage.removeItem('roundsReduced');

        const info = JSON.parse(roundsReducedJson);
        const notice = document.createElement('div');
        notice.className = 'rounds-reduced-notice';

        let rankingWarning = '';
        if (info.requested >= 10 && info.actual < 10) {
            rankingWarning = '<div class="ranking-warning">랭킹 등록 조건(10라운드) 미충족</div>';
        }

        notice.innerHTML = `
            <div class="notice-content">
                <span class="notice-icon">&#x26A0;</span>
                <div class="notice-text">
                    <div class="main-message">재생 불가 곡으로 인해 ${info.requested}라운드 → ${info.actual}라운드로 축소되었습니다</div>
                    ${rankingWarning}
                </div>
            </div>
        `;
        document.body.appendChild(notice);

        const duration = rankingWarning ? 5000 : 3500;
        setTimeout(() => {
            notice.classList.add('fade-out');
            setTimeout(() => notice.remove(), 500);
        }, duration);
    }
}

async function loadRound(roundNumber) {
    try {
        const response = await fetch(`/game/retro/round/${roundNumber}`);
        const result = await response.json();

        if (!result.success) {
            alert(result.message);
            return;
        }

        currentRound = roundNumber;
        currentSong = result.song;

        updateChallengeProgress(roundNumber);

        if (result.totalRounds) {
            actualTotalRounds = result.totalRounds;
        }

        document.getElementById('currentRound').textContent = roundNumber;

        resetUI();
        loadAudioSource();
        document.getElementById('answerInput').focus();

    } catch (error) {
        console.error('라운드 로딩 오류:', error);
        alert('라운드를 불러오는 중 오류가 발생했습니다.');
    }
}

function loadAudioSource() {
    if (!currentSong) return;

    const shouldAutoPlay = currentRound > 1;
    videoReady = false;
    pendingAutoPlay = false;

    if (currentSong.youtubeVideoId && youtubePlayerReady) {
        if (shouldAutoPlay) {
            console.log('자동 재생 시작 (라운드:', currentRound, ')');
            YouTubePlayerManager.loadAndPlay(currentSong.youtubeVideoId, currentSong.startTime || 0);
            isPlaying = true;
            document.getElementById('playBtn').innerHTML = '<span class="pause-icon">❚❚</span>';
            document.getElementById('musicIcon').textContent = '🎶';
            document.getElementById('musicIcon').classList.add('playing');
            document.getElementById('playerStatus').textContent = '재생 중...';
            playStartTime = Date.now();
            progressInterval = setInterval(updateProgress, 100);
        } else {
            YouTubePlayerManager.loadVideo(currentSong.youtubeVideoId, currentSong.startTime || 0);
        }
        updateTimeDisplay();
    } else if (currentSong.filePath) {
        audioPlayer.src = `/uploads/songs/${currentSong.filePath}`;
        audioPlayer.currentTime = 0;
        audioPlayer.onloadedmetadata = function() {
            updateTimeDisplay();
            if (shouldAutoPlay) {
                playAudio();
            }
        };
    }
}

function togglePlay() {
    if (!currentSong || (!currentSong.youtubeVideoId && !currentSong.filePath)) {
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
    if (currentSong.youtubeVideoId && youtubePlayerReady) {
        YouTubePlayerManager.play();
    } else {
        audioPlayer.play();
    }
    isPlaying = true;
    playStartTime = Date.now();

    document.getElementById('playBtn').innerHTML = '<span class="pause-icon">❚❚</span>';
    document.getElementById('musicIcon').textContent = '🎶';
    document.getElementById('musicIcon').classList.add('playing');
    document.getElementById('playerStatus').textContent = '재생 중...';

    progressInterval = setInterval(updateProgress, 100);
}

function pauseAudio() {
    if (currentSong && currentSong.youtubeVideoId && youtubePlayerReady) {
        YouTubePlayerManager.pause();
    } else {
        audioPlayer.pause();
    }
    isPlaying = false;

    if (playStartTime !== null) {
        totalPlayTime += (Date.now() - playStartTime) / 1000;
        playStartTime = null;
    }

    document.getElementById('playBtn').innerHTML = '<span class="play-icon">▶</span>';
    document.getElementById('musicIcon').textContent = '🎵';
    document.getElementById('musicIcon').classList.remove('playing');
    document.getElementById('playerStatus').textContent = '일시정지';

    clearInterval(progressInterval);
}

function stopAudio() {
    if (currentSong && currentSong.youtubeVideoId && youtubePlayerReady) {
        YouTubePlayerManager.stop();
    } else {
        audioPlayer.pause();
        audioPlayer.currentTime = 0;
    }
    isPlaying = false;

    if (playStartTime !== null) {
        totalPlayTime += (Date.now() - playStartTime) / 1000;
        playStartTime = null;
    }

    document.getElementById('playBtn').innerHTML = '<span class="play-icon">▶</span>';
    document.getElementById('musicIcon').textContent = '🎵';
    document.getElementById('musicIcon').classList.remove('playing');
    document.getElementById('playerStatus').textContent = '재생 대기중';
    document.getElementById('progressBar').style.width = '0%';

    clearInterval(progressInterval);
}

function updateProgress() {
    if (!currentSong) return;

    const duration = currentSong.playDuration || 10;
    let currentTime;

    if (currentSong.youtubeVideoId && youtubePlayerReady) {
        const startTime = currentSong.startTime || 0;
        currentTime = YouTubePlayerManager.getCurrentTime() - startTime;
    } else {
        currentTime = audioPlayer.currentTime;
    }

    currentTime = Math.max(0, currentTime);
    const progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    updateTimeDisplay();
    updateLiveScoreIndicator(currentTime);

    if (currentTime >= duration) {
        pauseAudio();
        disablePlayButton();
    }
}

function disablePlayButton() {
    const playBtn = document.getElementById('playBtn');
    if (playBtn) {
        playBtn.disabled = true;
        playBtn.innerHTML = '<span class="play-icon">▶</span>';
    }
    document.getElementById('playerStatus').textContent = '재생 완료';
}

function updateTimeDisplay() {
    const duration = currentSong ? (currentSong.playDuration || 10) : 0;
    let currentTime;

    if (currentSong && currentSong.youtubeVideoId && youtubePlayerReady) {
        const startTime = currentSong.startTime || 0;
        currentTime = Math.max(0, YouTubePlayerManager.getCurrentTime() - startTime);
    } else {
        currentTime = Math.max(0, audioPlayer.currentTime);
    }

    document.getElementById('currentTime').textContent = formatTime(Math.min(currentTime, duration));
    document.getElementById('totalTime').textContent = formatTime(duration);
}

function formatTime(seconds) {
    if (isNaN(seconds) || seconds === null || seconds === undefined) {
        return '0:00';
    }
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function getActualPlayTime() {
    if (!currentSong) return null;

    let currentTime = 0;

    if (currentSong.youtubeVideoId && youtubePlayerReady) {
        const startTime = currentSong.startTime || 0;
        currentTime = YouTubePlayerManager.getCurrentTime() - startTime;
    } else if (currentSong.filePath) {
        currentTime = audioPlayer.currentTime;
    } else {
        return null;
    }

    return Math.max(0, currentTime);
}

function resetUI() {
    stopAudio();
    isRoundEnded = false;
    isRoundReady = false;
    videoReady = false;
    pendingAutoPlay = false;
    totalPlayTime = 0;
    playStartTime = null;
    lastPossibleScore = 100;
    document.getElementById('answerInput').value = '';
    const playBtn = document.getElementById('playBtn');
    if (playBtn) {
        playBtn.disabled = false;
    }
    const feedbackEl = document.getElementById('attemptFeedback');
    if (feedbackEl) {
        feedbackEl.style.display = 'none';
        feedbackEl.textContent = '';
    }
    resetLiveScoreIndicator();
}

function confirmReady() {
    isRoundReady = true;
    const playBtn = document.getElementById('playBtn');
    const readyPrompt = document.getElementById('readyPrompt');

    if (playBtn) {
        playBtn.disabled = false;
    }
    if (readyPrompt) {
        readyPrompt.style.display = 'none';
    }

    document.getElementById('answerInput').focus();
}

async function submitAnswer() {
    if (isRoundEnded) return;

    const answerInput = document.getElementById('answerInput');
    const userAnswer = answerInput.value.trim();

    if (!userAnswer) {
        alert('정답을 입력해주세요.');
        answerInput.focus();
        return;
    }

    if (!currentSong) return;

    let answerTime = getActualPlayTime();
    if (answerTime === null || answerTime === 0) {
        answerTime = 0;
    }

    try {
        const response = await fetch('/game/retro/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundNumber: currentRound,
                answer: userAnswer,
                isSkip: false,
                answerTime: answerTime
            })
        });

        const result = await response.json();

        if (result.success) {
            if (result.isRoundOver) {
                isRoundEnded = true;
                stopAudio();

                if (result.isCorrect) {
                    score = result.totalScore;
                    correctCount++;
                    document.getElementById('currentScore').textContent = score;
                    document.getElementById('correctCount').textContent = correctCount;
                } else {
                    wrongCount++;
                    document.getElementById('wrongCount').textContent = wrongCount;
                }

                showAnswerModal(result.isCorrect, userAnswer, result.answer, result.isGameOver, false, result.earnedScore, result.answerTime);
            } else {
                showAttemptFeedback(result.remainingAttempts, userAnswer);
                answerInput.value = '';
                answerInput.focus();
            }
        } else {
            alert(result.message);
        }
    } catch (error) {
        console.error('답변 제출 오류:', error);
    }
}

function showAttemptFeedback(remaining, wrongAnswer) {
    let feedbackEl = document.getElementById('attemptFeedback');
    if (!feedbackEl) {
        feedbackEl = document.createElement('div');
        feedbackEl.id = 'attemptFeedback';
        feedbackEl.className = 'attempt-feedback';
        document.querySelector('.answer-input-wrapper').after(feedbackEl);
    }

    feedbackEl.innerHTML = `❌ 오답입니다! 남은 기회: <strong>${remaining}회</strong>`;
    feedbackEl.style.display = 'block';
    feedbackEl.classList.add('shake');
    setTimeout(() => feedbackEl.classList.remove('shake'), 500);
}

async function skipRound() {
    if (!currentSong) return;
    if (isRoundEnded) return;

    isRoundEnded = true;
    stopAudio();

    try {
        const response = await fetch('/game/retro/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundNumber: currentRound,
                answer: null,
                isSkip: true
            })
        });

        const result = await response.json();

        if (result.success) {
            skipCount++;
            document.getElementById('skipCount').textContent = skipCount;
            showAnswerModal(false, null, result.answer, result.isGameOver, true);
        } else {
            isRoundEnded = false;
            alert(result.message);
        }
    } catch (error) {
        isRoundEnded = false;
        console.error('스킵 오류:', error);
    }
}

function showAnswerModal(isCorrect, userAnswer, answerInfo, isGameOver, isSkip = false, earnedScore = 0, answerTime = null) {
    const modal = document.getElementById('answerModal');
    const header = document.getElementById('answerHeader');
    const userAnswerInfo = document.getElementById('userAnswerInfo');
    const nextBtn = document.getElementById('nextRoundBtn');

    if (isSkip) {
        header.textContent = '⏭ 스킵';
        header.className = 'answer-header skip';
        userAnswerInfo.innerHTML = '';
    } else if (isCorrect) {
        header.textContent = '🎉 정답!';
        header.className = 'answer-header correct';
        let timeText = answerTime !== null ? answerTime.toFixed(1) + '초' : '';
        userAnswerInfo.innerHTML = `
            <span class="attempt-info">${timeText}만에 정답!</span>
            <span class="correct-text">+${earnedScore}점!</span>
        `;
    } else {
        header.textContent = '❌ 오답';
        header.className = 'answer-header wrong';
        userAnswerInfo.innerHTML = `
            <span class="attempt-info">3번 모두 실패</span>
            <span class="wrong-text">내 마지막 답: ${userAnswer}</span>
        `;
    }

    document.getElementById('answerTitle').textContent = answerInfo.title;
    document.getElementById('answerArtist').textContent = answerInfo.artist;

    let meta = [];
    if (answerInfo.releaseYear) meta.push(answerInfo.releaseYear + '년');
    if (answerInfo.genre) meta.push(answerInfo.genre);
    document.getElementById('answerMeta').textContent = meta.join(' · ');

    if (isGameOver) {
        nextBtn.textContent = '결과 보기 🏆';
        nextBtn.onclick = function() { window.location.href = '/game/retro/result'; };
    } else {
        nextBtn.textContent = '다음 라운드 →';
        nextBtn.onclick = nextRound;
    }

    modal.classList.add('show');
}

function nextRound() {
    document.getElementById('answerModal').classList.remove('show');

    if (currentRound < actualTotalRounds) {
        loadRound(currentRound + 1);
    } else {
        window.location.href = '/game/retro/result';
    }
}

async function quitGame() {
    if (!confirm('정말 게임을 종료하시겠습니까?')) return;

    try {
        await fetch('/game/retro/end', { method: 'POST' });
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

// Enter 키로 제출
document.getElementById('answerInput').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        const modal = document.getElementById('answerModal');
        if (modal && modal.classList.contains('show')) {
            e.preventDefault();
            return;
        }
        if (isRoundEnded) {
            e.preventDefault();
            return;
        }
        submitAnswer();
    }
});

// 재생 실패 처리
function handlePlaybackError(errorInfo) {
    if (!currentSong) return;

    console.log('재생 실패 처리:', errorInfo);

    if (errorInfo && errorInfo.isPlaybackError) {
        reportUnplayableSong(currentSong.id, errorInfo.code);
        showPlaybackErrorModal(errorInfo);
    }
}

async function reportUnplayableSong(songId, errorCode) {
    try {
        await fetch('/api/song-report', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                songId: songId,
                reportType: 'UNPLAYABLE',
                description: '자동 신고: YouTube 에러 코드 ' + errorCode
            })
        });
        console.log('재생 불가 곡 자동 신고 완료');
    } catch (error) {
        console.error('자동 신고 실패:', error);
    }
}

function showPlaybackErrorModal(errorInfo) {
    let modal = document.getElementById('playbackErrorModal');
    if (modal) {
        modal.remove();
    }

    modal = document.createElement('div');
    modal.id = 'playbackErrorModal';
    modal.className = 'modal show';
    modal.innerHTML = `
        <div class="modal-content playback-error-modal">
            <div class="error-icon">⚠️</div>
            <h3>재생할 수 없는 곡입니다</h3>
            <p class="error-message">${errorInfo ? errorInfo.message : '알 수 없는 오류'}</p>
            <div class="auto-report-notice">
                <span class="auto-report-badge">✓ 자동 신고 완료</span>
                <p>관리자가 확인 후 조치합니다</p>
            </div>
            <div class="error-actions">
                <button class="btn-skip" onclick="skipUnplayableRound()">다음 곡으로 넘어가기</button>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
}

async function skipUnplayableRound() {
    const modal = document.getElementById('playbackErrorModal');
    if (modal) {
        modal.remove();
    }

    if (!currentSong) return;
    if (isRoundEnded) return;

    isRoundEnded = true;
    stopAudio();

    try {
        const response = await fetch('/game/retro/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundNumber: currentRound,
                answer: null,
                isSkip: true
            })
        });

        const result = await response.json();

        if (result.success) {
            skipCount++;
            document.getElementById('skipCount').textContent = skipCount;

            if (result.isGameOver) {
                window.location.href = '/game/retro/result';
            } else {
                loadRound(currentRound + 1);
            }
        } else {
            isRoundEnded = false;
            alert(result.message);
        }
    } catch (error) {
        isRoundEnded = false;
        console.error('스킵 오류:', error);
    }
}

// 실시간 점수 인디케이터
function calculatePossibleScore(playTime) {
    for (const threshold of SCORE_THRESHOLDS) {
        if (playTime < threshold.maxTime) {
            return threshold;
        }
    }
    return SCORE_THRESHOLDS[SCORE_THRESHOLDS.length - 1];
}

function updateLiveScoreIndicator(currentTime) {
    const indicator = document.querySelector('.live-score-indicator');
    const scoreValue = document.getElementById('possibleScore');
    const countdownTime = document.getElementById('nextDropTime');
    const segments = document.querySelectorAll('.score-segment');

    if (!indicator || !scoreValue) return;

    indicator.classList.remove('waiting');

    const currentThreshold = calculatePossibleScore(currentTime);
    const currentScore = currentThreshold.score;

    if (currentScore < lastPossibleScore) {
        scoreValue.classList.add('dropping');
        setTimeout(() => scoreValue.classList.remove('dropping'), 400);
        lastPossibleScore = currentScore;
    }

    scoreValue.textContent = currentScore;

    if (currentThreshold.nextAt !== null) {
        const remaining = Math.max(0, currentThreshold.nextAt - currentTime);
        countdownTime.textContent = remaining.toFixed(1);

        if (remaining < 2) {
            countdownTime.classList.add('urgent');
        } else {
            countdownTime.classList.remove('urgent');
        }
    } else {
        countdownTime.textContent = '-';
        countdownTime.classList.remove('urgent');
    }

    segments.forEach(seg => {
        const segScore = parseInt(seg.dataset.score);
        seg.classList.remove('active', 'passed');

        if (segScore === currentScore) {
            seg.classList.add('active');
        } else if (segScore > currentScore) {
            seg.classList.add('passed');
        }
    });
}

function resetLiveScoreIndicator() {
    const indicator = document.querySelector('.live-score-indicator');
    const scoreValue = document.getElementById('possibleScore');
    const countdownTime = document.getElementById('nextDropTime');
    const segments = document.querySelectorAll('.score-segment');

    if (!indicator) return;

    indicator.classList.add('waiting');

    if (scoreValue) {
        scoreValue.textContent = '100';
        scoreValue.classList.remove('dropping');
    }

    if (countdownTime) {
        countdownTime.textContent = '5.0';
        countdownTime.classList.remove('urgent');
    }

    segments.forEach((seg, index) => {
        seg.classList.remove('active', 'passed');
        if (index === 0) {
            seg.classList.add('active');
        }
    });
}
