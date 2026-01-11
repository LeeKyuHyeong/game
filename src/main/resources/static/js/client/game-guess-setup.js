let maxAvailableSongs = 999;

// 초기 노래 개수 로딩
document.addEventListener('DOMContentLoaded', function() {
    updateSongCount();
    updateRankingNotice();

    // 로그인한 경우 닉네임 자동 입력
    if (typeof isLoggedIn !== 'undefined' && isLoggedIn && memberNickname) {
        document.getElementById('nickname').value = memberNickname;
    }

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
});

// 라운드 수 조절
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

// 프리셋 버튼으로 라운드 설정
function setRounds(value) {
    if (value > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }
    document.getElementById('totalRounds').value = value;
    updatePresetButtons();
    updateRankingNotice();
}

// 프리셋 버튼 활성화 상태 업데이트
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

// 라운드 수 유효성 검증
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

// 현재 라운드 수 가져오기
function getTotalRounds() {
    return parseInt(document.getElementById('totalRounds').value) || 10;
}

// 최고기록 랭킹 조건 안내 업데이트
function updateRankingNotice() {
    const rounds = getTotalRounds();
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;
    const loggedIn = typeof isLoggedIn !== 'undefined' && isLoggedIn;

    // 연도 범위 체크
    const yearFrom = document.getElementById('yearFrom').value;
    const yearTo = document.getElementById('yearTo').value;
    const noYearFilter = !yearFrom && !yearTo;

    // 아티스트 유형 체크
    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    const allArtistTypes = artistType === 'all';

    // 조건 체크
    const conditionLoginMet = loggedIn;
    const conditionModeMet = gameMode === 'RANDOM';
    const conditionRoundsMet = rounds >= 10;
    const conditionYearMet = noYearFilter;
    const conditionArtistMet = allArtistTypes;

    // UI 업데이트
    updateConditionUI('conditionLogin', conditionLoginMet);
    updateConditionUI('conditionMode', conditionModeMet);
    updateConditionUI('conditionRounds', conditionRoundsMet);
    updateConditionUI('conditionYear', conditionYearMet);
    updateConditionUI('conditionArtist', conditionArtistMet);

    // 결과 메시지
    const resultEl = document.getElementById('noticeResult');
    const allMet = conditionLoginMet && conditionModeMet && conditionRoundsMet && conditionYearMet && conditionArtistMet;

    if (allMet) {
        resultEl.textContent = '최고기록 랭킹에 등록됩니다!';
        resultEl.className = 'notice-result success';
    } else {
        const missing = [];
        if (!conditionLoginMet) missing.push('로그인');
        if (!conditionModeMet) missing.push('전체 랜덤 모드');
        if (!conditionRoundsMet) missing.push('10라운드 이상');
        if (!conditionYearMet) missing.push('연도 범위 미설정');
        if (!conditionArtistMet) missing.push('아티스트 유형 전체');
        resultEl.textContent = `조건 미충족: ${missing.join(', ')}`;
        resultEl.className = 'notice-result warning';
    }
}

