/**
 * 자동재생 로직 테스트 - 네트워크 속도에 따른 시나리오
 *
 * TDD Red Phase: 현재 버그를 재현하는 실패 테스트 작성
 */

// ========================================
// Mock 설정
// ========================================

// YouTube Player Manager Mock
const createYouTubePlayerMock = () => ({
  loadAndPlay: jest.fn(),
  loadVideo: jest.fn(),
  play: jest.fn(),
  pause: jest.fn(),
  stop: jest.fn(),
  getCurrentTime: jest.fn(() => 0),
  // 상태 변경 콜백을 저장하여 나중에 호출
  _stateChangeCallback: null,
  _simulateStateChange: function(state) {
    if (this._stateChangeCallback) {
      this._stateChangeCallback({ data: state });
    }
  }
});

// Audio Player Mock (HTML5 Audio)
const createAudioPlayerMock = () => ({
  src: '',
  currentTime: 0,
  play: jest.fn(() => Promise.resolve()),
  pause: jest.fn(),
  onloadedmetadata: null,
  _simulateMetadataLoaded: function() {
    if (this.onloadedmetadata) {
      this.onloadedmetadata();
    }
  }
});

// UI Updater Mock
const createUIUpdaterMock = () => ({
  setPlayingState: jest.fn(),
  setPausedState: jest.fn(),
  updateProgress: jest.fn(),
  updateTimeDisplay: jest.fn()
});

// ========================================
// 테스트 대상: AutoPlayController (추출될 로직)
// ========================================

/**
 * 자동재생 로직을 테스트 가능하게 추출한 컨트롤러
 *
 * 현재 game-host-play.js의 loadAudioSource()에서 추출
 */
class AutoPlayController {
  constructor(dependencies) {
    this.youtubePlayer = dependencies.youtubePlayer;
    this.audioPlayer = dependencies.audioPlayer;
    this.uiUpdater = dependencies.uiUpdater;
    this.onAutoPlayStart = dependencies.onAutoPlayStart || (() => {});

    // 상태
    this.currentRound = 1;
    this.currentSong = null;
    this.isPlaying = false;
    this.youtubePlayerReady = false;
    this.videoReady = false;
    this.pendingAutoPlay = false;
  }

  /**
   * YouTube 상태 변경 핸들러 - 수정된 버전
   */
  handleYouTubeStateChange(state) {
    const YOUTUBE_STATE = {
      CUED: 5,      // 영상 로드 완료
      ENDED: 0,     // 재생 종료
      PLAYING: 1    // 재생 중
    };

    if (state === YOUTUBE_STATE.CUED) {
      this.videoReady = true;

      // CUED 상태에서 자동 재생 (느린 네트워크)
      if (this.pendingAutoPlay && this.currentSong) {
        this.pendingAutoPlay = false;
        this._startPlayback();
      }
    } else if (state === YOUTUBE_STATE.ENDED) {
      this._pausePlayback();
    } else if (state === YOUTUBE_STATE.PLAYING) {
      this.videoReady = true;
      // ✅ FIX: PLAYING 상태에서 UI 업데이트 (CUED 건너뛰는 경우)
      if (this.pendingAutoPlay) {
        this.pendingAutoPlay = false;
        this.isPlaying = true;
        this.uiUpdater.setPlayingState();
        this.onAutoPlayStart();
      }
    }
  }

