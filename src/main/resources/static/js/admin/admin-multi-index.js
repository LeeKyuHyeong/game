/**
 * Admin Multi Index Page - Room & Chat Management
 */

var currentTab = currentTab || 'room';
var currentRoomId = currentRoomId || null;

// ========== Initialization ==========

document.addEventListener('DOMContentLoaded', () => {
    loadTabContent(currentTab);
});

// ========== Tab Management ==========

function switchTab(tab) {
    if (currentTab === tab) return;

    currentTab = tab;

    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    window.history.pushState({}, '', url);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector(`.tab-btn:nth-child(${tab === 'room' ? 1 : 2})`).classList.add('active');

    loadTabContent(tab);
}

async function loadTabContent(tab, params = '') {
    const tabContent = document.getElementById('tabContent');
    tabContent.innerHTML = `
        <div class="loading-spinner">
            <div class="spinner"></div>
            <span>로딩 중...</span>
        </div>
    `;

    try {
        const url = tab === 'room'
            ? `/admin/room/content${params ? '?' + params : ''}`
            : `/admin/chat/content${params ? '?' + params : ''}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error('Failed to load content');

        const html = await response.text();
        tabContent.innerHTML = html;

        initTabScripts();
    } catch (error) {
        tabContent.innerHTML = `
            <div class="error-message">
                <p>콘텐츠를 불러오는데 실패했습니다.</p>
                <button class="btn btn-primary" onclick="loadTabContent('${tab}')">다시 시도</button>
            </div>
        `;
    }
}

function initTabScripts() {
    const searchForm = document.querySelector('.tab-content .search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const formData = new FormData(searchForm);
            const params = new URLSearchParams(formData).toString();
            loadTabContent(currentTab, params);
        });
    }

    const resetBtn = document.querySelector('.tab-content .btn-reset');
    if (resetBtn) {
        resetBtn.addEventListener('click', (e) => {
            e.preventDefault();
            loadTabContent(currentTab);
        });
    }
}

// ========== Utility Functions ==========

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

function goToPage(page) {
    const params = new URLSearchParams();
    const form = document.querySelector('.tab-content .search-form');
    if (form) {
        new FormData(form).forEach((value, key) => {
            if (value) params.set(key, value);
        });
    }
    params.set('page', page);
    loadTabContent(currentTab, params.toString());
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function openModal(modalId) {
    document.getElementById(modalId).classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
    document.body.style.overflow = '';
    if (modalId === 'chatModal') {
        document.getElementById('deleteAllChatsBtn').style.display = 'none';
    }
}

// ========== Room Functions ==========

async function viewRoomDetail(id) {
    try {
        const response = await fetch(`/admin/room/detail/${id}`);
        if (!response.ok) throw new Error('방을 찾을 수 없습니다.');

        const data = await response.json();
        currentRoomId = id;

        const content = document.getElementById('roomDetailContent');
        content.innerHTML = '';

        const grid = document.createElement('div');
        grid.className = 'detail-grid';

        function createItem(label, value) {
            const item = document.createElement('div');
            item.className = 'detail-item';
            item.innerHTML = `<div class="detail-label">${label}</div><div class="detail-value">${value}</div>`;
            return item;
        }

        grid.appendChild(createItem('방 코드', data.roomCode));
        grid.appendChild(createItem('방 이름', escapeHtml(data.roomName)));
        grid.appendChild(createItem('방장', escapeHtml(data.hostNickname)));
        grid.appendChild(createItem('상태', getStatusBadge(data.status)));
        grid.appendChild(createItem('인원', `${data.currentPlayers}/${data.maxPlayers}`));
        grid.appendChild(createItem('라운드', `${data.currentRound}/${data.totalRounds}`));
        grid.appendChild(createItem('비공개', data.isPrivate ? '예' : '아니오'));
        grid.appendChild(createItem('생성일', new Date(data.createdAt).toLocaleString('ko-KR')));

        if (data.participants && data.participants.length > 0) {
            const participantsDiv = document.createElement('div');
            participantsDiv.className = 'detail-item full-width';
            participantsDiv.innerHTML = '<div class="detail-label">참가자</div>';

            const participantsList = document.createElement('div');
            participantsList.className = 'participants-list';

            data.participants.forEach(p => {
                const pItem = document.createElement('div');
                pItem.className = 'participant-item';
                pItem.innerHTML = `
                    <span class="participant-name">${escapeHtml(p.nickname)}</span>
                    <span class="participant-score">${p.score}점</span>
                    <span class="participant-status ${p.isReady ? 'ready' : ''}">${p.isReady ? '준비완료' : '대기중'}</span>
                `;
                participantsList.appendChild(pItem);
            });

            participantsDiv.appendChild(participantsList);
            grid.appendChild(participantsDiv);
        }

        content.appendChild(grid);

        const actions = document.createElement('div');
        actions.className = 'detail-actions';
        actions.innerHTML = `
            <button class="btn btn-info" onclick="viewRoomChats(${id})">채팅 보기</button>
            ${data.status !== 'FINISHED' ? `<button class="btn btn-warning" onclick="closeRoom(${id})">방 종료</button>` : ''}
            <button class="btn btn-danger" onclick="deleteRoom(${id})">방 삭제</button>
        `;
        content.appendChild(actions);

        openModal('roomDetailModal');
    } catch (error) {
        showToast('방 정보를 불러오는데 실패했습니다.', 'error');
    }
}

function getStatusBadge(status) {
    const badges = {
        'WAITING': '<span class="status-badge waiting">대기중</span>',
        'PLAYING': '<span class="status-badge playing">게임중</span>',
        'FINISHED': '<span class="status-badge finished">종료</span>'
    };
    return badges[status] || status;
}

async function closeRoom(id) {
    if (!confirm('이 방을 강제 종료하시겠습니까?')) return;

    try {
        const response = await fetch(`/admin/room/close/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast('방이 종료되었습니다.', 'success');
            closeModal('roomDetailModal');
            loadTabContent('room');
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        showToast('방 종료에 실패했습니다.', 'error');
    }
}

