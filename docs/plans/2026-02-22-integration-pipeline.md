# Integration Pipeline + Admin Dashboard Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Connect OpenClaw's agent runs to the finance module's cost tracking, serve a web dashboard for monitoring, and provide CLI status.

**Architecture:** OpenClaw emits a lifecycle "end" event enriched with usage data. A new event listener in the gateway POSTs cost data to a finance Express server (port 3456). The server stores events, forwards to Paid.ai, and serves a web dashboard + REST API.

**Tech Stack:** OpenClaw (TypeScript), Express, Paid.ai SDK, HTML/CSS/JS (inline dashboard)

---

### Task 1: Finance Express Server with REST API + Data Store

**Files:**
- Create: `finance/src/server.ts`
- Create: `finance/src/event-store.ts`

**Step 1: Install express**

Run: `cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance" && npm install express @types/express`

**Step 2: Write event-store.ts**

```typescript
// finance/src/event-store.ts
import { readFileSync, writeFileSync, existsSync, mkdirSync } from "fs";
import { dirname } from "path";

const STORE_PATH = new URL("../data/events.json", import.meta.url).pathname
  .replace(/%20/g, " ");

export interface TrackingEvent {
  type: "cost" | "revenue" | "build";
  agentId: string;
  timestamp: string;
  data: Record<string, unknown>;
}

let events: TrackingEvent[] = [];

function ensureDir() {
  const dir = dirname(STORE_PATH);
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
}

export function loadEvents(): TrackingEvent[] {
  ensureDir();
  if (!existsSync(STORE_PATH)) return [];
  try {
    events = JSON.parse(readFileSync(STORE_PATH, "utf-8"));
  } catch { events = []; }
  return events;
}

export function addEvent(event: TrackingEvent) {
  events.push(event);
  ensureDir();
  writeFileSync(STORE_PATH, JSON.stringify(events, null, 2));
}

export function getEvents(): TrackingEvent[] {
  return events;
}

export function getAgentSummaries() {
  const agents = new Map<string, { cost: number; revenue: number; tokens: number; builds: number }>();
  for (const evt of events) {
    const id = evt.agentId;
    if (!agents.has(id)) agents.set(id, { cost: 0, revenue: 0, tokens: 0, builds: 0 });
    const a = agents.get(id)!;
    if (evt.type === "cost") {
      a.cost += (evt.data.costUsd as number) || 0;
      a.tokens += ((evt.data.inputTokens as number) || 0) + ((evt.data.outputTokens as number) || 0);
    }
    if (evt.type === "revenue") a.revenue += (evt.data.amountUsd as number) || 0;
    if (evt.type === "build") a.builds += 1;
  }
  return Object.fromEntries(agents);
}
```

**Step 3: Write server.ts**

```typescript
// finance/src/server.ts
import express from "express";
import { readFileSync } from "fs";
import { loadEvents, addEvent, getEvents, getAgentSummaries } from "./event-store.js";
import { trackCost, trackRevenue } from "./cost-tracker.js";
import { calculateTokenCost } from "./agent-economics.js";

const app = express();
app.use(express.json());

// Load persisted events on startup
loadEvents();

// POST /api/track-cost — receives cost events from OpenClaw hook
app.post("/api/track-cost", async (req, res) => {
  const { agentId, model, inputTokens, outputTokens, cacheRead, cacheWrite, runId, durationMs } = req.body;
  const costUsd = calculateTokenCost(model || "claude-opus-4-6", inputTokens || 0, outputTokens || 0);

  addEvent({
    type: "cost",
    agentId: agentId || runId || "unknown",
    timestamp: new Date().toISOString(),
    data: { model, inputTokens, outputTokens, cacheRead, cacheWrite, costUsd, runId, durationMs },
  });

  // Forward to Paid.ai
  await trackCost({
    agentId: agentId || runId || "unknown",
    model: model || "claude-opus-4-6",
    inputTokens: inputTokens || 0,
    outputTokens: outputTokens || 0,
    estimatedCostUsd: costUsd,
    timestamp: new Date().toISOString(),
  });

  res.json({ ok: true, costUsd });
});

// POST /api/track-revenue — receives revenue events
app.post("/api/track-revenue", async (req, res) => {
  const { agentId, source, amountUsd, description } = req.body;

  addEvent({
    type: "revenue",
    agentId: agentId || "unknown",
    timestamp: new Date().toISOString(),
    data: { source, amountUsd, description },
  });

  await trackRevenue({
    agentId: agentId || "unknown",
    source: source || "android_app",
    amountUsd: amountUsd || 0,
    description: description || "",
    timestamp: new Date().toISOString(),
  });

  res.json({ ok: true });
});

// POST /api/track-build — receives build completion events
app.post("/api/track-build", (req, res) => {
  const { agentId, appName, apkSize, success, runId } = req.body;

  addEvent({
    type: "build",
    agentId: agentId || "unknown",
    timestamp: new Date().toISOString(),
    data: { appName, apkSize, success, runId },
  });

  res.json({ ok: true });
});

// GET /api/events — raw event log
app.get("/api/events", (_req, res) => {
  res.json(getEvents());
});

// GET /api/economics — agent summaries
app.get("/api/economics", (_req, res) => {
  res.json(getAgentSummaries());
});

// GET / — serve dashboard HTML
app.get("/", (_req, res) => {
  try {
    const html = readFileSync(new URL("./dashboard.html", import.meta.url).pathname.replace(/%20/g, " "), "utf-8");
    res.type("html").send(html);
  } catch {
    res.type("html").send("<h1>Dashboard not found</h1>");
  }
});

const PORT = 3456;
app.listen(PORT, () => {
  console.log(`\n  New Dejima Finance Server running at http://localhost:${PORT}`);
  console.log(`  Dashboard: http://localhost:${PORT}/`);
  console.log(`  API:       http://localhost:${PORT}/api/economics\n`);
});
```

**Step 4: Verify server starts**

Run: `cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance" && npx tsx src/server.ts &`
Expected: "New Dejima Finance Server running at http://localhost:3456"

