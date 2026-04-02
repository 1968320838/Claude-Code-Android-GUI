package com.claudebox.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.claudebox.R
import com.claudebox.data.repository.ConnectionManager
import com.claudebox.data.ssh.ConnectionState
import com.claudebox.data.ssh.SSHConfig
import com.claudebox.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicReference

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private var currentConfig: SSHConfig? = null
    private var isInitialLoad = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
        observeConnectionState()
    }

    private fun setupUI() {
        // 认证方式切换
        binding.radioAuthType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_password -> {
                    binding.layoutPassword.isVisible = true
                    binding.layoutPrivateKey.isVisible = false
                }
                R.id.radio_key -> {
                    binding.layoutPassword.isVisible = false
                    binding.layoutPrivateKey.isVisible = true
                }
            }
        }

        // 测试连接按钮
        binding.btnTestConnection.setOnClickListener {
            val config = buildSSHConfigFromUI()
            val validation = viewModel.validateConfig(config)
            if (!validation.valid) {
                Toast.makeText(context, validation.errorMessage, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.testConnection(config)
        }

        // 保存配置按钮
        binding.btnSaveConfig.setOnClickListener {
            val config = buildSSHConfigFromUI()
            val validation = viewModel.validateConfig(config)
            if (!validation.valid) {
                Toast.makeText(context, validation.errorMessage, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.saveConfig(config)
        }

        // 连接/断开按钮
        binding.btnConnect.setOnClickListener {
            val state = viewModel.connectionState.value
            if (state is ConnectionState.Connected) {
                viewModel.disconnect()
            } else {
                val config = buildSSHConfigFromUI()
                val validation = viewModel.validateConfig(config)
                if (!validation.valid) {
                    Toast.makeText(context, validation.errorMessage, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.connect(config)
            }
        }
    }

    private fun observeViewModel() {
        // 观察加载的配置
        viewModel.sshConfig.observe(viewLifecycleOwner) { config ->
            if (isInitialLoad && config != null) {
                populateUI(config)
                currentConfig = config
                isInitialLoad = false
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.isVisible = isLoading
            binding.btnTestConnection.isEnabled = !isLoading
            binding.btnSaveConfig.isEnabled = !isLoading
            binding.btnConnect.isEnabled = !isLoading
        }

        // 观察测试结果
        viewModel.testResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                binding.textTestResult.isVisible = true
                binding.textTestResult.text = if (it.success) {
                    getString(R.string.test_success)
                } else {
                    getString(R.string.test_failed, it.message)
                }
                binding.textTestResult.setTextColor(
                    if (it.success) {
                        resources.getColor(R.color.connection_success, null)
                    } else {
                        resources.getColor(R.color.connection_error, null)
                    }
                )
            }
        }

        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            Toast.makeText(
                context,
                if (success) R.string.config_saved else R.string.config_save_failed,
                Toast.LENGTH_SHORT
            ).show()
        }

        // 观察连接状态
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            updateConnectionUI(state)
        }
    }

    private fun observeConnectionState() {
        // 从 ConnectionManager 观察连接状态变化
        try {
            val cm = ConnectionManager.getInstance()
            val stateRef = cm.connectionStateRef
            // 使用轮询方式观察状态变化（简化实现）
            binding.root.postDelayed(object : Runnable {
                override fun run() {
                    stateRef.get()?.let { state ->
                        viewModel.updateConnectionState(state)
                    }
                    if (_binding != null) {
                        binding.root.postDelayed(this, 500)
                    }
                }
            }, 500)
        } catch (e: Exception) {
            // ConnectionManager 尚未初始化
        }
    }

    private fun populateUI(config: SSHConfig) {
        binding.inputHost.setText(config.host)
        binding.inputPort.setText(config.port.toString())
        binding.inputUsername.setText(config.username)

        when (config.authType) {
            SSHConfig.AuthType.PASSWORD -> {
                binding.radioPassword.isChecked = true
                binding.layoutPassword.isVisible = true
                binding.layoutPrivateKey.isVisible = false
                binding.inputPassword.setText(config.password)
            }
            SSHConfig.AuthType.PRIVATE_KEY -> {
                binding.radioKey.isChecked = true
                binding.layoutPassword.isVisible = false
                binding.layoutPrivateKey.isVisible = true
                binding.inputPrivateKey.setText(config.privateKeyPath)
            }
            else -> {
                binding.radioPassword.isChecked = true
            }
        }

        binding.inputWrapperPath.setText(
            config.claudeWrapperPath ?: "~/.local/bin/claude-wrapper.sh"
        )
    }

    private fun buildSSHConfigFromUI(): SSHConfig {
        val authType = if (binding.radioPassword.isChecked) {
            SSHConfig.AuthType.PASSWORD
        } else {
            SSHConfig.AuthType.PRIVATE_KEY
        }

        return viewModel.createSSHConfig(
            host = binding.inputHost.text.toString().trim(),
            port = binding.inputPort.text.toString().toIntOrNull() ?: 8022,
            username = binding.inputUsername.text.toString().trim(),
            authType = authType,
            password = if (authType == SSHConfig.AuthType.PASSWORD) {
                binding.inputPassword.text.toString()
            } else null,
            privateKeyPath = if (authType == SSHConfig.AuthType.PRIVATE_KEY) {
                binding.inputPrivateKey.text.toString().trim()
            } else null,
            wrapperPath = binding.inputWrapperPath.text.toString().trim()
        )
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            is ConnectionState.Disconnected -> {
                binding.connectionIndicator.setBackgroundResource(R.drawable.connection_indicator_disconnected)
                binding.connectionStatusText.text = getString(R.string.connection_disconnected)
                binding.btnConnect.text = getString(R.string.connect)
            }
            is ConnectionState.Connecting -> {
                binding.connectionIndicator.setBackgroundResource(R.drawable.connection_indicator_connecting)
                binding.connectionStatusText.text = getString(R.string.connection_connecting)
                binding.btnConnect.text = getString(R.string.connect)
                binding.btnConnect.isEnabled = false
            }
            is ConnectionState.Connected -> {
                binding.connectionIndicator.setBackgroundResource(R.drawable.connection_indicator_connected)
                binding.connectionStatusText.text = getString(R.string.connection_connected)
                binding.btnConnect.text = getString(R.string.disconnect)
                binding.btnConnect.isEnabled = true
            }
            is ConnectionState.Error -> {
                binding.connectionIndicator.setBackgroundResource(R.drawable.connection_indicator_disconnected)
                binding.connectionStatusText.text = getString(R.string.connection_error)
                binding.btnConnect.text = getString(R.string.reconnect)
                binding.btnConnect.isEnabled = true
            }
            is ConnectionState.Reconnecting -> {
                binding.connectionIndicator.setBackgroundResource(R.drawable.connection_indicator_connecting)
                binding.connectionStatusText.text = getString(
                    R.string.connection_reconnecting,
                    state.attempt,
                    3
                )
                binding.btnConnect.text = getString(R.string.connect)
                binding.btnConnect.isEnabled = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
