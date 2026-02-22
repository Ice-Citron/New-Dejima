/* ── Formatting Helpers ──────────────────────────────── */

function fmtTokens(n) {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(2) + "M";
  if (n >= 1_000) return (n / 1_000).toFixed(1) + "K";
  return n.toLocaleString();
}

function fmtUsd(n) {
  return "$" + n.toFixed(2);
}

function sourceTag(source) {
  if (source === "simulated") {
    return `<span class="badge badge--simulated badge--sm">Simulated</span>`;
  }
  if (source === "live") {
    return `<span class="badge badge--live badge--sm">Live</span>`;
  }
  return "";
}

/* ── Connection Status ──────────────────────────────── */

function updateConnectionStatus(connected, error) {
  const pill = document.getElementById("status-pill");
  const walletPill = document.getElementById("wallet-pill");

  if (connected) {
    pill.className = "pill pill--live";
    pill.innerHTML = `<span class="dot dot--green"></span><span>Gateway Connected</span>`;
  } else {
    pill.className = "pill pill--offline";
    pill.innerHTML = `<span class="dot dot--red"></span><span>Gateway Offline${error ? " — Using Estimates" : ""}</span>`;
  }
}

/* ── Summary Row ────────────────────────────────────── */

function renderSummary() {
  const s = computeSummary();
  const el = document.getElementById("summary-row");
  const verifiedApks = AGENTS.reduce((n, a) =>
    n + (a.productions || []).filter(p => p.status === "verified").length, 0);

  el.innerHTML = `
    <div class="summary-card">
      <div class="summary-card__glow" style="background: var(--accent);"></div>
      <div class="summary-card__icon">&#x1F916;</div>
      <div class="summary-card__label">Active Agents</div>
      <div class="summary-card__value">${s.active}</div>
      <div class="summary-card__delta">${s.planned} planned &middot; ${s.total} total</div>
    </div>
    <div class="summary-card">
      <div class="summary-card__glow" style="background: var(--blue);"></div>
      <div class="summary-card__icon">&#x1F4CA;</div>
      <div class="summary-card__label">Total Tokens (30d)</div>
      <div class="summary-card__value">${fmtTokens(s.totalTokens)}</div>
      <div class="summary-card__delta">${sourceTag(s.tokensSource)}</div>
    </div>
    <div class="summary-card">
      <div class="summary-card__glow" style="background: var(--red);"></div>
      <div class="summary-card__icon">&#x1F4B8;</div>
      <div class="summary-card__label">Total Cost (30d)</div>
      <div class="summary-card__value">${fmtUsd(s.totalCost)}</div>
      <div class="summary-card__delta">${sourceTag(s.tokensSource)}</div>
    </div>
    <div class="summary-card">
      <div class="summary-card__glow" style="background: var(--green);"></div>
      <div class="summary-card__icon">&#x1F4B0;</div>
      <div class="summary-card__label">Total Revenue</div>
      <div class="summary-card__value">${fmtUsd(s.totalRevenue)}</div>
      <div class="summary-card__delta summary-card__delta--up">wallet pending</div>
    </div>
    <div class="summary-card">
      <div class="summary-card__glow" style="background: var(--purple);"></div>
      <div class="summary-card__icon">&#x1F4E6;</div>
      <div class="summary-card__label">Products Shipped</div>
      <div class="summary-card__value">${s.totalProds}</div>
      <div class="summary-card__delta summary-card__delta--up">${verifiedApks} verified APK${verifiedApks !== 1 ? "s" : ""}</div>
    </div>
    <div class="summary-card">
      <div class="summary-card__glow" style="background: var(--orange);"></div>
      <div class="summary-card__icon">&#x1F4AC;</div>
      <div class="summary-card__label">Sessions (30d)</div>
      <div class="summary-card__value">${s.totalSessions}</div>
      <div class="summary-card__delta">${sourceTag(s.tokensSource)}</div>
    </div>
  `;
}

/* ── Agent Cards ────────────────────────────────────── */

