<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import {
  getProjects, getAgentModels, getActivities, getChartData,
  getSummaryStats, getSystemModules, getWallet
} from '@/services/mock-data'
import {
  fetchLiveStats, fetchTimeline, fetchEvents, fetchAgents, fetchPaidStatus,
  type ApiEvent, type ApiAgent, type ApiTimelinePoint
} from '@/services/api'
import {
  FolderKanban, CreditCard, TrendingUp, DollarSign,
  Rocket, Bot, Hammer, Clock, Zap, ArrowUpRight, ChevronRight,
  Server, Smartphone, Globe, Megaphone, Wallet, CheckCircle2,
  AlertCircle, Minus, Wifi, WifiOff
} from 'lucide-vue-next'

// ── Mock baseline ────────────────────────────────────────────────────────────
const projects   = getProjects()
const activities = ref(getActivities())
const modules    = getSystemModules()
const wallet     = getWallet()

// ── Live state ───────────────────────────────────────────────────────────────
const isLive          = ref(false)   // true once finance server has real data
const financeOnline   = ref(false)
const paidConfigured  = ref(false)
const liveAgents      = ref<ApiAgent[]>([])
const liveTimeline    = ref<ApiTimelinePoint[]>([])

const stats = ref(getSummaryStats())
const agents = ref(getAgentModels())

onMounted(async () => {
  // Check server health
  const [liveStats, timeline, rawAgents, rawEvents, paidStatus] = await Promise.all([
    fetchLiveStats(),
    fetchTimeline(),
    fetchAgents(),
    fetchEvents(),
    fetchPaidStatus(),
  ])

  financeOnline.value  = true
  paidConfigured.value = paidStatus.configured
  isLive.value         = liveStats.hasRealData

  if (liveStats.hasRealData) {
    // Override KPI stats with real numbers
    stats.value = {
      activeProjects: projects.filter(p => p.status === 'live').length,
      totalSpend:    liveStats.totalSpend,
      totalRevenue:  liveStats.totalRevenue,
      profit:        liveStats.profit,
      spendTrend:    0,
      revenueTrend:  0,
    }
  }

  if (rawAgents.length > 0) {
    liveAgents.value = rawAgents
    // Synthesize agent list from real session data
    agents.value = rawAgents.slice(0, 4).map((a, i) => ({
      id: a.sessionId || a.key || `agent-${i}`,
      name: a.name || a.sessionId?.split('/').pop() || `Agent ${i + 1}`,
      provider: 'Anthropic',
      model: a.model || 'claude',
      status: (a.status as any) || 'active',
      tokensUsed: a.eventTokens || 0,
      cost: a.cost || 0,
      tasksCompleted: a.builds || 0,
      uptime: '',
    }))
  }

  if (timeline.length > 0) {
    liveTimeline.value = timeline
  }

  // Convert raw events to activity items
  if (rawEvents.length > 0) {
    activities.value = rawEvents.slice(0, 10).map((e: ApiEvent, i) => ({
      id: `evt-${i}`,
      type: e.type as any,
      message: buildEventMessage(e),
      timestamp: e.timestamp,
    }))
  }
})

function buildEventMessage(e: ApiEvent): string {
  if (e.type === 'cost') {
    const cost = (e.data.costUsd as number)?.toFixed(4) || '0.0000'
    const model = e.data.model as string || 'unknown model'
    return `$${cost} API cost — ${model} (${e.agentId})`
  }
  if (e.type === 'revenue') {
    const amt = (e.data.amountUsd as number)?.toFixed(2) || '0.00'
    return `$${amt} revenue — ${e.data.source || 'unknown source'}`
  }
  if (e.type === 'build') {
    const app = e.data.appName as string || 'unknown app'
    const ok = e.data.success ? 'succeeded' : 'failed'
    return `Build ${ok} — ${app} (${e.agentId})`
  }
  return `${e.type} event — ${e.agentId}`
}

