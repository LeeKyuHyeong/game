// 아티스트 챌린지 설정 페이지 JavaScript

let selectedArtist = null;
let artistList = [];
let searchTimeout = null;
let selectedDifficulty = 'NORMAL';
let selectedStage = 1;
let availableStages = [];

// 난이도별 설정
const DIFFICULTY_CONFIG = {
    NORMAL: { playTime: 7, answerTime: 6, lives: 3, hint: false, ranked: false, icon: '⭐' },
    HARDCORE: { playTime: 5, answerTime: 5, lives: 3, hint: false, ranked: true, icon: '🔥' }
};

// 챌린지 곡 수 (서버와 동일하게 20곡 고정)
const CHALLENGE_SONG_COUNT = 20;

document.addEventListener('DOMContentLoaded', function() {
    // 닉네임 초기값 설정
    if (typeof memberNickname !== 'undefined' && memberNickname) {
        document.getElementById('nickname').value = memberNickname;
    }

    // 아티스트 목록 로드
    loadArtists();

    // 검색 이벤트 설정
    const searchInput = document.getElementById('artistSearch');
    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            searchArtists(this.value);
        }, 300);
    });

    // 검색창 포커스 아웃 시 결과 숨기기
    searchInput.addEventListener('blur', function() {
        setTimeout(() => {
            document.getElementById('artistSearchResults').style.display = 'none';
        }, 200);
    });

    // 검색창 포커스 시 결과 표시
    searchInput.addEventListener('focus', function() {
        if (this.value.trim()) {
            searchArtists(this.value);
        }
    });

    // 초기 난이도 규칙 표시
    updateRulesDisplay();

    // 초기 버튼 상태 설정 (닉네임 자동입력 시에도 버튼 상태 갱신)
    updateStartButton();
});

async function loadArtists() {
    try {
        const response = await fetch('/game/fan-challenge/artists');
        if (!response.ok) throw new Error('아티스트 목록 로드 실패');

        artistList = await response.json();

        // 곡 수가 많은 순으로 정렬 후 상위 20개 표시
        const sortedArtists = [...artistList].sort((a, b) => b.count - a.count).slice(0, 20);

        const container = document.getElementById('artistList');
        container.innerHTML = '';

        if (sortedArtists.length === 0) {
            container.innerHTML = '<div class="no-results">도전 가능한 아티스트가 없습니다.<br><small>20곡 이상 보유한 아티스트가 필요합니다.</small></div>';
            return;
        }

        sortedArtists.forEach(artist => {
            const item = document.createElement('div');
            item.className = 'artist-item';
            const nameSpan = document.createElement('span');
            nameSpan.className = 'artist-name';
            nameSpan.textContent = artist.name;
            const countSpan = document.createElement('span');
            countSpan.className = 'song-count';
            countSpan.textContent = artist.count + '곡';
            item.appendChild(nameSpan);
            item.appendChild(countSpan);
            item.onclick = () => selectArtist(artist.name, artist.count);
            container.appendChild(item);
        });
    } catch (error) {
        // console.error('아티스트 로드 오류:', error);
    }
}

async function searchArtists(keyword) {
    const resultsContainer = document.getElementById('artistSearchResults');

    if (!keyword.trim()) {
        resultsContainer.style.display = 'none';
        return;
    }

    try {
        let filtered;

        if (artistList.length > 0) {
            // 로컬 필터링 (이미 로드된 목록에서)
            filtered = artistList.filter(a =>
                a.name.toLowerCase().includes(keyword.toLowerCase())
            ).slice(0, 10);
        } else {
            // 로컬 목록이 비어있으면 서버 API 사용
            const response = await fetch(`/game/fan-challenge/artists/search?keyword=${encodeURIComponent(keyword)}`);
            if (!response.ok) throw new Error('검색 실패');
            const artists = await response.json();
            filtered = artists.slice(0, 10).map(name => ({ name, count: null }));
        }

        resultsContainer.innerHTML = '';

        if (filtered.length === 0) {
            const noResult = document.createElement('div');
            noResult.className = 'no-results';
            noResult.textContent = '검색 결과가 없습니다';
            resultsContainer.appendChild(noResult);
        } else {
            filtered.forEach(artist => {
                const item = document.createElement('div');
                item.className = 'search-result-item';
                const nameSpan = document.createElement('span');
                nameSpan.className = 'artist-name';
                nameSpan.textContent = artist.name;
                const countSpan = document.createElement('span');
                countSpan.className = 'song-count';
                countSpan.textContent = artist.count != null ? artist.count + '곡' : '';
                item.appendChild(nameSpan);
                item.appendChild(countSpan);
                item.onclick = () => {
                    selectArtist(artist.name, artist.count || 0);
                    resultsContainer.style.display = 'none';
                    document.getElementById('artistSearch').value = '';
                };
                resultsContainer.appendChild(item);
            });
        }

        resultsContainer.style.display = 'block';
    } catch (error) {
        // console.error('아티스트 검색 오류:', error);
    }
}

