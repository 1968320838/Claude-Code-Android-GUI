package com.claudebox.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.claudebox.R
import com.claudebox.data.ssh.ConnectionState
import com.claudebox.databinding.FragmentChatBinding
import com.claudebox.domain.model.Session
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels()

    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupInput()
        setupObservers()
    }

    private fun setupToolbar() {
        binding.chatToolbar.setOnClickListener {
            showSessionSelector()
        }

        binding.btnCreateSession.setOnClickListener {
            viewModel.createSession()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()

        binding.messagesRecyclerView.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupInput() {
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        binding.messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val content = binding.messageInput.text?.toString()?.trim()
        if (content.isNullOrEmpty()) return

        viewModel.sendMessage(content)
        binding.messageInput.text?.clear()
    }

    private fun setupObservers() {
        viewModel.currentSession.observe(viewLifecycleOwner) { session ->
            if (session == null) {
                showEmptyState()
                binding.sessionName.text = getString(R.string.no_session_selected)
            } else {
                hideEmptyState()
                binding.sessionName.text = session.name
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                // Scroll to bottom when new messages arrive
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            updateConnectionIndicator(state)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun showSessionSelector() {
        val sessions = viewModel.sessions.value ?: emptyList()

        if (sessions.isEmpty()) {
            viewModel.createSession()
            return
        }

        val sessionNames = sessions.map { it.name }.toTypedArray()
        val currentSession = viewModel.currentSession.value
        val currentIndex = sessions.indexOfFirst { it.id == currentSession?.id }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sessions)
            .setSingleChoiceItems(sessionNames, currentIndex) { dialog, which ->
                viewModel.selectSession(sessions[which])
                dialog.dismiss()
            }
            .setPositiveButton(R.string.new_session) { _, _ ->
                viewModel.createSession()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEmptyState() {
        binding.emptyState.isVisible = true
        binding.messagesRecyclerView.isVisible = false
        binding.inputContainer.isVisible = false
    }

    private fun hideEmptyState() {
        binding.emptyState.isVisible = false
        binding.messagesRecyclerView.isVisible = true
        binding.inputContainer.isVisible = true
    }

    private fun updateConnectionIndicator(state: ConnectionState) {
        val indicatorRes = when (state) {
            is ConnectionState.Connected -> R.drawable.connection_indicator_connected
            is ConnectionState.Connecting, is ConnectionState.Reconnecting -> R.drawable.connection_indicator_connecting
            is ConnectionState.Disconnected, is ConnectionState.Error -> R.drawable.connection_indicator_disconnected
            else -> R.drawable.connection_indicator_disconnected
        }
        binding.connectionIndicator.setBackgroundResource(indicatorRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
