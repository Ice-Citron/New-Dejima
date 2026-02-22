// ── Types ────────────────────────────────────────────────────────────────────

export interface Project {
  id: string
  name: string
  description: string
  type: 'android' | 'webapp'
  status: 'live' | 'building' | 'paused' | 'failed'
  model: string      // which AI model is building it
  cost: number
  revenue: number
  builds: number
  createdAt: string
}

export interface AgentModel {
  id: string
  name: string        // e.g. "Claude Opus 4"
  provider: string    // "Anthropic" | "Google"
  model: string       // model slug
  status: 'active' | 'idle' | 'stopped'
  tokensUsed: number
  cost: number
  tasksCompleted: number
  uptime: string
}

export interface SystemModule {
  id: string
  name: string
  description: string
  icon: string          // emoji shorthand for display
  status: 'active' | 'idle' | 'error'
  metric: string        // primary metric value
  metricLabel: string
  integrations: string[]
}

export interface Activity {
  id: string
  type: 'build' | 'deploy' | 'revenue' | 'cost' | 'agent' | 'marketing' | 'infra'
  message: string
  timestamp: string
}

export interface BillingItem {
  id: string
  date: string
  description: string
  amount: number
  status: 'paid' | 'pending' | 'failed'
}

export interface WalletInfo {
  solBalance: number
  solUsdValue: number
  totalEarned: number
  lastTx: string
}

// ── Data ─────────────────────────────────────────────────────────────────────

export function getProjects(): Project[] {
  return [
    { id: 'proj_001', name: 'Golf Deals', description: 'AI-curated golf equipment price comparison', type: 'android', status: 'live', model: 'claude-opus-4', cost: 12.48, revenue: 87.20, builds: 14, createdAt: '2026-02-18T10:00:00Z' },
    { id: 'proj_002', name: 'MealPrep AI', description: 'Personalized meal planning & grocery lists', type: 'android', status: 'building', model: 'claude-sonnet-4', cost: 4.32, revenue: 0, builds: 3, createdAt: '2026-02-20T14:30:00Z' },
    { id: 'proj_003', name: 'ParkFinder', description: 'Real-time nearby parking with live pricing', type: 'android', status: 'live', model: 'claude-opus-4', cost: 8.91, revenue: 34.50, builds: 9, createdAt: '2026-02-15T09:00:00Z' },
    { id: 'proj_004', name: 'StudyBuddy', description: 'Flashcard generator from lecture notes', type: 'android', status: 'paused', model: 'gemini-2.5-flash', cost: 2.10, revenue: 12.00, builds: 5, createdAt: '2026-02-12T16:00:00Z' },
    { id: 'proj_005', name: 'FitTrack', description: 'Workout tracker with AI form analysis', type: 'android', status: 'failed', model: 'claude-sonnet-4', cost: 6.78, revenue: 0, builds: 7, createdAt: '2026-02-19T11:00:00Z' },
    { id: 'proj_006', name: 'BudgetLens', description: 'Receipt scanner & expense categorizer', type: 'android', status: 'building', model: 'gemini-2.5-flash', cost: 1.45, revenue: 0, builds: 1, createdAt: '2026-02-21T08:00:00Z' },
    { id: 'proj_007', name: 'DejimaLanding', description: 'New Dejima marketing & SaaS website', type: 'webapp', status: 'live', model: 'gemini-2.0-flash', cost: 3.20, revenue: 0, builds: 6, createdAt: '2026-02-17T12:00:00Z' },
    { id: 'proj_008', name: 'AgentPortal', description: 'Internal agent management dashboard', type: 'webapp', status: 'building', model: 'claude-sonnet-4', cost: 2.80, revenue: 0, builds: 4, createdAt: '2026-02-21T16:00:00Z' },
  ]
}

export function getAgentModels(): AgentModel[] {
  return [
    { id: 'claude-opus-4', name: 'Claude Opus 4', provider: 'Anthropic', model: 'claude-opus-4-6', status: 'active', tokensUsed: 2_450_000, cost: 21.39, tasksCompleted: 23, uptime: '4d 12h' },
    { id: 'claude-sonnet-4', name: 'Claude Sonnet 4', provider: 'Anthropic', model: 'claude-sonnet-4-6', status: 'active', tokensUsed: 1_120_000, cost: 11.10, tasksCompleted: 10, uptime: '2d 8h' },
    { id: 'gemini-2.5-flash', name: 'Gemini 2.5 Flash', provider: 'Google', model: 'gemini-2.5-flash', status: 'idle', tokensUsed: 680_000, cost: 2.10, tasksCompleted: 5, uptime: '6d 1h' },
    { id: 'gemini-2.0-flash', name: 'Gemini 2.0 Flash', provider: 'Google', model: 'gemini-2.0-flash-exp', status: 'active', tokensUsed: 340_000, cost: 1.45, tasksCompleted: 1, uptime: '0d 18h' },
  ]
}

/** Legacy alias so old pages don't break */
export function getAgents() {
  return getAgentModels().map(a => ({ ...a, id: a.id }))
}

