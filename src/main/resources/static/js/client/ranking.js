/**
 * client/ranking.html - 전체 랭킹
 */

let currentTab = 'tier';      // tier, best30, retro, fanChallenge, genreChallenge, stats
let best30Period = 'weekly';  // weekly, monthly, alltime
let retroPeriod = 'score';    // score, best30, weekly
let fanChallengePeriod = 'perfect';  // perfect, artist
let selectedGenreCode = '';   // 선택된 장르 코드
let genreList = [];           // 장르 목록 캐시
let statsType = 'score';      // score, participation, avgScorePerRound, accuracyMin10
let participationSubType = 'games';  // games, rounds (서브탭 선택)
let showAllBest30 = false;

document.addEventListener('DOMContentLoaded', function() {
    loadGenreList();  // 장르 목록 미리 로드
    loadRanking();
    setupTabs();
    setupSubTabs();
});

// 장르 목록 로드
async function loadGenreList() {
    try {
        const response = await fetch('/api/ranking/genre-challenge/genres');
        genreList = await response.json();

        const dropdown = document.getElementById('genreSelectDropdown');
        if (dropdown && genreList.length > 0) {
            dropdown.innerHTML = '<option value="">장르를 선택하세요</option>' +
                genreList.map(g => `<option value="${g.code}">${g.name}</option>`).join('');
        }
    } catch (error) {
        // 장르 목록 로드 실패
    }
}

function setupTabs() {
    // 메인 탭 (PC/태블릿)
    document.querySelectorAll('.mode-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            switchTab(this.dataset.mode);
        });
    });

    // 모바일 select
    const mobileSelect = document.getElementById('mobileTabSelect');
    if (mobileSelect) {
        mobileSelect.addEventListener('change', function() {
            switchTab(this.value);
        });
    }
}

// 탭 전환 공통 함수
function switchTab(mode) {
    currentTab = mode;

    // PC 탭 버튼 active 상태 동기화
    document.querySelectorAll('.mode-tab').forEach(t => t.classList.remove('active'));
    const activeTab = document.querySelector(`.mode-tab[data-mode="${mode}"]`);
    if (activeTab) activeTab.classList.add('active');

    // 모바일 select 동기화
    const mobileSelect = document.getElementById('mobileTabSelect');
    if (mobileSelect) mobileSelect.value = mode;

    // 서브탭 초기화
    if (currentTab === 'best30') {
        best30Period = 'weekly';
        document.querySelectorAll('#best30PeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
        document.querySelector('#best30PeriodTabs .period-tab[data-period="weekly"]').classList.add('active');
        showAllBest30 = false;
    }

    if (currentTab === 'retro') {
        retroPeriod = 'score';
        document.querySelectorAll('#retroPeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
        document.querySelector('#retroPeriodTabs .period-tab[data-period="score"]').classList.add('active');
    }

    if (currentTab === 'fanChallenge') {
        fanChallengePeriod = 'perfect';
        document.querySelectorAll('#fanChallengePeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
        document.querySelector('#fanChallengePeriodTabs .period-tab[data-period="perfect"]').classList.add('active');
    }

    if (currentTab === 'genreChallenge') {
        selectedGenreCode = '';
        const dropdown = document.getElementById('genreSelectDropdown');
        if (dropdown) dropdown.value = '';
    }

    if (currentTab === 'stats') {
        statsType = 'score';
        participationSubType = 'games';
        document.querySelectorAll('.stats-type-tabs .period-tab').forEach(t => t.classList.remove('active'));
        document.querySelector('.stats-type-tabs .period-tab[data-stats-type="score"]').classList.add('active');
        document.getElementById('participationSubTabs').style.display = 'none';
        document.querySelectorAll('#participationSubTabs .sub-tab').forEach(t => t.classList.remove('active'));
        document.querySelector('#participationSubTabs .sub-tab[data-sub-type="games"]').classList.add('active');
    }

    updateTabsVisibility();
    loadRanking();
}

function setupSubTabs() {

    // 30개 챌린지 기간 탭
    document.querySelectorAll('#best30PeriodTabs .period-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            if (currentTab !== 'best30') return;
            document.querySelectorAll('#best30PeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            best30Period = this.dataset.period;
            showAllBest30 = false;
            loadRanking();
        });
    });

    // 레트로 기간 탭
    document.querySelectorAll('#retroPeriodTabs .period-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            if (currentTab !== 'retro') return;
            document.querySelectorAll('#retroPeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            retroPeriod = this.dataset.period;
            loadRanking();
        });
    });

    // 팬 챌린지 기간 탭
    document.querySelectorAll('#fanChallengePeriodTabs .period-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            if (currentTab !== 'fanChallenge') return;
            document.querySelectorAll('#fanChallengePeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            fanChallengePeriod = this.dataset.period;
            loadRanking();
        });
    });

    // 장르 챌린지 장르 선택 드롭다운
    const genreDropdown = document.getElementById('genreSelectDropdown');
    if (genreDropdown) {
        genreDropdown.addEventListener('change', function() {
            if (currentTab !== 'genreChallenge') return;
            selectedGenreCode = this.value;
            loadRanking();
        });
    }

    // 통계 유형 탭
    document.querySelectorAll('.stats-type-tabs .period-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            if (currentTab !== 'stats') return;
            document.querySelectorAll('.stats-type-tabs .period-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            statsType = this.dataset.statsType;

            // 최다 참여 서브탭 표시/숨김
            const subTabsContainer = document.getElementById('participationSubTabs');
            if (statsType === 'participation') {
                subTabsContainer.style.display = 'flex';
            } else {
                subTabsContainer.style.display = 'none';
            }

            loadRanking();
        });
    });

    // 최다 참여 서브탭
    document.querySelectorAll('#participationSubTabs .sub-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('#participationSubTabs .sub-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            participationSubType = this.dataset.subType;
            loadRanking();
        });
    });
}

