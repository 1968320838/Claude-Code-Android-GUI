package com.claudebox.ui.files

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.claudebox.R
import com.claudebox.databinding.FragmentFilePreviewBinding
import com.claudebox.domain.model.FileItem
import com.claudebox.domain.repository.TermuxRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FilePreviewDialogFragment : DialogFragment() {

    @Inject
    lateinit var termuxRepository: TermuxRepository

    private var _binding: FragmentFilePreviewBinding? = null
    private val binding get() = _binding!!

    private var fileItem: FileItem? = null

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_NAME = "name"
        private const val ARG_IS_DIRECTORY = "is_directory"
        private const val ARG_SIZE = "size"
        private const val MAX_PREVIEW_SIZE = 100 * 1024 // 100KB

        fun newInstance(fileItem: FileItem): FilePreviewDialogFragment {
            return FilePreviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PATH, fileItem.path)
                    putString(ARG_NAME, fileItem.name)
                    putBoolean(ARG_IS_DIRECTORY, fileItem.isDirectory)
                    putLong(ARG_SIZE, fileItem.size)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_ClaudeBox_FullScreenDialog)

        arguments?.let {
            fileItem = FileItem(
                it.getString(ARG_NAME, ""),
                it.getString(ARG_PATH, ""),
                it.getBoolean(ARG_IS_DIRECTORY, false),
                it.getLong(ARG_SIZE, 0),
                0
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }

        fileItem?.let { item ->
            binding.fileName.text = item.path

            if (item.isDirectory) {
                showUnsupported("Cannot preview directory")
                return
            }

            // Check file size
            if (item.size > MAX_PREVIEW_SIZE) {
                showUnsupported(getString(R.string.preview_too_large))
                return
            }

            // Determine file type and preview
            val extension = getFileExtension(item.name).lowercase()

            if (isImageFile(extension)) {
                showUnsupported("Image preview not yet implemented")
            } else {
                // Use WebView with syntax highlighting
                setupWebView()
                loadCodeContent(item.path, item.name)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webContent.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        binding.webContent.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.loading.visibility = View.GONE
            }
        }
    }

    private fun loadCodeContent(path: String, fileName: String) {
        showLoading()

        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                termuxRepository.readFile(path, MAX_PREVIEW_SIZE)
            }

            if (content != null) {
                showCodePreview(content, fileName)
            } else {
                showError()
            }
        }
    }

    private fun showCodePreview(code: String, fileName: String) {
        binding.loading.visibility = View.GONE
        binding.webContent.visibility = View.VISIBLE
        binding.imageContent.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE

        // Load preview HTML with code content
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    * { box-sizing: border-box; }
                    body {
                        margin: 0;
                        padding: 16px;
                        background-color: #1C1B1F;
                        color: #E6E1E5;
                        font-family: monospace;
                        font-size: 13px;
                        line-height: 1.5;
                    }
                    pre {
                        margin: 0;
                        padding: 12px;
                        background-color: #2D2D2D;
                        border-radius: 8px;
                        overflow-x: auto;
                    }
                    code { font-family: monospace; font-size: 13px; }
                    .hljs { background: transparent; color: #D4D4D4; }
                    .hljs-comment, .hljs-quote { color: #6A9955; }
                    .hljs-keyword, .hljs-selector-tag { color: #569CD6; }
                    .hljs-number, .hljs-string { color: #CE9178; }
                    .hljs-title, .hljs-section, .hljs-name { color: #DCDCAA; }
                    .hljs-attribute, .hljs-attr { color: #4EC9B0; }
                    .hljs-symbol, .hljs-bullet { color: #D16969; }
                    .hljs-built_in { color: #4EC9B0; }
                </style>
            </head>
            <body>
                <pre><code id="code" class="language-${getLanguage(fileName)}">${escapeHtml(code)}</code></pre>
                <script>
                    function escapeHtml(text) {
                        return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                    }
                    function getLanguage(filename) {
                        const ext = filename.split('.').pop().toLowerCase();
                        const map = {
                            'kt': 'kotlin', 'java': 'java', 'cpp': 'cpp', 'c': 'c',
                            'h': 'c', 'py': 'python', 'js': 'javascript', 'ts': 'typescript',
                            'html': 'html', 'xml': 'xml', 'css': 'css', 'json': 'json',
                            'gradle': 'groovy', 'sh': 'bash', 'bash': 'bash',
                            'yaml': 'yaml', 'yml': 'yaml', 'md': 'markdown'
                        };
                        return map[ext] || 'plaintext';
                    }
                </script>
                <script src="highlight.min.js"></script>
            </body>
            </html>
        """.trimIndent()

        // Load HTML directly
        binding.webContent.loadDataWithBaseURL(
            "file:///android_asset/highlight/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun getLanguage(filename: String): String {
        val ext = getFileExtension(filename).lowercase()
        val langMap = mapOf(
            "kt" to "kotlin",
            "java" to "java",
            "cpp" to "cpp",
            "c" to "c",
            "h" to "c",
            "py" to "python",
            "js" to "javascript",
            "ts" to "typescript",
            "html" to "html",
            "xml" to "xml",
            "css" to "css",
            "json" to "json",
            "gradle" to "groovy",
            "sh" to "bash",
            "bash" to "bash",
            "yaml" to "yaml",
            "yml" to "yaml",
            "toml" to "toml",
            "md" to "markdown",
            "txt" to "plaintext",
            "sql" to "sql",
            "go" to "go",
            "rs" to "rust",
            "rb" to "ruby",
            "php" to "php"
        )
        return langMap[ext] ?: "plaintext"
    }

    private fun showLoading() {
        binding.loading.visibility = View.VISIBLE
        binding.webContent.visibility = View.GONE
        binding.imageContent.visibility = View.GONE
        binding.errorContainer.visibility = View.GONE
    }

    private fun showUnsupported(message: String) {
        binding.loading.visibility = View.GONE
        binding.webContent.visibility = View.GONE
        binding.imageContent.visibility = View.GONE
        binding.errorContainer.visibility = View.VISIBLE
        binding.errorMessage.text = message
    }

    private fun showError() {
        showUnsupported(getString(R.string.preview_error))
    }

    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) fileName.substring(lastDot + 1) else ""
    }

    private fun isImageFile(extension: String): Boolean {
        return extension in listOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
    }

    override fun onDestroyView() {
        binding.webContent.stopLoading()
        super.onDestroyView()
        _binding = null
    }
}