  /**
   * 오디오 소스 로드 - 버그 수정 전 버전 (테스트용 보존)
   */
  loadAudioSourceBuggy() {
    if (!this.currentSong) return;

    const shouldAutoPlay = this.currentRound > 1;
    this.videoReady = false;
    this.pendingAutoPlay = false;  // 🔴 BUG: 여기서 항상 false로 설정

    if (this.currentSong.youtubeVideoId && this.youtubePlayerReady) {
      if (shouldAutoPlay) {
        // 🔴 BUG: loadAndPlay를 직접 호출하고 UI만 업데이트
        this.youtubePlayer.loadAndPlay(
          this.currentSong.youtubeVideoId,
          this.currentSong.startTime || 0
        );
        this.isPlaying = true;
        this.uiUpdater.setPlayingState();
        this.onAutoPlayStart();
      } else {
        this.youtubePlayer.loadVideo(
          this.currentSong.youtubeVideoId,
          this.currentSong.startTime || 0
        );
      }
    } else if (this.currentSong.filePath) {
      this.audioPlayer.src = `/uploads/songs/${this.currentSong.filePath}`;
      this.audioPlayer.currentTime = 0;

      const controller = this;
      this.audioPlayer.onloadedmetadata = function() {
        controller.uiUpdater.updateTimeDisplay();
        if (shouldAutoPlay) {
          controller._startPlayback();
        }
      };
    }
  }

  /**
   * 오디오 소스 로드 - 수정된 버전 (실제 game-host-play.js 반영)
   */
  loadAudioSource() {
    if (!this.currentSong) return;

    const shouldAutoPlay = this.currentRound > 1;
    this.videoReady = false;
    this.pendingAutoPlay = false;

    if (this.currentSong.youtubeVideoId && this.youtubePlayerReady) {
      if (shouldAutoPlay) {
        // ✅ FIX: pendingAutoPlay 설정하여 CUED/PLAYING 상태에서 재생 보장
        this.pendingAutoPlay = true;
        this.youtubePlayer.loadAndPlay(
          this.currentSong.youtubeVideoId,
          this.currentSong.startTime || 0
        );
        // UI 업데이트는 onStateChange에서 처리
      } else {
        this.youtubePlayer.loadVideo(
          this.currentSong.youtubeVideoId,
          this.currentSong.startTime || 0
        );
      }
    } else if (this.currentSong.filePath) {
      this.audioPlayer.src = `/uploads/songs/${this.currentSong.filePath}`;
      this.audioPlayer.currentTime = 0;

      const controller = this;
      this.audioPlayer.onloadedmetadata = function() {
        controller.uiUpdater.updateTimeDisplay();
        if (shouldAutoPlay) {
          controller._startPlayback();
        }
      };
    }
  }

  _startPlayback() {
    if (this.currentSong.youtubeVideoId && this.youtubePlayerReady) {
      this.youtubePlayer.play();
    } else {
      this.audioPlayer.play();
    }
    this.isPlaying = true;
    this.uiUpdater.setPlayingState();
    this.onAutoPlayStart();
  }

  _pausePlayback() {
    if (this.currentSong && this.currentSong.youtubeVideoId && this.youtubePlayerReady) {
      this.youtubePlayer.pause();
    } else {
      this.audioPlayer.pause();
    }
    this.isPlaying = false;
    this.uiUpdater.setPausedState();
  }
}

// ========================================
// 테스트 시나리오
// ========================================