async function deleteRoom(id) {
    if (!confirm('이 방을 삭제하시겠습니까? 채팅 기록도 함께 삭제됩니다.')) return;

    try {
        const response = await fetch(`/admin/room/delete/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast('방이 삭제되었습니다.', 'success');
            closeModal('roomDetailModal');
            loadTabContent('room');
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        showToast('방 삭제에 실패했습니다.', 'error');
    }
}

// ========== Chat Functions ==========

async function viewRoomChats(id) {
    try {
        const response = await fetch(`/admin/room/chat/${id}`);
        const result = await response.json();

        if (!result.success) throw new Error(result.message);

        currentRoomId = id;
        document.getElementById('chatModalTitle').textContent = `채팅 내역 - ${escapeHtml(result.roomName)} (${result.roomCode})`;

        const content = document.getElementById('chatContent');
        content.innerHTML = '';

        if (result.chats.length === 0) {
            content.innerHTML = '<div class="empty-message">채팅 기록이 없습니다.</div>';
        } else {
            const chatList = document.createElement('div');
            chatList.className = 'chat-list';

            result.chats.forEach(chat => {
                const chatItem = document.createElement('div');
                chatItem.className = `chat-item ${chat.messageType.toLowerCase()}`;
                chatItem.innerHTML = `
                    <div class="chat-header">
                        <span class="chat-nickname">${escapeHtml(chat.nickname)}</span>
                        <span class="chat-time">${new Date(chat.createdAt).toLocaleString('ko-KR')}</span>
                        <button class="btn btn-sm btn-danger" onclick="deleteChat(${chat.id}, event)">삭제</button>
                    </div>
                    <div class="chat-message">${escapeHtml(chat.message)}</div>
                `;
                chatList.appendChild(chatItem);
            });

            content.appendChild(chatList);

            document.getElementById('deleteAllChatsBtn').style.display = 'inline-block';
            document.getElementById('deleteAllChatsBtn').onclick = () => deleteAllChats(id);
        }

        closeModal('roomDetailModal');
        openModal('chatModal');
    } catch (error) {
        showToast('채팅 내역을 불러오는데 실패했습니다.', 'error');
    }
}

async function deleteChat(chatId, event) {
    event.stopPropagation();
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return;

    try {
        const response = await fetch(`/admin/room/chat/delete/${chatId}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast('채팅이 삭제되었습니다.', 'success');
            if (currentRoomId) viewRoomChats(currentRoomId);
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        showToast('채팅 삭제에 실패했습니다.', 'error');
    }
}

async function deleteAllChats(roomId) {
    if (!confirm('이 방의 모든 채팅을 삭제하시겠습니까?')) return;

    try {
        const response = await fetch(`/admin/room/chat/delete-all/${roomId}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast('모든 채팅이 삭제되었습니다.', 'success');
            closeModal('chatModal');
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        showToast('채팅 삭제에 실패했습니다.', 'error');
    }
}

async function deleteChatFromList(chatId) {
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return;

    try {
        const response = await fetch(`/admin/chat/delete/${chatId}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast('채팅이 삭제되었습니다.', 'success');
            loadTabContent('chat');
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        showToast('채팅 삭제에 실패했습니다.', 'error');
    }
}
