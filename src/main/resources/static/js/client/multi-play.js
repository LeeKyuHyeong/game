// 게임 상태
let currentRound = 0;
let currentPhase = null;  // null, PREPARING, PLAYING, RESULT
let currentSong = null;
let isPlaying = false;
let youtubePlayerReady = false;
let isRoundReady = false;  // 내가 라운드 준비 완료 했는지

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
let serverTimeOffset = 0;  // 서버 시간 - 클라이언트 시간

// 페이지 로드 시 시작
document.addEventListener('DOMContentLoaded', async function() {
    // YouTube Player 초기화
    try {
        await YouTubePlayerManager.init('youtubePlayerContainer', {
            onStateChange: function(e) {
                if (e.data === 0) { // ENDED
                    isPlaying = false;
                }
            },
            onError: function(e, errorInfo) {
                console.error('YouTube 재생 오류:', e.data);
                if (currentSong && currentSong.filePath) {
                    currentSong.youtubeVideoId = null;
                    loadSong(currentSong);
                } else {
                    // MP3 없으면 재생 불가 처리
                    handlePlaybackError(errorInfo);
                }
            }
        });
        youtubePlayerReady = true;
    } catch (error) {
        console.warn('YouTube Player 초기화 실패:', error);
    }

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

        // 서버 시간 오프셋 업데이트 (서버 시간 - 클라이언트 시간)
        if (result.serverTime) {
            serverTimeOffset = result.serverTime - Date.now();
        }

        // 오디오 동기화
        syncAudio(result.audioPlaying, result.audioPlayedAt);

        // 스코어보드 업데이트 및 내 준비 상태 동기화
        if (result.participants) {
            // 내 roundReady 상태 동기화 (새 라운드 시작 시 서버에서 false로 초기화됨)
            var myParticipant = result.participants.find(function(p) { return p.memberId === myMemberId; });
            if (myParticipant) {
                isRoundReady = myParticipant.roundReady;
                // PREPARING 상태일 때만 버튼 업데이트
                if (currentPhase === 'PREPARING') {
                    updateRoundReadyButton();
                }
            }
        }
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
    document.getElementById('roundPreparing').style.display = 'none';
    document.getElementById('roundPlaying').style.display = 'none';
    document.getElementById('roundResult').style.display = 'none';

    if (currentPhase === 'PREPARING') {
        // 광고 시청 후 준비 완료 단계
        document.getElementById('roundPreparing').style.display = 'block';
        document.getElementById('preparingRound').textContent = currentRound;
        stopProgressUpdate();
        // 내가 이미 준비했는지 체크하지 않고, 버튼 상태만 업데이트
        // isRoundReady는 fetchRoundInfo에서 참가자 정보로 동기화됨
        updateRoundReadyButton();
    } else if (currentPhase === 'PLAYING') {
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

// ========== 라운드 준비 (PREPARING 단계) ==========

function updateRoundReadyButton() {
    var btn = document.getElementById('roundReadyBtn');
    if (!btn) return;

    if (isRoundReady) {
        btn.disabled = true;
        btn.textContent = '준비 완료!';
        btn.classList.add('ready-done');
    } else {
        btn.disabled = false;
        btn.textContent = '준비 완료';
        btn.classList.remove('ready-done');
    }
}

async function setRoundReady() {
    if (isRoundReady) return;

    var btn = document.getElementById('roundReadyBtn');
    btn.disabled = true;
    btn.textContent = '처리 중...';

    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/round-ready', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            isRoundReady = true;
            btn.textContent = '준비 완료!';
            btn.classList.add('ready-done');
        } else {
            btn.disabled = false;
            btn.textContent = '준비 완료';
            alert(result.message || '준비 처리 실패');
        }

    } catch (error) {
        console.error('라운드 준비 오류:', error);
        btn.disabled = false;
        btn.textContent = '준비 완료';
    }
}

// ========== 오디오 동기화 ==========

function syncAudio(serverPlaying, serverPlayedAt) {
    if (serverPlaying === lastAudioPlaying && serverPlayedAt === lastAudioPlayedAt) {
        return;
    }

    lastAudioPlaying = serverPlaying;
    lastAudioPlayedAt = serverPlayedAt;

    if (serverPlaying && serverPlayedAt && currentSong) {
        // 서버 시간 기준으로 경과 시간 계산 (오프셋 보정)
        var adjustedClientTime = Date.now() + serverTimeOffset;
        var elapsedMs = adjustedClientTime - serverPlayedAt;
        var elapsedSec = elapsedMs / 1000;
        var startTime = currentSong.startTime || 0;

        var playDuration = currentSong.playDuration || 30;

        // 디버깅: 비정상적인 시간 차이 확인
        if (Math.abs(elapsedSec) > 5 || Math.abs(serverTimeOffset) > 5000) {
            console.warn('Audio sync info:', {
                serverPlayedAt: serverPlayedAt,
                clientNow: Date.now(),
                serverTimeOffset: serverTimeOffset,
                adjustedClientTime: adjustedClientTime,
                elapsedSec: elapsedSec.toFixed(1),
                startTime: startTime,
                playDuration: playDuration
            });
        }

        // 음수이거나 재생 시간을 초과하면 처음부터
        if (elapsedSec < 0 || elapsedSec > playDuration) {
            elapsedSec = 0;
        }

        var targetTime = startTime + elapsedSec;

        if (currentSong.youtubeVideoId && youtubePlayerReady) {
            // YouTube 동기화
            YouTubePlayerManager.seekTo(targetTime);
            if (!isPlaying) {
                YouTubePlayerManager.play();
                isPlaying = true;
            }
        } else {
            // MP3 동기화
            audioPlayer.currentTime = targetTime;
            if (!isPlaying) {
                audioPlayer.play().catch(function(e) {
                    console.log('자동 재생 실패:', e);
                });
                isPlaying = true;
            }
        }
    } else {
        if (isPlaying) {
            if (currentSong && currentSong.youtubeVideoId && youtubePlayerReady) {
                YouTubePlayerManager.pause();
            } else {
                audioPlayer.pause();
            }
            isPlaying = false;
        }
    }
}

function loadSong(song) {
    if (!song) return;

    if (song.youtubeVideoId && youtubePlayerReady) {
        YouTubePlayerManager.loadVideo(song.youtubeVideoId, song.startTime || 0);
    } else if (song.filePath) {
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
    var currentTime;

    if (currentSong.youtubeVideoId && youtubePlayerReady) {
        currentTime = YouTubePlayerManager.getCurrentTime() - startTime;
    } else {
        currentTime = audioPlayer.currentTime - startTime;
    }

    currentTime = Math.max(0, currentTime);
    var progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    document.getElementById('currentTime').textContent = formatTime(Math.min(currentTime, duration));
    document.getElementById('totalTime').textContent = formatTime(duration);
}

function formatTime(seconds) {
    if (isNaN(seconds) || seconds === null || seconds === undefined) {
        return '0:00';
    }
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

        // PREPARING 단계에서 준비 상태 표시
        var readyBadge = '';
        if (currentPhase === 'PREPARING') {
            readyBadge = p.roundReady ? ' <span class="ready-badge">✓</span>' : ' <span class="not-ready-badge">...</span>';
        }

        html += '<div class="score-item ' + meClass + '">' +
            '<span class="rank">' + (index + 1) + '</span>' +
            '<span class="player-name">' + hostIcon + escapeHtml(p.nickname) + meBadge + readyBadge + '</span>' +
            '<span class="player-score">' + p.score + '</span>' +
        '</div>';
    });

    container.innerHTML = html;

    // PREPARING 단계에서 준비 인원 카운트 업데이트
    if (currentPhase === 'PREPARING') {
        var readyCount = participants.filter(function(p) { return p.roundReady; }).length;
        var totalCount = participants.length;
        var readyStatus = document.getElementById('readyStatusCount');
        if (readyStatus) {
            readyStatus.textContent = readyCount + ' / ' + totalCount + ' 명 준비 완료';
        }
    }
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

// ========== 재생 실패 처리 ==========

/**
 * YouTube 재생 실패 시 처리 (Multiplayer)
 * @param {object} errorInfo - 에러 정보 (code, message, isPlaybackError)
 */
function handlePlaybackError(errorInfo) {
    if (!currentSong) return;

    console.log('재생 실패 처리:', errorInfo);

    // 재생 불가 에러인 경우에만 처리
    if (errorInfo && errorInfo.isPlaybackError) {
        // 1. 자동 신고 (서버에 재생 불가 보고)
        reportUnplayableSong(currentSong.id, errorInfo.code);

        // 2. 로컬 알림 표시 (채팅에 시스템 메시지 추가)
        showPlaybackErrorNotice(errorInfo);
    }
}

/**
 * 재생 불가 곡 자동 신고
 */
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

/**
 * 재생 불가 알림 표시 (채팅 영역에 로컬 메시지 + 방장에게 스킵 버튼)
 */
function showPlaybackErrorNotice(errorInfo) {
    var container = document.getElementById('chatMessages');
    var div = document.createElement('div');
    div.className = 'chat-message system-message playback-error-notice';

    var html = '<span class="system-text">⚠️ 이 곡을 재생할 수 없습니다<br>' +
        '<small style="color:#888;">(' + (errorInfo ? errorInfo.message : '알 수 없는 오류') +
        ') - ✓ 자동 신고 완료</small></span>';

    // 방장에게만 스킵 버튼 표시
    if (isHost && currentSong) {
        html += '<button class="btn-skip-song" onclick="skipUnplayableSong(' + currentSong.id + ')">다른 곡으로 변경</button>';
    }

    div.innerHTML = html;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

/**
 * 재생 불가 곡 스킵 (방장만)
 */
async function skipUnplayableSong(songId) {
    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/skip-song', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ songId: songId })
        });

        const result = await response.json();

        if (result.success) {
            if (result.isGameOver) {
                // 게임 종료
                window.location.href = '/game/multi/room/' + roomCode + '/result';
            }
            // 성공 시 폴링에서 새 곡 정보를 받아옴
        } else {
            alert(result.message || '스킵에 실패했습니다.');
        }
    } catch (error) {
        console.error('스킵 요청 실패:', error);
        alert('스킵 요청 중 오류가 발생했습니다.');
    }
}