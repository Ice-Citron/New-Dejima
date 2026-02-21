# Design: Autonomous Android App Development via OpenClaw

**Date**: 2026-02-15
**Status**: Approved
**Project**: New Dejima / Project Altiera
**Repo**: `/Users/administrator/Black Projects/Project Altiera/openclaw`

---

## Overview

Build an end-to-end pipeline where an OpenClaw agent autonomously creates an Android app from a natural language prompt, compiles it, tests it on both an emulator and a physical Samsung A16, validates the UI, detects crashes, and produces a signed debug APK.

**Key decisions:**
- App type: Agent's choice (unpredictable structure)
- Test targets: Android emulator + Samsung A16 physical device
- Runtime: Full OpenClaw gateway (not standalone script)
- Vision model: Gemini 3 Pro configured as OpenClaw's image tool provider
- Scaffolding: All three layers (system prompt + skill + plugin)

---

## 1. Prerequisites

### Already Installed

| Tool | Version |
|------|---------|
| Node.js (via nvm) | v24.13.1 |
| pnpm | 10.23.0 |
| Java OpenJDK | 25.0.1 |
| Docker | 29.1.1 |
| Homebrew | installed |
| Android SDK Platform 36 | installed |
| Android Build-Tools 36.0.0 | installed |
| Android Platform-Tools (ADB) | installed (not in PATH) |
| Android Emulator | installed (not in PATH) |
| Android System Image (API 35 arm64) | installed |
| OpenClaw | built |
| Playwright Chromium | installed |

### Still Needed

**1. Shell environment variables** — add to `~/.zshrc`:
```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```
Then `source ~/.zshrc`.

**2. Android Virtual Device (AVD)**:
```bash
avdmanager create avd -n "test_device" \
  -k "system-images;android-35;google_apis;arm64-v8a" \
  -d "pixel_6"
```

**3. Gemini API key** — from ai.google.dev

**4. Samsung A16 USB debugging** — Settings > About Phone > tap Build Number 7x > Developer Options > USB Debugging ON

**5. Anthropic + OpenAI API keys** — assumed available (~$3k credits)

### Verification Commands

```bash
# Core tools
node --version                              # >= 22.12.0
pnpm --version                              # 10.23.0
java --version                              # >= 17

# Android toolchain
adb --version                               # 36.x
emulator -list-avds                         # shows "test_device"
echo $ANDROID_HOME                          # ~/Library/Android/sdk

# Emulator boot test
emulator -avd test_device -no-window -no-audio &
sleep 30
adb wait-for-device
adb shell getprop sys.boot_completed        # returns "1"
adb emu kill

# Samsung connection test (with USB cable)
adb devices                                 # shows Samsung serial

# OpenClaw
node openclaw.mjs --version                 # prints version
```

---

## 2. OpenClaw Modifications

### Layer A: Configuration

**File**: `~/.openclaw/config.yaml`

- Define custom agent `android-dev` with workspace, model, skills, tools
- Add Google Gemini 3 Pro as vision provider
- Set primary coding model (Claude Opus 4.6)
- Configure exec approval defaults for Android dev commands

**File**: `~/.openclaw/exec-approvals.json`

- Pre-approve safe commands: `./gradlew`, `adb install`, `adb shell`, `adb exec-out`, `adb logcat`, `adb devices`, `emulator`, `avdmanager`, `aapt2`
- These prevent the agent from getting stuck waiting for human approval on every build/test command

### Layer B: Custom Skill — `android-app-builder`

**File**: `~/.openclaw/skills/android-app-builder/SKILL.md`

A markdown file with YAML frontmatter defining a 7-phase pipeline:

#### Phase 1: Project Scaffolding

