let currentRound = 0;
let currentPhase = null;
let currentSong = null;
let isPlaying = false;
let audioPlayer = document.getElementById('audioPlayer');
let progressInterval = null;
let pollingInterval = null;
let hasSubmitted = false;

// 오디오 동기화 상태
let lastAudioPlaying = false;
let lastAudioPlayedAt = null;

// 페이지 로드 시 시작
document.addEventListener('DOMContentLoaded', function() {
    startPolling();
});

// 페이지 떠날 때 정리
window.addEventListener('beforeunload', function() {
    stopPolling();
});

// 폴링 시작
function startPolling() {
    fetchRoundInfo();
    pollingInterval = setInterval(fetchRoundInfo, 1000);
}

// 폴링 중지
function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

// 라운드 정보 조회
async function fetchRoundInfo() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/round`);
        const result = await response.json();

        if (!result.success) {
            console.error('라운드 정보 조회 실패:', result.message);
            return;
        }

        // 게임 종료 체크
        if (result.status === 'FINISHED') {
            stopPolling();
            window.location.href = `/game/multi/room/${roomCode}/result`;
            return;
        }

        // 라운드 변경 감지
        if (result.currentRound !== currentRound) {
            currentRound = result.currentRound;
            document.getElementById('currentRound').textContent = currentRound;
            hasSubmitted = false;
            resetAnswerUI();
            lastAudioPlaying = false;
            lastAudioPlayedAt = null;
        }

        // 페이즈 변경 감지
        if (result.roundPhase !== currentPhase) {
            currentPhase = result.roundPhase;
            updatePhaseUI(result);
        }

        // 노래 정보 업데이트
        if (result.song && (!currentSong || currentSong.id !== result.song.id)) {
            currentSong = result.song;
            loadSong(currentSong);
        }

        // 오디오 동기화
        syncAudio(result.audioPlaying, result.audioPlayedAt);

        // 스코어보드 업데이트
        updateScoreboard(result.participants);

        // 결과 단계일 때 정답 표시
        if (currentPhase === 'RESULT' && result.answer) {
            showAnswer(result.answer);
            showRoundResultsUI(result.participants);
        }

        // 방장용: 모두 답변했는지 표시
        if (isHost && currentPhase === 'PLAYING') {
            updateHostControls(result.participants);
        }

    } catch (error) {
        console.error('라운드 정보 조회 오류:', error);
    }
}

// 오디오 동기화
function syncAudio(serverPlaying, serverPlayedAt) {
    if (serverPlaying === lastAudioPlaying && serverPlayedAt === lastAudioPlayedAt) {
        return;
    }

    lastAudioPlaying = serverPlaying;
    lastAudioPlayedAt = serverPlayedAt;

    if (serverPlaying && serverPlayedAt) {
        const elapsedMs = Date.now() - serverPlayedAt;
        const elapsedSec = elapsedMs / 1000;
        const startTime = currentSong ? (currentSong.startTime || 0) : 0;

        audioPlayer.currentTime = startTime + elapsedSec;

        if (!isPlaying) {
            audioPlayer.play().catch(e => console.log('자동 재생 실패:', e));
            isPlaying = true;
            updatePlayingUI(true);
        }

        const syncStatus = document.getElementById('syncStatus');
        if (syncStatus) {
            syncStatus.textContent = '🎶 노래 재생 중...';
        }
    } else {
        if (isPlaying) {
            audioPlayer.pause();
            isPlaying = false;
            updatePlayingUI(false);
        }

        const syncStatus = document.getElementById('syncStatus');
        if (syncStatus) {
            syncStatus.textContent = '🎧 방장이 재생을 시작하면 노래가 들립니다';
        }
    }
}

// 재생 UI 업데이트
function updatePlayingUI(playing) {
    const playBtn = document.getElementById('playBtn');
    const musicIcon = document.getElementById('musicIcon');
    const playerStatus = document.getElementById('playerStatus');

    if (playing) {
        if (playBtn) playBtn.innerHTML = '<span class="pause-icon">❚❚</span>';
        if (musicIcon) {
            musicIcon.textContent = '🎶';
            musicIcon.classList.add('playing');
        }
        if (playerStatus) playerStatus.textContent = '재생 중...';
        startProgressUpdate();
    } else {
        if (playBtn) playBtn.innerHTML = '<span class="play-icon">▶</span>';
        if (musicIcon) {
            musicIcon.textContent = '🎵';
            musicIcon.classList.remove('playing');
        }
        if (playerStatus) playerStatus.textContent = '일시정지';
        stopProgressUpdate();
    }
}

function startProgressUpdate() {
    stopProgressUpdate();
    progressInterval = setInterval(updateProgress, 100);
}

function stopProgressUpdate() {
    if (progressInterval) {
        clearInterval(progressInterval);
        progressInterval = null;
    }
}

// 페이즈 UI 업데이트
function updatePhaseUI(result) {
    document.getElementById('genreSelectPhase').style.display = 'none';
    document.getElementById('playingPhase').style.display = 'none';
    document.getElementById('resultPhase').style.display = 'none';

    switch (currentPhase) {
        case 'GENRE_SELECT':
            document.getElementById('genreSelectPhase').style.display = 'block';
            if (isHost) {
                document.getElementById('genreSelectDesc').textContent = '다음 라운드의 장르를 선택하세요';
                loadGenres();
            } else {
                document.getElementById('genreSelectDesc').textContent = '방장이 장르를 선택중입니다...';
                document.getElementById('genreGrid').innerHTML = '<div class="waiting-host">⏳ 잠시만 기다려주세요...</div>';
            }
            break;

        case 'PLAYING':
            document.getElementById('playingPhase').style.display = 'block';
            break;

        case 'RESULT':
            document.getElementById('resultPhase').style.display = 'block';
            stopProgressUpdate();
            break;
    }
}

// 장르 목록 로드
async function loadGenres() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/genres`);
        const result = await response.json();

        if (!result.success) {
            console.error('장르 목록 로드 실패:', result.message);
            return;
        }

        const genreGrid = document.getElementById('genreGrid');
        genreGrid.innerHTML = '';

        result.genres.forEach(genre => {
            const item = document.createElement('div');
            item.className = 'genre-item';
            if (genre.availableCount === 0) {
                item.classList.add('disabled');
            }
            item.innerHTML = `
                <span class="genre-name">${escapeHtml(genre.name)}</span>
                <span class="genre-count">${genre.availableCount}곡</span>
            `;

            if (genre.availableCount > 0) {
                item.dataset.genreId = genre.id;
                item.addEventListener('click', function() {
                    selectGenre(this.dataset.genreId);
                });
            }

            genreGrid.appendChild(item);
        });

    } catch (error) {
        console.error('장르 목록 로드 오류:', error);
    }
}

