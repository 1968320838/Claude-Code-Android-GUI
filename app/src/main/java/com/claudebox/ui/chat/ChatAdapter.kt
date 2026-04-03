package com.claudebox.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.claudebox.databinding.ItemMessageBotBinding
import com.claudebox.databinding.ItemMessageUserBinding
import com.claudebox.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_BOT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFromUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemMessageUserBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                UserMessageViewHolder(binding)
            }
            else -> {
                val binding = ItemMessageBotBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                BotMessageViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is BotMessageViewHolder -> holder.bind(message)
        }
    }

    inner class UserMessageViewHolder(
        private val binding: ItemMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.messageContent.text = message.content
            binding.messageTime.text = formatTime(message.timestamp)
        }

        private fun formatTime(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    inner class BotMessageViewHolder(
        private val binding: ItemMessageBotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.messageTime.text = formatTime(message.timestamp)

            // Render Markdown for bot messages
            val context = binding.root.context
            val markdown = message.content
            val spanned = MarkwonFactory.toMarkdown(context, markdown)
            binding.messageContent.text = spanned
        }

        private fun formatTime(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.content == newItem.content &&
                    oldItem.timestamp == newItem.timestamp
        }
    }
}
