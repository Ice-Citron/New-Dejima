/**
 * Dashboard data layer.
 *
 * Connects to the OpenClaw gateway to pull live agent, usage, and cost data.
 * Falls back to simulated estimates only for metrics the gateway doesn't provide
 * (revenue, wallet balances — pending crypto wallet integration).
 *
 * After calling `await loadLiveData()`, AGENTS and COST_DAILY are populated
 * with real numbers. Fields that couldn't be fetched keep their fallback values
 * and are tagged with source: "simulated".
 */

const SRC = { LIVE: "live", SIMULATED: "simulated" };

// Known agent metadata (role descriptions, colors, etc.)
// IDs will be matched against live agents.list response.
const AGENT_META = {
  dev:                  { emoji: "\uD83E\uDD16", role: "Android App Builder",            color: "#6366f1", order: 0 },
  "idea-engine":        { emoji: "\uD83D\uDCA1", role: "Problem Discovery & Ranking",    color: "#eab308", order: 1 },
  "dropship-engine":    { emoji: "\uD83D\uDECD\uFE0F", role: "Autonomous Store Builder", color: "#22c55e", order: 2 },
  "survival-kernel":    { emoji: "\uD83E\uDDE0", role: "Meta-brain / Capital Allocator", color: "#a855f7", order: 3 },
  "prediction-markets": { emoji: "\uD83C\uDFB2", role: "Polymarket Trader",              color: "#f97316", order: 4 },
};

const AGENT_COLORS = ["#6366f1","#eab308","#22c55e","#a855f7","#f97316","#3b82f6","#ef4444","#06b6d4"];

// Productions that we know about from the project (hard facts)
const KNOWN_PRODUCTIONS = [
  {
    agentId: "dev",
    name: "Workout Timer",
    type: "Android APK",
    status: "verified",
    size: "22.3 MB",
    date: "2026-02-21",
  },
  {
    agentId: "dev",
    name: "GolfDeals",
    type: "Android APK",
    status: "verified",
    size: "51.4 MB",
    date: "2026-02-21",
  },
  {
    agentId: "dropship-engine",
    name: "Trending Store (Demo)",
    type: "React Storefront",
    status: "demo",
    size: "-",
    date: "2026-02-22",
  },
];

// Planned agents that won't appear in gateway yet
const PLANNED_AGENTS = [
  {
    id: "survival-kernel",
    name: "Survival Kernel",
    status: "planned",
    model: "TBD",
  },
  {
    id: "prediction-markets",
    name: "Prediction Markets",
    status: "planned",
    model: "TBD",
  },
];

/* ── Live state ─────────────────────────────────────── */

let AGENTS = [];
let COST_DAILY = [];
let USAGE_TOTALS = null;
let USAGE_AGGREGATES = null;
let GATEWAY_CONNECTED = false;

const gateway = new GatewayClient("ws://127.0.0.1:18789");

gateway.onStatusChange((connected) => {
  GATEWAY_CONNECTED = connected;
});

/**
 * Fetch live data from the gateway and populate AGENTS / COST_DAILY.
 * Returns { connected: boolean, error?: string }.
 */
async function loadLiveData() {
  let agentsList = null;
  let usageData = null;
  let costData = null;
  let connected = false;

  try {
    await gateway.connect();
    connected = true;

    const results = await Promise.allSettled([
      gateway.listAgents(),
      gateway.getUsage({ days: 30, limit: 1000 }),
      gateway.getCost({ days: 30 }),
    ]);

    agentsList = results[0].status === "fulfilled" ? results[0].value : null;
    usageData  = results[1].status === "fulfilled" ? results[1].value : null;
    costData   = results[2].status === "fulfilled" ? results[2].value : null;
  } catch (err) {
    console.warn("[dashboard] Gateway not reachable, using simulated data:", err.message);
    buildFallbackState();
    return { connected: false, error: err.message };
  }

  // Build per-agent usage lookup from aggregates
  const agentUsage = {};
  if (usageData?.aggregates?.byAgent) {
    for (const entry of usageData.aggregates.byAgent) {
      agentUsage[entry.agentId] = entry.totals;
    }
  }

  // Build per-agent session count from sessions
  const agentSessionCount = {};
  if (usageData?.sessions) {
    for (const sess of usageData.sessions) {
      const aid = sess.agentId ?? "unknown";
      agentSessionCount[aid] = (agentSessionCount[aid] || 0) + 1;
    }
  }

  // Build AGENTS from live agents.list + usage
  const liveAgents = (agentsList?.agents ?? []).map((agent, i) => {
    const meta = AGENT_META[agent.id] ?? {};
    const usage = agentUsage[agent.id] ?? null;
    const sessions = agentSessionCount[agent.id] ?? 0;
    const prods = KNOWN_PRODUCTIONS.filter(p => p.agentId === agent.id);

    return {
      id: agent.id,
      name: agent.identity?.name ?? agent.name ?? agent.id,
      emoji: agent.identity?.emoji ?? meta.emoji ?? "\uD83E\uDD16",
      role: meta.role ?? agent.identity?.theme ?? "AI Agent",
      model: agent.model ?? "unknown",
      status: "active",
      color: meta.color ?? AGENT_COLORS[i % AGENT_COLORS.length],
      order: meta.order ?? 10 + i,
      metrics: {
        tokensUsed:   { value: usage?.totalTokens  ?? 0,  source: usage ? SRC.LIVE : SRC.SIMULATED, period: "30d" },
        costUsd:      { value: usage?.totalCost     ?? 0,  source: usage ? SRC.LIVE : SRC.SIMULATED, period: "30d" },
        revenueUsd:   { value: 0,                          source: SRC.SIMULATED, period: "30d" },
        sessions:     { value: sessions,                   source: sessions > 0 ? SRC.LIVE : SRC.SIMULATED, period: "30d" },
        inputTokens:  { value: usage?.input         ?? 0,  source: usage ? SRC.LIVE : SRC.SIMULATED, period: "30d" },
        outputTokens: { value: usage?.output        ?? 0,  source: usage ? SRC.LIVE : SRC.SIMULATED, period: "30d" },
        cacheRead:    { value: usage?.cacheRead      ?? 0, source: usage ? SRC.LIVE : SRC.SIMULATED, period: "30d" },
      },
      wallet: { address: null, balance: null },
      productions: prods,
    };
  });

  // Append planned agents
  for (const planned of PLANNED_AGENTS) {
    if (!liveAgents.find(a => a.id === planned.id)) {
      const meta = AGENT_META[planned.id] ?? {};
      liveAgents.push({
        id: planned.id,
        name: planned.name,
        emoji: meta.emoji ?? "\uD83D\uDEE0\uFE0F",
        role: meta.role ?? "Planned",
        model: planned.model,
        status: "planned",
        color: meta.color ?? "#8888a0",
        order: meta.order ?? 99,
        metrics: {
          tokensUsed: { value: 0, source: SRC.LIVE, period: "30d" },
          costUsd:    { value: 0, source: SRC.LIVE, period: "30d" },
          revenueUsd: { value: 0, source: SRC.LIVE, period: "30d" },
        },
        wallet: { address: null, balance: null },
        productions: KNOWN_PRODUCTIONS.filter(p => p.agentId === planned.id),
      });
    }
  }

  liveAgents.sort((a, b) => (a.order ?? 99) - (b.order ?? 99));
  AGENTS = liveAgents;

  // Cost daily
  COST_DAILY = costData?.daily ?? [];
  USAGE_TOTALS = usageData?.totals ?? costData?.totals ?? null;
  USAGE_AGGREGATES = usageData?.aggregates ?? null;

  return { connected: true };
}

