// 게임 상태
let currentRound = 0;
let currentPhase = null;  // null, PLAYING, RESULT
let currentSong = null;
let isPlaying = false;
let youtubePlayerReady = false;
let videoReady = false;  // YouTube 영상 로드 완료 여부 (CUED 상태)
let pendingPlay = false; // 서버에서 재생 요청이 왔지만 영상 로드 대기 중
let mySkipVoted = false;  // 내가 스킵 투표했는지

// 폴링 관련
let roundPollingInterval = null;
let chatPollingInterval = null;
let progressInterval = null;
let lastChatId = 0;
let networkErrorCount = 0;  // 연속 네트워크 오류 횟수
const MAX_NETWORK_ERRORS = 5;  // 최대 연속 오류 허용 횟수

// 오디오 동기화
let lastAudioPlaying = false;
let lastAudioPlayedAt = null;
let serverTimeOffset = 0;  // 서버 시간 - 클라이언트 시간

// 재시도 관련
let loadRetryCount = 0;
const MAX_LOAD_RETRIES = 2;
let errorRetryCount = 0;
const MAX_ERROR_RETRIES = 1;

// YouTube video ID 유효성 검사 (11자 alphanumeric + -_)
function isValidYoutubeVideoId(videoId) {
    if (!videoId || typeof videoId !== 'string') return false;
    var trimmed = videoId.trim();
    // 정확히 11자, alphanumeric + - _
    return /^[a-zA-Z0-9_-]{11}$/.test(trimmed) &&
           trimmed !== 'undefined' &&
           trimmed !== 'null';
}

// 페이지 로드 시 시작
document.addEventListener('DOMContentLoaded', async function() {
    // YouTube Player 초기화
    try {
        await YouTubePlayerManager.init('youtubePlayerContainer', {
            onStateChange: function(e) {
                // 상태: -1=unstarted, 0=ended, 1=playing, 2=paused, 3=buffering, 5=cued
                console.log('YouTube 상태 변경:', e.data);

                if (e.data === 5) { // CUED - 영상 로드 완료
                    videoReady = true;
                    console.log('영상 로드 완료 (CUED)');

                    // 서버에서 재생 요청이 대기 중이었으면 자동 재생
                    if (pendingPlay && currentSong) {
                        console.log('대기 중이던 재생 시작');
                        startPlayback();
                    }
                } else if (e.data === 1) { // PLAYING
                    isPlaying = true;
                    pendingPlay = false;
                } else if (e.data === 0) { // ENDED
                    isPlaying = false;
                } else if (e.data === 2) { // PAUSED
                    isPlaying = false;
                }
            },
            onError: function(e, errorInfo) {
                console.error('YouTube 재생 오류:', e.data);
                videoReady = false;
                pendingPlay = false;
                // 재생 불가 처리 (자동 신고 + 방장에게 스킵 버튼 표시)
                handlePlaybackError(errorInfo);
            }
        });
        youtubePlayerReady = true;
    } catch (error) {
        console.error('YouTube Player 초기화 실패:', error);
        showYouTubeInitError();
    }

    startPolling();

    // Enter 키로 채팅 전송
    document.getElementById('chatInput').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            sendChat();
        }
    });
});

// 페이지 떠날 때 정리 및 방 나가기
window.addEventListener('beforeunload', function() {
    stopPolling();
    // sendBeacon으로 방 나가기 요청 (페이지 언로드되어도 전송 보장)
    navigator.sendBeacon('/game/multi/room/' + roomCode + '/leave');
});

