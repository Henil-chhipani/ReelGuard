ReelGuard closes Instagram's share sheet before a reel leaves your phone. It
runs fully offline: no internet permission, no accounts, no analytics.

## Install

1. Download `ReelGuard-{{VERSION}}.apk` from the assets below. Do not install
   ReelGuard from anywhere else.
2. Optional: verify the download. Compare `SHA256SUMS.txt` with the output of:
   ```bash
   shasum -a 256 ReelGuard-{{VERSION}}.apk
   ```
3. Open the APK and install it. If Play Protect warns, tap **Install anyway**.
4. Open ReelGuard, tap **Enable ReelGuard**, and turn on
   **ReelGuard share blocker** in Accessibility.
5. Open Instagram and tap Share on a reel. The sheet should close and the
   counter should increase.

On Android 13 and newer, if Android will not let you enable ReelGuard, open
**App info** → three-dot menu → **Allow restricted settings**, then try again.

## Requirements

- Android 8.0 or newer.
- Instagram, Instagram Lite, or Instagram Basel.

## What it does

- Closes the share sheet in supported Instagram apps and counts blocked shares.
- Tracks a share-free streak with your start date and best streak. The streak
  restarts when a share is blocked.
- Shows an optional "Share blocked" toast and follows your light or dark theme.

## Notes

- Updates install over your existing copy and keep your counters and streak.
  Uninstalling and reinstalling clears them.
- Counters, streak dates, and preferences stay on your device.
- ReelGuard is not on Google Play. This release page is the only supported
  download.

ReelGuard is unofficial and is not affiliated with, endorsed by, or sponsored
by Meta Platforms, Inc. Instagram is a trademark of Meta. Use at your own risk.
