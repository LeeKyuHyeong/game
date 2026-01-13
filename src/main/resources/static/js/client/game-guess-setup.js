let maxAvailableSongs = 999;

// 선택된 연도/아티스트
let selectedYears = [];
let selectedArtists = [];
let allYears = [];
let allArtists = [];

// 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 로그인한 경우 닉네임 자동 입력
    if (typeof isLoggedIn !== 'undefined' && isLoggedIn && memberNickname) {
        document.getElementById('nickname').value = memberNickname;
    }

    // 데이터 로드
    loadYears();
    loadArtists();

    // 초기 노래 개수 및 랭킹 조건 업데이트
    updateSongCount();
    updateRankingNotice();

    // 라운드 입력 이벤트
    document.getElementById('totalRounds').addEventListener('change', function() {
        validateRounds();
        updatePresetButtons();
        updateRankingNotice();
    });

    document.getElementById('totalRounds').addEventListener('input', function() {
        updatePresetButtons();
        updateRankingNotice();
    });

    // 게임 모드 변경 이벤트
    document.querySelectorAll('input[name="gameMode"]').forEach(radio => {
        radio.addEventListener('change', handleGameModeChange);
    });

    // 장르 선택 변경 이벤트
    document.getElementById('fixedGenreId').addEventListener('change', updateSongCount);

    // 아티스트 검색 이벤트
    document.getElementById('artistSearchInput').addEventListener('input', function() {
        renderArtistChips(this.value);
    });

    // 아티스트 유형 변경 이벤트
    document.querySelectorAll('input[name="artistType"]').forEach(radio => {
        radio.addEventListener('change', function() {
            updateSongCount();
            updateRankingNotice();
        });
    });

    // Enter 키 이벤트
    document.getElementById('nickname').addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            startGame();
        }
    });
});

// ========== 게임 모드 처리 ==========

function handleGameModeChange() {
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    // 모든 선택 영역 숨기기
    document.getElementById('genreSelectArea').style.display = 'none';
    document.getElementById('artistSelectArea').style.display = 'none';
    document.getElementById('yearSelectArea').style.display = 'none';
    document.getElementById('perRoundOptions').style.display = 'none';

    // 선택 초기화
    selectedYears = [];
    selectedArtists = [];

    // 모드별 UI 표시
    switch (gameMode) {
        case 'FIXED_GENRE':
            document.getElementById('genreSelectArea').style.display = 'block';
            break;
        case 'FIXED_ARTIST':
            document.getElementById('artistSelectArea').style.display = 'block';
            document.getElementById('artistTypeArea').style.display = 'none'; // 아티스트 유형 숨김
            renderArtistChips();
            break;
        case 'FIXED_YEAR':
            document.getElementById('yearSelectArea').style.display = 'block';
            renderYearChips();
            break;
        case 'GENRE_PER_ROUND':
        case 'ARTIST_PER_ROUND':
        case 'YEAR_PER_ROUND':
            document.getElementById('perRoundOptions').style.display = 'block';
            break;
    }

    // 아티스트 고정 모드가 아니면 아티스트 유형 영역 표시
    if (gameMode !== 'FIXED_ARTIST') {
        document.getElementById('artistTypeArea').style.display = 'block';
    }

    updateSongCount();
    updateRankingNotice();
}

// ========== 라운드 설정 ==========

function adjustRounds(delta) {
    const input = document.getElementById('totalRounds');
    let value = parseInt(input.value) || 10;
    value = Math.max(1, Math.min(50, value + delta));

    if (value > maxAvailableSongs) {
        value = maxAvailableSongs;
    }

    input.value = value;
    updatePresetButtons();
    updateRankingNotice();
}

function setRounds(value) {
    if (value > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }
    document.getElementById('totalRounds').value = value;
    updatePresetButtons();
    updateRankingNotice();
}

function updatePresetButtons() {
    const currentValue = parseInt(document.getElementById('totalRounds').value) || 10;

    document.querySelectorAll('.preset-btn').forEach(btn => {
        const btnValue = parseInt(btn.textContent);
        btn.classList.remove('active');

        if (btnValue > maxAvailableSongs) {
            btn.classList.add('disabled');
            btn.disabled = true;
        } else {
            btn.classList.remove('disabled');
            btn.disabled = false;
            if (btnValue === currentValue) {
                btn.classList.add('active');
            }
        }
    });
}

function validateRounds() {
    const input = document.getElementById('totalRounds');
    let value = parseInt(input.value) || 10;

    if (value < 1) value = 1;
    if (value > 50) value = 50;
    if (value > maxAvailableSongs && maxAvailableSongs > 0) {
        value = maxAvailableSongs;
    }

    input.value = value;
}

function getTotalRounds() {
    return parseInt(document.getElementById('totalRounds').value) || 10;
}

// ========== 랭킹 조건 ==========

