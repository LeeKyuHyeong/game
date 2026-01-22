const modal = document.getElementById('genreModal');
const modalTitle = document.getElementById('modalTitle');
const genreForm = document.getElementById('genreForm');

function openModal() {
    modalTitle.textContent = '장르 추가';
    genreForm.reset();
    document.getElementById('genreId').value = '';
    document.getElementById('code').readOnly = false;
    document.querySelector('input[name="useYn"][value="Y"]').checked = true;
    modal.classList.add('show');
}

function closeModal() {
    modal.classList.remove('show');
    genreForm.reset();
}

async function editGenre(id) {
    try {
        const response = await fetch(`/admin/genre/detail/${id}`);
        if (!response.ok) throw new Error('장르 정보를 불러올 수 없습니다.');

        const genre = await response.json();

        modalTitle.textContent = '장르 수정';
        document.getElementById('genreId').value = genre.id;
        document.getElementById('code').value = genre.code || '';
        document.getElementById('code').readOnly = true;
        document.getElementById('name').value = genre.name || '';
        document.getElementById('displayOrder').value = genre.displayOrder || 0;

        const useYnRadio = document.querySelector(`input[name="useYn"][value="${genre.useYn || 'Y'}"]`);
        if (useYnRadio) useYnRadio.checked = true;

        modal.classList.add('show');
    } catch (error) {
        showToast(error.message);
    }
}

async function deleteGenre(id) {
    if (!confirm('정말 삭제하시겠습니까?\n해당 장르를 사용하는 노래가 있으면 삭제되지 않습니다.')) return;

    try {
        const response = await fetch(`/admin/genre/delete/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            showToast(result.message);
            location.reload();
        } else {
            showToast(result.message);
        }
    } catch (error) {
        showToast('삭제 중 오류가 발생했습니다.');
    }
}

async function toggleStatus(id) {
    try {
        const response = await fetch(`/admin/genre/toggle/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            location.reload();
        } else {
            showToast(result.message);
        }
    } catch (error) {
        showToast('상태 변경 중 오류가 발생했습니다.');
    }
}

genreForm.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = {
        id: document.getElementById('genreId').value || null,
        code: document.getElementById('code').value.toUpperCase(),
        name: document.getElementById('name').value,
        displayOrder: parseInt(document.getElementById('displayOrder').value) || 0,
        useYn: document.querySelector('input[name="useYn"]:checked').value
    };

    try {
        const response = await fetch('/admin/genre/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });

        const result = await response.json();

        if (result.success) {
            showToast(result.message);
            closeModal();
            location.reload();
        } else {
            showToast(result.message);
        }
    } catch (error) {
        showToast('저장 중 오류가 발생했습니다.');
    }
});

modal.addEventListener('click', function(e) {
    if (e.target === modal) closeModal();
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && modal.classList.contains('show')) closeModal();
});

// 행 펼치기/접기 (모바일)
function toggleRowExpand(row) {
    if (window.innerWidth <= 768) {
        row.classList.toggle('expanded');
    }
}

// 정렬
function sortBy(column) {
    const params = new URLSearchParams(window.location.search);
    if (typeof currentSort !== 'undefined' && currentSort === column) {
        params.set('direction', (typeof currentDirection !== 'undefined' && currentDirection === 'asc') ? 'desc' : 'asc');
    } else {
        params.set('sort', column);
        params.set('direction', 'asc');
    }
    params.set('page', '0');
    window.location.href = '/admin/genre?' + params.toString();
}