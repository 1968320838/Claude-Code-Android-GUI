package com.claudebox.ui.terminal

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.WebSettings
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.claudebox.R
import com.claudebox.data.ssh.ConnectionState
import com.claudebox.databinding.FragmentTerminalBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TerminalFragment : Fragment(), TerminalJavaScriptInterface.TerminalDataListener {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TerminalViewModel by viewModels()

    private val jsInterface = TerminalJavaScriptInterface()
    private var webViewClient: TerminalWebViewClient? = null

    // For keyboard resize detection
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var lastVisibleHeight: Int = 0
    private var isKeyboardVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWebView()
        setupObservers()
        setupLayoutResizeListener()

        jsInterface.listener = this
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webViewClient = TerminalWebViewClient().apply {
            listener = object : TerminalWebViewClient.TerminalWebViewClientListener {
                override fun onPageFinished(view: android.webkit.WebView?) {
                    binding.loadingOverlay.isVisible = false
                    viewModel.setTerminalReady(true)
                }
            }
        }

        binding.terminalWebview.apply {
            webViewClient = webViewClient!!

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
            }

            // Add JavaScript interface
            addJavascriptInterface(jsInterface, "AndroidInterface")

            // Show loading
            binding.loadingOverlay.isVisible = true

            // Load terminal.html from assets
            loadUrl("file:///android_asset/terminal.html")
        }
    }

    private fun setupObservers() {
        viewModel.connectionState.observe(viewLifecycleOwner) { state ->
            updateConnectionIndicator(state)
            updateConnectionState(state)
        }

        viewModel.terminalOutput.observe(viewLifecycleOwner) { data ->
            jsInterface.writeToTerminal(binding.terminalWebview, "\"${escapeForJs(data)}\"")
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.isTerminalReady.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                binding.loadingOverlay.isVisible = false
            }
        }
    }

    /**
     * Setup layout listener for terminal resize (keyboard show/hide, rotation).
     */
    private fun setupLayoutResizeListener() {
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val visibleRect = Rect()
            binding.terminalWebview.getWindowVisibleDisplayFrame(visibleRect)
            val visibleHeight = visibleRect.height()

            if (lastVisibleHeight == 0) {
                lastVisibleHeight = visibleHeight
                return@OnGlobalLayoutListener
            }

            val heightDifference = lastVisibleHeight - visibleHeight
            val screenHeight = binding.terminalWebview.rootView.height

            // Detect keyboard (difference > 15% of screen height)
            val keyboardThreshold = screenHeight * 0.15

            if (heightDifference > keyboardThreshold) {
                // Keyboard shown
                if (!isKeyboardVisible) {
                    isKeyboardVisible = true
                    // Small delay to let keyboard fully appear
                    binding.terminalWebview.postDelayed({
                        jsInterface.fitTerminal(binding.terminalWebview)
                        notifyTerminalResize()
                    }, 100)
                }
            } else if (heightDifference < -keyboardThreshold) {
                // Keyboard hidden
                if (isKeyboardVisible) {
                    isKeyboardVisible = false
                    binding.terminalWebview.postDelayed({
                        jsInterface.fitTerminal(binding.terminalWebview)
                        notifyTerminalResize()
                    }, 100)
                }
            }

            lastVisibleHeight = visibleHeight
        }

        binding.terminalWebview.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    /**
     * Notify terminal about resize and inform ViewModel.
     */
    private fun notifyTerminalResize() {
        binding.terminalWebview.evaluateJavascript("getTerminalSize()") { result ->
            // Parse result like {cols:80, rows:24}
            val colsMatch = Regex("""cols[:\s]*(\d+)""").find(result)
            val rowsMatch = Regex("""rows[:\s]*(\d+)""").find(result)

            val cols = colsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 80
            val rows = rowsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 24

            viewModel.resizeTerminal(cols, rows)
        }
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

    private fun updateConnectionState(state: ConnectionState) {
        val stateStr = when (state) {
            is ConnectionState.Connected -> "connected"
            is ConnectionState.Connecting -> "connecting"
            is ConnectionState.Reconnecting -> "connecting"
            is ConnectionState.Disconnected -> "disconnected"
            is ConnectionState.Error -> "disconnected"
            else -> "disconnected"
        }
        jsInterface.onConnectionStateChanged(binding.terminalWebview, stateStr)
    }

    private fun escapeForJs(data: String): String {
        return data
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("'", "\\'")
    }

    // TerminalDataListener implementation

    override fun onTerminalData(data: String) {
        viewModel.sendInput(data)
    }

    override fun onTerminalResize(cols: Int, rows: Int) {
        viewModel.resizeTerminal(cols, rows)
    }

    override fun onTerminalReady() {
        viewModel.setTerminalReady(true)
    }

    override fun onTerminalSelection(text: String) {
        // Handle selection if needed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setTerminalReady(false)

        // Remove layout listener
        layoutListener?.let {
            binding.terminalWebview.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        layoutListener = null

        binding.terminalWebview.apply {
            clearHistory()
            clearCache(true)
            loadUrl("about:blank")
            removeJavascriptInterface("AndroidInterface")
        }
        _binding = null
    }
}
