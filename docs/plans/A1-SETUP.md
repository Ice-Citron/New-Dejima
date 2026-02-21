# A1 Setup Guide — New Dejima Android Pipeline

Quick reference for what was built and how to use it.

## What Was Created

### 1. Reference App: `android/hello-dejima/`

Jetpack Compose app with pinned versions from A1-SPEC. Build with:

```powershell
cd android\hello-dejima
.\gradlew.bat assembleDebug
```

APK output: `app\build\outputs\apk\debug\app-debug.apk`

Also copied to `%TEMP%\hello-dejima\` for the agent pipeline.

### 2. Skill: `skills/android-app-builder/SKILL.md`

7-phase autonomous pipeline for building Android apps from natural language. Installed to:

- `~/.openclaw/skills/android-app-builder/SKILL.md` (for OpenClaw)
- `skills/android-app-builder/SKILL.md` (version controlled in repo)

### 3. Plugin: `openclaw/extensions/android-dev/`

MVP stub for the android-dev plugin. To use with OpenClaw:

1. Copy `openclaw/extensions/android-dev/` into your OpenClaw repo at `extensions/android-dev/`
2. Adjust `package.json` if OpenClaw uses a different extension format
3. Register the plugin in OpenClaw's plugin loader

## Environment Checklist (A1-SPEC §14)

Before running the agent pipeline:

- [ ] `java --version` >= 17
- [ ] `node --version` >= 22
- [ ] `adb --version` 36.x
- [ ] `emulator -list-avds` shows `test_device`
- [ ] `ANDROID_HOME` set (e.g. `$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"`)
- [ ] OpenClaw cloned and built
- [ ] `ANTHROPIC_API_KEY`, `GOOGLE_AI_API_KEY` set
- [ ] Manual hello-dejima build succeeds

## Test Prompts (A1-SPEC §13)

Once the pipeline is running:

1. **Tip calculator:** "Build me a simple tip calculator app. Input field for bill amount, slider for tip percentage (10–30%), show calculated tip and total."
2. **Workout timer:** "Build me a workout timer app. Preset timers (30s, 60s, 90s), start/pause button, countdown with circular progress."
3. **Mood tracker:** "Build me a mood tracker app. Select mood (Happy, Sad, Neutral, Excited, Tired) with emoji buttons, save with timestamp, show scrollable history."

## Paths (Windows)

| Item        | Path                          |
|-------------|-------------------------------|
| Workspace   | `%TEMP%\dejima-workspace\`     |
| Reference   | `%TEMP%\hello-dejima\`        |
| Skill       | `~/.openclaw/skills/android-app-builder/` |

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/setup-android-env.ps1` | Set ANDROID_HOME, create AVD test_device |
| `scripts/start-emulator.ps1` | Start emulator and wait for boot |
| `scripts/clear-workspace.ps1` | Clear workspace between test prompts |

## OpenClaw Config

Config template: `config/openclaw.json` (also at `~/.openclaw/openclaw.json`).

Adjust `workspace` path if your TEMP differs. Ensure `ANTHROPIC_API_KEY` and `GOOGLE_AI_API_KEY` are set before starting the gateway.

## Run the Agent Pipeline

1. **Set API keys and configure auth** (OpenClaw uses auth-profiles.json, not env vars):
   ```powershell
   $env:ANTHROPIC_API_KEY = "sk-ant-..."
   $env:GOOGLE_AI_API_KEY = "..."   # For vision/screenshot analysis
   .\scripts\setup-auth.ps1
   ```

2. **Start gateway** (Terminal 1):
   ```powershell
   cd c:\Users\nahom\New-Dejima
   .\scripts\run-gateway.ps1
   ```
   Or: `npx openclaw@latest gateway run --port 18789`

3. **Start emulator** (if not already running):
   ```powershell
   .\scripts\start-emulator.ps1
   ```

4. **Send test prompt** (Terminal 2):
   ```powershell
   cd c:\Users\nahom\New-Dejima
   .\scripts\send-test-prompt.ps1
   ```
   Or use interactive TUI: `npx openclaw@latest tui`

5. **Control UI**: http://127.0.0.1:18789 (when gateway is running)
