/**
 * 게임 관리 통합 페이지 JavaScript
 * - 게임 이력, 멀티 운영, 챌린지 기록 탭 관리
 * - 이 파일 하나로 통합 페이지의 모든 기능 처리 (다른 JS와 충돌 방지)
 */

// ========== 전역 변수 ==========
var currentTab = 'history';
var currentRoomId = null;
var VALID_TABS = ['history', 'multi', 'challenge'];

// ========== 페이지 초기화 ==========
document.addEventListener('DOMContentLoaded', function() {
    // 1. URL에서 탭 파라미터 확인
    var urlParams = new URLSearchParams(window.location.search);
    var tab = urlParams.get('tab');

    // 2. URL에 유효한 탭이 없으면 DOM의 data-active-tab 속성에서 읽기
    if (!tab || VALID_TABS.indexOf(tab) === -1) {
        var tabContent = document.getElementById('tabContent');
        if (tabContent && tabContent.dataset.activeTab) {
            tab = tabContent.dataset.activeTab;
        }
    }

    // 3. 여전히 유효하지 않으면 history로 fallback
    if (!tab || VALID_TABS.indexOf(tab) === -1) {
        tab = 'history';
    }
    currentTab = tab;

    // 탭 버튼 활성화 상태 설정
    document.querySelectorAll('.tab-btn').forEach(function(btn) {
        btn.classList.remove('active');
    });
    var activeBtn = document.querySelector('[data-tab="' + tab + '"]');
    if (activeBtn) {
        activeBtn.classList.add('active');
    }

    // 콘텐츠 로드
    loadTabContent(tab);

    // 이벤트 위임: 동적으로 로드되는 검색 폼 처리
    document.getElementById('tabContent').addEventListener('submit', function(e) {
        if (e.target.classList.contains('search-form')) {
            e.preventDefault();
            var params = new URLSearchParams(new FormData(e.target)).toString();

            if (currentTab === 'history') {
                loadHistoryContent(params);
            } else if (currentTab === 'multi') {
                loadMultiSubContent(params);
            } else if (currentTab === 'challenge') {
                loadChallengeSubContent(params);
            }
        }
    });

    // 이벤트 위임: 초기화 버튼 처리
    document.getElementById('tabContent').addEventListener('click', function(e) {
        if (e.target.classList.contains('btn-reset')) {
            e.preventDefault();

            if (currentTab === 'history') {
                loadHistoryContent();
            } else if (currentTab === 'multi') {
                loadMultiSubContent();
            } else if (currentTab === 'challenge') {
                loadChallengeSubContent();
            }
        }
    });
});

// 브라우저 뒤로가기/앞으로가기 처리
window.addEventListener('popstate', function(event) {
    if (event.state && event.state.tab) {
        currentTab = event.state.tab;
        loadTabContent(event.state.tab);
        document.querySelectorAll('.tab-btn').forEach(function(btn) {
            btn.classList.remove('active');
        });
        var activeBtn = document.querySelector('[data-tab="' + event.state.tab + '"]');
        if (activeBtn) {
            activeBtn.classList.add('active');
        }
    }
});

// ========== 탭 전환 ==========
function switchTab(tab) {
    if (currentTab === tab) return;

    currentTab = tab;

    // 탭 버튼 활성화 상태 변경
    document.querySelectorAll('.tab-btn').forEach(function(btn) {
        btn.classList.remove('active');
    });
    var activeBtn = document.querySelector('[data-tab="' + tab + '"]');
    if (activeBtn) {
        activeBtn.classList.add('active');
    }

    // URL 업데이트
    var url = new URL(window.location);
    url.searchParams.set('tab', tab);
    history.pushState({tab: tab}, '', url);

    // 콘텐츠 로드
    loadTabContent(tab);
}

// ========== 탭 콘텐츠 로드 ==========
function loadTabContent(tab, params) {
    var container = document.getElementById('tabContent');
    container.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    var url = '';
    switch (tab) {
        case 'history':
            url = '/admin/history/content';
            break;
        case 'multi':
            url = '/admin/multi/content';
            break;
        case 'challenge':
            url = '/admin/challenge/content';
            break;
        default:
            url = '/admin/history/content';
    }

    // 파라미터 추가
    if (params) {
        var searchParams = new URLSearchParams(params);
        url += '?' + searchParams.toString();
    }

    fetch(url)
        .then(function(response) {
            return response.text();
        })
        .then(function(html) {
            container.innerHTML = html;
            initializeTabScripts();
        })
        .catch(function(error) {
            container.innerHTML = '<div class="error-message">콘텐츠를 불러오는데 실패했습니다.</div>';
            console.error('Error loading tab content:', error);
        });
}

