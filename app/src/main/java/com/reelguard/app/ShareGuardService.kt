package com.reelguard.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.Locale

/**
 * Watches Instagram for its share sheet and presses Back as soon as the sheet
 * shows up, so a reel can never be sent. Accessibility services cannot consume
 * taps inside another app, so closing the sheet is the reliable equivalent.
 */
class ShareGuardService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var cooldownUntil = 0L
    private var lastScan = 0L
    private var lastRetry = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Prefs.ensureJourneyStarted(this)
        serviceInfo?.let { info ->
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 60
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString()
        if (packageName == null || packageName !in TARGETS) return

        val now = SystemClock.uptimeMillis()
        if (now < cooldownUntil) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (isShareButton(labelOf(event.source))) {
                    handler.postDelayed(::scanAndBlock, CLICK_FALLBACK_MS)
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (now - lastScan >= SCAN_THROTTLE_MS) {
                    lastScan = now
                    scanAndBlock()
                }
                scheduleRetry()
            }
        }
    }

    private fun scheduleRetry() {
        val now = SystemClock.uptimeMillis()
        if (now - lastRetry < SCAN_THROTTLE_MS) return
        lastRetry = now
        handler.postDelayed(::scanAndBlock, RETRY_DELAY_MS)
    }

    private fun scanAndBlock() {
        if (SystemClock.uptimeMillis() < cooldownUntil) return

        val root = rootInActiveWindow ?: return
        val rootPackage = root.packageName?.toString()
        if (rootPackage == null || rootPackage !in TARGETS) return

        if (hasShareSheet(root)) {
            blockShare()
        }
    }

    private fun hasShareSheet(root: AccessibilityNodeInfo): Boolean = try {
        when {
            hasText(root, "Send separately") -> true
            hasText(root, "Send to") -> hasText(root, "Add to story") ||
                hasText(root, "Copy link") ||
                hasText(root, "Share to")

            hasText(root, "Add to story") && hasText(root, "Copy link") -> true
            else -> hasText(root, "Copy link") &&
                (hasText(root, "Share to") || hasText(root, "Share via"))
        }
    } catch (_: RuntimeException) {
        false
    }

    private fun blockShare() {
        cooldownUntil = SystemClock.uptimeMillis() + COOLDOWN_MS
        if (performGlobalAction(GLOBAL_ACTION_BACK)) {
            Prefs.recordBlock(this)
            if (Prefs.toastEnabled(this)) {
                Toast.makeText(this, R.string.share_blocked_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        val TARGETS = setOf(
            "com.instagram.android",
            "com.instagram.basel",
            "com.instagram.lite"
        )

        const val COOLDOWN_MS = 1200L
        const val SCAN_THROTTLE_MS = 250L
        const val CLICK_FALLBACK_MS = 600L
        const val RETRY_DELAY_MS = 180L

        fun hasText(root: AccessibilityNodeInfo, text: String): Boolean {
            val matches = root.findAccessibilityNodeInfosByText(text)
            return !matches.isNullOrEmpty()
        }

        fun isShareButton(label: String): Boolean {
            if (label.isEmpty()) return false
            val l = label.trim()
            return l == "share" || l.startsWith("share reel") || l.startsWith("share post")
        }

        fun labelOf(node: AccessibilityNodeInfo?): String {
            node ?: return ""
            val text = node.text?.toString().orEmpty()
            val description = node.contentDescription?.toString().orEmpty()
            return "$text $description".trim().lowercase(Locale.US)
        }
    }
}
