/**
 * YouTube IFrame API Player Manager
 * 공통 모듈: 모든 게임 모드에서 YouTube 영상 재생에 사용
 */
const YouTubePlayerManager = {
    player: null,
    isReady: false,
    containerId: null,
    onReadyCallback: null,
    onStateChangeCallback: null,
    onErrorCallback: null,
    lastError: null,  // 마지막 에러 정보
    mediaSessionInterval: null,  // 메타데이터 덮어씌우기 인터벌

    // YouTube 에러 코드 설명
    ERROR_CODES: {
        2: '잘못된 파라미터',
        5: 'HTML5 플레이어 오류',
        100: '영상을 찾을 수 없음',
        101: '임베드가 차단됨',
        150: '임베드가 차단됨'
    },

    /**
     * YouTube IFrame API 초기화
     * @param {string} containerId - YouTube 플레이어를 삽입할 DOM 요소 ID
     * @param {object} options - 추가 옵션 (onReady, onStateChange, onError 콜백)
     * @returns {Promise} - 플레이어 준비 완료 시 resolve
     */
    init(containerId, options = {}) {
        this.containerId = containerId;
        this.onReadyCallback = options.onReady || null;
        this.onStateChangeCallback = options.onStateChange || null;
        this.onErrorCallback = options.onError || null;

        return new Promise((resolve) => {
            // 이미 YT API가 로드된 경우
            if (window.YT && window.YT.Player) {
                this.createPlayer(resolve);
                return;
            }

            // YouTube IFrame API 스크립트 로드
            if (!document.querySelector('script[src*="youtube.com/iframe_api"]')) {
                const tag = document.createElement('script');
                tag.src = 'https://www.youtube.com/iframe_api';
                document.body.appendChild(tag);
            }

            // API 로드 완료 시 플레이어 생성
            window.onYouTubeIframeAPIReady = () => {
                this.createPlayer(resolve);
            };
        });
    },

    /**
     * YouTube 플레이어 생성
     * @param {function} resolve - Promise resolve 함수
     */
    createPlayer(resolve) {
        this.player = new YT.Player(this.containerId, {
            height: '0',
            width: '0',
            playerVars: {
                autoplay: 0,
                controls: 0,
                disablekb: 1,
                enablejsapi: 1,
                modestbranding: 1,
                rel: 0,
                showinfo: 0,
                playsinline: 1,  // iOS 인라인 재생
                origin: window.location.origin
            },
            events: {
                onReady: (e) => {
                    this.isReady = true;
                    console.log('YouTube Player ready');
                    if (this.onReadyCallback) this.onReadyCallback(e);
                    resolve();
                },
                onStateChange: (e) => {
                    if (this.onStateChangeCallback) this.onStateChangeCallback(e);
                },
                onError: (e) => {
                    const errorCode = e.data;
                    const errorMessage = this.ERROR_CODES[errorCode] || '알 수 없는 오류';
                    console.error('YouTube Player Error:', errorCode, errorMessage);

                    // 에러 정보 저장
                    this.lastError = {
                        code: errorCode,
                        message: errorMessage,
                        timestamp: Date.now(),
                        isPlaybackError: [100, 101, 150].includes(errorCode)  // 재생 불가 에러
                    };

                    if (this.onErrorCallback) this.onErrorCallback(e, this.lastError);
                }
            }
        });
    },

    /**
     * 영상 로드 (재생 대기 상태)
     * @param {string} videoId - YouTube Video ID (11자리)
     * @param {number} startTime - 시작 시간 (초)
     */
    loadVideo(videoId, startTime = 0) {
        if (!this.isReady || !this.player) {
            console.warn('YouTube Player not ready');
            return;
        }
        this.startMediaSessionProtection();  // 로드 시점부터 메타데이터 숨김
        this.player.cueVideoById({
            videoId: videoId,
            startSeconds: startTime
        });
    },

    /**
     * 재생
     */
    play() {
        if (this.isReady && this.player) {
            this.startMediaSessionProtection();  // 메타데이터 숨김 시작
            this.player.playVideo();
        }
    },

    /**
     * 일시정지
     */
    pause() {
        if (this.isReady && this.player) {
            this.player.pauseVideo();
        }
    },

    /**
     * 정지 (영상 초기화)
     */
    stop() {
        if (this.isReady && this.player) {
            this.stopMediaSessionProtection();  // 메타데이터 숨김 중지
            this.player.stopVideo();
        }
    },

    /**
     * 특정 시간으로 이동
     * @param {number} seconds - 이동할 시간 (초)
     * @param {boolean} allowSeekAhead - 버퍼링 허용 여부
     */
    seekTo(seconds, allowSeekAhead = true) {
        if (this.isReady && this.player) {
            this.player.seekTo(seconds, allowSeekAhead);
        }
    },

    /**
     * 현재 재생 시간 조회
     * @returns {number} 현재 재생 시간 (초)
     */
    getCurrentTime() {
        if (this.isReady && this.player) {
            return this.player.getCurrentTime();
        }
        return 0;
    },

    /**
     * 영상 전체 길이 조회
     * @returns {number} 영상 길이 (초)
     */
    getDuration() {
        if (this.isReady && this.player) {
            return this.player.getDuration();
        }
        return 0;
    },

    /**
     * 재생 상태 확인
     * @returns {boolean} 재생 중이면 true
     */
    isPlaying() {
        if (this.isReady && this.player) {
            return this.player.getPlayerState() === YT.PlayerState.PLAYING;
        }
        return false;
    },

    /**
     * 현재 플레이어 상태 조회
     * @returns {number} YT.PlayerState 값 (-1:시작안됨, 0:종료, 1:재생, 2:일시정지, 3:버퍼링, 5:큐)
     */
    getPlayerState() {
        if (this.isReady && this.player) {
            return this.player.getPlayerState();
        }
        return -1;
    },

    /**
     * 볼륨 설정
     * @param {number} volume - 볼륨 (0-100)
     */
    setVolume(volume) {
        if (this.isReady && this.player) {
            this.player.setVolume(volume);
        }
    },

    /**
     * 음소거
     */
    mute() {
        if (this.isReady && this.player) {
            this.player.mute();
        }
    },

    /**
     * 음소거 해제
     */
    unMute() {
        if (this.isReady && this.player) {
            this.player.unMute();
        }
    },

    /**
     * 플레이어 준비 상태 확인
     * @returns {boolean}
     */
    ready() {
        return this.isReady;
    },

    /**
     * 마지막 에러 정보 조회
     * @returns {object|null} 에러 정보 또는 null
     */
    getLastError() {
        return this.lastError;
    },

    /**
     * 에러 상태 초기화
     */
    clearError() {
        this.lastError = null;
    },

    /**
     * 재생 불가 에러인지 확인
     * @returns {boolean}
     */
    hasPlaybackError() {
        return this.lastError && this.lastError.isPlaybackError;
    },

    /**
     * 에러 메시지 조회
     * @param {number} errorCode - YouTube 에러 코드
     * @returns {string} 에러 메시지
     */
    getErrorMessage(errorCode) {
        return this.ERROR_CODES[errorCode] || '알 수 없는 오류';
    },

    /**
     * Media Session 메타데이터를 가짜 정보로 덮어씌움
     * 볼륨 조절 시 제목/아티스트 노출 방지
     */
    hideMediaMetadata() {
        if ('mediaSession' in navigator) {
            try {
                navigator.mediaSession.metadata = new MediaMetadata({
                    title: '🎵 ???',
                    artist: '누구의 노래일까요?',
                    album: 'Song Quiz Game',
                    artwork: []
                });
                // 미디어 컨트롤 버튼 비활성화
                navigator.mediaSession.setActionHandler('play', null);
                navigator.mediaSession.setActionHandler('pause', null);
                navigator.mediaSession.setActionHandler('seekbackward', null);
                navigator.mediaSession.setActionHandler('seekforward', null);
                navigator.mediaSession.setActionHandler('previoustrack', null);
                navigator.mediaSession.setActionHandler('nexttrack', null);
            } catch (e) {
                // Media Session API 미지원 브라우저
            }
        }
    },

    /**
     * Media Session 메타데이터 주기적 덮어씌우기 시작
     * YouTube가 메타데이터를 다시 설정하는 것을 방지
     */
    startMediaSessionProtection() {
        this.hideMediaMetadata();
        // 기존 인터벌 정리
        if (this.mediaSessionInterval) {
            clearInterval(this.mediaSessionInterval);
        }
        // 500ms마다 덮어씌움 (YouTube가 다시 설정해도 바로 가림)
        this.mediaSessionInterval = setInterval(() => {
            this.hideMediaMetadata();
        }, 500);
    },

    /**
     * Media Session 보호 중지
     */
    stopMediaSessionProtection() {
        if (this.mediaSessionInterval) {
            clearInterval(this.mediaSessionInterval);
            this.mediaSessionInterval = null;
        }
    }
};