// 장르 선택
async function selectGenre(genreId) {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/select-genre`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ genreId: parseInt(genreId) })
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '장르 선택에 실패했습니다.');
        }

    } catch (error) {
        console.error('장르 선택 오류:', error);
    }
}

// 노래 로드
function loadSong(song) {
    if (song && song.filePath) {
        audioPlayer.src = `/uploads/songs/${song.filePath}`;
        audioPlayer.currentTime = song.startTime || 0;
        resetAudioUI();
    }
}

// 오디오 UI 초기화
function resetAudioUI() {
    if (isPlaying) {
        audioPlayer.pause();
        isPlaying = false;
    }
    document.getElementById('progressBar').style.width = '0%';
    updatePlayingUI(false);
    updateTimeDisplay();
}

// 방장용: 재생/일시정지 토글
async function hostTogglePlay() {
    if (!currentSong || !currentSong.filePath) {
        alert('재생할 노래가 없습니다.');
        return;
    }

    try {
        const endpoint = isPlaying ? 'pause' : 'play';
        const response = await fetch(`/game/multi/room/${roomCode}/audio/${endpoint}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '오디오 컨트롤에 실패했습니다.');
        }

    } catch (error) {
        console.error('오디오 컨트롤 오류:', error);
    }
}

function updateProgress() {
    if (!currentSong) return;

    const startTime = currentSong.startTime || 0;
    const duration = currentSong.playDuration || 10;
    const currentTime = audioPlayer.currentTime - startTime;
    const progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    updateTimeDisplay();

    if (currentTime >= duration && isHost && isPlaying) {
        hostTogglePlay();
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

// 답변 제출
async function submitAnswer() {
    if (hasSubmitted) {
        alert('이미 답변을 제출했습니다.');
        return;
    }

    const answerInput = document.getElementById('answerInput');
    const answer = answerInput.value.trim();

    if (!answer) {
        alert('정답을 입력해주세요.');
        answerInput.focus();
        return;
    }

    try {
        const response = await fetch(`/game/multi/room/${roomCode}/answer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ answer: answer })
        });

        const result = await response.json();

        if (result.success) {
            hasSubmitted = true;
            document.getElementById('submitBtn').disabled = true;
            document.getElementById('answerInput').disabled = true;

            const statusEl = document.getElementById('answerStatus');
            if (result.isCorrect) {
                statusEl.innerHTML = '<span class="correct">🎉 정답! +' + result.earnedScore + '점</span>';
            } else {
                statusEl.innerHTML = '<span class="wrong">❌ 오답입니다</span>';
            }
            statusEl.style.display = 'block';
        } else {
            alert(result.message || '답변 제출에 실패했습니다.');
        }

    } catch (error) {
        console.error('답변 제출 오류:', error);
    }
}

// 답변 UI 초기화
function resetAnswerUI() {
    document.getElementById('answerInput').value = '';
    document.getElementById('answerInput').disabled = false;
    document.getElementById('submitBtn').disabled = false;
    document.getElementById('answerStatus').style.display = 'none';
}

// 방장용: 호스트 컨트롤 업데이트
function updateHostControls(participants) {
    const allAnswered = participants.every(function(p) { return p.hasAnswered; });
    const showResultBtn = document.getElementById('showResultBtn');

    if (showResultBtn) {
        if (allAnswered) {
            showResultBtn.textContent = '🎉 정답 공개';
            showResultBtn.classList.add('all-answered');
        } else {
            const answeredCount = participants.filter(function(p) { return p.hasAnswered; }).length;
            showResultBtn.textContent = '정답 공개 (' + answeredCount + '/' + participants.length + ')';
            showResultBtn.classList.remove('all-answered');
        }
    }
}

// 라운드 결과 공개
async function showRoundResult() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/show-result`, {
            method: 'POST'
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '결과 공개에 실패했습니다.');
        }

    } catch (error) {
        console.error('결과 공개 오류:', error);
    }
}

// 다음 라운드
async function nextRound() {
    try {
        const response = await fetch(`/game/multi/room/${roomCode}/next-round`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            if (result.isGameOver) {
                window.location.href = `/game/multi/room/${roomCode}/result`;
            }
        } else {
            alert(result.message || '다음 라운드 진행에 실패했습니다.');
        }

    } catch (error) {
        console.error('다음 라운드 오류:', error);
    }
}

// 정답 표시
function showAnswer(answer) {
    document.getElementById('answerTitle').textContent = answer.title;
    document.getElementById('answerArtist').textContent = answer.artist;

    var meta = [];
    if (answer.releaseYear) meta.push(answer.releaseYear + '년');
    if (answer.genre) meta.push(answer.genre);
    document.getElementById('answerMeta').textContent = meta.join(' · ');
}

// 라운드 결과 UI 표시
function showRoundResultsUI(participants) {
    const container = document.getElementById('roundResults');

    const sorted = [...participants].sort(function(a, b) {
        if (a.currentRoundCorrect && !b.currentRoundCorrect) return -1;
        if (!a.currentRoundCorrect && b.currentRoundCorrect) return 1;
        return b.currentRoundScore - a.currentRoundScore;
    });

    var html = '';
    sorted.forEach(function(p) {
        var correctClass = p.currentRoundCorrect ? 'correct' : 'wrong';
        var hostBadge = p.isHost ? '<span class="host-badge">👑</span>' : '';
        var answerText = p.hasAnswered
            ? '<span class="answer-text">' + escapeHtml(p.currentAnswer || '-') + '</span>'
            : '<span class="no-answer">미제출</span>';
        var scoreText = p.currentRoundCorrect
            ? '<span class="score-plus">+' + p.currentRoundScore + '</span>'
            : '<span class="score-zero">0</span>';

        html += '<div class="result-item ' + correctClass + '">' +
            '<div class="result-player">' +
                '<span class="player-name">' + escapeHtml(p.nickname) + '</span>' +
                hostBadge +
            '</div>' +
            '<div class="result-answer">' + answerText + '</div>' +
            '<div class="result-score">' + scoreText + '</div>' +
        '</div>';
    });

    container.innerHTML = html;
}

// 스코어보드 업데이트
function updateScoreboard(participants) {
    const container = document.getElementById('scoreList');

    const sorted = [...participants].sort(function(a, b) { return b.score - a.score; });

    var html = '';
    sorted.forEach(function(p, index) {
        var meClass = p.memberId === myMemberId ? 'me' : '';
        var hostIcon = p.isHost ? '👑 ' : '';
        var meBadge = p.memberId === myMemberId ? '<span class="me-badge">(나)</span>' : '';
        var answerIndicator = currentPhase === 'PLAYING'
            ? '<span class="answer-indicator ' + (p.hasAnswered ? 'answered' : '') + '">' + (p.hasAnswered ? '✓' : '...') + '</span>'
            : '';

        html += '<div class="score-item ' + meClass + '" data-member-id="' + p.memberId + '">' +
            '<span class="rank">' + (index + 1) + '</span>' +
            '<span class="player-name">' + hostIcon + escapeHtml(p.nickname) + meBadge + '</span>' +
            '<span class="player-score">' + p.score + '점</span>' +
            answerIndicator +
        '</div>';
    });

    container.innerHTML = html;
}

// 게임 나가기
async function quitGame() {
    if (!confirm('정말 게임을 나가시겠습니까?')) return;

    try {
        await fetch(`/game/multi/room/${roomCode}/leave`, { method: 'POST' });
        window.location.href = '/game/multi';
    } catch (error) {
        window.location.href = '/game/multi';
    }
}

// 오디오 이벤트
audioPlayer.addEventListener('ended', function() {
    isPlaying = false;
    updatePlayingUI(false);
});

audioPlayer.addEventListener('error', function() {
    console.error('오디오 파일을 재생할 수 없습니다.');
    isPlaying = false;
    updatePlayingUI(false);
});

// Enter 키로 제출
document.getElementById('answerInput').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        submitAnswer();
    }
});

// HTML 이스케이프
function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}