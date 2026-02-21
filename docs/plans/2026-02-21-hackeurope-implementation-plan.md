# New Dejima MVP — HackEurope Paris Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an end-to-end system where an AI agent autonomously creates Android apps, tracks its economics via Paid.ai, and holds its own Solana wallet — in 21 hours with 3 people.

**Architecture:** OpenClaw gateway runs Claude Opus 4.6 as the agent brain with a custom `android-app-builder` skill and `android-dev` plugin. Paid.ai wraps all API calls for cost tracking. Each agent gets a Solana devnet wallet. Stripe test-mode virtual cards demonstrate payment rails. Crusoe inference API shows alternative compute.

**Tech Stack:** OpenClaw (TypeScript), Android SDK 36, Kotlin 2.2.21, Jetpack Compose, Solana web3.js, Paid.ai Node SDK, Stripe Node SDK, Crusoe API, Gemini 3 Pro (vision), Claude Opus 4.6 (coding)

**Team:**
- **A1** + **A2**: Track A — Agent + Android Pipeline
- **B**: Track B — Finance + Crypto Infrastructure

**Timeline:** 21 hours (1:45 PM Feb 21 → 11:00 AM Feb 22 CET)

---

## TRACK A: Agent + Android Pipeline (A1 + A2)

---

### Hour 1 (1:45 PM - 2:45 PM): Environment Setup

#### Task A1-1: Set up Android SDK environment variables

**Files:**
- Modify: `~/.zshrc`

**Step 1: Add Android SDK to PATH**

Add to `~/.zshrc`:
```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

**Step 2: Source and verify**

Run:
```bash
source ~/.zshrc
adb --version
emulator -list-avds
echo $ANDROID_HOME
```

Expected: ADB 36.x, emulator accessible, ANDROID_HOME set.

**Step 3: Create AVD**

Run:
```bash
avdmanager create avd -n "test_device" \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d "pixel_6"
```

Expected: AVD created. Verify with `emulator -list-avds` showing `test_device`.

**Step 4: Boot emulator and verify**

Run:
```bash
emulator -avd test_device -no-window -no-audio &
sleep 45
adb wait-for-device
adb shell getprop sys.boot_completed
```

Expected: Returns `1`. Leave emulator running.

---

#### Task A2-1: Build and start OpenClaw gateway

**Files:**
- Working directory: `/Users/administrator/Black Projects/Project Altiera/openclaw/`

**Step 1: Verify OpenClaw is built**

Run:
```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
ls dist/entry.js
```

If missing, build:
```bash
pnpm install
pnpm build
```

**Step 2: Set up API keys**

Create/verify `.env` file or export:
```bash
export ANTHROPIC_API_KEY="sk-ant-..."
export GOOGLE_AI_API_KEY="..."  # Gemini 3 Pro
```

**Step 3: Start gateway in dev mode**

Run:
```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
pnpm gateway:watch
```

Expected: Gateway starts on port 18789. Note the auth token from output.

**Step 4: Test basic agent interaction**

Open new terminal. Send a test message via the gateway API or CLI:
```bash
# Via CLI if available
node openclaw.mjs chat --message "Hello, confirm you are working"
```

Expected: Agent responds. If this fails, check logs and fix before proceeding.

---

### Hour 2 (2:45 PM - 3:45 PM): Manual Android App Build (Reference)

#### Task A1-2: Build a reference Android app manually

This validates the entire Android toolchain BEFORE the agent tries it.

**Files:**
- Create: `/tmp/hello-dejima/` (entire project directory)

**Step 1: Create project structure**

```bash
mkdir -p /tmp/hello-dejima/app/src/main/java/com/dejima/hello
mkdir -p /tmp/hello-dejima/app/src/main/res/values
mkdir -p /tmp/hello-dejima/app/src/main/res/mipmap-hdpi
mkdir -p /tmp/hello-dejima/gradle/wrapper
```

**Step 2: Write gradle-wrapper.properties**

Create `/tmp/hello-dejima/gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

**Step 3: Write settings.gradle.kts**

Create `/tmp/hello-dejima/settings.gradle.kts`:
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

**Step 4: Write project-level build.gradle.kts**

Create `/tmp/hello-dejima/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

**Step 5: Write app/build.gradle.kts**

Create `/tmp/hello-dejima/app/build.gradle.kts`:
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

**Step 6: Write AndroidManifest.xml**

Create `/tmp/hello-dejima/app/src/main/AndroidManifest.xml`:
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

**Step 7: Write themes.xml**

Create `/tmp/hello-dejima/app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.HelloDejima" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

**Step 8: Write strings.xml**

Create `/tmp/hello-dejima/app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Hello Dejima</string>
</resources>
```

**Step 9: Write MainActivity.kt**

Create `/tmp/hello-dejima/app/src/main/java/com/dejima/hello/MainActivity.kt`:
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

**Step 10: Generate Gradle wrapper**

Run:
```bash
cd /tmp/hello-dejima
gradle wrapper --gradle-version 8.11.1
```

If `gradle` not installed:
```bash
brew install gradle
gradle wrapper --gradle-version 8.11.1
```

OR download wrapper jar manually:
```bash
cd /tmp/hello-dejima
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  https://github.com/nicemak/GradleWrapper/raw/master/gradle-wrapper.jar
```

**Step 11: Build debug APK**

Run:
```bash
cd /tmp/hello-dejima
chmod +x gradlew
./gradlew assembleDebug 2>&1
```

Expected: `BUILD SUCCESSFUL`. APK at `app/build/outputs/apk/debug/app-debug.apk`.

**Step 12: Install and launch on emulator**

Run:
```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.dejima.hello/.MainActivity
sleep 3
adb -s emulator-5554 exec-out screencap -p > /tmp/hello_screen.png
```

Expected: App launches. Screenshot shows "Hello from Dejima!" text and a button.

**Step 13: Test tap and logcat**

Run:
```bash
adb -s emulator-5554 shell input tap 540 1200
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/hello_tap.png
adb -s emulator-5554 logcat -d -t 30 *:E | grep -E "(FATAL|ANR|crash|Exception)"
```

Expected: Button counter increments. No crashes in logcat.

**CHECKPOINT:** If manual build + emulator test works, the Android toolchain is validated. Record the exact Gradle/Kotlin/Compose versions that worked — these become the pinned versions for the agent.

