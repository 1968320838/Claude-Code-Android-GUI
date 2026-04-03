package com.claudebox.ui.terminal

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.claudebox.data.repository.ConnectionManager
import com.claudebox.data.ssh.ConnectionState
import com.claudebox.domain.repository.TermuxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    application: Application,
    private val termuxRepository: TermuxRepository,
    private val connectionManager: ConnectionManager
) : AndroidViewModel(application) {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Connection state
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: LiveData<ConnectionState> = _connectionState

    // Terminal output
    private val _terminalOutput = MutableLiveData<String>()
    val terminalOutput: LiveData<String> = _terminalOutput

    // Terminal ready state
    private val _isTerminalReady = MutableLiveData(false)
    val isTerminalReady: LiveData<Boolean> = _isTerminalReady

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Shell output listener
    private val shellOutputListener = object : TermuxRepository.ShellOutputListener {
        override fun onOutput(data: String) {
            _terminalOutput.postValue(data)
        }

        override fun onClosed() {
            mainHandler.post {
                _terminalOutput.postValue("\r\n[Shell closed]\r\n")
            }
        }
    }

    init {
        startConnectionStatePolling()
    }

    private fun startConnectionStatePolling() {
        mainHandler.post(object : Runnable {
            override fun run() {
                val state = connectionManager.connectionState
                _connectionState.value = state

                when (state) {
                    is ConnectionState.Connected -> {
                        openShellIfNeeded()
                    }
                    is ConnectionState.Disconnected -> {
                        // Shell is closed by TermuxRepositoryImpl
                    }
                    is ConnectionState.Error -> {
                        _errorMessage.postValue(state.message)
                    }
                    else -> {}
                }

                mainHandler.postDelayed(this, 500)
            }
        })
    }

    /**
     * Open shell if not already open.
     */
    private fun openShellIfNeeded() {
        // Add listener first
        termuxRepository.addShellOutputListener(shellOutputListener)

        // Then open shell (will be no-op if already open)
        termuxRepository.openClaudeSession(object : TermuxRepository.ClaudeSessionCallback {
            override fun onOpened() {
                mainHandler.post {
                    _terminalOutput.postValue("[Shell started]\r\n")
                }
            }

            override fun onOutput(data: String) {
                // Output is sent to ShellOutputListener
            }

            override fun onError(error: String) {
                _errorMessage.postValue(error)
            }

            override fun onClosed() {
                // Handled by ShellOutputListener
            }
        })
    }

    /**
     * Set terminal ready state.
     */
    fun setTerminalReady(ready: Boolean) {
        _isTerminalReady.postValue(ready)
    }

    /**
     * Send data to terminal input.
     */
    fun sendInput(data: String) {
        if (!connectionManager.isConnected) {
            _errorMessage.postValue("Not connected")
            return
        }

        termuxRepository.sendToClaude(data)
    }

    /**
     * Resize terminal.
     */
    fun resizeTerminal(cols: Int, rows: Int) {
        // Resize is handled via the shell channel in the repository
    }

    /**
     * Close shell session.
     */
    fun closeShell() {
        termuxRepository.removeShellOutputListener(shellOutputListener)
        termuxRepository.closeClaudeSession()
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        termuxRepository.removeShellOutputListener(shellOutputListener)
    }
}
