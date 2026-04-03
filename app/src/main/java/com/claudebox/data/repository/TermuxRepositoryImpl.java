package com.claudebox.data.repository;

import com.claudebox.data.ssh.SSHClient;
import com.claudebox.data.ssh.SSHConfig;
import com.claudebox.domain.repository.TermuxRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
}
