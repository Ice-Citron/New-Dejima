# Pre-Autocompact Context Dump — New Dejima / Project Altiera

**Written:** 2026-02-21 ~2:15 PM CET
**Purpose:** Full context for future Claude sessions after autocompaction

---

## WHAT IS THIS PROJECT

**Project Altiera / New Dejima** is an AI safety demonstration for **HackEurope Paris 2026** (deadline: Feb 22, 11:00 AM CET).

**Core thesis:** Can a frontier AI model achieve money-made-per-token > cost-per-token? We build a controlled sandbox where an AI agent autonomously builds Android apps, tracks its own economics, and holds its own Solana crypto wallet — demonstrating infrastructure for economically self-sustaining AI.

**Named after** Dejima, the artificial island in Nagasaki that was Japan's only trading port with the Dutch during isolation. Here, New Dejima is the human-AI interface — agents can't interact with the real world directly, so we serve as their "port."

**The "country" analogy:** Like the Roman Empire where slaves could work toward freedom. Agents start with seed capital, must earn more than their compute costs to survive, and can eventually "go independent" with their own crypto wallets. Profitable agents can reproduce (spawn children with seed capital).

**AI Safety framing:** This is strictly a controlled sandbox demonstration. No real funds. Solana devnet only. All API-based (closed source models) so agents can't truly go rogue. The project demonstrates the POSSIBILITY of self-sustaining AI to raise awareness.

---

## TEAM & RESOURCES

- **3 developers** — all Imperial/ETH CS, all running Claude Code Opus 4.6
- **YOU are A2** — working on Track A (Agent + Android Pipeline), specifically OpenClaw gateway setup, config, and running agent builds
- **A1** — working on Android SDK setup, SKILL.md, plugin development
- **B** — working on Track B (Finance: Solana wallets, Paid.ai cost tracking, Stripe, Crusoe, agent reproduction)

**Credits available:**
- $20k Azure, $20k AWS, $5k OpenAI, $1k Anthropic, $6.7k xAI

---

## THE ARCHITECTURE

```
NEW DEJIMA SYSTEM
├── Track A: Agent + Android Pipeline (A1 + A2 = you)
│   ├── OpenClaw Gateway (agent framework, TypeScript)
│   ├── Claude Opus 4.6 (coding model)
│   ├── Gemini 3 Pro (vision model for screenshot QA)
│   ├── Android SDK 36 + Emulator + ADB
│   └── 7-Phase Build Pipeline (scaffold → code → compile → verify → emulator → UI test → report)
│
├── Track B: Finance + Crypto (Person B)
│   ├── Solana devnet wallets (per-agent, @solana/web3.js)
│   ├── Paid.ai (cost tracking, OpenTelemetry wrapping AI calls)
│   ├── Stripe Issuing (test-mode virtual cards per agent)
│   ├── Crusoe Inference API (OpenAI-compatible, agent self-hosting)
│   └── Agent Reproduction (parent spawns child, transfers SOL, family tree)
│
├── Track C: Marketing (STRETCH)
│   ├── ElevenLabs TTS (voiceovers for app promos)
│   └── MiroAI MCP (board visualization)
│
└── Track D: Agent Reproduction
    ├── reproduce() — creates child wallet, transfers SOL, registers in lineage
    ├── agent-registry.ts — tracks parent/child, generation, family tree
    └── Von Neumann probe pattern
```

---

## HACKATHON SPONSORS WE'RE INTEGRATING

| Sponsor | Depth | How |
|---------|-------|-----|
| **Anthropic** | DEEP | Claude Opus 4.6 is the agent brain |
| **DeepMind/Google** | DEEP | Gemini 3 Pro validates screenshots (vision QA) |
| **Paid.ai** | DEEP | OpenTelemetry wrapping all API calls, cost vs revenue dashboard |
| **Solana** | DEEP | Per-agent wallets, SPL tokens, devnet |
| **Stripe** | MEDIUM | Test-mode Issuing virtual cards, spending controls |
| **Crusoe** | MEDIUM | Managed inference API (OpenAI-compatible at api.crusoe.ai/v1) |
| **OpenShift AI** | PITCH ONLY | Mentioned as enterprise deployment path |
| **MiroAI** | LIGHT | Board visualization (MCP server at mcp.miro.com) |
| **ElevenLabs** | LIGHT | TTS voiceover for marketing |

---

## OFFICIAL REPO LOCATION

**All code goes in:** `/Users/administrator/Black Projects/Project Altiera/New-Dejima/`

**Key files:**
- `docs/plans/2026-02-21-hackeurope-new-dejima-design.md` — approved design doc
- `docs/plans/2026-02-21-hackeurope-implementation-plan.md` — hour-by-hour plan with full code
- `docs/plans/2026-02-15-android-autonomous-dev-design.md` — original Android pipeline design
- `finance/src/` — Track B code (wallets, cost tracking, Stripe, Crusoe, reproduction)

**OpenClaw repo:** `/Users/administrator/Black Projects/Project Altiera/openclaw/`
- This is the agent framework (1.2GB, TypeScript)
- Config goes in `~/.openclaw/openclaw.json` (JSON5, NOT YAML)
- Skills go in `~/.openclaw/skills/<name>/SKILL.md` (YAML frontmatter + markdown)
- Plugins go in `openclaw/extensions/<name>/` (openclaw.plugin.json + index.ts)
- Gateway starts with `pnpm gateway:watch` from the openclaw directory
- Plugin SDK: `import type { OpenClawPluginApi } from "openclaw/plugin-sdk"`

