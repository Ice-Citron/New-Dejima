# Session Handoff — Feb 14, 2026

**Purpose:** Complete context dump so the next chat session can pick up seamlessly. Feed this entire file to the new session.

---

## 1. What This Project Is

**Project Altiera / "New Dejima"** — an AI safety research project exploring whether AI models can be economically self-sustaining (i.e., `revenue_per_token > cost_per_token`). The broader vision is a "country-like" system where AI agents get seed capital, pay for their own compute, and must earn money to survive. If they earn enough, they gain financial independence via crypto wallets.

**Current MVP focus:** Getting AI agents to autonomously build and deploy **Telegram Mini Apps (TMAs)** that generate real revenue via Telegram Stars. This was pivoted from Android Play Store (2-week review wait) to TMAs (instant deploy).

**Original vision doc:** `/Users/administrator/Black Projects/Project Altiera/start.md`
**Previous handoff:** `/Users/administrator/Black Projects/Project Altiera/HANDOFF.md`

---

## 2. Repository Situation

### The Problem
The old repo (`github.com/philippbogdan/altiera`) is owned by Phil (co-founder who left). The user (Ice-Citron / Shi Hao) can't add new contributors, manage settings, etc. Solution: migrate everything to a new repo under their own ownership.

### Old Repo
- **Location:** `/Users/administrator/Black Projects/Project Altiera/miscellaneous/altiera`
- **Remote:** `https://github.com/philippbogdan/altiera.git`
- **Contributors:** Ice-Citron (5 commits), Philipp Bogdan (26 commits), dependabot (3 commits)
- **Currently checked out branch:** `feature/telegram-mini-app`

### New Repo
- **Location:** `/Users/administrator/Black Projects/Project Altiera/new-dejima`
- **Remote:** `https://github.com/Ice-Citron/new-dejima.git`
- **State:** Fresh — 1 commit ("Initial commit"), just a `README.md`

### Migration Plan (NOT YET DONE)
**Decision made:** Full history replay — preserve all commits with original authors/dates.
**Branches to migrate:** `main` + `feature/telegram-mini-app` only.

**Why skip the other branches:**
- `phil` branch: 12 commits ahead of main, but they are **parallel duplicates** of commits already on main (same changes, different SHAs). No unique files vs main. Main actually has MORE content (includes TMA merge).
- `codex/altiera-v1` branch: Same situation — subset of phil, all already on main.
- `dependabot/*` branches: Just GitHub Actions version bumps (checkout v4->v6, setup-node v4->v6, setup-python v5->v6). Not worth migrating.

**Migration approach (not yet executed):**
1. Add old repo as a remote in the new repo
2. Fetch all history
3. Replay commits onto the new repo (preserving authors/dates)
4. Push `main` and `feature/telegram-mini-app` branches

---

## 3. Branch Analysis (Old Repo — Detailed)

### Commit Graph Summary
```
* d0b6bb3 (dependabot) bump actions/setup-node 4->6
| * 805688b (feature/telegram-mini-app) progress for now
| | * da14a1f (dependabot) bump actions/checkout 4->6
| | * 8571325 (dependabot) bump actions/setup-python 5->6
* | 9e97413 (main) feat: phil branch — new tools, resilience hardening...
* | 6ddb9a9 fix: resilience hardening
* | ...12 more commits on main...
* | 53ad06f Merge PR #3 from feature/telegram-mini-app
|\|
| * ea0e022 remerging to main branch for sake of vercel
| * bb0572a new branch created
|/
| * 7e68e49 (phil) feat: phil branch — new tools...
| * ...11 more commits on phil (parallel to main)...
|/
* f7bd0ca initial prompt push
* 0611e71 initial prompt push
* ac985e5 chore: standardize on python/pip
* 593754e chore: normalize file endings
* 077a683 Initial repo scaffolding and end-state spec
```

### What's on `main` (the most complete branch)
All of Phil's v1 core work merged in, PLUS the TMA work via PR #3:
- Agent infrastructure scaffolding (Python)
- Dashboard (Next.js)
- Configs (claude, openai, ralph_demo, etc.)
- Tests (13+ test files)
- GitHub CI/CD
- Mini-app (Plinko TMA)
- Docs (END_STATE.md, SAFETY.md, DEJIMA_65H_PLAN.md, SPRINT_10H_ANDROID.md)

