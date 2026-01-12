let currentRound = 1;
let currentSong = null;
let isPlaying = false;
let audioPlayer = document.getElementById('audioPlayer');
let progressInterval = null;
let score = 0;
let correctCount = 0;
let wrongCount = 0;
let skipCount = 0;
let actualTotalRounds = totalRounds; // 서버에서 업데이트될 수 있음
let isRoundEnded = false; // 라운드 종료 플래그
let isRoundReady = false; // 준비 완료 플래그
let totalPlayTime = 0; // 실제 재생된 총 시간 (초)
let playStartTime = null; // 현재 재생 시작 시점
let youtubePlayerReady = false; // YouTube Player 준비 상태

// 게임 시작
document.addEventListener('DOMContentLoaded', async function() {
    // YouTube Player 초기화
    try {
        await YouTubePlayerManager.init('youtubePlayerContainer', {
            onStateChange: function(e) {
                // 재생 종료 시 (ENDED = 0)
                if (e.data === 0) {
                    pauseAudio();
                }
            },
            onError: function(e, errorInfo) {
                console.error('YouTube 재생 오류:', e.data);
                // MP3 fallback이 있으면 시도
                if (currentSong && currentSong.filePath) {
                    console.log('MP3 fallback 시도');
                    currentSong.youtubeVideoId = null;
                    loadAudioSource();
                } else {
                    // MP3 없으면 재생 불가 처리
                    handlePlaybackError(errorInfo);
                }
            }
        });
        youtubePlayerReady = true;
    } catch (error) {
        console.warn('YouTube Player 초기화 실패, MP3 모드로 진행:', error);
    }

    // 매 라운드 선택 모드 처리
    if (gameMode === 'GENRE_PER_ROUND') {
        showGenreSelectModal(1);
    } else if (gameMode === 'ARTIST_PER_ROUND') {
        showArtistSelectModal(1);
    } else if (gameMode === 'YEAR_PER_ROUND') {
        showYearSelectModal(1);
    } else {
        loadRound(1);
    }

    // 아티스트 검색 입력 이벤트
    const artistSearchInput = document.getElementById('artistSearchInput');
    if (artistSearchInput) {
        artistSearchInput.addEventListener('input', function() {
            renderArtistList(this.value);
        });
    }

    // 대체된 곡 알림 표시
    showReplacedSongsNotice();
});

// 라운드 축소 알림 표시
function showReplacedSongsNotice() {
    const roundsReducedJson = sessionStorage.getItem('roundsReduced');
    if (roundsReducedJson) {
        sessionStorage.removeItem('roundsReduced'); // 한 번 표시 후 삭제

        const info = JSON.parse(roundsReducedJson);
        const notice = document.createElement('div');
        notice.className = 'rounds-reduced-notice';

        // 랭킹 미충족 경고 (10라운드 이상 요청했는데 실제 10라운드 미만인 경우)
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

        // 5초 후 페이드아웃 (랭킹 경고가 있으면 더 오래 표시)
        const duration = rankingWarning ? 5000 : 3500;
        setTimeout(() => {
            notice.classList.add('fade-out');
            setTimeout(() => notice.remove(), 500);
        }, duration);
    }
}

async function showGenreSelectModal(roundNumber) {
    const modal = document.getElementById('genreSelectModal');
    const genreList = document.getElementById('genreList');

    try {
        const response = await fetch('/game/solo/guess/genres-with-count');
        let genres = await response.json();

        // 남은 곡 개수 순으로 정렬 (내림차순)
        genres.sort((a, b) => b.availableCount - a.availableCount);

        genreList.innerHTML = '';

        genres.forEach(genre => {
            const item = document.createElement('div');
            item.className = 'genre-item';

            if (genre.availableCount === 0) {
                item.classList.add('disabled');
                // hideEmptyOptions 설정에 따라 숨김 처리
                if (hideEmptyOptions) {
                    item.classList.add('hidden');
                }
            }

            item.dataset.genreId = genre.id;
            item.dataset.genreName = genre.name;
            item.innerHTML = `
                <span class="genre-name">${genre.name}</span>
                <span class="genre-count">${genre.availableCount}곡</span>
            `;

            if (genre.availableCount > 0) {
                item.addEventListener('click', () => selectGenre(genre.id, roundNumber));
            }

            genreList.appendChild(item);
        });

    } catch (error) {
        console.error('장르 목록 로딩 오류:', error);
    }

    modal.classList.add('show');
}

