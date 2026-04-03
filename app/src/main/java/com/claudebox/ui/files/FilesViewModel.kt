package com.claudebox.ui.files

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudebox.domain.model.FileItem
import com.claudebox.domain.repository.TermuxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val termuxRepository: TermuxRepository
) : ViewModel() {

    private val _currentPath = MutableLiveData("/")
    val currentPath: LiveData<String> = _currentPath

    private val _displayFiles = MutableLiveData<List<FileItem>>(emptyList())
    val displayFiles: LiveData<List<FileItem>> = _displayFiles

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _navigationEvent = MutableLiveData<FileItem?>(null)
    val navigationEvent: LiveData<FileItem?> = _navigationEvent

    // 面包屑路径分段
    private val _breadcrumbs = MutableLiveData<List<String>>(listOf("/"))
    val breadcrumbs: LiveData<List<String>> = _breadcrumbs

    // 操作结果事件
    private val _operationResult = MutableLiveData<OperationResult?>(null)
    val operationResult: LiveData<OperationResult?> = _operationResult

    // 目录内容缓存: path -> List<FileItem>
    private val directoryCache = mutableMapOf<String, List<FileItem>>()

    // 当前显示的扁平文件列表（包含展开的子项）
    private val flatFileList = mutableListOf<FileItem>()

    init {
        loadDirectory(_currentPath.value ?: "/")
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val files = withContext(Dispatchers.IO) {
                    termuxRepository.listDirectory(path)
                }

                // 缓存目录内容
                directoryCache[path] = files

                // 更新当前路径
                _currentPath.value = path

                // 更新面包屑
                updateBreadcrumbs(path)

                // 重置扁平列表并显示当前目录内容
                flatFileList.clear()
                // 标记所有项目未展开
                files.forEach { it.isExpanded = false }
                flatFileList.addAll(files)

                _displayFiles.value = flatFileList.toList()

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load files"
                _displayFiles.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleExpand(fileItem: FileItem) {
        val index = flatFileList.indexOfFirst { it.path == fileItem.path }
        if (index == -1) return

        val item = flatFileList[index]
        item.isExpanded = !item.isExpanded

        if (item.isExpanded) {
            // 展开：加载并插入子项
            loadAndExpandChildren(item, index)
        } else {
            // 折叠：移除子项
            collapseDirectory(item)
        }

        _displayFiles.value = flatFileList.toList()
    }

    private fun loadAndExpandChildren(parent: FileItem, parentIndex: Int) {
        val children = directoryCache[parent.path]
        if (children != null) {
            insertChildren(parent, parentIndex, children)
        } else {
            // 需要从服务器加载
            viewModelScope.launch {
                try {
                    val children = withContext(Dispatchers.IO) {
                        termuxRepository.listDirectory(parent.path)
                    }
                    directoryCache[parent.path] = children
                    insertChildren(parent, parentIndex, children)
                    _displayFiles.value = flatFileList.toList()
                } catch (e: Exception) {
                    _error.value = "Failed to load ${parent.name}"
                    parent.isExpanded = false
                    _displayFiles.value = flatFileList.toList()
                }
            }
        }
    }

    private fun insertChildren(parent: FileItem, parentIndex: Int, children: List<FileItem>) {
        // 为子项设置父路径
        children.forEach { it.parentPath = parent.path }
        // 在父目录后插入子项
        flatFileList.addAll(parentIndex + 1, children)
    }

    private fun collapseDirectory(parent: FileItem) {
        val childrenToRemove = flatFileList.filter {
            it.parentPath == parent.path
        }
        flatFileList.removeAll(childrenToRemove)
        parent.isExpanded = false
    }

    fun navigateTo(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            loadDirectory(fileItem.path)
        } else {
            _navigationEvent.value = fileItem
        }
    }

    fun navigateToBreadcrumb(path: String) {
        loadDirectory(path)
    }

    fun navigateUp(): Boolean {
        val current = _currentPath.value ?: "/"
        if (current == "/") return false

        val parent = current.substringBeforeLast('/', current)
        loadDirectory(parent.ifEmpty { "/" })
        return true
    }

    fun refresh() {
        // 清除缓存并重新加载
        directoryCache.clear()
        loadDirectory(_currentPath.value ?: "/")
    }

    fun clearCache() {
        directoryCache.clear()
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    fun clearError() {
        _error.value = null
    }

    // ==================== 文件操作 ====================

    fun createFile(fileName: String) {
        val current = _currentPath.value ?: "/"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    termuxRepository.createFile(current, fileName)
                }
                if (success) {
                    _operationResult.value = OperationResult(OperationType.CREATE, true, "File created")
                    refresh()
                } else {
                    _operationResult.value = OperationResult(OperationType.CREATE, false, "Failed to create file")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult(OperationType.CREATE, false, e.message ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createDirectory(dirName: String) {
        val current = _currentPath.value ?: "/"
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    termuxRepository.createDirectory(current, dirName)
                }
                if (success) {
                    _operationResult.value = OperationResult(OperationType.CREATE, true, "Directory created")
                    refresh()
                } else {
                    _operationResult.value = OperationResult(OperationType.CREATE, false, "Failed to create directory")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult(OperationType.CREATE, false, e.message ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    termuxRepository.deleteFile(fileItem.path, fileItem.isDirectory)
                }
                if (success) {
                    _operationResult.value = OperationResult(OperationType.DELETE, true, "Deleted successfully")
                    // 从缓存中移除
                    directoryCache.remove(fileItem.parentPath)
                    refresh()
                } else {
                    _operationResult.value = OperationResult(OperationType.DELETE, false, "Failed to delete")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult(OperationType.DELETE, false, e.message ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newPath = withContext(Dispatchers.IO) {
                    termuxRepository.renameFile(fileItem.path, newName)
                }
                if (newPath != null) {
                    _operationResult.value = OperationResult(OperationType.RENAME, true, "Renamed to $newName")
                    // 清除缓存
                    directoryCache.remove(fileItem.parentPath)
                    directoryCache.remove(fileItem.path)
                    refresh()
                } else {
                    _operationResult.value = OperationResult(OperationType.RENAME, false, "Failed to rename")
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult(OperationType.RENAME, false, e.message ?: "Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    private fun updateBreadcrumbs(path: String) {
        val segments = mutableListOf<String>()
        if (path == "/") {
            segments.add("/")
        } else {
            val parts = path.split("/").filter { it.isNotEmpty() }
            var accumulated = ""
            for (part in parts) {
                accumulated += "/$part"
                segments.add(accumulated)
            }
        }
        _breadcrumbs.value = segments
    }

    // 操作结果数据类
    data class OperationResult(
        val type: OperationType,
        val success: Boolean,
        val message: String
    )

    enum class OperationType {
        CREATE, DELETE, RENAME
    }
}
