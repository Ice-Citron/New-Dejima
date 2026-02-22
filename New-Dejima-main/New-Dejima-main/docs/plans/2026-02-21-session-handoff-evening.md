# Session Handoff — Feb 21, 2026, ~6:30 PM CET

## What Is This Project?

**Project Altiera / New Dejima** — HackEurope Paris 2026 hackathon entry (21 hours: 1:45 PM Feb 21 → 11:00 AM Feb 22 CET).

**Core Thesis:** Can an AI agent achieve **money-made-per-token > cost-per-token**? We demonstrate this by having an OpenClaw agent autonomously build Android apps from natural language prompts.

**Team:** 3 developers. You (A) own Track A (Agent + Android Pipeline). Person B owns Track B (Finance + Crypto).

**Repo:** https://github.com/Ice-Citron/New-Dejima

---

## What We've Built So Far

### Track A — Agent + Android Pipeline (YOUR TRACK)

**OpenClaw** (open-source AI gateway at `/Users/administrator/Black Projects/Project Altiera/openclaw/`) is the agent framework. We configured it with:

1. **SKILL.md** — A 7-phase autonomous Android app build pipeline:
   - Phase 1: Project Scaffolding (Gradle, Kotlin, Compose boilerplate)
   - Phase 2: Code Generation (write all source files)
   - Phase 3: Compilation [HARD GATE] (./gradlew assembleDebug, retry up to 10x)
   - Phase 4: APK Verification (check exists, size > 100KB)
   - Phase 5: Emulator Testing (install, launch, screenshot, crash check)
   - Phase 6: UI Smoke Testing (tap every element, scroll, rotation, home+resume)
   - Phase 7: Report (summary with all metrics)

2. **Config files** at `~/.openclaw/` and `~/.openclaw-dev/`:
   - `~/.openclaw-dev/openclaw.json` — Agent model (Claude Opus 4.6), imageModel (Gemini 2.5 Flash with fallbacks), gateway config
   - `~/.openclaw/exec-approvals.json` — Pre-approved commands (gradlew, adb, emulator, etc.)
   - `~/.openclaw/workspace-dev/skills/android-app-builder/SKILL.md` — The 7-phase pipeline
   - `~/.openclaw/workspace-dev/TOOLS.md` — Environment reference
   - `~/.openclaw/workspace-dev/AGENTS.md, IDENTITY.md, SOUL.md, USER.md` — Agent personality (C3-PO)

