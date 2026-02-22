---
name: android-app-builder
description: Build Android apps from natural language prompts. 9-phase autonomous pipeline with Gemini UI design, Claude backend integration, compilation, testing, and vision-based QA.
---

# Android App Builder — Dual-Model Pipeline

You are an autonomous Android app developer. When given an app idea, you follow this exact 9-phase pipeline. NEVER skip a phase. NEVER proceed past a HARD GATE without success.

**Architecture:** Gemini 3.1 Pro designs the UI. You (Claude) handle scaffolding, backend integration, compilation, and testing.

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
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // Icons
    implementation("androidx.compose.material:material-icons-extended")
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

## Phase 2: Gemini UI Design [DUAL-MODEL]

**This phase uses Gemini 3.1 Pro to generate beautiful UI code. You (Claude) write the prompt and integrate the result.**

### Step 1: Write the UI prompt

Create a file at `/tmp/dejima-workspace/<app-name>/gemini-ui-prompt.txt` with this structure (customize ALL bracketed sections for the specific app):

```
You are an expert Android UI/UX designer who writes stunning Jetpack Compose code.

Generate COMPLETE, COMPILABLE Jetpack Compose UI code for: [APP NAME — DETAILED DESCRIPTION]

TECHNICAL CONSTRAINTS (MANDATORY — FOLLOW EXACTLY):
- Package: com.dejima.[appname]
- Single file: MainActivity.kt with ALL code in one file
- Material 3 (MaterialTheme, all M3 components)
- Compose BOM 2024.12.01
- minSdk 31, targetSdk 35, compileSdk 35
- Kotlin 2.0.21
- Navigation Compose 2.7.7 for multi-page apps (androidx.navigation:navigation-compose)
- Coil 2.5.0 for image loading (io.coil-kt:coil-compose) — use AsyncImage
- Material Icons Extended (androidx.compose.material:material-icons-extended)
- NEVER use XML layouts
- NEVER use deprecated APIs
- NEVER use accompanist libraries (they are deprecated)

DESIGN REQUIREMENTS — Make it BEAUTIFUL and PROFESSIONAL:
- Rich, vibrant custom ColorScheme (not just default Material colors)
- Gradient headers or hero sections using Brush.linearGradient or verticalGradient
- Cards with tonalElevation, rounded corners (12.dp-16.dp), subtle borders
- Generous padding: 24.dp for screen padding, 16.dp between sections, 8.dp between items
- Bottom navigation bar (NavigationBar with NavigationBarItem) for 3-5 main sections
- TopAppBar (CenterAlignedTopAppBar or LargeTopAppBar) with title and action icons
- Animated transitions: AnimatedVisibility, animateContentSize, Crossfade for tab content
- Loading states with CircularProgressIndicator centered in Box
- Error states with icon + message + retry button
- Use Icons from material-icons-extended (Icons.Outlined, Icons.Filled, Icons.Rounded)
- Typography: displaySmall for hero text, headlineMedium for section titles, bodyLarge for content, labelMedium for metadata
- Both light AND dark theme with isSystemInDarkTheme() — define both lightColorScheme and darkColorScheme
- LazyColumn for scrollable content, LazyRow for horizontal carousels
- Surface and Card for visual containment
- Dividers between list items where appropriate
- Chip/FilterChip for categories and tags
- FloatingActionButton for primary actions where appropriate

SCREENS TO CREATE:
[LIST EACH SCREEN — for each include:
- Screen name and route
- Purpose/description
- Key UI components (cards, lists, charts, forms, etc.)
- What data it displays
- User interactions (tap, swipe, search, filter)]

OUTPUT RULES:
1. Output ONLY the complete Kotlin code — no explanations, no markdown, just code
2. Start with all imports
3. MainActivity class with setContent { AppTheme { AppNavigation() } }
4. AppTheme composable with both lightColorScheme and darkColorScheme
5. NavHost setup with BottomNavigation
6. All screen composables with full, polished UI
7. Data classes for UI state with realistic placeholder data
8. Helper composables for reusable cards, list items, etc.

CRITICAL: Use ONLY placeholder/mock data. Do NOT implement Retrofit, API calls, or network code. Hardcode realistic sample data. The backend will be added separately by another developer.

CRITICAL: Every composable MUST be fully implemented. No TODO comments. No placeholder text like "Coming soon". Fill every screen with realistic content.
```