/** Build fallback simulated state when gateway is unreachable */
function buildFallbackState() {
  AGENTS = [
    buildFallbackAgent("dev",              "C3-PO",              "active",  "anthropic/claude-opus-4-6",        { tokensUsed: 2_847_320, costUsd: 42.18, sessions: 14 }),
    buildFallbackAgent("idea-engine",      "Idea Engine",        "active",  "anthropic/claude-sonnet-4-20250514",{ tokensUsed: 523_400,  costUsd: 7.85,  sessions: 8 }),
    buildFallbackAgent("dropship-engine",  "Dropship Engine",    "active",  "anthropic/claude-sonnet-4-20250514",{ tokensUsed: 891_200,  costUsd: 13.37, sessions: 5 }),
    buildFallbackAgent("survival-kernel",  "Survival Kernel",    "planned", "TBD",                              { tokensUsed: 0, costUsd: 0, sessions: 0 }),
    buildFallbackAgent("prediction-markets","Prediction Markets","planned", "TBD",                              { tokensUsed: 0, costUsd: 0, sessions: 0 }),
  ];

  COST_DAILY = [];
  USAGE_TOTALS = null;
  USAGE_AGGREGATES = null;
}

function buildFallbackAgent(id, name, status, model, est) {
  const meta = AGENT_META[id] ?? {};
  return {
    id,
    name,
    emoji: meta.emoji ?? "\uD83E\uDD16",
    role: meta.role ?? "Agent",
    model,
    status,
    color: meta.color ?? "#8888a0",
    order: meta.order ?? 99,
    metrics: {
      tokensUsed: { value: est.tokensUsed, source: SRC.SIMULATED, period: "est." },
      costUsd:    { value: est.costUsd,    source: SRC.SIMULATED, period: "est." },
      revenueUsd: { value: 0,             source: SRC.SIMULATED, period: "est." },
      sessions:   { value: est.sessions,  source: SRC.SIMULATED, period: "est." },
    },
    wallet: { address: null, balance: null },
    productions: KNOWN_PRODUCTIONS.filter(p => p.agentId === id),
  };
}

function computeSummary() {
  const active  = AGENTS.filter(a => a.status === "active").length;
  const planned = AGENTS.filter(a => a.status === "planned").length;

  // Prefer gateway totals if available (more accurate than summing per-agent)
  const totalTokens   = USAGE_TOTALS?.totalTokens ?? AGENTS.reduce((s, a) => s + (a.metrics.tokensUsed?.value ?? 0), 0);
  const totalCost     = USAGE_TOTALS?.totalCost   ?? AGENTS.reduce((s, a) => s + (a.metrics.costUsd?.value ?? 0), 0);
  const totalRevenue  = AGENTS.reduce((s, a) => s + (a.metrics.revenueUsd?.value ?? 0), 0);
  const totalProds    = AGENTS.reduce((s, a) => s + (a.productions?.length ?? 0), 0);
  const totalSessions = AGENTS.reduce((s, a) => s + (a.metrics.sessions?.value ?? 0), 0);

  const tokensSource = USAGE_TOTALS ? SRC.LIVE : SRC.SIMULATED;

  return { active, planned, total: AGENTS.length, totalTokens, totalCost, totalRevenue, totalProds, totalSessions, tokensSource };
}
