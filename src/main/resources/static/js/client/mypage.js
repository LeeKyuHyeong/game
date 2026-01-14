// 마이페이지 JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // 페이지 로드 시 새 뱃지 체크
    checkNewBadges();

    // 뱃지 클릭 이벤트 (이벤트 위임)
    const badgeGrid = document.querySelector('.badge-grid');
    if (badgeGrid) {
        badgeGrid.addEventListener('click', function(e) {
            const badgeItem = e.target.closest('.badge-item');
            if (badgeItem && badgeItem.dataset.owned === 'true') {
                const badgeId = badgeItem.dataset.badgeId;
                selectBadge(badgeId);
            }
        });
    }
});

// 뱃지 선택
async function selectBadge(badgeId) {
    try {
        const response = await fetch('/mypage/badge/select', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ badgeId: badgeId })
        });

        const result = await response.json();

        if (result.success) {
            // 페이지 새로고침으로 UI 업데이트
            window.location.reload();
        } else {
            alert(result.message || '뱃지 선택에 실패했습니다.');
        }
    } catch (error) {
        console.error('뱃지 선택 오류:', error);
        alert('뱃지 선택 중 오류가 발생했습니다.');
    }
}

// 뱃지 선택 해제
async function clearBadge() {
    try {
        const response = await fetch('/mypage/badge/select', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ badgeId: null })
        });

        const result = await response.json();

        if (result.success) {
            window.location.reload();
        } else {
            alert(result.message || '뱃지 해제에 실패했습니다.');
        }
    } catch (error) {
        console.error('뱃지 해제 오류:', error);
        alert('뱃지 해제 중 오류가 발생했습니다.');
    }
}

// 새 뱃지 체크 및 읽음 처리
async function checkNewBadges() {
    try {
        const response = await fetch('/mypage/badges/new');
        const newBadges = await response.json();

        if (newBadges.length > 0) {
            // 새 뱃지가 있으면 읽음 처리
            await fetch('/mypage/badges/mark-read', { method: 'POST' });
        }
    } catch (error) {
        console.error('새 뱃지 체크 오류:', error);
    }
}

// 프로필 이미지 업로드
async function uploadProfileImage(input) {
    const file = input.files[0];
    if (!file) return;

    // 파일 크기 체크 (5MB)
    if (file.size > 5 * 1024 * 1024) {
        alert('파일 크기는 5MB 이하만 가능합니다.');
        input.value = '';
        return;
    }

    // 이미지 타입 체크
    if (!file.type.startsWith('image/')) {
        alert('이미지 파일만 업로드 가능합니다.');
        input.value = '';
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
        const response = await fetch('/mypage/profile/image', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            // 이미지 업데이트
            const profileImage = document.getElementById('profileImage');
            profileImage.style.backgroundImage = `url(${result.imageUrl})`;

            // 기본 아바타 숨기기
            const defaultAvatar = profileImage.querySelector('.default-avatar');
            if (defaultAvatar) {
                defaultAvatar.style.display = 'none';
            }

            // 삭제 버튼 표시
            document.getElementById('deleteImageBtn').style.display = '';
        } else {
            alert(result.message || '이미지 업로드에 실패했습니다.');
        }
    } catch (error) {
        console.error('이미지 업로드 오류:', error);
        alert('이미지 업로드 중 오류가 발생했습니다.');
    }

    input.value = '';
}

// 프로필 이미지 삭제
async function deleteProfileImage() {
    if (!confirm('프로필 이미지를 삭제하시겠습니까?')) return;

    try {
        const response = await fetch('/mypage/profile/image', {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            // 이미지 제거
            const profileImage = document.getElementById('profileImage');
            profileImage.style.backgroundImage = '';

            // 기본 아바타 표시
            const defaultAvatar = profileImage.querySelector('.default-avatar');
            if (defaultAvatar) {
                defaultAvatar.style.display = '';
            }

            // 삭제 버튼 숨기기
            document.getElementById('deleteImageBtn').style.display = 'none';
        } else {
            alert(result.message || '이미지 삭제에 실패했습니다.');
        }
    } catch (error) {
        console.error('이미지 삭제 오류:', error);
        alert('이미지 삭제 중 오류가 발생했습니다.');
    }
}
