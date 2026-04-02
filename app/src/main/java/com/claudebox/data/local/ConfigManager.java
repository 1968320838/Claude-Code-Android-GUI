package com.claudebox.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.claudebox.data.ssh.SSHConfig;
import com.claudebox.data.ssh.ConnectionState;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ConfigManager {
    private static final String PREFS_NAME = "claudebox_config";
    private static final String KEY_HOST = "ssh_host";
    private static final String KEY_PORT = "ssh_port";
    private static final String KEY_USERNAME = "ssh_username";
    private static final String KEY_AUTH_TYPE = "ssh_auth_type";
    private static final String KEY_PASSWORD = "ssh_password";
    private static final String KEY_PRIVATE_KEY_PATH = "ssh_private_key_path";
    private static final String KEY_WRAPPER_PATH = "claude_wrapper_path";

    private SharedPreferences prefs;

    public ConfigManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to regular SharedPreferences (less secure)
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * 保存 SSH 配置
     */
    public void saveSSHConfig(SSHConfig config) {
        prefs.edit()
                .putString(KEY_HOST, config.getHost())
                .putInt(KEY_PORT, config.getPort())
                .putString(KEY_USERNAME, config.getUsername())
                .putString(KEY_AUTH_TYPE, config.getAuthType() != null ? config.getAuthType().name() : null)
                .putString(KEY_PASSWORD, config.getPassword())
                .putString(KEY_PRIVATE_KEY_PATH, config.getPrivateKeyPath())
                .putString(KEY_WRAPPER_PATH, config.getClaudeWrapperPath())
                .apply();
    }

    /**
     * 加载 SSH 配置
     */
    public SSHConfig loadSSHConfig() {
        SSHConfig config = new SSHConfig();
        config.setHost(prefs.getString(KEY_HOST, ""));
        config.setPort(prefs.getInt(KEY_PORT, 8022));
        config.setUsername(prefs.getString(KEY_USERNAME, ""));

        String authTypeStr = prefs.getString(KEY_AUTH_TYPE, null);
        if (authTypeStr != null) {
            try {
                config.setAuthType(SSHConfig.AuthType.valueOf(authTypeStr));
            } catch (IllegalArgumentException e) {
                config.setAuthType(SSHConfig.AuthType.PASSWORD);
            }
        } else {
            config.setAuthType(SSHConfig.AuthType.PASSWORD);
        }

        config.setPassword(prefs.getString(KEY_PASSWORD, ""));
        config.setPrivateKeyPath(prefs.getString(KEY_PRIVATE_KEY_PATH, ""));
        config.setClaudeWrapperPath(prefs.getString(KEY_WRAPPER_PATH, "~/.local/bin/claude-wrapper.sh"));

        return config;
    }

    /**
     * 检查是否已有配置
     */
    public boolean hasSSHConfig() {
        String host = prefs.getString(KEY_HOST, null);
        String username = prefs.getString(KEY_USERNAME, null);
        return host != null && !host.isEmpty() && username != null && !username.isEmpty();
    }

    /**
     * 清空配置
     */
    public void clearSSHConfig() {
        prefs.edit().clear().apply();
    }

    /**
     * 保存连接状态
     */
    public void saveConnectionState(ConnectionState state) {
        String stateName;
        if (state instanceof ConnectionState.Connected) {
            stateName = "Connected";
        } else if (state instanceof ConnectionState.Disconnected) {
            stateName = "Disconnected";
        } else {
            stateName = "Disconnected";
        }
        prefs.edit().putString("last_connection_state", stateName).apply();
    }

    /**
     * 获取上次连接状态
     */
    public String getLastConnectionState() {
        return prefs.getString("last_connection_state", "Disconnected");
    }
}
