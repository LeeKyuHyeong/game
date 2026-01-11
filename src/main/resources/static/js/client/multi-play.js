// 게임 상태
let currentRound = 0;
let currentPhase = null;  // null, PLAYING, RESULT
let currentSong = null;
let isPlaying = false;

// DOM 요소
const audioPlayer = document.getElementById('audioPlayer');

// 폴링 관련
let roundPollingInterval = null;
let chatPollingInterval = null;
let progressInterval = null;
let lastChatId = 0;

// 오디오 동기화
let lastAudioPlaying = false;
let lastAudioPlayedAt = null;

// 페이지 로드 시 시작
document.addEventListener('DOMContentLoaded', function() {
    startPolling();
    
    // Enter 키로 채팅 전송
    document.getElementById('chatInput').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            sendChat();
        }
    });
});

// 페이지 떠날 때 정리
window.addEventListener('beforeunload', function() {
    stopPolling();
});

// ========== 폴링 ==========

function startPolling() {
    fetchRoundInfo();
    fetchChats();
    roundPollingInterval = setInterval(fetchRoundInfo, 1000);
    chatPollingInterval = setInterval(fetchChats, 500);  // 채팅은 더 빠르게
}

function stopPolling() {
    if (roundPollingInterval) {
        clearInterval(roundPollingInterval);
        roundPollingInterval = null;
    }
    if (chatPollingInterval) {
        clearInterval(chatPollingInterval);
        chatPollingInterval = null;
    }
    stopProgressUpdate();
}

// ========== 라운드 정보 조회 ==========

async function fetchRoundInfo() {
    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/round');
        const result = await response.json();

        if (!result.success) {
            console.error('라운드 정보 조회 실패:', result.message);
            return;
        }

        // 게임 종료 체크
        if (result.status === 'FINISHED') {
            stopPolling();
            window.location.href = '/game/multi/room/' + roomCode + '/result';
            return;
        }

        // 라운드 변경 감지
        if (result.currentRound !== currentRound) {
            currentRound = result.currentRound;
            document.getElementById('currentRound').textContent = currentRound;
        }

        // 페이즈 변경 감지
        var newPhase = result.roundPhase;
        if (newPhase !== currentPhase) {
            currentPhase = newPhase;
            updatePhaseUI();
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

        // 결과 단계일 때 정답/정답자 표시
        if (currentPhase === 'RESULT') {
            if (result.answer) {
                showAnswer(result.answer);
            }
            if (result.winnerNickname) {
                showWinner(result.winnerNickname);
            }
        }

    } catch (error) {
        console.error('라운드 정보 조회 오류:', error);
    }
}

// ========== 페이즈 UI ==========

function updatePhaseUI() {
    document.getElementById('roundWaiting').style.display = 'none';
    document.getElementById('roundPlaying').style.display = 'none';
    document.getElementById('roundResult').style.display = 'none';

    if (currentPhase === 'PLAYING') {
        document.getElementById('roundPlaying').style.display = 'block';
        startProgressUpdate();
    } else if (currentPhase === 'RESULT') {
        document.getElementById('roundResult').style.display = 'block';
        stopProgressUpdate();

        // ★ 마지막 라운드면 버튼 텍스트 변경
        updateNextRoundButton();
    } else {
        // 대기 상태
        document.getElementById('roundWaiting').style.display = 'block';
        stopProgressUpdate();

        // 라운드 시작 버튼 상태 복원
        resetStartRoundButton();

        // 대기 메시지 업데이트
        var msg = currentRound === 0
            ? '방장이 라운드를 시작하면 노래가 재생됩니다'
            : '방장이 다음 라운드를 시작해주세요';
        document.getElementById('waitingMessage').textContent = msg;
    }
}

// ★ 버튼 상태 복원 함수 추가
function resetStartRoundButton() {
    var btn = document.getElementById('startRoundBtn');
    if (btn) {
        btn.disabled = false;
        btn.textContent = '🎵 라운드 시작';
    }
}

