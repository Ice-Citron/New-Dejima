# Session Handoff: Project Altiera / New Dejima
**Date**: 15 Feb 2026
**Purpose**: Complete context dump so a new Claude session can resume seamlessly.
**Instruction to new session**: Read this entire file. It contains everything you need.

---

## 1. WHAT IS THIS PROJECT

**New Dejima** is an AI safety research project. The core thesis: can an AI agent earn more money than it costs to run? We're building infrastructure to test this.

The system is modelled after the Roman Empire's slave economy — agents are "born" with seed capital ($1-10k), charged for their compute costs (API/server), and must earn enough to survive. Successful agents earn into their own crypto wallets and can eventually "go free" (run independently on the internet). The human operators act as an interface ("port") between AI agents and the real world — like the historical island of Dejima in Nagasaki that let isolationist Japan trade with the Dutch.

**First milestone**: An agent autonomously builds an Android app, compiles it, tests it, and produces a working APK. We're NOT deploying to Play Store yet (no Play Console account approved). Just producing a valid debug APK and testing it on emulator + physical device.

**Why OpenClaw**: OpenClaw is an open-source AI gateway/assistant framework (similar to Claude Code). It has a full agent runtime, 60+ tools, plugin system, and the exact infrastructure we need. We're studying it deeply and building on top of it.

**Backing**: YC, AWS ($20k credits), Azure ($20k credits), OpenAI ($5k credits), Anthropic ($1k credits), xAI ($6.7k credits).

**Team**: 2 developers. Tech stack: Python, TypeScript, Next.js, Kotlin (Android).

**Timeline**: 60 hours total across 4 weekends.

---

## 2. FILE LOCATIONS

### Project Structure
```
/Users/administrator/Black Projects/Project Altiera/
├── openclaw/                          # OpenClaw repository (the FOSS codebase we're building on)
├── docs/
│   └── plans/
│       ├── 2026-02-15-android-autonomous-dev-design.md   # APPROVED design doc
│       └── SESSION-HANDOFF-15-Feb-2026.md                # THIS FILE
```

### Previous Session Notes (08 Feb 2026)
```
/Users/administrator/Black Projects/Project-Liberty/Copied/Project Altiera/
├── 08-Feb-original-prompt.ipynb       # Original vision/prompt for New Dejima
├── 08-Feb-session-resume.ipynb        # Summary of what was done/not done on 08 Feb
├── 08-Feb-storage.ipynb               # Competitive landscape research results
├── 08-Feb.ipynb                       # Full study notes (large, 41K tokens)
```

### Old Copy of OpenClaw (DO NOT USE)
```
/Users/administrator/Black Projects/Project-Liberty/Copying/openclaw/   # OLD LOCATION, IGNORE
```

### OpenClaw Key Source Paths (inside the repo)
```
openclaw/
├── src/agents/                         # Agent runtime (~300+ files) - THE BRAIN
│   ├── pi-embedded-runner/run.ts       # Main agent loop entry: runEmbeddedPiAgent() line 137
│   ├── pi-embedded-runner/run/attempt.ts  # Core turn logic: runEmbeddedAttempt() line 140
│   ├── system-prompt.ts                # System prompt builder: buildAgentSystemPrompt() line 164
│   ├── openclaw-tools.ts               # 22 core tools created here: createOpenClawTools() line 22
│   ├── bash-tools.exec.ts              # Bash execution: createExecTool() line 800
│   ├── bash-tools.process.ts           # Background processes: createProcessTool() line 44
│   ├── pi-tools.ts                     # Coding tools: createOpenClawCodingTools() line 115
│   ├── model-selection.ts              # Model selection & aliases
│   ├── model-fallback.ts               # Failover chain: runWithModelFallback() line 207
│   ├── agent-scope.ts                  # Agent config resolution line 75
│   ├── tools/
│   │   ├── browser-tool.ts             # Playwright browser automation (725 lines)
│   │   ├── image-tool.ts               # Vision analysis (450 lines) - NEEDS GEMINI ADDITION
│   │   ├── nodes-tool.ts               # Android/device control (492 lines)
│   │   ├── canvas-tool.ts              # Canvas UI rendering (180 lines)
│   │   ├── sessions-spawn-tool.ts      # Subagent spawning line 83
│   │   ├── message-tool.ts             # Messaging
│   │   └── web-tools.ts               # web_search, web_fetch
│   ├── skills/workspace.ts             # Skill loading from 4 sources (line 99)
│   └── pi-embedded-subscribe.handlers.tools.ts  # Tool result processing (line 148)
├── src/memory/                         # Memory system (deep-dived on 08 Feb)
│   ├── manager.ts                      # 77KB main memory manager
│   ├── hybrid.ts                       # Vector + keyword hybrid search
│   ├── embeddings.ts                   # 4 embedding backends
│   └── memory-schema.ts               # SQLite schema
├── src/plugins/                        # Plugin SDK
│   ├── loader.ts                       # Plugin lifecycle (line 169)
│   ├── types.ts                        # OpenClawPluginApi (line 243)
│   └── tools.ts                        # Plugin tool registration (line 43)
├── src/browser/                        # Playwright integration
│   └── pw-session.ts                   # CDP connection (line 335)
├── apps/android/                       # OpenClaw's own Android app (66 Kotlin files)
│   ├── app/build.gradle.kts            # Build config (compileSdk 36, Kotlin 2.2.21)
│   ├── app/src/main/java/ai/openclaw/android/
│   └── gradlew                         # Gradle wrapper 9.2.1
├── extensions/                         # 30+ channel plugins (Discord, Telegram, etc.)
├── skills/                             # 50+ bundled skills
└── Dockerfile                          # Sandbox Docker image
```

