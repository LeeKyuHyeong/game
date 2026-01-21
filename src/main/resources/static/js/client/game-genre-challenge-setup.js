// 장르 챌린지 설정 페이지 JavaScript

let selectedGenre = null;
let genreList = [];
let selectedDifficulty = 'NORMAL';

// 난이도별 설정
const DIFFICULTY_CONFIG = {
    NORMAL: { totalTime: 10, lives: 5, ranked: false, icon: '⭐' },
    HARDCORE: { totalTime: 5, lives: 5, ranked: true, icon: '🔥' }
};

// 최소 곡 수 (서버와 동일하게 10곡)
const MIN_SONG_COUNT = 10;

document.addEventListener('DOMContentLoaded', function() {
    // 닉네임 초기값 설정
    if (typeof memberNickname !== 'undefined' && memberNickname) {
        document.getElementById('nickname').value = memberNickname;
    }

    // 장르 목록 로드
    loadGenres();

    // 초기 난이도 규칙 표시
    updateRulesDisplay();

    // 초기 버튼 상태 설정
    updateStartButton();
});

async function loadGenres() {
    try {
        const response = await fetch('/game/genre-challenge/genres');
        if (!response.ok) throw new Error('장르 목록 로드 실패');

        genreList = await response.json();

        const container = document.getElementById('genreList');
        container.innerHTML = '';

        if (genreList.length === 0) {
            container.innerHTML = '<div class="no-genres">도전 가능한 장르가 없습니다.</div>';
            return;
        }

        genreList.forEach(genre => {
            const item = document.createElement('div');
            item.className = 'genre-item';
            item.innerHTML = `
                <span class="genre-name">${genre.name}</span>
                <span class="song-count">${genre.count}곡</span>
            `;
            item.onclick = () => selectGenre(genre.code, genre.name, genre.count);
            container.appendChild(item);
        });
    } catch (error) {
        console.error('장르 로드 오류:', error);
    }
}

function selectGenre(code, name, count) {
    selectedGenre = { code, name, count };

    // 선택된 장르 정보 표시
    document.getElementById('selectedGenreName').textContent = name;
    document.getElementById('selectedGenreCount').textContent = `${count}곡 전체 도전`;

    // 선택 영역 숨기고 선택 완료 영역 표시
    document.getElementById('genreSelectArea').style.display = 'none';
    document.getElementById('selectedGenreArea').style.display = 'block';

    // 장르 챌린지 정보 로드 (내 기록 + 1위 기록)
    loadGenreChallengeInfo(code);

    updateStartButton();
}

// 장르 챌린지 정보 로드 (하드코어 기준)
async function loadGenreChallengeInfo(genreCode) {
    const infoContainer = document.getElementById('genreRecordInfo');
    const myRecordInfo = document.getElementById('myRecordInfo');
    const topRecordInfo = document.getElementById('topRecordInfo');
    const noRecordInfo = document.getElementById('noRecordInfo');

    // 초기화
    myRecordInfo.style.display = 'none';
    topRecordInfo.style.display = 'none';
    noRecordInfo.style.display = 'none';

    try {
        const response = await fetch(`/game/genre-challenge/info/${encodeURIComponent(genreCode)}`);
        if (!response.ok) throw new Error('정보 로드 실패');

        const data = await response.json();

        // 내 기록 표시
        if (data.myRecord) {
            const myRecord = data.myRecord;
            const timeText = myRecord.bestTimeMs ? ` ${(myRecord.bestTimeMs / 1000).toFixed(1)}초` : '';
            const comboText = myRecord.maxCombo ? ` 콤보 ${myRecord.maxCombo}` : '';
            document.getElementById('myRecordValue').innerHTML =
                `<span class="score">${myRecord.correctCount}/${myRecord.totalSongs}</span>` +
                `<span class="combo">${comboText}</span>` +
                `<span class="time">${timeText}</span>`;
            myRecordInfo.style.display = 'flex';
        }

        // 1위 기록 표시
        if (data.topRecord) {
            const top = data.topRecord;
            const timeText = top.bestTimeMs ? ` ${(top.bestTimeMs / 1000).toFixed(1)}초` : '';
            const comboText = top.maxCombo ? ` 콤보 ${top.maxCombo}` : '';
            document.getElementById('topRecordValue').innerHTML =
                `<span class="nickname">${top.nickname}</span>` +
                `<span class="score">${top.correctCount}/${top.totalSongs}</span>` +
                `<span class="combo">${comboText}</span>` +
                `<span class="time">${timeText}</span>`;
            topRecordInfo.style.display = 'flex';
        } else {
            // 기록이 없을 때
            noRecordInfo.style.display = 'block';
        }

        infoContainer.style.display = 'block';

    } catch (error) {
        console.error('장르 챌린지 정보 로드 오류:', error);
    }
}

