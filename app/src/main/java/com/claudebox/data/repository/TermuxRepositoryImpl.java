package com.claudebox.data.repository;

import com.claudebox.data.ssh.SSHClient;
import com.claudebox.data.ssh.SSHConfig;
import com.claudebox.domain.model.FileItem;
import com.claudebox.domain.repository.TermuxRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TermuxRepositoryImpl implements TermuxRepository {
    private final SSHClient sshClient;
    private final ExecutorService executor;
    private final AtomicBoolean isReading = new AtomicBoolean(false);

    private ConnectionCallback connectionCallback;
    private SSHClient.ShellChannel currentShell;
    private final CopyOnWriteArrayList<ShellOutputListener> outputListeners = new CopyOnWriteArrayList<>();

    public TermuxRepositoryImpl() {
        this.sshClient = new SSHClient();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }

    @Override
    public void connect(SSHConfig config) {
        executor.execute(() -> {
            if (connectionCallback != null) {
                connectionCallback.onConnecting();
            }

            boolean success = sshClient.connect(config);

            if (success) {
                if (connectionCallback != null) {
                    connectionCallback.onConnected();
                }
            } else {
                if (connectionCallback != null) {
                    connectionCallback.onError("连接失败，请检查配置");
                }
            }
        });
    }

    @Override
    public void disconnect() {
        isReading.set(false);
        closeClaudeSession();
        sshClient.disconnect();

        if (connectionCallback != null) {
            connectionCallback.onDisconnected();
        }
    }

    @Override
    public String executeCommandSync(String command) {
        if (!sshClient.isConnected()) {
            return "Error: Not connected";
        }

        return sshClient.executeCommand(command);
    }

    @Override
    public void executeCommand(String command, CommandCallback callback) {
        executor.execute(() -> {
            if (!sshClient.isConnected()) {
                if (callback != null) {
                    callback.onError("Not connected");
                }
                return;
            }

            SSHClient.ShellChannel shell = sshClient.openShellChannel();
            if (shell == null) {
                if (callback != null) {
                    callback.onError("Failed to open shell");
                }
                return;
            }

            try {
                OutputStream out = shell.getOutputStream();
                InputStream in = shell.getInputStream();

                String fullCommand = command + "\n";
                out.write(fullCommand.getBytes(StandardCharsets.UTF_8));
                out.flush();

                byte[] buffer = new byte[1024];
                int len;
                StringBuilder output = new StringBuilder();

                Thread.sleep(500);

                while ((len = in.read(buffer)) != -1) {
                    String chunk = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    output.append(chunk);

                    if (chunk.contains("$") || chunk.contains("#") || chunk.contains(">")) {
                        Thread.sleep(200);
                        break;
                    }
                }

                if (callback != null) {
                    callback.onOutput(output.toString());
                    callback.onComplete();
                }

                shell.disconnect();

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
                if (shell != null) {
                    shell.disconnect();
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        return sshClient.isConnected();
    }

    @Override
    public void addShellOutputListener(ShellOutputListener listener) {
        if (listener != null && !outputListeners.contains(listener)) {
            outputListeners.add(listener);
        }
    }

    @Override
    public void removeShellOutputListener(ShellOutputListener listener) {
        if (listener != null) {
            outputListeners.remove(listener);
        }
    }

    @Override
    public void openClaudeSession(ClaudeSessionCallback callback) {
        if (!sshClient.isConnected()) {
            if (callback != null) {
                callback.onError("Not connected");
            }
            return;
        }

        executor.execute(() -> {
            // 如果已经打开 shell，不要重新创建，只注册回调
            if (currentShell != null && currentShell.isConnected()) {
                if (callback != null) {
                    callback.onOpened();
                }
                return;
            }

            try {
                currentShell = sshClient.openShellChannel();
                if (currentShell == null) {
                    if (callback != null) {
                        callback.onError("Failed to open shell");
                    }
                    return;
                }

                isReading.set(true);

                // 启动读取线程
                executor.execute(() -> {
                    readOutputLoop();
                });

                if (callback != null) {
                    callback.onOpened();
                }

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    @Override
    public void sendToClaude(String message) {
        if (currentShell == null || !currentShell.isConnected()) {
            return;
        }

        executor.execute(() -> {
            try {
                OutputStream out = currentShell.getOutputStream();
                String fullCommand = message + "\n";
                out.write(fullCommand.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void closeClaudeSession() {
        isReading.set(false);

        // 通知所有监听器 shell 已关闭
        for (ShellOutputListener listener : outputListeners) {
            listener.onClosed();
        }

        if (currentShell != null) {
            currentShell.disconnect();
            currentShell = null;
        }
    }

    private void readOutputLoop() {
        if (currentShell == null) return;

        try {
            InputStream in = currentShell.getInputStream();
            byte[] buffer = new byte[1024];

            while (isReading.get() && currentShell.isConnected()) {
                while (in.available() > 0) {
                    int len = in.read(buffer);
                    if (len > 0) {
                        String chunk = new String(buffer, 0, len, StandardCharsets.UTF_8);
                        // 通知所有监听器
                        for (ShellOutputListener listener : outputListeners) {
                            listener.onOutput(chunk);
                        }
                    }
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } catch (IOException e) {
            if (isReading.get()) {
                // 通知所有监听器发生错误
                for (ShellOutputListener listener : outputListeners) {
                    listener.onClosed();
                }
            }
        } finally {
            // 通知所有监听器 shell 已关闭
            for (ShellOutputListener listener : outputListeners) {
                listener.onClosed();
            }
        }
    }

    @Override
    public List<FileItem> listDirectory(String path) {
        List<FileItem> result = new ArrayList<>();

        if (!sshClient.isConnected()) {
            return result;
        }

        try {
            // 使用 ls -la 命令获取目录列表
            String lsCommand = "ls -la \"" + path + "\" 2>/dev/null";
            String output = sshClient.executeCommand(lsCommand);

            if (output == null || output.isEmpty()) {
                return result;
            }

            String[] lines = output.split("\n");
            boolean firstLine = true;

            for (String line : lines) {
                // 跳过第一行（total）和空行
                if (firstLine) {
                    firstLine = false;
                    if (line.startsWith("total")) {
                        continue;
                    }
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // 解析 ls -la 格式
                // drwxr-xr-x  2 user group 4096 Apr  3 10:00 foldername
                // -rw-r--r--  1 user group  123 Apr  3 10:00 filename.txt
                String[] parts = line.split("\\s+");
                if (parts.length < 8) {
                    continue;
                }

                boolean isDirectory = parts[0].startsWith("d");
                String name = parts[parts.length - 1];

                // 跳过 . 和 ..
                if (name.equals(".") || name.equals("..")) {
                    continue;
                }

                // 跳过以 . 开头的隐藏文件（可选）
                // if (name.startsWith(".")) continue;

                long size = 0;
                if (!isDirectory && parts.length >= 5) {
                    try {
                        size = Long.parseLong(parts[4]);
                    } catch (NumberFormatException ignored) {
                    }
                }

                // 构建完整路径
                String fullPath = path.endsWith("/") ? path + name : path + "/" + name;

                // 获取修改时间（简化处理，使用当前时间）
                long modifiedAt = System.currentTimeMillis();

                result.add(new FileItem(name, fullPath, isDirectory, size, modifiedAt));
            }

            // 按目录优先、名称排序
            result.sort((a, b) -> {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public boolean createFile(String parentPath, String fileName) {
        if (!sshClient.isConnected()) {
            return false;
        }

        try {
            String fullPath = parentPath.endsWith("/") ? parentPath + fileName : parentPath + "/" + fileName;
            String command = "touch \"" + fullPath + "\"";
            String result = sshClient.executeCommand(command);
            return result == null || !result.contains("cannot touch") && !result.contains("No such file");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean createDirectory(String parentPath, String dirName) {
        if (!sshClient.isConnected()) {
            return false;
        }

        try {
            String fullPath = parentPath.endsWith("/") ? parentPath + dirName : parentPath + "/" + dirName;
            String command = "mkdir \"" + fullPath + "\"";
            String result = sshClient.executeCommand(command);
            return result == null || !result.contains("cannot create") && !result.contains("No such file");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteFile(String path, boolean isDirectory) {
        if (!sshClient.isConnected()) {
            return false;
        }

        try {
            String command = isDirectory ? "rm -rf \"" + path + "\"" : "rm \"" + path + "\"";
            String result = sshClient.executeCommand(command);
            return result == null || !result.contains("cannot remove") && !result.contains("No such file");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String renameFile(String oldPath, String newName) {
        if (!sshClient.isConnected()) {
            return null;
        }

        try {
            int lastSlash = oldPath.lastIndexOf('/');
            String parentPath = oldPath.substring(0, lastSlash);
            String newPath = parentPath.endsWith("/") ? parentPath + newName : parentPath + "/" + newName;

            String command = "mv \"" + oldPath + "\" \"" + newPath + "\"";
            String result = sshClient.executeCommand(command);

            if (result == null || !result.contains("cannot rename") && !result.contains("No such file")) {
                return newPath;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String readFile(String path, int maxSize) {
        if (!sshClient.isConnected()) {
            return null;
        }

        try {
            // 使用 cat 读取文件内容，head 限制行数
            String command = "head -c " + maxSize + " \"" + path + "\"";
            return sshClient.executeCommand(command);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
