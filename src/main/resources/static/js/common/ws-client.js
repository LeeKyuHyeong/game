/**
 * 멀티플레이어 게임용 WebSocket 클라이언트 (STOMP over SockJS)
 * - 서버 → 클라이언트 push 수신
 * - 연결 실패 시 polling fallback 지원
 */
const GameWebSocket = {
    stompClient: null,
    subscription: null,
    connected: false,
    roomCode: null,
    handlers: {},
    reconnectAttempts: 0,
    maxReconnectAttempts: 5,
    reconnectTimer: null,
    fallbackCallback: null,

    /**
     * WebSocket 연결 및 방 토픽 구독
     * @param {string} roomCode - 방 코드
     * @param {Object} messageHandlers - 타입별 핸들러 {ROOM_UPDATE: fn, CHAT: fn, ...}
     * @param {Function} [fallbackFn] - WS 연결 실패 시 호출할 polling 시작 함수
     */
    connect(roomCode, messageHandlers, fallbackFn) {
        this.roomCode = roomCode;
        this.handlers = messageHandlers || {};
        this.fallbackCallback = fallbackFn || null;
        this.reconnectAttempts = 0;

        this._doConnect();
    },

    _doConnect() {
        try {
            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);

            // STOMP 디버그 로그 비활성화 (프로덕션)
            this.stompClient.debug = null;

            // CSRF 토큰 헤더
            const headers = {};
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            this.stompClient.connect(headers, () => {
                this.connected = true;
                this.reconnectAttempts = 0;
                console.log('[WS] Connected to /topic/room/' + this.roomCode);

                // 방 토픽 구독
                this.subscription = this.stompClient.subscribe(
                    '/topic/room/' + this.roomCode,
                    (message) => {
                        try {
                            const data = JSON.parse(message.body);
                            this._onMessage(data);
                        } catch (e) {
                            console.error('[WS] Message parse error:', e);
                        }
                    }
                );
            }, (error) => {
                console.warn('[WS] Connection error:', error);
                this.connected = false;
                this._handleDisconnect();
            });

            // SockJS 연결 종료 감지
            socket.onclose = () => {
                if (this.connected) {
                    this.connected = false;
                    console.warn('[WS] Connection closed');
                    this._handleDisconnect();
                }
            };
        } catch (e) {
            console.error('[WS] Failed to create connection:', e);
            this._activateFallback();
        }
    },

    _onMessage(data) {
        const type = data.type;
        const payload = data.payload;

        if (this.handlers[type]) {
            this.handlers[type](payload);
        }
    },

    _handleDisconnect() {
        this.reconnectAttempts++;

        if (this.reconnectAttempts > this.maxReconnectAttempts) {
            console.warn('[WS] Max reconnect attempts reached, activating fallback');
            this._activateFallback();
            return;
        }

        // 지수 백오프 재연결 (1s, 2s, 4s, 8s, 16s)
        const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts - 1), 16000);
        console.log(`[WS] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

        this.reconnectTimer = setTimeout(() => {
            this._doConnect();
        }, delay);
    },

    _activateFallback() {
        if (this.fallbackCallback) {
            console.log('[WS] Activating polling fallback');
            this.fallbackCallback();
        }
    },

    disconnect() {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        if (this.subscription) {
            this.subscription.unsubscribe();
            this.subscription = null;
        }
        if (this.stompClient && this.connected) {
            this.stompClient.disconnect();
        }
        this.connected = false;
        this.stompClient = null;
    },

    isConnected() {
        return this.connected;
    }
};
