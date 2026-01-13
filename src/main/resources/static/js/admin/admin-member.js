// 현재 선택된 회원 ID
let currentMemberId = null;

// 모달 열기
function openModal(modalId) {
    document.getElementById(modalId).classList.add('show');
}

// 모달 닫기
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// 회원 상세 보기
function viewDetail(id) {
    currentMemberId = id;
    fetch(`/admin/member/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            const content = document.getElementById('detailContent');
            content.innerHTML = `
                <div class="detail-grid">
                    <div class="detail-item">
                        <div class="detail-label">이메일</div>
                        <div class="detail-value">${data.email}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">닉네임</div>
                        <div class="detail-value">${data.nickname}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">이름</div>
                        <div class="detail-value">${data.username || '-'}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">티어</div>
                        <div class="detail-value">${data.tierDisplayName}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">권한</div>
                        <div class="detail-value">${data.role === 'ADMIN' ? '관리자' : '사용자'}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">상태</div>
                        <div class="detail-value">${getStatusText(data.status)}</div>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>게임 통계</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <div class="detail-label">총 게임 수</div>
                            <div class="detail-value">${data.totalGames?.toLocaleString() || 0}회</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">총 점수</div>
                            <div class="detail-value">${data.totalScore?.toLocaleString() || 0}점</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">정답률</div>
                            <div class="detail-value">${data.accuracyRate || 0}%</div>
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>모드별 통계</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <div class="detail-label">내가맞추기 게임</div>
                            <div class="detail-value">${data.guessGames?.toLocaleString() || 0}회 / ${data.guessScore?.toLocaleString() || 0}점</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">멀티게임</div>
                            <div class="detail-value">${data.multiGames?.toLocaleString() || 0}회 / ${data.multiScore?.toLocaleString() || 0}점</div>
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>멀티게임 LP 티어</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <div class="detail-label">LP 티어</div>
                            <div class="detail-value">
                                <span style="background-color: ${data.multiTierColor}; color: #000; padding: 2px 8px; border-radius: 4px; font-weight: bold;">
                                    ${data.multiTierDisplayName}
                                </span>
                            </div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">LP</div>
                            <div class="detail-value">${data.multiLp || 0} LP</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">멀티게임 1위</div>
                            <div class="detail-value">${data.multiWins?.toLocaleString() || 0}회</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">멀티게임 TOP3</div>
                            <div class="detail-value">${data.multiTop3?.toLocaleString() || 0}회</div>
                        </div>
                    </div>
                </div>

                <div class="detail-section">
                    <h3>계정 정보</h3>
                    <div class="detail-grid">
                        <div class="detail-item">
                            <div class="detail-label">가입일</div>
                            <div class="detail-value">${formatDateTime(data.createdAt)}</div>
                        </div>
                        <div class="detail-item">
                            <div class="detail-label">최근 로그인</div>
                            <div class="detail-value">${formatDateTime(data.lastLoginAt)}</div>
                        </div>
                    </div>
                </div>
            `;
            openModal('detailModal');
        })
        .catch(error => {
            alert('회원 정보를 불러오는데 실패했습니다.');
            console.error(error);
        });
}

// 상태 텍스트 변환
function getStatusText(status) {
    switch(status) {
        case 'ACTIVE': return '활성';
        case 'INACTIVE': return '비활성';
        case 'BANNED': return '정지';
        default: return status;
    }
}

// 날짜/시간 포맷팅
function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('ko-KR');
}

// 상태 변경 모달 열기
function openStatusModal(id) {
    currentMemberId = id;
    document.getElementById('statusMemberId').value = id;

    // 현재 상태 가져오기
    fetch(`/admin/member/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            // 현재 상태에 해당하는 라디오 버튼 체크
            const statusRadio = document.querySelector(`input[name="status"][value="${data.status}"]`);
            if (statusRadio) {
                statusRadio.checked = true;
            }
            openModal('statusModal');
        });
}

// 권한 변경 모달 열기
function openRoleModal(id) {
    currentMemberId = id;
    document.getElementById('roleMemberId').value = id;

    // 현재 권한 가져오기
    fetch(`/admin/member/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            // 현재 권한에 해당하는 라디오 버튼 체크
            const roleRadio = document.querySelector(`input[name="role"][value="${data.role}"]`);
            if (roleRadio) {
                roleRadio.checked = true;
            }
            openModal('roleModal');
        });
}

// 상태 변경 폼 제출
document.getElementById('statusForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const id = document.getElementById('statusMemberId').value;
    const status = document.querySelector('input[name="status"]:checked').value;

    fetch(`/admin/member/update-status/${id}?status=${status}`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
                closeModal('statusModal');
                location.reload();
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            alert('상태 변경에 실패했습니다.');
            console.error(error);
        });
});

// 권한 변경 폼 제출
document.getElementById('roleForm').addEventListener('submit', function(e) {
    e.preventDefault();
    const id = document.getElementById('roleMemberId').value;
    const role = document.querySelector('input[name="role"]:checked').value;

    if (role === 'ADMIN' && !confirm('정말 관리자 권한을 부여하시겠습니까?')) {
        return;
    }

    fetch(`/admin/member/update-role/${id}?role=${role}`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
                closeModal('roleModal');
                location.reload();
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            alert('권한 변경에 실패했습니다.');
            console.error(error);
        });
});

// 비밀번호 초기화
function resetPassword() {
    if (!currentMemberId) return;
    if (!confirm('정말 비밀번호를 초기화하시겠습니까?')) return;

    fetch(`/admin/member/reset-password/${currentMemberId}`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            alert('비밀번호 초기화에 실패했습니다.');
            console.error(error);
        });
}

// 세션 강제 종료
function kickSession() {
    if (!currentMemberId) return;
    if (!confirm('정말 세션을 강제 종료하시겠습니까?')) return;

    fetch(`/admin/member/kick-session/${currentMemberId}`, {
        method: 'POST'
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert(data.message);
            } else {
                alert(data.message);
            }
        })
        .catch(error => {
            alert('세션 종료에 실패했습니다.');
            console.error(error);
        });
}

// 모달 외부 클릭 시 닫기
document.querySelectorAll('.modal').forEach(modal => {
    modal.addEventListener('click', function(e) {
        if (e.target === this) {
            this.classList.remove('show');
        }
    });
});

// ESC 키로 모달 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal.show').forEach(modal => {
            modal.classList.remove('show');
        });
    }
});
