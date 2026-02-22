# A1 Spec — Autonomous Android Builder Reference

> **Purpose:** Single source of truth for A1 work. Reference this spec when working with any Cursor agent.
>
> **Role:** A1 owns Android environment, reference app, skill, plugin, fixes, and vision verification.

---

## 1. Project Context

- **Project:** New Dejima / HackEurope Paris 2026
- **Goal:** AI agent autonomously builds Android apps from natural language prompts
- **A1 scope:** Track A — Agent + Android Pipeline (Android side)
- **A2 scope:** OpenClaw config, exec approvals, agent testing
- **Timeline:** 21 hours total; Hour 9 checkpoint = first working app

---

## 2. Pinned Versions (DO NOT CHANGE)

| Component | Version |
|-----------|---------|
| compileSdk | 35 |
| minSdk | 31 |
| targetSdk | 35 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Android Gradle Plugin | 8.7.3 |
| Gradle wrapper | 8.11.1 |
| JVM target | 17 |
| Activity Compose | 1.9.3 |

**Stack:** Jetpack Compose only (no XML layouts), Material 3, Kotlin only (no Java).

---

## 3. File Locations

| Item | Path |
|------|------|
| Workspace (agent projects) | `/tmp/dejima-workspace/` (macOS/Linux) or `%TEMP%\dejima-workspace\` (Windows) |
| Reference app | `/tmp/hello-dejima/` (macOS/Linux) or `%TEMP%\hello-dejima\` (Windows) |
| Skill | `~/.openclaw/skills/android-app-builder/SKILL.md` |
| Plugin | `<openclaw-repo>/extensions/android-dev/` |
| OpenClaw config | `~/.openclaw/openclaw.json` (or `config.yaml` — check OpenClaw docs) |

---

## 4. Environment Setup

### Windows (PowerShell)

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\emulator;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"
```

### macOS/Linux

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

### Verify

```bash
adb --version          # expect 36.x
emulator -list-avds    # list AVDs
echo $ANDROID_HOME     # (or $env:ANDROID_HOME on Windows)
```

### Create AVD

```bash
avdmanager create avd -n "test_device" -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_6"
```

### Boot emulator

```bash
emulator -avd test_device -no-window -no-audio &
# wait ~45s
adb wait-for-device
adb shell getprop sys.boot_completed   # expect "1"
```

---

## 5. Reference App Structure

```
hello-dejima/
├── app/
│   ├── src/main/
│   │   ├── java/com/dejima/hello/
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── values/themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
    ├── gradle-wrapper.properties
    └── gradle-wrapper.jar
```

---

## 6. Reference App File Contents

### gradle-wrapper.properties

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "hello-dejima"
include(":app")
```

### build.gradle.kts (project-level)

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

### app/build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dejima.hello"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dejima.hello"
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

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="Hello Dejima"
        android:theme="@style/Theme.HelloDejima">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### res/values/themes.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.HelloDejima" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

### res/values/strings.xml

```xml
<resources>
    <string name="app_name">Hello Dejima</string>
</resources>
```

### MainActivity.kt

```kotlin
package com.dejima.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hello from Dejima!", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        var count by remember { mutableIntStateOf(0) }
                        Button(onClick = { count++ }) {
                            Text("Clicked $count times")
                        }
                    }
                }
            }
        }
    }
}
```

---

## 7. Build & Test Commands

### Build APK

```bash
cd <project-dir>
./gradlew assembleDebug
# Windows: .\gradlew.bat assembleDebug
```

**Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Install on emulator

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
```

### Launch app

```bash
adb -s emulator-5554 shell am start -n com.dejima.hello/.MainActivity
```

### Screenshot

```bash
adb -s emulator-5554 exec-out screencap -p > /tmp/screen.png
```

### Check for crashes

```bash
adb -s emulator-5554 logcat -d -t 30 *:E | grep -E "(FATAL|ANR|crash|Exception)"
```

### Tap at coordinates

```bash
adb -s emulator-5554 shell input tap <x> <y>
```

### Press back

