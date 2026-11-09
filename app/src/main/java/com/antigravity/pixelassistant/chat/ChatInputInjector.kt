package com.antigravity.pixelassistant.chat

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object ChatInputInjector {

    /**
     * Injects the chosen reply text into the chat input field.
     * Respects user safety policy: By default, autoSend is FALSE (inserts draft only).
     * If user explicitly enables autoSend in settings, triggers the send button.
     */
    fun injectReply(
        inputNode: AccessibilityNodeInfo?,
        sendButtonNode: AccessibilityNodeInfo?,
        replyText: String,
        autoSend: Boolean
    ): Boolean {
        if (inputNode == null) return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replyText)
        }
        val setTextSuccess = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        if (setTextSuccess && autoSend && sendButtonNode != null) {
            // Trigger send only if autoSend is explicitly opted in by user
            sendButtonNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        return setTextSuccess
    }
}
