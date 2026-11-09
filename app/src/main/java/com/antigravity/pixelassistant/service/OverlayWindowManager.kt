package com.antigravity.pixelassistant.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.antigravity.pixelassistant.R
import com.antigravity.pixelassistant.ai.ReplyCandidate

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Reply Assistant Overlay
    private var replyView: View? = null
    private var isReplyCollapsed = false

    // Netflix Subtitle Overlay
    private var subsView: View? = null
    private var subsParams: WindowManager.LayoutParams? = null

    fun showReplyAssistant(
        platform: String,
        partnerMessage: String,
        candidates: List<ReplyCandidate>,
        onSelectReply: (String) -> Unit,
        onDirectType: () -> Unit,
        onRegenerate: () -> Unit
    ) {
        if (replyView == null) {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.overlay_reply_assistant, null)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 120 // Position just above keyboard/navigation
            }

            replyView = view
            try {
                windowManager.addView(view, params)
            } catch (_: Exception) {
                return
            }
        }

        val v = replyView ?: return

        v.findViewById<TextView>(R.id.badgePlatform).text = platform
        v.findViewById<TextView>(R.id.textPartnerMessage).text = partnerMessage

        val optionBtns = listOf(
            v.findViewById<Button>(R.id.btnOption1),
            v.findViewById<Button>(R.id.btnOption2),
            v.findViewById<Button>(R.id.btnOption3),
            v.findViewById<Button>(R.id.btnOption4)
        )

        for (i in optionBtns.indices) {
            val btn = optionBtns[i]
            if (i < candidates.size) {
                val cand = candidates[i]
                btn.visibility = View.VISIBLE
                btn.text = "${cand.label}\n${cand.text}"
                btn.setOnClickListener {
                    onSelectReply(cand.text)
                }
            } else {
                btn.visibility = View.GONE
            }
        }

        // Direct typing button: collapses assistant and lets user type directly
        v.findViewById<View>(R.id.btnDirectType).setOnClickListener {
            toggleReplyCollapse(true)
            onDirectType()
        }

        v.findViewById<View>(R.id.btnRegenerate).setOnClickListener {
            onRegenerate()
        }

        v.findViewById<View>(R.id.btnMinimize).setOnClickListener {
            toggleReplyCollapse(!isReplyCollapsed)
        }
        v.findViewById<View>(R.id.headerLayout).setOnClickListener {
            toggleReplyCollapse(!isReplyCollapsed)
        }
    }

    private fun toggleReplyCollapse(collapse: Boolean) {
        val v = replyView ?: return
        isReplyCollapsed = collapse
        v.findViewById<View>(R.id.bodyLayout).visibility = if (collapse) View.GONE else View.VISIBLE
        v.findViewById<TextView>(R.id.btnMinimize).text = if (collapse) "▴" else "▾"
    }

    fun hideReplyAssistant() {
        replyView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            replyView = null
        }
    }

    fun showNetflixSubtitle(translatedText: String, originalText: String) {
        if (subsView == null) {
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.overlay_netflix_subtitles, null)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 280 // Placed comfortably above native subtitle position to prevent overlapping
            }

            subsParams = params
            subsView = view

            // Enable touch & drag to let user freely reposition the subtitle banner
            setupDragHandle(view, params)

            view.findViewById<View>(R.id.btnCloseSubs).setOnClickListener {
                hideNetflixSubtitle()
            }

            try {
                windowManager.addView(view, params)
            } catch (_: Exception) {
                return
            }
        }

        val v = subsView ?: return
        v.findViewById<TextView>(R.id.textTranslatedSubtitle).text = translatedText
        val origEl = v.findViewById<TextView>(R.id.textOriginalSubtitle)
        if (originalText.isNotBlank()) {
            origEl.visibility = View.VISIBLE
            origEl.text = originalText
        } else {
            origEl.visibility = View.GONE
        }
    }

    private fun setupDragHandle(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY - (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (_: Exception) {}
                    true
                }
                else -> false
            }
        }
    }

    fun hideNetflixSubtitle() {
        subsView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            subsView = null
        }
    }
}