Run: `curl -s http://localhost:3456/api/economics`
Expected: `{}`

---

### Task 2: Web Dashboard HTML

**Files:**
- Create: `finance/src/dashboard.html`

**Step 1: Write dashboard.html**

Single-file HTML dashboard with dark theme, auto-refresh, agent economics table, event log, and build history. See implementation for full HTML (inline CSS + JS, fetches from `/api/economics` and `/api/events` every 5 seconds).

**Step 2: Verify dashboard loads**

Run: `curl -s http://localhost:3456/ | head -5`
Expected: HTML content with "New Dejima" title

---

### Task 3: OpenClaw Cost Tracking Hook

**Files:**
- Modify: `openclaw/src/agents/pi-embedded-subscribe.handlers.lifecycle.ts:55-63`
- Create: `openclaw/src/gateway/cost-tracking-hook.ts`
- Modify: `openclaw/src/gateway/server.impl.ts:477-490`

**Step 1: Enrich lifecycle "end" event with usage data**

In `pi-embedded-subscribe.handlers.lifecycle.ts`, the `handleAgentEnd` function emits the lifecycle "end" event at line 56-63. Modify it to include usage data from `ctx.getUsageTotals()`:

```typescript
// In handleAgentEnd, replace lines 56-63:
emitAgentEvent({
  runId: ctx.params.runId,
  stream: "lifecycle",
  data: {
    phase: "end",
    endedAt: Date.now(),
    usage: ctx.getUsageTotals(),
    model: ctx.params.model,
    provider: ctx.params.provider,
    sessionKey: ctx.params.sessionKey,
  },
});
```

Check that `ctx` has `getUsageTotals` — it's on the `EmbeddedPiSubscribeContext` type. Also check `ctx.params` for `model` and `provider`.

**Step 2: Create cost-tracking-hook.ts**

```typescript
// openclaw/src/gateway/cost-tracking-hook.ts
import type { AgentEventPayload } from "../infra/agent-events.js";

const FINANCE_SERVER = "http://localhost:3456";

export function handleCostTrackingEvent(evt: AgentEventPayload) {
  if (evt.stream !== "lifecycle") return;
  const phase = evt.data?.phase;
  if (phase !== "end" && phase !== "error") return;

  const usage = evt.data?.usage as Record<string, number> | undefined;
  if (!usage) return;

  const payload = {
    runId: evt.runId,
    agentId: evt.sessionKey || evt.runId,
    model: (evt.data?.model as string) || "claude-opus-4-6",
    provider: (evt.data?.provider as string) || "anthropic",
    inputTokens: usage.input || 0,
    outputTokens: usage.output || 0,
    cacheRead: usage.cacheRead || 0,
    cacheWrite: usage.cacheWrite || 0,
    durationMs: evt.data?.endedAt
      ? (evt.data.endedAt as number) - evt.ts
      : 0,
  };

  // Fire-and-forget POST to finance server
  fetch(`${FINANCE_SERVER}/api/track-cost`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  }).catch(() => {
    // Finance server might not be running — that's fine
  });
}
```