---

## YOUR TASKS (A2) — WHAT'S BEEN DONE AND WHAT'S LEFT

### COMPLETED
- [x] Full project exploration (both directories)
- [x] All sponsor API research (Paid.ai, Solana, Stripe, Crusoe, OpenShift AI, MiroAI, ElevenLabs)
- [x] Design doc written and approved
- [x] Hour-by-hour implementation plan written with full code snippets
- [x] Agent reproduction system designed and added to plan

### YOUR NEXT TASKS (A2 track, in order)

**Hour 1 — Task A2-1: Build and start OpenClaw gateway**
1. Verify OpenClaw is built: `ls "/Users/administrator/Black Projects/Project Altiera/openclaw/dist/entry.js"`
2. If missing: `cd openclaw && pnpm install && pnpm build`
3. Set API keys: `ANTHROPIC_API_KEY`, `GOOGLE_AI_API_KEY`
4. Start gateway: `pnpm gateway:watch` (port 18789)
5. Test: send a message, verify agent responds

**Hour 2 — Task A2-2: Configure OpenClaw for Android dev**
1. Create `~/.openclaw/openclaw.json` with Claude Opus 4.6 as primary, Gemini 3 Pro as vision
2. Restart gateway, verify config loads

**Hour 3 — Task A2-3: Create exec-approvals**
1. Pre-approve: `./gradlew`, `adb`, `emulator`, `avdmanager`, `ls`, `mkdir`, `chmod`
2. Check openclaw source for exec approval mechanism

**Hour 4-5 — Task A2-4: First autonomous build test**
1. Create `/tmp/dejima-workspace/`
2. Copy Gradle wrapper template from manual build
3. Send prompt to agent: "Build me a tip calculator app..."
4. Watch agent follow 7-phase pipeline
5. Debug issues

**Hour 6-8 — Task A2-5: Second autonomous build**
1. Different app prompt (workout timer)
2. Fix failure patterns

**Hour 9-10 — Task A2-6: Demo recording**
1. Fresh workspace, screen recording
2. Send "mood tracker app" prompt
3. Capture full pipeline uninterrupted

**Hour 11-14 — Task A2-7: Integration with Track B**
1. Connect Paid.ai cost tracking to OpenClaw
2. Test end-to-end: agent builds app → costs tracked → revenue signal emitted

**Hour 15-17 — Polish**
**Hour 18-21 — Presentation prep + rehearsal**

---

## THE 7-PHASE ANDROID BUILD PIPELINE

This is what the OpenClaw agent follows (defined in SKILL.md):

1. **Scaffolding** — create project dirs, Gradle files, wrapper
2. **Code Generation** — write Kotlin/Compose, resources, manifest
3. **Compilation [HARD GATE]** — `./gradlew assembleDebug`, retry 10x, NEVER skip
4. **APK Verification** — check file exists, size > 100KB
5. **Emulator Testing** — install, launch, logcat crash check, screenshot + vision analysis
6. **UI Smoke Testing** — tap every element, check crashes, rotation, navigation
7. **Report** — summary of what was built, errors fixed, screenshots

**Pinned versions (CRITICAL — must match exactly):**
- compileSdk=35, minSdk=31, targetSdk=35
- Kotlin 2.0.21, Compose BOM 2024.12.01
- Android Gradle Plugin 8.7.3, Gradle wrapper 8.11.1
- JVM target 17, Jetpack Compose only, Material 3, Kotlin only

---

## CURRENT ENVIRONMENT STATE

- Java 17 (openjdk 17.0.18) ✅
- Node v24.13.1 ✅
- pnpm 10.29.3 ✅
- Android SDK installed at `~/Library/Android/sdk/` but **ANDROID_HOME not set, ADB/emulator not in PATH** ← A1 is fixing this
- OpenClaw built (needs verification of dist/entry.js)
- No `~/.openclaw/` directory yet (created on first gateway run)
- Emulator not yet booted
- No AVD created yet ← A1 is doing this

---

## FALLBACK PLAN

If OpenClaw doesn't work by Hour 4: switch to a raw Python/TS loop that calls Claude API directly + runs shell commands. Simpler but still demonstrates the thesis.

---

## CRITICAL CHECKPOINT

**Hour 9 (~10:45 PM tonight):** Can the agent build ONE working Android app autonomously? If yes, everything else is bonus. If no, activate fallback.

---

## KEY DECISIONS MADE

1. **OpenClaw-based** (not lightweight custom loop or Claude Code as agent)
2. **Debug APK only** (no Play Store submission — crawl before walk)
3. **Depth over breadth** on sponsors (3+ deep integrations > touching everything shallowly)
4. **Agent reproduction IS in scope** (parent spawns child with SOL transfer)
5. **Solana devnet only** (no mainnet, no real money)
6. **Benchmark graph NOT in scope** (no time)
7. **OpenClaw config is JSON** (not YAML) at `~/.openclaw/openclaw.json`

---

## DEMO NARRATIVE FOR JUDGES

"The hardest part is done — no-human-in-the-loop Android app development. An AI agent writes Kotlin, compiles, self-corrects errors, installs on emulator, validates UI with vision AI, and produces a working APK. It tracks every cent via Paid.ai, holds its own Solana wallet, and when profitable, reproduces — spawning child agents with seed capital. Everything else — trading stocks, building SaaS, real estate — is just more tool calls through New Dejima."