function updateNextRoundButton() {
    var btn = document.getElementById('nextRoundBtn');
    if (btn) {
        btn.disabled = false;
        // ★ 마지막 라운드면 "결과 보기"로 표시
        if (currentRound >= totalRounds) {
            btn.textContent = '🏆 결과 보기';
        } else {
            btn.textContent = '다음 라운드 →';
        }
    }
}

function resetNextRoundButton() {
    updateNextRoundButton();
}

// ========== 오디오 동기화 ==========

function syncAudio(serverPlaying, serverPlayedAt) {
    if (serverPlaying === lastAudioPlaying && serverPlayedAt === lastAudioPlayedAt) {
        return;
    }

    lastAudioPlaying = serverPlaying;
    lastAudioPlayedAt = serverPlayedAt;

    if (serverPlaying && serverPlayedAt && currentSong) {
        var elapsedMs = Date.now() - serverPlayedAt;
        var elapsedSec = elapsedMs / 1000;
        var startTime = currentSong.startTime || 0;

        var playDuration = currentSong.playDuration || 30;

        // 디버깅: 비정상적인 시간 차이 확인
        if (elapsedSec > 5) {
            console.warn('Audio sync warning:', {
                serverPlayedAt: serverPlayedAt,
                clientNow: Date.now(),
                elapsedSec: elapsedSec.toFixed(1),
                startTime: startTime,
                playDuration: playDuration
            });
        }

        // 음수이거나 재생 시간을 초과하면 처음부터 (시간 동기화 문제 방지)
        if (elapsedSec < 0 || elapsedSec > playDuration) {
            elapsedSec = 0;
        }

        audioPlayer.currentTime = startTime + elapsedSec;

        if (!isPlaying) {
            audioPlayer.play().catch(function(e) {
                console.log('자동 재생 실패:', e);
            });
            isPlaying = true;
        }
    } else {
        if (isPlaying) {
            audioPlayer.pause();
            isPlaying = false;
        }
    }
}

function loadSong(song) {
    if (song && song.filePath) {
        audioPlayer.src = '/uploads/songs/' + song.filePath;
        audioPlayer.currentTime = song.startTime || 0;
    }
}

// ========== 진행 바 ==========

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

function updateProgress() {
    if (!currentSong) return;

    var startTime = currentSong.startTime || 0;
    var duration = currentSong.playDuration || 10;
    var currentTime = audioPlayer.currentTime - startTime;
    var progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    document.getElementById('currentTime').textContent = formatTime(Math.min(currentTime, duration));
    document.getElementById('totalTime').textContent = formatTime(duration);
}

function formatTime(seconds) {
    var mins = Math.floor(seconds / 60);
    var secs = Math.floor(seconds % 60);
    return mins + ':' + secs.toString().padStart(2, '0');
}

// ========== 채팅 ==========

async function fetchChats() {
    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/chats?lastId=' + lastChatId);
        const result = await response.json();

        if (!result.success) return;

        var chats = result.chats;
        if (chats && chats.length > 0) {
            var container = document.getElementById('chatMessages');

            chats.forEach(function(chat) {
                appendChatMessage(chat);
                lastChatId = Math.max(lastChatId, chat.id);
            });

            // 스크롤 아래로
            container.scrollTop = container.scrollHeight;
        }

    } catch (error) {
        console.error('채팅 조회 오류:', error);
    }
}

function appendChatMessage(chat) {
    var container = document.getElementById('chatMessages');
    var div = document.createElement('div');

    var messageClass = 'chat-message';
    if (chat.messageType === 'CORRECT_ANSWER') {
        messageClass += ' correct-answer';
    } else if (chat.messageType === 'SYSTEM') {
        messageClass += ' system-message';
    } else if (chat.memberId === myMemberId) {
        messageClass += ' my-message';
    }

    div.className = messageClass;

    if (chat.messageType === 'SYSTEM') {
        div.innerHTML = '<span class="system-text">' + escapeHtml(chat.message) + '</span>';
    } else {
        var hostBadge = chat.isHost ? '<span class="host-badge">👑</span>' : '';
        div.innerHTML =
            '<span class="chat-nickname">' + hostBadge + escapeHtml(chat.nickname) + '</span>' +
            '<span class="chat-text">' + escapeHtml(chat.message) + '</span>';
    }

    container.appendChild(div);
}

