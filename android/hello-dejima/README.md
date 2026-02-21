# Hello Dejima — Reference Android App

Reference app for the New Dejima / HackEurope Paris 2026 autonomous Android builder pipeline.

## Pinned Versions (A1-SPEC)

- compileSdk = 35, minSdk = 31, targetSdk = 35
- Kotlin = 2.0.21
- Compose BOM = 2024.12.01
- Android Gradle Plugin = 8.7.3
- Gradle = 8.11.1
- JVM target = 17

## Setup

### 1. Generate Gradle wrapper (if gradle-wrapper.jar is missing)

If you have Gradle installed:
```bash
gradle wrapper --gradle-version 8.11.1
```

Or download the wrapper JAR manually:
```powershell
# Windows
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar" -OutFile "gradle/wrapper/gradle-wrapper.jar" -UseBasicParsing
```

### 2. Build

```powershell
# Windows
.\gradlew.bat assembleDebug
```

```bash
# macOS/Linux
chmod +x gradlew
./gradlew assembleDebug
```

### 3. Install on emulator

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.dejima.hello/.MainActivity
```

## Environment paths

- **Reference app:** `%TEMP%\hello-dejima\` (Windows) or `/tmp/hello-dejima/` (macOS/Linux)
- **Workspace:** `%TEMP%\dejima-workspace\` (Windows) or `/tmp/dejima-workspace/` (macOS/Linux)

Copy this project to the temp path if needed for the agent pipeline.
