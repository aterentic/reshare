# Backlog

Future ideas and improvements - not currently planned.

## UI / Theming
- Migrate from Material 2 (AppCompat) to Material 3 — enables dynamic color, tonal palettes, dark mode support
- Design app icon — adaptive icon + monochrome layer for themed icons (API 33+) → [spec](specs/ui-redesign.md)
- Choose colour palette — pick seed color, generate M3 tonal scheme → [spec](specs/ui-redesign.md)
- Apply icon and colours to the app → [spec](specs/ui-redesign.md)
- Add dark mode — light + dark theme variants
- Migrate from XML layouts to Jetpack Compose (requires toolchain upgrade first)

## PDF Input Enhancements
- Image extraction from PDFs (pdftohtml `-i` flag currently ignores images)
- Scanned PDF detection and user warning (OCR-only PDFs produce empty output)

## Build
- Upgrade to AGP 10.0 when stable (track Android Studio Meerkat feature drops)

## Distribution
- First GitHub release: keystore setup, tag `v1.0`, signed APK → [keystore](specs/keystore-setup.md)
- F-Droid release
- Play Store release

## Platform
- iOS port