// ── Chart data (live timeline or mock) ──────────────────────────────────────
const chartData = computed(() => {
  if (liveTimeline.value.length > 0) {
    return liveTimeline.value.map(p => ({ label: p.time, cost: p.cost, revenue: p.revenue }))
  }
  return getChartData()
})

// ── Chart ──────────────────────────────────────────────────────────────────
const chartH = 160
const chartW = 700
const maxVal = computed(() => {
  const d = chartData.value
  if (!d.length) return 1
  return Math.max(...d.map(p => Math.max(p.cost, p.revenue))) * 1.15
})
const n = computed(() => chartData.value.length || 1)
const step = computed(() => chartW / (n.value - 1 || 1))

function pt(i: number, val: number) {
  return { x: i * step.value, y: (1 - val / maxVal.value) * chartH }
}
function toPath(key: 'cost' | 'revenue') {
  return chartData.value.map((d, i) => `${i === 0 ? 'M' : 'L'}${pt(i, d[key]).x.toFixed(1)},${pt(i, d[key]).y.toFixed(1)}`).join(' ')
}
function toArea(key: 'cost' | 'revenue') {
  return `${toPath(key)} L${(step.value * (n.value - 1)).toFixed(1)},${chartH} L0,${chartH} Z`
}
const totalCost    = computed(() => chartData.value.reduce((s, d) => s + d.cost, 0))
const totalRevenue = computed(() => chartData.value.reduce((s, d) => s + d.revenue, 0))

// ── Projects ───────────────────────────────────────────────────────────────
const topProjects = computed(() => projects.slice(0, 5))
const statusMeta: Record<string, { dot: string }> = {
  live:     { dot: '#4ade80' },
  building: { dot: '#4f8ef7' },
  paused:   { dot: '#fbbf24' },
  failed:   { dot: '#f87171' },
}
function roi(p: { cost: number; revenue: number }) {
  if (!p.cost || !p.revenue) return null
  return Math.round((p.revenue / p.cost) * 100) + '%'
}
function fmtUsd(v: number) { return '$' + v.toFixed(2) }

// ── Agents ─────────────────────────────────────────────────────────────────
const agentStatusColor: Record<string, string> = {
  active: '#4ade80', idle: '#fbbf24', stopped: '#6b6460'
}
const providerColor: Record<string, string> = {
  Anthropic: '#cc785c', Google: '#4f8ef7'
}
function fmtTokens(v: number) {
  if (v >= 1_000_000) return (v / 1_000_000).toFixed(1) + 'M'
  if (v >= 1_000) return Math.round(v / 1_000) + 'K'
  return String(v)
}

// ── Modules ────────────────────────────────────────────────────────────────
const moduleIcon: Record<string, any> = {
  infra: Server, android: Smartphone, webapp: Globe, marketing: Megaphone, finance: Wallet
}
const moduleStatusIcon: Record<string, any> = {
  active: CheckCircle2, idle: Minus, error: AlertCircle
}
const moduleStatusColor: Record<string, string> = {
  active: 'var(--color-success)', idle: 'var(--color-text-muted)', error: 'var(--color-danger)'
}

// ── Activity ───────────────────────────────────────────────────────────────
const activityIcon: Record<string, any> = {
  build: Hammer, deploy: Rocket, revenue: DollarSign, cost: CreditCard,
  agent: Bot, marketing: Megaphone, infra: Server
}
const activityColor: Record<string, string> = {
  build: 'icon-blue', deploy: 'icon-violet', revenue: 'icon-emerald',
  cost: 'icon-red', agent: 'icon-amber', marketing: 'icon-blue', infra: 'icon-violet'
}
function timeAgo(ts: string) {
  const m = Math.floor((Date.now() - new Date(ts).getTime()) / 60000)
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}
</script>

