# Contributing

Thanks for helping. Keep changes small, focused, and easy to review.

## Setup

- JDK 17
- Android SDK 36
- `./gradlew assembleDebug`

You can also open the project in Android Studio and run the `app` configuration
on a device or emulator.

## Useful contributions

- New Instagram package names or share-sheet markers.
- Faster detection without background polling or extra battery cost.
- Clearer UI and accessibility wording.
- Documentation fixes.

## Detection changes

The detection logic lives in
`app/src/main/java/com/reelguard/app/ShareGuardService.java`:

- `TARGETS` lists the supported Instagram package names.
- `hasShareSheet()` decides whether a sheet is open. It requires combinations of
  markers so ordinary captions ("send this to...") do not trigger a Back press.

When changing markers:

1. Reproduce the sheet on a real device and note the exact visible text.
2. Add the smallest pair of markers that reliably identifies the sheet.
3. Verify that normal reels, captions, and the three-dot menu still work.
4. Confirm the share sheet closes and the counter increments.

## Pull requests

- Run `./gradlew lintDebug assembleDebug` before opening a PR.
- Do not add network permissions, analytics, or third-party dependencies without
  discussing them first.
- Never commit keystores, `keystore.properties`, APKs, or build outputs.
- Keep the UI on framework views; the app intentionally has no AndroidX or
  Compose dependency.

## Releases (maintainers)

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Make sure these repository secrets exist:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Tag and push:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. The release workflow builds the signed APK, generates `SHA256SUMS.txt`, and
   attaches both to the GitHub release.

Encode the keystore for the secret with:

```bash
openssl base64 -A -in keystore/reelguard-release.jks
```

Never share the keystore password or the encoded key in an issue or PR.
