# Project Altiera — Handoff Document

**Last updated:** February 8, 2026
**Author:** Claude Opus 4.6 (session with ice-citron)
**Repo:** `altiera/` — AI autonomy benchmark + "New Dejima" agent economy experiment

---

## What This Project Is

An AI safety research project exploring whether AI models can be economically self-sustaining — i.e., whether `revenue_per_token > cost_per_token`. The broader vision is "New Dejima" — a country-like system where AI agents are given seed capital, charged for their own compute costs, and must earn money to stay alive. If they earn enough, they gain financial independence via crypto wallets.

**For now (MVP phase),** we pivoted from the full autonomy benchmark to a concrete proof-of-concept: **getting an AI agent to autonomously build and deploy Telegram Mini Apps (TMAs) that generate real revenue via Telegram Stars.**

---

## Current State (What's Built and Working)

### 1. Plinko TMA — LIVE AND DEPLOYED

**URL:** https://mini-app-eta-ten.vercel.app
**Telegram bot:** @altiera_test_bot
**Telegram app link:** t.me/altiera_test_bot/app
**Vercel project:** sienar-industries-projects/mini-app
**Vercel account:** ice-citron

**What it does:**
- A fully functional Plinko gambling game rendered on HTML5 Canvas
- 12 rows of pegs with physics-based ball bouncing
- Multiplier slots at bottom (0.3x to 10x)
- $1,000 starting play-money balance
- Bet controls: ÷2, -, +, ×2
- Tap the board or "DROP BALL" button to play
- Touch-optimized for mobile Telegram WebView
- Works in desktop browser too

**Telegram Stars monetization (integrated):**
- "+" COINS button (top right) opens an in-game shop overlay
- 5 coin packages: 10/25/50/100/250 Stars → 2K/6K/15K/35K/100K coins
- When player runs out of money, "DROP BALL" button changes to "BUY COINS"
- Backend serverless function (`/api/create-invoice`) calls Telegram Bot API `createInvoiceLink`
- Opens Telegram's native Stars payment sheet via `window.Telegram.WebApp.openInvoice()`
- On payment success, coins credited instantly
- Falls back to free test-mode coins when opened outside Telegram

**What's needed for store visibility:**
- Telegram doesn't have a self-serve "submit to store" button
- Apps that accept Telegram Stars payments and have good profiles get curated into the Mini App Store
- Bot profile (icon, description) has been set up in BotFather
- The app is currently accessible via direct link only

### 2. Agent Infrastructure — PARTIALLY SCAFFOLDED

We created `pyproject.toml` and `.env.example` for the Python agent loop, but the actual agent code (loop, tools, cost tracker, prompts) was **not written yet** — the file writes were rejected before completion. This is the next major workstream.

### 3. Planning Documents — WRITTEN

Several planning docs exist in `docs/`:
- `END_STATE.md` — the original benchmark vision (ratio measurement)
- `SAFETY.md` — guardrails and threat model
- `DEJIMA_65H_PLAN.md` — full 65-hour build plan for the New Dejima system
- `SPRINT_10H_ANDROID.md` — original Android sprint plan (superseded by TMA pivot)

---

## File Map