// 뒤로가기/앞으로가기 시에도 나가기 처리
window.addEventListener('pagehide', function() {
    stopPolling();
    navigator.sendBeacon('/game/multi/room/' + roomCode + '/leave');
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

        // 성공 시 오류 카운터 초기화
        networkErrorCount = 0;

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
            // 라운드 변경 시 스킵 투표 상태 초기화
            mySkipVoted = false;
            resetSkipVoteUI();
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

        // 스코어보드 업데이트
        if (result.participants) {
            updateScoreboard(result.participants);
            // 내 스킵 투표 상태 확인
            var myParticipant = result.participants.find(function(p) {
                return p.memberId === myMemberId;
            });
            if (myParticipant && myParticipant.skipVote) {
                mySkipVoted = true;
                updateSkipVoteButton();
            }
        }

        // 스킵 투표 현황 업데이트
        if (result.skipVoteStatus && currentPhase === 'PLAYING') {
            updateSkipVoteStatus(result.skipVoteStatus);
        }

        // 결과 단계일 때 정답/정답자 표시
        if (currentPhase === 'RESULT') {
            if (result.answer) {
                showAnswer(result.answer);
            }
            if (result.winnerNickname) {
                showWinner(result.winnerNickname);
            } else {
                // 정답자가 없는 경우 (모두 포기)
                showNoWinner();
            }
        }

    } catch (error) {
        console.error('라운드 정보 조회 오류:', error);
        networkErrorCount++;

        if (networkErrorCount >= MAX_NETWORK_ERRORS) {
            console.error('네트워크 오류가 계속 발생하여 폴링을 중단합니다.');
            stopPolling();
            showNetworkErrorNotice();
        }
    }
}

/**
 * YouTube 초기화 실패 알림 표시
 */