---

## 3. WHAT WE DID ON 08 FEB (Previous Session)

1. **Full OpenClaw repo walkthrough** — explored all major directories, understood architecture
2. **Deep dive: Memory system** — read every file in `src/memory/`, documented 8 layers
3. **Competitive landscape research** — found nobody has built the full agent-earns-money-to-survive loop. Pieces exist (Skyfire payments, Virtuals Protocol, ElizaOS trading, Truth Terminal memecoin). EA Forum estimates self-sustaining agents viable late 2027 - mid 2028.
4. **Project decision** — New Dejima became the third project (alongside drone interceptors + robotic arms)
5. **DID NOT** answer the brainstorming question (A/B/C for first demo)
6. **DID NOT** write any code
7. **DID NOT** write implementation plans

---

## 4. WHAT WE DID ON 15 FEB (This Session)

### Deep Codebase Exploration (4 parallel agents)

Thoroughly explored 4 major OpenClaw subsystems with detailed findings:

**A. Agent Runtime** (`src/agents/`, ~300+ files):
- The agent loop is event-driven, not a while-true loop
- Entry: `runEmbeddedPiAgent()` → `runEmbeddedAttempt()` → SDK streams events back
- Multi-level failover: auth profile rotation → model fallback → context reduction (compaction, truncation)
- Subagents spawn async, run independently, can be queried by parent
- Session state persisted as JSONL, auto-compacted when context exceeds window
- System prompt built with 25+ sections, supports "full"/"minimal"/"none" modes
- Config layers cascade: global defaults → per-agent → runtime → subagent

**B. Tool System**:
- 22 core tools + coding tools (read/write/edit/exec/process)
- Bash exec has 3 modes: sandbox (Docker), gateway (host), node (remote device)
- Approval system with allowlists, per-agent permissions, 120s timeout
- PTY support for interactive CLIs (important for Gradle)
- Background execution via `yieldMs` or `background=true` (important for slow Android builds)
- Plugin tools registered dynamically

**C. Android App** (`apps/android/`, 66 Kotlin files):
- Jetpack Compose + Material 3, Kotlin, compileSdk 36
- WebSocket to gateway (OkHttp3), auto-discovery via mDNS
- Ed25519 auth, encrypted preferences
- Device capabilities: camera, screen record, location, SMS
- Build: `./gradlew :app:assembleDebug` → output `openclaw-{version}-debug.apk`
- CI: Java 21 + Android SDK + Gradle 8.11.1 on GitHub Actions

**D. Skills & Plugin SDK**:
- Skills are markdown files (SKILL.md) with YAML frontmatter
- Loaded from 4 sources: bundled > extra > managed (`~/.openclaw/skills`) > workspace
- Progressive disclosure: metadata always loaded, body on trigger, resources on demand
- Plugins defined via `openclaw.plugin.json` manifest
- Plugin API: registerTool, registerHook, registerChannel, registerGatewayMethod, etc.
- 15+ lifecycle hooks: before_agent_start, before_tool_call, after_tool_call, message_received, etc.

### Scaffolding & Testing Deep Dive (2 parallel agents)