function selectArtist(name, count) {
    selectedArtist = { name, count };

    // 선택된 아티스트 정보 표시
    document.getElementById('selectedArtistName').textContent = name;
    document.getElementById('selectedArtistCount').textContent = `보유 ${count}곡`;

    // 선택 영역 숨기고 선택 완료 영역 표시
    document.getElementById('artistSelectArea').style.display = 'none';
    document.getElementById('selectedArtistArea').style.display = 'block';

    // 검색 결과 숨기기
    document.getElementById('artistSearchResults').style.display = 'none';
    document.getElementById('artistSearch').value = '';

    // HARDCORE일 경우 단계 로드
    if (selectedDifficulty === 'HARDCORE') {
        loadStagesForArtist(name, count);
        document.getElementById('stageSelectArea').style.display = 'block';
    }

    // 아티스트 챌린지 정보 로드 (내 기록 + 1위 기록) - 현재 선택된 단계 기준
    loadArtistChallengeInfo(name, selectedStage);

    updateStartButton();
}

// 아티스트 챌린지 정보 로드 (하드코어 기준, 단계별)
async function loadArtistChallengeInfo(artist, stageLevel = 1) {
    const infoContainer = document.getElementById('artistRecordInfo');
    const myRecordInfo = document.getElementById('myRecordInfo');
    const topRecordInfo = document.getElementById('topRecordInfo');
    const noRecordInfo = document.getElementById('noRecordInfo');

    // 초기화
    myRecordInfo.style.display = 'none';
    topRecordInfo.style.display = 'none';
    noRecordInfo.style.display = 'none';

    try {
        const response = await fetch(`/game/fan-challenge/info/${encodeURIComponent(artist)}?stageLevel=${stageLevel}`);
        if (!response.ok) throw new Error('정보 로드 실패');

        const data = await response.json();

        // 내 기록 표시
        if (data.myRecord) {
            const myRecord = data.myRecord;
            const container = document.getElementById('myRecordValue');
            container.innerHTML = '';
            const scoreSpan = document.createElement('span');
            scoreSpan.className = 'score';
            scoreSpan.textContent = myRecord.correctCount + '/' + myRecord.totalSongs;
            container.appendChild(scoreSpan);
            if (myRecord.bestTimeMs) {
                const timeSpan = document.createElement('span');
                timeSpan.className = 'time';
                timeSpan.textContent = ' ' + (myRecord.bestTimeMs / 1000).toFixed(1) + '초';
                container.appendChild(timeSpan);
            }
            if (myRecord.isPerfectClear) {
                const perfectSpan = document.createElement('span');
                perfectSpan.className = 'perfect-badge';
                perfectSpan.textContent = ' PERFECT';
                container.appendChild(perfectSpan);
            }
            myRecordInfo.style.display = 'flex';
        }

        // 1위 기록 표시
        if (data.topRecord) {
            const top = data.topRecord;
            const container = document.getElementById('topRecordValue');
            container.innerHTML = '';
            const nickSpan = document.createElement('span');
            nickSpan.className = 'nickname';
            nickSpan.textContent = top.nickname;
            container.appendChild(nickSpan);
            const scoreSpan = document.createElement('span');
            scoreSpan.className = 'score';
            scoreSpan.textContent = top.correctCount + '/' + top.totalSongs;
            container.appendChild(scoreSpan);
            if (top.bestTimeMs) {
                const timeSpan = document.createElement('span');
                timeSpan.className = 'time';
                timeSpan.textContent = ' ' + (top.bestTimeMs / 1000).toFixed(1) + '초';
                container.appendChild(timeSpan);
            }
            if (top.isPerfectClear) {
                const perfectSpan = document.createElement('span');
                perfectSpan.className = 'perfect-badge';
                perfectSpan.textContent = ' PERFECT';
                container.appendChild(perfectSpan);
            }
            topRecordInfo.style.display = 'flex';
        } else {
            // 기록이 없을 때
            noRecordInfo.style.display = 'block';
        }

        infoContainer.style.display = 'block';

    } catch (error) {
        // console.error('아티스트 챌린지 정보 로드 오류:', error);
    }
}

function clearSelectedArtist() {
    selectedArtist = null;

    // 선택 완료 영역 숨기고 선택 영역 다시 표시
    document.getElementById('selectedArtistArea').style.display = 'none';
    document.getElementById('artistSelectArea').style.display = 'block';

    updateStartButton();
}