function updateTabsVisibility() {
    const tierNotice = document.getElementById('tierNotice');
    const best30PeriodTabs = document.getElementById('best30PeriodTabs');
    const best30Notice = document.getElementById('best30Notice');
    const retroPeriodTabs = document.getElementById('retroPeriodTabs');
    const retroNotice = document.getElementById('retroNotice');
    const fanChallengePeriodTabs = document.getElementById('fanChallengePeriodTabs');
    const fanChallengeNotice = document.getElementById('fanChallengeNotice');
    const genreChallengeGenreSelect = document.getElementById('genreChallengeGenreSelect');
    const genreChallengeNotice = document.getElementById('genreChallengeNotice');
    const statsTabsContainer = document.getElementById('statsTabsContainer');

    // 모두 숨기기
    tierNotice.style.display = 'none';
    best30PeriodTabs.style.display = 'none';
    best30Notice.style.display = 'none';
    retroPeriodTabs.style.display = 'none';
    retroNotice.style.display = 'none';
    fanChallengePeriodTabs.style.display = 'none';
    fanChallengeNotice.style.display = 'none';
    genreChallengeGenreSelect.style.display = 'none';
    genreChallengeNotice.style.display = 'none';
    statsTabsContainer.style.display = 'none';

    if (currentTab === 'tier') {
        tierNotice.style.display = 'flex';
    } else if (currentTab === 'best30') {
        best30PeriodTabs.style.display = 'flex';
        best30Notice.style.display = 'flex';
    } else if (currentTab === 'retro') {
        retroPeriodTabs.style.display = 'flex';
        retroNotice.style.display = 'flex';
    } else if (currentTab === 'fanChallenge') {
        fanChallengePeriodTabs.style.display = 'flex';
        fanChallengeNotice.style.display = 'flex';
    } else if (currentTab === 'genreChallenge') {
        genreChallengeGenreSelect.style.display = 'flex';
        genreChallengeNotice.style.display = 'flex';
    } else if (currentTab === 'stats') {
        statsTabsContainer.style.display = 'flex';
    }
}

