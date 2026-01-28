/**
 * 시스템 설정 통합 페이지 JavaScript
 * 배치 관리, 메뉴 관리 탭을 하나의 JS로 통합
 */

var currentTab = 'batch';
var currentParams = {};

// ========== Initialization ==========

document.addEventListener('DOMContentLoaded', function() {
    const urlParams = new URLSearchParams(window.location.search);
    const tab = urlParams.get('tab') || currentTab;
    currentTab = tab;

    loadTabContent(tab);

    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.querySelector('[data-tab="' + tab + '"]');
    if (activeBtn) {
        activeBtn.classList.add('active');
    }

    // 모달 이벤트 설정
    setupModalEvents();
});

// 브라우저 뒤로가기/앞으로가기 처리
window.addEventListener('popstate', function(event) {
    if (event.state && event.state.tab) {
        currentTab = event.state.tab;
        loadTabContent(event.state.tab);
        document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
        const activeBtn = document.querySelector('[data-tab="' + event.state.tab + '"]');
        if (activeBtn) activeBtn.classList.add('active');
    }
});

// ========== Tab Management ==========

function switchTab(tab) {
    if (currentTab === tab) return;

    currentTab = tab;
    currentParams = {};

    // 탭 버튼 활성화 상태 변경
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.querySelector('[data-tab="' + tab + '"]');
    if (activeBtn) activeBtn.classList.add('active');

    // URL 업데이트
    const url = new URL(window.location);
    url.searchParams.set('tab', tab);
    history.pushState({tab: tab}, '', url);

    // 콘텐츠 로드
    loadTabContent(tab);
}

function loadTabContent(tab, params) {
    const container = document.getElementById('tabContent');
    container.innerHTML = '<div class="loading-spinner"><div class="spinner"></div><span>로딩 중...</span></div>';

    let url = '';
    switch (tab) {
        case 'batch':
            url = '/admin/batch/content';
            break;
        case 'menu':
            url = '/admin/menu/content';
            break;
        case 'badword':
            url = '/admin/badword/content';
            break;
        default:
            url = '/admin/batch/content';
    }

    // 파라미터 추가
    if (params) {
        const searchParams = typeof params === 'string' ? params : new URLSearchParams(params).toString();
        url += '?' + searchParams;
    }

    fetch(url)
        .then(response => response.text())
        .then(html => {
            container.innerHTML = html;
            initializeTabScripts();
        })
        .catch(error => {
            container.innerHTML = '<div class="error-message">콘텐츠를 불러오는데 실패했습니다.</div>';
            console.error('Error loading tab content:', error);
        });
}

function initializeTabScripts() {
    // 동적으로 로드된 인라인 스크립트만 실행
    // 주의: 외부 스크립트(script.src)는 로드하지 않음 - 함수 충돌 방지
    const scripts = document.querySelectorAll('#tabContent script');

    scripts.forEach(script => {
        // 인라인 스크립트만 실행 (외부 스크립트는 무시)
        if (!script.src && script.textContent) {
            const newScript = document.createElement('script');
            newScript.textContent = script.textContent;
            document.body.appendChild(newScript);
        }
    });

    // 탭별 초기화
    if (currentTab === 'batch') {
        initBatchTab();
    } else if (currentTab === 'menu') {
        initMenuTab();
    } else if (currentTab === 'badword') {
        initBadWordTab();
    }
}

// 외부 스크립트 순차 로드
function loadScriptsSequentially(urls, callback) {
    if (urls.length === 0) {
        callback();
        return;
    }

    const url = urls.shift();

    // 이미 로드된 스크립트인지 확인
    if (document.querySelector(`script[src="${url}"]`)) {
        loadScriptsSequentially(urls, callback);
        return;
    }

    const script = document.createElement('script');
    script.src = url;
    script.onload = () => loadScriptsSequentially(urls, callback);
    script.onerror = () => {
        console.error('Failed to load script:', url);
        loadScriptsSequentially(urls, callback);
    };
    document.body.appendChild(script);
}

// ========== Batch Tab Functions ==========

