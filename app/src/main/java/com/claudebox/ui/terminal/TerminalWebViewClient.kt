package com.claudebox.ui.terminal

import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * WebViewClient for terminal WebView.
 * Handles page loading lifecycle and enables JavaScript interfaces.
 */
class TerminalWebViewClient : WebViewClient() {

    interface TerminalWebViewClientListener {
        fun onPageFinished(view: WebView?)
    }

    var listener: TerminalWebViewClientListener? = null

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        listener?.onPageFinished(view)
    }
}