**E. Error Feedback Loop** (already built into OpenClaw):
- stderr/stdout captured continuously during command execution
- Non-zero exit code → full output + "Command exited with code X" returned to LLM
- Model sees complete error, can read stacktrace, fix code, retry
- Tool result truncation: max 30% of context window, hard cap 400K chars
- Truncation warning appended so model knows it's incomplete
- NO explicit system prompt instructions to run tests / verify compilation — model CAN do it but isn't TOLD to. This is our scaffolding gap.

**F. Browser/UI Testing Capabilities** (already built):
- Playwright integration via CDP (chromium.connectOverCDP)
- Browser tool: click, type, press, hover, drag, select, fill, resize, wait, evaluate JS
- Snapshots: AI-readable DOM with element refs (e1, e2) for targeting
- Screenshots: full page or element, PNG/JPEG, max 2000px, auto-compression
- Image tool: send screenshots to vision models (Claude, GPT, MiniMax — we're adding Gemini)
- Nodes tool: camera_snap, screen_record, run shell commands ON device, invoke commands
- No ADB/Android-specific tooling exists — we build this via skill + plugin

### Design Decisions Made

User chose:
- **App type**: Agent's choice (not prescribed — scaffolding must handle unpredictable structure)
- **Test targets**: Both emulator AND Samsung A16 physical device
- **Runtime**: Full OpenClaw gateway (not standalone script)
- **Vision model**: Gemini 3 Pro configured as OpenClaw's image tool provider
- **Scaffolding approach**: All three layers (system prompt + skill + plugin)

### Design Document Written & Approved

Full design at: `/Users/administrator/Black Projects/Project Altiera/docs/plans/2026-02-15-android-autonomous-dev-design.md`

Covers: prerequisites, 7-phase pipeline, 4 layers of OpenClaw modifications, 40 tests across 4 categories, end-to-end flow diagram, files to create/modify.

---

## 5. WHAT IS LEFT TO DO

### Immediate (Environment Setup)
- [ ] Add ANDROID_HOME and PATH to `~/.zshrc`
- [ ] Create AVD: `avdmanager create avd -n "test_device" -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_6"`
- [ ] Get Gemini API key from ai.google.dev
- [ ] Enable USB debugging on Samsung A16
- [ ] Verify: `adb --version`, `emulator -list-avds`, `adb devices`

### Layer A: OpenClaw Configuration
- [ ] Create/modify `~/.openclaw/config.yaml` — define `android-dev` agent, add Gemini provider, set model preferences
- [ ] Create/modify `~/.openclaw/exec-approvals.json` — pre-approve `./gradlew`, `adb`, `emulator`, `aapt2` commands

### Layer B: Skill Creation
- [ ] Create `~/.openclaw/skills/android-app-builder/SKILL.md` with 7-phase pipeline
- [ ] Test that skill loads into agent's system prompt

### Layer C: Plugin Creation
- [ ] Create `~/.openclaw/plugins/android-dev/openclaw.plugin.json`
- [ ] Create `~/.openclaw/plugins/android-dev/index.ts` — register 3 tools
- [ ] Create `android-build.ts` — gradle build + APK verify wrapper
- [ ] Create `android-test.ts` — install + launch + screenshot + vision wrapper
- [ ] Create `android-logcat.ts` — crash detection wrapper
- [ ] Test that plugin loads and tools appear in agent's tool list

### Layer D: Image Tool Modification
- [ ] Check if Gemini 3 Pro is already supported as a provider in OpenClaw's models config
- [ ] If not: modify `src/agents/tools/image-tool.ts` to add Gemini 3 Pro vision provider
- [ ] Test: agent takes screenshot, sends to Gemini, gets description back

### Testing (40 tests defined in design doc)
- [ ] Run prerequisites verification tests (11 tests)
- [ ] Run OpenClaw integration tests (8 tests)
- [ ] Run manual Android pipeline tests (10 tests — do manually first)
- [ ] Run autonomous Android pipeline tests (agent does it)
- [ ] Run UI scaffolding tests (10 tests — crash, navigation, rotation, etc.)

### End-to-End Validation
- [ ] Ask agent: "Build me an Android app" (agent's choice)
- [ ] Agent completes all 7 phases autonomously
- [ ] Working APK produced, tested on emulator and Samsung A16
- [ ] Demo video recorded from Samsung

### NOT started / Future work
- [ ] Write the full 60-hour plan (only the first milestone is designed)
- [ ] Play Store deployment (waiting for Play Console approval)
- [ ] Crypto wallet integration (seed capital, earnings, bills)
- [ ] Multi-agent economy (agents hiring agents, companies within Dejima)
- [ ] Benchmark: money-made-per-token vs cost-per-token across models
- [ ] Analyse $BUNKER (moltbunker-sdk) at `/Users/administrator/Imperial-College-London/Projects/2026/2026-02 February/Project Altiera/resources/moltbunker-sdk`
- [ ] Analyse Grok 4.20 situational awareness methods
- [ ] Consider pi-mono integration

---

## 6. KEY TECHNIQUES & ARCHITECTURE

### How OpenClaw's Agent Loop Works
```
User message → runEmbeddedPiAgent() → while(true) retry loop
  → runEmbeddedAttempt() → load session → build tools → build system prompt
  → create agent session → subscribe to streaming events → call LLM SDK
  → events stream back: message_start/update/end, tool_execution_start/update/end
  → tool results accumulated → session persisted (JSONL)
  → on error: auth rotation → model fallback → context compaction → tool truncation
  → on success: break, return payloads
```

### How the Error Feedback Loop Works (Critical for Self-Correction)
```
Agent calls exec: "./gradlew assembleDebug"
  → command runs, stdout+stderr captured continuously
  → exit code checked: code === 0 ? success : failure
  → on failure: full output + "Command exited with code 1" returned to LLM
  → LLM reads Gradle stacktrace, identifies file:line, fixes code, retries
  → no auto-retry logic — model must decide to retry (our skill instructs this)
```

### How Vision-Based UI Testing Works
```
Agent runs: adb exec-out screencap -p > /tmp/screen.png
  → image tool sends screenshot to Gemini 3 Pro
  → Gemini analyses: "List all clickable elements with x,y coordinates"
  → Agent taps each element: adb shell input tap X Y
  → After each tap: screenshot → crash check (logcat) → vision verify
  → Press back → verify return to previous screen
  → Edge cases: rotation, home+return, force-stop+relaunch
```

### The 7-Phase Pipeline
1. **Scaffolding** — create project structure with pinned versions (compileSdk 36, Kotlin 2.2.21, Compose BOM 2025.12.00, Gradle 9.2.1)
2. **Code Generation** — write all Kotlin, resources, manifest
3. **Compilation** — `./gradlew assembleDebug`, retry up to 10x on failure (HARD GATE)
4. **APK Verification** — check exists, size > 500KB, contains classes.dex
5. **Emulator Testing** — install, launch, crash check, screenshot analysis
6. **UI Smoke Testing** — tap every element, navigation integrity, rotation, home+return, force-stop
7. **Samsung A16 Testing** — same + performance metrics + demo video

### OpenClaw's Existing Tools We Use (No Modification Needed)
- `exec` — shell commands (gradle, adb)
- `process` — background execution for long builds
- `read` / `write` / `edit` — file operations
- `image` — screenshot analysis (adding Gemini provider)
- `browser` — Playwright (for web-view testing if needed)
- `sessions_spawn` — subagent spawning
- Skills system — our pipeline instructions
- Plugin SDK — our custom wrapper tools

### What We Add
- **Skill**: `android-app-builder` — 7-phase markdown instructions
- **Plugin tools**: `android_build`, `android_test`, `android_logcat` — structured wrappers
- **Gemini provider**: in image-tool.ts or via config
- **Exec approvals**: pre-approve gradle/adb commands

---

## 7. CURRENT ENVIRONMENT STATE

```
Machine: macOS 26.2, Apple Silicon (arm64)
Node: v24.13.1 (via nvm, default set to 24)
pnpm: 10.23.0
Java: OpenJDK 25.0.1
Docker: 29.1.1
Homebrew: installed
Android SDK: ~/Library/Android/sdk (platform-tools, build-tools 36, emulator, platforms 36, system-image API 35 arm64)
ANDROID_HOME: NOT SET (needs ~/.zshrc addition)
ADB: installed but NOT in PATH
Emulator: installed but NOT in PATH
AVD: NOT CREATED
Samsung A16: not yet configured for USB debugging
OpenClaw: built at /Users/administrator/Black Projects/Project Altiera/openclaw
Playwright: installed
```

---

## 8. RESUME INSTRUCTIONS

When starting a new chat:

1. Paste this file as context
2. Point Claude at the OpenClaw repo: `/Users/administrator/Black Projects/Project Altiera/openclaw`
3. Point at the design doc: `/Users/administrator/Black Projects/Project Altiera/docs/plans/2026-02-15-android-autonomous-dev-design.md`
4. Tell Claude to invoke `/superpowers:writing-plans` to create a step-by-step implementation plan from the design doc
5. Then execute the plan

The design is already approved. No brainstorming needed. Go straight to implementation planning.