function initBatchTab() {
    // 배치 카드 이벤트 바인딩
    document.querySelectorAll('.batch-card').forEach(function(card) {
        var batchId = card.dataset.batchId;
        var batchName = card.dataset.batchName;

        var detailBtn = card.querySelector('.btn-detail');
        if (detailBtn) {
            detailBtn.addEventListener('click', function() {
                viewBatchDetail(batchId);
            });
        }

        var editBtn = card.querySelector('.btn-edit');
        if (editBtn) {
            editBtn.addEventListener('click', function() {
                openBatchEditModal(batchId);
            });
        }

        var toggleBtn = card.querySelector('.btn-toggle');
        if (toggleBtn) {
            toggleBtn.addEventListener('click', function() {
                toggleBatchEnabled(batchId);
            });
        }

        var runBtn = card.querySelector('.btn-run');
        if (runBtn && runBtn.dataset.implemented === 'true') {
            runBtn.addEventListener('click', function() {
                runBatch(batchId, batchName);
            });
        }
    });

    // 배치 수정 폼 이벤트
    var batchEditForm = document.getElementById('batchEditForm');
    if (batchEditForm) {
        batchEditForm.addEventListener('submit', function(e) {
            e.preventDefault();
            saveBatchConfig();
        });
    }
}

// 배치 필터링
function filterBatches() {
    var priority = document.getElementById('filterPriority').value;
    var status = document.getElementById('filterStatus').value;
    var result = document.getElementById('filterResult').value;

    document.querySelectorAll('.batch-card').forEach(function(card) {
        var show = true;

        if (priority && card.dataset.priority !== priority) {
            show = false;
        }

        if (status) {
            var isEnabled = card.dataset.enabled === 'true';
            if (status === 'enabled' && !isEnabled) show = false;
            if (status === 'disabled' && isEnabled) show = false;
        }

        if (result) {
            var cardResult = card.dataset.result;
            if (result === 'never' && cardResult) show = false;
            if (result === 'SUCCESS' && cardResult !== 'SUCCESS') show = false;
            if (result === 'FAIL' && cardResult !== 'FAIL') show = false;
        }

        card.style.display = show ? '' : 'none';
    });
}

// 전체 배치 수동 실행
function runAllBatches() {
    if (!confirm('모든 구현된 배치를 수동으로 실행하시겠습니까?\n\n이 작업은 시간이 걸릴 수 있습니다.')) return;

    var batchCards = document.querySelectorAll('.batch-card');
    var batches = [];

    batchCards.forEach(function(card) {
        var runBtn = card.querySelector('.btn-run');
        if (runBtn && runBtn.dataset.implemented === 'true' && card.dataset.enabled === 'true') {
            batches.push({
                id: card.dataset.batchId,
                name: card.dataset.batchName
            });
        }
    });

    if (batches.length === 0) {
        showToast('실행할 수 있는 활성화된 배치가 없습니다.', 'warning');
        return;
    }

    openModal('runningModal');
    var progressEl = document.getElementById('runningProgress');
    var nameEl = document.getElementById('runningBatchName');
    var total = batches.length;

    function runNext(index) {
        if (index >= batches.length) {
            closeModal('runningModal');
            showToast('총 ' + total + '개 배치 실행 완료!', 'success');
            loadTabContent('batch');
            return;
        }

        var batch = batches[index];
        nameEl.textContent = batch.name + ' 실행 중...';
        progressEl.textContent = (index + 1) + ' / ' + total;

        fetch('/admin/batch/run/' + batch.id, { method: 'POST' })
            .then(function(response) {
                return response.json();
            })
            .then(function() {
                runNext(index + 1);
            })
            .catch(function() {
                runNext(index + 1);
            });
    }

    runNext(0);
}

