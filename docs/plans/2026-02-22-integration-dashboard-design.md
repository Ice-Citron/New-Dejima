# New Dejima — Integration Pipeline + Admin Dashboard Design

**Date:** 2026-02-22
**Status:** Approved
**Scope:** A1-7 (Paid.ai hook) + B-2 (Paid.ai integration) + B-6 (Track A-B connection) + Admin Dashboard

## Problem

Track A (Android app building) and Track B (finance/economics) operate independently. When the agent builds an app, no cost or revenue data flows to the economics system. There's also no way to monitor the system's health or economics in real time.

## Solution

Four components:

### 1. OpenClaw Cost Tracking Hook

**File:** `openclaw/src/gateway/cost-tracking-hook.ts`

Subscribe to `onAgentEvent()` lifecycle events in the gateway. On every agent run completion:
- Extract: model, provider, token usage (input/output/cacheRead/cacheWrite), duration
- Calculate cost using per-model pricing
- POST to finance server at `localhost:3456/api/track-cost`

**Pricing table:** Claude Opus $15/$75 per 1M tokens, Sonnet $3/$15, Gemini Flash $0.10/$0.40.

### 2. Finance Server

**File:** `finance/src/server.ts`

Express server on port 3456 with:
- `POST /api/track-cost` — receives cost events from OpenClaw hook
- `POST /api/track-revenue` — receives revenue events (app completion)
- `GET /api/economics` — returns all agent economics
- `GET /api/agents` — returns agent registry
- `GET /api/builds` — returns build history
- `GET /api/events` — returns raw event log
- `GET /` — serves web dashboard HTML

Persists to `finance/data/events.json` (append-only JSON lines).

Forwards to Paid.ai when `PAID_AI_API_KEY` is configured.

### 3. Web Dashboard

**File:** `finance/src/dashboard.html`

Single-file HTML dashboard served at `http://localhost:3456`:
- Agent economics table (cost, revenue, sustainability ratio)
- Build history with status
- Family tree visualization
- Real-time auto-refresh (5s polling)
- Dark theme

### 4. CLI Status Command

**File:** `finance/src/cli-status.ts`

Terminal command: `npx tsx src/cli-status.ts`
- Reads from same data store
- Shows summary table in terminal

## Data Flow

```
OpenClaw agent run completes
  → cost-tracking-hook.ts extracts usage from EmbeddedPiRunResult
  → POST http://localhost:3456/api/track-cost
  → Finance server stores event, updates economics
  → Paid.ai receives event (if key present)
  → Dashboard auto-refreshes, shows new data
```

## Implementation Order

1. Finance server with REST API + data store
2. Web dashboard HTML
3. OpenClaw cost-tracking hook
4. CLI status command
5. Wire it all together and test