async function loadRanking() {
    try {
        let rankings;

        if (currentTab === 'tier') {
            // 멀티게임 티어 랭킹
            const response = await fetch('/api/ranking?mode=multi&period=tier&limit=20');
            rankings = await response.json();
            updateTierUI(rankings);
        } else if (currentTab === 'best30') {
            // 30개 챌린지 랭킹
            const response = await fetch(`/api/ranking/best30?period=${best30Period}&limit=50`);
            rankings = await response.json();
            updateBest30UI(rankings);
        } else if (currentTab === 'retro') {
            // 레트로 랭킹
            const response = await fetch(`/api/ranking?mode=retro&period=${retroPeriod}&limit=20`);
            rankings = await response.json();
            updateRetroUI(rankings);
        } else if (currentTab === 'fanChallenge') {
            // 팬 챌린지 랭킹
            const response = await fetch(`/api/ranking/fan-challenge?type=${fanChallengePeriod}&limit=20`);
            rankings = await response.json();
            updateFanChallengeUI(rankings);
        } else if (currentTab === 'genreChallenge') {
            // 장르 챌린지 랭킹 (장르별)
            if (!selectedGenreCode) {
                // 장르가 선택되지 않으면 빈 상태 표시
                updateGenreChallengeUI([]);
                return;
            }
            const response = await fetch(`/api/ranking/genre-challenge/by-genre?genreCode=${selectedGenreCode}&limit=20`);
            rankings = await response.json();
            updateGenreChallengeUI(rankings);
        } else if (currentTab === 'stats') {
            // 통계 랭킹 (내가맞추기 전용)
            // participation 타입은 서브탭(games/rounds)으로 실제 API 호출
            const apiType = (statsType === 'participation') ? participationSubType : statsType;
            const response = await fetch(`/api/ranking?mode=guess&type=${apiType}&period=all&limit=20`);
            rankings = await response.json();
            updateStatsUI(rankings);
        }
    } catch (error) {
        // console.error('랭킹 로딩 오류:', error);
    }
}

// 멀티게임 티어 UI
function updateTierUI(rankings) {
    if (rankings.length === 0) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        return;
    }

    document.getElementById('topThreePodium').style.display = 'flex';
    document.getElementById('rankingTable').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';

    updateTierPodium(rankings);
    updateTierTable(rankings);
}

function updateTierPodium(rankings) {
    const places = [
        { id: 'place1', index: 0 },
        { id: 'place2', index: 1 },
        { id: 'place3', index: 2 }
    ];

    places.forEach(place => {
        const el = document.getElementById(place.id);
        const member = rankings[place.index];

        el.style.display = 'flex';
        if (member) {
            el.classList.remove('empty');
            const badgeEmoji = member.badgeEmoji ? member.badgeEmoji + ' ' : '';
            el.querySelector('.podium-name').textContent = badgeEmoji + member.nickname;
            el.querySelector('.podium-value').textContent = (member.multiLp || 0) + ' LP';
            el.querySelector('.podium-stand').textContent = place.index + 1;

            const tierEl = el.querySelector('.podium-tier');
            tierEl.textContent = member.multiTierDisplayName || '';
            tierEl.style.color = member.multiTierColor || '#cd7f32';
            tierEl.className = 'podium-tier tier-badge tier-' + (member.multiTier || 'BRONZE').toLowerCase();
            tierEl.style.display = 'block';
        } else {
            el.classList.add('empty');
            el.querySelector('.podium-name').textContent = '도전하세요!';
            el.querySelector('.podium-value').textContent = '-';
            el.querySelector('.podium-stand').textContent = place.index + 1;
        }
    });
}

function updateTierTable(rankings) {
    const table = document.getElementById('rankingTable');

    table.innerHTML = rankings.map((member, index) => {
        const tierName = member.multiTier || 'BRONZE';
        const tierColor = member.multiTierColor || '#cd7f32';
        const tierDisplayName = member.multiTierDisplayName || '';
        const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';

        return `
            <div class="ranking-row ${index < 3 ? 'top-' + (index + 1) : ''}">
                <div class="rank-cell">
                    ${index < 3 ? getMedal(index) : (index + 1)}
                </div>
                <div class="name-cell">
                    <span class="tier-badge tier-${tierName.toLowerCase()}" style="color: ${tierColor}">${tierDisplayName}</span>
                    ${badgeEmoji}
                    <span class="member-name">${member.nickname}</span>
                </div>
                <div class="stats-cell">
                    <span class="main-stat">${(member.multiLp || 0)} LP</span>
                    <span class="sub-stat">1등 ${member.multiWins || 0}회 · Top3 ${member.multiTop3 || 0}회</span>
                </div>
            </div>
        `;
    }).join('');
}

