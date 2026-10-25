# ReelGuard

Small Android app that stops you from sharing Instagram reels. When Instagram's
share sheet appears, ReelGuard detects it and immediately presses Back, so the
reel is never sent. It also counts how many shares you dodged.

Why not just block the button? Android does not let one app eat taps inside
another app. Reacting the instant the sheet opens is the reliable way to break
the habit without breaking scrolling or watching reels.

## Install

1. Build the APK (or use `app/build/outputs/apk/debug/app-debug.apk`):
   ```
   ./gradlew assembleDebug
   ```
2. Install it:
   ```
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Or copy the APK to the phone and open it.
3. Open ReelGuard and tap **Enable ReelGuard**.
4. In Accessibility, find **ReelGuard share blocker** and turn it on.

On Android 13+ a sideloaded app may be blocked from Accessibility. Fix:
**App info -> three-dot menu -> Allow restricted settings**, then try again.

## What it does and does not do

- Watches only `com.instagram.android` and `com.instagram.lite`.
- Detects share-sheet text such as "Send to", "Add to story", "Copy link".
- Presses global Back once per detected sheet (1.2 s cooldown).
- Keeps a local daily/all-time counter and an optional "Share blocked" toast.
- No internet permission, no accounts, nothing leaves the device.

Caveats:

- Instagram UI changes can break detection; update the marker strings in
  `ShareGuardService.java` if a new version renames things.
- Tapping Share may briefly flash the sheet before it closes.
- Not Play Store friendly: Google restricts AccessibilityService use for
  blocking other apps. Sideload and personal use only.
- It blocks honest shares too (posts, stories, profile), not just reels.

## Project layout

- `ShareGuardService.java` - accessibility service and share-sheet detection
- `MainActivity.java` - status, counter, toast toggle, Accessibility shortcut
- `Prefs.java` - local counters and settings
