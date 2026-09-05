ShadowCalc V4 Build Instructions
=================================

GITHUB ACTIONS (Recommended)
----------------------------
1. Push this repo to GitHub
2. Go to Actions tab → "Build ShadowCalc V4 APK" → "Run workflow"
3. The workflow will automatically:
   - Set up JDK 17
   - Download Gradle wrapper
   - Build debug APK
   - Upload APK as artifact

LOCAL BUILD (Android Studio)
----------------------------
1. Download gradle-wrapper.jar:
   https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar
   Place in: gradle/wrapper/gradle-wrapper.jar

2. Open in Android Studio and sync

Project Config:
- compileSdk: 34
- minSdk: 24
- targetSdk: 34
- Gradle: 8.2
- Kotlin: 1.9.20
