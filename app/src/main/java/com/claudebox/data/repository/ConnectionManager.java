package com.claudebox.data.repository;

import com.claudebox.data.ssh.ConnectionState;
import com.claudebox.data.ssh.SSHConfig;
import com.claudebox.domain.repository.TermuxRepository;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionManager {
    private static ConnectionManager instance;

    private final TermuxRepository termuxRepository;
    private final AtomicReference<ConnectionState> connectionState;
    private final AtomicInteger reconnectAttempt;
    private final int maxReconnectAttempts;
    private final int reconnectDelayMs;

    private SSHConfig lastConfig;
    private boolean isReconnecting;
    private Thread reconnectThread;

    private ConnectionManager(TermuxRepository termuxRepository) {
        this.termuxRepository = termuxRepository;
        this.connectionState = new AtomicReference<>(new ConnectionState.Disconnected());
        this.reconnectAttempt = new AtomicInteger(0);
        this.maxReconnectAttempts = 3;
        this.reconnectDelayMs = 2000;
        this.isReconnecting = false;

        // 设置连接回调
        termuxRepository.setConnectionCallback(new TermuxRepository.ConnectionCallback() {
            @Override
            public void onConnecting() {
                connectionState.set(new ConnectionState.Connecting());
            }

            @Override
            public void onConnected() {
                reconnectAttempt.set(0);
                isReconnecting = false;
                connectionState.set(new ConnectionState.Connected());
            }

            @Override
            public void onDisconnected() {
                connectionState.set(new ConnectionState.Disconnected());
            }

            @Override
            public void onError(String message) {
                if (isReconnecting) {
                    handleReconnect();
                } else {
                    connectionState.set(new ConnectionState.Error(message));
                }
            }
        });
    }

    public static synchronized ConnectionManager getInstance(TermuxRepository termuxRepository) {
        if (instance == null) {
            instance = new ConnectionManager(termuxRepository);
        }
        return instance;
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ConnectionManager not initialized");
        }
        return instance;
    }

    /**
     * 获取当前连接状态
     */
    public ConnectionState getConnectionState() {
        return connectionState.get();
    }

    /**
     * 观察连接状态变化（返回当前状态引用）
     */
    public AtomicReference<ConnectionState> getConnectionStateRef() {
        return connectionState;
    }

    /**
     * 连接
     */
    public void connect(SSHConfig config) {
        lastConfig = config;
        reconnectAttempt.set(0);
        isReconnecting = false;

        // 取消之前的重连线程
        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }

        connectionState.set(new ConnectionState.Connecting());
        termuxRepository.connect(config);
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        isReconnecting = false;

        // 取消重连
        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }

        reconnectAttempt.set(0);
        termuxRepository.disconnect();
        connectionState.set(new ConnectionState.Disconnected());
    }

    /**
     * 手动重连
     */
    public void reconnect() {
        if (lastConfig != null) {
            disconnect();
            reconnectAttempt.set(0);
            connect(lastConfig);
        }
    }

    /**
     * 处理自动重连逻辑
     */
    private void handleReconnect() {
        if (isReconnecting || lastConfig == null) {
            return;
        }

        int attempt = reconnectAttempt.incrementAndGet();
        if (attempt <= maxReconnectAttempts) {
            isReconnecting = true;
            connectionState.set(new ConnectionState.Reconnecting(attempt));

            reconnectThread = new Thread(() -> {
                try {
                    // 指数退避: 2s, 4s, 8s
                    long delay = reconnectDelayMs * attempt;
                    Thread.sleep(delay);

                    if (!isReconnecting || Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    // 重新连接
                    connectionState.set(new ConnectionState.Connecting());
                    termuxRepository.connect(lastConfig);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isReconnecting = false;
                    connectionState.set(new ConnectionState.Error("重连被中断"));
                }
            });
            reconnectThread.start();
        } else {
            // 重连失败
            isReconnecting = false;
            connectionState.set(new ConnectionState.Error("连接失败，已停止重试（" + maxReconnectAttempts + "次）"));
        }
    }

    /**
     * 检查是否正在重连
     */
    public boolean isReconnecting() {
        return isReconnecting;
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return termuxRepository.isConnected();
    }

    /**
     * 获取 TermuxRepository（用于直接操作）
     */
    public TermuxRepository getTermuxRepository() {
        return termuxRepository;
    }
}
