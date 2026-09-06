ShadowCalc V5 Build Instructions
=================================

Requirements:
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Kotlin 1.9.20

Build Steps:
1. Open project in Android Studio
2. Sync Gradle
3. Build > Build Bundle(s) / APK(s) > Build APK(s)
4. APK will be at: app/build/outputs/apk/debug/app-debug.apk

Features V5:
- 100% normal calculator disguise (no PIN dots, no biometric button)
- First-time PIN setup dialog
- Auto-lock after inactivity (configurable 1-30 min)
- Background kill (app to background = vault locks)
- Unified "All Media" gallery with tabs
- Figma-style vault dashboard with storage meter
- Figma-style browser with quick access & recent sites
- Storage breakdown in Settings
- Decoy PIN support
- Recovery question
- 10 accent color themes
- AES-256-CBC encryption for all vault files
- PBKDF2 key derivation (100k iterations)
- FLAG_SECURE screenshot blocking
- Ad blocking in browser
- Video download detection & resolution picker