### What's on `feature/telegram-mini-app` (13 ahead, 1 behind main)
Your TMA branch — the Plinko game + 60H master plan:
- `mini-app/src/components/Game.tsx` — 748-line Plinko game (written from scratch)
- `mini-app/api/create-invoice.ts` — Telegram Stars payment serverless function
- `mini-app/vercel.json` — Vercel deploy config
- `docs/PLAN_60H_MASTER.md` — 60-hour master plan
- Modified: App.tsx, Root.tsx, index.tsx, index.css, index.html

---

## 4. What's Already Built and Working

### Plinko TMA (LIVE)
- **URL:** https://mini-app-eta-ten.vercel.app
- **Telegram bot:** @altiera_test_bot
- **Telegram link:** t.me/altiera_test_bot/app
- **Vercel project:** sienar-industries-projects/mini-app (ice-citron account)
- Canvas-based Plinko gambling game, 12 rows of pegs, physics-based
- Telegram Stars monetization integrated (5 coin packages)
- Touch-optimized for mobile, works in desktop too

### Agent Infrastructure (PARTIALLY SCAFFOLDED)
- `pyproject.toml` and `.env.example` exist
- Actual agent loop code NOT written yet

### Planning Documents
- `docs/END_STATE.md` — benchmark vision
- `docs/SAFETY.md` — guardrails and threat model
- `docs/DEJIMA_65H_PLAN.md` — full 65-hour build plan
- `docs/SPRINT_10H_ANDROID.md` — superseded Android sprint plan

---

## 5. Tech Stack

| Component | Technology |
|-----------|-----------|
| TMA frontend | React 18 + TypeScript + Vite 6, Canvas-based |
| TMA SDK | `@tma.js/sdk-react` 3.0.8 (installed, currently bypassed) |
| Hosting | Vercel (free tier), auto-deploys from CLI |
| Serverless API | Vercel Functions (Node.js) — `/api/create-invoice.ts` |
| Bot | Telegram Bot API |
| Package manager | pnpm |
| Agent infra (planned) | Python 3.11+, Anthropic SDK |
| Dashboard | Next.js (on main branch) |

---

## 6. OpenClaw Reference Notes

OpenClaw was mentioned in `start.md` as a potential reference. We researched it this session:

- **What it is:** Open-source personal AI assistant by Peter Steinberger (formerly "Clawdbot", Nov 2025). 100K+ installations.
- **Plugin SDK:** Real — `dist/plugin-sdk/` with TypeScript types. Covers channels, tools, AI providers, memory backends. 50+ integrations.
- **Android companion app:** Real — runs as a "node" connecting to gateway via WebSocket. Gives AI agents access to camera, screen recording, SMS, location, canvas control on a real phone.
- **NOT a tool for building Android apps** — it IS an Android app that remote-controls phone hardware for agents.
- **Relevance to Altiera:** Useful reference for how to give agents real-world tool access, but different goal (personal assistant vs autonomous economic survival).
- **Why it's not well-known in frontier AI circles:** Positioned as self-hosted personal assistant, not research. Used by automation/self-hosted community, not SF frontier labs crowd.

Sources:
- https://github.com/openclaw/openclaw
- https://docs.openclaw.ai/platforms/android
- https://deepwiki.com/openclaw/openclaw/10-extensions-and-plugins

---

## 7. Android Studio Situation

### Current Install
- **Version:** Android Studio 2021.1 (Bumblebee) — installed Feb 2022
- **Location:** `/Applications/Android Studio.app`
- **Install method:** Manual (not Homebrew, not Toolbox)
- Also has IntelliJ IDEA installed at `/Applications/IntelliJ IDEA.app`

### Latest Available
- **Android Studio Panda 1** (2025.3.1 Patch 1) — released Feb 10, 2026
- Preview: Android Studio Panda 2 (2025.3.2) in Canary

