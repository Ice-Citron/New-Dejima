import express from "express";
import { readFileSync } from "fs";
import { loadEvents, addEvent, getEvents, getAgentSummaries } from "./event-store.js";
import {
  trackCost,
  trackRevenue,
  trackBuild,
  ensurePaidSetup,
  isPaidConfigured,
  getCostTraces,
  getUsageSummary,
  getCostSummary,
  checkUsageStatus,
  listProducts,
  listCustomers,
  listOrders,
} from "./cost-tracker.js";
import { calculateTokenCost } from "./agent-economics.js";
import {
  listAgents,
  getAgentStatus,
  getAgentMessages,
  getAgentBuilds,
  getAgentCostBreakdown,
  addSSEClient,
  watchSessionFile,
  listSessionFiles,
} from "./agent-sessions.js";

const app = express();
app.use(express.json());

// CORS for dashboard dev
app.use((_req, res, next) => {
  res.header("Access-Control-Allow-Origin", "*");
  res.header("Access-Control-Allow-Headers", "Content-Type");
  next();
});

// Load persisted events on startup
loadEvents();

// ── POST /api/track-cost — receives cost events from OpenClaw hook ──
app.post("/api/track-cost", async (req, res) => {
  const { agentId, model, inputTokens, outputTokens, cacheRead, cacheWrite, runId, durationMs } =
    req.body;
  const costUsd = calculateTokenCost(
    model || "claude-opus-4-6",
    inputTokens || 0,
    outputTokens || 0
  );

  addEvent({
    type: "cost",
    agentId: agentId || runId || "unknown",
    timestamp: new Date().toISOString(),
    data: { model, inputTokens, outputTokens, cacheRead, cacheWrite, costUsd, runId, durationMs },
  });

  // Forward to Paid.ai via usageRecordBulkV2
  await trackCost({
    agentId: agentId || runId || "unknown",
    model: model || "claude-opus-4-6",
    inputTokens: inputTokens || 0,
    outputTokens: outputTokens || 0,
    estimatedCostUsd: costUsd,
    timestamp: new Date().toISOString(),
    runId,
    cacheRead,
    cacheWrite,
    durationMs,
  });

  res.json({ ok: true, costUsd });
});

// ── POST /api/track-revenue — receives revenue events ──
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

// ── POST /api/track-build — receives build completion events ──
app.post("/api/track-build", async (req, res) => {
  const { agentId, appName, apkSize, success, runId } = req.body;

  addEvent({
    type: "build",
    agentId: agentId || "unknown",
    timestamp: new Date().toISOString(),
    data: { appName, apkSize, success, runId },
  });

  // Forward build signal to Paid.ai
  await trackBuild({
    agentId: agentId || "unknown",
    appName: appName || "unknown-app",
    success: success ?? true,
    apkSize,
    runId,
  });

  res.json({ ok: true });
});

// ── GET /api/events — raw event log ──
app.get("/api/events", (_req, res) => {
  res.json(getEvents());
});

// ── GET /api/economics — agent summaries ──
app.get("/api/economics", (_req, res) => {
  res.json(getAgentSummaries());
});

// ═══════════════════════════════════════════════════════════════════
// Paid.ai API proxy endpoints — lets the dashboard query Paid.ai
// ═══════════════════════════════════════════════════════════════════

// GET /api/paid/status — check if Paid.ai is connected
app.get("/api/paid/status", (_req, res) => {
  res.json({ configured: isPaidConfigured() });
});

// GET /api/paid/traces — cost traces from Paid.ai dashboard
app.get("/api/paid/traces", async (_req, res) => {
  const traces = await getCostTraces();
  if (!traces) return res.json({ error: "Paid.ai not configured or error", traces: [] });
  res.json(traces);
});

// GET /api/paid/usage — usage summaries from Paid.ai
app.get("/api/paid/usage", async (_req, res) => {
  const usage = await getUsageSummary();
  if (!usage) return res.json({ error: "Paid.ai not configured or error", data: null });
  res.json(usage);
});

// GET /api/paid/costs — cost summaries from Paid.ai
app.get("/api/paid/costs", async (_req, res) => {
  const costs = await getCostSummary();
  if (!costs) return res.json({ error: "Paid.ai not configured or error", data: null });
  res.json(costs);
});

// GET /api/paid/check — entitlement/usage check
app.get("/api/paid/check", async (_req, res) => {
  const status = await checkUsageStatus();
  if (!status) return res.json({ error: "Paid.ai not configured or error", data: null });
  res.json(status);
});

// GET /api/paid/products — list all products
app.get("/api/paid/products", async (_req, res) => {
  const products = await listProducts();
  if (!products) return res.json({ error: "Paid.ai not configured or error", products: [] });
  res.json(products);
});

// GET /api/paid/customers — list all customers
app.get("/api/paid/customers", async (_req, res) => {
  const customers = await listCustomers();
  if (!customers) return res.json({ error: "Paid.ai not configured or error", customers: [] });
  res.json(customers);
});

// GET /api/paid/orders — list all orders
app.get("/api/paid/orders", async (_req, res) => {
  const orders = await listOrders();
  if (!orders) return res.json({ error: "Paid.ai not configured or error", orders: [] });
  res.json(orders);
});

// ═══════════════════════════════════════════════════════════════════
// Agent endpoints — for agent detail page
// ═══════════════════════════════════════════════════════════════════