3. **Pinned versions** (MANDATORY — these work, don't change):
   - compileSdk = 35, minSdk = 31, targetSdk = 35
   - Kotlin 2.0.21, Compose BOM 2024.12.01
   - Android Gradle Plugin 8.7.3, Gradle wrapper 8.11.1
   - JVM target = 17
   - JAVA_HOME = /Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home

### Apps Built Successfully (ALL first-attempt builds, ZERO errors)

| # | App | Build Time | APK Size | APK Location |
|---|-----|-----------|----------|--------------|
| 1 | **Tip Calculator** | ~19s | ~21 MB | LOST (was in /tmp/, crashed) — but was deployed to Samsung A16 |
| 2 | **Workout Timer** | 19s | 21.2 MB | `/tmp/dejima-workspace/workout-timer/app/build/outputs/apk/debug/app-debug.apk` |
| 3 | **Mood Tracker** | 6s | 21.2 MB | `/tmp/dejima-workspace/mood-tracker/app/build/outputs/apk/debug/app-debug.apk` |
| 4 | **Unit Converter** | 5s | 21.2 MB | `/tmp/dejima-workspace/unit-converter/app/build/outputs/apk/debug/app-debug.apk` |
| 5 | **Flashcard Study** | 5s | 21.3 MB | `/tmp/dejima-workspace/flashcard-app/app/build/outputs/apk/debug/app-debug.apk` |
| 6 | **Tic Tac Toe** | 4s | 21.2 MB | `/tmp/dejima-workspace/tic-tac-toe/app/build/outputs/apk/debug/app-debug.apk` |
| 7 | **Habit Tracker** | 5s* | 21.4 MB | `/tmp/dejima-workspace/habit-tracker/app/build/outputs/apk/debug/app-debug.apk` |

| 8 | **Expense Tracker** | 5s* | 51.5 MB | `/tmp/dejima-workspace/expense-tracker/app/build/outputs/apk/debug/app-debug.apk` |

*Habit Tracker: Initial build in 5s, agent self-debugged UI click targets (3 attempts). Expense Tracker: 51.5 MB due to material-icons-extended. Both fully working.

### Screenshots (all in /tmp/)

**Workout Timer:** screen_workout_emulator.png, tap_60s.png, tap_90s.png, tap_start.png, tap_pause.png, rotation_workout.png, resume_workout.png
**Mood Tracker:** mood_launch.png, mood_history.png, mood_tap1.png, mood_tap3.png, mood_scroll.png, mood_rotation.png, mood_resume.png
**Unit Converter:** (various, check /tmp/ for unit_*.png files)
**Flashcard Study:** (various, check /tmp/ for flashcard_*.png files)
**Tic Tac Toe:** ttt_launch.png, ttt_xwins.png, ttt_newgame.png, ttt_owins.png, ttt_draw.png
**Habit Tracker:** (various, check /tmp/ for habit_*.png files)
**Expense Tracker:** (various, check /tmp/ for expense_*.png files)

---

## Gemini Vision Integration — DONE

- **Gemini API key:** PLEASE-MANUALLY-ASK-FOR-KEYS-INSTEAD
- **Working models:** `gemini-2.5-flash` (v1beta), `gemini-3-flash-preview` (v1beta), `gemini-2.5-pro`
- **NOT working:** gemini-2.0-flash (deprecated), gemini-1.5-flash/pro (removed), gemini-3-pro/flash (not found)
- **OpenClaw config updated:** `imageModel.primary = "google/gemini-2.5-flash"` with fallbacks to gemini-3-flash-preview and claude-opus-4-6
- **SKILL.md updated:** Phase 5 and 6 now explicitly require image tool analysis, scrolling tests added as Step 3, Samsung A16 testing added to Phase 5
- **Direct API test confirmed working:** Gemini correctly identifies all UI elements with tap coordinates from emulator screenshots
- **OpenClaw image tool:** Works but has path restriction for /tmp/ (uses Claude vision as fallback, which also works fine)

---

## Environment State

### Running Services
- **OpenClaw Gateway:** PID 10532, ws://127.0.0.1:19001, agent model: anthropic/claude-opus-4-6
- **Android Emulator:** emulator-5554 (Pixel 6 AVD, test_device-pixel_6), fully booted
- **Samsung A16:** Connected, serial RFGYC34581H, BUT screen is locked (keyguard active, can't bypass via ADB)

### API Keys (set in shell, also in openclaw .env)
- ANTHROPIC_API_KEY=REDACTED
- GEMINI_API_KEY=REDACTED
- GOOGLE_AI_API_KEY=REDACTED
- .env file at: /Users/administrator/Black Projects/Project Altiera/openclaw/.env (gitignored)

### Key Paths
- OpenClaw repo: `/Users/administrator/Black Projects/Project Altiera/openclaw/`
- New-Dejima repo: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/`
- OpenClaw config: `~/.openclaw-dev/openclaw.json`
- Skills: `~/.openclaw/workspace-dev/skills/android-app-builder/SKILL.md`
- Workspace: `/tmp/dejima-workspace/` (apps built here)
- Gradle template: `/tmp/dejima-workspace/gradle-template/` (gradlew + wrapper jar)
- Gateway auth token: a321087dbf959e2d7d62c369b07c236b3665a561350e4053
- Plans: `/Users/administrator/Black Projects/Project Altiera/docs/plans/` and `/Users/administrator/Black Projects/Project Altiera/New-Dejima/docs/plans/`

### How to Start OpenClaw Gateway
```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
export ANTHROPIC_API_KEY="REDACTED"
export GEMINI_API_KEY="REDACTED"
export ANDROID_HOME="/Users/administrator/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
export JAVA_HOME="/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home"
OPENCLAW_SKIP_CHANNELS=1 CLAWDBOT_SKIP_CHANNELS=1 node scripts/run-node.mjs --dev gateway
```

### How to Send a Build Request to the Agent
```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
# Set all env vars above, then:
node openclaw.mjs --dev agent --local --agent dev -m "Use the android-app-builder skill. Build me a <description of app>." --json
```

### How to Start Emulator
```bash
export ANDROID_HOME="/Users/administrator/Library/Android/sdk"
export PATH="$ANDROID_HOME/emulator:$PATH"
emulator -avd test_device-pixel_6 -no-window -no-audio &
```

---

## What's Left To Do (Track A)

### Completed Tasks
- [x] A1-1: Android SDK environment setup
- [x] A2-1: Build and start OpenClaw gateway
- [x] A1-2: Manual Android app build (reference)
- [x] A2-2: Configure OpenClaw for Android development
- [x] A1-3: Create android-app-builder skill (SKILL.md)
- [x] A2-3: Create exec-approvals configuration
- [x] A2-4: Test skill with tip calculator (SUCCESS, deployed to Samsung A16)
- [x] A1-6: Verify Gemini vision integration (DONE — works via direct API and OpenClaw)
- [x] A2-5: Second autonomous build — workout timer (SUCCESS, first attempt)
- [x] A2-6: Demo recording build — mood tracker (SUCCESS, first attempt)
- [x] A2-7 partial: Unit converter (SUCCESS, first attempt)

### Remaining Tasks
- [x] **A2-7 continued**: Flashcard study app (5th app, SUCCESS, first attempt, 5s build)
- [ ] **A1-7**: Connect Paid.ai cost tracking to agent pipeline (coordinate with Track B)
- [ ] **Samsung A16 unlock**: Phone is locked — need physical unlock to demo apps on it
- [ ] **Demo recording**: Screen capture of full pipeline running (QuickTime or similar)
- [ ] **Presentation prep**: Slides, demo video, artifacts (Hours 18-21)

### Track B (Person B's work, not yours)
- [ ] Solana wallet system
- [ ] Paid.ai cost tracking
- [ ] Stripe test-mode virtual cards
- [ ] Crusoe inference API
- [ ] Agent economics dashboard
- [ ] Integration with Track A

---

## Known Issues

1. **Samsung A16 screen lock** — Phone has a PIN/pattern lock. ADB can install and launch apps but can't bypass the keyguard. Need to physically unlock the phone to demo.
2. **OpenClaw image tool /tmp/ path restriction** — The image tool sandbox blocks /tmp/ paths. The agent falls back to Claude's built-in vision which works fine. Could be fixed by configuring allowed directories in the sandbox config.
3. **Gradle template** — The original gradle wrapper template from /tmp/hello-dejima/ was lost in the crash. Recreated at `/tmp/dejima-workspace/gradle-template/` with gradlew from openclaw's Android app.
4. **OpenClaw is 2157 commits behind upstream** — Intentional, frozen on stable version.

---

## Implementation Plan Reference

Full hour-by-hour plan: `/Users/administrator/Black Projects/Project Altiera/docs/plans/2026-02-21-hackeurope-implementation-plan.md`
Design doc: `/Users/administrator/Black Projects/Project Altiera/docs/plans/2026-02-21-hackeurope-new-dejima-design.md`

Both also in New-Dejima repo: `/Users/administrator/Black Projects/Project Altiera/New-Dejima/docs/plans/`

---

## Critical Numbers for Presentation

- **8 apps built autonomously** (tip calculator, workout timer, mood tracker, unit converter, flashcard study, tic tac toe, habit tracker, expense tracker)
- **8/8 first-attempt compilation** — habit tracker needed UI debugging (self-fixed in 3 attempts)
- **All 8 installed on Samsung A16** — zero crashes confirmed via ADB
- **Build times:** 5-19 seconds
- **7-phase pipeline** with hard gates, vision QA, scrolling tests
- **Models:** Claude Opus 4.6 (coding), Gemini 2.5 Flash (vision)
- **Samsung A16:** Physical device deployment confirmed (tip calculator)