// 탭 내 스크립트 초기화
function initializeTabScripts() {
    // 동적으로 로드된 인라인 스크립트만 실행
    // 주의: 외부 스크립트(script.src)는 로드하지 않음 - 함수 충돌 방지
    var scripts = document.querySelectorAll('#tabContent script');

    scripts.forEach(function(script) {
        // 인라인 스크립트만 실행 (외부 스크립트는 무시)
        if (!script.src && script.textContent) {
            var newScript = document.createElement('script');
            newScript.textContent = script.textContent;
            document.body.appendChild(newScript);
        }
    });
}

// 외부 스크립트 순차 로드
function loadScriptsSequentially(urls, callback) {
    if (urls.length === 0) {
        callback();
        return;
    }

    var url = urls.shift();

    // 이미 로드된 스크립트인지 확인
    if (document.querySelector('script[src="' + url + '"]')) {
        loadScriptsSequentially(urls, callback);
        return;
    }

    var script = document.createElement('script');
    script.src = url;
    script.onload = function() { loadScriptsSequentially(urls, callback); };
    script.onerror = function() {
        console.error('Failed to load script:', url);
        loadScriptsSequentially(urls, callback);
    };
    document.body.appendChild(script);
}

// ========== 공통 유틸리티 함수 ==========
function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function openModal(modalId) {
    var modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
        document.body.style.overflow = 'hidden';
    }
}

function closeModal(modalId) {
    var modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
    if (modalId === 'chatModal') {
        var deleteBtn = document.getElementById('deleteAllChatsBtn');
        if (deleteBtn) deleteBtn.style.display = 'none';
    }
}

function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

function goToPage(page) {
    var params = new URLSearchParams();
    var form = document.querySelector('.tab-content .search-form');
    if (form) {
        new FormData(form).forEach(function(value, key) {
            if (value) params.set(key, value);
        });
    }
    params.set('page', page);

    // 현재 탭에 따라 적절한 콘텐츠 로드
    if (currentTab === 'history') {
        loadHistoryContent(params.toString());
    } else if (currentTab === 'multi') {
        loadMultiSubContent(params.toString());
    } else if (currentTab === 'challenge') {
        loadChallengeSubContent(params.toString());
    }
}

// ========== 멀티 운영 - 방 관리 함수 ==========
function viewRoomDetail(id) {
    fetch('/admin/room/detail/' + id)
        .then(function(response) {
            if (!response.ok) throw new Error('방을 찾을 수 없습니다.');
            return response.json();
        })
        .then(function(data) {
            currentRoomId = id;
            var content = document.getElementById('roomDetailContent');
            content.innerHTML = '';

            var grid = document.createElement('div');
            grid.className = 'detail-grid';

            function createItem(label, value) {
                var item = document.createElement('div');
                item.className = 'detail-item';
                item.innerHTML = '<div class="detail-label">' + label + '</div><div class="detail-value">' + value + '</div>';
                return item;
            }

            grid.appendChild(createItem('방 코드', data.roomCode));
            grid.appendChild(createItem('방 이름', escapeHtml(data.roomName)));
            grid.appendChild(createItem('방장', escapeHtml(data.hostNickname)));
            grid.appendChild(createItem('상태', getStatusBadge(data.status)));
            grid.appendChild(createItem('인원', data.currentPlayers + '/' + data.maxPlayers));
            grid.appendChild(createItem('라운드', data.currentRound + '/' + data.totalRounds));
            grid.appendChild(createItem('비공개', data.isPrivate ? '예' : '아니오'));
            grid.appendChild(createItem('생성일', new Date(data.createdAt).toLocaleString('ko-KR')));

            if (data.participants && data.participants.length > 0) {
                var participantsDiv = document.createElement('div');
                participantsDiv.className = 'detail-item full-width';
                participantsDiv.innerHTML = '<div class="detail-label">참가자</div>';

                var participantsList = document.createElement('div');
                participantsList.className = 'participants-list';

                data.participants.forEach(function(p) {
                    var pItem = document.createElement('div');
                    pItem.className = 'participant-item';
                    pItem.innerHTML =
                        '<span class="participant-name">' + escapeHtml(p.nickname) + '</span>' +
                        '<span class="participant-score">' + p.score + '점</span>' +
                        '<span class="participant-status ' + (p.isReady ? 'ready' : '') + '">' + (p.isReady ? '준비완료' : '대기중') + '</span>';
                    participantsList.appendChild(pItem);
                });

                participantsDiv.appendChild(participantsList);
                grid.appendChild(participantsDiv);
            }

            content.appendChild(grid);

            var actions = document.createElement('div');
            actions.className = 'detail-actions';
            var actionsHtml = '<button class="btn btn-info" onclick="viewRoomChats(' + id + ')">채팅 보기</button>';
            if (data.status !== 'FINISHED') {
                actionsHtml += '<button class="btn btn-warning" onclick="closeRoom(' + id + ')">방 종료</button>';
            }
            actionsHtml += '<button class="btn btn-danger" onclick="deleteRoom(' + id + ')">방 삭제</button>';
            actions.innerHTML = actionsHtml;
            content.appendChild(actions);

            openModal('roomDetailModal');
        })
        .catch(function(error) {
            showToast('방 정보를 불러오는데 실패했습니다.', 'error');
        });
}