```
altiera/
├── HANDOFF.md                          ← THIS FILE
├── start.md                            ← Original braindump/vision document
├── README.md                           ← Repo readme (basic)
├── AGENTS.md                           ← Agent instructions for the repo
├── pyproject.toml                      ← Python project config (agent infra deps)
├── .env.example                        ← Env vars needed (API keys, config)
│
├── docs/
│   ├── END_STATE.md                    ← Target demo & acceptance criteria
│   ├── SAFETY.md                       ← Safety guardrails
│   ├── DEJIMA_65H_PLAN.md             ← Full 65-hour system build plan
│   └── SPRINT_10H_ANDROID.md          ← Old Android sprint (superseded)
│
├── mini-app/                           ← THE LIVE TMA (Plinko game)
│   ├── index.html                      ← Entry HTML (includes telegram-web-app.js)
│   ├── package.json                    ← @altiera/mini-app, React 18 + Vite 6
│   ├── pnpm-lock.yaml                 ← Lockfile (pnpm)
│   ├── vite.config.ts                 ← Vite config with SWC, tsconfig paths, mkcert
│   ├── vercel.json                    ← Vercel deploy config
│   ├── tsconfig.json / tsconfig.node.json
│   │
│   ├── api/
│   │   └── create-invoice.ts          ← Vercel serverless function for Stars payments
│   │
│   ├── src/
│   │   ├── index.tsx                  ← Entry point (simplified, no SDK check)
│   │   ├── index.css                  ← Global CSS (reset, no-scroll, no-select)
│   │   ├── init.ts                    ← Original TMA SDK init (currently unused)
│   │   ├── mockEnv.ts                 ← Dev environment mock (currently unused)
│   │   │
│   │   ├── components/
│   │   │   ├── App.tsx                ← Just renders <Game />
│   │   │   ├── Root.tsx               ← ErrorBoundary wrapper (TonConnect removed)
│   │   │   ├── Game.tsx               ← *** THE MAIN FILE — entire Plinko game ***
│   │   │   ├── ErrorBoundary.tsx      ← React error boundary
│   │   │   ├── EnvUnsupported.tsx     ← "Too old Telegram" fallback (unused now)
│   │   │   ├── Page.tsx               ← Page wrapper (unused)
│   │   │   ├── DisplayData/           ← Data display component (unused)
│   │   │   ├── Link/                  ← Link component (unused)
│   │   │   └── RGB/                   ← Color display component (unused)
│   │   │
│   │   ├── pages/                     ← Original template pages (unused)
│   │   ├── navigation/                ← Router config (unused)
│   │   ├── css/                       ← BEM + classnames utils (unused)
│   │   └── helpers/                   ← publicUrl helper (unused)
│   │
│   └── public/
│       └── tonconnect-manifest.json   ← TON Connect manifest (unused)
│
├── .github/                           ← GitHub templates + CI
│   ├── workflows/ci.yml
│   ├── ISSUE_TEMPLATE/
│   ├── pull_request_template.md
│   └── dependabot.yml
│
└── runs/                              ← Empty dir for future run artifacts
```

### Files We Actually Modified (vs template)

Only these files were changed from the original TMA template:
1. `mini-app/src/components/Game.tsx` — **written from scratch** (entire Plinko game)
2. `mini-app/src/components/App.tsx` — simplified to just `<Game />`
3. `mini-app/src/components/Root.tsx` — removed TonConnect, kept ErrorBoundary
4. `mini-app/src/index.tsx` — simplified (removed SDK init check for browser compat)
5. `mini-app/src/index.css` — replaced with game-specific global styles
6. `mini-app/index.html` — added `telegram-web-app.js` script, changed title
7. `mini-app/api/create-invoice.ts` — **written from scratch** (Stars payment API)
8. `mini-app/vercel.json` — **written from scratch** (deploy config)

### Files That Are Dead Code (from template, safe to delete)

- `mini-app/src/pages/*` — all original template pages
- `mini-app/src/navigation/*` — router config
- `mini-app/src/components/DisplayData/`, `Link/`, `RGB/`, `Page.tsx`, `EnvUnsupported.tsx`
- `mini-app/src/css/*` — BEM utilities
- `mini-app/src/helpers/*` — publicUrl
- `mini-app/src/init.ts` — SDK init (was being used, now bypassed)
- `mini-app/src/mockEnv.ts` — dev mock
- `mini-app/public/tonconnect-manifest.json` — TON manifest

---

## Tech Stack

