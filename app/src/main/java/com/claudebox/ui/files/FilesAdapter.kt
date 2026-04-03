package com.claudebox.ui.files

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.claudebox.R
import com.claudebox.databinding.ItemFileBinding
import com.claudebox.domain.model.FileItem

class FilesAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onDirectoryClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit
) : ListAdapter<FileItem, FilesAdapter.FileViewHolder>(FileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item.isDirectory) {
                        onDirectoryClick(item)
                    } else {
                        onItemClick(item)
                    }
                }
            }

            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
        }

        fun bind(item: FileItem) {
            binding.fileName.text = item.name

            if (item.isDirectory) {
                binding.fileIcon.setImageResource(R.drawable.ic_folder)
                binding.fileInfo.text = if (item.isExpanded) "Expanded" else "Tap to open"
                binding.fileChevron.visibility = View.VISIBLE
                // Rotate chevron when expanded
                binding.fileChevron.rotation = if (item.isExpanded) 90f else 0f
            } else {
                binding.fileIcon.setImageResource(getFileIcon(item.name))
                binding.fileInfo.text = formatFileSize(item.size)
                binding.fileChevron.visibility = View.GONE
            }
        }

        private fun getFileIcon(fileName: String): Int {
            val lowerName = fileName.lowercase()
            return when {
                lowerName.endsWith(".png") ||
                lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".gif") ||
                lowerName.endsWith(".webp") ||
                lowerName.endsWith(".bmp") -> R.drawable.ic_file_image

                lowerName.endsWith(".kt") ||
                lowerName.endsWith(".java") ||
                lowerName.endsWith(".cpp") ||
                lowerName.endsWith(".c") ||
                lowerName.endsWith(".h") ||
                lowerName.endsWith(".py") ||
                lowerName.endsWith(".js") ||
                lowerName.endsWith(".ts") ||
                lowerName.endsWith(".html") ||
                lowerName.endsWith(".css") ||
                lowerName.endsWith(".xml") ||
                lowerName.endsWith(".json") ||
                lowerName.endsWith(".gradle") ||
                lowerName.endsWith(".sh") -> R.drawable.ic_file_code

                else -> R.drawable.ic_file_document
            }
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size < 1024 -> binding.root.context.getString(R.string.files_bytes, size)
                size < 1024 * 1024 -> binding.root.context.getString(R.string.files_kilobytes, size / 1024f)
                else -> binding.root.context.getString(R.string.files_megabytes, size / (1024f * 1024f))
            }
        }
    }

    private class FileDiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.name == newItem.name &&
                   oldItem.isDirectory == newItem.isDirectory &&
                   oldItem.size == newItem.size &&
                   oldItem.isExpanded == newItem.isExpanded
        }
    }
}
