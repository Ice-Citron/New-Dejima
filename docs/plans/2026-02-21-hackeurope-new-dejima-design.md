# Design: New Dejima MVP — HackEurope Paris 2026

**Date**: 2026-02-21
**Status**: Approved
**Deadline**: 2026-02-22 11:00 CET (~21 hours from start)
**Team**: 3 developers (Imperial/ETH CS, all running Claude Code Opus 4.6)
**Project**: New Dejima / Project Altiera

---

## Core Thesis

Can a frontier AI model achieve **money-made-per-token > cost-per-token**? We build a controlled sandbox where an AI agent autonomously builds Android apps, tracks its own economics, and holds its own crypto wallet — demonstrating the infrastructure for economically self-sustaining AI.

## Demo North Star

An OpenClaw agent autonomously builds an Android app from a natural language prompt, compiles it, tests it on emulator, validates the UI with vision AI, and produces a working APK — while Paid.ai tracks every cent of cost vs revenue in real-time, and the agent holds its own Solana wallet.

**Narrative to judges:** "The hardest part is done — no-human-in-the-loop app development. Everything else (trading, real estate, SaaS) is just more tool calls."

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     NEW DEJIMA SYSTEM                        │
│                                                              │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────┐  │
│  │  TRACK A        │  │  TRACK B         │  │  TRACK C   │  │
│  │  Agent+Android  │  │  Finance+Crypto  │  │  Marketing │  │
│  │  (2 people)     │  │  (1 person)      │  │  (stretch) │  │
│  └────────┬────────┘  └────────┬─────────┘  └─────┬──────┘  │
│           │                    │                   │         │
│  ┌────────▼────────┐  ┌───────▼──────────┐  ┌─────▼──────┐  │
│  │ OpenClaw Gateway│  │ Solana Devnet    │  │ ElevenLabs │  │
│  │ Claude Opus 4.6 │  │ Wallet per agent │  │ TTS API    │  │
│  │ Gemini 3 Pro    │  │                  │  │            │  │
│  │ (vision)        │  │ Paid.ai          │  │ Miro MCP   │  │
│  │                 │  │ Cost tracking    │  │ (viz)      │  │
│  │ Android SDK     │  │ Revenue signals  │  │            │  │
│  │ ADB + Emulator  │  │                  │  │            │  │
│  │                 │  │ Stripe Issuing   │  │            │  │
│  │ 7-Phase Build   │  │ Virtual cards    │  │            │  │
│  │ Pipeline        │  │ (test mode)      │  │            │  │
│  │                 │  │                  │  │            │  │
│  │                 │  │ Crusoe Inference │  │            │  │
│  │                 │  │ Agent self-host  │  │            │  │
│  └─────────────────┘  └──────────────────┘  └────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## Track A: Agent + Android Pipeline

### Goal
OpenClaw agent receives a natural language prompt, autonomously builds a working Android APK, tests it, and validates UI — zero human intervention.

### Tech Stack
- **Agent framework**: OpenClaw (already built in repo)
- **Coding model**: Claude Opus 4.6 (via Anthropic API)
- **Vision model**: Gemini 3 Pro (via Google AI API)
- **Android**: SDK Platform 36, Build-Tools 36.0.0, Kotlin 2.2.21, Compose BOM 2025.12.00
- **Testing**: Android Emulator (Pixel 6 AVD, API 35 arm64), ADB

### 7-Phase Pipeline (from existing design doc)
1. **Scaffolding** — create project structure, Gradle files, wrapper
2. **Code Generation** — write Kotlin, Compose UI, resources, manifest
3. **Compilation** — `./gradlew assembleDebug` (retry 10x) [HARD GATE]
4. **APK Verification** — check exists, size > 500KB, valid contents
5. **Emulator Testing** — install, launch, logcat crash check, screenshot
6. **UI Smoke Testing** — tap every element, check navigation, rotation
7. **Samsung A16 Testing** — physical device (stretch goal)

### OpenClaw Modifications
- **Layer A**: `~/.openclaw/config.yaml` — android-dev agent, Gemini vision, exec approvals
- **Layer B**: `~/.openclaw/skills/android-app-builder/SKILL.md` — 7-phase pipeline
- **Layer C**: `~/.openclaw/plugins/android-dev/` — android_build, android_test, android_logcat tools

### Deliverables
- Working APK built by agent
- Screenshots of each pipeline phase
- Demo video of agent building an app end-to-end
- Compilation error → self-correction logs

---

## Track B: Finance + Crypto Infrastructure

