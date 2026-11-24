package com.reelguard.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.Locale

/**
 * Watches Instagram for its share and comment sheets and presses Back as soon
 * as one shows up, so a reel can never be sent and comments stay closed.
 * Accessibility services cannot consume taps inside another app, so closing
 * the sheet is the reliable equivalent.
 */
class ShareGuardService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var cooldownUntil = 0L
    private var commentCooldownUntil = 0L
    private var lastScan = 0L
    private var lastRetry = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = this
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

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val label = labelOf(event.source)
                if (isShareButton(label) || isCommentButton(label)) {
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
        val now = SystemClock.uptimeMillis()

        val root = rootInActiveWindow ?: return
        val rootPackage = root.packageName?.toString()
        if (rootPackage == null || rootPackage !in TARGETS) return

        if (now >= cooldownUntil && hasShareSheet(root)) {
            blockShare()
            return
        }

        if (!Prefs.commentBlockEnabled(this)) return
        if (now < commentCooldownUntil) return
        if (hasCommentsSheet(root)) {
            blockComments()
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

    private fun hasCommentsSheet(root: AccessibilityNodeInfo): Boolean = try {
        val hasTitle = findNodes(root, "Comments").any { node ->
            isCommentsTitleNode(node)
        }
        hasTitle && hasCommentsComposer(root)
    } catch (_: RuntimeException) {
        false
    }

    private fun hasCommentsComposer(root: AccessibilityNodeInfo): Boolean {
        val foundByText = COMMENT_COMPOSER_MARKERS.any { marker ->
            findNodes(root, marker).any { node ->
                matchesNode(node, SheetMatcher::isCommentsComposer)
            }
        }
        if (foundByText) return true
        return hasComposerHint(root)
    }

    private fun hasComposerHint(root: AccessibilityNodeInfo): Boolean {
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.addLast(root)
        var visited = 0
        while (pending.isNotEmpty() && visited < MAX_TREE_NODES) {
            val node = pending.removeLast()
            visited++
            val hint = node.hintText?.toString().orEmpty()
            val description = node.contentDescription?.toString().orEmpty()
            if (SheetMatcher.isCommentsComposer(hint) ||
                SheetMatcher.isCommentsComposer(description)
            ) {
                return true
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pending::addLast)
            }
        }
        return false
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

    private fun blockComments() {
        commentCooldownUntil = SystemClock.uptimeMillis() + COOLDOWN_MS
        if (performGlobalAction(GLOBAL_ACTION_BACK)) {
            Prefs.recordCommentBlock(this)
            if (Prefs.toastEnabled(this)) {
                Toast.makeText(this, R.string.comments_blocked_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        running = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        running = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var running: ShareGuardService? = null

        /** Turns the service off so UPI apps stop refusing payments. */
        fun requestPause(): Boolean {
            val service = running ?: return false
            service.disableSelf()
            return true
        }

        val TARGETS = setOf(
            "com.instagram.android",
            "com.instagram.basel",
            "com.instagram.lite"
        )

        const val COOLDOWN_MS = 1200L
        const val SCAN_THROTTLE_MS = 250L
        const val CLICK_FALLBACK_MS = 600L
        const val RETRY_DELAY_MS = 180L
        const val MAX_TREE_NODES = 800

        val COMMENT_COMPOSER_MARKERS = listOf(
            "Add a comment",
            "View all",
            "Be the first to comment",
            "Start the conversation"
        )

        fun hasText(root: AccessibilityNodeInfo, text: String): Boolean {
            val matches = root.findAccessibilityNodeInfosByText(text)
            return !matches.isNullOrEmpty()
        }

        fun findNodes(root: AccessibilityNodeInfo, text: String): List<AccessibilityNodeInfo> =
            root.findAccessibilityNodeInfosByText(text).orEmpty()

        fun matchesNode(node: AccessibilityNodeInfo, predicate: (String) -> Boolean): Boolean =
            predicate(node.text?.toString().orEmpty()) ||
                predicate(node.contentDescription?.toString().orEmpty()) ||
                predicate(node.hintText?.toString().orEmpty())

        fun isCommentsTitleNode(node: AccessibilityNodeInfo): Boolean {
            if (node.isClickable) return false
            val className = node.className?.toString().orEmpty()
            if (className.endsWith("Button") || className.endsWith("ImageView")) {
                return false
            }
            return SheetMatcher.isCommentsTitle(node.text?.toString().orEmpty())
        }

        fun isShareButton(label: String): Boolean {
            if (label.isEmpty()) return false
            val l = label.trim()
            return l == "share" || l.startsWith("share reel") || l.startsWith("share post")
        }

        fun isCommentButton(label: String): Boolean {
            if (label.isEmpty()) return false
            val l = label.trim()
            return l == "comment" || l == "comments"
        }

        fun labelOf(node: AccessibilityNodeInfo?): String {
            node ?: return ""
            val text = node.text?.toString().orEmpty()
            val description = node.contentDescription?.toString().orEmpty()
            return "$text $description".trim().lowercase(Locale.US)
        }
    }
}