Create Android project directory structure:
```
app-name/
├── app/
│   ├── src/main/
│   │   ├── java/com/dejima/<appname>/
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   ├── values/themes.xml
│   │   │   └── mipmap-hdpi/ic_launcher.png
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts (project-level)
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

Pinned versions (known working from OpenClaw's own Android app):
- compileSdk = 36, minSdk = 31, targetSdk = 36
- Kotlin = 2.2.21
- Compose BOM = 2025.12.00
- Gradle wrapper = 9.2.1
- JVM target = 17
- Always Jetpack Compose (not XML layouts)
- Always Material 3
- Always Kotlin (never Java)

#### Phase 2: Code Generation

- Write all Kotlin source files
- Write all resource files (strings, themes, colors)
- Write AndroidManifest.xml with correct package, permissions, activities
- Re-read every file after writing to verify no syntax errors
- Do NOT proceed to build until all files are written

#### Phase 3: Compilation Check (MANDATORY — HARD GATE)

```bash
cd <project-dir> && ./gradlew assembleDebug 2>&1
```

Rules:
- Exit code 0 → proceed to Phase 4
- Exit code != 0 → read full error, identify file:line, fix, re-run
- Maximum 10 retry attempts
- If still failing after 10 → STOP and report to user
- NEVER skip this phase
- NEVER proceed with a broken build

#### Phase 4: APK Verification

```bash
# Check APK exists
ls -la app/build/outputs/apk/debug/*.apk

# Check file size (should be > 500KB)
# Verify APK contents
aapt2 dump badging app/build/outputs/apk/debug/*.apk 2>&1 | head -5

# Fallback if aapt2 unavailable
unzip -l app/build/outputs/apk/debug/*.apk | head -20
# Must contain: classes.dex, AndroidManifest.xml, res/
```

#### Phase 5: Emulator Testing

```bash
# Ensure emulator running
adb devices | grep emulator
# If not: emulator -avd test_device -no-window -no-audio &
# Wait: adb wait-for-device && adb shell getprop sys.boot_completed

# Install
adb -s emulator-5554 install -r app/build/outputs/apk/debug/*.apk

# Launch
adb -s emulator-5554 shell am start -n <package>/<package>.MainActivity

# Crash check (within 30s of launch)
adb -s emulator-5554 logcat -d -t 30 *:E | grep -E "(FATAL|ANR|crash|Exception|Process.*died)"

# Screenshot + vision analysis
adb -s emulator-5554 exec-out screencap -p > /tmp/screen_launch.png
# Analyse with Gemini 3 Pro: "Does this app look like it launched correctly?"
```

#### Phase 6: UI Smoke Testing (AUTOMATED QA)

**Step 1 — Element inventory:**
- Take screenshot
- Vision analysis: "List every clickable element with description and x,y coordinates"

**Step 2 — Tap testing (for each element):**
```bash
adb shell input tap <x> <y>               # Tap element
sleep 2
adb logcat -d -t 5 *:E | grep FATAL       # Check crash
adb exec-out screencap -p > /tmp/tap.png   # Screenshot
# Vision: "Did app crash? New screen? Back button visible? Text overlap?"
adb shell input keyevent KEYCODE_BACK      # Go back
sleep 1
adb exec-out screencap -p > /tmp/back.png  # Verify return
```

**Step 3 — Navigation integrity:**
- Press back repeatedly until home screen
- Count presses — should match navigation depth
- `adb shell dumpsys activity activities | grep -A 5 <package>` — verify activity stack

**Step 4 — Edge cases:**
- Rotation: `adb shell settings put system accelerometer_rotation 1` → screenshot → verify layout
- Home + return: KEYCODE_HOME → wait → relaunch → verify state restoration
- Force stop + relaunch: `adb shell am force-stop <package>` → relaunch → verify cold start

#### Phase 7: Samsung A16 Device Testing

Same as Phase 5-6 but targeting Samsung serial instead of emulator-5554.

Additional physical device checks:
```bash
# Performance (frame jank)
adb -s <serial> shell dumpsys gfxinfo <package> | grep "Total frames"

# Memory usage
adb -s <serial> shell dumpsys meminfo <package> | head -20

# Demo video (10 seconds)
adb -s <serial> shell screenrecord /sdcard/demo.mp4 --time-limit 10
adb -s <serial> pull /sdcard/demo.mp4 /tmp/demo.mp4
```

### Layer C: Custom Plugin — `android-dev-plugin`

**Directory**: `~/.openclaw/plugins/android-dev/`

```
android-dev/
├── openclaw.plugin.json
├── index.ts
└── tools/
    ├── android-build.ts
    ├── android-test.ts
    └── android-logcat.ts
```

**`android_build` tool** — Wraps Phase 3 + Phase 4:
- Runs `./gradlew assembleDebug`
- On failure: extracts file, line, error message from Gradle output (structured)
- On success: verifies APK exists, checks size, validates contents
- Returns: `{ success: bool, apkPath: string, apkSizeBytes: number, errors: [{file, line, message}] }`

**`android_test` tool** — Wraps Phase 5 + Phase 6:
- Parameters: `{ apkPath, device: "emulator"|"samsung", testDepth: "smoke"|"full" }`
- Installs APK, launches, checks crash, takes screenshot
- Returns: `{ launched: bool, crashed: bool, crashLog: string, screenshotPath: string }`

**`android_logcat` tool** — Crash detection:
- Parameters: `{ device, packageName, seconds: number }`
- Runs `adb logcat -d -t <seconds> *:E | grep <package>`
- Returns: `{ hasCrash: bool, hasANR: bool, errors: string[] }`

### Layer D: Image Tool Modification — Gemini 3 Pro

**File**: `src/agents/tools/image-tool.ts` (in OpenClaw repo)

Add Gemini 3 Pro as a vision provider alongside existing Claude/GPT/MiniMax:
- Add `GOOGLE_IMAGE_PRIMARY = "google/gemini-3-pro"` constant
- Add Gemini API call logic in execute function
- Make provider selection configurable via config YAML

If OpenClaw already supports Gemini as a general provider via `models-config.providers.ts`, may only need config YAML changes (no code modification).

---

## 3. Testing Checklists

### A. Prerequisites Verification

| # | Test | Command | Expected |
|---|------|---------|----------|
| 1 | Node version | `node --version` | v24.x.x |
| 2 | pnpm version | `pnpm --version` | 10.23.0 |
| 3 | Java version | `java --version` | >= 17 |
| 4 | ADB accessible | `adb --version` | 36.x |
| 5 | Emulator accessible | `emulator -list-avds` | shows test_device |
| 6 | AVD boots | `emulator -avd test_device -no-window` | emulator starts |
| 7 | ADB sees emulator | `adb devices` | lists emulator-5554 |
| 8 | Samsung connected | `adb devices` (USB) | lists Samsung serial |
| 9 | Gradle works | `./gradlew tasks` in test project | lists tasks |
| 10 | OpenClaw runs | `node openclaw.mjs --version` | prints version |
| 11 | Gemini API valid | curl test | returns 200 |

### B. OpenClaw Integration

| # | Test | How | Expected |
|---|------|-----|----------|
| 12 | Agent starts | Start gateway, create session | Agent responds |
| 13 | exec tool works | Agent runs `echo hello` | Returns "hello" |
| 14 | exec error feedback | Agent runs `exit 1` | Returns error + exit code |
| 15 | File write works | Agent writes test file | File on disk |
| 16 | Image tool + Gemini | Agent analyses screenshot | Returns description |
| 17 | Skill loaded | System prompt includes android-app-builder | Skill visible |
| 18 | Plugin loaded | Tool list includes android_build | Tool available |
| 19 | Background process | Agent runs long command with yieldMs | Runs in background |

### C. Android Pipeline (Manual Then Autonomous)

| # | Test | How | Expected |
|---|------|-----|----------|
| 20 | Manual APK build | hello-world + `./gradlew assembleDebug` | APK in build/outputs |
| 21 | Manual emulator install | `adb install` APK | App on emulator |
| 22 | Manual launch | `adb shell am start` | App opens |
| 23 | Manual screenshot | `adb exec-out screencap -p > test.png` | PNG captured |
| 24 | Manual logcat | `adb logcat -d *:E` | No FATAL errors |
| 25 | Manual tap | `adb shell input tap 540 960` | Element responds |
| 26 | Manual Samsung install | `adb -s <serial> install` | App on phone |
| 27 | Agent builds APK | "build a calculator app" | APK produced |
| 28 | Agent self-corrects | Introduce typo, ask agent to build | Finds and fixes |
| 29 | Agent emulator test | Agent runs Phase 5-6 | Screenshots analysed |
| 30 | Agent Samsung test | Agent runs Phase 7 | Video recorded |

### D. UI Scaffolding (Crash/Navigation Tests)

| # | Test | Catches |
|---|------|---------|
| 31 | Launch screenshot analysis | Blank/white/black screen |
| 32 | Tap every button | Button crashes app or does nothing |
| 33 | Navigation depth check | Orphan screen (can't go back) |
| 34 | Screen rotation | Layout breaks on rotation |
| 35 | Home + return | App doesn't restore state |
| 36 | Force stop + relaunch | Crash on cold start |
| 37 | Logcat FATAL grep | Uncaught exceptions |
| 38 | ANR detection | App freezes |
| 39 | Memory usage | Excessive RAM (>200MB for simple app) |
| 40 | Frame jank check | >10% janky frames |

---

## 4. End-to-End Flow

```
User: "Build me an Android app that [does X]"
  |
  v
OpenClaw agent (Claude Opus 4.6) + android-app-builder skill
  |
  v
Phase 1: Scaffolding ---- write build files, manifest, wrapper
  |
  v
Phase 2: Code Gen ------- write Kotlin, Compose UI, resources
  |
  v
Phase 3: Compile -------- ./gradlew assembleDebug (retry 10x) [HARD GATE]
  |
  v
Phase 4: APK Verify ----- check exists, size > 500KB, valid contents
  |
  v
Phase 5: Emulator ------- install, launch, logcat crash check, screenshot
  |
  v
Phase 6: UI Smoke ------- tap every element, check navigation, rotation, etc.
  |
  v
Phase 7: Samsung A16 ---- install, test, performance metrics, demo video
  |
  v
DONE: Report APK path, screenshots, issues fixed, demo video
```

---

## 5. Files to Create/Modify

| # | File | Action | Purpose |
|---|------|--------|---------|
| 1 | `~/.zshrc` | Modify | ANDROID_HOME + PATH |
| 2 | `~/.openclaw/config.yaml` | Create/Modify | Agent config, Gemini, approvals |
| 3 | `~/.openclaw/skills/android-app-builder/SKILL.md` | Create | 7-phase pipeline skill |
| 4 | `~/.openclaw/plugins/android-dev/openclaw.plugin.json` | Create | Plugin manifest |
| 5 | `~/.openclaw/plugins/android-dev/index.ts` | Create | Plugin entry, register 3 tools |
| 6 | `~/.openclaw/plugins/android-dev/tools/android-build.ts` | Create | Build + verify wrapper |
| 7 | `~/.openclaw/plugins/android-dev/tools/android-test.ts` | Create | Install + test wrapper |
| 8 | `~/.openclaw/plugins/android-dev/tools/android-logcat.ts` | Create | Crash detection wrapper |
| 9 | `src/agents/tools/image-tool.ts` | Modify | Add Gemini 3 Pro vision provider |
| 10 | `~/.openclaw/exec-approvals.json` | Create/Modify | Pre-approve gradle, adb commands |

---

## 6. Existing OpenClaw Infrastructure Leveraged

All of these are already built and require NO modification:

- **exec tool** (`bash-tools.exec.ts`) — runs shell commands, captures stderr/stdout, returns exit code
- **Error feedback loop** — failed commands return full output to LLM, model self-corrects
- **Tool result truncation** — handles huge Gradle outputs (30% of context window, max 400K chars)
- **process tool** (`bash-tools.process.ts`) — background execution for long builds
- **read/write/edit tools** — file operations for code generation
- **image tool** (`image-tool.ts`) — screenshot analysis (adding Gemini as provider)
- **Browser tool** (`browser-tool.ts`) — Playwright-powered, could test web views
- **Subagent spawning** (`sessions-spawn-tool.ts`) — parallelise build steps
- **Skills system** — loads SKILL.md files, progressive disclosure
- **Plugin SDK** — register custom tools, hooks, lifecycle events
- **Session persistence** — JSONL session files, auto-compaction, long-running context
- **Model failover** — auth profile rotation, model fallback, context reduction
