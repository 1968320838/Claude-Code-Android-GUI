package com.claudebox.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.claudebox.R
import com.claudebox.databinding.DialogConfirmBinding
import com.claudebox.databinding.DialogInputBinding
import com.claudebox.databinding.FragmentFilesBinding
import com.claudebox.domain.model.FileItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FilesViewModel by viewModels()
    private lateinit var filesAdapter: FilesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupAdapter() {
        filesAdapter = FilesAdapter(
            onItemClick = { fileItem ->
                // 显示文件预览
                if (!fileItem.isDirectory) {
                    FilePreviewDialogFragment.newInstance(fileItem)
                        .show(childFragmentManager, "file_preview")
                }
            },
            onDirectoryClick = { fileItem ->
                // 切换展开/折叠状态
                viewModel.toggleExpand(fileItem)
            },
            onItemLongClick = { fileItem ->
                // 显示上下文菜单
                showContextMenu(fileItem)
            }
        )
        binding.filesList.adapter = filesAdapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            viewModel.navigateUp()
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.refresh()
        }

        binding.fabNew.setOnClickListener {
            showCreateDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.displayFiles.observe(viewLifecycleOwner) { files ->
            filesAdapter.submitList(files.toList())
            binding.filesEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            binding.filesList.visibility = if (files.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.filesLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { fileItem ->
            fileItem?.let {
                Toast.makeText(requireContext(), "Preview: ${it.name}", Toast.LENGTH_SHORT).show()
                viewModel.clearNavigationEvent()
            }
        }

        viewModel.breadcrumbs.observe(viewLifecycleOwner) { breadcrumbs ->
            updateBreadcrumbs(breadcrumbs)
        }

        viewModel.currentPath.observe(viewLifecycleOwner) { path ->
            // 禁用返回按钮在根目录
            binding.btnBack.isEnabled = path != "/"
            binding.btnBack.alpha = if (path == "/") 0.3f else 1f
        }

        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.success) {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                }
                viewModel.clearOperationResult()
            }
        }
    }

    private fun showContextMenu(fileItem: FileItem) {
        val popup = PopupMenu(requireContext(), binding.filesList.findViewHolderForAdapterPosition(
            filesAdapter.currentList.indexOf(fileItem)
        )?.itemView ?: return)

        popup.menu.add(0, 1, 0, getString(R.string.open))
        popup.menu.add(0, 2, 1, getString(R.string.rename))
        popup.menu.add(0, 3, 2, getString(R.string.delete))

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    // Open
                    if (fileItem.isDirectory) {
                        viewModel.navigateTo(fileItem)
                    } else {
                        Toast.makeText(requireContext(), "Preview: ${fileItem.name}", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                2 -> {
                    // Rename
                    showRenameDialog(fileItem)
                    true
                }
                3 -> {
                    // Delete
                    showDeleteConfirmDialog(fileItem)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showCreateDialog() {
        val options = arrayOf(getString(R.string.new_file), getString(R.string.new_folder))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.new_file))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showInputDialog(false)
                    1 -> showInputDialog(true)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showInputDialog(isDirectory: Boolean) {
        val dialogBinding = DialogInputBinding.inflate(layoutInflater)
        dialogBinding.inputLayout.hint = if (isDirectory) {
            getString(R.string.folder_name)
        } else {
            getString(R.string.file_name)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isDirectory) getString(R.string.new_folder) else getString(R.string.new_file))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val name = dialogBinding.inputText.text.toString().trim()
                if (name.isNotEmpty()) {
                    if (isDirectory) {
                        viewModel.createDirectory(name)
                    } else {
                        viewModel.createFile(name)
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_empty_name), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val dialogBinding = DialogInputBinding.inflate(layoutInflater)
        dialogBinding.inputLayout.hint = getString(R.string.file_name)
        dialogBinding.inputText.setText(fileItem.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.rename))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                val newName = dialogBinding.inputText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renameFile(fileItem, newName)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_empty_name), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmDialog(fileItem: FileItem) {
        val dialogBinding = DialogConfirmBinding.inflate(layoutInflater)
        dialogBinding.confirmMessage.text = getString(R.string.delete_confirm, fileItem.name)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteFile(fileItem)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateBreadcrumbs(breadcrumbs: List<String>) {
        binding.breadcrumbContainer.removeAllViews()

        breadcrumbs.forEachIndexed { index, path ->
            // 添加分隔符
            if (index > 0) {
                val separator = TextView(requireContext()).apply {
                    text = " / "
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.breadcrumb_separator))
                }
                binding.breadcrumbContainer.addView(separator)
            }

            // 添加路径片段
            val segmentView = TextView(requireContext()).apply {
                text = if (path == "/") "Root" else path.substringAfterLast('/')
                setTextColor(
                    if (index == breadcrumbs.size - 1)
                        ContextCompat.getColor(requireContext(), R.color.breadcrumb_active)
                    else
                        ContextCompat.getColor(requireContext(), R.color.breadcrumb_inactive)
                )
                textSize = if (index == breadcrumbs.size - 1) 14f else 13f
                isClickable = index < breadcrumbs.size - 1

                setOnClickListener {
                    if (index < breadcrumbs.size - 1) {
                        viewModel.navigateToBreadcrumb(path)
                    }
                }
            }
            binding.breadcrumbContainer.addView(segmentView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
