package com.claudebox.ui.terminal

import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * JavaScript interface for WebView to communicate with Android.
 * Exposes terminal operations to the JavaScript code in terminal.html.
 */
class TerminalJavaScriptInterface {

    interface TerminalDataListener {
        fun onTerminalData(data: String)
        fun onTerminalResize(cols: Int, rows: Int)
        fun onTerminalReady()
        fun onTerminalSelection(text: String)
    }

    var listener: TerminalDataListener? = null

    /**
     * Called from JavaScript when user types in terminal.
     * Sends data to SSH PTY input stream.
     */
    @JavascriptInterface
    fun onTerminalData(data: String) {
        listener?.onTerminalData(data)
    }

    /**
     * Called from JavaScript when terminal is resized.
     * Notifies SSH PTY about new dimensions.
     */
    @JavascriptInterface
    fun onTerminalResize(cols: Int, rows: Int) {
        listener?.onTerminalResize(cols, rows)
    }

    /**
     * Called from JavaScript when terminal is ready.
     */
    @JavascriptInterface
    fun onTerminalReady() {
        listener?.onTerminalReady()
    }

    /**
     * Called from JavaScript when user selects text.
     */
    @JavascriptInterface
    fun onTerminalSelection(text: String) {
        listener?.onTerminalSelection(text)
    }

    /**
     * Write data to terminal display.
     * Called from Android to send SSH PTY output to terminal.
     */
    fun writeToTerminal(webView: WebView, data: String) {
        webView.post {
            webView.evaluateJavascript("write($data)", null)
        }
    }

    /**
     * Write line to terminal display.
     */
    fun writelnToTerminal(webView: WebView, data: String) {
        webView.post {
            webView.evaluateJavascript("writeln($data)", null)
        }
    }

    /**
     * Resize terminal.
     */
    fun resizeTerminal(webView: WebView, cols: Int, rows: Int) {
        webView.post {
            webView.evaluateJavascript("resize($cols, $rows)", null)
        }
    }

    /**
     * Clear terminal.
     */
    fun clearTerminal(webView: WebView) {
        webView.post {
            webView.evaluateJavascript("clear()", null)
        }
    }

    /**
     * Fit terminal to container.
     */
    fun fitTerminal(webView: WebView) {
        webView.post {
            webView.evaluateJavascript("fit()", null)
        }
    }

    /**
     * Set terminal font size.
     */
    fun setFontSize(webView: WebView, size: Int) {
        webView.post {
            webView.evaluateJavascript("setFontSize($size)", null)
        }
    }

    /**
     * Notify connection state change.
     */
    fun onConnectionStateChanged(webView: WebView, state: String) {
        webView.post {
            webView.evaluateJavascript("onConnectionStateChanged('$state')", null)
        }
    }
}
