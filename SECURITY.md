# Security Policy

## Reporting a vulnerability

Use GitHub's private vulnerability reporting:
**Security** tab -> **Report a vulnerability**.

Do not open a public issue with exploit details, and never post signing keys,
keystore files, or passwords anywhere in this repository.

## Supported versions

Only the latest GitHub release is supported.

## Scope

ReelGuard runs an AccessibilityService, so these areas matter:

- Handling of screen text and window content.
- Unintended Back actions caused by false share-sheet detection.
- Local storage of counters and preferences.

The app has no internet permission, no analytics, and no accounts. It only reads
window text from Instagram packages and stores counters in app-private storage.

## Release integrity

Install APKs only from this repository's **Releases** page, and compare the file
against the published `SHA256SUMS.txt`. A repackaged APK with Accessibility
access could read anything on your screen.