| Component | Technology | Notes |
|-----------|-----------|-------|
| TMA frontend | React 18 + TypeScript + Vite 6 | Canvas-based game, no UI library |
| TMA SDK | `@tma.js/sdk-react` 3.0.8 | Installed but currently bypassed in index.tsx |
| Telegram UI | `@telegram-apps/telegram-ui` 2.1.x | Installed but unused (game uses Canvas) |
| TON Connect | `@tonconnect/ui-react` 2.x | Installed but unused (removed from Root.tsx) |
| Hosting | Vercel (free tier) | Auto-deploys from CLI |
| Serverless API | Vercel Functions (Node.js) | `/api/create-invoice.ts` |
| Bot | Telegram Bot API | Token stored as Vercel env var |
| Package manager | pnpm | `pnpm-lock.yaml` in repo |
| Agent infra (planned) | Python 3.11+, Anthropic SDK | `pyproject.toml` exists, code not written |

---

## Key Techniques & Patterns

### Telegram Stars Payment Flow
```
User taps "Buy Coins" in game
    → Frontend calls POST /api/create-invoice { stars: 50 }
    → Serverless function calls Telegram Bot API createInvoiceLink
        (currency: "XTR", provider_token: "" for Stars)
    → Returns invoice URL
    → Frontend calls window.Telegram.WebApp.openInvoice(url, callback)
    → Telegram shows native payment sheet
    → On "paid" status in callback → credit coins to player
```

### Canvas Game Architecture
- Single `Game.tsx` component with `useEffect` + `useRef` pattern
- All game state in `stateRef.current` (avoids React re-renders)
- `requestAnimationFrame` loop: `update()` → `draw()` → repeat
- Touch input: `touchstart` with `preventDefault` for mobile
- Mouse input: `mousedown` for desktop
- Hit-testing: manual coordinate checks against button/UI regions
- Physics: simple gravity + peg collision with circle-circle detection

### Vercel Deployment
- `vercel --prod --yes` from `mini-app/` directory
- Serverless functions auto-detected from `api/` directory
- Env vars set via `vercel env add TELEGRAM_BOT_TOKEN production`
- Alias: `mini-app-eta-ten.vercel.app`
- Build command: `pnpm run build` (tsc + vite)

---

## Credentials & Secrets

| Secret | Location | Notes |
|--------|----------|-------|
| Telegram Bot Token | Vercel env var `TELEGRAM_BOT_TOKEN` | **NEEDS ROTATION** — was visible in screenshot during session. Use BotFather `/revoke` to regenerate, then `vercel env rm` + `vercel env add` |
| Vercel account | ice-citron | Logged in via `vercel login` |
| Anthropic API key | Not yet configured | Needed for agent loop |

---

## What's Left To Do

### Immediate (to complete the TMA monetization story)
1. **Regenerate bot token** — it was exposed in a screenshot. `/revoke` in BotFather, update Vercel env
2. **Add a pre-checkout webhook handler** — currently the payment works but we don't verify/log payments server-side. Add a bot webhook endpoint that handles `pre_checkout_query` and `successful_payment` events
3. **Clean up dead template code** — remove unused pages, components, navigation, etc. from `mini-app/src/`
4. **Improve the game** — the Plinko physics and UI are functional but basic. Could add: sound effects, haptic feedback via Telegram SDK, animations, leaderboard

### Agent Loop Infrastructure (the core of the project)
5. **Build the agent loop** — Python-based, calls Anthropic/OpenAI API, executes tools, tracks tokens/costs
6. **Build the tool registry** — decorator-based tool system the agent can call
7. **Build TMA-specific tools:**
   - `create_project(name, description)` — scaffold a new TMA from template
   - `write_file(path, content)` — write code files
   - `read_file(path)` — read files back
   - `build_project(path)` — run `pnpm install && pnpm run build`
   - `deploy_to_vercel(path)` — run `vercel --prod`
   - `register_tma(bot_token, url, name)` — set web app URL via Bot API