function showYouTubeInitError() {
    var container = document.getElementById('chatMessages');
    var div = document.createElement('div');
    div.className = 'chat-message system-message playback-error-notice';
    div.innerHTML = '<span class="system-text">⚠️ YouTube 플레이어 초기화에 실패했습니다.<br>' +
        '<small style="color:#888;">페이지를 새로고침해주세요.</small><br>' +
        '<button class="btn-skip-song" onclick="location.reload()">새로고침</button></span>';
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

/**
 * 네트워크 오류 알림 표시
 */
function showNetworkErrorNotice() {
    var container = document.getElementById('chatMessages');
    var div = document.createElement('div');
    div.className = 'chat-message system-message playback-error-notice';
    div.innerHTML = '<span class="system-text">⚠️ 서버 연결이 끊어졌습니다.<br>' +
        '<button class="btn-skip-song" onclick="reconnect()">다시 연결</button></span>';
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

/**
 * 재연결 시도
 */
function reconnect() {
    networkErrorCount = 0;
    startPolling();

    // 알림 메시지 제거
    var notices = document.querySelectorAll('.playback-error-notice');
    notices.forEach(function(notice) {
        if (notice.textContent.includes('서버 연결이 끊어졌습니다')) {
            notice.remove();
        }
    });
}

// ========== 페이즈 UI ==========

function updatePhaseUI() {
    document.getElementById('roundWaiting').style.display = 'none';
    document.getElementById('roundPlaying').style.display = 'none';
    document.getElementById('roundResult').style.display = 'none';

    if (currentPhase === 'PLAYING') {
        document.getElementById('roundPlaying').style.display = 'block';
        startProgressUpdate();
        updateHeaderPlayerStatus('playing');
    } else if (currentPhase === 'RESULT') {
        document.getElementById('roundResult').style.display = 'block';
        stopProgressUpdate();
        updateHeaderPlayerStatus('result');

        // ★ 마지막 라운드면 버튼 텍스트 변경
        updateNextRoundButton();
    } else {
        // 대기 상태
        document.getElementById('roundWaiting').style.display = 'block';
        stopProgressUpdate();
        updateHeaderPlayerStatus('waiting');

        // 라운드 시작 버튼 상태 복원
        resetStartRoundButton();

        // 대기 메시지 업데이트
        var msg = currentRound === 0
            ? '방장이 라운드를 시작하면 노래가 재생됩니다'
            : '방장이 다음 라운드를 시작해주세요';
        document.getElementById('waitingMessage').textContent = msg;
    }
}

// 헤더 재생 상태 업데이트 (모바일용)
function updateHeaderPlayerStatus(status) {
    var iconEl = document.getElementById('headerMusicIcon');
    var statusEl = document.getElementById('headerPlayerStatus');
    if (!iconEl || !statusEl) return;

    switch (status) {
        case 'playing':
            iconEl.textContent = '🎶';
            statusEl.textContent = '재생 중...';
            iconEl.classList.add('playing');
            break;
        case 'result':
            iconEl.textContent = '✅';
            statusEl.textContent = '정답!';
            iconEl.classList.remove('playing');
            break;
        case 'waiting':
        default:
            iconEl.textContent = '🎵';
            statusEl.textContent = '대기중';
            iconEl.classList.remove('playing');
            break;
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
        // 에러 상태면 재생 시도 안함
        if (YouTubePlayerManager.hasPlaybackError()) {
            console.warn('플레이어 에러 상태, 재생 스킵');
            return;
        }

        if (!youtubePlayerReady) {
            console.warn('YouTube Player not ready for sync');
            pendingPlay = true;
            return;
        }

        // 영상이 아직 로드되지 않았으면 대기
        if (!videoReady) {
            console.log('영상 로드 대기 중... pendingPlay 설정');
            pendingPlay = true;
            return;
        }

        // 영상 준비됨 - 재생 시작
        startPlayback();
    } else {
        // 재생 중지
        pendingPlay = false;
        if (isPlaying && youtubePlayerReady) {
            YouTubePlayerManager.pause();
            isPlaying = false;
        }
    }
}

/**
 * 영상 재생 시작 (영상 로드 완료 후 호출)
 */
function startPlayback() {
    if (!currentSong || !youtubePlayerReady || !videoReady) {
        console.warn('startPlayback: 조건 불충족', {
            currentSong: !!currentSong,
            youtubePlayerReady: youtubePlayerReady,
            videoReady: videoReady
        });
        return;
    }

    // 서버 시간 기준으로 경과 시간 계산 (오프셋 보정)
    var adjustedClientTime = Date.now() + serverTimeOffset;
    var elapsedMs = adjustedClientTime - lastAudioPlayedAt;
    var elapsedSec = elapsedMs / 1000;
    var startTime = currentSong.startTime || 0;
    var playDuration = currentSong.playDuration || 30;

    // 디버깅: 비정상적인 시간 차이 확인
    if (Math.abs(elapsedSec) > 5 || Math.abs(serverTimeOffset) > 5000) {
        console.warn('Audio sync info:', {
            lastAudioPlayedAt: lastAudioPlayedAt,
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

    console.log('재생 시작: targetTime=' + targetTime.toFixed(1) + 's');

    // YouTube 동기화 및 재생
    YouTubePlayerManager.seekTo(targetTime);
    YouTubePlayerManager.play();
    isPlaying = true;
    pendingPlay = false;
}

function loadSong(song) {
    if (!song) return;

    // 새 곡 로드 시 상태 초기화
    videoReady = false;
    pendingPlay = false;

    if (!youtubePlayerReady) {
        console.warn('YouTube Player not ready for loadSong');
        // 1초 후 재시도
        if (loadRetryCount < MAX_LOAD_RETRIES) {
            loadRetryCount++;
            console.log('YouTube Player not ready, 재시도 예정 (' + loadRetryCount + '/' + MAX_LOAD_RETRIES + ')');
            setTimeout(function() { loadSong(song); }, 1000);
        }
        return;
    }

    if (isValidYoutubeVideoId(song.youtubeVideoId)) {
        // 새 곡이면 카운터 리셋
        if (!currentSong || currentSong.id !== song.id) {
            loadRetryCount = 0;
            errorRetryCount = 0;
        }

        console.log('영상 로드 시작: ' + song.youtubeVideoId);
        var loaded = YouTubePlayerManager.loadVideo(song.youtubeVideoId.trim(), song.startTime || 0);
        if (!loaded && loadRetryCount < MAX_LOAD_RETRIES) {
            loadRetryCount++;
            console.log('YouTube loadVideo 실패, 재시도 예정 (' + loadRetryCount + '/' + MAX_LOAD_RETRIES + ')');
            setTimeout(function() { loadSong(song); }, 1000);
            return;
        }

        if (!loaded) {
            console.error('YouTube loadVideo 실패 (최대 재시도 초과)');
            handlePlaybackError({
                code: 'LOAD_FAILED',
                message: 'YouTube 영상 로드 실패',
                isPlaybackError: true
            });
        }
    } else {
        console.error('유효하지 않은 YouTube Video ID:', song.youtubeVideoId);
        handlePlaybackError({
            code: 'INVALID_VIDEO_ID',
            message: '유효하지 않은 YouTube Video ID',
            isPlaybackError: true
        });
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
    if (!currentSong || !youtubePlayerReady) return;

    var startTime = currentSong.startTime || 0;
    var duration = currentSong.playDuration || 10;
    var currentTime = YouTubePlayerManager.getCurrentTime() - startTime;

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
        var hostIcon = p.isHost ? '👑' : '';
        var meBadge = p.memberId === myMemberId ? '(나)' : '';
        var displayName = hostIcon + escapeHtml(p.nickname) + meBadge;

        // 내 점수 헤더에 업데이트
        if (p.memberId === myMemberId) {
            var myScoreEl = document.getElementById('myScore');
            if (myScoreEl) {
                myScoreEl.textContent = p.score;
            }
        }

        // 모바일: "순위 - 이름 - 점수" 형식
        html += '<div class="score-item ' + meClass + '">' +
            '<span class="rank">' + (index + 1) + '</span>' +
            '<span class="player-name">' + displayName + '</span>' +
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
    document.getElementById('noWinnerInfo').style.display = 'none';
    document.getElementById('resultTitle').textContent = '🎉 정답!';
}

function showNoWinner() {
    document.getElementById('winnerInfo').style.display = 'none';
    document.getElementById('noWinnerInfo').style.display = 'block';
    document.getElementById('resultTitle').textContent = '⏭️ 라운드 스킵';
}

// ========== 스킵 투표 ==========

async function voteSkipRound() {
    if (mySkipVoted) return;

    var btn = document.getElementById('skipVoteBtn');
    btn.disabled = true;

    try {
        const response = await fetch('/game/multi/room/' + roomCode + '/skip-vote', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            mySkipVoted = true;
            updateSkipVoteButton();
        } else {
            alert(result.message || '포기 투표 실패');
            btn.disabled = false;
        }

    } catch (error) {
        console.error('포기 투표 오류:', error);
        btn.disabled = false;
    }
}

function updateSkipVoteButton() {
    // 모바일/데스크톱 버튼 둘 다 업데이트
    var btn = document.getElementById('skipVoteBtn');
    var btnDesktop = document.getElementById('skipVoteBtnDesktop');

    if (mySkipVoted) {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '✓ 완료';
            btn.classList.add('voted');
        }
        if (btnDesktop) {
            btnDesktop.disabled = true;
            btnDesktop.textContent = '✓ 포기함';
            btnDesktop.classList.add('voted');
        }
    }
}

function updateSkipVoteStatus(status) {
    // 모바일/데스크톱 둘 다 업데이트
    var countEl = document.getElementById('skipVoteCount');
    var totalEl = document.getElementById('skipVoteTotal');
    var countDesktop = document.getElementById('skipVoteCountDesktop');
    var totalDesktop = document.getElementById('skipVoteTotalDesktop');

    if (countEl) countEl.textContent = status.votedCount;
    if (totalEl) totalEl.textContent = status.totalCount;
    if (countDesktop) countDesktop.textContent = status.votedCount;
    if (totalDesktop) totalDesktop.textContent = status.totalCount;
}

function resetSkipVoteUI() {
    // 모바일 버튼 (아이콘 span 포함)
    var btn = document.getElementById('skipVoteBtn');
    if (btn) {
        btn.disabled = false;
        btn.innerHTML = '<span class="skip-icon">🏳️</span> 포기';
        btn.classList.remove('voted');
    }
    // 데스크톱 버튼
    var btnDesktop = document.getElementById('skipVoteBtnDesktop');
    if (btnDesktop) {
        btnDesktop.disabled = false;
        btnDesktop.textContent = '⏭️ 포기';
        btnDesktop.classList.remove('voted');
    }

    // 카운트 초기화
    var countEl = document.getElementById('skipVoteCount');
    var countDesktop = document.getElementById('skipVoteCountDesktop');
    if (countEl) countEl.textContent = '0';
    if (countDesktop) countDesktop.textContent = '0';
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
            } else {
                // ★ 다음 라운드 시작 성공 시 채팅 입력창에 자동 포커스 (방장 UX 개선)
                document.getElementById('chatInput').focus();
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
 * 재생 불가 곡 자동 신고 및 서버에 재생 오류 보고
 */
async function reportUnplayableSong(songId, errorCode) {
    // 1. 기존 신고 API 호출
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

    // 2. 서버에 재생 오류 보고 (YouTube 유효성 플래그 업데이트)
    try {
        await fetch('/game/multi/room/' + roomCode + '/playback-error', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                songId: songId,
                errorCode: errorCode
            })
        });
        console.log('서버에 재생 오류 보고 완료');
    } catch (error) {
        console.error('재생 오류 보고 실패:', error);
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

    // 모든 사용자에게 플레이어 재시작 버튼 표시
    html += '<button class="btn-skip-song" onclick="reinitializePlayer()" style="margin-left:5px;">플레이어 재시작</button>';

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

// ========== YouTube 플레이어 재초기화 ==========

/**
 * YouTube IFrame API 재초기화
 * 심각한 오류 시 플레이어를 재생성합니다.
 */
async function reinitializePlayer() {
    console.log('YouTube 플레이어 재초기화 중...');

    // 기존 플레이어 정리
    if (YouTubePlayerManager.player) {
        try {
            YouTubePlayerManager.player.destroy();
        } catch (e) {
            console.warn('플레이어 destroy 실패:', e);
        }
    }

    YouTubePlayerManager.player = null;
    YouTubePlayerManager.isReady = false;
    YouTubePlayerManager.lastError = null;
    YouTubePlayerManager.currentVideoId = null;
    YouTubePlayerManager.retryCount = 0;
    youtubePlayerReady = false;
    videoReady = false;
    pendingPlay = false;
    isPlaying = false;

    // 컨테이너 재생성
    var container = document.getElementById('youtubePlayerContainer');
    if (container) {
        container.innerHTML = '<div id="youtubePlayerContainer"></div>';
    }

    try {
        await YouTubePlayerManager.init('youtubePlayerContainer', {
            onStateChange: function(e) {
                console.log('YouTube 상태 변경:', e.data);

                if (e.data === 5) { // CUED - 영상 로드 완료
                    videoReady = true;
                    console.log('영상 로드 완료 (CUED)');
                    if (pendingPlay && currentSong) {
                        console.log('대기 중이던 재생 시작');
                        startPlayback();
                    }
                } else if (e.data === 1) { // PLAYING
                    isPlaying = true;
                    pendingPlay = false;
                } else if (e.data === 0) { // ENDED
                    isPlaying = false;
                } else if (e.data === 2) { // PAUSED
                    isPlaying = false;
                }
            },
            onError: function(e, errorInfo) {
                console.error('YouTube 재생 오류:', e.data);
                videoReady = false;
                pendingPlay = false;
                handlePlaybackError(errorInfo);
            }
        });
        youtubePlayerReady = true;

        // 현재 곡 다시 로드
        if (currentSong) {
            loadRetryCount = 0;
            errorRetryCount = 0;
            loadSong(currentSong);
        }

        console.log('YouTube 플레이어 재초기화 완료');

        // 재초기화 성공 알림
        var chatContainer = document.getElementById('chatMessages');
        var div = document.createElement('div');
        div.className = 'chat-message system-message';
        div.innerHTML = '<span class="system-text">✓ 플레이어가 재시작되었습니다.</span>';
        chatContainer.appendChild(div);
        chatContainer.scrollTop = chatContainer.scrollHeight;

    } catch (error) {
        console.error('YouTube 플레이어 재초기화 실패:', error);
        showYouTubeInitError();
    }
}