# Overnight Plan — Feb 21-22, 2026

## Schedule

| Time (CET) | Who | What |
|-------------|-----|------|
| **10:00 PM** | All | Merge everything into New-Dejima repo, git push, verify shippable |
| **10:30 PM - 12:00 AM** | All | Continue building API-powered apps (more complex, more APIs) |
| **12:00 AM - 2:00 AM** | Person A + teammates | Build more apps + start Track B integration |
| **2:00 AM - 7:00 AM** | Person A sleeps, teammates continue | Teammates: presentation prep, Track B work, more app builds |
| **7:00 AM - 9:00 AM** | All | Final app builds, polish, integration testing |
| **9:00 AM - 11:00 AM** | All | **FREEZE CODE** — Documentation, pitch prep, demo recording |
| **11:00 AM** | All | **SUBMIT** |

---

## What's Already Done (Shippable NOW)

### Track A — Agent + Android Pipeline (COMPLETE)

**8 simple apps + 2-3 API-powered apps, all first-attempt compiles:**

| # | App | Type | APIs Used | Status |
|---|-----|------|-----------|--------|
| 1 | Tip Calculator | Simple | None | On Samsung A16 |
| 2 | Workout Timer | Simple | None | On Samsung A16 |
| 3 | Mood Tracker | Simple | None | On Samsung A16 |
| 4 | Unit Converter | Simple | None | On Samsung A16 |
| 5 | Flashcard Study | Simple | None | On Samsung A16 |
| 6 | Tic Tac Toe | Game | None | On Samsung A16 |
| 7 | Habit Tracker | Complex | None | On Samsung A16 |
| 8 | Expense Tracker | Complex | None | On Samsung A16 |
| 9 | Crypto Dashboard | **Multi-page API** | CoinGecko (live) | On Samsung A16 |
| 10 | Weather & Space | **Multi-page API** | Open-Meteo + NASA (live) | On Samsung A16 |
| 11 | Stock Tracker | **Multi-page API** | Finnhub (live) | Building/Built |

**Infrastructure:**
- OpenClaw gateway configured and running
- 7-phase build pipeline with vision QA (SKILL.md)
- Gemini 2.5 Flash for UI testing vision
- 20+ API keys configured
- Samsung A16 physical device deployment
- Emulator (Pixel 6 AVD) running

---

## What To Build Next (Priority Order)

### HIGH PRIORITY — More API-powered apps (impressive for demo)

1. **Recipe Finder** — Spoonacular API
   - Search recipes by ingredients
   - Recipe detail with instructions + nutrition
   - API key: check .env (SPOONACULAR_API_KEY)

2. **News Briefing** — Currents API + Groq AI summaries
   - Top news by category
   - AI-generated 3-sentence summaries (Groq Llama 3)
   - API keys: check .env (CURRENTS_API_KEY, GROQ_API_KEY)

3. **Translation App** — DeepL API
   - Text translation between 30+ languages
   - Language detection
   - Translation history
   - API key: check .env (DEEPL_API_KEY)

4. **Photo Gallery** — Unsplash/Pexels API
   - Search beautiful photos
   - Set as wallpaper
   - API keys: check .env (UNSPLASH_ACCESS_KEY, PEXELS_API_KEY)

### MEDIUM PRIORITY — Games (shows breadth)

5. **Trivia Quiz** — OpenTriviaDB (no key needed)
   - 24 categories, difficulty levels
   - Score tracking, streak counter

6. **Country Explorer** — REST Countries (no key needed)
   - Search countries, see flag, capital, currency, population
   - Quiz mode: guess the capital/flag

### LOWER PRIORITY — Complex multi-API apps

7. **Finance Hub** — Finnhub + CoinGecko + ExchangeRate-API (all keys available)
   - Combined stocks + crypto + forex in one app

8. **Smart Calculator** — WolframAlpha API
   - Solve math, physics, chemistry
   - Step-by-step solutions

---

## How to Build Each App

