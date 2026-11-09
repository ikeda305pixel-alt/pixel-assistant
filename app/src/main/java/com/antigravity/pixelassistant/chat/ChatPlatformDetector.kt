package com.antigravity.pixelassistant.chat

import android.view.accessibility.AccessibilityNodeInfo

data class ChatDetectionResult(
    val platform: String,
    val partnerMessage: String,
    val inputNode: AccessibilityNodeInfo?,
    val sendButtonNode: AccessibilityNodeInfo?
)

/**
 * Analyzes Accessibility Node hierarchy for LINE and Bumble apps.
 */
object ChatPlatformDetector {

    const val PKG_LINE = "jp.naver.line.android"
    const val PKG_BUMBLE = "com.bumble.app"

    fun detect(rootNode: AccessibilityNodeInfo?, packageName: String): ChatDetectionResult? {
        if (rootNode == null) return null

        val platform = when (packageName) {
            PKG_LINE -> "LINE"
            PKG_BUMBLE -> "Bumble"
            else -> return null
        }

        val inputNode = findInputEditText(rootNode) ?: return null
        val partnerMessage = findLatestPartnerMessage(rootNode, platform) ?: return null
        val sendButton = findSendButton(rootNode)

        return ChatDetectionResult(
            platform = platform,
            partnerMessage = partnerMessage,
            inputNode = inputNode,
            sendButtonNode = sendButton
        )
    }

    private fun findInputEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        var candidate: AccessibilityNodeInfo? = null

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val className = node.className?.toString() ?: ""
            if (className.contains("EditText", ignoreCase = true) && node.isEditable) {
                candidate = node
                break
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return candidate
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val desc = (node.contentDescription?.toString() ?: "").lowercase()
            val text = (node.text?.toString() ?: "").lowercase()
            val id = (node.viewIdResourceName ?: "").lowercase()

            if (node.isClickable && (
                desc.contains("送信") || desc.contains("send") ||
                text.contains("送信") || text.contains("send") ||
                id.contains("send") || id.contains("submit")
            )) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findLatestPartnerMessage(root: AccessibilityNodeInfo, platform: String): String? {
        val textNodes = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim() ?: ""
            if (text.length in 2..300) {
                // Filter out standard UI labels
                if (!isSystemUiLabel(text)) {
                    textNodes.add(text)
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        // Return latest chat message that looks like conversational text
        return textNodes.lastOrNull { !isInputPlaceholder(it) }
    }

    private fun isSystemUiLabel(text: String): Boolean {
        val t = text.lowercase()
        return t in listOf("line", "bumble", "送信", "send", "キャンセル", "cancel", "既読", "read", "通話")
    }

    private fun isInputPlaceholder(text: String): Boolean {
        return text.contains("メッセージを入力") || text.contains("Type a message") || text.contains("Enter a message")
    }
}