// 배치 상세 조회
function viewBatchDetail(batchId) {
    fetch('/admin/batch/detail/' + batchId)
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            document.getElementById('batchDetailTitle').textContent = data.name + ' 상세';

            var html = '<div class="detail-grid">';
            html += '<div class="detail-item"><span class="detail-label">배치 ID</span><span class="detail-value"><code>' + data.batchId + '</code></span></div>';
            html += '<div class="detail-item"><span class="detail-label">대상 엔티티</span><span class="detail-value">' + data.targetEntity + '</span></div>';
            html += '<div class="detail-item"><span class="detail-label">우선순위</span><span class="detail-value"><span class="batch-priority ' + data.priority.toLowerCase() + '">' + data.priority + '</span></span></div>';
            html += '<div class="detail-item"><span class="detail-label">Cron 표현식</span><span class="detail-value"><code>' + data.cronExpression + '</code></span></div>';
            html += '<div class="detail-item"><span class="detail-label">실행 주기</span><span class="detail-value">' + data.scheduleText + '</span></div>';
            html += '<div class="detail-item"><span class="detail-label">상태</span><span class="detail-value">';
            html += data.implemented ? '<span class="status-badge active">구현됨</span>' : '<span class="status-badge inactive">미구현</span>';
            html += data.enabled ? '<span class="status-badge active">활성화</span>' : '<span class="status-badge inactive">비활성화</span>';
            if (data.isScheduled) html += '<span class="status-badge scheduled">스케줄됨</span>';
            html += '</span></div>';
            html += '<div class="detail-item full-width"><span class="detail-label">설명</span><span class="detail-value">' + data.description + '</span></div>';
            html += '</div>';

            if (data.lastExecutedAt) {
                html += '<div class="detail-section"><h4>마지막 실행 정보</h4><div class="detail-grid">';
                html += '<div class="detail-item"><span class="detail-label">실행 시간</span><span class="detail-value">' + formatDateTime(data.lastExecutedAt) + '</span></div>';
                html += '<div class="detail-item"><span class="detail-label">결과</span><span class="detail-value"><span class="result-badge ' + (data.lastResult === 'SUCCESS' ? 'success' : 'fail') + '">' + data.lastResult + '</span></span></div>';
                html += '<div class="detail-item"><span class="detail-label">처리 건수</span><span class="detail-value">' + (data.lastAffectedCount !== null ? data.lastAffectedCount + '건' : '-') + '</span></div>';
                html += '<div class="detail-item"><span class="detail-label">소요 시간</span><span class="detail-value">' + (data.lastExecutionTimeMs !== null ? data.lastExecutionTimeMs + 'ms' : '-') + '</span></div>';
                if (data.lastResultMessage) {
                    html += '<div class="detail-item full-width"><span class="detail-label">메시지</span><span class="detail-value result-message">' + data.lastResultMessage + '</span></div>';
                }
                html += '</div></div>';
            }

            if (data.recentHistory && data.recentHistory.length > 0) {
                html += '<div class="detail-section"><h4>최근 실행 이력 (최대 10건)</h4>';
                html += '<div class="history-table-wrapper"><table class="data-table mini-table"><thead><tr><th>실행 시간</th><th>유형</th><th>결과</th><th>처리</th><th>소요</th><th>메시지</th></tr></thead><tbody>';
                data.recentHistory.forEach(function(h) {
                    html += '<tr class="' + (h.result === 'FAIL' ? 'fail-row' : '') + '">';
                    html += '<td>' + formatDateTime(h.executedAt) + '</td>';
                    html += '<td><span class="exec-type ' + h.executionType.toLowerCase() + '">' + h.executionType + '</span></td>';
                    html += '<td><span class="result-badge ' + (h.result === 'SUCCESS' ? 'success' : 'fail') + '">' + h.result + '</span></td>';
                    html += '<td>' + (h.affectedCount !== null ? h.affectedCount + '건' : '-') + '</td>';
                    html += '<td>' + (h.executionTimeMs !== null ? h.executionTimeMs + 'ms' : '-') + '</td>';
                    html += '<td class="message-cell">' + (h.message || '-') + '</td>';
                    html += '</tr>';
                });
                html += '</tbody></table></div></div>';
            } else {
                html += '<div class="detail-section"><h4>최근 실행 이력</h4><p class="no-data">실행 이력이 없습니다.</p></div>';
            }

            document.getElementById('batchDetailContent').innerHTML = html;
            openModal('batchDetailModal');
        })
        .catch(function() {
            showToast('배치 상세 정보를 불러오는 중 오류가 발생했습니다.', 'error');
        });
}

