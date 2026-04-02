package com.claudebox.data.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

public class SSHClient {
    private Session session;
    private SSHConfig config;
    private boolean connected = false;

    public SSHClient() {
    }

    public boolean connect(SSHConfig config) {
        this.config = config;

        try {
            JSch jsch = new JSch();

            // 如果使用私钥认证，设置私钥
            if (config.getAuthType() == SSHConfig.AuthType.PRIVATE_KEY) {
                if (config.getPrivateKeyPath() != null && !config.getPrivateKeyPath().isEmpty()) {
                    jsch.addIdentity(config.getPrivateKeyPath());
                }
            }

            // 设置已知主机
            if (config.getKnownHostsPath() != null && !config.getKnownHostsPath().isEmpty()) {
                jsch.setKnownHosts(config.getKnownHostsPath());
            }

            session = jsch.getSession(
                    config.getUsername(),
                    config.getHost(),
                    config.getPort()
            );

            // 设置用户信息
            final String password = config.getPassword();
            final SSHConfig.AuthType authType = config.getAuthType();

            session.setUserInfo(new UserInfo() {
                @Override
                public String getPassword() {
                    return password;
                }

                @Override
                public boolean promptPassword(String message) {
                    return password != null && !password.isEmpty();
                }

                @Override
                public String getPassphrase() {
                    return null;
                }

                @Override
                public boolean promptPassphrase(String message) {
                    return false;
                }

                @Override
                public boolean promptYesNo(String message) {
                    return false;
                }

                @Override
                public void showMessage(String message) {
                }
            });

            // 设置连接超时（30秒）
            session.setTimeout(30000);

            // 根据认证类型设置密码或空密码
            if (authType == SSHConfig.AuthType.PASSWORD && password != null) {
                session.setPassword(password);
            }

            // 连接
            session.connect();

            connected = session.isConnected();
            return connected;

        } catch (JSchException e) {
            e.printStackTrace();
            connected = false;
            return false;
        }
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        connected = false;
        session = null;
    }

    public boolean isConnected() {
        return session != null && session.isConnected() && connected;
    }

    /**
     * 执行命令并返回输出
     */
    public String executeCommand(String command) {
        if (!isConnected()) {
            return null;
        }

        try {
            com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            InputStream in = channel.getInputStream();
            InputStream err = channel.getExtInputStream();

            channel.connect();

            StringBuilder output = new StringBuilder();
            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ee) {
                    break;
                }
            }

            channel.disconnect();
            return output.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 打开交互式 shell 通道（用于 PTY）
     */
    public com.jcraft.jsch.Channel openChannel() {
        if (!isConnected()) {
            return null;
        }

        try {
            com.jcraft.jsch.ChannelShell channel = (com.jcraft.jsch.ChannelShell) session.openChannel("shell");
            channel.setPtyType("xterm-256color");
            return channel;
        } catch (JSchException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 请求 PTY（伪终端）
     */
    public boolean requestPty(String term, int cols, int rows) {
        if (!isConnected()) {
            return false;
        }

        try {
            com.jcraft.jsch.ChannelShell channel = (com.jcraft.jsch.ChannelShell) session.openChannel("shell");
            channel.setPtyType(term);
            channel.setPtySize(cols, rows, 0, 0);
            channel.connect();
            return true;
        } catch (JSchException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 打开 shell 并返回输入输出流
     */
    public ShellChannel openShellChannel() {
        if (!isConnected()) {
            return null;
        }

        try {
            com.jcraft.jsch.ChannelShell channel = (com.jcraft.jsch.ChannelShell) session.openChannel("shell");
            channel.setPtyType("xterm-256color");

            // 设置窗口大小
            channel.setPtySize(80, 24, 0, 0);

            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();

            channel.connect();

            return new ShellChannel(channel, in, out);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取 Claude wrapper 脚本路径
     */
    public String getClaudeWrapperPath() {
        if (config != null && config.getClaudeWrapperPath() != null) {
            return config.getClaudeWrapperPath();
        }
        return "~/.local/bin/claude-wrapper.sh";
    }

    /**
     * Shell 通道封装类
     */
    public static class ShellChannel {
        private final com.jcraft.jsch.ChannelShell channel;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ShellChannel(com.jcraft.jsch.ChannelShell channel, InputStream in, OutputStream out) {
            this.channel = channel;
            this.inputStream = in;
            this.outputStream = out;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }

        public boolean isConnected() {
            return channel != null && channel.isConnected();
        }

        public void disconnect() {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }

        public void resize(int cols, int rows) {
            if (channel != null && channel.isConnected()) {
                channel.setPtySize(cols, rows, 0, 0);
            }
        }
    }
}