**Step 3: Wire hook into gateway**

In `server.impl.ts`, after the existing `onAgentEvent` call (line 479), add a second listener:

```typescript
import { handleCostTrackingEvent } from "./cost-tracking-hook.js";

// After line 490:
const costTrackingUnsub = onAgentEvent(handleCostTrackingEvent);
```

**Step 4: Rebuild OpenClaw**

Run: `cd "/Users/administrator/Black Projects/Project Altiera/openclaw" && pnpm build`
Expected: Build succeeds

**Step 5: Restart gateway**

Kill existing gateway, restart it. Verify no errors in startup logs.

---

### Task 4: CLI Status Command

**Files:**
- Create: `finance/src/cli-status.ts`

**Step 1: Write cli-status.ts**

```typescript
// finance/src/cli-status.ts
const FINANCE_SERVER = "http://localhost:3456";

async function main() {
  try {
    const [ecoRes, evtRes] = await Promise.all([
      fetch(`${FINANCE_SERVER}/api/economics`),
      fetch(`${FINANCE_SERVER}/api/events`),
    ]);
    const economics = await ecoRes.json();
    const events = await evtRes.json();

    console.log("\n═══════════════════════════════════════════");
    console.log("  NEW DEJIMA — SYSTEM STATUS");
    console.log("═══════════════════════════════════════════\n");

    const agents = Object.entries(economics);
    if (agents.length === 0) {
      console.log("  No agent activity recorded yet.\n");
    } else {
      for (const [id, data] of agents) {
        const d = data as any;
        const ratio = d.cost > 0 ? (d.revenue / d.cost) : 0;
        console.log(`  Agent: ${id}`);
        console.log(`    Cost:    $${d.cost.toFixed(4)}  |  Revenue: $${d.revenue.toFixed(4)}`);
        console.log(`    Tokens:  ${d.tokens.toLocaleString()}  |  Builds: ${d.builds}`);
        console.log(`    Ratio:   ${ratio.toFixed(2)}x ${ratio >= 1 ? "(SUSTAINABLE)" : "(deficit)"}`);
        console.log();
      }
    }

    console.log(`  Total events: ${events.length}`);
    console.log(`  Dashboard:    http://localhost:3456/\n`);
  } catch {
    console.error("Finance server not running. Start with: npx tsx src/server.ts");
  }
}

main();
```

**Step 2: Verify**

Run: `cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance" && npx tsx src/cli-status.ts`
Expected: Shows system status or "no activity" message

---

### Task 5: Integration Test — End to End

**Step 1: Start finance server**

Run: `cd "/Users/administrator/Black Projects/Project Altiera/New-Dejima/finance" && npx tsx src/server.ts &`

**Step 2: Send a test cost event manually**

Run:
```bash
curl -X POST http://localhost:3456/api/track-cost \
  -H "Content-Type: application/json" \
  -d '{"agentId":"test-agent","model":"claude-opus-4-6","inputTokens":80000,"outputTokens":20000,"runId":"test-1"}'
```

Expected: `{"ok":true,"costUsd":2.7}`

**Step 3: Send a test revenue event**

Run:
```bash
curl -X POST http://localhost:3456/api/track-revenue \
  -H "Content-Type: application/json" \
  -d '{"agentId":"test-agent","source":"android_app","amountUsd":0.99,"description":"Tip Calculator download"}'
```

**Step 4: Check dashboard**

Open `http://localhost:3456/` in browser — should show test-agent with $2.70 cost, $0.99 revenue, 0.37x ratio.

**Step 5: Check CLI**

Run: `npx tsx src/cli-status.ts`
Expected: Shows test-agent summary.

**Step 6: Trigger a real agent build**

Send a simple build prompt through OpenClaw. After completion, check:
- `http://localhost:3456/api/events` — should have a new cost event from the hook
- Dashboard should update automatically

---

### Task 6: Add npm scripts for convenience

**Files:**
- Modify: `finance/package.json`

Add to scripts:
```json
{
  "scripts": {
    "server": "tsx src/server.ts",
    "status": "tsx src/cli-status.ts",
    "demo": "tsx src/demo.ts"
  }
}
```