function updateRankingNotice() {
    const rounds = getTotalRounds();
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;
    const loggedIn = typeof isLoggedIn !== 'undefined' && isLoggedIn;
    const artistType = document.querySelector('input[name="artistType"]:checked').value;

    const conditionModeMet = gameMode === 'RANDOM';
    const conditionLoginMet = loggedIn;
    const conditionRoundsMet = rounds >= 10;
    // 아티스트 고정 모드에서는 아티스트 유형 조건 미충족
    const conditionArtistMet = gameMode === 'FIXED_ARTIST' ? false : artistType === 'all';

    // 전체 랜덤 모드가 아니면 다른 조건들 비활성화 표시
    updateConditionUI('conditionMode', conditionModeMet, false);
    updateConditionUI('conditionLogin', conditionLoginMet, !conditionModeMet);
    updateConditionUI('conditionRounds', conditionRoundsMet, !conditionModeMet);
    updateConditionUI('conditionArtist', conditionArtistMet, !conditionModeMet);

    const resultEl = document.getElementById('noticeResult');
    const allMet = conditionLoginMet && conditionModeMet && conditionRoundsMet && conditionArtistMet;

    if (allMet) {
        resultEl.textContent = '최고기록 랭킹에 등록됩니다!';
        resultEl.className = 'notice-result success';
    } else {
        const missing = [];
        if (!conditionModeMet) missing.push('전체 랜덤 모드');
        else {
            if (!conditionLoginMet) missing.push('로그인');
            if (!conditionRoundsMet) missing.push('10라운드 이상');
            if (!conditionArtistMet) missing.push('아티스트 유형 전체');
        }
        resultEl.textContent = `조건 미충족: ${missing.join(', ')}`;
        resultEl.className = 'notice-result warning';
    }
}

function updateConditionUI(elementId, isMet, disabled = false) {
    const el = document.getElementById(elementId);
    if (!el) return;
    const iconEl = el.querySelector('.condition-icon');

    // 비활성화 상태 처리
    if (disabled) {
        el.classList.add('disabled');
        el.classList.remove('met', 'unmet');
        iconEl.textContent = '−';
        return;
    }

    el.classList.remove('disabled');
    if (isMet) {
        el.classList.add('met');
        el.classList.remove('unmet');
        iconEl.textContent = '✓';
    } else {
        el.classList.add('unmet');
        el.classList.remove('met');
        iconEl.textContent = '○';
    }
}

// ========== 데이터 로드 ==========

async function loadYears() {
    try {
        const response = await fetch('/game/solo/guess/years');
        allYears = await response.json();
    } catch (error) {
        console.error('연도 목록 로드 오류:', error);
    }
}

async function loadArtists() {
    try {
        const response = await fetch('/game/solo/guess/artists');
        allArtists = await response.json();
    } catch (error) {
        console.error('아티스트 목록 로드 오류:', error);
    }
}

// ========== 연도 칩 ==========

function renderYearChips() {
    const container = document.getElementById('yearSelectChips');
    if (allYears.length === 0) {
        container.innerHTML = '<span class="chips-loading">등록된 연도가 없습니다</span>';
        return;
    }

    container.innerHTML = allYears.map(y => {
        const isSelected = selectedYears.includes(y.year);
        return `<div class="chip ${isSelected ? 'selected' : ''}" onclick="toggleYear(${y.year})">
            ${y.year}<span class="chip-count">(${y.count})</span>
        </div>`;
    }).join('');

    updateYearCountInfo();
}

function toggleYear(year) {
    const idx = selectedYears.indexOf(year);
    if (idx > -1) {
        selectedYears.splice(idx, 1);
    } else {
        selectedYears.push(year);
    }
    renderYearChips();
    updateSongCount();
}

function updateYearCountInfo() {
    const el = document.getElementById('yearSelectCountInfo');
    if (selectedYears.length > 0) {
        el.textContent = `(${selectedYears.length}개 선택)`;
    } else {
        el.textContent = '';
    }
}

// ========== 아티스트 칩 ==========

function renderArtistChips(filterKeyword = '') {
    const container = document.getElementById('artistSelectChips');
    let artistsToShow = allArtists;

    if (filterKeyword) {
        artistsToShow = allArtists.filter(a =>
            a.name.toLowerCase().includes(filterKeyword.toLowerCase())
        );
    }

    if (artistsToShow.length === 0) {
        container.innerHTML = '<span class="chips-loading">검색 결과가 없습니다</span>';
        return;
    }

    container.innerHTML = artistsToShow.map(a => {
        const isSelected = selectedArtists.includes(a.name);
        const escapedName = a.name.replace(/'/g, "\\'").replace(/"/g, '\\"');
        return `<div class="chip ${isSelected ? 'selected' : ''}" onclick="toggleArtist('${escapedName}')">
            ${a.name}<span class="chip-count">(${a.count})</span>
        </div>`;
    }).join('');

    updateArtistCountInfo();
}

function toggleArtist(name) {
    const idx = selectedArtists.indexOf(name);
    if (idx > -1) {
        selectedArtists.splice(idx, 1);
    } else {
        selectedArtists.push(name);
    }
    const keyword = document.getElementById('artistSearchInput').value;
    renderArtistChips(keyword);
    updateSongCount();
}

