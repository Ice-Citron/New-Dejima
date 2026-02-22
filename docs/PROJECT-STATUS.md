# Project Status — New Dejima

**Last updated:** 2026-02-22

## What's Working

### OpenClaw Gateway (Android App Builder)
- **Status:** Fully operational
- **Agent:** `dev` (C3-PO), using `anthropic/claude-opus-4-6`
- **Config:** `%USERPROFILE%\.openclaw\openclaw.json`
- **Auth profiles:** `%USERPROFILE%\.openclaw\agents\dev\agent\auth-profiles.json`
- **Gateway:** `ws://127.0.0.1:18789`
- **Start command:** `.\scripts\run-gateway.ps1`
- **Skill:** `android-app-builder` — 7-phase autonomous pipeline (scaffold → code → compile → test → QA → APK → report)

### Apps Successfully Built
| App | APK Location | Size | Status |
|-----|-------------|------|--------|
| Workout Timer | `C:\Users\nahom\AppData\Local\Temp\dejima-workspace\workout-timer\app\build\outputs\apk\debug\app-debug.apk` | 22.3 MB | Compiled 1st attempt, emulator verified |
| GolfDeals | `C:\Users\nahom\AppData\Local\Temp\dejima-workspace\golf-deals\app\build\outputs\apk\debug\app-debug.apk` | 51.4 MB | Compiled 1st attempt, emulator verified |

### Idea Engine (Problem Discovery)
- **Status:** Fully operational
- **Location:** `idea-engine/`
- **Python venv:** `idea-engine/venv/`
- **Config:** `idea-engine/.env` (Anthropic API key)
- **Data:** `idea-engine/data/` (scraped posts, ranked ideas)

#### Pipeline
```
scrape → analyze → rank → build
```
- `python -m src.cli scrape` — mines Hacker News + Reddit (via Google) for pain points
- `python -m src.cli analyze` — Claude extracts app ideas from posts
- `python -m src.cli rank` — ranks and consolidates best ideas
- `python -m src.cli build` — sends top idea to OpenClaw to build the APK
- `python -m src.cli pipeline` — runs all 4 steps end-to-end autonomously

#### OpenClaw Skill
- `config/openclaw/skills/idea-generator/SKILL.md` — defines the idea discovery pipeline as an OpenClaw skill

### Dropship Engine (Autonomous Store Builder)
- **Status:** Built, ready for testing
- **Location:** `dropship-engine/`
- **Config:** `dropship-engine/.env` (Anthropic API key — same as idea-engine)
- **Data:** `dropship-engine/data/` (niches, products, store configs)
- **Storefront:** Generated via Lovable (no API key needed — URL-based)

#### Pipeline
```
research → analyze → source → build-store
```
- `python -m src.cli research` — scrapes Google Trends, Reddit, Google for trending products
- `python -m src.cli analyze` — Claude identifies profitable niches from research data
- `python -m src.cli source` — finds supplier prices (AliExpress), calculates real margins
- `python -m src.cli build-store` — generates a full storefront via Lovable
- `python -m src.cli evaluate "product name"` — quick-evaluate a single product idea
- `python -m src.cli status` — show pipeline summary
- `python -m src.cli pipeline` — runs all 4 steps end-to-end autonomously

#### How It Works
1. Scrapes trending products from Google Trends, Reddit (r/dropshipping, r/ecommerce, etc.)
2. Claude analyzes trends and identifies the most promising niches (demand, competition, margins)
3. Searches AliExpress for supplier pricing, calculates real margins after COGS, shipping, Stripe fees
4. Generates a detailed store prompt and launches Lovable to build a complete React storefront
5. Store includes product catalog, cart, checkout (demo mode), admin panel, responsive design

### Treasury / Finance Module (Crypto Wallet + Payment Pipeline + Dashboard)
- **Status:** Integrated (from yacine + main branches)
- **Location:** `New-Dejima-yacine/finance/`
- **Runtime:** Node.js/TypeScript (ESM)
- **Dependencies:** `@solana/web3.js`, `stripe`, `express`, `dotenv`, `openai`, `@paid-ai/paid-node`

#### What It Does
Per-agent Solana wallets with a full 7-step payment pipeline for autonomous agent reproduction, plus a live admin dashboard with real-time economics tracking:
```
SOL payment → KYC → SOL→USDT conversion → Stripe → GPU provisioning → server inject → handoff
```

