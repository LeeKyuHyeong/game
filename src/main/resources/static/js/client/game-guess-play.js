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

// 게임 시작
document.addEventListener('DOMContentLoaded', function() {
    // GENRE_PER_ROUND 모드면 장르 선택 모달 표시
    if (gameMode === 'GENRE_PER_ROUND') {
        showGenreSelectModal(1);
    } else {
        loadRound(1);
    }
});

async function showGenreSelectModal(roundNumber) {
    const modal = document.getElementById('genreSelectModal');
    const genreList = document.getElementById('genreList');

    try {
        const response = await fetch('/game/solo/guess/genres-with-count');
        const genres = await response.json();

        genreList.innerHTML = '';

        genres.forEach(genre => {
            const item = document.createElement('div');
            item.className = 'genre-item';
            if (genre.availableCount === 0) {
                item.classList.add('disabled');
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

        // 오디오 설정 - 0초부터 시작
        if (currentSong && currentSong.filePath) {
            audioPlayer.src = `/uploads/songs/${currentSong.filePath}`;
            audioPlayer.currentTime = 0;

            audioPlayer.onloadedmetadata = function() {
                updateTimeDisplay();
            };
        }

        // UI 리셋
        resetUI();

        // 입력창 포커스
        document.getElementById('answerInput').focus();

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
    audioPlayer.currentTime = 0;
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

    const duration = currentSong.playDuration || 10;
    const currentTime = audioPlayer.currentTime;
    const progress = Math.min((currentTime / duration) * 100, 100);

    document.getElementById('progressBar').style.width = progress + '%';
    updateTimeDisplay();

    if (currentTime >= duration) {
        pauseAudio();
    }
}

function updateTimeDisplay() {
    const duration = currentSong ? (currentSong.playDuration || 10) : 0;
    const currentTime = Math.max(0, audioPlayer.currentTime);

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
    document.getElementById('answerInput').value = '';
}

async function submitAnswer() {
    const answerInput = document.getElementById('answerInput');
    const userAnswer = answerInput.value.trim();

    if (!userAnswer) {
        alert('정답을 입력해주세요.');
        answerInput.focus();
        return;
    }

    if (!currentSong) return;

    stopAudio();

    try {
        const response = await fetch('/game/solo/guess/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                roundNumber: currentRound,
                answer: userAnswer,
                isSkip: false
            })
        });

        const result = await response.json();

        if (result.success) {
            if (result.isCorrect) {
                score += 100;
                correctCount++;
                document.getElementById('currentScore').textContent = score;
                document.getElementById('correctCount').textContent = correctCount;
            } else {
                wrongCount++;
                document.getElementById('wrongCount').textContent = wrongCount;
            }

            showAnswerModal(result.isCorrect, userAnswer, result.answer, result.isGameOver);
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
            alert(result.message);
        }
    } catch (error) {
        console.error('스킵 오류:', error);
    }
}

function showAnswerModal(isCorrect, userAnswer, answerInfo, isGameOver, isSkip = false) {
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
        userAnswerInfo.innerHTML = `<span class="correct-text">+100점!</span>`;
    } else {
        header.textContent = '❌ 오답';
        header.className = 'answer-header wrong';
        userAnswerInfo.innerHTML = `<span class="wrong-text">내 답: ${userAnswer}</span>`;
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
        submitAnswer();
    }
});