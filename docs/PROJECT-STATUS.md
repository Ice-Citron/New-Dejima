# Project Status — New Dejima

**Last updated:** 2026-02-21

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

### Start the full pipeline
```powershell
# Terminal 1: Gateway
cd C:\Users\nahom\New-Dejima
.\scripts\run-gateway.ps1

# Terminal 2: Idea engine (after gateway shows "listening")
cd C:\Users\nahom\New-Dejima\idea-engine
.\venv\Scripts\Activate.ps1
python -m src.cli pipeline
```

### Install APKs
```powershell
adb devices                    # list connected devices
adb -s <DEVICE> install <APK>  # install to specific device
```

## What's Not Done Yet
- Google AI API key (Gemini) not configured — vision QA falls back to Claude
- Android SDK paths in `openclaw.json` still point to macOS paths (builds work via emulator on Windows)
- Marketing agent (Track C from HackEurope plan) — not started
- App Store publishing pipeline — not started
- User feedback/iteration loop in idea engine — not implemented
