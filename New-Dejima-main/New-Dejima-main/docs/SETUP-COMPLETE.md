# OpenClaw Setup Complete

## What was configured

### 2. Auth token
- **Token:** `cf6620ddf86e2f1d1be198a85c2bd5d771c4471d9bc048ac`
- **Location:** `%USERPROFILE%\.openclaw\openclaw.json` → `gateway.auth.token`
- **Dashboard:** Paste this token when opening http://127.0.0.1:18789/

### 3. Android SDK paths
- **ANDROID_HOME:** `C:\Users\nahom\AppData\Local\Android\Sdk`
- **PATH:** platform-tools, emulator, cmdline-tools
- **Location:** `openclaw.json` → `skills.entries.android-app-builder.env`

### 4. Test scripts
- **Quick test:** `.\scripts\send-test-prompt.ps1` — sends "Hello" to the agent
- **Workout app:** `.\scripts\send-workout-prompt.ps1` — sends full build prompt

Both scripts auto-load the token from config.

## Before testing

1. **Set API keys** (required for the agent to respond):
   ```powershell
   $env:ANTHROPIC_API_KEY = "sk-ant-..."
   $env:GOOGLE_AI_API_KEY = "..."   # For Gemini vision
   ```

2. **Start the gateway:**
   ```powershell
   .\scripts\run-gateway.ps1
   ```
   Or: `cd openclaw; pnpm gateway:watch`

3. **Run a test:**
   ```powershell
   .\scripts\send-test-prompt.ps1
   ```

## Web dashboard

Open http://127.0.0.1:18789/ and paste the token above when prompted.
