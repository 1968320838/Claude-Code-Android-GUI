package com.claudebox.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.claudebox.data.local.ConfigManager
import com.claudebox.data.repository.ConnectionManager
import com.claudebox.data.ssh.ConnectionState
import com.claudebox.data.ssh.SSHConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val configManager = ConfigManager(application)
    private val prefs: SharedPreferences = application.getSharedPreferences("app_prefs", 0)

    // SSH 配置状态
    private val _sshConfig = MutableLiveData<SSHConfig>()
    val sshConfig: LiveData<SSHConfig> = _sshConfig

    // 连接状态
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    // UI 状态
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _testResult = MutableLiveData<TestResult?>()
    val testResult: LiveData<TestResult?> = _testResult

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    // Theme state
    private val _themeMode = MutableLiveData<Int>()
    val themeMode: LiveData<Int> = _themeMode

    // Font size state
    private val _fontSize = MutableLiveData<Int>()
    val fontSize: LiveData<Int> = _fontSize

    init {
        loadConfig()
        loadThemeMode()
        loadFontSize()
    }

    /**
     * 加载保存的配置
     */
    fun loadConfig() {
        viewModelScope.launch {
            val config = withContext(Dispatchers.IO) {
                configManager.loadSSHConfig()
            }
            _sshConfig.value = config
        }
    }

    /**
     * 加载主题模式
     */
    private fun loadThemeMode() {
        val mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        _themeMode.value = mode
    }

    /**
     * 加载字体大小
     */
    private fun loadFontSize() {
        val size = prefs.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM)
        _fontSize.value = size
    }

    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: Int) {
        prefs.edit { putInt(KEY_THEME_MODE, mode) }
        _themeMode.value = mode
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * 获取当前字体大小
     */
    fun getFontSize(): Int {
        return _fontSize.value ?: FONT_SIZE_MEDIUM
    }

    /**
     * 设置字体大小
     */
    fun setFontSize(size: Int) {
        prefs.edit { putInt(KEY_FONT_SIZE, size) }
        _fontSize.value = size
    }

    companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_FONT_SIZE = "font_size"

        const val FONT_SIZE_SMALL = 0
        const val FONT_SIZE_MEDIUM = 1
        const val FONT_SIZE_LARGE = 2
        const val FONT_SIZE_EXTRA_LARGE = 3
    }

    /**
     * 保存 SSH 配置
     */
    fun saveConfig(config: SSHConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            val success = withContext(Dispatchers.IO) {
                try {
                    configManager.saveSSHConfig(config)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            _isLoading.value = false
            _saveResult.value = success
        }
    }

    /**
     * 测试连接
     */
    fun testConnection(config: SSHConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            _testResult.value = null

            val result = withContext(Dispatchers.IO) {
                try {
                    // 临时使用测试配置
                    val testConfig = config.copy().apply {
                        // config 的 copy() 需要在 Java 中实现
                    }

                    // 使用 SSHClient 直接测试
                    val sshClient = com.claudebox.data.ssh.SSHClient()
                    val connected = sshClient.connect(config)

                    if (connected) {
                        sshClient.disconnect()
                        TestResult(true, "Connection successful!")
                    } else {
                        TestResult(false, "Connection failed")
                    }
                } catch (e: Exception) {
                    TestResult(false, e.message ?: "Unknown error")
                }
            }

            _isLoading.value = false
            _testResult.value = result
        }
    }

    /**
     * 连接到 Termux
     */
    fun connect(config: SSHConfig) {
        viewModelScope.launch {
            _isLoading.value = true

            withContext(Dispatchers.IO) {
                // 保存配置
                configManager.saveSSHConfig(config)

                // 获取 ConnectionManager 并连接
                val cm = ConnectionManager.getInstance()
                cm.connect(config)
            }
        }
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            ConnectionManager.getInstance().disconnect()
        }
    }

    /**
     * 验证配置
     */
    fun validateConfig(config: SSHConfig): ValidationResult {
        return when {
            config.host.isNullOrBlank() -> ValidationResult(false, "Host cannot be empty")
            config.port <= 0 || config.port > 65535 -> ValidationResult(false, "Port must be between 1 and 65535")
            config.username.isNullOrBlank() -> ValidationResult(false, "Username cannot be empty")
            config.authType == SSHConfig.AuthType.PASSWORD && config.password.isNullOrBlank() ->
                ValidationResult(false, "Password cannot be empty")
            config.authType == SSHConfig.AuthType.PRIVATE_KEY && config.privateKeyPath.isNullOrBlank() ->
                ValidationResult(false, "Private key path cannot be empty")
            else -> ValidationResult(true, null)
        }
    }

    /**
     * 创建 SSHConfig 对象
     */
    fun createSSHConfig(
        host: String,
        port: Int,
        username: String,
        authType: SSHConfig.AuthType,
        password: String?,
        privateKeyPath: String?,
        wrapperPath: String?
    ): SSHConfig {
        return SSHConfig().apply {
            setHost(host)
            setPort(port)
            setUsername(username)
            setAuthType(authType)
            setPassword(password)
            setPrivateKeyPath(privateKeyPath ?: "")
            setClaudeWrapperPath(wrapperPath ?: "~/.local/bin/claude-wrapper.sh")
        }
    }

    /**
     * 更新连接状态（从 ConnectionManager 观察）
     */
    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
        _isLoading.value = state is ConnectionState.Connecting || state is ConnectionState.Reconnecting
    }

    /**
     * 清除测试结果
     */
    fun clearTestResult() {
        _testResult.value = null
    }

    data class TestResult(val success: Boolean, val message: String)
    data class ValidationResult(val valid: Boolean, val errorMessage: String?)
}
