package com.claudebox.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.claudebox.R
import com.claudebox.databinding.FragmentSessionListBinding
import com.claudebox.domain.model.Session
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionListFragment : Fragment() {

    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by activityViewModels()

    private lateinit var sessionAdapter: SessionAdapter

    interface SessionListListener {
        fun onSessionSelected(session: Session)
    }

    var listener: SessionListListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        setupObservers()
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            onSessionClick = { session ->
                viewModel.selectSession(session)
                listener?.onSessionSelected(session)
            },
            onDeleteClick = { session ->
                showDeleteConfirmation(session)
            }
        )

        binding.sessionRecyclerView.apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabNewSession.setOnClickListener {
            viewModel.createSession()
        }
    }

    private fun setupObservers() {
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions)
            updateEmptyState(sessions.isEmpty())
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.isVisible = isEmpty
        binding.sessionRecyclerView.isVisible = !isEmpty
    }

    private fun showDeleteConfirmation(session: Session) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_session)
            .setMessage(R.string.delete_session_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteSession(session)
                Toast.makeText(requireContext(), R.string.session_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