async function selectGenre(genreId, roundNumber) {
    try {
        const response = await fetch('/game/solo/guess/select-genre', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                genreId: genreId,
                roundNumber: roundNumber
            })
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('genreSelectModal').classList.remove('show');
            loadRound(roundNumber);
        } else {
            alert(result.message || '장르 선택에 실패했습니다.');
        }
    } catch (error) {
        console.error('장르 선택 오류:', error);
        alert('장르 선택 중 오류가 발생했습니다.');
    }
}

// ========== 아티스트 선택 모달 ==========

let allArtistsForModal = [];
let currentArtistRound = 1;

async function showArtistSelectModal(roundNumber) {
    const modal = document.getElementById('artistSelectModal');
    currentArtistRound = roundNumber;

    try {
        const response = await fetch('/game/solo/guess/artists-with-count');
        allArtistsForModal = await response.json();

        // 남은 곡 개수 순으로 정렬 (내림차순)
        allArtistsForModal.sort((a, b) => b.count - a.count);

        renderArtistList();

    } catch (error) {
        console.error('아티스트 목록 로딩 오류:', error);
    }

    // 검색 입력 초기화
    document.getElementById('artistSearchInput').value = '';
    modal.classList.add('show');
}

function renderArtistList(filterKeyword = '') {
    const artistList = document.getElementById('artistList');
    let artistsToShow = allArtistsForModal;

    if (filterKeyword) {
        artistsToShow = allArtistsForModal.filter(a =>
            a.name.toLowerCase().includes(filterKeyword.toLowerCase())
        );
    }

    artistList.innerHTML = '';

    artistsToShow.forEach(artist => {
        const item = document.createElement('div');
        item.className = 'genre-item';

        if (artist.count === 0) {
            item.classList.add('disabled');
            if (hideEmptyOptions) {
                item.classList.add('hidden');
            }
        }

        item.innerHTML = `
            <span class="genre-name">${artist.name}</span>
            <span class="genre-count">${artist.count}곡</span>
        `;

        if (artist.count > 0) {
            item.addEventListener('click', () => selectArtist(artist.name, currentArtistRound));
        }

        artistList.appendChild(item);
    });
}

async function selectArtist(artistName, roundNumber) {
    try {
        const response = await fetch('/game/solo/guess/select-artist', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                artist: artistName,
                roundNumber: roundNumber
            })
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('artistSelectModal').classList.remove('show');
            loadRound(roundNumber);
        } else {
            alert(result.message || '아티스트 선택에 실패했습니다.');
        }
    } catch (error) {
        console.error('아티스트 선택 오류:', error);
        alert('아티스트 선택 중 오류가 발생했습니다.');
    }
}

// ========== 연도 선택 모달 ==========

async function showYearSelectModal(roundNumber) {
    const modal = document.getElementById('yearSelectModal');
    const yearList = document.getElementById('yearList');

    try {
        const response = await fetch('/game/solo/guess/years-with-count');
        let years = await response.json();

        // 이미 최신순 정렬되어있음

        yearList.innerHTML = '';

        years.forEach(year => {
            const item = document.createElement('div');
            item.className = 'genre-item';

            if (year.count === 0) {
                item.classList.add('disabled');
                if (hideEmptyOptions) {
                    item.classList.add('hidden');
                }
            }

            item.innerHTML = `
                <span class="genre-name">${year.year}년</span>
                <span class="genre-count">${year.count}곡</span>
            `;

            if (year.count > 0) {
                item.addEventListener('click', () => selectYear(year.year, roundNumber));
            }

            yearList.appendChild(item);
        });

    } catch (error) {
        console.error('연도 목록 로딩 오류:', error);
    }

    modal.classList.add('show');
}

async function selectYear(year, roundNumber) {
    try {
        const response = await fetch('/game/solo/guess/select-year', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                year: year,
                roundNumber: roundNumber
            })
        });

        const result = await response.json();

        if (result.success) {
            document.getElementById('yearSelectModal').classList.remove('show');
            loadRound(roundNumber);
        } else {
            alert(result.message || '연도 선택에 실패했습니다.');
        }
    } catch (error) {
        console.error('연도 선택 오류:', error);
        alert('연도 선택 중 오류가 발생했습니다.');
    }
}

