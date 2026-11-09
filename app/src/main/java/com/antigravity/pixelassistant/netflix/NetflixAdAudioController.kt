package com.antigravity.pixelassistant.netflix

import android.content.Context
import android.media.AudioManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects Netflix subtitles and ad breaks.
 * Subtitles: Detects on-screen English subtitles and provides coordinates to position
 * Japanese translation directly above/below the English text without overlapping.
 * Ads: Detects ad breaks and mutes the media audio stream, restoring it when the ad ends.
 */
class NetflixAdAudioController(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isMutedByAd = false
    private var originalVolume = -1

    fun inspectAndHandleAd(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        val isAdActive = detectAdPresence(rootNode)

        if (isAdActive && !isMutedByAd) {
            // Ad break started -> Mute audio
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            isMutedByAd = true
            return true
        } else if (!isAdActive && isMutedByAd) {
            // Ad break ended -> Restore user's previous volume
            if (originalVolume >= 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            }
            isMutedByAd = false
            return false
        }

        return isAdActive
    }

    private fun detectAdPresence(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = (node.text?.toString() ?: "").lowercase()
            val desc = (node.contentDescription?.toString() ?: "").lowercase()
            val id = (node.viewIdResourceName ?: "").lowercase()

            if (text.contains("広告") || text.contains("ad 1 of") || text.contains("ad 2 of") ||
                desc.contains("広告") || id.contains("ad_pod") || id.contains("ad-info")
            ) {
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    fun release() {
        if (isMutedByAd && originalVolume >= 0) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            isMutedByAd = false
        }
    }
}
