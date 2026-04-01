package com.claudebox.data.ssh;

public class SSHConfig {
    private String host;
    private int port;
    private String username;
    private AuthType authType;
    private String password;
    private String privateKeyPath;
    private String knownHostsPath;
    private String claudeWrapperPath;

    public enum AuthType {
        PASSWORD,
        PRIVATE_KEY
    }

    public SSHConfig() {
        this.port = 8022;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getKnownHostsPath() {
        return knownHostsPath;
    }

    public void setKnownHostsPath(String knownHostsPath) {
        this.knownHostsPath = knownHostsPath;
    }

    public String getClaudeWrapperPath() {
        return claudeWrapperPath;
    }

    public void setClaudeWrapperPath(String claudeWrapperPath) {
        this.claudeWrapperPath = claudeWrapperPath;
    }
}