async function sendChat() {
    var input = document.getElementById('chatInput');
    var message = input.value.trim();

    if (!message) return;

    input.value = '';
    input.focus();

    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message })
        });

        const result = await response.json();

        if (!result.success) {
            alert(result.message || '메시지 전송 실패');
        }
        // 정답이든 아니든 채팅 폴링에서 자동으로 표시됨

    } catch (error) {
        console.error('채팅 전송 오류:', error);
    }
}

// ========== 스코어보드 ==========

function updateScoreboard(participants) {
    var container = document.getElementById('scoreList');
    var sorted = participants.slice().sort(function(a, b) {
        return b.score - a.score;
    });

    var html = '';
    sorted.forEach(function(p, index) {
        var meClass = p.memberId === myMemberId ? 'me' : '';
        var hostIcon = p.isHost ? '👑 ' : '';
        var meBadge = p.memberId === myMemberId ? ' (나)' : '';

        html += '<div class="score-item ' + meClass + '">' +
            '<span class="rank">' + (index + 1) + '</span>' +
            '<span class="player-name">' + hostIcon + escapeHtml(p.nickname) + meBadge + '</span>' +
            '<span class="player-score">' + p.score + '</span>' +
        '</div>';
    });

    container.innerHTML = html;
}

// ========== 정답/정답자 표시 ==========

function showAnswer(answer) {
    document.getElementById('answerTitle').textContent = answer.title;
    document.getElementById('answerArtist').textContent = answer.artist;

    var meta = [];
    if (answer.releaseYear) meta.push(answer.releaseYear + '년');
    if (answer.genre) meta.push(answer.genre);
    document.getElementById('answerMeta').textContent = meta.join(' · ');
}

function showWinner(nickname) {
    document.getElementById('winnerName').textContent = nickname;
    document.getElementById('winnerInfo').style.display = 'flex';
}

// ========== 방장 컨트롤 ==========

async function startRound() {
    if (!isHost) return;

    var btn = document.getElementById('startRoundBtn');
    btn.disabled = true;
    btn.textContent = '시작 중...';

    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/start-round', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            if (result.isGameOver) {
                window.location.href = '/game/multi/room/' + roomCode + '/result';
            }
            // ★ 성공 시에도 버튼 복원 (폴링에서 UI 변경되기 전까지 대비)
        } else {
            alert(result.message || '라운드 시작 실패');
            btn.disabled = false;
            btn.textContent = '🎵 라운드 시작';
        }

    } catch (error) {
        console.error('라운드 시작 오류:', error);
        btn.disabled = false;
        btn.textContent = '🎵 라운드 시작';
    }
}

async function nextRound() {
    if (!isHost) return;

    var btn = document.getElementById('nextRoundBtn');
    btn.disabled = true;
    btn.textContent = '시작 중...';

    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/next-round', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            if (result.isGameOver) {
                window.location.href = '/game/multi/room/' + roomCode + '/result';
            }
            // ★ 성공해도 폴링에서 PLAYING으로 바뀌면 roundResult가 숨겨지므로
            // 다음 RESULT 때를 대비해 버튼 복원은 updatePhaseUI()에서 처리
        } else {
            alert(result.message || '다음 라운드 진행 실패');
            btn.disabled = false;
            btn.textContent = '다음 라운드 →';
        }

    } catch (error) {
        console.error('다음 라운드 오류:', error);
        btn.disabled = false;
        btn.textContent = '다음 라운드 →';
    }
}

// ========== 게임 나가기 ==========

async function quitGame() {
    if (!confirm('정말 게임을 나가시겠습니까?')) return;

    try {
        await fetch('/game/multi/room/' + roomCode + '/leave', { method: 'POST' });
        window.location.href = '/game/multi';
    } catch (error) {
        window.location.href = '/game/multi';
    }
}

// ========== 유틸 ==========

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 오디오 이벤트
audioPlayer.addEventListener('ended', function() {
    isPlaying = false;
});

audioPlayer.addEventListener('error', function() {
    console.error('오디오 재생 오류');
    isPlaying = false;
});