// 배치 설정 수정 모달
function openBatchEditModal(batchId) {
    fetch('/admin/batch/detail/' + batchId)
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            document.getElementById('editBatchId').value = data.batchId;
            document.getElementById('editBatchName').value = data.name;
            document.getElementById('editCronExpression').value = data.cronExpression;
            document.getElementById('editScheduleText').value = data.scheduleText || '';
            openModal('batchEditModal');
        })
        .catch(function() {
            showToast('배치 정보를 불러오는 중 오류가 발생했습니다.', 'error');
        });
}

function setCronPreset(cron, text) {
    document.getElementById('editCronExpression').value = cron;
    document.getElementById('editScheduleText').value = text;
}

function saveBatchConfig() {
    var batchId = document.getElementById('editBatchId').value;
    var cronExpression = document.getElementById('editCronExpression').value;
    var scheduleText = document.getElementById('editScheduleText').value;

    var params = new URLSearchParams();
    params.append('cronExpression', cronExpression);
    params.append('scheduleText', scheduleText);

    fetch('/admin/batch/update/' + batchId, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(result) {
        if (result.success) {
            showToast(result.message, 'success');
            closeModal('batchEditModal');
            loadTabContent('batch');
        } else {
            showToast(result.message, 'error');
        }
    })
    .catch(function() {
        showToast('배치 설정 저장 중 오류가 발생했습니다.', 'error');
    });
}

