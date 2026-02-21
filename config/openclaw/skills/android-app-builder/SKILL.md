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
- ALWAYS use Jetpack Compose (NEVER XML layouts)
- ALWAYS use Material 3
- ALWAYS use Kotlin (NEVER Java)

## Environment

```
JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
ANDROID_HOME=/Users/administrator/Library/Android/sdk
PATH includes: $JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin
```

**CRITICAL**: Before running ANY gradle command, you MUST set JAVA_HOME:
```bash
export JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
```
The system default JDK is v25 which is INCOMPATIBLE with Kotlin 2.0.21. Builds WILL fail without this.

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
└── gradle/wrapper/gradle-wrapper.properties
```

The workspace directory is: /tmp/dejima-workspace/
Create app directory inside there.

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

### settings.gradle.kts:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "<app-name>"
include(":app")
```

### gradle.properties:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.java.home=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
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

Always generate the Gradle wrapper by copying it from /tmp/dejima-workspace/gradle-template/ if available,
or downloading it with: `gradle wrapper --gradle-version 8.11.1`

## Phase 2: Code Generation

- Write ALL Kotlin source files for the app
- Write ALL resource files (strings, themes, colors if needed)
- Write AndroidManifest.xml with correct package name, permissions, and activities
- Re-read every file after writing to verify no syntax errors
- DO NOT proceed to Phase 3 until all files are written and re-read

## Phase 3: Compilation [HARD GATE]

Run:
```bash
cd <project-dir> && ./gradlew assembleDebug 2>&1
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
ls -la app/build/outputs/apk/debug/*.apk
```

Check:
- APK file exists
- File size > 100KB (very simple apps can be small)

## Phase 5: Emulator Testing

```bash
# Check emulator is running
adb devices | grep emulator

# Install APK
adb -s emulator-5554 install -r app/build/outputs/apk/debug/*.apk

# Launch app
adb -s emulator-5554 shell am start -n <package>/.MainActivity

# Wait for launch
sleep 5

# Check for crashes
adb -s emulator-5554 logcat -d -t 30 *:E | grep -E "(FATAL|ANR|crash|Exception|Process.*died)"

# Take screenshot
adb -s emulator-5554 exec-out screencap -p > /tmp/screen_launch.png
```

After taking the screenshot, you MUST use the image analysis tool to examine it. Do NOT just take the screenshot and move on.

Use the image tool with this prompt: "Does this app look like it launched correctly? Describe what you see on screen. Is there any error dialog, blank screen, or crash? List all visible UI elements."

If crash detected: read logcat, fix the code, rebuild (go back to Phase 3).

### Physical Device Testing (Samsung A16)
If the emulator test passes, also install on the physical Samsung A16:
```bash
adb -s RFGYC34581H install -r app/build/outputs/apk/debug/*.apk
adb -s RFGYC34581H shell am start -n <package>/.MainActivity
sleep 5
adb -s RFGYC34581H exec-out screencap -p > /tmp/samsung_screen.png
```
Analyze the Samsung screenshot with the image tool too.

## Phase 6: UI Smoke Testing

**Step 1 — Screenshot analysis (MANDATORY — use image tool):**
Take a screenshot and use the image analysis tool with this prompt: "List every clickable element visible on this Android screen. For each, describe what it is and estimate its x,y tap coordinates (assuming 1080x2400 resolution for emulator, 1080x2340 for Samsung A16)."

**Step 2 — Tap each element:**
For each clickable element:
```bash
adb -s emulator-5554 shell input tap <x> <y>
sleep 2
adb -s emulator-5554 logcat -d -t 5 *:E | grep FATAL
adb -s emulator-5554 exec-out screencap -p > /tmp/tap_result.png
```
Analyze screenshot: "Did the app crash? Did anything change? Is there a new screen?"
Then press back:
```bash
adb -s emulator-5554 shell input keyevent KEYCODE_BACK
sleep 1
```

**Step 3 — Scrolling test:**
If the app has scrollable content:
```bash
# Scroll down
adb -s emulator-5554 shell input swipe 540 1800 540 600 500
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/scroll_down.png
# Scroll up
adb -s emulator-5554 shell input swipe 540 600 540 1800 500
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/scroll_up.png
```
Analyze both screenshots with the image tool: "Did the content scroll? Are new elements visible? Any rendering issues?"

**Step 4 — Edge cases:**
```bash
# Rotation test
adb -s emulator-5554 shell settings put system accelerometer_rotation 0
adb -s emulator-5554 shell settings put system user_rotation 1
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/rotation.png
# Reset rotation
adb -s emulator-5554 shell settings put system user_rotation 0

# Home + return test
adb -s emulator-5554 shell input keyevent KEYCODE_HOME
sleep 2
adb -s emulator-5554 shell am start -n <package>/.MainActivity
sleep 3
adb -s emulator-5554 exec-out screencap -p > /tmp/resume.png
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
6. The workspace is /tmp/dejima-workspace/ — create all projects there
