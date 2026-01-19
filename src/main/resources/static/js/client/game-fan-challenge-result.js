/**
 * client/game/fan-challenge/result.html - 아티스트 챌린지 결과
 */

document.addEventListener('DOMContentLoaded', function() {
    const badgesCollection = document.querySelector('.badges-collection');
    if (badgesCollection) {
        enableDragScroll(badgesCollection);
    }
});