### Plan (NOT YET DONE)
- Install JetBrains Toolbox (manages both Android Studio AND IntelliJ IDEA)
- Install latest Android Studio Panda via Toolbox
- Delete old 2021.1 install from `/Applications/`
- Toolbox handles auto-updates, rollback, side-by-side versions going forward

---

## 8. Key File Locations

```
/Users/administrator/Black Projects/Project Altiera/
├── start.md                    ← Original project vision/braindump
├── HANDOFF.md                  ← Previous session handoff (Feb 8, 2026)
├── docs/
│   └── plans/
│       ├── 2026-02-14-session-handoff.md    ← THIS FILE
│       └── 2026-02-15-android-autonomous-dev-design.md  ← Android dev design
├── miscellaneous/
│   └── altiera/                ← OLD REPO (philippbogdan/altiera)
│       ├── mini-app/           ← Plinko TMA (React + Vite)
│       ├── dashboard/          ← Next.js dashboard
│       ├── configs/            ← Agent configs (claude, openai, etc.)
│       ├── docs/               ← Planning docs (END_STATE, SAFETY, etc.)
│       ├── tests/              ← 13+ Python test files
│       └── .github/            ← CI/CD, issue templates
└── new-dejima/                 ← NEW REPO (Ice-Citron/new-dejima)
    └── README.md               ← Just "# new-dejima" — empty
```

---

## 9. What's Left To Do (Priority Order)

### A. Repo Migration (paused, do whenever ready)
- Full history replay from `philippbogdan/altiera` to `Ice-Citron/new-dejima`
- Migrate `main` and `feature/telegram-mini-app` branches
- Preserve all original authors/dates/commit messages

### B. Android Studio Update (paused, user switched tasks)
- Install JetBrains Toolbox
- Install Android Studio Panda 1 (2025.3.1)
- Remove old 2021.1 install

### C. TMA Immediate Work (from HANDOFF.md)
1. Regenerate bot token (was exposed in screenshot) — `/revoke` in BotFather
2. Add pre-checkout webhook handler for payment verification
3. Clean up dead template code from mini-app/src/
4. Improve game (sound, haptics, animations, leaderboard)

### D. Agent Loop Infrastructure (the core of the project)
5. Build the agent loop (Python, calls Anthropic/OpenAI API, tools, cost tracking)
6. Build tool registry (decorator-based)
7. Build TMA-specific tools (create_project, write_file, build, deploy, register)
8. Build cost tracking (token counts, USD cost per model)
9. Build consciousness loop (THINK -> ACT -> OBSERVE -> COMPACT -> SLEEP)
10. Build benchmark harness (run episodes across models, compare ratio)

### E. Bigger Picture
11. Policy engine (tool allowlist, deny-by-default, budget enforcement)
12. Run artifacts (events.jsonl, ledger.csv, tokens.json, metrics.json)
13. Economic sandbox
14. Multi-model comparison
15. Crypto wallet system
16. TMA control dashboard

---

## 10. Credentials & Secrets

| Secret | Status | Notes |
|--------|--------|-------|
| Telegram Bot Token | NEEDS ROTATION | Was exposed in screenshot. Use BotFather `/revoke` then update Vercel env |
| Vercel account | ice-citron | Logged in via `vercel login` |
| Anthropic API key | Not configured | Needed for agent loop |
| GitHub (new repo) | Ice-Citron | `https://github.com/Ice-Citron/new-dejima.git` |
| GitHub (old repo) | philippbogdan | `https://github.com/philippbogdan/altiera.git` (read-only for you) |

---

## 11. Resources Available

- 20K Azure credits
- 20K AWS credits
- 5K OpenAI credits
- 1K Anthropic credits
- 6.7K xAI credits
- Vercel free tier

---

## 12. Discussions & Decisions Made This Session

1. **Migration scope:** Full history replay, main + feature/telegram-mini-app only. Phil/codex branches are duplicates of main.
2. **OpenClaw:** Verified as real project with real plugin SDK and Android companion app. Useful as reference, not direct dependency.
3. **Android Studio:** Decided to go JetBrains Toolbox route for managing IDE updates. Not yet executed.
4. **Academia observation:** User noted that academic AI research has fallen behind industry speed. Relevant context for project positioning — this is applied research, not academic paper fodder.
