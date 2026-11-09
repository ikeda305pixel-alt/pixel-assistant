package com.antigravity.pixelassistant.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.antigravity.pixelassistant.ai.FreeTierQuotaManager
import com.antigravity.pixelassistant.ai.GeminiApiClient
import com.antigravity.pixelassistant.chat.ChatInputInjector
import com.antigravity.pixelassistant.chat.ChatPlatformDetector
import com.antigravity.pixelassistant.netflix.NetflixAdAudioController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AssistantAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var prefs: SharedPreferences
    private lateinit var quotaManager: FreeTierQuotaManager
    private lateinit var geminiClient: GeminiApiClient
    private lateinit var overlayManager: OverlayWindowManager
    private lateinit var netflixAdController: NetflixAdAudioController

    private var lastHandledPartnerMessage = ""
    private var lastHandledSubtitleText = ""

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("pixel_assistant_prefs", Context.MODE_PRIVATE)
        quotaManager = FreeTierQuotaManager(this)
        overlayManager = OverlayWindowManager(this)
        netflixAdController = NetflixAdAudioController(this)

        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        geminiClient = GeminiApiClient(apiKey, quotaManager)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: ""
        val rootNode = rootInActiveWindow ?: return

        when (packageName) {
            ChatPlatformDetector.PKG_LINE, ChatPlatformDetector.PKG_BUMBLE -> {
                handleChatApp(rootNode, packageName)
            }
            "com.netflix.mediaclient" -> {
                handleNetflixApp(rootNode)
            }
            else -> {
                // When switching away from supported apps, hide overlays
                overlayManager.hideReplyAssistant()
                overlayManager.hideNetflixSubtitle()
            }
        }
    }

    private fun handleChatApp(rootNode: AccessibilityNodeInfo, packageName: String) {
        val result = ChatPlatformDetector.detect(rootNode, packageName)
        if (result == null) {
            overlayManager.hideReplyAssistant()
            return
        }

        if (result.partnerMessage == lastHandledPartnerMessage) {
            return // Already showing or handled for this message
        }
        lastHandledPartnerMessage = result.partnerMessage

        serviceScope.launch {
            val candidates = geminiClient.generateReplies(result.platform, result.partnerMessage)
            overlayManager.showReplyAssistant(
                platform = result.platform,
                partnerMessage = result.partnerMessage,
                candidates = candidates,
                onSelectReply = { selectedText ->
                    val autoSend = prefs.getBoolean("auto_send", false) // Default: FALSE (safe draft)
                    ChatInputInjector.injectReply(
                        inputNode = result.inputNode,
                        sendButtonNode = result.sendButtonNode,
                        replyText = selectedText,
                        autoSend = autoSend
                    )
                },
                onDirectType = {
                    result.inputNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                },
                onRegenerate = {
                    serviceScope.launch {
                        val newCandidates = geminiClient.generateReplies(result.platform, result.partnerMessage)
                        overlayManager.showReplyAssistant(
                            result.platform, result.partnerMessage, newCandidates,
                            onSelectReply = { selectedText ->
                                val autoSend = prefs.getBoolean("auto_send", false)
                                ChatInputInjector.injectReply(result.inputNode, result.sendButtonNode, selectedText, autoSend)
                            },
                            onDirectType = { result.inputNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS) },
                            onRegenerate = {}
                        )
                    }
                }
            )
        }
    }

    private fun handleNetflixApp(rootNode: AccessibilityNodeInfo) {
        val enableAdMute = prefs.getBoolean("netflix_ad_mute", true)
        val enableSubs = prefs.getBoolean("netflix_subs", true)

        if (enableAdMute) {
            val adActive = netflixAdController.inspectAndHandleAd(rootNode)
            if (adActive) {
                overlayManager.hideNetflixSubtitle()
                return
            }
        }

        if (enableSubs) {
            val englishSub = extractNetflixSubtitleText(rootNode)
            if (englishSub.isNotBlank() && englishSub != lastHandledSubtitleText) {
                lastHandledSubtitleText = englishSub
                serviceScope.launch {
                    val translated = geminiClient.translateSubtitle(englishSub)
                    overlayManager.showNetflixSubtitle(translated, englishSub)
                }
            }
        }
    }

    private fun extractNetflixSubtitleText(root: AccessibilityNodeInfo): String {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val id = (node.viewIdResourceName ?: "").lowercase()
            val text = (node.text?.toString() ?: "").trim()
            if (id.contains("subtitle") || id.contains("timedtext") || id.contains("closedcaption")) {
                if (text.isNotBlank() && !text.contains("広告")) {
                    return text
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return ""
    }

    override fun onInterrupt() {
        overlayManager.hideReplyAssistant()
        overlayManager.hideNetflixSubtitle()
        netflixAdController.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideReplyAssistant()
        overlayManager.hideNetflixSubtitle()
        netflixAdController.release()
    }
}