// GET /api/agents — list all agents with summary stats
app.get("/api/agents", (_req, res) => {
  const agents = listAgents();
  // Merge with event-store data for cost/revenue/builds
  const economics = getAgentSummaries();
  const enriched = agents.map((a) => {
    const eco = economics[a.sessionId] || economics[a.key] || null;
    return {
      ...a,
      cost: eco?.cost || 0,
      revenue: eco?.revenue || 0,
      builds: eco?.builds || 0,
      eventTokens: eco?.tokens || 0,
    };
  });
  res.json(enriched);
});

// GET /api/agents/:id/status — current task, active/idle, last message
app.get("/api/agents/:id/status", (req, res) => {
  const sessionKey = decodeURIComponent(req.params.id);
  const status = getAgentStatus(sessionKey);
  res.json(status);
});

// GET /api/agents/:id/messages — prompt/response history
app.get("/api/agents/:id/messages", (req, res) => {
  const sessionKey = decodeURIComponent(req.params.id);
  const limit = Math.min(parseInt(req.query.limit as string) || 50, 200);
  const offset = parseInt(req.query.offset as string) || 0;
  const result = getAgentMessages(sessionKey, limit, offset);
  res.json(result);
});

// GET /api/agents/:id/builds — build history for this agent
app.get("/api/agents/:id/builds", (req, res) => {
  const agentId = decodeURIComponent(req.params.id);
  const builds = getAgentBuilds(agentId, getEvents());
  res.json(builds);
});

// GET /api/agents/:id/costs — cost breakdown for this agent
app.get("/api/agents/:id/costs", (req, res) => {
  const agentId = decodeURIComponent(req.params.id);
  const breakdown = getAgentCostBreakdown(agentId, getEvents());
  res.json(breakdown);
});

// GET /api/agents/:id/stream — SSE live stream of agent activity
app.get("/api/agents/:id/stream", (req, res) => {
  const sessionKey = decodeURIComponent(req.params.id);

  res.writeHead(200, {
    "Content-Type": "text/event-stream",
    "Cache-Control": "no-cache",
    Connection: "keep-alive",
    "Access-Control-Allow-Origin": "*",
  });

  // Send initial heartbeat
  res.write(`data: ${JSON.stringify({ type: "connected", sessionKey })}\n\n`);

  // Register client and start watching
  addSSEClient(sessionKey, res);
  watchSessionFile(sessionKey);

  // Heartbeat every 30s to keep connection alive
  const heartbeat = setInterval(() => {
    res.write(`: heartbeat\n\n`);
  }, 30000);

  req.on("close", () => {
    clearInterval(heartbeat);
  });
});

// GET /api/sessions — list all session transcript files
app.get("/api/sessions", (_req, res) => {
  res.json(listSessionFiles());
});

// GET /api/agents/:id/events — all events for a specific agent
app.get("/api/agents/:id/events", (req, res) => {
  const agentId = decodeURIComponent(req.params.id);
  const events = getEvents().filter((e) => e.agentId === agentId);
  res.json(events);
});

// GET /api/stats/timeline — time-series cost/revenue for charts
app.get("/api/stats/timeline", (_req, res) => {
  const allEvents = getEvents();
  if (allEvents.length === 0) {
    return res.json([]);
  }

  // Group events into 30-minute buckets with cumulative totals
  const buckets = new Map<string, { cost: number; revenue: number }>();

  for (const evt of allEvents) {
    const d = new Date(evt.timestamp);
    // Round down to 30-min bucket
    d.setMinutes(d.getMinutes() < 30 ? 0 : 30, 0, 0);
    const key = d.toISOString();

    if (!buckets.has(key)) buckets.set(key, { cost: 0, revenue: 0 });
    const b = buckets.get(key)!;

    if (evt.type === "cost") b.cost += (evt.data.costUsd as number) || 0;
    if (evt.type === "revenue") b.revenue += (evt.data.amountUsd as number) || 0;
  }

  // Sort and compute cumulative totals
  const sorted = [...buckets.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  let cumCost = 0;
  let cumRev = 0;
  const timeline = sorted.map(([time, v]) => {
    cumCost += v.cost;
    cumRev += v.revenue;
    const d = new Date(time);
    return {
      time: d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      cost: Math.round(cumCost * 100) / 100,
      revenue: Math.round(cumRev * 100) / 100,
    };
  });

  res.json(timeline);
});

// ── GET / — serve dashboard HTML ──
import { fileURLToPath } from "url";
import { dirname, join } from "path";

const __dirname = dirname(fileURLToPath(import.meta.url));

app.get("/", (_req, res) => {
  try {
    const html = readFileSync(join(__dirname, "dashboard.html"), "utf-8");
    res.type("html").send(html);
  } catch {
    res.type("html").send("<h1>Dashboard not found — create finance/src/dashboard.html</h1>");
  }
});

// ── Boot ──────────────────────────────────────────────────────────
const PORT = 3456;
app.listen(PORT, async () => {
  console.log(`\n  New Dejima Finance Server running at http://localhost:${PORT}`);
  console.log(`  Dashboard:   http://localhost:${PORT}/`);
  console.log(`  API:         http://localhost:${PORT}/api/economics`);
  console.log(`  Paid.ai API: http://localhost:${PORT}/api/paid/status\n`);

  // Initialize Paid.ai customer/product/order on startup
  if (isPaidConfigured()) {
    console.log("  [Paid.ai] API key detected — initializing...");
    const result = await ensurePaidSetup();
    console.log("  [Paid.ai] Setup complete:", result);
    console.log("  [Paid.ai] Dashboard: https://app.paid.ai\n");
  } else {
    console.log("  [Paid.ai] No API key — set PAID_AI_API_KEY in .env");
    console.log("  [Paid.ai] Get your key at: https://app.paid.ai\n");
  }
});