```bash
adb -s emulator-5554 shell input keyevent KEYCODE_BACK
```

---

## 8. 7-Phase Pipeline (Skill Content)

The agent must follow these phases in order. NEVER skip. NEVER proceed past a HARD GATE without success.

| Phase | Name | Key actions |
|-------|------|-------------|
| 1 | Scaffolding | Create dir structure, gradle files, manifest, wrapper |
| 2 | Code Generation | Write all Kotlin, resources, manifest. Re-read to verify. |
| 3 | Compilation [HARD GATE] | `./gradlew assembleDebug`. Retry up to 10x on failure. |
| 4 | APK Verification | Check exists, size > 100KB |
| 5 | Emulator Testing | Install, launch, logcat crash check, screenshot, vision analysis |
| 6 | UI Smoke Testing | Tap each element, check navigation, rotation, home+return |
| 7 | Report | Summary: APK path, compilation attempts, errors fixed, screenshots |

---

## 9. Skill Requirements

**File:** `~/.openclaw/skills/android-app-builder/SKILL.md`

- YAML frontmatter: `name`, `description`
- Pinned versions (section 2 above)
- Full 7-phase templates (from implementation plan)
- Phase 5: MUST use image tool after screenshot — do not skip
- Workspace: `/tmp/dejima-workspace/` (or Windows equivalent)
- Emulator start: `emulator -avd test_device -no-window -no-audio &`
- Package: `com.dejima.<appname>`

---

## 10. Plugin Requirements

**Directory:** `<openclaw-repo>/extensions/android-dev/`

**Files:**
- `openclaw.plugin.json` — manifest with `id: "android-dev"`
- `package.json` — `@openclaw/android-dev`, extensions entry
- `index.ts` — entry point (MVP: can be stub that logs registration)

**Note:** MVP relies on skill + exec tool. Plugin tools (android_build, android_test, android_logcat) are stretch.

---

## 11. Exec Approvals (A2, but A1 should know)

Pre-approve so agent doesn't block:
- `./gradlew *`, `gradle *`
- `adb *`, `emulator *`, `avdmanager *`
- `ls *`, `mkdir *`, `chmod *`, `cat *`, `curl *`

---

## 12. Common Failure Modes & Fixes

| Issue | Fix |
|-------|-----|
| Gradle wrapper missing | Copy from hello-dejima/gradle/ or add download step in skill |
| Wrong package name | Manifest namespace + package must match directory structure |
| Compose/BOM mismatch | Enforce exact BOM 2024.12.01 |
| Emulator not found | `adb devices` before tests; start emulator if needed |
| gradlew not executable | `chmod +x gradlew` in skill |
| `mutableIntStateOf` not found | Kotlin 2.0.21+; use `mutableStateOf` if older |
| Agent skips vision | Add explicit "MUST use image tool" in Phase 5 |

---

## 13. Test Prompts (for A2 to run)

1. **First app (simple):** "Build me a simple tip calculator app. Input field for bill amount, slider for tip percentage (10–30%), show calculated tip and total."
2. **Second app:** "Build me a workout timer app. Preset timers (30s, 60s, 90s), start/pause button, countdown with circular progress."
3. **Third app:** "Build me a mood tracker app. Select mood (Happy, Sad, Neutral, Excited, Tired) with emoji buttons, save with timestamp, show scrollable history."

---

## 14. Checklist Before Starting

- [ ] `java --version` >= 17
- [ ] `node --version` >= 22
- [ ] `adb --version` 36.x
- [ ] `emulator -list-avds` shows test_device
- [ ] ANDROID_HOME set
- [ ] OpenClaw cloned and built
- [ ] ANTHROPIC_API_KEY, GOOGLE_AI_API_KEY set
- [ ] Manual hello-dejima build succeeds

---

## 15. Related Docs

- Full implementation plan: `docs/plans/2026-02-21-hackeurope-implementation-plan.md`
- Design: `docs/plans/2026-02-21-hackeurope-new-dejima-design.md`
- Original Android design: `docs/plans/2026-02-15-android-autonomous-dev-design.md`