function getStatusBadge(status) {
    var badges = {
        'WAITING': '<span class="status-badge waiting">대기중</span>',
        'PLAYING': '<span class="status-badge playing">게임중</span>',
        'FINISHED': '<span class="status-badge finished">종료</span>'
    };
    return badges[status] || status;
}

function closeRoom(id) {
    if (!confirm('이 방을 강제 종료하시겠습니까?')) return;

    fetch('/admin/room/close/' + id, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast('방이 종료되었습니다.', 'success');
                closeModal('roomDetailModal');
                loadTabContent('multi');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function(error) {
            showToast('방 종료에 실패했습니다.', 'error');
        });
}

function deleteRoom(id) {
    if (!confirm('이 방을 삭제하시겠습니까? 채팅 기록도 함께 삭제됩니다.')) return;

    fetch('/admin/room/delete/' + id, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast('방이 삭제되었습니다.', 'success');
                closeModal('roomDetailModal');
                loadTabContent('multi');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function(error) {
            showToast('방 삭제에 실패했습니다.', 'error');
        });
}

// ========== 멀티 운영 - 채팅 관리 함수 ==========
function viewRoomChats(id) {
    fetch('/admin/room/chat/' + id)
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (!result.success) throw new Error(result.message);

            currentRoomId = id;
            document.getElementById('chatModalTitle').textContent = '채팅 내역 - ' + escapeHtml(result.roomName) + ' (' + result.roomCode + ')';

            var content = document.getElementById('chatContent');
            content.innerHTML = '';

            if (result.chats.length === 0) {
                content.innerHTML = '<div class="empty-message">채팅 기록이 없습니다.</div>';
            } else {
                var chatList = document.createElement('div');
                chatList.className = 'chat-list';

                result.chats.forEach(function(chat) {
                    var chatItem = document.createElement('div');
                    chatItem.className = 'chat-item ' + chat.messageType.toLowerCase();
                    chatItem.innerHTML =
                        '<div class="chat-header">' +
                        '<span class="chat-nickname">' + escapeHtml(chat.nickname) + '</span>' +
                        '<span class="chat-time">' + new Date(chat.createdAt).toLocaleString('ko-KR') + '</span>' +
                        '<button class="btn btn-sm btn-danger" onclick="deleteChat(' + chat.id + ', event)">삭제</button>' +
                        '</div>' +
                        '<div class="chat-message">' + escapeHtml(chat.message) + '</div>';
                    chatList.appendChild(chatItem);
                });

                content.appendChild(chatList);

                var deleteAllBtn = document.getElementById('deleteAllChatsBtn');
                deleteAllBtn.style.display = 'inline-block';
                deleteAllBtn.onclick = function() { deleteAllChats(id); };
            }

            closeModal('roomDetailModal');
            openModal('chatModal');
        })
        .catch(function(error) {
            showToast('채팅 내역을 불러오는데 실패했습니다.', 'error');
        });
}

function deleteChat(chatId, event) {
    if (event) event.stopPropagation();
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return;

    fetch('/admin/room/chat/delete/' + chatId, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast('채팅이 삭제되었습니다.', 'success');
                if (currentRoomId) viewRoomChats(currentRoomId);
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function(error) {
            showToast('채팅 삭제에 실패했습니다.', 'error');
        });
}

function deleteAllChats(roomId) {
    if (!confirm('이 방의 모든 채팅을 삭제하시겠습니까?')) return;

    fetch('/admin/room/chat/delete-all/' + roomId, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast('모든 채팅이 삭제되었습니다.', 'success');
                closeModal('chatModal');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function(error) {
            showToast('채팅 삭제에 실패했습니다.', 'error');
        });
}