// 30개 챌린지 UI
function updateBest30UI(rankings) {
    if (rankings.length === 0) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        return;
    }

    document.getElementById('topThreePodium').style.display = 'flex';
    document.getElementById('rankingTable').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';

    updateBest30Podium(rankings);
    updateBest30Table(rankings);
}

function updateBest30Podium(rankings) {
    const places = [
        { id: 'place1', index: 0 },
        { id: 'place2', index: 1 },
        { id: 'place3', index: 2 }
    ];

    places.forEach(place => {
        const el = document.getElementById(place.id);
        const member = rankings[place.index];

        el.style.display = 'flex';
        if (member) {
            el.classList.remove('empty');
            const badgeEmoji = member.badgeEmoji ? member.badgeEmoji + ' ' : '';
            el.querySelector('.podium-name').textContent = badgeEmoji + member.nickname;
            el.querySelector('.podium-value').textContent = (member.score || 0) + '점';
            el.querySelector('.podium-stand').textContent = member.rank;

            const tierEl = el.querySelector('.podium-tier');
            tierEl.textContent = '';
            tierEl.style.display = 'none';
        } else {
            el.classList.add('empty');
            el.querySelector('.podium-name').textContent = '도전하세요!';
            el.querySelector('.podium-value').textContent = '-';
            el.querySelector('.podium-stand').textContent = place.index + 1;
        }
    });
}

function updateBest30Table(rankings) {
    const table = document.getElementById('rankingTable');
    const top10 = rankings.slice(0, 10);
    const rest = rankings.slice(10);

    let html = top10.map((member, index) => {
        const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';
        const achievedDate = member.achievedAt ? new Date(member.achievedAt).toLocaleDateString('ko-KR') : '';

        return `
            <div class="ranking-row ${index < 3 ? 'top-' + (index + 1) : ''}">
                <div class="rank-cell">
                    ${member.rank <= 3 ? getMedal(member.rank - 1) : member.rank + '위'}
                </div>
                <div class="name-cell">
                    ${badgeEmoji}
                    <span class="member-name">${member.nickname}</span>
                </div>
                <div class="stats-cell">
                    <span class="main-stat">${(member.score || 0).toLocaleString()}점</span>
                    <span class="sub-stat">${achievedDate}</span>
                </div>
            </div>
        `;
    }).join('');

    // 10위 이후 접기/펼치기
    if (rest.length > 0) {
        const restHtml = rest.map((member) => {
            const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';
            const achievedDate = member.achievedAt ? new Date(member.achievedAt).toLocaleDateString('ko-KR') : '';

            return `
                <div class="ranking-row">
                    <div class="rank-cell">${member.rank}위</div>
                    <div class="name-cell">
                        ${badgeEmoji}
                        <span class="member-name">${member.nickname}</span>
                    </div>
                    <div class="stats-cell">
                        <span class="main-stat">${(member.score || 0).toLocaleString()}점</span>
                        <span class="sub-stat">${achievedDate}</span>
                    </div>
                </div>
            `;
        }).join('');

        html += `
            <div class="ranking-expand-section">
                <button class="expand-toggle" onclick="toggleBest30Expand()">
                    <span id="expandIcon">▼</span> ${rest.length}명 더보기
                </button>
                <div class="ranking-rest" id="best30Rest" style="display: ${showAllBest30 ? 'block' : 'none'};">
                    ${restHtml}
                </div>
            </div>
        `;
    }

    table.innerHTML = html;
}

function toggleBest30Expand() {
    showAllBest30 = !showAllBest30;
    const restEl = document.getElementById('best30Rest');
    const iconEl = document.getElementById('expandIcon');

    if (showAllBest30) {
        restEl.style.display = 'block';
        iconEl.textContent = '▲';
    } else {
        restEl.style.display = 'none';
        iconEl.textContent = '▼';
    }
}

