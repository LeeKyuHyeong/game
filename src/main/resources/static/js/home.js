function startSinglePlay() {
    alert('싱글 플레이 기능은 곧 구현됩니다!');
}

function startMultiPlay() {
    alert('멀티 플레이 기능은 준비 중입니다.');
}

document.addEventListener('DOMContentLoaded', function() {
    const singleCard = document.getElementById('singlePlay');
    const multiCard = document.getElementById('multiPlay');
    
    if (singleCard) {
        singleCard.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-8px)';
        });
        singleCard.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
        });
    }
});