<template>
  <div class="space-y-5 pb-6">

    <!-- ── Data source banner ────────────────────────────────── -->
    <div v-if="financeOnline" :class="[
      'flex items-center gap-2.5 px-4 py-2.5 rounded-xl text-xs font-medium',
      isLive
        ? 'bg-[rgba(74,222,128,0.08)] border border-[rgba(74,222,128,0.2)] text-[#4ade80]'
        : 'bg-[rgba(251,191,36,0.08)] border border-[rgba(251,191,36,0.2)] text-[#fbbf24]'
    ]">
      <component :is="isLive ? Wifi : WifiOff" :size="13" />
      <span v-if="isLive">Live data from finance server · Paid.ai {{ paidConfigured ? 'connected' : 'not configured' }}</span>
      <span v-else>Finance server online · No agent events yet — showing demo data · Start OpenClaw to populate</span>
    </div>

    <!-- ── Row 1: KPIs ────────────────────────────────────── -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">

      <div class="kpi-card">
        <div class="flex items-start justify-between">
          <div>
            <p class="kpi-label">Active Projects</p>
            <p class="kpi-value">{{ stats.activeProjects }}</p>
            <p class="kpi-sub text-[var(--color-text-muted)]">of {{ projects.length }} total</p>
          </div>
          <div class="kpi-icon icon-stat-blue"><FolderKanban :size="18" /></div>
        </div>
        <div class="mt-4 flex gap-1 h-1.5 rounded-full overflow-hidden">
          <div class="h-full rounded-full" style="width:37%;background:#4ade80" title="Live" />
          <div class="h-full rounded-full" style="width:25%;background:#4f8ef7" title="Building" />
          <div class="h-full rounded-full" style="width:13%;background:#fbbf24" title="Paused" />
          <div class="h-full rounded-full" style="width:25%;background:#f87171" title="Failed" />
        </div>
        <div class="mt-1.5 flex flex-wrap gap-x-3 gap-y-0.5">
          <span v-for="[c, l] in [['#4ade80','Live'],['#4f8ef7','Building'],['#fbbf24','Paused'],['#f87171','Failed']]" :key="l"
                class="flex items-center gap-1 text-[10px] text-[var(--color-text-muted)]">
            <span class="live-dot" :style="{ background: c }" />{{ l }}
          </span>
        </div>
      </div>

      <div class="kpi-card">
        <div class="flex items-start justify-between">
          <div>
            <p class="kpi-label">API Spend</p>
            <p class="kpi-value">{{ fmtUsd(stats.totalSpend) }}</p>
            <p v-if="stats.spendTrend" class="kpi-sub" style="color:var(--color-success)"><ArrowUpRight :size="11" />+{{ stats.spendTrend }}% vs last week</p>
          </div>
          <div class="kpi-icon icon-stat-red"><CreditCard :size="18" /></div>
        </div>
        <div class="mt-4 flex items-end gap-0.5 h-8">
          <div v-for="(d, i) in chartData" :key="i" class="flex-1 rounded-sm transition-all"
               style="background:rgba(248,113,113,0.55)" :style="{ height: `${(d.cost / 8.3) * 100}%` }" />
        </div>
      </div>

      <div class="kpi-card">
        <div class="flex items-start justify-between">
          <div>
            <p class="kpi-label">Revenue</p>
            <p class="kpi-value">{{ fmtUsd(stats.totalRevenue) }}</p>
            <p class="kpi-sub" style="color:var(--color-success)"><ArrowUpRight :size="11" />+{{ stats.revenueTrend }}% vs last week</p>
          </div>
          <div class="kpi-icon icon-stat-emerald"><TrendingUp :size="18" /></div>
        </div>
        <div class="mt-4 flex items-end gap-0.5 h-8">
          <div v-for="(d, i) in chartData" :key="i" class="flex-1 rounded-sm transition-all"
               style="background:rgba(74,222,128,0.55)" :style="{ height: `${(d.revenue / 28.7) * 100}%` }" />
        </div>
      </div>

      <div class="kpi-card">
        <div class="flex items-start justify-between">
          <div>
            <p class="kpi-label">Agent Wallet</p>
            <p class="kpi-value">{{ wallet.solBalance }} SOL</p>
            <p class="kpi-sub text-[var(--color-text-muted)]">≈ {{ fmtUsd(wallet.solUsdValue) }}</p>
          </div>
          <div class="kpi-icon icon-stat-violet"><Wallet :size="18" /></div>
        </div>
        <div class="mt-4">
          <div class="h-1.5 rounded-full overflow-hidden bg-[var(--color-border)]">
            <div class="h-full rounded-full bg-[#a78bfa] transition-all duration-700" style="width:73%" />
          </div>
          <p class="mt-1.5 text-[10px] text-[var(--color-text-muted)]">{{ fmtUsd(stats.profit) }} net profit this week</p>
        </div>
      </div>
    </div>

    <!-- ── Row 2: System Modules ──────────────────────────── -->
    <div>
      <div class="flex items-baseline gap-3 mb-3">
        <h2 class="section-heading">System Modules</h2>
        <span class="text-xs text-[var(--color-text-muted)]">Powered by OpenClaw · Claude · Gemini · LangGraph</span>
      </div>
      <div class="grid sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <div v-for="mod in modules" :key="mod.id" class="module-card">
          <div class="flex items-center justify-between mb-3">
            <div class="module-icon-wrap">
              <component :is="moduleIcon[mod.id]" :size="16" />
            </div>
            <component
              :is="moduleStatusIcon[mod.status]"
              :size="14"
              :style="{ color: moduleStatusColor[mod.status] }"
            />
          </div>
          <p class="text-sm font-semibold text-[var(--color-text)] leading-tight">{{ mod.name }}</p>
          <p class="text-[11px] text-[var(--color-text-muted)] mt-1 leading-snug mb-3">{{ mod.description }}</p>
          <div class="mt-auto">
            <p class="text-base font-bold text-[var(--color-text)]">{{ mod.metric }}</p>
            <p class="text-[10px] text-[var(--color-text-muted)]">{{ mod.metricLabel }}</p>
            <div class="mt-2.5 flex flex-wrap gap-1">
              <span v-for="tag in mod.integrations.slice(0, 3)" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Row 3: Chart + Projects ────────────────────────── -->
    <div class="grid lg:grid-cols-5 gap-4">

      <div class="lg:col-span-3 panel">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h2 class="panel-title">Cost vs Revenue</h2>
            <p class="panel-sub">Last 7 days across all projects</p>
          </div>
          <div class="flex items-center gap-5 text-xs">
            <div class="flex flex-col items-end gap-0.5">
              <span class="legend-label">Cost</span>
              <span class="font-semibold font-mono" style="color:#f87171">${{ totalCost.toFixed(2) }}</span>
            </div>
            <div class="flex flex-col items-end gap-0.5">
              <span class="legend-label">Revenue</span>
              <span class="font-semibold font-mono" style="color:#4ade80">${{ totalRevenue.toFixed(2) }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-4 mb-3">
          <span class="legend-item"><span class="legend-dot" style="background:#f87171" />Cost</span>
          <span class="legend-item"><span class="legend-dot" style="background:#4ade80" />Revenue</span>
        </div>
        <svg :viewBox="`0 0 ${chartW} ${chartH}`" class="w-full" :style="`height:${chartH}px`" preserveAspectRatio="none">
          <defs>
            <linearGradient id="gCost" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#f87171" stop-opacity="0.25" />
              <stop offset="100%" stop-color="#f87171" stop-opacity="0" />
            </linearGradient>
            <linearGradient id="gRev" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#4ade80" stop-opacity="0.2" />
              <stop offset="100%" stop-color="#4ade80" stop-opacity="0" />
            </linearGradient>
          </defs>
          <line v-for="i in 3" :key="i" x1="0" :y1="(chartH / 4) * i" :x2="chartW" :y2="(chartH / 4) * i"
                stroke="var(--color-border)" stroke-dasharray="4,6" stroke-width="1" />
          <path :d="toArea('cost')"    fill="url(#gCost)" />
          <path :d="toArea('revenue')" fill="url(#gRev)" />
          <path :d="toPath('cost')"    fill="none" stroke="#f87171" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          <path :d="toPath('revenue')" fill="none" stroke="#4ade80" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          <circle v-for="(d, i) in chartData" :key="'c'+i" :cx="pt(i,d.cost).x" :cy="pt(i,d.cost).y" r="3.5"
                  fill="var(--color-bg-elevated)" stroke="#f87171" stroke-width="2" />
          <circle v-for="(d, i) in chartData" :key="'r'+i" :cx="pt(i,d.revenue).x" :cy="pt(i,d.revenue).y" r="3.5"
                  fill="var(--color-bg-elevated)" stroke="#4ade80" stroke-width="2" />
        </svg>
        <div class="flex justify-between mt-2">
          <span v-for="d in chartData" :key="d.label" class="text-[10px] text-[var(--color-text-muted)]">{{ d.label }}</span>
        </div>
      </div>

      <div class="lg:col-span-2 panel flex flex-col">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h2 class="panel-title">Projects</h2>
            <p class="panel-sub">Performance overview</p>
          </div>
          <router-link to="/app/projects" class="see-more">View all <ChevronRight :size="13" /></router-link>
        </div>
        <div class="flex flex-col gap-1 flex-1">
          <div v-for="p in topProjects" :key="p.id" class="proj-row">
            <span class="live-dot" :style="{ background: statusMeta[p.status]?.dot }" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-1.5">
                <p class="text-sm font-medium text-[var(--color-text)] truncate">{{ p.name }}</p>
                <span class="type-badge">{{ p.type }}</span>
              </div>
              <p class="text-[11px] text-[var(--color-text-muted)]">{{ p.model }}</p>
            </div>
            <div class="text-right flex-shrink-0">
              <p class="text-xs font-mono font-semibold"
                 :style="{ color: p.revenue > 0 ? 'var(--color-success)' : 'var(--color-text-muted)' }">
                {{ p.revenue > 0 ? fmtUsd(p.revenue) : '—' }}
              </p>
              <p v-if="roi(p)" class="text-[10px] text-[var(--color-text-muted)]">{{ roi(p) }} ROI</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ── Row 4: AI Models + Activity ────────────────────── -->
    <div class="grid lg:grid-cols-5 gap-4">

      <!-- AI Engine status -->
      <div class="lg:col-span-2 panel">
        <div class="flex items-center justify-between mb-3">
          <div>
            <h2 class="panel-title">AI Engine</h2>
            <p class="panel-sub">Active model instances</p>
          </div>
          <router-link to="/app/agents" class="see-more">Manage <ChevronRight :size="13" /></router-link>
        </div>
        <div class="flex flex-col gap-2">
          <div v-for="a in agents" :key="a.id" class="agent-row">
            <div class="flex items-center gap-2.5 flex-1 min-w-0">
              <div class="relative flex-shrink-0">
                <div class="w-8 h-8 rounded-lg flex items-center justify-center text-[11px] font-bold"
                     :style="{ background: providerColor[a.provider] + '25', color: providerColor[a.provider] }">
                  {{ a.provider === 'Anthropic' ? 'AN' : 'GG' }}
                </div>
                <span class="status-pip" :style="{ background: agentStatusColor[a.status] }" />
              </div>
              <div class="min-w-0">
                <p class="text-sm font-semibold text-[var(--color-text)] truncate">{{ a.name }}</p>
                <p class="text-[11px] text-[var(--color-text-muted)] font-mono truncate">{{ a.model }}</p>
              </div>
            </div>
            <div class="flex gap-4 text-xs flex-shrink-0">
              <div class="text-right">
                <p class="font-mono text-[var(--color-text-secondary)]">{{ fmtTokens(a.tokensUsed) }}</p>
                <p class="text-[10px] text-[var(--color-text-muted)]">tokens</p>
              </div>
              <div class="text-right">
                <p class="font-mono text-[var(--color-text-secondary)]">{{ fmtUsd(a.cost) }}</p>
                <p class="text-[10px] text-[var(--color-text-muted)]">cost</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Activity feed -->
      <div class="lg:col-span-3 panel">
        <div class="mb-3">
          <h2 class="panel-title">Live Activity</h2>
          <p class="panel-sub">Real-time events across all modules</p>
        </div>
        <div class="flex flex-col divide-y divide-[var(--color-border)]">
          <div v-for="a in activities" :key="a.id"
               class="flex items-center gap-3 py-2.5 first:pt-0 last:pb-0">
            <div :class="['w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0', activityColor[a.type]]">
              <component :is="activityIcon[a.type]" :size="12" />
            </div>
            <p class="flex-1 text-xs text-[var(--color-text-secondary)] leading-snug">{{ a.message }}</p>
            <span class="text-[10px] text-[var(--color-text-muted)] flex-shrink-0 whitespace-nowrap">{{ timeAgo(a.timestamp) }}</span>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.kpi-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: var(--shadow-sm);
  transition: border-color 200ms;
}
.kpi-card:hover { border-color: var(--color-border-strong); }
.kpi-label {
  font-size: 11px; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--color-text-muted); margin-bottom: 6px;
}
.kpi-value {
  font-size: 1.65rem; font-weight: 700;
  letter-spacing: -0.03em; line-height: 1;
  color: var(--color-text);
}
.kpi-sub { margin-top: 5px; font-size: 11px; display: flex; align-items: center; gap: 3px; }
.kpi-icon { width: 38px; height: 38px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.section-heading { font-size: 15px; font-weight: 700; color: var(--color-text); letter-spacing: -0.02em; }

.module-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 16px;
  display: flex; flex-direction: column;
  box-shadow: var(--shadow-sm);
  transition: border-color 150ms, box-shadow 150ms;
}
.module-card:hover { border-color: var(--color-border-strong); box-shadow: var(--shadow-md); }
.module-icon-wrap {
  width: 32px; height: 32px; border-radius: 8px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  display: flex; align-items: center; justify-content: center;
  color: var(--color-text-secondary);
}
.tag {
  font-size: 10px; font-weight: 500;
  padding: 2px 7px; border-radius: 99px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
}