function deleteChatFromList(chatId) {
    if (!confirm('이 채팅을 삭제하시겠습니까?')) return;

    fetch('/admin/chat/delete/' + chatId, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast('채팅이 삭제되었습니다.', 'success');
                loadTabContent('multi');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function(error) {
            showToast('채팅 삭제에 실패했습니다.', 'error');
        });
}

// ========== 챌린지 기록 - 상세보기 함수 ==========
function viewDetail(id, type) {
    var url = type === 'fan'
        ? '/admin/fan-challenge/detail/' + id
        : '/admin/genre-challenge/detail/' + id;

    fetch(url)
        .then(function(response) {
            if (!response.ok) throw new Error('기록을 찾을 수 없습니다.');
            return response.json();
        })
        .then(function(data) {
            document.getElementById('detailModalTitle').textContent =
                type === 'fan' ? '팬 챌린지 기록 상세' : '장르 챌린지 기록 상세';

            renderDetailContent(data, type);
            openModal('detailModal');
        })
        .catch(function(error) {
            showToast('기록을 불러오는데 실패했습니다.', 'error');
        });
}

function renderDetailContent(data, type) {
    var detailContent = document.getElementById('detailContent');
    detailContent.textContent = '';

    var grid = document.createElement('div');
    grid.className = 'detail-grid';

    function createDetailItem(label, value) {
        var item = document.createElement('div');
        item.className = 'detail-item';

        var labelDiv = document.createElement('div');
        labelDiv.className = 'detail-label';
        labelDiv.textContent = label;

        var valueDiv = document.createElement('div');
        valueDiv.className = 'detail-value';
        valueDiv.textContent = value;

        item.appendChild(labelDiv);
        item.appendChild(valueDiv);
        return item;
    }

    var bestTime = data.bestTimeMs ? (data.bestTimeMs / 1000).toFixed(1) + '초' : '-';
    var achievedAt = data.achievedAt ? new Date(data.achievedAt).toLocaleString('ko-KR') : '-';

    grid.appendChild(createDetailItem('회원', data.memberNickname));
    grid.appendChild(createDetailItem('이메일', data.memberEmail));

    if (type === 'fan') {
        grid.appendChild(createDetailItem('아티스트', data.artist));
    } else {
        grid.appendChild(createDetailItem('장르', data.genreName));
    }

    grid.appendChild(createDetailItem('난이도', data.difficultyDisplayName));
    grid.appendChild(createDetailItem('달성 시점 정답/전체', data.correctCount + '/' + data.totalSongs + ' (' + data.clearRate + '%)'));
    grid.appendChild(createDetailItem('현재 시점 정답/전체', data.correctCount + '/' + data.currentSongCount + ' (' + data.currentClearRate + '%)'));

    if (type === 'fan') {
        var perfectItem = document.createElement('div');
        perfectItem.className = 'detail-item';

        var perfectLabel = document.createElement('div');
        perfectLabel.className = 'detail-label';
        perfectLabel.textContent = '퍼펙트 상태';

        var perfectValue = document.createElement('div');
        perfectValue.className = 'detail-value';

        if (data.isPerfectClear) {
            if (data.isCurrentPerfect) {
                perfectValue.innerHTML = '<span class="perfect-badge achieved">✅ 현재 유효</span>';
            } else {
                perfectValue.innerHTML = '<span class="perfect-badge invalid">⭐ 곡 추가로 무효화</span>';
            }
        } else {
            perfectValue.textContent = '미달성';
        }

        perfectItem.appendChild(perfectLabel);
        perfectItem.appendChild(perfectValue);
        grid.appendChild(perfectItem);
    } else {
        grid.appendChild(createDetailItem('최대 콤보', data.maxCombo));
    }

    grid.appendChild(createDetailItem('최고 클리어 시간', bestTime));
    grid.appendChild(createDetailItem('달성일', achievedAt));

    detailContent.appendChild(grid);
}

// ========== 게임 이력 - 정렬 및 필터 함수 ==========
function sortBy(column, baseUrl) {
    var params = new URLSearchParams(window.location.search);
    var currentSort = params.get('sort');
    var currentDirection = params.get('direction') || 'desc';

    if (currentSort === column) {
        params.set('direction', currentDirection === 'asc' ? 'desc' : 'asc');
    } else {
        params.set('sort', column);
        params.set('direction', 'desc');
    }
    params.set('page', '0');
    params.delete('tab');

    // 현재 활성화된 탭에 따라 콘텐츠 로드
    if (currentTab === 'challenge') {
        loadChallengeSubContent(params.toString());
    } else {
        loadTabContent(currentTab, params.toString());
    }
}