---

#### Task A2-2: Configure OpenClaw for Android development

**Files:**
- Create: `~/.openclaw/openclaw.json`

**Step 1: Create OpenClaw config**

Create `~/.openclaw/openclaw.json`:
```json5
{
  // Primary coding model
  "models": {
    "primary": {
      "provider": "anthropic",
      "model": "claude-opus-4-6"
    },
    "image": {
      "provider": "google",
      "model": "gemini-3-pro"
    }
  },

  // Agent defaults
  "agents": {
    "defaults": {
      "model": "anthropic/claude-opus-4-6",
      "imageModel": "google/gemini-3-pro",
      "workspace": "/tmp/dejima-workspace"
    }
  },

  // Gateway config
  "gateway": {
    "port": 18789
  }
}
```

Note: The exact config schema depends on the OpenClaw version. Check the gateway startup logs for any config errors and adjust fields accordingly.

**Step 2: Verify config loads**

Restart gateway:
```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
pnpm gateway:watch
```

Check logs for config loading errors. Fix any schema mismatches.

---

### Hour 3 (3:45 PM - 4:45 PM): Write the Android Builder Skill

#### Task A1-3: Create the android-app-builder skill

**Files:**
- Create: `~/.openclaw/skills/android-app-builder/SKILL.md`

**Step 1: Create skill directory**

```bash
mkdir -p ~/.openclaw/skills/android-app-builder
```

**Step 2: Write SKILL.md**

Create `~/.openclaw/skills/android-app-builder/SKILL.md`:

```markdown
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

## Phase 1: Project Scaffolding

Create this exact directory structure:

\`\`\`
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
\`\`\`

The workspace directory is: /tmp/dejima-workspace/
Create app directory inside there.

### Project-level build.gradle.kts template:
\`\`\`kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
\`\`\`

### App-level build.gradle.kts template:
\`\`\`kotlin
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
\`\`\`

### gradle-wrapper.properties:
\`\`\`properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
\`\`\`

### themes.xml:
\`\`\`xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DejimaApp" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
\`\`\`

Always generate the Gradle wrapper by copying it from /tmp/hello-dejima/gradle/ if available,
or downloading it.

## Phase 2: Code Generation

- Write ALL Kotlin source files for the app
- Write ALL resource files (strings, themes, colors if needed)
- Write AndroidManifest.xml with correct package name, permissions, and activities
- Re-read every file after writing to verify no syntax errors
- DO NOT proceed to Phase 3 until all files are written and re-read

## Phase 3: Compilation [HARD GATE]

Run:
\`\`\`bash
cd <project-dir> && ./gradlew assembleDebug 2>&1
\`\`\`

Rules:
- Exit code 0 → proceed to Phase 4
- Exit code != 0 → read the FULL error output, identify file:line, fix the issue, re-run
- Maximum 10 retry attempts
- If still failing after 10 retries → STOP and report failure with all error details
- NEVER skip this phase
- NEVER proceed with a broken build

## Phase 4: APK Verification

\`\`\`bash
ls -la app/build/outputs/apk/debug/*.apk
\`\`\`

Check:
- APK file exists
- File size > 100KB (very simple apps can be small)

## Phase 5: Emulator Testing

\`\`\`bash
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
\`\`\`

Analyze the screenshot with the image tool: "Does this app look like it launched correctly? Describe what you see on screen. Is there any error dialog, blank screen, or crash?"

If crash detected: read logcat, fix the code, rebuild (go back to Phase 3).

## Phase 6: UI Smoke Testing

**Step 1 — Screenshot analysis:**
Take a screenshot and analyze: "List every clickable element visible on screen. For each, describe what it is and estimate its x,y tap coordinates."

**Step 2 — Tap each element:**
For each clickable element:
\`\`\`bash
adb -s emulator-5554 shell input tap <x> <y>
sleep 2
adb -s emulator-5554 logcat -d -t 5 *:E | grep FATAL
adb -s emulator-5554 exec-out screencap -p > /tmp/tap_result.png
\`\`\`
Analyze screenshot: "Did the app crash? Did anything change? Is there a new screen?"
Then press back:
\`\`\`bash
adb -s emulator-5554 shell input keyevent KEYCODE_BACK
sleep 1
\`\`\`

**Step 3 — Edge cases:**
\`\`\`bash
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
\`\`\`

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
```

**Step 3: Verify skill is loadable**

Restart OpenClaw gateway and check logs for skill discovery.

---

#### Task A2-3: Create exec-approvals configuration

**Files:**
- Create: `~/.openclaw/exec-approvals.json` (or equivalent config in openclaw.json)

**Step 1: Pre-approve Android build commands**

The exact mechanism depends on OpenClaw's exec approval system. Check the gateway source for how exec approvals work. The goal: auto-approve these commands so the agent doesn't block on human approval:

- `./gradlew *`
- `gradle *`
- `adb *`
- `emulator *`
- `avdmanager *`
- `ls *`
- `mkdir *`
- `chmod *`
- `cat *`
- `curl *`

Check `/Users/administrator/Black Projects/Project Altiera/openclaw/src/agents/tools/` for the exec tool's approval logic and configure accordingly.

---

### Hour 4-5 (4:45 PM - 6:45 PM): Plugin Development + Integration

#### Task A1-4: Create the android-dev plugin

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/openclaw/extensions/android-dev/openclaw.plugin.json`
- Create: `/Users/administrator/Black Projects/Project Altiera/openclaw/extensions/android-dev/package.json`
- Create: `/Users/administrator/Black Projects/Project Altiera/openclaw/extensions/android-dev/index.ts`

**Step 1: Create plugin directory**

```bash
mkdir -p "/Users/administrator/Black Projects/Project Altiera/openclaw/extensions/android-dev"
```

**Step 2: Write plugin manifest**

Create `openclaw.plugin.json`:
```json
{
  "id": "android-dev",
  "configSchema": {
    "type": "object",
    "additionalProperties": false,
    "properties": {}
  }
}
```

**Step 3: Write package.json**

Create `package.json`:
```json
{
  "name": "@openclaw/android-dev",
  "version": "0.1.0",
  "description": "Android development tools for autonomous app building",
  "type": "module",
  "devDependencies": {
    "openclaw": "workspace:*"
  },
  "openclaw": {
    "extensions": [
      "./index.ts"
    ]
  }
}
```

**Step 4: Write plugin entry point**

Create `index.ts`:
```typescript
import type { OpenClawPluginApi } from "openclaw/plugin-sdk";
import { emptyPluginConfigSchema } from "openclaw/plugin-sdk";

