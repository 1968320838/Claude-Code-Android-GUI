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

    /**
     * 列出目录内容
     * @param path 目录路径
     * @return FileItem 列表
     */
    java.util.List<com.claudebox.domain.model.FileItem> listDirectory(String path);

    /**
     * 创建文件
     * @param parentPath 父目录路径
     * @param fileName 文件名
     * @return 是否成功
     */
    boolean createFile(String parentPath, String fileName);

    /**
     * 创建目录
     * @param parentPath 父目录路径
     * @param dirName 目录名
     * @return 是否成功
     */
    boolean createDirectory(String parentPath, String dirName);

    /**
     * 删除文件或目录
     * @param path 文件或目录路径
     * @param isDirectory 是否为目录
     * @return 是否成功
     */
    boolean deleteFile(String path, boolean isDirectory);

    /**
     * 重命名文件或目录
     * @param oldPath 原路径
     * @param newName 新名称
     * @return 新路径，失败返回 null
     */
    String renameFile(String oldPath, String newName);

    /**
     * 读取文件内容
     * @param path 文件路径
     * @param maxSize 最大读取字节数
     * @return 文件内容，失败返回 null
     */
    String readFile(String path, int maxSize);
}