describe('AutoPlayController - 네트워크 속도별 시나리오', () => {
  let controller;
  let youtubePlayerMock;
  let audioPlayerMock;
  let uiUpdaterMock;
  let autoPlayStarted;

  beforeEach(() => {
    youtubePlayerMock = createYouTubePlayerMock();
    audioPlayerMock = createAudioPlayerMock();
    uiUpdaterMock = createUIUpdaterMock();
    autoPlayStarted = false;

    controller = new AutoPlayController({
      youtubePlayer: youtubePlayerMock,
      audioPlayer: audioPlayerMock,
      uiUpdater: uiUpdaterMock,
      onAutoPlayStart: () => { autoPlayStarted = true; }
    });

    // YouTube 상태 변경 콜백 연결
    youtubePlayerMock._stateChangeCallback = (e) => {
      controller.handleYouTubeStateChange(e.data);
    };

    // 기본 상태 설정
    controller.youtubePlayerReady = true;
  });

  // ========================================
  // 시나리오 1: 1라운드 - 자동재생 없음
  // ========================================
  describe('시나리오 1: 1라운드 (수동 재생)', () => {
    test('1라운드에서는 자동재생하지 않아야 함', () => {
      // Given: 1라운드, YouTube 영상
      controller.currentRound = 1;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      // When: 오디오 소스 로드
      controller.loadAudioSource();

      // Then: loadVideo만 호출 (loadAndPlay 아님)
      expect(youtubePlayerMock.loadVideo).toHaveBeenCalled();
      expect(youtubePlayerMock.loadAndPlay).not.toHaveBeenCalled();
      expect(controller.isPlaying).toBe(false);
      expect(autoPlayStarted).toBe(false);
    });
  });

  // ========================================
  // 시나리오 2: 빠른 네트워크 (100ms 이내)
  // ========================================
  describe('시나리오 2: 빠른 네트워크 (YouTube 즉시 응답)', () => {
    test('2라운드 이후 자동재생 - 빠른 네트워크에서 정상 작동', async () => {
      // Given: 2라운드, YouTube 영상
      controller.currentRound = 2;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      // When: 오디오 소스 로드
      controller.loadAudioSource();

      // Then: loadAndPlay 호출됨
      expect(youtubePlayerMock.loadAndPlay).toHaveBeenCalledWith('test123', 0);

      // 빠른 네트워크: 즉시 CUED 상태 수신
      youtubePlayerMock._simulateStateChange(5); // CUED

      // 현재 버그 있는 코드에서도 UI는 업데이트됨 (실제 재생과 무관하게)
      expect(controller.isPlaying).toBe(true);
    });
  });

  // ========================================
  // 시나리오 3: 느린 네트워크 (500ms+ 지연)
  // ========================================
  describe('시나리오 3: 느린 네트워크 (CUED 지연)', () => {
    test('🔴 버그 수정 전: pendingAutoPlay가 설정되지 않음', async () => {
      // Given: 2라운드, YouTube 영상
      controller.currentRound = 2;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      // When: 버그 있는 코드로 로드
      controller.loadAudioSourceBuggy();

      // Then: 버그 확인 - pendingAutoPlay가 false
      expect(controller.pendingAutoPlay).toBe(false);
      // 버그 코드에서는 UI가 즉시 업데이트됨
      expect(controller.isPlaying).toBe(true);
    });

    test('✅ 버그 수정 후: 느린 네트워크에서도 CUED 대기 후 재생', async () => {
      // Given: 2라운드, YouTube 영상
      controller.currentRound = 2;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      // When: 수정된 오디오 소스 로드
      controller.loadAudioSource();

      // Then: pendingAutoPlay가 true로 설정됨
      expect(controller.pendingAutoPlay).toBe(true);
      expect(controller.isPlaying).toBe(false); // 아직 재생 시작 안 함

      // 느린 네트워크: 나중에 CUED 상태 수신
      youtubePlayerMock._simulateStateChange(5); // CUED

      // CUED 상태에서 자동재생 시작
      expect(controller.pendingAutoPlay).toBe(false); // 사용 후 리셋
      expect(controller.videoReady).toBe(true);
      expect(youtubePlayerMock.play).toHaveBeenCalled();
      expect(controller.isPlaying).toBe(true);
      expect(autoPlayStarted).toBe(true);
    });

    test('✅ 버그 수정 후: 빠른 네트워크에서 PLAYING 직행 시에도 작동', async () => {
      // Given: 2라운드, YouTube 영상
      controller.currentRound = 2;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      // When: 수정된 오디오 소스 로드
      controller.loadAudioSource();

      // Then: pendingAutoPlay가 true로 설정됨
      expect(controller.pendingAutoPlay).toBe(true);

      // 빠른 네트워크: CUED 건너뛰고 바로 PLAYING 상태
      youtubePlayerMock._simulateStateChange(1); // PLAYING

      // PLAYING 상태에서 UI 업데이트
      expect(controller.pendingAutoPlay).toBe(false);
      expect(controller.isPlaying).toBe(true);
      expect(autoPlayStarted).toBe(true);
    });
  });

  // ========================================
  // 시나리오 4: MP3 Fallback
  // ========================================
  describe('시나리오 4: MP3 Fallback (YouTube 없음)', () => {
    test('MP3 파일에서 자동재생 - onloadedmetadata 이벤트 기반', () => {
      // Given: 2라운드, MP3 파일만 있음
      controller.currentRound = 2;
      controller.currentSong = {
        youtubeVideoId: null,
        filePath: 'song.mp3'
      };

      // When: 오디오 소스 로드
      controller.loadAudioSource();

      // Then: 오디오 소스 설정됨
      expect(audioPlayerMock.src).toBe('/uploads/songs/song.mp3');

      // metadata 로드 완료 시뮬레이션
      audioPlayerMock._simulateMetadataLoaded();

      // 자동재생 시작
      expect(audioPlayerMock.play).toHaveBeenCalled();
      expect(controller.isPlaying).toBe(true);
    });
  });

  // ========================================
  // 시나리오 5: YouTube 에러 후 MP3 Fallback
  // ========================================
  describe('시나리오 5: YouTube 에러 → MP3 Fallback', () => {
    test('YouTube 실패 시 MP3로 fallback하고 자동재생', () => {
      // Given: 2라운드, YouTube + MP3 둘 다 있음
      controller.currentRound = 2;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        filePath: 'song.mp3',
        startTime: 0
      };

      // YouTube 에러 핸들러 시뮬레이션 (실제 코드의 onError)
      const handleYouTubeError = () => {
        controller.videoReady = false;
        controller.pendingAutoPlay = false;

        // MP3 fallback
        controller.currentSong.youtubeVideoId = null;
        controller.loadAudioSource();
      };

      // When: 먼저 YouTube 로드 시도
      controller.loadAudioSource();

      // YouTube 에러 발생
      handleYouTubeError();

      // Then: MP3로 전환되어 로드됨
      expect(audioPlayerMock.src).toBe('/uploads/songs/song.mp3');

      // metadata 로드 완료
      audioPlayerMock._simulateMetadataLoaded();

      // 자동재생 실행
      expect(audioPlayerMock.play).toHaveBeenCalled();
    });
  });

  // ========================================
  // 시나리오 6: 연속 라운드 전환
  // ========================================
  describe('시나리오 6: 연속 라운드 전환 (3→4→5라운드)', () => {
    test('연속된 라운드에서 각각 자동재생 작동', () => {
      const rounds = [3, 4, 5];

      rounds.forEach((round, index) => {
        // 상태 리셋 (resetPlayerUI 시뮬레이션)
        controller.videoReady = false;
        controller.pendingAutoPlay = false;
        controller.isPlaying = false;
        autoPlayStarted = false;
        jest.clearAllMocks();

        // Given: N라운드
        controller.currentRound = round;
        controller.currentSong = {
          youtubeVideoId: `video${round}`,
          startTime: 0
        };

        // When: 수정된 코드로 로드
        controller.loadAudioSource();

        // Then: 각 라운드마다 pendingAutoPlay 설정
        expect(controller.pendingAutoPlay).toBe(true);

        // CUED 상태 수신
        youtubePlayerMock._simulateStateChange(5);

        // 자동재생 실행
        expect(controller.isPlaying).toBe(true);
        expect(autoPlayStarted).toBe(true);
      });
    });
  });

  // ========================================
  // 시나리오 7: 상태 플래그 검증
  // ========================================
  describe('시나리오 7: 상태 플래그 일관성', () => {
    test('loadAudioSource 호출 시 상태 플래그 초기화 후 재설정', () => {
      // Given: 이전 라운드 상태가 남아있음
      controller.videoReady = true;
      controller.pendingAutoPlay = true;
      controller.currentRound = 3;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      // When: 새 라운드 로드
      controller.loadAudioSource();

      // Then: videoReady 초기화, pendingAutoPlay는 true로 재설정
      expect(controller.videoReady).toBe(false);
      expect(controller.pendingAutoPlay).toBe(true); // ✅ 수정됨
    });

    test('1라운드에서는 pendingAutoPlay가 false 유지', () => {
      controller.currentRound = 1;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      controller.loadAudioSource();

      // 1라운드는 수동 재생이므로 pendingAutoPlay = false
      expect(controller.pendingAutoPlay).toBe(false);
    });

    test('2라운드 이후에서 pendingAutoPlay 올바르게 설정', () => {
      controller.currentRound = 3;
      controller.currentSong = {
        youtubeVideoId: 'test123',
        startTime: 0
      };

      controller.loadAudioSource();

      // ✅ 수정 후: shouldAutoPlay일 때 pendingAutoPlay = true
      expect(controller.pendingAutoPlay).toBe(true);
    });
  });
});

