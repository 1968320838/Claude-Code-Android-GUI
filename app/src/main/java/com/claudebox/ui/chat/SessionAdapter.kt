package com.claudebox.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.claudebox.databinding.ItemSessionBinding
import com.claudebox.domain.model.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SessionAdapter(
    private val onSessionClick: (Session) -> Unit,
    private val onDeleteClick: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: Session) {
            binding.sessionName.text = session.name
            binding.sessionLastActive.text = formatLastActive(session.lastActiveAt)

            binding.root.setOnClickListener {
                onSessionClick(session)
            }

            binding.sessionDelete.setOnClickListener {
                onDeleteClick(session)
            }
        }

        private fun formatLastActive(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes min ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours hours ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days days ago"
                }
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.name == newItem.name &&
                    oldItem.lastActiveAt == newItem.lastActiveAt
        }
    }
}