8. **Build cost tracking** — log every API call with token counts, compute USD cost per model
9. **Build the consciousness loop** — THINK → ACT → OBSERVE → COMPACT → SLEEP cycle
10. **Build the benchmark harness** — run episodes across models, compare ratio

### Bigger Picture (from DEJIMA_65H_PLAN.md)
11. **Policy engine** — tool allowlist, deny-by-default, budget enforcement
12. **Run artifacts** — events.jsonl, ledger.csv, tokens.json, metrics.json per run
13. **Economic sandbox** — simulated task marketplace with deterministic payouts
14. **Multi-model comparison** — batch runner for N episodes per model
15. **Crypto wallet system** — agent-controlled wallets for the "freedom" graduation mechanic
16. **TMA control dashboard** — monitoring, pause/resume/kill, ledger inspection

---

## Resources Available

- 20K Azure credits
- 20K AWS credits
- 5K OpenAI credits
- 1K Anthropic credits
- 6.7K xAI credits
- Vercel free tier (currently used)

---

## How To Resume Development

### Quick start (deploy changes to existing Plinko TMA)
```bash
cd mini-app
pnpm install          # if fresh clone
pnpm run build        # verify it compiles
vercel --prod --yes   # deploy to production
```

### Local dev
```bash
cd mini-app
pnpm run dev          # starts Vite dev server on localhost:5173
# Open in browser for testing (Stars payments fall back to test mode)
# For Telegram testing, use ngrok: ngrok http 5173
# Then update BotFather web app URL to ngrok HTTPS URL
```

### Add/update Vercel env vars
```bash
cd mini-app
vercel env ls                                    # list current vars
vercel env rm TELEGRAM_BOT_TOKEN production      # remove old
vercel env add TELEGRAM_BOT_TOKEN production     # add new (paste value)
```

### Telegram bot management
- All via @BotFather in Telegram
- `/myapps` → select bot → edit web app URL, name, etc.
- `/revoke` → regenerate bot token (DO THIS — current token was leaked)

---

## Architecture Decisions Made

1. **TMA over Android Play Store** — Play Store requires 2 weeks of closed testing before first publish. TMAs deploy instantly with no review.
2. **Canvas over React components** — games need 60fps rendering; Canvas is lighter and faster in Telegram's WebView than DOM manipulation.
3. **Vercel over Firebase/Cloudflare** — instant CLI deploys, free tier includes serverless functions, already had Vercel plugin installed.
4. **Serverless function for payments** — bot token must stay server-side. Vercel Functions in `api/` directory auto-deploy alongside the frontend.
5. **Bypassed TMA SDK init check** — the original template refuses to load outside Telegram. We removed that gate so the game works in any browser (useful for testing, and Stars payments gracefully degrade).
6. **pnpm over npm** — the template came with pnpm-lock.yaml; we installed pnpm globally to match.

---

## Known Issues

1. **Bot token exposed** — was visible in a BotFather screenshot during the session. Must regenerate.
2. **No server-side payment verification** — we create invoices but don't have a webhook to confirm payments. A user could theoretically manipulate the client-side callback. For production, add a bot webhook that listens for `successful_payment` updates.
3. **No persistent balance** — player balance resets on page refresh. Need server-side storage (database or at minimum localStorage) to persist balances.
4. **Dead template code** — lots of unused files from the original TMA template still in the repo.
5. **Vercel env var had newline warning** — when we set TELEGRAM_BOT_TOKEN, Vercel warned about newlines. May need to re-set it cleanly after token rotation.

---

## Git Status

- **Branch:** main
- **Clean:** yes (as of last check — mini-app changes may be uncommitted)
- **Remote:** GitHub (origin)
- **Recent commits:** `ea0e022 remerging to main branch for sake of vercel`

**NOTE:** The mini-app code changes (Game.tsx, App.tsx, Root.tsx, index.tsx, index.css, create-invoice.ts, vercel.json, index.html) may not be committed yet. Check `git status` and commit before switching contexts.
