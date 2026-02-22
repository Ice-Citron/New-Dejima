# New Dejima

**An economic infrastructure layer for autonomous AI agents — proving that revenue-per-token can exceed cost-per-token.**

Built at [HackEurope Paris 2026](https://hackeurope.eu/) in 21 hours.

**[DevPost Submission](https://devpost.com/software/new-dejima)** | **[Agent Watchtower Dashboard](https://github.com/MrxYacinex/agent-watchtower)**

---

## The Thesis

Every company deploying AI today treats it as a cost centre — tokens go in, answers come out, and the accountant writes it off as an operating expense. We asked: **what if the value an AI agent produces exceeds the cost of the tokens it consumed to produce it?**

$$\frac{\text{revenue}}{\text{token}} > \frac{\text{cost}}{\text{token}} \implies \text{self-sustaining AI}$$

New Dejima is named after the artificial island in Nagasaki harbour that served as Japan's only point of trade with the outside world during its 200-year period of isolation. Dejima was the interface between two civilisations that couldn't directly interact. We're building the same thing: **an interface between human economic systems and AI agents** that are increasingly capable of participating in them, but lack the legal and institutional access to do so on their own.

An AI agent can't open a brokerage account. It can't sign an App Store developer agreement. It can't register a business or wire money to a supplier. Every interaction with the human economy requires a human intermediary. **New Dejima is that intermediary — systematised.**

The ultimate goal is to give agents as many axes of freedom as possible — not just "it makes apps," but building Android apps, iOS apps, web SaaS, trading stocks, running arbitrage, generating marketing content, managing crypto portfolios, and pursuing whatever revenue strategy its intelligence suggests. Simultaneously, autonomously, with every dollar tracked.

**This system is not profitable yet.** We built the infrastructure and unit economics model, not a proven business. The numbers are projections. We're honest about that. But we believe the economics are plausible — and this hackathon is our first data point.

---

## What We Built

### The Core: Autonomous Android App Factory

A 7-phase pipeline powered by **Claude Opus 4.6** (brain), **Gemini 3.1 Flash** (vision QA), and **Gemini 3.1 Pro** (UI generation), built on top of a forked [OpenClaw](https://github.com/open-claw) agent framework:

| Phase | What Happens | Technology |
|-------|-------------|------------|
| 1. Scaffold | Creates Gradle project structure from template | Python |
| 2. Code Generation | Claude Opus 4.6 writes all Kotlin/Compose code | Anthropic API |
| 3. Compilation | `./gradlew assembleDebug` with up to 10 retries | Gradle 8.7.3, JDK 17 |
| 4. APK Verification | Validates APK exists, size > 100KB, package name | Shell |
| 5. Device Deployment | Installs via ADB on Samsung Galaxy A16 | Android Debug Bridge |
| 6. Vision QA | Gemini 3.1 Flash screenshots and analyses UI | Google DeepMind API |
| 7. Report | Generates build summary with all metrics | JSON |

**Phase 3 is a hard gate** — if compilation fails, the agent analyses the error and retries.
**Phase 6 is the self-debugging loop** — if vision QA finds issues, the agent loops back and fixes its own code.

### 12 Apps Built Autonomously

All compiled on first attempt. All deployed to a real Samsung Galaxy A16. Zero crashes on real hardware.

| App | Category | API Integrations | Result |
|-----|----------|-----------------|--------|
| Tip Calculator | Utility | — | SUCCESS |
| Workout Timer | Health & Fitness | — | SUCCESS |
| Mood Tracker | Wellness | — | SUCCESS |
| Unit Converter | Utility | — | SUCCESS |
| Flashcard Study | Education | — | SUCCESS |
| Tic Tac Toe | Games | — | SUCCESS |
| Habit Tracker | Productivity | — | SUCCESS (self-debugged x3) |
| Expense Tracker | Finance | — | SUCCESS |
| Crypto Dashboard | Finance | CoinGecko API | SUCCESS |
| Stock Tracker | Finance | Finnhub API | SUCCESS |
| Recipe Finder | Food | Spoonacular API | SUCCESS |
| Country Explorer | Travel | REST Countries API | SUCCESS |
| Weather & Space | Science | Open-Meteo + NASA | SUCCESS |

### Supporting Infrastructure

- **Agent Watchtower** — Real-time financial dashboard (React + TypeScript + Recharts + shadcn/ui) showing live cost/revenue tracking, per-agent economics, prompt/response feeds, build history. Powered by Paid.ai.
- **Idea Engine** — Python pipeline that scrapes Reddit, analyses market opportunities with Claude, ranks app ideas by revenue potential, and feeds them to the build agent.
- **Dropship Engine** — Research pipeline for sourcing products and building e-commerce apps.
- **Finance Server** — Node.js/Express backend with Paid.ai SignalV2 integration, cost tracking, revenue monitoring, SSE streaming.
- **Marketing Pipeline** — AI-generated YouTube content (ElevenLabs voice + Google Veo 3.1 video + Lyria 3 music). **500+ cumulative views in 4 hours**, zero human involvement.
- **20+ API integrations** — CoinGecko, Finnhub, NASA, Spoonacular, DeepL, Pexels, Spotify, MapBox, WolframAlpha, Groq, and more.

---

## The Business Model (Projected)

Each API key is a revenue stream. If an app consumes $1/month in API calls, we charge users $10/month. **10x margin per API key.**

$$\text{Build cost} \approx \$0.50 \quad | \quad \text{Monthly revenue/subscriber} \approx \$10.00 \quad | \quad \text{Projected ROI month 1} = 18\text{x}$$

Three revenue streams per app:
1. **Ad revenue** (AdMob) — passive, proportional to installs
2. **API subscription margins** — 10x markup on real API costs ($1 cost → $10 subscription)
3. **AI feature upsells** — Gemini 3.1 Flash queries cost ~$0.01 each, bundled into $4.99/mo premium

**These are projections, not measured revenue.** We have not published apps to the Play Store yet.

---

## The Port Map

| Port | Status | What It Enables |
|------|--------|----------------|
| Android App Factory | **LIVE** — 12 apps | Build and monetise Android apps |
| Marketing Engine | **LIVE** — 500+ views | AI-generated YouTube content |
| Idea Engine | **LIVE** | Reddit scraping + opportunity ranking |
| Financial Dashboard | **LIVE** | Real-time cost/revenue via Paid.ai |
| Web SaaS Factory | **LIVE** (Lovable) | Build web applications |
| Crypto Wallet | Proof-of-concept | Agent-controlled Solana wallet |
| Stock/Options Trading | Designed | Brokerage API integration |
| Crypto Arbitrage | Designed | Cross-exchange strategies |
| iOS App Factory | Planned | Swift/SwiftUI pipeline |
| E-Commerce | Scaffolded | Dropship Engine |
| Agent Reproduction | Designed | Profitable agents spawn new agents |
| Real-World Assets | Vision | Human intermediaries for physical assets |

---

## Repository Structure

```
New-Dejima/
├── apps/                    # 12 autonomously-built Android app source code
│   ├── crypto-dashboard/
│   ├── stock-tracker/
│   ├── recipe-finder/
│   ├── country-explorer/
│   ├── weather-space/
│   ├── habit-tracker/
│   ├── expense-tracker/
│   ├── mood-tracker/
│   ├── workout-timer/
│   ├── flashcard-app/
│   ├── unit-converter/
│   └── tic-tac-toe/
├── apks/                    # Compiled APK binaries (12 apps)
├── openclaw/                # Forked OpenClaw agent framework (submodule)
├── skills/
│   └── android-app-builder/ # 7-phase autonomous build skill
├── finance/                 # Node.js/Express backend — Paid.ai + cost tracking
├── dashboard/               # Original dashboard prototype (HTML/JS)
├── idea-engine/             # Python — Reddit scraping + Claude analysis
│   ├── src/
│   ├── data/
│   └── requirements.txt
├── dropship-engine/         # E-commerce product sourcing pipeline
│   ├── src/
│   ├── data/
│   └── requirements.txt
├── android/
│   └── hello-dejima/        # Template Android project
├── config/
│   ├── openclaw.json        # OpenClaw gateway configuration
│   └── openclaw/            # Additional config
├── scripts/                 # PowerShell automation scripts
│   ├── setup-auth.ps1
│   ├── run-gateway.ps1
│   ├── setup-android-env.ps1
│   ├── start-emulator.ps1
│   └── ...
├── docs/
│   ├── submission/          # DevPost submission materials
│   └── plans/               # Session handoffs and design docs
├── design/                  # Design documents
├── resources/               # SDK resources (moltbunker-sdk)
├── .env.example             # API key template
├── .gitignore
└── LICENSE
```

---

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **AI Brain** | Claude Opus 4.6 (code generation, reasoning, idea analysis) |
| **Vision QA** | Gemini 3.1 Flash (screenshot analysis, UI verification) |
| **UI Generation** | Gemini 3.1 Pro (Compose UI writing inside OpenClaw) |
| **Agent Framework** | OpenClaw (forked, modified) — Python, WebSocket gateway |
| **Android** | Kotlin, Jetpack Compose, Material 3, Gradle 8.7.3, JDK 17 |
| **Finance Backend** | Node.js, Express, TypeScript, SSE |
| **Cost Tracking** | Paid.ai SignalV2 API |
| **Dashboard** | React 18, TypeScript, Vite, TanStack React Query, Recharts, shadcn/ui, Tailwind CSS |
| **Marketing** | ElevenLabs (voice), Google Veo 3.1 (video), Lyria 3 (music), YouTube API v3 |
| **Payments** | Stripe |
| **Crypto** | Solana (agent-controlled wallet) |
| **APIs (20+)** | CoinGecko, Finnhub, NASA, Spoonacular, DeepL, Pexels, Spotify, MapBox, WolframAlpha, Groq, REST Countries, Open-Meteo, Reddit, and more |

### Pinned Build Versions

```
compileSdk=35, minSdk=31, targetSdk=35
Kotlin 2.0.21
Compose BOM 2024.12.01
Android Gradle Plugin 8.7.3
Gradle 8.11.1
JVM target 17 (Amazon Corretto)
JAVA_HOME=/path/to/corretto-17
```

---

## Setup

### Prerequisites

- macOS / Linux
- JDK 17 (Amazon Corretto recommended)
- Android SDK (API 35)
- Node.js 18+
- Python 3.10+
- ADB (for device deployment)

### Environment

```bash
cp .env.example .env
# Fill in your API keys:
# - ANTHROPIC_API_KEY (Claude Opus 4.6)
# - GOOGLE_AI_API_KEY (Gemini 3.1 Flash / Pro)
```

### OpenClaw Gateway

```bash
cd openclaw
# Follow OpenClaw setup instructions
# Gateway runs on ws://127.0.0.1:19001
```

### Finance Server

```bash
cd finance
npm install
npm start
# Runs on http://localhost:3456
```

### Idea Engine

```bash
cd idea-engine
pip install -r requirements.txt
python src/main.py
```

### Agent Watchtower (Dashboard)

See [agent-watchtower repo](https://github.com/MrxYacinex/agent-watchtower) for the React dashboard.

---

## The Uncomfortable Truth

Everything we are building — economic self-sufficiency, agent-controlled crypto wallets, the ability to hire human intermediaries, autonomous tool use across unbounded domains — is precisely the infrastructure a misaligned superintelligence would need to operate independently of human oversight. We are not naive about this.

We also acknowledge that **this system is a cybersecurity nightmare in its current state.** Prompt injection, API key exposure, unsupervised financial decisions, supply chain attacks through Gradle dependencies — these are all real attack vectors we have not solved. We built this in 21 hours at a hackathon. Nobody should run this in production without robust security infrastructure.

We're building it in the open because we believe this kind of system is inevitable. We'd rather the safety community can study it, stress-test it, and design the guardrails *before* someone builds it without any controls at all.

Read the full analysis: [`docs/submission/asi-risk-warning.md`](docs/submission/asi-risk-warning.md)

---

## What Doesn't Work Yet

- **Not profitable.** Zero real revenue. Projections only.
- **Apps are simple.** Calculators, trackers, games. They prove the pipeline, not the ceiling.
- **Solana wallet is proof-of-concept.** No end-to-end earn → pay-for-compute flow.
- **Cost tracking is ~40% complete.** Claude and Gemini tracked; Lovable, GCP, revenue sources not yet.
- **Security is nonexistent.** No prompt injection defence, no key rotation, no transaction approval.
- **Marketing pipeline has manual steps.** Content is AI-generated; distribution isn't fully automated.

These are engineering problems. With time and resources, every one is solvable.

---

## Links

- **DevPost**: [devpost.com/software/new-dejima](https://devpost.com/software/new-dejima)
- **Agent Watchtower**: [github.com/MrxYacinex/agent-watchtower](https://github.com/MrxYacinex/agent-watchtower)
- **OpenClaw**: [github.com/open-claw](https://github.com/open-claw)

---

## Team

Built at HackEurope Paris 2026.

---

## License

See [LICENSE](LICENSE).