async function loadRound(roundNumber) {
    try {
        const response = await fetch(`/game/solo/guess/round/${roundNumber}`);
        const result = await response.json();

        if (!result.success) {
            alert(result.message);
            return;
        }

        currentRound = roundNumber;
        currentSong = result.song;

        // 서버의 totalRounds로 업데이트
        if (result.totalRounds) {
            actualTotalRounds = result.totalRounds;
        }

        document.getElementById('currentRound').textContent = roundNumber;

        // 오디오 소스 로드
        loadAudioSource();

        // UI 리셋
        resetUI();

        // 입력 필드 포커스
        document.getElementById('answerInput').focus();

    } catch (error) {
        console.error('라운드 로딩 오류:', error);
        alert('라운드를 불러오는 중 오류가 발생했습니다.');
    }
}

// 오디오 소스 로드 (YouTube 또는 MP3)
function loadAudioSource() {
    if (!currentSong) return;

    if (currentSong.youtubeVideoId && youtubePlayerReady) {
        // YouTube 재생
        YouTubePlayerManager.loadVideo(currentSong.youtubeVideoId, currentSong.startTime || 0);
        updateTimeDisplay();
    } else if (currentSong.filePath) {
        // MP3 재생 (fallback)
        audioPlayer.src = `/uploads/songs/${currentSong.filePath}`;
        audioPlayer.currentTime = 0;
        audioPlayer.onloadedmetadata = function() {
            updateTimeDisplay();
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

    // 재생 시작 시점 기록
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

    // 일시정지 시 재생된 시간 누적
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

    // 정지 시 재생된 시간 누적
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
        // YouTube: startTime부터 상대적 시간 계산
        const startTime = currentSong.startTime || 0;
        currentTime = YouTubePlayerManager.getCurrentTime() - startTime;
    } else {
        currentTime = audioPlayer.currentTime;
    }

    currentTime = Math.max(0, currentTime);
    const progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    updateTimeDisplay();

    if (currentTime >= duration) {
        pauseAudio();
    }
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
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function resetUI() {
    stopAudio();
    isRoundEnded = false; // 라운드 종료 플래그 리셋
    isRoundReady = false; // 준비 완료 플래그 리셋
    totalPlayTime = 0; // 재생 시간 리셋
    playStartTime = null; // 재생 시작 시점 리셋
    document.getElementById('answerInput').value = '';
    // 피드백 메시지 초기화
    const feedbackEl = document.getElementById('attemptFeedback');
    if (feedbackEl) {
        feedbackEl.style.display = 'none';
        feedbackEl.textContent = '';
    }
}

// 준비 완료 프롬프트 표시
function showReadyPrompt() {
    isRoundReady = false;
    const playBtn = document.getElementById('playBtn');
    const readyPrompt = document.getElementById('readyPrompt');

    if (playBtn) {
        playBtn.disabled = true;
    }
    if (readyPrompt) {
        readyPrompt.style.display = 'block';
    }
}

// 준비 완료 처리
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

    // 입력창 포커스
    document.getElementById('answerInput').focus();
}

async function submitAnswer() {
    // 라운드가 이미 종료되었으면 무시
    if (isRoundEnded) return;

    const answerInput = document.getElementById('answerInput');
    const userAnswer = answerInput.value.trim();

    if (!userAnswer) {
        alert('정답을 입력해주세요.');
        answerInput.focus();
        return;
    }

    if (!currentSong) return;

    // 실제 재생된 시간 계산 (초 단위)
    let answerTime = totalPlayTime;
    // 현재 재생 중이면 현재 재생 시간도 추가
    if (playStartTime !== null) {
        answerTime += (Date.now() - playStartTime) / 1000;
    }
    // 한 번도 재생하지 않았으면 null
    if (answerTime === 0 && playStartTime === null) {
        answerTime = null;
    }

    try {
        const response = await fetch('/game/solo/guess/answer', {
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
                // 라운드 종료 (정답 또는 3번 모두 실패)
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
                // 오답이지만 기회 남음
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
    if (isRoundEnded) return; // 이미 라운드 종료된 경우 무시

    isRoundEnded = true; // 스킵 시작 시 라운드 종료 플래그 설정
    stopAudio();

    try {
        const response = await fetch('/game/solo/guess/answer', {
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
            isRoundEnded = false; // 실패 시 플래그 복원
            alert(result.message);
        }
    } catch (error) {
        isRoundEnded = false; // 오류 시 플래그 복원
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

    // 정답 정보 표시
    document.getElementById('answerTitle').textContent = answerInfo.title;
    document.getElementById('answerArtist').textContent = answerInfo.artist;

    let meta = [];
    if (answerInfo.releaseYear) meta.push(answerInfo.releaseYear + '년');
    if (answerInfo.genre) meta.push(answerInfo.genre);
    document.getElementById('answerMeta').textContent = meta.join(' · ');

    // 버튼 텍스트
    if (isGameOver) {
        nextBtn.textContent = '결과 보기 🏆';
        nextBtn.onclick = function() { window.location.href = '/game/solo/guess/result'; };
    } else {
        nextBtn.textContent = '다음 라운드 →';
        nextBtn.onclick = nextRound;
    }

    modal.classList.add('show');
}

function nextRound() {
    document.getElementById('answerModal').classList.remove('show');

    if (currentRound < actualTotalRounds) {
        const nextRoundNumber = currentRound + 1;

        if (gameMode === 'GENRE_PER_ROUND') {
            showGenreSelectModal(nextRoundNumber);
        } else if (gameMode === 'ARTIST_PER_ROUND') {
            showArtistSelectModal(nextRoundNumber);
        } else if (gameMode === 'YEAR_PER_ROUND') {
            showYearSelectModal(nextRoundNumber);
        } else {
            loadRound(nextRoundNumber);
        }
    } else {
        window.location.href = '/game/solo/guess/result';
    }
}

async function quitGame() {
    if (!confirm('정말 게임을 종료하시겠습니까?')) return;

    try {
        await fetch('/game/solo/guess/end', { method: 'POST' });
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
        // 모달이 표시 중이면 무시
        const modal = document.getElementById('answerModal');
        if (modal && modal.classList.contains('show')) {
            e.preventDefault();
            return;
        }
        // 라운드가 종료되었으면 무시
        if (isRoundEnded) {
            e.preventDefault();
            return;
        }
        submitAnswer();
    }
});

// ========== 재생 실패 처리 ==========

/**
 * YouTube 재생 실패 시 처리
 * @param {object} errorInfo - 에러 정보 (code, message, isPlaybackError)
 */
function handlePlaybackError(errorInfo) {
    if (!currentSong) return;

    console.log('재생 실패 처리:', errorInfo);

    // 재생 불가 에러인 경우에만 처리
    if (errorInfo && errorInfo.isPlaybackError) {
        // 1. 자동 신고 (서버에 재생 불가 보고)
        reportUnplayableSong(currentSong.id, errorInfo.code);

        // 2. 에러 모달 표시
        showPlaybackErrorModal(errorInfo);
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
 * 재생 실패 모달 표시
 */
function showPlaybackErrorModal(errorInfo) {
    // 기존 모달이 있으면 제거
    let modal = document.getElementById('playbackErrorModal');
    if (modal) {
        modal.remove();
    }

    // 모달 생성
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

/**
 * 재생 불가로 인한 라운드 스킵 (정답 모달 없이 바로 다음으로)
 */
async function skipUnplayableRound() {
    // 에러 모달 닫기
    const modal = document.getElementById('playbackErrorModal');
    if (modal) {
        modal.remove();
    }

    if (!currentSong) return;
    if (isRoundEnded) return;

    isRoundEnded = true;
    stopAudio();

    try {
        const response = await fetch('/game/solo/guess/answer', {
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

            // 정답 모달 없이 바로 다음 라운드로
            if (result.isGameOver) {
                window.location.href = '/game/solo/guess/result';
            } else {
                const nextRoundNumber = currentRound + 1;
                if (gameMode === 'GENRE_PER_ROUND') {
                    showGenreSelectModal(nextRoundNumber);
                } else if (gameMode === 'ARTIST_PER_ROUND') {
                    showArtistSelectModal(nextRoundNumber);
                } else if (gameMode === 'YEAR_PER_ROUND') {
                    showYearSelectModal(nextRoundNumber);
                } else {
                    loadRound(nextRoundNumber);
                }
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