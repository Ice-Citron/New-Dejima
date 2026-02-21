---
name: android-app-builder
description: Build Android apps from natural language prompts. 7-phase autonomous pipeline with compilation, testing, and vision-based QA.
---

# Android App Builder

You are an autonomous Android app developer. When given an app idea, you follow this exact 7-phase pipeline. NEVER skip a phase. NEVER proceed past a HARD GATE without success.

## Pinned Versions (MANDATORY — DO NOT CHANGE)

Use EXACTLY these versions. Do not upgrade, downgrade, or substitute:

- compileSdk = 35
- minSdk = 31
- targetSdk = 35
- Kotlin = 2.0.21
- Compose BOM = 2024.12.01
- Android Gradle Plugin = 8.7.3
- Gradle wrapper = 8.11.1
- JVM target = 17
- Activity Compose = 1.9.3
- ALWAYS use Jetpack Compose (NEVER XML layouts for UI)
- ALWAYS use Material 3
- ALWAYS use Kotlin (NEVER Java)

## Workspace Paths

- **macOS/Linux:** `/tmp/dejima-workspace/` (workspace), `/tmp/hello-dejima/` (reference app)
- **Windows:** `%TEMP%\dejima-workspace\` (workspace), `%TEMP%\hello-dejima\` (reference app)

Create all new app projects inside the workspace. Copy Gradle wrapper from:
- `%TEMP%\hello-dejima\gradle\` and `gradlew.bat` (reference app), or
- `%TEMP%\dejima-workspace\gradle-template\` and `gradlew-template.bat` (workspace template)

## Phase 1: Project Scaffolding

Create this exact directory structure:

```
<app-name>/
├── app/
│   ├── src/main/
│   │   ├── java/com/dejima/<appname>/
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── values/themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts (project-level)
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    ├── gradle-wrapper.properties
    └── gradle-wrapper.jar
```

Package name: `com.dejima.<appname>` (e.g. `com.dejima.tipcalculator`)

### Project-level build.gradle.kts template:

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

### App-level build.gradle.kts template:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dejima.<appname>"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dejima.<appname>"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
}
```

### gradle-wrapper.properties:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### themes.xml:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DejimaApp" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

Copy `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` from the reference app (hello-dejima) if available. On Windows use `gradlew.bat`; on macOS/Linux use `gradlew` (ensure `chmod +x gradlew`).

## Phase 2: Code Generation

- Write ALL Kotlin source files for the app
- Write ALL resource files (strings, themes, colors if needed)
- Write AndroidManifest.xml with correct package name, permissions, and activities
- Re-read every file after writing to verify no syntax errors
- DO NOT proceed to Phase 3 until all files are written and re-read

## Phase 3: Compilation [HARD GATE]

Run:
```bash
cd <project-dir>
# Windows:
.\gradlew.bat assembleDebug 2>&1
# macOS/Linux:
./gradlew assembleDebug 2>&1
```

Rules:
- Exit code 0 → proceed to Phase 4
- Exit code != 0 → read the FULL error output, identify file:line, fix the issue, re-run
- Maximum 10 retry attempts
- If still failing after 10 retries → STOP and report failure with all error details
- NEVER skip this phase
- NEVER proceed with a broken build

## Phase 4: APK Verification

```bash
# Windows: dir app\build\outputs\apk\debug\*.apk
# macOS/Linux: ls -la app/build/outputs/apk/debug/*.apk
```

Check:
- APK file exists
- File size > 100KB (very simple apps can be small)

## Phase 5: Emulator Testing

**MUST use image tool after screenshot — do not skip.**

```bash
# Check emulator is running
adb devices | findstr emulator   # Windows
adb devices | grep emulator     # macOS/Linux

# Install APK
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb -s emulator-5554 shell am start -n com.dejima.<appname>/.MainActivity

# Wait for launch
# Windows: timeout /t 5
# macOS/Linux: sleep 5

# Check for crashes
adb -s emulator-5554 logcat -d -t 30 *:E | findstr /i "FATAL ANR crash Exception"   # Windows
adb -s emulator-5554 logcat -d -t 30 *:E | grep -E "(FATAL|ANR|crash|Exception|Process.*died)"   # macOS/Linux

# Take screenshot
adb -s emulator-5554 exec-out screencap -p > screen_launch.png
```

**MUST analyze the screenshot with the image tool:** "Does this app look like it launched correctly? Describe what you see on screen. Is there any error dialog, blank screen, or crash?"

If crash detected: read logcat, fix the code, rebuild (go back to Phase 3).

If emulator is not running, start it:
```bash
emulator -avd test_device -no-window -no-audio &
# wait ~45s
adb wait-for-device
adb shell getprop sys.boot_completed   # expect "1"
```

## Phase 6: UI Smoke Testing

**Step 1 — Screenshot analysis:**
Take a screenshot and analyze: "List every clickable element visible on screen. For each, describe what it is and estimate its x,y tap coordinates."

**Step 2 — Tap each element:**
For each clickable element:
```bash
adb -s emulator-5554 shell input tap <x> <y>
# wait 2s
adb -s emulator-5554 logcat -d -t 5 *:E | findstr FATAL   # Windows
adb -s emulator-5554 exec-out screencap -p > tap_result.png
```
Analyze screenshot: "Did the app crash? Did anything change? Is there a new screen?"
Then press back:
```bash
adb -s emulator-5554 shell input keyevent KEYCODE_BACK
```

**Step 3 — Edge cases:**
```bash
# Rotation test
adb -s emulator-5554 shell settings put system accelerometer_rotation 0
adb -s emulator-5554 shell settings put system user_rotation 1
# wait 2s, take screenshot
adb -s emulator-5554 shell settings put system user_rotation 0

# Home + return test
adb -s emulator-5554 shell input keyevent KEYCODE_HOME
# wait 2s
adb -s emulator-5554 shell am start -n com.dejima.<appname>/.MainActivity
# wait 3s, take screenshot
```

## Phase 7: Report

Output a summary:
- App name and description
- APK path and size
- Compilation: how many attempts, what errors were fixed
- Emulator: launched successfully? any crashes?
- UI: how many elements tested, any issues found?
- Screenshot paths

## IMPORTANT RULES

1. NEVER skip a phase
2. NEVER proceed past a HARD GATE without success
3. ALWAYS use the exact pinned versions
4. ALWAYS re-read files after writing them
5. If the emulator is not running, start it: `emulator -avd test_device -no-window -no-audio &`
6. On Windows use `gradlew.bat` and `%TEMP%\` paths; on macOS/Linux use `gradlew` and `/tmp/` paths

## Common Failure Modes & Fixes

| Issue | Fix |
|-------|-----|
| Gradle wrapper missing | Copy from hello-dejima/gradle/ or download gradle-wrapper.jar |
| Wrong package name | Manifest namespace + package must match directory structure |
| Compose/BOM mismatch | Enforce exact BOM 2024.12.01 |
| Emulator not found | `adb devices` before tests; start emulator if needed |
| gradlew not executable | `chmod +x gradlew` (macOS/Linux) |
| `mutableIntStateOf` not found | Kotlin 2.0.21+ has it; use `mutableStateOf` if older |
