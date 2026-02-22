# Lovable Prompt — New Dejima Agent Dashboard

Paste everything below the line into Lovable.

---

Build a dashboard called "New Dejima" that monitors a fleet of autonomous AI agents. It connects to a WebSocket gateway to show real-time data, and falls back to hardcoded data when the gateway is offline.

The design should be modern, technical, and clean — think a mission control panel for an AI agent fleet. Dark theme, monospace numbers, minimal chrome, generous whitespace, subtle borders. It should feel like something an engineer would build for themselves, not a marketing page.

## WebSocket Gateway Integration

Connect to `ws://127.0.0.1:18789` using this protocol:

**Handshake:**
1. Open WebSocket
2. Server may send `{ type: "event", event: "connect.challenge", payload: { nonce: "..." } }`
3. After receiving the challenge (or after 800ms if none arrives), send:
```json
{
  "type": "req",
  "id": "unique-id",
  "method": "connect",
  "params": {
    "minProtocol": 1,
    "maxProtocol": 3,
    "client": { "id": "dejima-dashboard", "version": "1.0.0", "platform": "web", "mode": "control" },
    "caps": []
  }
}
```
4. Server responds: `{ "type": "res", "id": "unique-id", "ok": true, "payload": { ... } }`

**Requests (after connected):**
Send: `{ "type": "req", "id": "unique-id", "method": "METHOD_NAME", "params": { ... } }`
Receive: `{ "type": "res", "id": "unique-id", "ok": true, "payload": { ... } }`

### API calls to make

**`agents.list`** — params: `{}`
```ts
// response payload
{
  defaultId: string,
  agents: Array<{
    id: string,
    name?: string,
    model?: string,
    identity?: { name?: string, emoji?: string, theme?: string }
  }>
}
```

**`sessions.usage`** — params: `{ days: 30, limit: 1000 }`
```ts
// response payload
{
  sessions: Array<{ key: string, agentId?: string, model?: string, usage: { input: number, output: number, cacheRead: number, cacheWrite: number, totalTokens: number, totalCost: number } | null }>,
  totals: { input: number, output: number, cacheRead: number, cacheWrite: number, totalTokens: number, totalCost: number },
  aggregates: {
    byAgent: Array<{ agentId: string, totals: { totalTokens: number, totalCost: number, input: number, output: number, cacheRead: number } }>,
    byModel: Array<{ model?: string, count: number, totals: { totalTokens: number, totalCost: number } }>,
    daily: Array<{ date: string, tokens: number, cost: number, messages: number }>
  }
}
```

**`usage.cost`** — params: `{ days: 30 }`
```ts
// response payload
{
  daily: Array<{ date: string, totalTokens: number, totalCost: number, input: number, output: number, cacheRead: number, cacheWrite: number }>,
  totals: { totalTokens: number, totalCost: number }
}
```

## Fallback Data (when gateway is offline)

```ts
const FALLBACK_AGENTS = [
  { id: "dev", name: "C3-PO", emoji: "🤖", role: "Android App Builder", model: "anthropic/claude-opus-4-6", status: "active", color: "#6366f1",
    tokens: 2_847_320, cost: 42.18, revenue: 0, sessions: 14,
    productions: [
      { name: "Workout Timer", type: "Android APK", status: "verified", size: "22.3 MB", date: "2026-02-21" },
      { name: "GolfDeals", type: "Android APK", status: "verified", size: "51.4 MB", date: "2026-02-21" },
    ]},
  { id: "idea-engine", name: "Idea Engine", emoji: "💡", role: "Problem Discovery & Ranking", model: "anthropic/claude-sonnet-4-20250514", status: "active", color: "#eab308",
    tokens: 523_400, cost: 7.85, revenue: 0, sessions: 8, productions: [] },
  { id: "dropship-engine", name: "Dropship Engine", emoji: "🛍️", role: "Autonomous Store Builder", model: "anthropic/claude-sonnet-4-20250514", status: "active", color: "#22c55e",
    tokens: 891_200, cost: 13.37, revenue: 0, sessions: 5,
    productions: [
      { name: "Trending Store (Demo)", type: "React Storefront", status: "demo", size: "-", date: "2026-02-22" },
    ]},
  { id: "survival-kernel", name: "Survival Kernel", emoji: "🧠", role: "Meta-brain / Capital Allocator", model: "TBD", status: "planned", color: "#a855f7",
    tokens: 0, cost: 0, revenue: 0, sessions: 0, productions: [] },
  { id: "prediction-markets", name: "Prediction Markets", emoji: "🎲", role: "Polymarket Trader", model: "TBD", status: "planned", color: "#f97316",
    tokens: 0, cost: 0, revenue: 0, sessions: 0, productions: [] },
];
```

## What to display

**Per agent:** name, emoji, role, status (active/planned), model, tokens used, cost, revenue, session count, list of productions, wallet address (null for now), wallet balance (null for now).

**Aggregated:** total active agents, total planned, total tokens, total cost, total revenue, total products shipped, total sessions.

**Daily cost chart:** bar chart of cost per day from `usage.cost` daily data (last 14 days).

**Cost by agent:** horizontal bars showing each agent's cost relative to the others.

**Production log table:** all products across all agents — columns: product name, agent, type, status, size, date.

**Wallet/treasury section:** placeholder section noting crypto wallet integration is pending. Show a stub card per active agent with placeholder address "0x???...??? (pending)" and balance "-- ETH".

**Connection status:** show whether the gateway is connected or offline. Tag each metric with "Live" or "Simulated" depending on source.

## Behavior

1. On load, try connecting to the gateway. If it connects, fetch all 3 API calls in parallel and use real data. If it fails or times out (10s), use the fallback data.
2. Merge gateway `agents.list` with `sessions.usage` aggregates.byAgent to get per-agent tokens/cost. Match on agent ID.
3. Revenue and wallet fields are always placeholder for now — a teammate is building the crypto wallet integration.
4. Refresh button re-fetches everything.
5. Gateway URL should be configurable (default `ws://127.0.0.1:18789`).