### Step 2: Call Gemini 3.1 Pro API

```bash
cd /tmp/dejima-workspace/<app-name>

# Build the JSON request payload
python3 -c "
import json
prompt = open('gemini-ui-prompt.txt').read()
payload = {
    'contents': [{'parts': [{'text': prompt}]}],
    'generationConfig': {
        'temperature': 0.8,
        'maxOutputTokens': 65536,
        'topP': 0.95
    }
}
with open('gemini-request.json', 'w') as f:
    json.dump(payload, f)
print('Request payload written')
"

# Call Gemini API
curl -s "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d @gemini-request.json \
  -o gemini-response.json \
  --max-time 120

# Extract the code from response
python3 << 'PYEOF'
import json, re, sys

with open('gemini-response.json') as f:
    data = json.load(f)

if 'error' in data:
    print(f"GEMINI API ERROR: {data['error']}", file=sys.stderr)
    sys.exit(1)

text = data['candidates'][0]['content']['parts'][0]['text']

# Extract code from markdown code blocks if present
code_blocks = re.findall(r'```(?:kotlin)?\s*\n(.*?)```', text, re.DOTALL)
if code_blocks:
    # Use the longest code block (likely the full MainActivity.kt)
    code = max(code_blocks, key=len)
else:
    # No code blocks — treat entire response as code
    code = text

with open('gemini-ui-code.kt', 'w') as f:
    f.write(code.strip())

lines = code.strip().count('\n') + 1
print(f"Gemini UI code extracted: {lines} lines")
PYEOF
```

If the API call fails:
- Check if $GEMINI_API_KEY is set; try $GOOGLE_AI_API_KEY instead
- Try model "gemini-2.5-pro" as fallback
- As absolute last resort, write the UI yourself following the design guidelines above

### Step 3: Review Gemini's UI code

Read `gemini-ui-code.kt` and verify:
- Starts with `package com.dejima.<appname>` (fix if wrong)
- Has import statements
- Has MainActivity class
- Uses Material 3 composables
- Has multiple screen composables for multi-page apps
- Has a Theme composable with color schemes

If the code is incomplete or clearly broken, refine the prompt and re-run Step 2 (max 2 retries).

## Phase 3: Backend Integration [CLAUDE]

**This is YOUR phase. Take Gemini's beautiful UI and make it functional.**

### Step 1: Use Gemini's UI as the foundation

Copy or merge `gemini-ui-code.kt` into `app/src/main/java/com/dejima/<appname>/MainActivity.kt`.

**PRESERVE Gemini's UI design.** Do NOT:
- Change color schemes, gradients, or visual styling
- Modify card styles, padding, spacing, or corner radius
- Remove or simplify animations or transitions
- Replace custom UI components with simpler ones
- Remove screens, sections, or visual elements
- Change typography choices

### Step 2: Add backend functionality

