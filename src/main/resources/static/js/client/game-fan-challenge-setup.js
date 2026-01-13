// 최고 팬 챌린지 설정 페이지 JavaScript

let selectedArtist = null;
let artistList = [];
let searchTimeout = null;

document.addEventListener('DOMContentLoaded', function() {
    // 닉네임 초기값 설정
    if (typeof memberNickname !== 'undefined' && memberNickname) {
        document.getElementById('nickname').value = memberNickname;
    }

    // 아티스트 목록 로드
    loadArtists();

    // 검색 이벤트 설정
    const searchInput = document.getElementById('artistSearch');
    searchInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            searchArtists(this.value);
        }, 300);
    });

    // 검색창 포커스 아웃 시 결과 숨기기
    searchInput.addEventListener('blur', function() {
        setTimeout(() => {
            document.getElementById('artistSearchResults').style.display = 'none';
        }, 200);
    });

    // 검색창 포커스 시 결과 표시
    searchInput.addEventListener('focus', function() {
        if (this.value.trim()) {
            searchArtists(this.value);
        }
    });
});

async function loadArtists() {
    try {
        const response = await fetch('/game/fan-challenge/artists');
        if (!response.ok) throw new Error('아티스트 목록 로드 실패');

        artistList = await response.json();

        // 곡 수가 많은 순으로 정렬 후 상위 20개 표시
        const sortedArtists = [...artistList].sort((a, b) => b.count - a.count).slice(0, 20);

        const container = document.getElementById('artistList');
        container.innerHTML = '';

        sortedArtists.forEach(artist => {
            const item = document.createElement('div');
            item.className = 'artist-item';
            item.innerHTML = `
                <span class="artist-name">${artist.name}</span>
                <span class="song-count">${artist.count}곡</span>
            `;
            item.onclick = () => selectArtist(artist.name, artist.count);
            container.appendChild(item);
        });
    } catch (error) {
        console.error('아티스트 로드 오류:', error);
    }
}

async function searchArtists(keyword) {
    const resultsContainer = document.getElementById('artistSearchResults');

    if (!keyword.trim()) {
        resultsContainer.style.display = 'none';
        return;
    }

    try {
        // 로컬 필터링 (이미 로드된 목록에서)
        const filtered = artistList.filter(a =>
            a.name.toLowerCase().includes(keyword.toLowerCase())
        ).slice(0, 10);

        resultsContainer.innerHTML = '';

        if (filtered.length === 0) {
            resultsContainer.innerHTML = '<div class="no-results">검색 결과가 없습니다</div>';
        } else {
            filtered.forEach(artist => {
                const item = document.createElement('div');
                item.className = 'search-result-item';
                item.innerHTML = `
                    <span class="artist-name">${artist.name}</span>
                    <span class="song-count">${artist.count}곡</span>
                `;
                item.onclick = () => {
                    selectArtist(artist.name, artist.count);
                    resultsContainer.style.display = 'none';
                    document.getElementById('artistSearch').value = '';
                };
                resultsContainer.appendChild(item);
            });
        }

        resultsContainer.style.display = 'block';
    } catch (error) {
        console.error('아티스트 검색 오류:', error);
    }
}

function selectArtist(name, count) {
    selectedArtist = { name, count };

    document.getElementById('selectedArtistName').textContent = name;
    document.getElementById('selectedArtistCount').textContent = count + '곡';
    document.getElementById('selectedArtistContainer').style.display = 'flex';

    // 아티스트 목록에서 선택된 항목 하이라이트
    document.querySelectorAll('.artist-item').forEach(item => {
        item.classList.remove('selected');
        if (item.querySelector('.artist-name').textContent === name) {
            item.classList.add('selected');
        }
    });

    updateStartButton();
}

function clearSelectedArtist() {
    selectedArtist = null;
    document.getElementById('selectedArtistContainer').style.display = 'none';
    document.querySelectorAll('.artist-item').forEach(item => {
        item.classList.remove('selected');
    });
    updateStartButton();
}

function updateStartButton() {
    const nickname = document.getElementById('nickname').value.trim();
    const startBtn = document.getElementById('startBtn');

    if (nickname && selectedArtist) {
        startBtn.disabled = false;
        startBtn.textContent = `${selectedArtist.name} 도전 시작! (${selectedArtist.count}곡)`;
    } else {
        startBtn.disabled = true;
        startBtn.textContent = '도전 시작!';
    }
}

// 닉네임 입력 시 버튼 상태 업데이트
document.getElementById('nickname').addEventListener('input', updateStartButton);

async function startGame() {
    const nickname = document.getElementById('nickname').value.trim();

    if (!nickname) {
        alert('닉네임을 입력해주세요');
        return;
    }

    if (!selectedArtist) {
        alert('아티스트를 선택해주세요');
        return;
    }

    const startBtn = document.getElementById('startBtn');
    startBtn.disabled = true;
    startBtn.textContent = '게임 시작 중...';

    try {
        const response = await fetch('/game/fan-challenge/start', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                nickname: nickname,
                artist: selectedArtist.name
            })
        });

        const result = await response.json();

        if (result.success) {
            window.location.href = '/game/fan-challenge/play';
        } else {
            alert(result.message || '게임 시작에 실패했습니다');
            startBtn.disabled = false;
            updateStartButton();
        }
    } catch (error) {
        console.error('게임 시작 오류:', error);
        alert('게임 시작 중 오류가 발생했습니다');
        startBtn.disabled = false;
        updateStartButton();
    }
}
