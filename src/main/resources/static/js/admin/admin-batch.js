// 이벤트 바인딩
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.batch-card').forEach(function(card) {
        var batchId = card.dataset.batchId;
        var batchName = card.dataset.batchName;

        card.querySelector('.btn-detail').addEventListener('click', function() {
            viewBatchDetail(batchId);
        });

        card.querySelector('.btn-edit').addEventListener('click', function() {
            openEditModal(batchId);
        });

        card.querySelector('.btn-toggle').addEventListener('click', function() {
            toggleEnabled(batchId);
        });

        var runBtn = card.querySelector('.btn-run');
        if (runBtn.dataset.implemented === 'true') {
            runBtn.addEventListener('click', function() {
                runBatch(batchId, batchName);
            });
        }
    });

    document.getElementById('batchEditForm').addEventListener('submit', function(e) {
        e.preventDefault();
        saveBatchConfig();
    });
});

// 필터링 기능
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
        alert('실행할 수 있는 활성화된 배치가 없습니다.');
        return;
    }

    openModal('runningModal');
    var progressEl = document.getElementById('runningProgress');
    var nameEl = document.getElementById('runningBatchName');
    var completed = 0;
    var total = batches.length;

    function runNext(index) {
        if (index >= batches.length) {
            closeModal('runningModal');
            alert('총 ' + total + '개 배치 실행 완료!');
            location.reload();
            return;
        }

        var batch = batches[index];
        nameEl.textContent = batch.name + ' 실행 중...';
        progressEl.textContent = (index + 1) + ' / ' + total;

        fetch('/admin/batch/run/' + batch.id, { method: 'POST' })
            .then(function(response) {
                return response.json();
            })
            .then(function(result) {
                completed++;
                runNext(index + 1);
            })
            .catch(function(error) {
                console.error('배치 실행 오류:', batch.id, error);
                completed++;
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
        .catch(function(error) {
            console.error('배치 상세 조회 오류:', error);
            alert('배치 상세 정보를 불러오는 중 오류가 발생했습니다.');
        });
}

// 배치 설정 수정 모달
function openEditModal(batchId) {
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
        .catch(function(error) {
            console.error('배치 정보 조회 오류:', error);
            alert('배치 정보를 불러오는 중 오류가 발생했습니다.');
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
            alert(result.message);
            closeModal('batchEditModal');
            location.reload();
        } else {
            alert(result.message);
        }
    })
    .catch(function(error) {
        console.error('배치 설정 저장 오류:', error);
        alert('배치 설정 저장 중 오류가 발생했습니다.');
    });
}

// 배치 활성화/비활성화
function toggleEnabled(batchId) {
    fetch('/admin/batch/toggle/' + batchId, { method: 'POST' })
        .then(function(response) {
            return response.json();
        })
        .then(function(result) {
            if (result.success) {
                location.reload();
            } else {
                alert(result.message);
            }
        })
        .catch(function(error) {
            console.error('배치 상태 변경 오류:', error);
            alert('배치 상태 변경 중 오류가 발생했습니다.');
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
                alert(result.message);
                location.reload();
            } else {
                alert(result.message);
                runBtn.disabled = false;
                runBtn.textContent = '수동 실행';
            }
        })
        .catch(function(error) {
            console.error('배치 실행 오류:', error);
            alert('배치 실행 중 오류가 발생했습니다.');
            runBtn.disabled = false;
            runBtn.textContent = '수동 실행';
        });
}

// 스케줄 새로고침
function refreshSchedules() {
    fetch('/admin/batch/refresh-schedules', { method: 'POST' })
        .then(function(response) {
            return response.json();

        .then(function(result) {
            if (result.success) {
                alert(result.message + ' (등록: ' + result.scheduledCount + '개)');
                location.reload();
            } else {
                alert(result.message);
            }
        })
        .catch(function(error) {
            console.error('스케줄 새로고침 오류:', error);
            alert('스케줄 새로고침 중 오류가 발생했습니다.');
        });
}

// 모달 제어
function openModal(modalId) {
    document.getElementById(modalId).classList.add('show');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

document.querySelectorAll('.modal').forEach(function(modal) {
    modal.addEventListener('click', function(e) {
        if (e.target === this && this.id !== 'runningModal') {
            this.classList.remove('show');
        }
    });
});

// 유틸리티
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