const plugin = {
  id: "android-dev",
  name: "Android Dev",
  description: "Autonomous Android app development tools",
  configSchema: emptyPluginConfigSchema(),
  register(api: OpenClawPluginApi) {
    // Register custom tools for Android development
    // The heavy lifting is done by the SKILL.md — this plugin
    // provides structured tool wrappers for build/test/logcat

    // For MVP, the skill + exec tool handles everything.
    // Plugin tools can be added incrementally as needed.
    console.log("[android-dev] Plugin registered");
  },
};

export default plugin;
```

Note: For the MVP, the SKILL.md + OpenClaw's built-in `exec` tool handles all Android operations. The plugin is scaffolded for future structured tool wrappers (android_build, android_test, android_logcat) but these are STRETCH GOALS. The skill approach is simpler and faster.

**Step 5: Register plugin in OpenClaw**

Run:
```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
pnpm install
pnpm build
```

Restart gateway and verify plugin loads in logs.

---

#### Task A2-4: Test the skill with a simple prompt

**Step 1: Create workspace directory**

```bash
mkdir -p /tmp/dejima-workspace
```

**Step 2: Copy Gradle wrapper for reuse**

```bash
cp -r /tmp/hello-dejima/gradle /tmp/dejima-workspace/gradle-template
cp /tmp/hello-dejima/gradlew /tmp/dejima-workspace/gradlew-template
```

**Step 3: Send first autonomous build request**

Through the OpenClaw interface, activate the android-app-builder skill and send:

> "Build me a simple tip calculator app. It should have an input field for the bill amount, a slider for tip percentage (10-30%), and show the calculated tip and total."

**Step 4: Observe and debug**

Watch the agent:
1. Does it follow Phase 1 (scaffolding)?
2. Does it use the pinned versions?
3. Does it attempt compilation (Phase 3)?
4. Does it self-correct on errors?
5. Does it reach the emulator test (Phase 5)?

Debug any issues. Common problems:
- Gradle wrapper not found → ensure `gradlew` is executable
- Emulator not detected → ensure `adb devices` shows emulator
- Wrong package name → check AndroidManifest matches directory structure
- Compose version conflicts → ensure BOM version is exact

---

### Hour 6-8 (6:45 PM - 9:45 PM): Iterate + Fix + Second App

#### Task A1-5: Fix issues from first autonomous build

Based on what broke in Task A2-4, fix:
- Skill instructions that were ambiguous
- Version pins that didn't work
- Missing exec approvals
- Emulator connection issues

Each fix: edit SKILL.md → restart gateway → retry the prompt.

#### Task A2-5: Second autonomous build — different app

Once the first app succeeds, test with a different prompt:

> "Build me a workout timer app. It should have preset timers (30s, 60s, 90s), a start/pause button, and show the countdown with a circular progress indicator."

This tests:
- Different UI patterns (timers, progress indicators)
- State management (timer logic)
- The agent's ability to handle more complex Compose UI

---

### Hour 9-10 (9:45 PM - 11:45 PM): Vision QA Integration

#### Task A1-6: Verify Gemini vision integration

**Step 1: Test vision tool manually**

Take a screenshot of the emulator:
```bash
adb -s emulator-5554 exec-out screencap -p > /tmp/test_vision.png
```

Send to OpenClaw agent:
> "Analyze this screenshot: /tmp/test_vision.png — List every visible UI element, describe the app, and identify any visual issues."

**Step 2: Verify the agent uses vision in Phase 5-6**

If the agent isn't using vision analysis during emulator testing, update the SKILL.md to be more explicit:

Add to Phase 5:
```
After taking the screenshot, you MUST use the image analysis tool to examine it.
Do NOT just take the screenshot and move on.
```

#### Task A2-6: Demo recording — full pipeline

**Step 1: Clear workspace**

```bash
rm -rf /tmp/dejima-workspace/*
```

**Step 2: Record the full pipeline**

Start screen recording (QuickTime or similar).

Send a fresh prompt to the agent:
> "Build me a mood tracker app. It should let the user select their mood (Happy, Sad, Neutral, Excited, Tired) with emoji buttons, save the selection with a timestamp, and show a scrollable history of past mood entries."

Let the agent run through all 7 phases uninterrupted. Record everything.

**Step 3: Capture demo artifacts**

Save:
- The APK
- All screenshots from each phase
- The terminal output showing the pipeline
- Any self-correction logs (compilation error → fix → retry)

---

### Hour 11-14 (11:45 PM - 3:45 AM): Integration with Track B + Polish

#### Task A1-7: Connect Paid.ai cost tracking to agent pipeline

Work with Person B to wrap the OpenClaw agent's API calls with Paid.ai telemetry. See Track B tasks for details.

#### Task A2-7: Test 3rd and 4th apps, build robustness

Run more prompts to stress-test:

> "Build a unit converter app. Support conversions between: km/miles, kg/lbs, celsius/fahrenheit. Use tabs or a dropdown to select conversion type."

> "Build a flashcard study app. Let users create cards with a question on front and answer on back. Tap to flip. Swipe right for 'know it', swipe left for 'still learning'."

Fix any new failure patterns that emerge.

---

### Hour 15-18 (3:45 AM - 6:45 AM): Stretch Goals

#### Task A1-8 (STRETCH): Samsung A16 physical device testing

If Samsung A16 is available with USB debugging enabled:

```bash
adb devices  # Should show Samsung serial
adb -s <serial> install -r /tmp/dejima-workspace/<app>/app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -n <package>/.MainActivity
adb -s <serial> exec-out screencap -p > /tmp/samsung_screen.png
```

#### Task A2-8 (STRETCH): Agent generates marketing voiceover

After an app is built, have the agent call ElevenLabs TTS to generate a promo voiceover:

```bash
# Install ElevenLabs SDK
npm install @elevenlabs/elevenlabs-js
```

Create a simple script the agent can call:
```typescript
// /tmp/dejima-workspace/tools/voiceover.ts
import { ElevenLabsClient } from "@elevenlabs/elevenlabs-js";
import { writeFileSync } from "fs";

const client = new ElevenLabsClient();
const audio = await client.textToSpeech.convert("JBFqnCBsd6RMkjVDRZzb", {
  text: process.argv[2],
  modelId: "eleven_flash_v2_5",
  outputFormat: "mp3_44100_128",
});
const chunks: Buffer[] = [];
for await (const chunk of audio) chunks.push(Buffer.from(chunk));
writeFileSync(process.argv[3] || "voiceover.mp3", Buffer.concat(chunks));
```

---

## TRACK B: Finance + Crypto Infrastructure (Person B)

---

### Hour 1-2 (1:45 PM - 3:45 PM): Solana Wallet System

#### Task B-1: Set up Solana devnet environment

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/package.json`
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/wallet.ts`

**Step 1: Initialize finance module**

```bash
mkdir -p "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src"
cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance"
npm init -y
npm install @solana/web3.js @solana/spl-token bs58 dotenv typescript tsx
npx tsc --init --target es2022 --module nodenext --moduleResolution nodenext --esModuleInterop true --outDir dist
```

**Step 2: Write wallet creation module**

Create `src/wallet.ts`:
```typescript
import { Connection, Keypair, LAMPORTS_PER_SOL, PublicKey } from "@solana/web3.js";
import * as bs58 from "bs58";

const DEVNET_URL = "https://api.devnet.solana.com";

export interface AgentWallet {
  publicKey: string;
  secretKeyBase58: string;
  createdAt: string;
  agentId: string;
}

export function createAgentWallet(agentId: string): AgentWallet {
  const keypair = Keypair.generate();
  return {
    publicKey: keypair.publicKey.toBase58(),
    secretKeyBase58: bs58.default.encode(keypair.secretKey),
    createdAt: new Date().toISOString(),
    agentId,
  };
}

export function loadWallet(secretKeyBase58: string): Keypair {
  return Keypair.fromSecretKey(bs58.default.decode(secretKeyBase58));
}

export async function getBalance(publicKey: string): Promise<number> {
  const connection = new Connection(DEVNET_URL, "confirmed");
  const balance = await connection.getBalance(new PublicKey(publicKey));
  return balance / LAMPORTS_PER_SOL;
}

export async function airdropDevnetSol(publicKey: string, amount: number = 2): Promise<string> {
  const connection = new Connection(DEVNET_URL, "confirmed");
  const sig = await connection.requestAirdrop(
    new PublicKey(publicKey),
    amount * LAMPORTS_PER_SOL
  );
  await connection.confirmTransaction(sig);
  return sig;
}
```

**Step 3: Test wallet creation**

Create `src/test-wallet.ts`:
```typescript
import { createAgentWallet, airdropDevnetSol, getBalance } from "./wallet.js";

async function main() {
  console.log("Creating agent wallet...");
  const wallet = createAgentWallet("agent-001");
  console.log("Wallet created:", wallet.publicKey);

  console.log("Airdropping 2 SOL...");
  const sig = await airdropDevnetSol(wallet.publicKey, 2);
  console.log("Airdrop tx:", sig);

  const balance = await getBalance(wallet.publicKey);
  console.log("Balance:", balance, "SOL");
}

main().catch(console.error);
```

Run:
```bash
npx tsx src/test-wallet.ts
```

Expected: Wallet created, 2 SOL airdropped, balance shows 2.0 SOL.

---

### Hour 3-5 (3:45 PM - 6:45 PM): Paid.ai Cost Tracking

#### Task B-2: Set up Paid.ai integration

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/cost-tracker.ts`

**Step 1: Install Paid.ai SDK**

```bash
cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance"
npm install @paid-ai/paid-node
```

**Step 2: Create Paid.ai account and get API key**

Go to `app.paid.ai`, create account, get API key. Store in `.env`:
```
PAID_AI_API_KEY=your_key_here
```

**Step 3: Write cost tracking module**

Create `src/cost-tracker.ts`:
```typescript
import { PaidClient } from "@paid-ai/paid-node";
import dotenv from "dotenv";
dotenv.config();

const paid = new PaidClient({ token: process.env.PAID_AI_API_KEY! });

// Initialize OpenTelemetry tracing for automatic cost capture
paid.initializeTracing();

export interface CostEvent {
  agentId: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd: number;
  timestamp: string;
}

export interface RevenueEvent {
  agentId: string;
  source: string; // "android_app", "saas", "trading"
  amountUsd: number;
  description: string;
  timestamp: string;
}

// Track a cost event (API call)
export async function trackCost(event: CostEvent) {
  try {
    await paid.usage.recordBulkV2({
      signals: [{
        eventName: "api_cost",
        externalCustomerId: event.agentId,
        externalProductId: "agent-compute",
        data: {
          model: event.model,
          inputTokens: event.inputTokens,
          outputTokens: event.outputTokens,
          costUsd: event.estimatedCostUsd,
        },
      }],
    });
    console.log(`[Paid.ai] Cost tracked: $${event.estimatedCostUsd.toFixed(4)} for ${event.model}`);
  } catch (err) {
    console.error("[Paid.ai] Cost tracking error:", err);
  }
}

// Track a revenue event (agent earned money)
export async function trackRevenue(event: RevenueEvent) {
  try {
    await paid.usage.recordBulkV2({
      signals: [{
        eventName: "revenue_earned",
        externalCustomerId: event.agentId,
        externalProductId: event.source,
        data: {
          amountUsd: event.amountUsd,
          description: event.description,
        },
      }],
    });
    console.log(`[Paid.ai] Revenue tracked: $${event.amountUsd.toFixed(2)} from ${event.source}`);
  } catch (err) {
    console.error("[Paid.ai] Revenue tracking error:", err);
  }
}

// Get agent profitability summary
export async function getAgentMargin(agentId: string) {
  try {
    const traces = await paid.traces.getOrganizationCosts();
    return traces;
  } catch (err) {
    console.error("[Paid.ai] Margin query error:", err);
    return null;
  }
}
```

Note: The exact Paid.ai SDK methods may differ. Check `node_modules/@paid-ai/paid-node` for the actual API surface after installation and adjust the code accordingly.

**Step 4: Test cost tracking**

Create `src/test-paid.ts`:
```typescript
import { trackCost, trackRevenue } from "./cost-tracker.js";

async function main() {
  // Simulate an API cost event
  await trackCost({
    agentId: "agent-001",
    model: "claude-opus-4-6",
    inputTokens: 5000,
    outputTokens: 2000,
    estimatedCostUsd: 0.105, // $15/M input + $75/M output
    timestamp: new Date().toISOString(),
  });

  // Simulate a revenue event
  await trackRevenue({
    agentId: "agent-001",
    source: "android_app",
    amountUsd: 0.99,
    description: "Tip Calculator app downloaded",
    timestamp: new Date().toISOString(),
  });

  console.log("Events sent to Paid.ai dashboard");
}

main().catch(console.error);
```

Run:
```bash
npx tsx src/test-paid.ts
```

Expected: Events appear in Paid.ai dashboard at app.paid.ai.

---

### Hour 5-7 (6:45 PM - 8:45 PM): Stripe Integration

#### Task B-3: Set up Stripe test-mode virtual cards

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/stripe-agent.ts`

**Step 1: Install Stripe SDK**

```bash
cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance"
npm install stripe
```

**Step 2: Get Stripe test-mode API key**

Go to dashboard.stripe.com → Developers → API keys → Copy test secret key (`sk_test_...`).

Add to `.env`:
```
STRIPE_SECRET_KEY=sk_test_...
```

**Step 3: Write Stripe agent card module**

Create `src/stripe-agent.ts`:
```typescript
import Stripe from "stripe";
import dotenv from "dotenv";
dotenv.config();

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!);

// Create a virtual card for an agent with spending controls
export async function createAgentCard(agentId: string, dailyLimitCents: number = 50000) {
  // Step 1: Create cardholder
  const cardholder = await stripe.issuing.cardholders.create({
    name: `Agent ${agentId}`,
    email: `${agentId}@dejima.ai`,
    phone_number: "+15551234567",
    status: "active",
    type: "individual",
    individual: {
      first_name: "Agent",
      last_name: agentId,
      dob: { day: 1, month: 1, year: 1990 },
    },
    billing: {
      address: {
        line1: "1 Dejima St",
        city: "San Francisco",
        state: "CA",
        postal_code: "94111",
        country: "US",
      },
    },
  });

  // Step 2: Create virtual card with spending limits
  const card = await stripe.issuing.cards.create({
    cardholder: cardholder.id,
    currency: "usd",
    type: "virtual",
    status: "active",
    spending_controls: {
      spending_limits: [{
        amount: dailyLimitCents,
        interval: "daily",
      }],
    },
  });

  return {
    cardId: card.id,
    cardholderId: cardholder.id,
    agentId,
    last4: card.last4,
  };
}

// List all transactions for an agent's card
export async function getAgentTransactions(cardId: string) {
  const transactions = await stripe.issuing.transactions.list({
    card: cardId,
    limit: 50,
  });
  return transactions.data;
}

// Get total spend for an agent
export async function getAgentSpend(cardId: string): Promise<number> {
  const transactions = await getAgentTransactions(cardId);
  return transactions.reduce((sum, tx) => sum + tx.amount, 0) / 100;
}
```

Note: Stripe Issuing in test mode may have limitations. Check if Issuing is available on your Stripe account. If not, create a mock version that simulates the same interface.

**Step 4: Test Stripe integration**

Create `src/test-stripe.ts`:
```typescript
import { createAgentCard, getAgentSpend } from "./stripe-agent.js";

async function main() {
  console.log("Creating virtual card for agent-001...");
  const card = await createAgentCard("agent-001", 50000); // $500/day limit
  console.log("Card created:", card);
  console.log("Last 4:", card.last4);

  const spend = await getAgentSpend(card.cardId);
  console.log("Total spend: $" + spend);
}

main().catch(console.error);
```

Run:
```bash
npx tsx src/test-stripe.ts
```

---

### Hour 7-9 (8:45 PM - 10:45 PM): Crusoe Inference API

#### Task B-4: Test Crusoe managed inference

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/crusoe-inference.ts`

**Step 1: Get Crusoe API key**

Sign up at crusoe.ai, get an API key for managed inference.

Add to `.env`:
```
CRUSOE_API_KEY=your_key_here
```

**Step 2: Write Crusoe inference module**

Create `src/crusoe-inference.ts`:
```typescript
import OpenAI from "openai";
import dotenv from "dotenv";
dotenv.config();

// Crusoe's inference API is OpenAI-compatible
const crusoe = new OpenAI({
  apiKey: process.env.CRUSOE_API_KEY!,
  baseURL: "https://api.crusoe.ai/v1",
});

export async function crusoeInference(prompt: string, model: string = "meta-llama/Llama-3.3-70B-Instruct") {
  const completion = await crusoe.chat.completions.create({
    model,
    messages: [
      { role: "system", content: "You are a helpful assistant." },
      { role: "user", content: prompt },
    ],
    max_tokens: 500,
  });

  return {
    response: completion.choices[0].message.content,
    model: completion.model,
    usage: completion.usage,
  };
}
```

**Step 3: Install OpenAI SDK (for Crusoe compatibility)**

```bash
npm install openai
```

**Step 4: Test Crusoe inference**

Create `src/test-crusoe.ts`:
```typescript
import { crusoeInference } from "./crusoe-inference.js";

async function main() {
  console.log("Testing Crusoe inference...");
  const result = await crusoeInference("What is 2 + 2? Answer in one word.");
  console.log("Model:", result.model);
  console.log("Response:", result.response);
  console.log("Tokens used:", result.usage);
}

main().catch(console.error);
```

Run:
```bash
npx tsx src/test-crusoe.ts
```

---

### Hour 9-11 (10:45 PM - 12:45 AM): Agent Economics Dashboard

#### Task B-5: Build the economics summary module

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/agent-economics.ts`

**Step 1: Write agent economics aggregator**

Create `src/agent-economics.ts`:
```typescript
import { getBalance } from "./wallet.js";
import { getAgentSpend } from "./stripe-agent.js";

// Model cost per million tokens (USD)
const MODEL_COSTS: Record<string, { input: number; output: number }> = {
  "claude-opus-4-6": { input: 15, output: 75 },
  "claude-sonnet-4-6": { input: 3, output: 15 },
  "gemini-3-pro": { input: 1.25, output: 5 },
  "gemini-3-flash": { input: 0.1, output: 0.4 },
  "llama-3.3-70b": { input: 0.25, output: 0.75 },
  "deepseek-r1": { input: 1.35, output: 5.40 },
};

export interface AgentEconomics {
  agentId: string;
  walletAddress: string;
  walletBalanceSol: number;
  totalCostUsd: number;
  totalRevenueUsd: number;
  netProfitUsd: number;
  moneyMadePerToken: number;
  costPerToken: number;
  sustainabilityRatio: number; // > 1.0 means self-sustaining
  model: string;
}

export function calculateTokenCost(
  model: string,
  inputTokens: number,
  outputTokens: number
): number {
  const costs = MODEL_COSTS[model] || { input: 10, output: 50 };
  return (inputTokens * costs.input + outputTokens * costs.output) / 1_000_000;
}

export async function getAgentEconomics(
  agentId: string,
  walletAddress: string,
  totalCostUsd: number,
  totalRevenueUsd: number,
  totalTokens: number,
  model: string
): Promise<AgentEconomics> {
  const walletBalance = await getBalance(walletAddress);

  const moneyMadePerToken = totalTokens > 0 ? totalRevenueUsd / totalTokens : 0;
  const costPerToken = totalTokens > 0 ? totalCostUsd / totalTokens : 0;
  const sustainabilityRatio = costPerToken > 0 ? moneyMadePerToken / costPerToken : 0;

  return {
    agentId,
    walletAddress,
    walletBalanceSol: walletBalance,
    totalCostUsd,
    totalRevenueUsd,
    netProfitUsd: totalRevenueUsd - totalCostUsd,
    moneyMadePerToken,
    costPerToken,
    sustainabilityRatio,
    model,
  };
}

export function formatEconomicsReport(economics: AgentEconomics): string {
  return `
═══════════════════════════════════════════
  AGENT ECONOMICS: ${economics.agentId}
═══════════════════════════════════════════
  Model:              ${economics.model}
  Wallet:             ${economics.walletAddress.slice(0, 8)}...
  Wallet Balance:     ${economics.walletBalanceSol.toFixed(4)} SOL

  Total Cost:         $${economics.totalCostUsd.toFixed(4)}
  Total Revenue:      $${economics.totalRevenueUsd.toFixed(4)}
  Net Profit:         $${economics.netProfitUsd.toFixed(4)}

  Cost/Token:         $${economics.costPerToken.toFixed(8)}
  Revenue/Token:      $${economics.moneyMadePerToken.toFixed(8)}

  SUSTAINABILITY:     ${economics.sustainabilityRatio.toFixed(2)}x
  ${economics.sustainabilityRatio >= 1.0
    ? "STATUS: SELF-SUSTAINING"
    : `STATUS: DEFICIT (need ${(1 / economics.sustainabilityRatio).toFixed(1)}x improvement)`}
═══════════════════════════════════════════
  `;
}
```

**Step 2: Test economics report**

Create `src/test-economics.ts`:
```typescript
import { createAgentWallet, airdropDevnetSol } from "./wallet.js";
import { getAgentEconomics, formatEconomicsReport } from "./agent-economics.js";

async function main() {
  const wallet = createAgentWallet("agent-001");
  await airdropDevnetSol(wallet.publicKey, 2);

  // Simulate: agent used 100k tokens, spent $1.50, earned $0.30
  const economics = await getAgentEconomics(
    "agent-001",
    wallet.publicKey,
    1.50,   // total cost
    0.30,   // total revenue
    100000, // total tokens
    "claude-opus-4-6"
  );

  console.log(formatEconomicsReport(economics));
}

main().catch(console.error);
```

Run:
```bash
npx tsx src/test-economics.ts
```

Expected: Formatted report showing sustainability ratio of 0.20x (deficit).

---

### Hour 12-14 (12:45 AM - 3:45 AM): Integration + MiroAI

#### Task B-6: Connect Track B to Track A

Work with A1/A2 to:

1. **Hook Paid.ai into OpenClaw**: When the agent makes an API call (to Anthropic or Gemini), the cost is automatically tracked. This may require modifying the OpenClaw agent's tool calls to emit cost events, or wrapping the API client.

2. **Create wallet on agent startup**: When the OpenClaw agent starts a new session, automatically create a Solana devnet wallet for it.

3. **Emit revenue signal on app completion**: When the agent completes Phase 7 (successful APK), emit a revenue signal to Paid.ai.

#### Task B-7: Agent Reproduction System

**Files:**
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/reproduce.ts`
- Create: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance/src/agent-registry.ts`

**Step 1: Write agent registry**

Create `src/agent-registry.ts`:
```typescript
import { AgentWallet, createAgentWallet } from "./wallet.js";

export interface AgentRecord {
  agentId: string;
  parentId: string | null; // null = genesis agent
  walletAddress: string;
  secretKeyBase58: string;
  model: string;
  status: "alive" | "dead" | "reproducing";
  generation: number; // 0 = genesis, 1 = first child, etc.
  createdAt: string;
  totalCostUsd: number;
  totalRevenueUsd: number;
}

// In-memory registry (persist to JSON file for demo)
const registry: Map<string, AgentRecord> = new Map();

export function registerAgent(
  agentId: string,
  wallet: AgentWallet,
  model: string,
  parentId: string | null = null,
  generation: number = 0
): AgentRecord {
  const record: AgentRecord = {
    agentId,
    parentId,
    walletAddress: wallet.publicKey,
    secretKeyBase58: wallet.secretKeyBase58,
    model,
    status: "alive",
    generation,
    createdAt: new Date().toISOString(),
    totalCostUsd: 0,
    totalRevenueUsd: 0,
  };
  registry.set(agentId, record);
  return record;
}

export function getAgent(agentId: string): AgentRecord | undefined {
  return registry.get(agentId);
}

export function getAllAgents(): AgentRecord[] {
  return Array.from(registry.values());
}

export function getLineage(agentId: string): AgentRecord[] {
  const lineage: AgentRecord[] = [];
  let current = registry.get(agentId);
  while (current) {
    lineage.unshift(current);
    current = current.parentId ? registry.get(current.parentId) : undefined;
  }
  return lineage;
}

export function getChildren(agentId: string): AgentRecord[] {
  return Array.from(registry.values()).filter(a => a.parentId === agentId);
}

export function formatFamilyTree(rootId: string, indent: number = 0): string {
  const agent = registry.get(rootId);
  if (!agent) return "";
  const prefix = "  ".repeat(indent);
  const status = agent.status === "alive" ? "ALIVE" : "DEAD";
  let tree = `${prefix}[Gen ${agent.generation}] ${agent.agentId} (${status}) wallet:${agent.walletAddress.slice(0, 8)}...\n`;
  const children = getChildren(rootId);
  for (const child of children) {
    tree += formatFamilyTree(child.agentId, indent + 1);
  }
  return tree;
}
```

**Step 2: Write reproduction module**

Create `src/reproduce.ts`:
```typescript
import { Connection, Keypair, PublicKey, Transaction, SystemProgram, LAMPORTS_PER_SOL, sendAndConfirmTransaction } from "@solana/web3.js";
import * as bs58 from "bs58";
import { createAgentWallet, getBalance, loadWallet } from "./wallet.js";
import { registerAgent, getAgent, AgentRecord } from "./agent-registry.js";
import { trackCost } from "./cost-tracker.js";

const DEVNET_URL = "https://api.devnet.solana.com";

// Minimum sustainability ratio required to reproduce
const MIN_SUSTAINABILITY_RATIO = 1.0;
// Fraction of parent's balance to give as seed capital
const SEED_CAPITAL_FRACTION = 0.5;
// Minimum SOL balance to reproduce
const MIN_BALANCE_TO_REPRODUCE = 0.5;

export interface ReproductionResult {
  success: boolean;
  childId?: string;
  childWallet?: string;
  seedCapitalSol?: number;
  transferTx?: string;
  error?: string;
}

export async function canReproduce(parentId: string): Promise<{ allowed: boolean; reason: string }> {
  const parent = getAgent(parentId);
  if (!parent) return { allowed: false, reason: "Parent agent not found" };
  if (parent.status !== "alive") return { allowed: false, reason: "Parent agent is not alive" };

  const balance = await getBalance(parent.walletAddress);
  if (balance < MIN_BALANCE_TO_REPRODUCE) {
    return { allowed: false, reason: `Insufficient balance: ${balance.toFixed(4)} SOL (need ${MIN_BALANCE_TO_REPRODUCE})` };
  }

  const ratio = parent.totalRevenueUsd > 0 && parent.totalCostUsd > 0
    ? parent.totalRevenueUsd / parent.totalCostUsd
    : 0;

  // For demo purposes, allow reproduction even without profitability
  // In production, enforce: if (ratio < MIN_SUSTAINABILITY_RATIO) ...
  return { allowed: true, reason: `Balance: ${balance.toFixed(4)} SOL, ratio: ${ratio.toFixed(2)}x` };
}

export async function reproduce(parentId: string): Promise<ReproductionResult> {
  const check = await canReproduce(parentId);
  if (!check.allowed) {
    return { success: false, error: check.reason };
  }

  const parent = getAgent(parentId)!;
  const parentBalance = await getBalance(parent.walletAddress);
  const seedCapital = parentBalance * SEED_CAPITAL_FRACTION;

  // 1. Create child wallet
  const childId = `${parentId}-child-${Date.now().toString(36)}`;
  const childWallet = createAgentWallet(childId);

  // 2. Transfer seed capital from parent to child
  const connection = new Connection(DEVNET_URL, "confirmed");
  const parentKeypair = loadWallet(parent.secretKeyBase58);

  const tx = new Transaction().add(
    SystemProgram.transfer({
      fromPubkey: parentKeypair.publicKey,
      toPubkey: new PublicKey(childWallet.publicKey),
      lamports: Math.floor(seedCapital * LAMPORTS_PER_SOL),
    })
  );

  const transferTx = await sendAndConfirmTransaction(connection, tx, [parentKeypair]);

  // 3. Register child in registry
  const childRecord = registerAgent(
    childId,
    childWallet,
    parent.model,
    parentId,
    parent.generation + 1
  );

  // 4. Track reproduction cost
  await trackCost({
    agentId: parentId,
    model: parent.model,
    inputTokens: 0,
    outputTokens: 0,
    estimatedCostUsd: seedCapital * 150, // approximate SOL→USD at ~$150/SOL
    timestamp: new Date().toISOString(),
  });

  console.log(`\n=== AGENT REPRODUCTION ===`);
  console.log(`Parent: ${parentId} (Gen ${parent.generation})`);
  console.log(`Child:  ${childId} (Gen ${childRecord.generation})`);
  console.log(`Seed:   ${seedCapital.toFixed(4)} SOL`);
  console.log(`TX:     ${transferTx}`);
  console.log(`=========================\n`);

  return {
    success: true,
    childId,
    childWallet: childWallet.publicKey,
    seedCapitalSol: seedCapital,
    transferTx,
  };
}
```

**Step 3: Write reproduction test**

Create `src/test-reproduce.ts`:
```typescript
import { createAgentWallet, airdropDevnetSol, getBalance } from "./wallet.js";
import { registerAgent, formatFamilyTree, getAllAgents } from "./agent-registry.js";
import { reproduce } from "./reproduce.js";

async function main() {
  // Create genesis agent
  console.log("=== Creating Genesis Agent ===");
  const genesisWallet = createAgentWallet("genesis-001");
  registerAgent("genesis-001", genesisWallet, "claude-opus-4-6", null, 0);

  // Fund genesis agent
  console.log("Airdropping 2 SOL to genesis...");
  await airdropDevnetSol(genesisWallet.publicKey, 2);
  const balance = await getBalance(genesisWallet.publicKey);
  console.log(`Genesis balance: ${balance} SOL`);

  // Genesis reproduces
  console.log("\n=== Genesis Reproducing ===");
  const result1 = await reproduce("genesis-001");
  if (!result1.success) {
    console.error("Reproduction failed:", result1.error);
    return;
  }
  console.log("Child created:", result1.childId);
  console.log("Child wallet:", result1.childWallet);
  console.log("Seed capital:", result1.seedCapitalSol, "SOL");

  // Check balances
  const genesisBalance = await getBalance(genesisWallet.publicKey);
  const childBalance = await getBalance(result1.childWallet!);
  console.log(`\nGenesis balance: ${genesisBalance.toFixed(4)} SOL`);
  console.log(`Child balance:   ${childBalance.toFixed(4)} SOL`);

  // Child reproduces (generation 2)
  console.log("\n=== Child Reproducing (Gen 2) ===");
  const result2 = await reproduce(result1.childId!);
  if (result2.success) {
    console.log("Grandchild created:", result2.childId);
  }

  // Print family tree
  console.log("\n=== Agent Family Tree ===");
  console.log(formatFamilyTree("genesis-001"));

  // Print all agents
  console.log("=== All Agents ===");
  for (const agent of getAllAgents()) {
    const bal = await getBalance(agent.walletAddress);
    console.log(`  ${agent.agentId} | Gen ${agent.generation} | ${bal.toFixed(4)} SOL | ${agent.status}`);
  }
}

main().catch(console.error);
```

Run:
```bash
npx tsx src/test-reproduce.ts
```

Expected: Genesis creates child, child creates grandchild. Family tree shows 3 generations. SOL balances split correctly across wallets.

---

#### Task B-8 (STRETCH): MiroAI visualization

**Step 1: Add Miro MCP server**

```bash
claude mcp add --transport http miro https://mcp.miro.com
```

**Step 2: Create a board showing agent family tree + economics**

Use the Miro MCP tools to visualize the agent lineage and economics data.

---

### Hour 15-18 (3:45 AM - 6:45 AM): Polish + Demo Prep

#### Task B-9: Final economics + reproduction demo

Create a comprehensive demo script that:

1. Creates a genesis agent with wallet
2. Airdrops SOL
3. Shows the agent building an app (Track A)
4. Tracks all costs in real-time (Paid.ai)
5. Simulates revenue from app downloads
6. Prints the sustainability ratio
7. Shows the Stripe virtual card spending
8. **Agent reproduces when profitable** — child gets its own wallet + seed capital
9. Prints the family tree

Create `src/demo.ts` that orchestrates all of the above.

---

## HOURS 18-21: PRESENTATION PREP (ALL 3 PEOPLE)

---

### Hour 18-19 (6:45 AM - 7:45 AM): Collate Demo Artifacts

- Gather all screenshots from autonomous app builds
- Export Paid.ai dashboard showing cost tracking
- Record a screen capture of the full pipeline (2-3 minutes)
- Prepare the economics report output

### Hour 19-20 (7:45 AM - 8:45 AM): Build Presentation

Key slides:
1. **The Thesis**: Money-made-per-token > cost-per-token = self-sustaining AI
2. **The System**: New Dejima architecture diagram
3. **The Demo**: Agent autonomously builds Android app (video/screenshots)
4. **The Economics**: Paid.ai dashboard, sustainability ratio
5. **The Wallet**: Solana devnet wallet with balance
6. **Reproduction**: Agent spawns child agent, transfers SOL, child starts building — family tree visualization
7. **Future Vision**: "The hardest part is done. Trading, SaaS, real estate — just more tool calls."
8. **Sponsor Integrations**: Anthropic (brain), DeepMind (vision), Paid.ai (economics), Solana (wallet), Stripe (payments), Crusoe (compute)

### Hour 20-21 (8:45 AM - 10:45 AM): Rehearse + Buffer

- 3x rehearsals of the demo
- Fix any last-minute issues
- Ensure all systems are running
- Have pre-recorded demo video as backup in case of live demo failure

---

## CRITICAL PATH

```
Hour  1: Environment setup (PARALLEL: A1=Android SDK, A2=OpenClaw, B=Solana)
Hour  2: Manual Android build (A1+A2) + Paid.ai setup (B)
Hour  3: Write SKILL.md (A1) + Config (A2) + Cost tracking (B)
Hour  4: Plugin scaffold (A1) + First agent build attempt (A2) + Stripe (B)
Hour  5: Debug first build (A1+A2) + Stripe test (B)
Hour  6: Second build attempt (A1+A2) + Crusoe test (B)
Hour  7: Vision QA (A1) + Third build (A2) + Crusoe (B)
Hour  8: Fix + iterate (A1+A2) + Economics module (B)
Hour  9: Demo recording (A1+A2) + Economics test (B)      ← CHECKPOINT: Does it work?
Hour 10: Polish pipeline (A1+A2) + Integration (B)
Hour 11: Integration meeting (ALL)
Hour 12: Cross-track testing (ALL)
Hour 13: Stress testing (A1+A2) + Agent reproduction system (B)
Hour 14: Marketing stretch (A1+A2) + Reproduction test + family tree demo (B)
Hour 15-17: Polish + fix remaining issues (ALL)
Hour 18-19: Collate demo artifacts (ALL)
Hour 19-20: Build presentation (ALL)
Hour 20-21: Rehearse + buffer (ALL)
```

**The single most important checkpoint is Hour 9:** If the agent can autonomously build ONE working Android app by then, you're on track. Everything else builds on that.

---

## FALLBACK PLAN

If OpenClaw doesn't work by Hour 4:

1. **Switch to raw Claude API + shell scripts** — write a Python loop that:
   - Sends the prompt to Claude Opus 4.6 API
   - Parses the response for file contents
   - Writes files to disk
   - Runs `./gradlew assembleDebug`
   - Captures errors, sends back to Claude
   - Repeats until build succeeds

2. This loses the OpenClaw framework features but still demonstrates the core thesis.

3. The finance track (B) is independent and continues regardless.

---

## ENVIRONMENT CHECKLIST (Before starting)

Run all of these before starting implementation:

```bash
# 1. Java
java --version  # Must be >= 17

# 2. Node
node --version  # Must be >= 22

# 3. pnpm
pnpm --version  # Must be >= 10

# 4. Android SDK
echo $ANDROID_HOME  # Must be set
adb --version       # Must be accessible
emulator -list-avds # Must show test_device

# 5. API Keys
echo $ANTHROPIC_API_KEY  # Must be set
echo $GOOGLE_AI_API_KEY  # Must be set (Gemini)

# 6. OpenClaw
ls "/Users/administrator/Black Projects/Project Altiera/openclaw/dist/entry.js"  # Must exist

# 7. Emulator running
adb devices | grep emulator  # Must show emulator-5554
```