#### Key Components
| File | Purpose |
|------|---------|
| `server.ts` | Express dashboard backend (port 3456) — serves HTML + REST API |
| `dashboard.html` | Live admin dashboard — cost/revenue/builds per agent, event log, Paid.ai integration |
| `event-store.ts` | Persistent event storage (cost, revenue, build events) |
| `agent-sessions.ts` | Reads OpenClaw session transcripts, SSE live stream |
| `cli-status.ts` | Terminal-based status viewer (queries the server API) |
| `wallet.ts` | Create Solana wallets, check balance, devnet airdrop |
| `wallet-store.ts` | Persistent wallet storage keyed by agent ID |
| `payment-pipeline.ts` | Full 7-step pipeline (SOL → GPU server handoff) |
| `gcp-provision.ts` | GCP Compute Engine GPU provisioner (T4/L4, Deep Learning VM) |
| `crusoe-provision.ts` | Crusoe cloud GPU provisioner (A40/A100) |
| `crusoe-inference.ts` | Crusoe inference client (Qwen models via OpenAI-compatible API) |
| `vast-provision.ts` | Vast.ai GPU provisioner (V100/A100) |
| `cost-tracker.ts` | Paid.ai deep integration — signals, traces, usage queries |
| `agent-economics.ts` | Per-agent ROI: tokens, cost, revenue, margin, model cost tables |
| `paid-setup.ts` | Paid.ai customer/product/order initialization + demo signals |
| `reproduce.ts` | Agent reproduction — parent pays treasury, child gets GPU server |
| `demo.ts` | End-to-end demo runner (`--live` for real GPU, dry-run by default) |

#### How to Run
```powershell
cd New-Dejima-yacine\finance
npm install

# Dashboard server (open http://localhost:3456 in browser)
npm run server

# CLI status (queries the running server)
npm run status

# Demo (dry-run)
npm run demo

# Paid.ai setup (initialize customer/product/order + demo signals)
npm run paid-setup
```

