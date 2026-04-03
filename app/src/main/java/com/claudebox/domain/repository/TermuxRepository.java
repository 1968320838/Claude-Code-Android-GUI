package com.claudebox.domain.repository;

import com.claudebox.data.ssh.SSHConfig;

public interface TermuxRepository {
    /**
     * 连接状态回调接口
     */
    interface ConnectionCallback {
        void onConnecting();
        void onConnected();
        void onDisconnected();
        void onError(String message);
    }

    /**
     * 命令执行回调接口
     */
    interface CommandCallback {
        void onOutput(String output);
        void onComplete();
        void onError(String error);
    }

    /**
     * Shell 输出监听器（可多个观察者）
     */
    interface ShellOutputListener {
        void onOutput(String data);
        void onClosed();
    }

    /**
     * Claude 会话回调
     */
    interface ClaudeSessionCallback {
        void onOpened();
        void onOutput(String output);
        void onError(String error);
        void onClosed();
    }

    /**
     * 设置连接状态回调
     */
    void setConnectionCallback(ConnectionCallback callback);

    /**
     * 连接到 Termux
     */
    void connect(SSHConfig config);

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 执行命令（同步方式，返回结果）
     */
    String executeCommandSync(String command);

    /**
     * 执行命令（异步方式，通过回调返回结果）
     */
    void executeCommand(String command, CommandCallback callback);

    /**
     * 检查是否已连接
     */
    boolean isConnected();

    /**
     * 添加 Shell 输出监听器
     */
    void addShellOutputListener(ShellOutputListener listener);

    /**
     * 移除 Shell 输出监听器
     */
    void removeShellOutputListener(ShellOutputListener listener);

    /**
     * 打开 Claude 会话（如果尚未打开）
     */
    void openClaudeSession(ClaudeSessionCallback callback);

    /**
     * 发送消息到 Claude 会话
     */
    void sendToClaude(String message);

    /**
     * 关闭 Claude 会话
     */
    void closeClaudeSession();
}