// 배치 활성화/비활성화
function toggleBatchEnabled(batchId) {
    fetch('/admin/batch/toggle/' + batchId, { method: 'POST' })
        .then(function(response) {
            return response.json();
        })
        .then(function(result) {
            if (result.success) {
                loadTabContent('batch');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function() {
            showToast('배치 상태 변경 중 오류가 발생했습니다.', 'error');
        });
}

// 배치 수동 실행
function runBatch(batchId, batchName) {
    if (!confirm('"' + batchName + '" 배치를 수동으로 실행하시겠습니까?')) return;

    var card = document.querySelector('[data-batch-id="' + batchId + '"]');
    var runBtn = card.querySelector('.btn-run');
    runBtn.disabled = true;
    runBtn.innerHTML = '<span class="btn-spinner"></span>';

    fetch('/admin/batch/run/' + batchId, { method: 'POST' })
        .then(function(response) {
            return response.json();
        })
        .then(function(result) {
            if (result.success) {
                showToast(result.message, 'success');
                loadTabContent('batch');
            } else {
                showToast(result.message, 'error');
                runBtn.disabled = false;
                runBtn.textContent = '수동 실행';
            }
        })
        .catch(function() {
            showToast('배치 실행 중 오류가 발생했습니다.', 'error');
            runBtn.disabled = false;
            runBtn.textContent = '수동 실행';
        });
}

// 스케줄 새로고침
function refreshSchedules() {
    fetch('/admin/batch/refresh-schedules', { method: 'POST' })
        .then(function(response) {
            return response.json();
        })
        .then(function(result) {
            if (result.success) {
                showToast(result.message + ' (등록: ' + result.scheduledCount + '개)', 'success');
                loadTabContent('batch');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function() {
            showToast('스케줄 새로고침 중 오류가 발생했습니다.', 'error');
        });
}

// 하위호환 함수
function refreshBatchSchedules() {
    refreshSchedules();
}

// ========== Menu Tab Functions ==========

function initMenuTab() {
    // 메뉴 탭 초기화 로직 (필요시 추가)
}

async function toggleMenu(menuId) {
    try {
        const response = await fetch(`/admin/menu/toggle/${menuId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();

        if (result.success) {
            showToast(result.message, 'success');
            loadTabContent('menu');
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        showToast('메뉴 상태 변경 중 오류가 발생했습니다.', 'error');
    }
}

// ========== BadWord Tab Functions ==========

function initBadWordTab() {
    // 필터 폼 이벤트 바인딩
    var filterForm = document.getElementById('badwordFilterForm');
    if (filterForm) {
        filterForm.addEventListener('submit', function(e) {
            e.preventDefault();
            var params = new URLSearchParams(new FormData(filterForm)).toString();
            loadTabContent('badword', params);
        });
    }

    // 금칙어 추가/수정 폼 이벤트
    var badWordForm = document.getElementById('badWordForm');
    if (badWordForm) {
        badWordForm.addEventListener('submit', function(e) {
            e.preventDefault();
            saveBadWord();
        });
    }

    // 일괄 등록 폼 이벤트
    var bulkAddForm = document.getElementById('bulkAddForm');
    if (bulkAddForm) {
        bulkAddForm.addEventListener('submit', function(e) {
            e.preventDefault();
            bulkAddBadWords();
        });
    }
}

// 금칙어 추가 모달 열기
function openBadWordModal() {
    document.getElementById('badWordModalTitle').textContent = '비속어 추가';
    document.getElementById('badWordId').value = '';
    document.getElementById('word').value = '';
    document.getElementById('replacement').value = '';
    document.getElementById('isActive').checked = true;
    openModal('badWordModal');
}

// 금칙어 수정 모달 열기
function editBadWord(id) {
    fetch('/admin/badword/detail/' + id)
        .then(function(response) { return response.json(); })
        .then(function(data) {
            document.getElementById('badWordModalTitle').textContent = '비속어 수정';
            document.getElementById('badWordId').value = data.id;
            document.getElementById('word').value = data.word;
            document.getElementById('replacement').value = data.replacement || '';
            document.getElementById('isActive').checked = data.isActive;
            openModal('badWordModal');
        })
        .catch(function() {
            showToast('비속어 정보를 불러오는 중 오류가 발생했습니다.', 'error');
        });
}

// 금칙어 저장 (추가/수정)
function saveBadWord() {
    var id = document.getElementById('badWordId').value;
    var word = document.getElementById('word').value.trim();
    var replacement = document.getElementById('replacement').value.trim();
    var isActive = document.getElementById('isActive').checked;

    if (!word) {
        showToast('비속어를 입력해주세요.', 'error');
        return;
    }

    var params = new URLSearchParams();
    if (id) params.append('id', id);
    params.append('word', word);
    params.append('replacement', replacement);
    params.append('isActive', isActive);

    fetch('/admin/badword/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(function(response) { return response.json(); })
    .then(function(result) {
        if (result.success) {
            showToast(result.message, 'success');
            closeBadWordModal();
            loadTabContent('badword');
        } else {
            showToast(result.message, 'error');
        }
    })
    .catch(function() {
        showToast('저장 중 오류가 발생했습니다.', 'error');
    });
}

// 금칙어 삭제
function deleteBadWord(id) {
    if (!confirm('이 비속어를 삭제하시겠습니까?')) return;

    fetch('/admin/badword/delete/' + id, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast(result.message, 'success');
                loadTabContent('badword');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function() {
            showToast('삭제 중 오류가 발생했습니다.', 'error');
        });
}

// 금칙어 상태 토글
function toggleBadWord(id) {
    fetch('/admin/badword/toggle/' + id, { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast(result.message, 'success');
                loadTabContent('badword');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function() {
            showToast('상태 변경 중 오류가 발생했습니다.', 'error');
        });
}

// 금칙어 모달 닫기
function closeBadWordModal() {
    closeModal('badWordModal');
}

// 일괄 등록 모달 열기
function openBulkAddModal() {
    document.getElementById('bulkWords').value = '';
    openModal('bulkAddModal');
}

// 일괄 등록 모달 닫기
function closeBulkAddModal() {
    closeModal('bulkAddModal');
}

// 일괄 등록 실행
function bulkAddBadWords() {
    var words = document.getElementById('bulkWords').value.trim();
    if (!words) {
        showToast('비속어 목록을 입력해주세요.', 'error');
        return;
    }

    var params = new URLSearchParams();
    params.append('words', words);

    fetch('/admin/badword/bulk-add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(function(response) { return response.json(); })
    .then(function(result) {
        if (result.success) {
            showToast(result.message, 'success');
            closeBulkAddModal();
            loadTabContent('badword');
        } else {
            showToast(result.message, 'error');
        }
    })
    .catch(function() {
        showToast('일괄 등록 중 오류가 발생했습니다.', 'error');
    });
}

// 필터 테스트 모달 열기
function openTestModal() {
    document.getElementById('testMessage').value = '';
    document.getElementById('testResult').style.display = 'none';
    openModal('testModal');
}

// 필터 테스트 모달 닫기
function closeTestModal() {
    closeModal('testModal');
}

// 필터 테스트 실행
function testBadWordFilter() {
    var message = document.getElementById('testMessage').value.trim();
    if (!message) {
        showToast('테스트할 메시지를 입력해주세요.', 'error');
        return;
    }

    var params = new URLSearchParams();
    params.append('message', message);

    fetch('/admin/badword/test', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params
    })
    .then(function(response) { return response.json(); })
    .then(function(result) {
        if (result.success) {
            document.getElementById('testOriginal').textContent = result.original;
            document.getElementById('testFiltered').textContent = result.filtered;
            document.getElementById('testFound').textContent = result.foundWords.length > 0 ? result.foundWords.join(', ') : '없음';
            document.getElementById('testResult').style.display = 'block';
        } else {
            showToast(result.message, 'error');
        }
    })
    .catch(function() {
        showToast('테스트 중 오류가 발생했습니다.', 'error');
    });
}

// 캐시 갱신
function reloadBadWordCache() {
    if (!confirm('금칙어 캐시를 갱신하시겠습니까?')) return;

    fetch('/admin/badword/reload-cache', { method: 'POST' })
        .then(function(response) { return response.json(); })
        .then(function(result) {
            if (result.success) {
                showToast(result.message, 'success');
            } else {
                showToast(result.message, 'error');
            }
        })
        .catch(function() {
            showToast('캐시 갱신 중 오류가 발생했습니다.', 'error');
        });
}

// 금칙어 필터 초기화
function resetBadWordFilter() {
    loadTabContent('badword');
}

// 금칙어 정렬
function sortBadWordBy(column) {
    var currentSort = typeof badwordSort !== 'undefined' ? badwordSort : 'createdAt';
    var currentDir = typeof badwordDirection !== 'undefined' ? badwordDirection : 'desc';
    var newDir = (currentSort === column && currentDir === 'desc') ? 'asc' : 'desc';

    var params = new URLSearchParams();
    params.set('sort', column);
    params.set('direction', newDir);

    var filterForm = document.getElementById('badwordFilterForm');
    if (filterForm) {
        new FormData(filterForm).forEach(function(v, k) {
            if (v) params.set(k, v);
        });
    }

    loadTabContent('badword', params.toString());
}

// 금칙어 페이지 이동
function loadBadWordPage(page) {
    var params = new URLSearchParams();
    params.set('page', page);

    var currentSort = typeof badwordSort !== 'undefined' ? badwordSort : 'createdAt';
    var currentDir = typeof badwordDirection !== 'undefined' ? badwordDirection : 'desc';
    params.set('sort', currentSort);
    params.set('direction', currentDir);

    var filterForm = document.getElementById('badwordFilterForm');
    if (filterForm) {
        new FormData(filterForm).forEach(function(v, k) {
            if (v) params.set(k, v);
        });
    }

    loadTabContent('badword', params.toString());
}

// ========== Modal Functions ==========

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
}

function setupModalEvents() {
    // ESC 키로 모달 닫기
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            var openModals = document.querySelectorAll('.modal.show');
            openModals.forEach(function(modal) {
                if (modal.id !== 'runningModal') {
                    closeModal(modal.id);
                }
            });
        }
    });

    // 모달 바깥 클릭 시 닫기
    document.querySelectorAll('.modal').forEach(function(modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this && this.id !== 'runningModal') {
                closeModal(this.id);
            }
        });
    });
}

// ========== Utility Functions ==========

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    var date = new Date(dateStr);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}