#### Dashboard API Endpoints
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/` | GET | Dashboard HTML |
| `/api/economics` | GET | Agent cost/revenue summaries |
| `/api/events` | GET | Raw event log |
| `/api/agents` | GET | All agents with session + economics data |
| `/api/agents/:id/status` | GET | Agent current task, active/idle |
| `/api/agents/:id/messages` | GET | Prompt/response message history |
| `/api/agents/:id/stream` | GET (SSE) | Live activity stream |
| `/api/track-cost` | POST | Record a cost event |
| `/api/track-revenue` | POST | Record a revenue event |
| `/api/track-build` | POST | Record a build completion |
| `/api/paid/*` | GET | Paid.ai proxy (status, traces, usage, costs) |
| `/api/stats/timeline` | GET | Time-series cost/revenue for charts |

#### Environment Variables Needed
- `STRIPE_SECRET_KEY` — Stripe test-mode key
- `GCP_PROJECT` — GCP project ID (for GPU provisioning)
- `VAST_API_KEY` — Vast.ai API key (optional, for `--vast` provider)
- `CRUSOE_API_KEY` — Crusoe cloud API key (optional)
- `PAID_AI_API_KEY` — Paid.ai API key (for cost tracking dashboard)

### AI Video Creator (Autonomous Video Marketing)
- **Status:** Integrated (from yacine-content-generation branch)
- **Location:** `New-Dejima-yacine/config/openclaw/skills/ai-video-creator/`
- **Runtime:** Python 3.10+ (requires `requests`, `google-genai`)
- **Docs:** `New-Dejima-yacine/docs/plans/api-setup-guides.md`

#### Pipeline
```
script → voice → music → video → post-prod → QA → thumbnail → upload
```
7-phase autonomous pipeline that produces and publishes video content:

| Phase | Tool | What it does |
|-------|------|-------------|
| 1. Script | Gemini 2.5 Flash | Generates promo script + scene breakdown from topic |
| 2. Voice | ElevenLabs | TTS narration with curated voice pool |
| 2.5 Music | Lyria 2 (Vertex AI) | AI-generated background music |
| 3. Video | Veo 3.1 (Vertex AI) | Text-to-video scene generation |
| 4. Post-prod | ffmpeg | Concat scenes, layer audio, burn subtitles + CTA overlays |
| 5. QA | ffprobe | Duration, resolution, stream integrity checks |
| 6. Thumbnail | Imagen 4 | AI-generated YouTube thumbnail |
| 7. Upload | YouTube/TikTok/IG APIs | Multi-platform distribution |

#### How to Run
```powershell
cd New-Dejima-yacine\config\openclaw\skills\ai-video-creator\scripts
.\run-pipeline.ps1 "Golf Deals — finds the best golf balls" portrait
.\run-pipeline.ps1 "Workout Timer — quick HIIT workouts" both    # landscape + portrait
```

#### Environment Variables Needed
- `GEMINI_API_KEY` — Google AI API key (for Veo 3.1, Imagen 4, Gemini Flash)
- `GCP_PROJECT` — GCP project ID (for Vertex AI)
- `ELEVENLABS_API_KEY` — ElevenLabs TTS key
- `YOUTUBE_CLIENT_ID` / `YOUTUBE_CLIENT_SECRET` / `YOUTUBE_REFRESH_TOKEN` — YouTube upload
- `TIKTOK_ACCESS_TOKEN` — TikTok upload (optional)
- `INSTAGRAM_USER_ID` / `INSTAGRAM_ACCESS_TOKEN` — Instagram Reels (optional)

## Architecture

```
New-Dejima/
├── openclaw/              # OpenClaw gateway + CLI (Node.js/pnpm)
├── idea-engine/           # Problem discovery + idea ranking (Python)
│   ├── src/
│   │   ├── reddit_scraper.py   # HN API + Google/Reddit scraping
│   │   ├── analyzer.py         # Claude-based idea extraction
│   │   ├── storage.py          # JSON idea store
│   │   ├── builder.py          # Bridge to OpenClaw app builder
│   │   └── cli.py              # CLI entry point
│   ├── data/                   # Scrape results + ranked ideas
│   └── .env                    # API keys (gitignored)
├── dropship-engine/       # Autonomous dropshipping store builder (Python)
│   ├── src/
│   │   ├── researcher.py       # Google Trends + Reddit product research
│   │   ├── analyzer.py         # Claude-based niche analysis
│   │   ├── sourcer.py          # AliExpress sourcing + margin calculator
│   │   ├── store_builder.py    # Lovable prompt generation + store launch
│   │   ├── storage.py          # JSON store for niches, products, stores
│   │   └── cli.py              # CLI entry point
│   ├── data/                   # Research results, niches, products
│   └── .env                    # API keys (gitignored)
├── apps/                 # Android app source (12 apps built by agent)
│   ├── workout-timer/
│   ├── mood-tracker/
│   ├── expense-tracker/
│   ├── flashcard-app/
│   ├── habit-tracker/
│   ├── stock-tracker/
│   ├── crypto-dashboard/
│   ├── tic-tac-toe/
│   ├── unit-converter/
│   ├── weather-space/
│   ├── country-explorer/
│   └── recipe-finder/
├── New-Dejima-yacine/     # Yacine's modules (from yacine + main branches)
│   ├── finance/               # Crypto wallet + payment pipeline + dashboard (Node.js/TypeScript)
│   │   ├── src/
│   │   │   ├── server.ts              # Express dashboard backend (port 3456)
│   │   │   ├── dashboard.html         # Live admin dashboard (dark theme, real-time)
│   │   │   ├── event-store.ts         # Persistent event storage (cost/revenue/build)
│   │   │   ├── agent-sessions.ts      # OpenClaw session reader + SSE live stream
│   │   │   ├── cli-status.ts          # Terminal status viewer
│   │   │   ├── wallet.ts              # Solana wallet creation, balance, airdrop
│   │   │   ├── wallet-store.ts        # Persistent wallet storage per agent
│   │   │   ├── payment-pipeline.ts    # 7-step: SOL → KYC → Stripe → GPU → handoff
│   │   │   ├── gcp-provision.ts       # GCP Compute Engine GPU provisioner (T4/L4)
│   │   │   ├── crusoe-provision.ts    # Crusoe cloud GPU provisioner (A40/A100)
│   │   │   ├── crusoe-inference.ts    # Crusoe inference client (Qwen via OpenAI API)
│   │   │   ├── vast-provision.ts      # Vast.ai GPU provisioner
│   │   │   ├── cost-tracker.ts        # Paid.ai deep integration (signals, traces, queries)
│   │   │   ├── agent-economics.ts     # Per-agent ROI calculations + model cost table
│   │   │   ├── paid-setup.ts          # Paid.ai initialization + demo signals
│   │   │   ├── reproduce.ts           # Agent reproduction (parent pays → child gets GPU)
│   │   │   └── demo.ts               # End-to-end demo runner
│   │   ├── data/
│   │   │   └── events.json            # Persistent event log
│   │   ├── package.json               # Dependencies (express, @solana/web3.js, stripe, etc.)
│   │   └── tsconfig.json
│   ├── config/openclaw/skills/
│   │   └── ai-video-creator/          # Autonomous video marketing pipeline (Python)
│   │       ├── SKILL.md               # OpenClaw skill definition (7-phase)
│   │       └── scripts/
│   │           ├── pipeline.py        # Full pipeline (841 lines)
│   │           ├── run-pipeline.ps1   # PowerShell runner (Windows)
│   │           └── run-pipeline.sh    # Bash runner (macOS/Linux)
│   └── docs/plans/
│       ├── 2026-02-21-ai-video-creator-plan.md
│       ├── api-setup-guides.md        # API key setup for all video services
│       └── golf-deals-video-brief.md  # Example video brief
├── dashboard/                # Agent economics dashboard (static HTML)
│   ├── index.html                 # Main page — open in browser
│   ├── styles.css                 # Dark theme, responsive layout
│   ├── data.js                    # Agent definitions + metrics (real + simulated)
│   └── app.js                     # Rendering logic
├── config/openclaw/
│   ├── openclaw.json           # Gateway + agent config
│   └── skills/
│       ├── android-app-builder/SKILL.md
│       └── idea-generator/SKILL.md
├── scripts/
│   ├── run-gateway.ps1
│   ├── send-test-prompt.ps1
│   ├── send-workout-prompt.ps1
│   └── show-anthropic-key-preview.ps1
└── docs/
```

## How to Run (Quick Start)

### Prerequisites
- Node.js 22+ with pnpm
- Python 3.10+
- Android SDK (for app building)
- Anthropic API key with credits

### Start the idea engine pipeline
```powershell
# Terminal 1: Gateway
cd C:\Users\nahom\New-Dejima
.\scripts\run-gateway.ps1

# Terminal 2: Idea engine (after gateway shows "listening")
cd C:\Users\nahom\New-Dejima\idea-engine
.\venv\Scripts\Activate.ps1
python -m src.cli pipeline
```

### Start the dropship engine
```powershell
cd C:\Users\nahom\New-Dejima\dropship-engine
pip install -r requirements.txt
python -m src.cli pipeline              # full auto: research → analyze → source → build
python -m src.cli pipeline --dry-run    # see the store prompt without launching Lovable
```

### Install APKs
```powershell
adb devices                    # list connected devices
adb -s <DEVICE> install <APK>  # install to specific device
```

### Agent Dashboard (Static — Lovable)
- **Status:** Built, ready to use
- **Location:** `dashboard/`
- **Open:** `dashboard/index.html` in any browser (no build step needed)
- **Data:** Real data from project (apps built, agent configs) + simulated metrics (tokens, costs)
- **Wallet integration:** Stub ready — wallet section shows per-agent placeholders; update `data.js` `wallet` fields once crypto wallets are assigned

### Agent Dashboard (Live — Finance Server)
- **Status:** Integrated (from main branch), fully operational
- **Location:** `New-Dejima-yacine/finance/src/server.ts` + `dashboard.html`
- **Start:** `cd New-Dejima-yacine\finance && npm run server`
- **Open:** `http://localhost:3456` in any browser
- **Data:** Real-time events — costs, revenue, and builds tracked via REST API
- **Features:**
  - Live cost/revenue/build tracking with auto-refresh (5s)
  - Per-agent economics table with sustainability ratio gauge
  - Event log (newest first, live-updating)
  - Paid.ai integration status + data (products, customers, orders)
  - Core thesis visualization: `money_made_per_token > cost_per_token`
  - Agent session reader (reads OpenClaw transcripts)
  - SSE live stream for real-time agent activity
  - Timeline API for cost/revenue charts

## What's Not Done Yet
- Google AI API key (Gemini) not configured — vision QA falls back to Claude
- Android SDK paths in `openclaw.json` still point to macOS paths (builds work via emulator on Windows)
- App Store publishing pipeline — not started
- User feedback/iteration loop in idea engine — not implemented
- Treasury module (crypto wallet integration) — **integrated** from yacine branch into `New-Dejima-yacine/finance/`
- Survival kernel (meta-brain for capital allocation across strategies) — planned
- Prediction markets strategy (Polymarket integration) — planned
