# ReelGuard

Small Android app that stops you from sharing Instagram reels. When Instagram's
share sheet appears, ReelGuard detects it and immediately presses Back, so the
reel is never sent. It also counts how many shares you dodged.

Why not just block the button? Android does not let one app eat taps inside
another app. Reacting the instant the sheet opens is the reliable way to break
the habit without breaking scrolling or watching reels.

## Features

- Closes Instagram's share sheet for `com.instagram.android`,
  `com.instagram.basel`, and `com.instagram.lite`.
- Local "blocked today" and all-time counters.
- Share-free streak with your start date and best streak.
- Optional "Share blocked" toast.
- Material 3 interface in Kotlin and Jetpack Compose, with light and dark themes.
- No internet permission, no analytics, no accounts.
- The release APK is about 1 MB.

## Install

1. Download `ReelGuard-X.Y.Z.apk` from the latest GitHub release, and compare
   it with the published `SHA256SUMS.txt`.
2. Open the APK and install it (tap **Install anyway** if Play Protect warns).
3. Open ReelGuard, tap **Enable ReelGuard**, and turn on
   **ReelGuard share blocker** in Accessibility.
4. Open Instagram and tap Share on a reel. The sheet should close and the
   counter should increase.

On Android 13+ a sideloaded app is blocked from Accessibility until you allow
it: **App info -> three-dot menu -> Allow restricted settings**, then try again.

> Install builds only from this repository's Releases. ReelGuard needs
> Accessibility access, so a repackaged APK could read anything on your screen.

## Build

You need JDK 17 and Android SDK 36.

```bash
./gradlew assembleDebug    # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease  # signed only if keystore.properties exists
./gradlew test lintDebug   # unit tests and lint
```

Release signing is optional locally. Create `keystore.properties` (gitignored):

```properties
storeFile=keystore/reelguard-release.jks
storePassword=...
keyAlias=reelguard
keyPassword=...
```

Back up the keystore and password. Future updates must be signed with the same
key, or Android refuses to install them over an existing version.

## Releasing

Push a tag and the release workflow builds the signed APK, generates
`SHA256SUMS.txt`, and attaches both to the GitHub release:

```bash
git tag v1.1.0
git push origin v1.1.0
```

Configure these repository secrets first:
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## How it works

`ShareGuardService` is an AccessibilityService. It watches Instagram windows
and looks for the share sheet's text ("Send to", "Add to story", "Copy link"),
then performs a global Back once. Detection requires a pair of markers, so
ordinary captions do not trigger it. A 1.2 s cooldown prevents back-spam.

The share-free streak starts the first time protection is enabled and restarts
whenever a share sheet is blocked, so it tracks how long you have gone without
trying to share. Best streak keeps your previous record after a restart. All of
it is stored locally.

Project layout:

- `MainActivity.kt` - Compose entry point, service status, Accessibility shortcut
- `ShareGuardService.kt` - accessibility service and share-sheet detection
- `Prefs.kt` - local counters, streak timestamps, and settings
- `StreakFormatter.kt` - formats the streak and start date
- `ui/MainScreen.kt` - Material 3 screen, dialogs, and status cards
- `ui/theme/Theme.kt` - light and dark color schemes and typography

## Limitations

- Instagram UI changes can break detection; update the marker strings in
  `ShareGuardService.kt` if a new version renames things.
- Tapping Share may briefly flash the sheet before it closes.
- Detection is react-and-close, not a true touch block.
- The streak only reflects share attempts ReelGuard can see; attempts made
  while protection is off are not recorded.
- It blocks honest shares too (posts, stories, profiles), not just reels.
- Google Play does not allow this use of AccessibilityService, so distribution
  is GitHub releases only.
- Secure Folder or cloned Instagram apps run in another user profile and are
  not covered.

## Privacy

No internet permission, no telemetry, no accounts. Only the counters, streak
timestamps, and preferences are stored locally in app-private storage.

## Contributing

Bug reports and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).
For security issues, use GitHub's private vulnerability reporting as described
in [SECURITY.md](SECURITY.md).

## License

MIT - see [LICENSE](LICENSE).

ReelGuard is unofficial and is not affiliated with, endorsed by, or sponsored
by Meta Platforms, Inc. Instagram is a trademark of Meta. Use at your own risk.