### Goal
Economic tracking layer that proves the cost vs revenue thesis, with agent-owned crypto wallets and payment rails.

### Components

#### 1. Solana Wallet System
- **SDK**: `@solana/web3.js` v1.x + `@solana/spl-token`
- **Network**: Devnet (not mainnet)
- **Per agent**: Generate keypair at creation, store encrypted
- **Tokens**: Custom SPL token on devnet (simulating USDT)
- **Operations**: Check balance, send/receive, transaction history

#### 2. Paid.ai Cost Tracking
- **SDK**: `@paid-ai/paid-node`
- **Integration**: OpenTelemetry wrapper around all AI API calls
- **Tracks**: Anthropic costs, Gemini costs, compute costs
- **Signals**: Emit when agent completes valuable actions (app built, test passed)
- **Dashboard**: Real-time margin per agent view

#### 3. Stripe Payment Rails (Test Mode)
- **Issuing**: Virtual card per agent with spending controls
- **Connect**: Marketplace revenue split (75% agent / 25% platform)
- **Authorization webhooks**: Real-time approve/deny based on agent budget
- **All in test mode** — no real money, but fully functional demo

#### 4. Crusoe Inference API
- **Endpoint**: `api.crusoe.ai/v1` (OpenAI-compatible)
- **Use case**: Agent calls Crusoe for inference (demonstrating it can use alternative compute)
- **Models**: Llama 3.3 70B, DeepSeek R1 (cheap, demonstrating cost efficiency)
- **MCP Server**: `@crusoeai/cloud-mcp` for agent to discover/manage infra

### Deliverables
- Solana devnet wallet with balance
- Paid.ai dashboard showing cost vs revenue
- Stripe test-mode virtual card transactions
- Crusoe inference API calls from agent

---

## Track C: Marketing Content (Stretch)

### Goal
Agent generates marketing assets for apps it builds.

### Components
- **ElevenLabs**: TTS voiceover for app demos (`eleven_flash_v2_5` model, free tier)
- **Miro MCP**: Agent visualizes its architecture decisions on a Miro board

### Deliverables
- Audio voiceover for app promo
- Miro board with agent's decision tree

---

## Sponsor Integration Depth

| Sponsor | Depth | What We Show |
|---------|-------|-------------|
| **Anthropic** | DEEP | Claude Opus 4.6 is the agent brain. Every line of Kotlin is Claude-generated. Self-correction loops. |
| **DeepMind** | DEEP | Gemini 3 Pro validates every screenshot. Vision-based QA with no human in the loop. |
| **Paid.ai** | DEEP | Full cost tracking. OpenTelemetry wrapping every API call. Revenue signals. Margin dashboard. |
| **Solana** | DEEP | Per-agent wallets. SPL token transfers. Balance tracking. Devnet demo. |
| **Stripe** | MEDIUM | Test-mode virtual cards. Spending controls. Revenue splits via Connect. |
| **Crusoe** | MEDIUM | Managed inference API calls. MCP server for infra discovery. |
| **MiroAI** | LIGHT | Board visualization of agent decisions (stretch). |
| **ElevenLabs** | LIGHT | TTS voiceover for marketing (stretch). |
| **OpenShift AI** | PITCH ONLY | Mentioned as "enterprise deployment path" in presentation. |

---

## What We're NOT Building (Scope Cuts)

- Full Play Store submission (review takes days)
- Mainnet crypto (devnet only)
- Agent reproduction / von Neumann probes (future work)
- Social media content / Sora video (future work)
- Lovable SaaS hosting (if you can build Android, web is strictly easier)
- Full marketplace / country economy simulation
- Benchmark graph (no time)
- Older models (GPT-4, etc.)

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| OpenClaw setup takes >3h | Fallback: raw Claude API + shell scripts |
| Android build fails repeatedly | Build reference app manually first (hour 1-2). Pin ALL versions. |
| Gemini vision gives bad analysis | Fallback to Claude Opus 4.6 vision |
| Solana devnet is slow/down | Pre-generate wallets, cache airdrop SOL |
| Paid.ai SDK issues | Worst case: manual cost logging |
| Stripe Issuing approval blocked | Test mode doesn't need approval |
| Time runs out | Android pipeline is THE demo. If it works by hour 12, everything else is bonus. |

---

## Resources Available

| Resource | Amount |
|----------|--------|
| Azure credits | $20,000 |
| AWS credits | $20,000 |
| OpenAI credits | $5,000 |
| Anthropic credits | $1,000 |
| xAI credits | $6,700 |
| Team | 3 developers, all Claude Code Opus 4.6 |
| Time | ~21 hours |
