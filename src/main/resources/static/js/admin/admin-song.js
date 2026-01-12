const modal = document.getElementById('songModal');
const modalTitle = document.getElementById('modalTitle');
const songForm = document.getElementById('songForm');

function openModal() {
    modalTitle.textContent = '노래 추가';
    songForm.reset();
    document.getElementById('songId').value = '';
    document.getElementById('currentYoutubeId').textContent = '';
    document.querySelector('input[name="useYn"][value="Y"]').checked = true;
    document.querySelector('input[name="isSolo"][value="false"]').checked = true;
    modal.classList.add('show');
}

function closeModal() {
    modal.classList.remove('show');
    songForm.reset();
    document.getElementById('currentYoutubeId').textContent = '';
}

async function editSong(id) {
    try {
        const response = await fetch(`/admin/song/detail/${id}`);
        if (!response.ok) throw new Error('노래 정보를 불러올 수 없습니다.');

        const song = await response.json();

        modalTitle.textContent = '노래 수정';
        document.getElementById('songId').value = song.id;
        document.getElementById('title').value = song.title || '';
        document.getElementById('artist').value = song.artist || '';
        document.getElementById('startTime').value = song.startTime || 0;
        document.getElementById('playDuration').value = song.playDuration || 10;
        document.getElementById('genreId').value = song.genreId || '';
        document.getElementById('releaseYear').value = song.releaseYear || '';

        const isSoloRadio = document.querySelector(`input[name="isSolo"][value="${song.isSolo}"]`);
        if (isSoloRadio) isSoloRadio.checked = true;

        const useYnRadio = document.querySelector(`input[name="useYn"][value="${song.useYn || 'Y'}"]`);
        if (useYnRadio) useYnRadio.checked = true;

        const currentYoutubeIdDiv = document.getElementById('currentYoutubeId');
        if (song.youtubeVideoId) {
            currentYoutubeIdDiv.innerHTML = `<span class="file-info">현재 Video ID: ${song.youtubeVideoId}</span>`;
        } else {
            currentYoutubeIdDiv.textContent = '';
        }

        modal.classList.add('show');
    } catch (error) {
        alert(error.message);
    }
}

async function deleteSong(id) {
    if (!confirm('정말 삭제하시겠습니까?')) return;

    try {
        const response = await fetch(`/admin/song/delete/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            alert(result.message);
            location.reload();
        } else {
            alert(result.message);
        }
    } catch (error) {
        alert('삭제 중 오류가 발생했습니다.');
    }
}

async function toggleStatus(id) {
    try {
        const response = await fetch(`/admin/song/toggle/${id}`, { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            location.reload();
        } else {
            alert(result.message);
        }
    } catch (error) {
        alert('상태 변경 중 오류가 발생했습니다.');
    }
}

songForm.addEventListener('submit', async function(e) {
    e.preventDefault();

    const formData = new FormData();

    const songId = document.getElementById('songId').value;
    if (songId) formData.append('id', songId);

    formData.append('title', document.getElementById('title').value);
    formData.append('artist', document.getElementById('artist').value);
    formData.append('startTime', document.getElementById('startTime').value || 0);
    formData.append('playDuration', document.getElementById('playDuration').value || 10);

    const genreId = document.getElementById('genreId').value;
    if (genreId) formData.append('genreId', genreId);

    const releaseYear = document.getElementById('releaseYear').value;
    if (releaseYear) formData.append('releaseYear', releaseYear);

    formData.append('isSolo', document.querySelector('input[name="isSolo"]:checked').value);
    formData.append('useYn', document.querySelector('input[name="useYn"]:checked').value);

    const youtubeUrl = document.getElementById('youtubeUrl').value;
    if (youtubeUrl) formData.append('youtubeUrl', youtubeUrl);

    try {
        const response = await fetch('/admin/song/save', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            alert(result.message);
            closeModal();
            location.reload();
        } else {
            alert(result.message);
        }
    } catch (error) {
        alert('저장 중 오류가 발생했습니다.');
    }
});

modal.addEventListener('click', function(e) {
    if (e.target === modal) closeModal();
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && modal.classList.contains('show')) closeModal();
});