.panel {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px; padding: 20px;
  box-shadow: var(--shadow-sm);
}
.panel-title { font-size: 14px; font-weight: 600; color: var(--color-text); letter-spacing: -0.01em; }
.panel-sub { font-size: 11px; color: var(--color-text-muted); margin-top: 2px; }

.see-more {
  display: flex; align-items: center; gap: 2px;
  font-size: 11px; color: var(--color-accent);
  transition: opacity 150ms;
  text-decoration: none;
}
.see-more:hover { opacity: 0.75; }

.legend-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-text-muted); }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 11px; color: var(--color-text-muted); }
.legend-dot { width: 16px; height: 2px; border-radius: 99px; display: inline-block; }

.live-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }

.proj-row {
  display: flex; align-items: center; gap: 10px;
  padding: 7px 10px; border-radius: 10px;
  transition: background 150ms;
}
.proj-row:hover { background: var(--color-bg-subtle); }
.type-badge {
  font-size: 9px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em;
  padding: 1px 5px; border-radius: 4px;
  background: var(--color-bg-subtle); color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.agent-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 10px; padding: 10px 10px;
  border-radius: 10px; transition: background 150ms;
}
.agent-row:hover { background: var(--color-bg-subtle); }
.status-pip {
  position: absolute; bottom: -1px; right: -1px;
  width: 9px; height: 9px; border-radius: 50%;
  border: 2px solid var(--color-bg-elevated);
}
</style>