function updateArtistCountInfo() {
    const el = document.getElementById('artistSelectCountInfo');
    if (selectedArtists.length > 0) {
        el.textContent = `(${selectedArtists.length}명 선택)`;
    } else {
        el.textContent = '';
    }
}

// ========== 노래 개수 조회 ==========

async function updateSongCount() {
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    let soloOnly = null;
    let groupOnly = null;
    if (artistType === 'solo') soloOnly = true;
    if (artistType === 'group') groupOnly = true;

    try {
        const body = {};
        if (soloOnly) body.soloOnly = soloOnly;
        if (groupOnly) body.groupOnly = groupOnly;

        // 모드별 파라미터
        if (gameMode === 'FIXED_GENRE') {
            const genreId = document.getElementById('fixedGenreId').value;
            if (genreId) body.genreId = parseInt(genreId);
        } else if (gameMode === 'FIXED_ARTIST') {
            if (selectedArtists.length > 0) body.artists = selectedArtists;
        } else if (gameMode === 'FIXED_YEAR') {
            if (selectedYears.length > 0) body.years = selectedYears;
        }

        const response = await fetch('/game/solo/guess/song-count', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const result = await response.json();

        maxAvailableSongs = result.count;

        const infoEl = document.getElementById('songCountInfo');
        if (maxAvailableSongs === 0) {
            infoEl.textContent = '(사용 가능한 노래 없음)';
            infoEl.style.color = '#ef4444';
        } else {
            infoEl.textContent = `(최대 ${maxAvailableSongs}라운드)`;
            infoEl.style.color = '#22c55e';
        }

        updatePresetButtons();

        const currentRounds = getTotalRounds();
        if (maxAvailableSongs > 0) {
            if (currentRounds > maxAvailableSongs) {
                // 현재 라운드가 max보다 크면 줄임
                document.getElementById('totalRounds').value = maxAvailableSongs;
                updatePresetButtons();
                updateRankingNotice();
            } else if (currentRounds < maxAvailableSongs && currentRounds < 10) {
                // 현재 라운드가 max보다 작고 10 미만이면 자동으로 올림 (최대 10 또는 max)
                document.getElementById('totalRounds').value = Math.min(10, maxAvailableSongs);
                updatePresetButtons();
                updateRankingNotice();
            }
        }

    } catch (error) {
        console.error('노래 개수 조회 오류:', error);
    }
}

// ========== 게임 시작 ==========

async function startGame() {
    const nickname = document.getElementById('nickname').value.trim();
    if (!nickname) {
        alert('닉네임을 입력해주세요.');
        document.getElementById('nickname').focus();
        return;
    }

    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    // 모드별 유효성 검사
    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            return;
        }
    }

    if (gameMode === 'FIXED_ARTIST') {
        if (selectedArtists.length === 0) {
            alert('아티스트를 선택해주세요.');
            return;
        }
    }

    if (gameMode === 'FIXED_YEAR') {
        if (selectedYears.length === 0) {
            alert('연도를 선택해주세요.');
            return;
        }
    }

    // 매 라운드 선택 모드가 아닌 경우 노래 개수 확인
    if (!gameMode.includes('PER_ROUND') && maxAvailableSongs === 0) {
        alert('현재 조건에 맞는 노래가 없습니다. 조건을 변경해주세요.');
        return;
    }

    const totalRounds = getTotalRounds();

    if (!gameMode.includes('PER_ROUND') && totalRounds > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }

    // 설정 수집
    const settings = {};

    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    if (artistType === 'solo') settings.soloOnly = true;
    if (artistType === 'group') settings.groupOnly = true;

    if (gameMode === 'FIXED_GENRE') {
        settings.fixedGenreId = parseInt(document.getElementById('fixedGenreId').value);
    }

    if (gameMode === 'FIXED_ARTIST') {
        settings.selectedArtists = selectedArtists;
    }

    if (gameMode === 'FIXED_YEAR') {
        settings.selectedYears = selectedYears;
    }

    if (gameMode.includes('PER_ROUND')) {
        settings.hideEmptyOptions = document.getElementById('hideEmptyOptions').checked;
    }

    // 서버로 전송
    try {
        const response = await fetch('/game/solo/guess/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                nickname: nickname,
                totalRounds: totalRounds,
                gameMode: gameMode,
                settings: settings
            })
        });

        const result = await response.json();

        if (result.success) {
            // 라운드 축소 정보 저장 (play 페이지에서 표시)
            if (result.reducedCount && result.reducedCount > 0) {
                sessionStorage.setItem('roundsReduced', JSON.stringify({
                    requested: result.requestedRounds,
                    actual: result.actualRounds,
                    reduced: result.reducedCount
                }));
            }
            window.location.href = '/game/solo/guess/play';
        } else {
            alert(result.message || '게임 시작에 실패했습니다.');
        }
    } catch (error) {
        alert('게임 시작 중 오류가 발생했습니다.');
        console.error(error);
    }
}
