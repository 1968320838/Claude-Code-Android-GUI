package com.claudebox.ui.chat

import android.content.Context
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.core.CorePlugin

/**
 * Factory for creating configured Markwon instances.
 *
 * Note: For MVP, basic Markdown rendering is provided.
 * Full syntax highlighting with Highlight.js can be added later.
 */
object MarkwonFactory {

    private var instance: Markwon? = null

    /**
     * Get the singleton Markwon instance.
     */
    fun getInstance(context: Context): Markwon {
        if (instance == null) {
            instance = createMarkwon(context)
        }
        return instance!!
    }

    private fun createMarkwon(context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .build()
    }

    /**
     * Render markdown text to Spanned.
     */
    fun toMarkdown(context: Context, markdown: String): Spanned {
        return getInstance(context).toMarkdown(markdown)
    }
}