```bash
cd "/Users/administrator/Black Projects/Project Altiera/openclaw"
# Source API keys
source .env
# Or export manually from .env file
export ANDROID_HOME="/Users/administrator/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
export JAVA_HOME="/Users/administrator/Library/Java/JavaVirtualMachines/corretto-17.0.17/Contents/Home"

# Build an app
node openclaw.mjs --dev agent --local --agent dev -m "Use the android-app-builder skill. Build me a <DETAILED DESCRIPTION>." --json
```

**CRITICAL NOTES:**
- Only ONE build at a time (session lock)
- Each build takes ~5-10 minutes (7-phase pipeline)
- For API apps, include in the prompt: dependencies to add, API endpoint URLs, API keys to hardcode, exact features
- Always include: `implementation("com.squareup.retrofit2:retrofit:2.9.0")` and friends for API apps
- Always include: `<uses-permission android:name="android.permission.INTERNET" />` in manifest

---

## Track B Tasks (Person B)

- [ ] Solana wallet system
- [ ] Paid.ai cost tracking integration
- [ ] Stripe test-mode virtual cards
- [ ] Crusoe inference API
- [ ] Agent economics dashboard
- [ ] Connect Track B to Track A (cost per app build, revenue tracking)

---

## Presentation Prep (9:00 AM - 11:00 AM)

### Pitch Structure (5-10 minutes)
1. **Problem** (30s): "AI inference costs money. Can AI make MORE money than it costs?"
2. **Solution** (60s): "An AI agent that autonomously builds, compiles, tests, and deploys Android apps from a single prompt"
3. **Demo** (3-5 min): Live build of an app, show it appear on phone
4. **Business Model** (60s): Three revenue streams — ads, API subscriptions (10x margin), AI upsells
5. **Numbers** (30s): X apps built, X/X first-attempt, $0.50/app cost, potential $10/mo/user revenue
6. **Tech Stack** (30s): OpenClaw + Claude Opus 4.6 + Gemini 2.5 Flash + Android SDK

### Demo Recording Plan
- Screen record: send prompt → watch agent scaffold → compile → test → APK appears
- Side-by-side: terminal + emulator showing app launching
- Samsung A16: show apps running on real hardware
- Tool: QuickTime screen recording or OBS

### Slides to Prepare
1. Title slide (New Dejima logo/name)
2. The question: "Can AI achieve money > cost?"
3. Architecture diagram
4. Live demo video
5. App gallery (screenshots of all apps)
6. Business model (3 revenue streams diagram)
7. Numbers / metrics
8. Tech stack
9. What's next / roadmap
10. Team

---

## Repo Structure (after consolidation)

```
New-Dejima/
├── .api-keys          (GITIGNORED — all API keys)
├── .gitignore
├── README.md
├── LICENSE
├── openclaw/          (full OpenClaw source, excl node_modules/.git)
├── openclaw-config/   (agent config files — SKILL.md, openclaw.json, etc.)
├── apps/              (generated app source code — Kotlin files, build configs)
├── apks/              (compiled APKs — ready to install)
├── config/            (copied config reference)
├── design/            (design docs)
├── docs/plans/        (all planning/handoff docs)
└── resources/         (hackathon resources)
```

---

## Key Reference Files

| File | Purpose |
|------|---------|
| `docs/plans/2026-02-21-session-handoff-night.md` | Full project state + environment setup |
| `docs/plans/2026-02-21-session-handoff-night-2.md` | Quick recovery guide |
| `docs/plans/2026-02-21-overnight-plan.md` | THIS FILE — overnight schedule + what to build |
| `.api-keys` | All API keys (GITIGNORED) |
| `openclaw-config/workspace-dev/skills/android-app-builder/SKILL.md` | The 7-phase pipeline |
| `docs/plans/2026-02-21-hackeurope-implementation-plan.md` | Original hour-by-hour plan |

---

## THE BUSINESS MODEL (put in every presentation)

Agent builds apps → Apps use real APIs → Users pay subscriptions → 10x margin on API costs

**Three revenue streams:**
1. Ad revenue (passive)
2. API subscription margins ($1 cost → $10 charge = 10x)
3. AI feature upsells ($0.01/query → $4.99/mo premium)

**Cost per app:** ~$0.50 in Claude tokens
**Potential revenue per app:** $10+/mo/user recurring

This proves: **money-made-per-token > cost-per-token**