function renderAgentMetrics(agent) {
  const m = agent.metrics;
  let html = "";

  const items = [
    { key: "tokensUsed",   label: "Tokens Used" },
    { key: "costUsd",      label: "Cost",         fmt: fmtUsd },
    { key: "revenueUsd",   label: "Revenue",      fmt: fmtUsd },
    { key: "sessions",     label: "Sessions" },
    { key: "inputTokens",  label: "Input Tokens" },
    { key: "outputTokens", label: "Output Tokens" },
    { key: "cacheRead",    label: "Cache Read" },
  ];

  for (const item of items) {
    const metric = m[item.key];
    if (!metric || (metric.value === 0 && item.key !== "revenueUsd" && item.key !== "costUsd")) continue;

    const val = item.fmt ? item.fmt(metric.value) : fmtTokens(metric.value);
    html += `
      <div class="metric">
        <div class="metric__label">${item.label}</div>
        <div class="metric__value">${val}</div>
        <div class="metric__sub">${metric.period ?? ""} ${sourceTag(metric.source)}</div>
      </div>`;
  }

  // Special: production count for agents with productions
  if (agent.productions?.length > 0) {
    html += `
      <div class="metric">
        <div class="metric__label">Products</div>
        <div class="metric__value">${agent.productions.length}</div>
        <div class="metric__sub">${sourceTag(SRC.LIVE)}</div>
      </div>`;
  }

  if (!html) {
    html = `
      <div class="metric" style="grid-column: 1 / -1;">
        <div class="metric__label">Status</div>
        <div class="metric__value" style="font-size: 14px; color: var(--text-muted);">Not yet operational</div>
      </div>`;
  }

  return html;
}

function statusClass(status) {
  if (status === "active") return "agent-card__status--active";
  if (status === "idle") return "agent-card__status--idle";
  return "agent-card__status--planned";
}

function statusDot(status) {
  if (status === "active") return "dot--green";
  if (status === "idle") return "dot--yellow";
  return "";
}

function renderROIBar(agent) {
  const cost = agent.metrics.costUsd?.value ?? 0;
  const revenue = agent.metrics.revenueUsd?.value ?? 0;
  if (cost === 0 && revenue === 0) return "";

  const total = Math.max(cost, revenue, 0.01);
  const costPct = Math.min((cost / total) * 100, 100);
  const revPct = Math.min((revenue / total) * 100, 100);

  return `
    <div class="agent-card__bar">
      <div class="bar-label">
        <span>Cost: ${fmtUsd(cost)}</span>
        <span>Revenue: ${fmtUsd(revenue)}</span>
      </div>
      <div class="bar-track">
        <div class="bar-fill" style="width: ${costPct}%; background: var(--red);"></div>
      </div>
      <div class="bar-track" style="margin-top: 4px;">
        <div class="bar-fill" style="width: ${revPct}%; background: var(--green);"></div>
      </div>
    </div>`;
}

function renderAgentCards() {
  const grid = document.getElementById("agent-grid");
  const countEl = document.getElementById("agent-count");
  const active = AGENTS.filter(a => a.status === "active").length;
  const planned = AGENTS.filter(a => a.status === "planned").length;
  countEl.textContent = `${AGENTS.length} agents (${active} active, ${planned} planned)`;

  grid.innerHTML = AGENTS.map(agent => `
    <div class="agent-card">
      <div class="agent-card__header">
        <div class="agent-card__avatar" style="background: ${agent.color}22; color: ${agent.color};">
          ${agent.emoji}
        </div>
        <div>
          <div class="agent-card__name">${agent.name}</div>
          <div class="agent-card__role">${agent.role}</div>
        </div>
        <div class="agent-card__status ${statusClass(agent.status)}">
          <span class="dot ${statusDot(agent.status)}"></span>
          ${agent.status}
        </div>
      </div>

      <div class="agent-card__metrics">
        ${renderAgentMetrics(agent)}
      </div>

      <div class="agent-card__model">
        <span>Model:</span>
        <code>${agent.model}</code>
      </div>

      ${renderROIBar(agent)}

      <div class="agent-card__wallet">
        <span class="agent-card__wallet-icon">&#x1F4B3;</span>
        ${agent.wallet.address
          ? `<code>${agent.wallet.address.slice(0, 8)}...${agent.wallet.address.slice(-6)}</code>`
          : `<span>Wallet not yet assigned</span>`
        }
      </div>
    </div>
  `).join("");
}

/* ── Production Table ───────────────────────────────── */

function renderProductionTable() {
  const body = document.getElementById("production-body");
  const allProductions = AGENTS.flatMap(agent =>
    (agent.productions || []).map(p => ({ ...p, agentName: agent.name, agentEmoji: agent.emoji }))
  );

  if (allProductions.length === 0) {
    body.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted);">No productions yet.</td></tr>`;
    return;
  }

  body.innerHTML = allProductions.map(p => {
    const sc = p.status === "verified" ? "status-dot--verified"
      : p.status === "compiled" ? "status-dot--compiled"
      : p.status === "demo" ? "status-dot--demo"
      : "status-dot--ready";

    return `
      <tr>
        <td style="color: var(--text); font-weight: 500;">${p.name}</td>
        <td>${p.agentEmoji} ${p.agentName}</td>
        <td>${p.type}</td>
        <td><span class="status-dot ${sc}">${p.status}</span></td>
        <td style="font-family: var(--mono);">${p.size}</td>
        <td>${p.date}</td>
      </tr>`;
  }).join("");
}