function clearSelectedGenre() {
    selectedGenre = null;

    // 선택 완료 영역 숨기고 선택 영역 다시 표시
    document.getElementById('selectedGenreArea').style.display = 'none';
    document.getElementById('genreSelectArea').style.display = 'block';

    updateStartButton();
}

function updateStartButton() {
    const startBtn = document.getElementById('startBtn');

    if (selectedGenre) {
        const config = DIFFICULTY_CONFIG[selectedDifficulty];
        const modeText = config.ranked ? '🏆 공식' : '📝 연습';
        startBtn.textContent = `${selectedGenre.name} 도전 시작! (${selectedGenre.count}곡) ${modeText}`;
    } else {
        startBtn.textContent = '도전 시작!';
    }
}

// 난이도 선택
function selectDifficulty(difficulty) {
    selectedDifficulty = difficulty;

    // 버튼 상태 업데이트
    document.querySelectorAll('.difficulty-btn').forEach(btn => {
        btn.classList.remove('selected');
        if (btn.dataset.difficulty === difficulty) {
            btn.classList.add('selected');
        }
    });

    // 규칙 표시 업데이트
    updateRulesDisplay();
    updateStartButton();
}

// 규칙 표시 업데이트
function updateRulesDisplay() {
    const config = DIFFICULTY_CONFIG[selectedDifficulty];
    const rulesList = document.getElementById('rulesList');

    let rulesHtml = `
        <li><span class="rule-icon">🎵</span> 해당 장르의 <strong>전곡</strong> 출제</li>
        <li><span class="rule-icon">⏱</span> <strong>${config.totalTime}초</strong> 안에 듣고 맞추기</li>
        <li><span class="rule-icon">❤</span> 라이프 <strong>${config.lives}개</strong> (오답/시간초과 시 -1)</li>
        <li><span class="rule-icon">🔥</span> <strong>콤보</strong> 시스템! 연속 정답을 노려라</li>
    `;

    if (config.ranked) {
        rulesHtml += `<li><span class="rule-icon">🏆</span> <strong>공식 랭킹</strong> 반영</li>`;
    } else {
        rulesHtml += `<li><span class="rule-icon">📝</span> 연습 모드 (랭킹 미반영)</li>`;
    }

    rulesList.innerHTML = rulesHtml;
}

async function startGame() {
    const nicknameInput = document.getElementById('nickname');
    const nickname = nicknameInput.value.trim();

    if (!nickname) {
        showToast('닉네임을 입력해주세요');
        nicknameInput.focus();
        return;
    }

    if (!selectedGenre) {
        showToast('장르를 선택해주세요');
        return;
    }

    const startBtn = document.getElementById('startBtn');
    startBtn.disabled = true;
    startBtn.textContent = '게임 시작 중...';

    try {
        const response = await fetch('/game/genre-challenge/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                nickname: nickname,
                genreCode: selectedGenre.code,
                difficulty: selectedDifficulty
            })
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = '/game/genre-challenge/play';
        } else {
            showToast(result.message || '게임 시작에 실패했습니다');
            startBtn.disabled = false;
            updateStartButton();
        }
    } catch (error) {
        console.error('게임 시작 오류:', error);
        showToast('게임 시작 중 오류가 발생했습니다');
        startBtn.disabled = false;
        updateStartButton();
    }
}
