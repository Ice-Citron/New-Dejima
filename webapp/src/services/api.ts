/**
 * api.ts — Real API service layer for New Dejima
 *
 * Connects to the finance server at localhost:3456.
 * Falls back gracefully to mock data when the backend returns empty
 * (i.e. no agents have run yet).
 *
 * As soon as OpenClaw agents start running and posting events,
 * this service will automatically surface real data.
 */

const BASE = 'http://localhost:3456'

// ── Helpers ──────────────────────────────────────────────────────────────────

async function get<T>(path: string): Promise<T | null> {
    try {
        const res = await fetch(`${BASE}${path}`)
        if (!res.ok) return null
        return res.json()
    } catch {
        return null  // server down or network error
    }
}

// ── Types ─────────────────────────────────────────────────────────────────────

export interface ApiEvent {
    type: 'cost' | 'revenue' | 'build' | string
    agentId: string
    timestamp: string
    data: Record<string, unknown>
}

export interface ApiAgent {
    sessionId: string
    key: string
    name?: string
    model?: string
    status?: string
    lastActive?: string
    cost: number
    revenue: number
    builds: number
    eventTokens: number
}

export interface ApiAgentSummary {
    [agentId: string]: {
        cost: number
        revenue: number
        builds: number
        tokens: number
    }
}

export interface ApiTimelinePoint {
    time: string
    cost: number
    revenue: number
}

export interface PaidUsage {
    totalCostUsd?: number
    totalRevenueUsd?: number
    period?: string
    [key: string]: unknown
}

// ── API calls ─────────────────────────────────────────────────────────────────

/** Raw event log from OpenClaw agent hooks */
export async function fetchEvents(): Promise<ApiEvent[]> {
    return (await get<ApiEvent[]>('/api/events')) ?? []
}

/** Cost/revenue/builds per agent */
export async function fetchEconomics(): Promise<ApiAgentSummary> {
    return (await get<ApiAgentSummary>('/api/economics')) ?? {}
}

/** All running/completed agents with merged economics */
export async function fetchAgents(): Promise<ApiAgent[]> {
    return (await get<ApiAgent[]>('/api/agents')) ?? []
}

/** Per-agent cost breakdown */
export async function fetchAgentCosts(agentId: string) {
    return get(`/api/agents/${encodeURIComponent(agentId)}/costs`)
}

/** Per-agent build history */
export async function fetchAgentBuilds(agentId: string) {
    return get(`/api/agents/${encodeURIComponent(agentId)}/builds`)
}

/** Per-agent latest status (active/idle, current task) */
export async function fetchAgentStatus(agentId: string) {
    return get(`/api/agents/${encodeURIComponent(agentId)}/status`)
}

/** Time-series data for the cost vs revenue chart */
export async function fetchTimeline(): Promise<ApiTimelinePoint[]> {
    return (await get<ApiTimelinePoint[]>('/api/stats/timeline')) ?? []
}

/** Paid.ai: is the API key configured? */
export async function fetchPaidStatus(): Promise<{ configured: boolean }> {
    return (await get<{ configured: boolean }>('/api/paid/status')) ?? { configured: false }
}

/** Paid.ai: real usage summary */
export async function fetchPaidUsage(): Promise<PaidUsage | null> {
    const res = await get<{ data: PaidUsage }>('/api/paid/usage')
    return res?.data ?? null
}

/** Paid.ai: cost traces */
export async function fetchPaidTraces() {
    return get('/api/paid/traces')
}

/** Paid.ai: orders (revenue from subscriptions) */
export async function fetchPaidOrders() {
    return get('/api/paid/orders')
}

/** Paid.ai: products */
export async function fetchPaidProducts() {
    return get('/api/paid/products')
}

/** Finance server health check */
export async function checkFinanceServer(): Promise<boolean> {
    const status = await fetchPaidStatus()
    return status !== null
}

// ── Smart summary builder ─────────────────────────────────────────────────────
// Computes dashboard KPIs from real events when available

export interface LiveStats {
    totalSpend: number
    totalRevenue: number
    profit: number
    eventCount: number
    activeAgents: number
    builds: number
    hasRealData: boolean   // false when backend returned empty — using mock
}

export async function fetchLiveStats(): Promise<LiveStats> {
    const [events, economics] = await Promise.all([fetchEvents(), fetchEconomics()])

    const hasRealData = events.length > 0 || Object.keys(economics).length > 0

    let totalSpend = 0
    let totalRevenue = 0
    let builds = 0

    for (const evt of events) {
        if (evt.type === 'cost') totalSpend += (evt.data.costUsd as number) || 0
        if (evt.type === 'revenue') totalRevenue += (evt.data.amountUsd as number) || 0
        if (evt.type === 'build') builds++
    }

    const activeAgents = Object.keys(economics).length

    return {
        totalSpend: Math.round(totalSpend * 100) / 100,
        totalRevenue: Math.round(totalRevenue * 100) / 100,
        profit: Math.round((totalRevenue - totalSpend) * 100) / 100,
        eventCount: events.length,
        activeAgents,
        builds,
        hasRealData,
    }
}
