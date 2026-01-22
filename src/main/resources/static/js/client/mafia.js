// 마피아 직업 분배 - 클라이언트 전용 (DB 없음)

let assignments = []; // 분배 결과 저장
let allVisible = false; // 전체 보기 상태

document.addEventListener('DOMContentLoaded', function() {
    // 참가자 입력 시 카운트 업데이트
    document.getElementById('participantsInput').addEventListener('input', updateParticipantCount);

    // 직업 수 변경 시 총합 업데이트
    document.getElementById('rolesGrid').addEventListener('input', updateRoleCount);

    // 초기 카운트 업데이트
    updateRoleCount();
});

// 참가자 목록 파싱
function getParticipants() {
    const input = document.getElementById('participantsInput').value;
    return input.split('\n')
        .map(name => name.trim())
        .filter(name => name.length > 0);
}

// 참가자 수 업데이트
function updateParticipantCount() {
    const count = getParticipants().length;
    document.getElementById('participantCount').textContent = count;
    checkBalance();
}

// 직업 설정 가져오기
function getRoleSettings() {
    const roles = [];
    document.querySelectorAll('.role-count').forEach(input => {
        const count = parseInt(input.value) || 0;
        if (count > 0) {
            const item = input.closest('.role-item');
            roles.push({
                name: input.dataset.role,
                icon: item.querySelector('.role-icon').textContent,
                count: count
            });
        }
    });
    return roles;
}

// 직업 총합 업데이트
function updateRoleCount() {
    const roles = getRoleSettings();
    const total = roles.reduce((sum, r) => sum + r.count, 0);
    document.getElementById('totalRoleCount').textContent = total;
    checkBalance();
}

// 참가자 수와 직업 수 균형 체크
function checkBalance() {
    const participantCount = getParticipants().length;
    const roleCount = parseInt(document.getElementById('totalRoleCount').textContent) || 0;
    const warning = document.getElementById('roleWarning');

    if (participantCount > 0 && participantCount !== roleCount) {
        warning.classList.remove('hidden');
    } else {
        warning.classList.add('hidden');
    }
}

// 커스텀 직업 추가
function addCustomRole() {
    const nameInput = document.getElementById('customRoleName');
    const iconInput = document.getElementById('customRoleIcon');

    const name = nameInput.value.trim();
    const icon = iconInput.value.trim() || '⭐';

    if (!name) {
        alert('직업 이름을 입력하세요.');
        return;
    }

    // 중복 체크
    const existing = document.querySelector(`[data-role="${name}"]`);
    if (existing) {
        alert('이미 존재하는 직업입니다.');
        return;
    }

    const grid = document.getElementById('rolesGrid');
    const roleItem = document.createElement('div');
    roleItem.className = 'role-item';
    roleItem.innerHTML = `
        <span class="role-icon">${icon}</span>
        <span class="role-name">${name}</span>
        <input type="number" class="role-count" data-role="${name}" min="0" value="1">
        <button type="button" class="role-delete" onclick="removeRole(this)">✕</button>
    `;
    grid.appendChild(roleItem);

    // 입력 초기화
    nameInput.value = '';
    iconInput.value = '';

    updateRoleCount();
}

// 커스텀 직업 삭제
function removeRole(btn) {
    btn.closest('.role-item').remove();
    updateRoleCount();
}

// TODO(human): 직업 분배 알고리즘 구현
// 참가자 배열과 직업 설정을 받아서 각 참가자에게 직업을 배정합니다.
// 반환 형식: [{ name: '참가자명', role: '직업명', icon: '이모지' }, ...]
function assignRoles(participants, roleSettings) {
    // 1. roleSettings를 기반으로 직업 배열 생성
    //    예: [{name:'마피아', icon:'🔪', count:2}] → ['마피아', '마피아']
    //
    // 2. 직업 배열을 랜덤하게 섞기 (Fisher-Yates 셔플 추천)
    //
    // 3. 각 참가자에게 순서대로 직업 배정
    //
    // 힌트: Fisher-Yates 셔플
    // for (let i = array.length - 1; i > 0; i--) {
    //     const j = Math.floor(Math.random() * (i + 1));
    //     [array[i], array[j]] = [array[j], array[i]];
    // }

    return []; // 구현 후 결과 배열 반환
}

// 직업 분배 실행
function distributeRoles() {
    const participants = getParticipants();
    const roleSettings = getRoleSettings();

    // 유효성 검사
    if (participants.length === 0) {
        alert('참가자를 입력하세요.');
        return;
    }

    const totalRoles = roleSettings.reduce((sum, r) => sum + r.count, 0);
    if (participants.length !== totalRoles) {
        alert(`참가자 수(${participants.length}명)와 직업 수(${totalRoles}개)가 일치하지 않습니다.`);
        return;
    }

    // 직업 분배 실행
    assignments = assignRoles(participants, roleSettings);

    if (assignments.length === 0) {
        alert('직업 분배 알고리즘을 구현해주세요! (mafia.js의 assignRoles 함수)');
        return;
    }

    // 결과 표시
    displayResults();
}

// 결과 표시
function displayResults() {
    const resultSection = document.getElementById('resultSection');
    const resultGrid = document.getElementById('resultGrid');
    const summaryResult = document.getElementById('roleSummaryResult');

    resultSection.classList.remove('hidden');
    allVisible = false;

    // 참가자별 카드 생성
    let cardsHtml = '';
    assignments.forEach((a, index) => {
        cardsHtml += `
            <div class="result-card" onclick="toggleRole(${index})">
                <div class="participant-name">${a.name}</div>
                <div class="role-reveal" id="role-${index}">
                    <span class="role-emoji">${a.icon}</span>
                    <span class="role-text">클릭하여 확인</span>
                </div>
            </div>
        `;
    });
    resultGrid.innerHTML = cardsHtml;

    // 직업별 요약 (진행자용)
    const roleCounts = {};
    assignments.forEach(a => {
        const key = `${a.icon} ${a.role}`;
        roleCounts[key] = (roleCounts[key] || 0) + 1;
    });

    let summaryHtml = '<h3>직업별 현황</h3><div class="summary-list">';
    for (const [role, count] of Object.entries(roleCounts)) {
        summaryHtml += `<div class="summary-item">${role}: ${count}명</div>`;
    }
    summaryHtml += '</div>';
    summaryResult.innerHTML = summaryHtml;

    // 결과 영역으로 스크롤
    resultSection.scrollIntoView({ behavior: 'smooth' });
}

// 개별 직업 토글
function toggleRole(index) {
    const reveal = document.getElementById(`role-${index}`);
    const isVisible = reveal.classList.contains('visible');

    if (isVisible) {
        reveal.classList.remove('visible');
        reveal.querySelector('.role-text').textContent = '클릭하여 확인';
    } else {
        reveal.classList.add('visible');
        reveal.querySelector('.role-text').textContent = assignments[index].role;
    }
}

// 전체 보기/숨기기 토글
function toggleAllResults() {
    allVisible = !allVisible;

    assignments.forEach((a, index) => {
        const reveal = document.getElementById(`role-${index}`);
        if (allVisible) {
            reveal.classList.add('visible');
            reveal.querySelector('.role-text').textContent = a.role;
        } else {
            reveal.classList.remove('visible');
            reveal.querySelector('.role-text').textContent = '클릭하여 확인';
        }
    });
}