function updateStartButton() {
    const startBtn = document.getElementById('startBtn');

    if (selectedArtist) {
        const config = DIFFICULTY_CONFIG[selectedDifficulty];
        const modeText = config.ranked ? '🏆 공식' : '📝 연습';

        // 현재 선택된 단계의 곡 수 결정
        let songCount = CHALLENGE_SONG_COUNT;
        let stageText = '';
        if (selectedDifficulty === 'HARDCORE' && availableStages.length > 0) {
            const currentStage = availableStages.find(s => s.level === selectedStage);
            if (currentStage) {
                songCount = currentStage.requiredSongs;
                stageText = ` ${currentStage.emoji}${currentStage.name}`;
            }
        }

        startBtn.textContent = `${selectedArtist.name}${stageText} 도전 시작! (${songCount}곡) ${modeText}`;
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

    // HARDCORE 선택 시 단계 선택 영역 표시
    const stageSelectArea = document.getElementById('stageSelectArea');
    if (difficulty === 'HARDCORE' && selectedArtist) {
        loadStagesForArtist(selectedArtist.name, selectedArtist.count);
        stageSelectArea.style.display = 'block';
    } else {
        stageSelectArea.style.display = 'none';
        selectedStage = 1; // NORMAL은 항상 1단계
    }

    // 규칙 표시 업데이트
    updateRulesDisplay();
    updateStartButton();
}

// 아티스트별 도전 가능한 단계 로드
async function loadStagesForArtist(artist, songCount) {
    document.getElementById('artistSongCount').textContent = songCount;
    const stageList = document.getElementById('stageList');
    stageList.innerHTML = '<div class="loading">단계 정보 로딩 중...</div>';

    try {
        const response = await fetch(`/game/fan-challenge/stages/${encodeURIComponent(artist)}`);
        if (!response.ok) throw new Error('단계 정보 로드 실패');

        availableStages = await response.json();

        if (availableStages.length === 0) {
            stageList.innerHTML = '<div class="no-stages">활성화된 단계가 없습니다.</div>';
            return;
        }

        stageList.innerHTML = availableStages.map(stage => `
            <button type="button" class="stage-btn ${stage.level === selectedStage ? 'selected' : ''} ${stage.available ? '' : 'disabled'}"
                    data-level="${stage.level}"
                    ${stage.available ? `onclick="selectStage(${stage.level})"` : 'disabled'}>
                <span class="stage-emoji">${stage.emoji}</span>
                <span class="stage-name">${stage.name}</span>
                <span class="stage-songs">${stage.requiredSongs}곡</span>
                ${stage.available ? '' : '<span class="stage-locked">🔒 곡 부족</span>'}
            </button>
        `).join('');

        // 도전 가능한 최대 단계 자동 선택
        const maxAvailable = availableStages.filter(s => s.available).pop();
        if (maxAvailable && !availableStages.find(s => s.level === selectedStage && s.available)) {
            selectStage(maxAvailable.level);
        }

    } catch (error) {
        stageList.innerHTML = '<div class="error">단계 정보 로드 실패</div>';
    }
}

// 단계 선택
function selectStage(level) {
    selectedStage = level;

    // 버튼 상태 업데이트
    document.querySelectorAll('.stage-btn').forEach(btn => {
        btn.classList.remove('selected');
        if (parseInt(btn.dataset.level) === level) {
            btn.classList.add('selected');
        }
    });

    // 해당 단계의 랭킹 정보 다시 로드
    if (selectedArtist) {
        loadArtistChallengeInfo(selectedArtist.name, level);
    }

    // 규칙 표시 업데이트
    updateRulesDisplay();
    updateStartButton();
}

// 규칙 표시 업데이트
function updateRulesDisplay() {
    const config = DIFFICULTY_CONFIG[selectedDifficulty];
    const rulesList = document.getElementById('rulesList');

    // 현재 선택된 단계의 곡 수 결정
    let songCount = CHALLENGE_SONG_COUNT;
    if (selectedDifficulty === 'HARDCORE' && availableStages.length > 0) {
        const currentStage = availableStages.find(s => s.level === selectedStage);
        if (currentStage) {
            songCount = currentStage.requiredSongs;
        }
    }

    let rulesHtml = `
        <li><span class="rule-icon">🎵</span> 해당 아티스트의 <strong>랜덤 ${songCount}곡</strong> 출제</li>
        <li><span class="rule-icon">⏱</span> <strong>${config.playTime}초</strong> 듣기 + <strong>${config.answerTime}초</strong> 입력</li>
        <li><span class="rule-icon">❤</span> 라이프 <strong>${config.lives}개</strong> (오답/시간초과 시 -1)</li>
        <li><span class="rule-icon">🚫</span> 스킵 <strong>불가능</strong></li>
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

    if (!selectedArtist) {
        showToast('아티스트를 선택해주세요');
        document.getElementById('artistSearch').focus();
        return;
    }

    const startBtn = document.getElementById('startBtn');
    startBtn.disabled = true;
    startBtn.textContent = '게임 시작 중...';

    try {
        const requestBody = {
            nickname: nickname,
            artist: selectedArtist.name,
            difficulty: selectedDifficulty
        };

        // HARDCORE 모드일 때만 stageLevel 전송
        if (selectedDifficulty === 'HARDCORE') {
            requestBody.stageLevel = selectedStage;
        }

        const response = await fetch('/game/fan-challenge/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = '/game/fan-challenge/play';
        } else {
            showToast(result.message || '게임 시작에 실패했습니다');
            startBtn.disabled = false;
            updateStartButton();
        }
    } catch (error) {
        // console.error('게임 시작 오류:', error);
        showToast('게임 시작 중 오류가 발생했습니다');
        startBtn.disabled = false;
        updateStartButton();
    }
}
