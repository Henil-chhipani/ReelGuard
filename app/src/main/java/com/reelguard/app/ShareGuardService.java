package com.reelguard.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Watches Instagram for its share sheet and presses Back as soon as the sheet
 * shows up, so a reel can never be sent. Accessibility services cannot consume
 * taps inside another app, so closing the sheet is the reliable equivalent.
 */
public class ShareGuardService extends AccessibilityService {

    private static final Set<String> TARGETS = new HashSet<>(Arrays.asList(
            "com.instagram.android",
            "com.instagram.basel",
            "com.instagram.lite"
    ));

    private static final long COOLDOWN_MS = 1200L;
    private static final long SCAN_THROTTLE_MS = 250L;
    private static final long CLICK_FALLBACK_MS = 600L;
    private static final long RETRY_DELAY_MS = 180L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long cooldownUntil = 0L;
    private long lastScan = 0L;
    private long lastRetry = 0L;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_VIEW_CLICKED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.notificationTimeout = 60;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            setServiceInfo(info);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null || !TARGETS.contains(packageName.toString())) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (now < cooldownUntil) {
            return;
        }

        int type = event.getEventType();
        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (isShareButton(labelOf(event.getSource()))) {
                handler.postDelayed(this::scanAndBlock, CLICK_FALLBACK_MS);
            }
            return;
        }

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (now - lastScan >= SCAN_THROTTLE_MS) {
                lastScan = now;
                scanAndBlock();
            }
            scheduleRetry();
        }
    }

    private void scheduleRetry() {
        long now = SystemClock.uptimeMillis();
        if (now - lastRetry < SCAN_THROTTLE_MS) {
            return;
        }
        lastRetry = now;
        handler.postDelayed(this::scanAndBlock, RETRY_DELAY_MS);
    }

    private void scanAndBlock() {
        if (SystemClock.uptimeMillis() < cooldownUntil) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }

        CharSequence rootPackage = root.getPackageName();
        if (rootPackage == null || !TARGETS.contains(rootPackage.toString())) {
            return;
        }

        if (hasShareSheet(root)) {
            blockShare();
        }
    }

    private boolean hasShareSheet(AccessibilityNodeInfo root) {
        try {
            if (hasText(root, "Send separately")) {
                return true;
            }

            if (hasText(root, "Send to")) {
                return hasText(root, "Add to story")
                        || hasText(root, "Copy link")
                        || hasText(root, "Share to");
            }

            if (hasText(root, "Add to story") && hasText(root, "Copy link")) {
                return true;
            }

            return hasText(root, "Copy link")
                    && (hasText(root, "Share to") || hasText(root, "Share via"));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean hasText(AccessibilityNodeInfo root, String text) {
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(text);
        return matches != null && !matches.isEmpty();
    }

    private void blockShare() {
        cooldownUntil = SystemClock.uptimeMillis() + COOLDOWN_MS;
        if (performGlobalAction(GLOBAL_ACTION_BACK)) {
            Prefs.recordBlock(this);
            if (Prefs.toastEnabled(this)) {
                Toast.makeText(this, "Share blocked", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static boolean isShareButton(String label) {
        if (label.isEmpty()) {
            return false;
        }
        String l = label.trim();
        return l.equals("share")
                || l.startsWith("share reel")
                || l.startsWith("share post");
    }

    private static String labelOf(AccessibilityNodeInfo node) {
        if (node == null) {
            return "";
        }
        String text = node.getText() != null ? node.getText().toString() : "";
        String description = node.getContentDescription() != null
                ? node.getContentDescription().toString()
                : "";
        return (text + " " + description).trim().toLowerCase(Locale.US);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
    }
}
