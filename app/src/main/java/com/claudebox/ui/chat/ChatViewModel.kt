package com.claudebox.ui.chat

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.claudebox.data.repository.ConnectionManager
import com.claudebox.data.repository.SessionRepositoryImpl
import com.claudebox.data.ssh.ConnectionState
import com.claudebox.domain.model.Message
import com.claudebox.domain.model.Session
import com.claudebox.domain.repository.SessionRepository
import com.claudebox.domain.repository.TermuxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val sessionRepository: SessionRepository,
    private val termuxRepository: TermuxRepository,
    private val connectionManager: ConnectionManager
) : AndroidViewModel(application) {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Sessions
    private val _sessions = MutableLiveData<List<Session>>(emptyList())
    val sessions: LiveData<List<Session>> = _sessions

    // Current session
    private val _currentSession = MutableLiveData<Session?>()
    val currentSession: LiveData<Session?> = _currentSession

    // Messages for current session
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    // Connection state
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: LiveData<ConnectionState> = _connectionState

    // Error message
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Is loading
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Shell output listener for chat
    private val shellOutputListener = object : TermuxRepository.ShellOutputListener {
        override fun onOutput(data: String) {
            mainHandler.post {
                appendToLastMessageOrCreateNew(data)
            }
        }

        override fun onClosed() {
            mainHandler.post {
                appendSystemMessage("[Shell closed]")
            }
        }
    }

    init {
        loadSessions()
        observeConnectionState()
    }

    private fun observeConnectionState() {
        mainHandler.post(object : Runnable {
            override fun run() {
                val state = connectionManager.connectionState
                _connectionState.value = state

                mainHandler.postDelayed(this, 500)
            }
        })
    }

    /**
     * Load all sessions from database.
     */
    fun loadSessions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionList = sessionRepository.sessions
            withContext(Dispatchers.Main) {
                _sessions.value = sessionList
            }
        }
    }

    /**
     * Create a new session.
     */
    fun createSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionRepository.createSession()
            withContext(Dispatchers.Main) {
                _currentSession.value = session
                _messages.value = emptyList()
                loadSessions()
                // Open shell for this session
                openShellIfNeeded()
            }
        }
    }

    /**
     * Select a session.
     */
    fun selectSession(session: Session) {
        _currentSession.value = session
        loadMessages(session.id)
        // Open shell for this session
        openShellIfNeeded()
    }

    /**
     * Open shell if not already open.
     */
    private fun openShellIfNeeded() {
        if (connectionManager.isConnected) {
            termuxRepository.addShellOutputListener(shellOutputListener)
            termuxRepository.openClaudeSession(object : TermuxRepository.ClaudeSessionCallback {
                override fun onOpened() {
                    // Shell opened, listener will receive output
                }

                override fun onOutput(data: String) {
                    // Output is also sent to ShellOutputListener
                }

                override fun onError(error: String) {
                    mainHandler.post {
                        _errorMessage.value = error
                    }
                }

                override fun onClosed() {
                    // Handled by ShellOutputListener
                }
            })
        }
    }

    /**
     * Delete a session.
     */
    fun deleteSession(session: Session) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.deleteSession(session.id)
            withContext(Dispatchers.Main) {
                if (_currentSession.value?.id == session.id) {
                    _currentSession.value = null
                    _messages.value = emptyList()
                    termuxRepository.removeShellOutputListener(shellOutputListener)
                }
                loadSessions()
            }
        }
    }

    /**
     * Load messages for a session.
     */
    private fun loadMessages(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val messageList = (sessionRepository as SessionRepositoryImpl).getMessages(sessionId)
            withContext(Dispatchers.Main) {
                _messages.value = messageList
            }
        }
    }

    /**
     * Send a message.
     */
    fun sendMessage(content: String) {
        val session = _currentSession.value ?: return

        // Add user message
        val userMessage = Message(
            UUID.randomUUID().toString(),
            session.id,
            content,
            content,
            true,
            System.currentTimeMillis()
        )

        // Add to UI immediately
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        currentMessages.add(userMessage)
        _messages.value = currentMessages

        // Save to database
        viewModelScope.launch(Dispatchers.IO) {
            (sessionRepository as SessionRepositoryImpl).addMessage(userMessage)
            sessionRepository.updateLastActive(session.id)
        }

        // Send via SSH if connected
        if (connectionManager.isConnected) {
            termuxRepository.sendToClaude(content)
        } else {
            _errorMessage.value = "Not connected to Termux"
        }
    }

    /**
     * Append output to last AI message or create new one.
     */
    private fun appendToLastMessageOrCreateNew(data: String) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        val lastIndex = currentMessages.lastIndex

        // Filter out command echo (lines starting with > or $)
        // This is a simple heuristic - in production would need better filtering
        val filteredData = filterCommandEcho(data)

        if (filteredData.isEmpty()) return

        if (lastIndex >= 0 && !currentMessages[lastIndex].isFromUser) {
            // Append to existing AI message
            val lastMessage = currentMessages[lastIndex]
            val updatedContent = lastMessage.content + filteredData
            val updatedMessage = Message(
                lastMessage.id,
                lastMessage.sessionId,
                updatedContent,
                updatedContent,
                false,
                lastMessage.timestamp
            )
            currentMessages[lastIndex] = updatedMessage
        } else {
            // Create new AI message
            val aiMessage = Message(
                UUID.randomUUID().toString(),
                _currentSession.value?.id ?: "",
                filteredData,
                filteredData,
                false,
                System.currentTimeMillis()
            )
            currentMessages.add(aiMessage)
        }

        _messages.value = currentMessages
    }

    /**
     * Filter out command echo from PTY output.
     */
    private fun filterCommandEcho(data: String): String {
        // Simple filter: remove lines that look like shell prompts
        // In production, would need more sophisticated filtering
        return data.lines()
            .filter { line ->
                !line.trim().startsWith("$") &&
                !line.trim().startsWith(">") &&
                !line.trim().startsWith("#") &&
                !line.trim().startsWith("claude")
            }
            .joinToString("\n")
    }

    /**
     * Append a system message.
     */
    private fun appendSystemMessage(msg: String) {
        val currentMessages = _messages.value?.toMutableList() ?: mutableListOf()
        val systemMessage = Message(
            UUID.randomUUID().toString(),
            _currentSession.value?.id ?: "",
            msg,
            msg,
            false,
            System.currentTimeMillis()
        )
        currentMessages.add(systemMessage)
        _messages.value = currentMessages
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