export function getSystemModules(): SystemModule[] {
  return [
    {
      id: 'infra',
      name: 'Infrastructure',
      description: 'GCP Compute VMs, auto-scaled per project build',
      icon: '☁️',
      status: 'active',
      metric: '4',
      metricLabel: 'VMs running',
      integrations: ['GCP', 'Solana', 'Stripe'],
    },
    {
      id: 'android',
      name: 'Android Apps',
      description: 'Full-stack app generation, Gradle build & Play Store deploy',
      icon: '📱',
      status: 'active',
      metric: '2',
      metricLabel: 'Builds in progress',
      integrations: ['Claude', 'Gemini', 'Spotify API', '+11 more'],
    },
    {
      id: 'webapp',
      name: 'Web Apps',
      description: 'Lovable-powered SaaS & landing page generation',
      icon: '🌐',
      status: 'active',
      metric: '2',
      metricLabel: 'Projects live',
      integrations: ['Lovable', 'Claude', 'Gemini'],
    },
    {
      id: 'marketing',
      name: 'Marketing',
      description: 'Automated video, audio & social content distribution',
      icon: '📣',
      status: 'active',
      metric: '6',
      metricLabel: 'Campaigns running',
      integrations: ['Veo 2.1', 'Imagen 3', 'ElevenLabs', 'YouTube', 'Reddit', 'Twitter'],
    },
    {
      id: 'finance',
      name: 'Finance',
      description: 'Agent-controlled Solana wallet & Stripe revenue routing',
      icon: '◎',
      status: 'active',
      metric: '12.4 SOL',
      metricLabel: 'Agent wallet balance',
      integrations: ['Solana', 'Stripe', 'Visa'],
    },
  ]
}

export function getWallet(): WalletInfo {
  return {
    solBalance: 12.4,
    solUsdValue: 2418.80,
    totalEarned: 133.70,
    lastTx: '2026-02-22T09:14:00Z',
  }
}

export function getActivities(): Activity[] {
  return [
    { id: 'a1', type: 'deploy', message: 'Golf Deals v2.1 deployed to Google Play Store', timestamp: '2026-02-22T09:14:00Z' },
    { id: 'a2', type: 'revenue', message: '$12.50 earned — Golf Deals ad impressions', timestamp: '2026-02-22T08:30:00Z' },
    { id: 'a3', type: 'marketing', message: 'Veo 2.1 generated 3 promo clips for MealPrep AI', timestamp: '2026-02-22T08:10:00Z' },
    { id: 'a4', type: 'build', message: 'MealPrep AI build #3 complete (Claude Sonnet 4)', timestamp: '2026-02-22T07:45:00Z' },
    { id: 'a5', type: 'infra', message: 'GCP VM spin-up — us-central1-a (BudgetLens build)', timestamp: '2026-02-22T07:30:00Z' },
    { id: 'a6', type: 'cost', message: 'Claude Opus 4 call — $0.34 (Golf Deals feature build)', timestamp: '2026-02-22T07:20:00Z' },
    { id: 'a7', type: 'agent', message: 'Gemini 2.5 Flash started BudgetLens module', timestamp: '2026-02-22T06:00:00Z' },
    { id: 'a8', type: 'revenue', message: '$8.00 earned — ParkFinder subscriptions', timestamp: '2026-02-21T22:10:00Z' },
    { id: 'a9', type: 'marketing', message: 'ElevenLabs narration posted to YouTube (ParkFinder)', timestamp: '2026-02-21T21:00:00Z' },
    { id: 'a10', type: 'build', message: 'FitTrack build #7 failed — Gradle dependency error', timestamp: '2026-02-21T20:30:00Z' },
  ]
}

export function getBilling(): BillingItem[] {
  return [
    { id: 'bill_001', date: '2026-02-22', description: 'Claude Opus 4.6 — API usage', amount: 15.20, status: 'pending' },
    { id: 'bill_002', date: '2026-02-21', description: 'Claude Sonnet 4.6 — API usage', amount: 8.45, status: 'paid' },
    { id: 'bill_003', date: '2026-02-21', description: 'Gemini 2.5 Flash — API usage', amount: 1.30, status: 'paid' },
    { id: 'bill_004', date: '2026-02-20', description: 'ElevenLabs TTS — 4 voiceovers', amount: 2.10, status: 'paid' },
    { id: 'bill_005', date: '2026-02-20', description: 'Veo 2.1 — 3 video generations', amount: 4.80, status: 'paid' },
    { id: 'bill_006', date: '2026-02-19', description: 'GCP Compute — VM hours', amount: 3.20, status: 'paid' },
    { id: 'bill_007', date: '2026-02-18', description: 'Vertex AI — Imagen 3 renders', amount: 1.70, status: 'paid' },
  ]
}

export function getSummaryStats() {
  return {
    activeProjects: 3,
    totalSpend: 36.75,
    totalRevenue: 133.70,
    profit: 96.95,
    spendTrend: +12,
    revenueTrend: +24,
  }
}

export function getChartData() {
  return [
    { label: 'Mon', cost: 4.2, revenue: 12.5 },
    { label: 'Tue', cost: 6.8, revenue: 18.3 },
    { label: 'Wed', cost: 5.1, revenue: 22.1 },
    { label: 'Thu', cost: 8.3, revenue: 28.7 },
    { label: 'Fri', cost: 4.9, revenue: 19.4 },
    { label: 'Sat', cost: 3.2, revenue: 15.8 },
    { label: 'Sun', cost: 3.5, revenue: 16.9 },
  ]
}
