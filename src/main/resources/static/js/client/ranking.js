/**
 * client/ranking.html - 전체 랭킹
 */

let currentTab = 'tier';      // tier, best30, stats
let best30Period = 'weekly';  // weekly, monthly, alltime
let statsType = 'score';      // score, participation, avgScorePerRound, accuracyMin10
let participationSubType = 'games';  // games, rounds (서브탭 선택)
let showAllBest30 = false;

document.addEventListener('DOMContentLoaded', function() {
    loadRanking();
    setupTabs();
});

function setupTabs() {
    // 메인 탭
    document.querySelectorAll('.mode-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            document.querySelectorAll('.mode-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            currentTab = this.dataset.mode;

            // 30개 챌린지 기간 탭 초기화
            if (currentTab === 'best30') {
                best30Period = 'weekly';
                document.querySelectorAll('#best30PeriodTabs .period-tab').forEach(t => t.classList.remove('active'));
                document.querySelector('#best30PeriodTabs .period-tab[data-period="weekly"]').classList.add('active');
                showAllBest30 = false;
            }

            // 통계 탭 초기화
            if (currentTab === 'stats') {
                statsType = 'score';
                participationSubType = 'games';
                document.querySelectorAll('.stats-type-tabs .period-tab').forEach(t => t.classList.remove('active'));
                document.querySelector('.stats-type-tabs .period-tab[data-stats-type="score"]').classList.add('active');
                // 서브탭 숨기기 및 초기화
                document.getElementById('participationSubTabs').style.display = 'none';
                document.querySelectorAll('#participationSubTabs .sub-tab').forEach(t => t.classList.remove('active'));
                document.querySelector('#participationSubTabs .sub-tab[data-sub-type="games"]').classList.add('active');
            }

            updateTabsVisibility();
            loadRanking();
        });
    });

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
    const statsTabsContainer = document.getElementById('statsTabsContainer');

    // 모두 숨기기
    tierNotice.style.display = 'none';
    best30PeriodTabs.style.display = 'none';
    best30Notice.style.display = 'none';
    statsTabsContainer.style.display = 'none';

    if (currentTab === 'tier') {
        tierNotice.style.display = 'flex';
    } else if (currentTab === 'best30') {
        best30PeriodTabs.style.display = 'flex';
        best30Notice.style.display = 'flex';
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