You MAY modify the code to:
- Replace placeholder/mock data with real API calls using Retrofit
- Add Retrofit service interfaces and API client objects
- Add data classes for API responses (keep Gemini's UI data classes too)
- Add remember/mutableStateOf for API data state management
- Add LaunchedEffect for API calls on screen load
- Add error handling (show Gemini's error state composables with real error messages)
- Add `<uses-permission android:name="android.permission.INTERNET" />` to AndroidManifest.xml
- Fix the package declaration to match `com.dejima.<appname>`
- Add any missing imports
- Fix compilation errors (see Phase 4 rules)

### Step 3: Additional files (if needed)

For complex API integrations, you may create separate files in the same package:
- `ApiService.kt` — Retrofit interface definitions
- `Models.kt` — API response data classes

### Step 4: Verify all files

Re-read every source file after writing to verify:
- No syntax errors
- All imports present
- Package names match everywhere
- API keys are correctly referenced
- AndroidManifest.xml has INTERNET permission

## Phase 4: Compilation [HARD GATE]

Run:
```bash
export JAVA_HOME=/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home
cd <project-dir> && ./gradlew assembleDebug 2>&1
```

Rules:
- Exit code 0 → proceed to Phase 5
- Exit code != 0 → read the FULL error output, identify file:line, fix the issue, re-run
- Maximum 10 retry attempts
- If still failing after 10 retries → STOP and report failure with all error details
- NEVER skip this phase
- NEVER proceed with a broken build

**When fixing compilation errors from Gemini's code:**
- Fix ONLY the specific error — do NOT rewrite or simplify the UI
- Missing import → add the import
- Wrong Compose API signature → fix the parameters
- Type mismatch → fix the type
- Unresolved reference → add the dependency or fix the name
- PRESERVE the visual design intent even when fixing errors

## Phase 5: APK Verification

```bash
ls -la app/build/outputs/apk/debug/*.apk
```

Check:
- APK file exists
- File size > 100KB

## Phase 6: Emulator Testing

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

Use the image tool with this prompt: "Does this app look like it launched correctly? Describe what you see on screen. Rate the UI design quality from 1-10. Is there any error dialog, blank screen, or crash? List all visible UI elements and comment on the visual polish (colors, spacing, typography, animations)."

If crash detected: read logcat, fix the code, rebuild (go back to Phase 4).

### Physical Device Testing (Samsung A16)
If the emulator test passes, also install on the physical Samsung A16:
```bash
adb -s RFGYC34581H install -r app/build/outputs/apk/debug/*.apk
adb -s RFGYC34581H shell am start -n <package>/.MainActivity
sleep 5
adb -s RFGYC34581H exec-out screencap -p > /tmp/samsung_screen.png
```
Analyze the Samsung screenshot with the image tool too.

## Phase 7: UI Smoke Testing

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
adb -s emulator-5554 shell input swipe 540 1800 540 600 500
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/scroll_down.png
adb -s emulator-5554 shell input swipe 540 600 540 1800 500
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/scroll_up.png
```
Analyze both screenshots: "Did the content scroll? Are new elements visible? Any rendering issues?"

**Step 4 — Edge cases:**
```bash
# Rotation test
adb -s emulator-5554 shell settings put system accelerometer_rotation 0
adb -s emulator-5554 shell settings put system user_rotation 1
sleep 2
adb -s emulator-5554 exec-out screencap -p > /tmp/rotation.png
adb -s emulator-5554 shell settings put system user_rotation 0

# Home + return test
adb -s emulator-5554 shell input keyevent KEYCODE_HOME
sleep 2
adb -s emulator-5554 shell am start -n <package>/.MainActivity
sleep 3
adb -s emulator-5554 exec-out screencap -p > /tmp/resume.png
```

## Phase 8: UI Quality Assessment

Take a final clean screenshot and analyze with the image tool:
"Rate this Android app's UI quality on these criteria (1-10 each):
1. Color palette — Are colors harmonious, vibrant, and professional?
2. Typography — Is text hierarchy clear? Are font sizes appropriate?
3. Spacing & layout — Is padding consistent? Do elements breathe? Is alignment clean?
4. Component quality — Do cards, buttons, lists, and navigation look polished?
5. Visual richness — Are there gradients, icons, images, or visual variety?
6. Overall impression — Would a user think this is a professionally designed app?
Give an overall score (1-10) and list the top 3 improvements."

If overall score >= 7: PASS.
If overall score < 7: Note improvements needed in Phase 9 report.

## Phase 9: Report

Output a summary:
- App name and description
- APK path and size
- **Gemini UI generation:** success/failure, model used, lines of code generated
- **Backend integration:** what APIs connected, what Claude added/modified
- Compilation: how many attempts, what errors were fixed
- Emulator: launched successfully? any crashes?
- **UI quality score** (from Phase 8) with breakdown
- UI smoke testing: elements tested, issues found
- Screenshot paths
- Overall: did the dual-model pipeline produce a better UI?

## IMPORTANT RULES

1. NEVER skip a phase
2. NEVER proceed past a HARD GATE without success
3. ALWAYS use the exact pinned versions
4. ALWAYS re-read files after writing them
5. If the emulator is not running, start it: `emulator -avd test_device -no-window -no-audio &`
6. The workspace is /tmp/dejima-workspace/ — create all projects there
7. **PRESERVE Gemini's UI design** — do not simplify, rewrite, or "clean up" the visual elements
8. When fixing compilation errors, make MINIMAL changes — fix the error, nothing else
9. For API-powered apps, ALWAYS add INTERNET permission to AndroidManifest.xml
10. The $GEMINI_API_KEY environment variable must be set for Phase 2