function updateConditionUI(elementId, isMet) {
    const el = document.getElementById(elementId);
    const iconEl = el.querySelector('.condition-icon');

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

// 게임 모드 변경 이벤트
document.querySelectorAll('input[name="gameMode"]').forEach(radio => {
    radio.addEventListener('change', function() {
        const genreSelect = document.getElementById('genreSelect');
        const genreRoundSetting = document.getElementById('genreRoundSetting');

        if (this.value === 'FIXED_GENRE') {
            genreSelect.style.display = 'block';
            genreRoundSetting.style.display = 'none';
        } else if (this.value === 'GENRE_PER_ROUND') {
            genreSelect.style.display = 'none';
            genreRoundSetting.style.display = 'block';
        } else {
            genreSelect.style.display = 'none';
            genreRoundSetting.style.display = 'none';
        }
        updateSongCount();
        updateRankingNotice();
    });
});

// 장르 선택 변경 이벤트
document.getElementById('fixedGenreId').addEventListener('change', function() {
    updateSongCount();
});

// 연도 범위 변경 이벤트
document.getElementById('yearFrom').addEventListener('change', function() {
    updateSongCount();
    updateRankingNotice();
});
document.getElementById('yearTo').addEventListener('change', function() {
    updateSongCount();
    updateRankingNotice();
});

// 아티스트 유형 변경 이벤트
document.querySelectorAll('input[name="artistType"]').forEach(radio => {
    radio.addEventListener('change', function() {
        updateSongCount();
        updateRankingNotice();
    });
});

async function updateSongCount() {
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    let genreId = null;
    if (gameMode === 'FIXED_GENRE') {
        genreId = document.getElementById('fixedGenreId').value || null;
    }

    const yearFrom = document.getElementById('yearFrom').value || null;
    const yearTo = document.getElementById('yearTo').value || null;

    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    let soloOnly = null;
    let groupOnly = null;
    if (artistType === 'solo') soloOnly = true;
    if (artistType === 'group') groupOnly = true;

    try {
        const params = new URLSearchParams();
        if (genreId) params.append('genreId', genreId);
        if (yearFrom) params.append('yearFrom', yearFrom);
        if (yearTo) params.append('yearTo', yearTo);
        if (soloOnly) params.append('soloOnly', soloOnly);
        if (groupOnly) params.append('groupOnly', groupOnly);

        const response = await fetch(`/game/solo/guess/song-count?${params.toString()}`);
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

        // 프리셋 버튼 업데이트
        updatePresetButtons();

        // 현재 라운드가 최대치 초과시 조정
        const currentRounds = getTotalRounds();
        if (currentRounds > maxAvailableSongs && maxAvailableSongs > 0) {
            document.getElementById('totalRounds').value = maxAvailableSongs;
            updatePresetButtons();
            updateRankingNotice();
        }

    } catch (error) {
        console.error('노래 개수 조회 오류:', error);
    }
}

function goToStep1() {
    document.getElementById('step2').style.display = 'none';
    document.getElementById('step1').style.display = 'block';
}

function goToStep2() {
    // 닉네임 검증
    const nickname = document.getElementById('nickname').value.trim();
    if (!nickname) {
        alert('닉네임을 입력해주세요.');
        document.getElementById('nickname').focus();
        return;
    }

    // 게임 모드 검증
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            return;
        }
    }

    if (maxAvailableSongs === 0) {
        alert('현재 조건에 맞는 노래가 없습니다. 조건을 변경해주세요.');
        return;
    }

    const totalRounds = getTotalRounds();
    if (totalRounds > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }

    document.getElementById('step1').style.display = 'none';
    document.getElementById('step2').style.display = 'block';

    // step2로 이동할 때 랭킹 조건 다시 업데이트
    updateRankingNotice();
}

async function startGame() {
    const nickname = document.getElementById('nickname').value.trim();
    if (!nickname) {
        alert('닉네임을 입력해주세요.');
        goToStep1();
        document.getElementById('nickname').focus();
        return;
    }

    // 게임 모드
    const gameMode = document.querySelector('input[name="gameMode"]:checked').value;

    // 장르 체크 (FIXED_GENRE 모드일 때만)
    if (gameMode === 'FIXED_GENRE') {
        const genreId = document.getElementById('fixedGenreId').value;
        if (!genreId) {
            alert('장르를 선택해주세요.');
            goToStep1();
            return;
        }
    }

    const totalRounds = getTotalRounds();

    // 최종 라운드 수 검증
    if (totalRounds > maxAvailableSongs) {
        alert(`현재 조건에서 최대 ${maxAvailableSongs}라운드까지 가능합니다.`);
        return;
    }

    // 설정 수집
    const settings = {};

    const yearFrom = document.getElementById('yearFrom').value;
    const yearTo = document.getElementById('yearTo').value;
    if (yearFrom) settings.yearFrom = parseInt(yearFrom);
    if (yearTo) settings.yearTo = parseInt(yearTo);

    const artistType = document.querySelector('input[name="artistType"]:checked').value;
    if (artistType === 'solo') settings.soloOnly = true;
    if (artistType === 'group') settings.groupOnly = true;

    if (gameMode === 'FIXED_GENRE') {
        settings.fixedGenreId = parseInt(document.getElementById('fixedGenreId').value);
    }

    if (gameMode === 'GENRE_PER_ROUND') {
        settings.hideEmptyGenres = document.getElementById('hideEmptyGenres').checked;
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
            window.location.href = '/game/solo/guess/play';
        } else {
            alert(result.message || '게임 시작에 실패했습니다.');
        }
    } catch (error) {
        alert('게임 시작 중 오류가 발생했습니다.');
        console.error(error);
    }
}

// Enter 키 이벤트
document.getElementById('nickname').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') {
        goToStep2();
    }
});