// 게임 이력 - 세션 삭제
function deleteSession(id) {
    if (!confirm('정말 삭제하시겠습니까?\n해당 게임의 모든 라운드 정보도 함께 삭제됩니다.')) return;

    fetch('/admin/history/delete/' + id, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast(result.message, 'success');
                loadTabContent('history');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function(error) {
            showToast('삭제 중 오류가 발생했습니다.', 'error');
        });
}

// 게임 이력 - 랭킹 타입 변경 (history 탭 내 서브탭용)
function changeRankType(rankType, artist) {
    var params = new URLSearchParams();
    params.set('rankType', rankType);
    if (artist) params.set('artist', artist);

    // history 탭의 ranking 서브탭 콘텐츠 로드
    var container = document.getElementById('tabContent');
    var url = '/admin/history/ranking/content?' + params.toString();

    fetch(url)
        .then(function(response) { return response.text(); })
        .then(function(html) {
            container.innerHTML = html;
            initializeTabScripts();
        })
        .catch(function(error) {
            console.error('Error:', error);
        });
}

// ========== 서브탭 검색 함수 ==========
// 멀티 운영 - 방 관리 검색
function searchMultiRoom() {
    var form = document.getElementById('multiRoomSearchForm');
    if (form) {
        var params = new URLSearchParams(new FormData(form)).toString();
        loadMultiSubContent(params);
    }
}

function resetMultiRoom() {
    loadMultiSubContent();
}

// 멀티 운영 - 채팅 내역 검색
function searchMultiChat() {
    var form = document.getElementById('multiChatSearchForm');
    if (form) {
        var params = new URLSearchParams(new FormData(form)).toString();
        loadMultiSubContent(params);
    }
}

function resetMultiChat() {
    loadMultiSubContent();
}

// 챌린지 기록 - 팬 챌린지 검색
function searchFanChallenge() {
    var form = document.getElementById('fanChallengeSearchForm');
    if (form) {
        var params = new URLSearchParams(new FormData(form)).toString();
        loadChallengeSubContent(params);
    }
}

function resetFanChallenge() {
    loadChallengeSubContent();
}

// 챌린지 기록 - 장르 챌린지 검색
function searchGenreChallenge() {
    var form = document.getElementById('genreChallengeSearchForm');
    if (form) {
        var params = new URLSearchParams(new FormData(form)).toString();
        loadChallengeSubContent(params);
    }
}

function resetGenreChallenge() {
    loadChallengeSubContent();
}

// 게임 이력 검색
function searchHistory() {
    var form = document.querySelector('.history-tab-content .filter-form');
    if (form) {
        var params = new URLSearchParams(new FormData(form)).toString();
        loadHistoryContent(params);
    }
}

function resetHistory() {
    loadHistoryContent();
}

// 서브탭 콘텐츠 로드 헬퍼 함수
function loadHistoryContent(params) {
    var container = document.getElementById('tabContent');
    var url = '/admin/history/content' + (params ? '?' + params : '');

    fetch(url)
        .then(function(response) { return response.text(); })
        .then(function(html) {
            container.innerHTML = html;
            initializeTabScripts();
        })
        .catch(function(error) {
            console.error('Error:', error);
        });
}

function loadMultiSubContent(params) {
    // 멀티 운영 탭의 서브 콘텐츠 로드
    var subTabContent = document.getElementById('multiSubTabContent');
    if (subTabContent) {
        var activeSubTab = document.querySelector('.sub-tab-btn.active');
        var subTab = activeSubTab ? activeSubTab.getAttribute('data-subtab') : 'room';
        var url = subTab === 'room' ? '/admin/room/content' : '/admin/chat/content';
        if (params) url += '?' + params;

        fetch(url)
            .then(function(response) { return response.text(); })
            .then(function(html) { subTabContent.innerHTML = html; })
            .catch(function(error) { console.error('Error:', error); });
    }
}

function loadChallengeSubContent(params) {
    // 챌린지 기록 탭의 서브 콘텐츠 로드
    var subTabContent = document.getElementById('challengeSubTabContent');
    if (subTabContent) {
        var activeSubTab = document.querySelector('.sub-tab-btn.active');
        var subTab = activeSubTab ? activeSubTab.getAttribute('data-subtab') : 'fan';
        var url;
        if (subTab === 'fan') {
            url = '/admin/fan-challenge/content';
        } else if (subTab === 'genre') {
            url = '/admin/genre-challenge/content';
        } else {
            url = '/admin/fan-challenge/stages/content';
        }
        if (params) url += '?' + params;

        fetch(url)
            .then(function(response) { return response.text(); })
            .then(function(html) { subTabContent.innerHTML = html; })
            .catch(function(error) { console.error('Error:', error); });
    }
}