// 레트로 UI
function updateRetroUI(rankings) {
    if (rankings.length === 0) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        return;
    }

    document.getElementById('topThreePodium').style.display = 'flex';
    document.getElementById('rankingTable').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';

    updateRetroPodium(rankings);
    updateRetroTable(rankings);
}

function updateRetroPodium(rankings) {
    const places = [
        { id: 'place1', index: 0 },
        { id: 'place2', index: 1 },
        { id: 'place3', index: 2 }
    ];

    places.forEach(place => {
        const el = document.getElementById(place.id);
        const member = rankings[place.index];

        el.style.display = 'flex';
        if (member) {
            el.classList.remove('empty');
            const badgeEmoji = member.badgeEmoji ? member.badgeEmoji + ' ' : '';
            el.querySelector('.podium-name').textContent = badgeEmoji + member.nickname;
            el.querySelector('.podium-value').textContent = (member.totalScore || 0).toLocaleString() + '점';
            el.querySelector('.podium-stand').textContent = place.index + 1;

            const tierEl = el.querySelector('.podium-tier');
            tierEl.textContent = '';
            tierEl.style.display = 'none';
        } else {
            el.classList.add('empty');
            el.querySelector('.podium-name').textContent = '도전하세요!';
            el.querySelector('.podium-value').textContent = '-';
            el.querySelector('.podium-stand').textContent = place.index + 1;
        }
    });
}

function updateRetroTable(rankings) {
    const table = document.getElementById('rankingTable');

    table.innerHTML = rankings.map((member, index) => {
        const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';
        let subStat = '';

        if (retroPeriod === 'best30') {
            const achievedDate = member.achievedAt ? new Date(member.achievedAt).toLocaleDateString('ko-KR') : '';
            subStat = achievedDate;
        } else {
            subStat = `${member.totalGames || 0}게임 · ${(member.accuracyRate || 0).toFixed(1)}%`;
        }

        return `
            <div class="ranking-row ${index < 3 ? 'top-' + (index + 1) : ''}">
                <div class="rank-cell">
                    ${index < 3 ? getMedal(index) : (index + 1)}
                </div>
                <div class="name-cell">
                    ${badgeEmoji}
                    <span class="member-name">${member.nickname}</span>
                </div>
                <div class="stats-cell">
                    <span class="main-stat">${(member.totalScore || 0).toLocaleString()}점</span>
                    <span class="sub-stat">${subStat}</span>
                </div>
            </div>
        `;
    }).join('');
}

// 팬 챌린지 UI
function updateFanChallengeUI(rankings) {
    if (rankings.length === 0) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        return;
    }

    document.getElementById('topThreePodium').style.display = 'flex';
    document.getElementById('rankingTable').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';

    updateFanChallengePodium(rankings);
    updateFanChallengeTable(rankings);
}

function updateFanChallengePodium(rankings) {
    const places = [
        { id: 'place1', index: 0 },
        { id: 'place2', index: 1 },
        { id: 'place3', index: 2 }
    ];

    places.forEach(place => {
        const el = document.getElementById(place.id);
        const member = rankings[place.index];

        el.style.display = 'flex';
        if (member) {
            el.classList.remove('empty');
            const badgeEmoji = member.badgeEmoji ? member.badgeEmoji + ' ' : '';
            el.querySelector('.podium-name').textContent = badgeEmoji + member.nickname;

            if (fanChallengePeriod === 'perfect') {
                el.querySelector('.podium-value').textContent = (member.perfectCount || 0) + '회';
            } else {
                el.querySelector('.podium-value').textContent = (member.artistCount || 0) + '명';
            }
            el.querySelector('.podium-stand').textContent = place.index + 1;

            const tierEl = el.querySelector('.podium-tier');
            tierEl.textContent = '';
            tierEl.style.display = 'none';
        } else {
            el.classList.add('empty');
            el.querySelector('.podium-name').textContent = '도전하세요!';
            el.querySelector('.podium-value').textContent = '-';
            el.querySelector('.podium-stand').textContent = place.index + 1;
        }
    });
}