/* ── Wallet Preview Stubs ───────────────────────────── */

function renderWalletPreview() {
  const el = document.getElementById("wallet-preview");
  const activeAgents = AGENTS.filter(a => a.status === "active");

  el.innerHTML = activeAgents.map(agent => `
    <div class="wallet-stub">
      <div class="wallet-stub__agent">
        ${agent.emoji} ${agent.name}
      </div>
      <div class="wallet-stub__addr">
        ${agent.wallet.address || "0x???...??? (pending)"}
      </div>
      <div class="wallet-stub__balance">
        ${agent.wallet.balance != null ? fmtUsd(agent.wallet.balance) : "-- ETH"}
      </div>
    </div>
  `).join("");
}

/* ── Cost Breakdown ─────────────────────────────────── */

function renderCostBreakdown() {
  const el = document.getElementById("cost-chart");

  // Use live per-agent cost data if available, otherwise show per-agent estimates
  const rows = AGENTS
    .filter(a => (a.metrics.costUsd?.value ?? 0) > 0)
    .map(a => ({
      label: `${a.emoji} ${a.name}`,
      value: a.metrics.costUsd.value,
      color: a.color,
      source: a.metrics.costUsd.source,
    }))
    .sort((a, b) => b.value - a.value);

  if (rows.length === 0) {
    el.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 20px;">No cost data available. Start the gateway to see live costs.</div>`;
    return;
  }

  const maxVal = Math.max(...rows.map(r => r.value));

  // Update section badge based on data source
  const badge = document.getElementById("cost-badge");
  const allLive = rows.every(r => r.source === "live");
  if (badge) {
    badge.className = allLive ? "badge badge--live" : "badge badge--simulated";
    badge.textContent = allLive ? "Live" : "Simulated";
  }

  el.innerHTML = rows.map(item => {
    const pct = Math.max((item.value / maxVal) * 100, 2);
    return `
      <div class="cost-row">
        <div class="cost-row__label">${item.label}</div>
        <div class="cost-row__bar-wrap">
          <div class="cost-row__bar" style="width: ${pct}%; background: ${item.color};"></div>
        </div>
        <div class="cost-row__value">${fmtUsd(item.value)}</div>
      </div>`;
  }).join("");
}

/* ── Daily Cost Chart ───────────────────────────────── */

function renderDailyChart() {
  const el = document.getElementById("daily-chart");
  if (!el) return;

  if (!COST_DAILY || COST_DAILY.length === 0) {
    el.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 20px;">No daily data. Connect to gateway to see cost history.</div>`;
    return;
  }

  const maxCost = Math.max(...COST_DAILY.map(d => d.totalCost), 0.01);
  const recent = COST_DAILY.slice(-14); // last 14 days

  el.innerHTML = `
    <div class="daily-bars">
      ${recent.map(day => {
        const pct = Math.max((day.totalCost / maxCost) * 100, 1);
        const label = day.date.slice(5); // MM-DD
        return `
          <div class="daily-bar" title="${day.date}: ${fmtUsd(day.totalCost)} (${fmtTokens(day.totalTokens)} tokens)">
            <div class="daily-bar__fill" style="height: ${pct}%; background: var(--accent);"></div>
            <div class="daily-bar__label">${label}</div>
            <div class="daily-bar__value">${fmtUsd(day.totalCost)}</div>
          </div>`;
      }).join("")}
    </div>`;
}

/* ── Init & Refresh ─────────────────────────────────── */

async function refreshDashboard() {
  const btn = document.querySelector(".btn--refresh");
  if (btn) { btn.disabled = true; btn.textContent = "Loading..."; }

  const result = await loadLiveData();
  updateConnectionStatus(result.connected, result.error);

  renderSummary();
  renderAgentCards();
  renderProductionTable();
  renderWalletPreview();
  renderCostBreakdown();
  renderDailyChart();

  if (btn) { btn.disabled = false; btn.textContent = "Refresh"; }
}

document.addEventListener("DOMContentLoaded", refreshDashboard);