// ========================================
// 타이밍 시뮬레이션 테스트 (Fake Timers)
// ========================================
describe('AutoPlayController - 타이밍 시뮬레이션', () => {
  let controller;
  let youtubePlayerMock;
  let uiUpdaterMock;

  beforeEach(() => {
    jest.useFakeTimers();

    youtubePlayerMock = createYouTubePlayerMock();
    uiUpdaterMock = createUIUpdaterMock();

    controller = new AutoPlayController({
      youtubePlayer: youtubePlayerMock,
      audioPlayer: createAudioPlayerMock(),
      uiUpdater: uiUpdaterMock
    });

    youtubePlayerMock._stateChangeCallback = (e) => {
      controller.handleYouTubeStateChange(e.data);
    };

    controller.youtubePlayerReady = true;
    controller.currentRound = 2;
    controller.currentSong = {
      youtubeVideoId: 'test123',
      startTime: 0
    };
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('✅ 네트워크 지연 50ms - 빠른 네트워크 시뮬레이션', () => {
    controller.loadAudioSource();

    expect(controller.pendingAutoPlay).toBe(true);
    expect(controller.isPlaying).toBe(false);

    // 50ms 후 PLAYING 도착 (빠른 네트워크는 CUED 건너뜀)
    setTimeout(() => {
      youtubePlayerMock._simulateStateChange(1); // PLAYING
    }, 50);

    jest.advanceTimersByTime(50);

    expect(controller.isPlaying).toBe(true);
    expect(controller.pendingAutoPlay).toBe(false);
  });

  test('✅ 네트워크 지연 500ms - 느린 네트워크 시뮬레이션', () => {
    controller.loadAudioSource();

    expect(controller.pendingAutoPlay).toBe(true);
    expect(controller.isPlaying).toBe(false);

    // 500ms 후 CUED 도착
    setTimeout(() => {
      youtubePlayerMock._simulateStateChange(5);
    }, 500);

    // 100ms 시점 - 아직 재생 안 됨
    jest.advanceTimersByTime(100);
    expect(controller.isPlaying).toBe(false);

    // 300ms 시점 - 아직 재생 안 됨
    jest.advanceTimersByTime(200);
    expect(controller.isPlaying).toBe(false);

    // 500ms 시점 - CUED 도착, 재생 시작
    jest.advanceTimersByTime(200);
    expect(controller.isPlaying).toBe(true);
  });

  test('🔴 버그 수정 전 코드: 네트워크 지연 시 UI와 실제 상태 불일치', () => {
    // 버그 있는 코드 사용
    controller.loadAudioSourceBuggy();

    // 버그 코드: pendingAutoPlay는 false, isPlaying은 true (UI만 업데이트)
    expect(controller.pendingAutoPlay).toBe(false);
    expect(controller.isPlaying).toBe(true); // UI는 "재생 중" (실제 재생 확인 안 됨)

    // 500ms 후 CUED 도착
    setTimeout(() => {
      youtubePlayerMock._simulateStateChange(5);
    }, 500);

    jest.advanceTimersByTime(500);

    // videoReady는 true가 되지만...
    expect(controller.videoReady).toBe(true);
    // pendingAutoPlay가 false이므로 추가 재생 명령이 없음
    // 느린 네트워크에서 loadAndPlay가 실패하면 재생 안 됨
  });
});