function updateFanChallengeTable(rankings) {
    const table = document.getElementById('rankingTable');

    table.innerHTML = rankings.map((member, index) => {
        const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';

        let mainStat = '';
        let subStat = '';

        if (fanChallengePeriod === 'perfect') {
            mainStat = (member.perfectCount || 0) + '회 퍼펙트';
            subStat = '하드코어 클리어';
        } else {
            mainStat = (member.artistCount || 0) + '명 도전';
            subStat = '고유 아티스트';
        }

        return `
            <div class="ranking-row ${index < 3 ? 'top-' + (index + 1) : ''}">
                <div class="rank-cell">
                    ${index < 3 ? getMedal(index) : (index + 1)}
                </div>
                <div class="name-cell">
                    ${badgeEmoji}
                    <span class="member-name">${member.nickname}</span>
                </div>
                <div class="stats-cell">
                    <span class="main-stat">${mainStat}</span>
                    <span class="sub-stat">${subStat}</span>
                </div>
            </div>
        `;
    }).join('');
}

// 장르 챌린지 UI
function updateGenreChallengeUI(rankings) {
    // 장르가 선택되지 않은 경우 안내 메시지 표시
    if (!selectedGenreCode) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        document.querySelector('#emptyState p').textContent = '장르를 선택해주세요';
        document.querySelector('#emptyState .empty-sub').textContent = '위 드롭다운에서 장르를 선택하면 랭킹을 볼 수 있습니다';
        return;
    }

    if (rankings.length === 0) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        document.querySelector('#emptyState p').textContent = '아직 랭킹 데이터가 없습니다.';
        document.querySelector('#emptyState .empty-sub').textContent = '게임을 플레이하고 랭킹에 도전하세요!';
        return;
    }

    document.getElementById('topThreePodium').style.display = 'flex';
    document.getElementById('rankingTable').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';

    updateGenreChallengePodium(rankings);
    updateGenreChallengeTable(rankings);
}

function updateGenreChallengePodium(rankings) {
    const places = [
        { id: 'place1', index: 0 },
        { id: 'place2', index: 1 },
        { id: 'place3', index: 2 }
    ];

    places.forEach(place => {
        const el = document.getElementById(place.id);
        const member = rankings[place.index];

        el.style.display = 'flex';
        if (member) {
            el.classList.remove('empty');
            const badgeEmoji = member.badgeEmoji ? member.badgeEmoji + ' ' : '';
            el.querySelector('.podium-name').textContent = badgeEmoji + member.nickname;
            // 정답수/총곡수 형식으로 표시
            el.querySelector('.podium-value').textContent =
                (member.correctCount || 0) + '/' + (member.totalSongs || 0) + '곡';
            el.querySelector('.podium-stand').textContent = place.index + 1;

            const tierEl = el.querySelector('.podium-tier');
            tierEl.textContent = '';
            tierEl.style.display = 'none';
        } else {
            el.classList.add('empty');
            el.querySelector('.podium-name').textContent = '도전하세요!';
            el.querySelector('.podium-value').textContent = '-';
            el.querySelector('.podium-stand').textContent = place.index + 1;
        }
    });
}

function updateGenreChallengeTable(rankings) {
    const table = document.getElementById('rankingTable');

    table.innerHTML = rankings.map((member, index) => {
        const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';

        // 메인: 정답수/총곡수
        const mainStat = (member.correctCount || 0) + '/' + (member.totalSongs || 0) + '곡';
        // 서브: 최대 콤보
        const subStat = '🔥' + (member.maxCombo || 0) + '콤보';

        return `
            <div class="ranking-row ${index < 3 ? 'top-' + (index + 1) : ''}">
                <div class="rank-cell">
                    ${index < 3 ? getMedal(index) : (index + 1)}
                </div>
                <div class="name-cell">
                    ${badgeEmoji}
                    <span class="member-name">${member.nickname}</span>
                </div>
                <div class="stats-cell">
                    <span class="main-stat">${mainStat}</span>
                    <span class="sub-stat">${subStat}</span>
                </div>
            </div>
        `;
    }).join('');
}

function getMedal(index) {
    const medals = ['🥇', '🥈', '🥉'];
    return medals[index] || (index + 1);
}

