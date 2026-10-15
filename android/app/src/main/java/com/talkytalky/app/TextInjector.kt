package com.talkytalky.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object TextInjector {
    /** Insert [text] at the caret (replacing any selection) of [node]. Falls back to clipboard paste. */
    fun inject(ctx: Context, node: AccessibilityNodeInfo, text: String): Boolean {
        node.refresh()
        if (!node.isEditable) return false
        val current = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
        val start = node.textSelectionStart.takeIf { it >= 0 }?.coerceAtMost(current.length) ?: current.length
        val end = node.textSelectionEnd.takeIf { it >= 0 }?.coerceAtMost(current.length) ?: start
        val lo = minOf(start, end); val hi = maxOf(start, end)
        val merged = current.substring(0, lo) + text + current.substring(hi)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, merged)
        }
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            // SET_TEXT resets the caret; put it back after the inserted text.
            val sel = Bundle().apply {
                val pos = lo + text.length
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, pos)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pos)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel)
            return true
        }
        // Some apps (webviews, custom editors) reject SET_TEXT.
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("talkytalky", text))
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }
}