// 통계 UI
function updateStatsUI(rankings) {
    if (rankings.length === 0) {
        document.getElementById('topThreePodium').style.display = 'none';
        document.getElementById('rankingTable').style.display = 'none';
        document.getElementById('emptyState').style.display = 'flex';
        return;
    }

    document.getElementById('topThreePodium').style.display = 'flex';
    document.getElementById('rankingTable').style.display = 'block';
    document.getElementById('emptyState').style.display = 'none';

    updateStatsPodium(rankings);
    updateStatsTable(rankings);
}

function updateStatsPodium(rankings) {
    const places = [
        { id: 'place1', index: 0 },
        { id: 'place2', index: 1 },
        { id: 'place3', index: 2 }
    ];

    places.forEach(place => {
        const el = document.getElementById(place.id);
        const member = rankings[place.index];

        el.style.display = 'flex';
        if (member) {
            el.classList.remove('empty');
            const badgeEmoji = member.badgeEmoji ? member.badgeEmoji + ' ' : '';
            el.querySelector('.podium-name').textContent = badgeEmoji + member.nickname;
            el.querySelector('.podium-value').textContent = formatStatsValue(member);
            el.querySelector('.podium-stand').textContent = place.index + 1;

            const tierEl = el.querySelector('.podium-tier');
            tierEl.textContent = '';
            tierEl.style.display = 'none';
        } else {
            el.classList.add('empty');
            el.querySelector('.podium-name').textContent = '도전하세요!';
            el.querySelector('.podium-value').textContent = '-';
            el.querySelector('.podium-stand').textContent = place.index + 1;
        }
    });
}

function updateStatsTable(rankings) {
    const table = document.getElementById('rankingTable');

    table.innerHTML = rankings.map((member, index) => {
        const badgeEmoji = member.badgeEmoji ? `<span class="member-badge" title="${member.badgeName || ''}">${member.badgeEmoji}</span>` : '';

        return `
            <div class="ranking-row ${index < 3 ? 'top-' + (index + 1) : ''}">
                <div class="rank-cell">
                    ${index < 3 ? getMedal(index) : (index + 1)}
                </div>
                <div class="name-cell">
                    ${badgeEmoji}
                    <span class="member-name">${member.nickname}</span>
                </div>
                <div class="stats-cell">
                    <span class="main-stat">${formatStatsValue(member)}</span>
                    <span class="sub-stat">${formatStatsSubStat(member)}</span>
                </div>
            </div>
        `;
    }).join('');
}

function formatStatsValue(member) {
    // participation 타입은 서브탭에 따라 값 표시
    const displayType = (statsType === 'participation') ? participationSubType : statsType;
    switch (displayType) {
        case 'games':
            return (member.totalGames || 0) + '게임';
        case 'rounds':
            return (member.totalRounds || 0) + '라운드';
        case 'score':
            return (member.totalScore || 0).toLocaleString() + '점';
        case 'avgScorePerRound':
            return (member.averageScorePerRound || 0).toFixed(2) + '점';
        case 'accuracyMin10':
            return (member.accuracyRate || 0).toFixed(1) + '%';
        default:
            return (member.totalScore || 0).toLocaleString() + '점';
    }
}

function formatStatsSubStat(member) {
    // participation 타입은 서브탭에 따라 값 표시
    const displayType = (statsType === 'participation') ? participationSubType : statsType;
    switch (displayType) {
        case 'games':
            return (member.totalScore || 0).toLocaleString() + '점 · ' + (member.accuracyRate || 0).toFixed(1) + '%';
        case 'rounds':
            return (member.totalGames || 0) + '게임 · ' + (member.totalScore || 0).toLocaleString() + '점';
        case 'score':
            return (member.totalGames || 0) + '게임 · ' + (member.accuracyRate || 0).toFixed(1) + '%';
        case 'avgScorePerRound':
            return (member.totalRounds || 0) + '라운드 · ' + (member.totalScore || 0).toLocaleString() + '점';
        case 'accuracyMin10':
            return (member.totalCorrect || 0) + '/' + (member.totalRounds || 0) + '문제 · ' + (member.totalGames || 0) + '게임';
        default:
            return (member.totalGames || 0) + '게임';
    }
}
