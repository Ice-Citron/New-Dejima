<script setup lang="ts">
import { getAgentModels } from '@/services/mock-data'
import AppBadge from '@/components/ui/AppBadge.vue'
import { Bot, Cpu, Clock, Zap, CreditCard, CheckCircle2, Minus } from 'lucide-vue-next'

const agents = getAgentModels()

const statusVariants: Record<string, 'success' | 'warning' | 'default'> = {
  active: 'success', idle: 'warning', stopped: 'default',
}
const providerColor: Record<string, string> = { Anthropic: '#cc785c', Google: '#4f8ef7' }
const statusDot:    Record<string, string> = { active: '#4ade80', idle: '#fbbf24', stopped: '#6b6460' }

function fmtTokens(v: number) {
  if (v >= 1_000_000) return (v / 1_000_000).toFixed(1) + 'M'
  if (v >= 1_000) return Math.round(v / 1_000) + 'K'
  return String(v)
}
function fmtUsd(v: number) { return '$' + v.toFixed(2) }

const totalCost   = agents.reduce((s, a) => s + a.cost, 0)
const totalTokens = agents.reduce((s, a) => s + a.tokensUsed, 0)
const totalTasks  = agents.reduce((s, a) => s + a.tasksCompleted, 0)
</script>

<template>
  <div class="space-y-5">

    <!-- Summary strip -->
    <div class="grid grid-cols-3 gap-4">
      <div class="strip-card">
        <p class="strip-label">Total Tokens</p>
        <p class="strip-value">{{ (totalTokens / 1_000_000).toFixed(2) }}M</p>
      </div>
      <div class="strip-card">
        <p class="strip-label">Total Cost</p>
        <p class="strip-value">{{ fmtUsd(totalCost) }}</p>
      </div>
      <div class="strip-card">
        <p class="strip-label">Tasks Completed</p>
        <p class="strip-value">{{ totalTasks }}</p>
      </div>
    </div>

    <!-- Model cards -->
    <div class="grid sm:grid-cols-2 xl:grid-cols-4 gap-4">
      <div v-for="a in agents" :key="a.id" class="model-card">
        <!-- Header -->
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="relative">
              <div class="w-10 h-10 rounded-xl flex items-center justify-center text-xs font-bold flex-shrink-0"
                   :style="{ background: providerColor[a.provider] + '20', color: providerColor[a.provider] }">
                {{ a.provider === 'Anthropic' ? 'AN' : 'GG' }}
              </div>
              <span class="status-pip" :style="{ background: statusDot[a.status] }" />
            </div>
            <div>
              <p class="text-sm font-bold text-[var(--color-text)]">{{ a.name }}</p>
              <p class="text-[11px] font-mono text-[var(--color-text-muted)]">{{ a.provider }}</p>
            </div>
          </div>
          <AppBadge :variant="statusVariants[a.status]">{{ a.status }}</AppBadge>
        </div>

        <!-- Model slug -->
        <div class="mb-4 px-3 py-2 rounded-lg bg-[var(--color-bg-subtle)] border border-[var(--color-border)]">
          <p class="text-[11px] font-mono text-[var(--color-text-secondary)]">{{ a.model }}</p>
        </div>

        <!-- Stats -->
        <div class="space-y-2.5 text-xs">
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-1.5 text-[var(--color-text-muted)]"><Zap :size="11" />Tokens used</span>
            <span class="font-mono font-medium text-[var(--color-text-secondary)]">{{ fmtTokens(a.tokensUsed) }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-1.5 text-[var(--color-text-muted)]"><CreditCard :size="11" />Cost</span>
            <span class="font-mono font-medium text-[var(--color-text-secondary)]">{{ fmtUsd(a.cost) }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-1.5 text-[var(--color-text-muted)]"><CheckCircle2 :size="11" />Tasks done</span>
            <span class="font-mono font-medium text-[var(--color-text-secondary)]">{{ a.tasksCompleted }}</span>
          </div>
          <div class="flex justify-between items-center">
            <span class="flex items-center gap-1.5 text-[var(--color-text-muted)]"><Clock :size="11" />Uptime</span>
            <span class="font-mono font-medium text-[var(--color-text-secondary)]">{{ a.uptime }}</span>
          </div>
        </div>

        <!-- Usage bar -->
        <div class="mt-4 pt-3 border-t border-[var(--color-border)]">
          <div class="flex justify-between text-[10px] text-[var(--color-text-muted)] mb-1.5">
            <span>Token utilization</span>
            <span>{{ ((a.tokensUsed / 2_500_000) * 100).toFixed(0) }}%</span>
          </div>
          <div class="h-1 rounded-full bg-[var(--color-border)] overflow-hidden">
            <div class="h-full rounded-full transition-all duration-700"
                 :style="{ width: ((a.tokensUsed / 2_500_000) * 100).toFixed(0) + '%', background: providerColor[a.provider] }" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.strip-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px; padding: 14px 18px;
  box-shadow: var(--shadow-sm);
}
.strip-label { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-text-muted); margin-bottom: 4px; }
.strip-value { font-size: 1.4rem; font-weight: 700; letter-spacing: -0.03em; color: var(--color-text); }

.model-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px; padding: 18px;
  box-shadow: var(--shadow-sm);
  transition: border-color 200ms, box-shadow 200ms;
}
.model-card:hover { border-color: var(--color-border-strong); box-shadow: var(--shadow-md); }

.status-pip {
  position: absolute; bottom: 0; right: 0;
  width: 10px; height: 10px; border-radius: 50%;
  border: 2px solid var(--color-bg-elevated);
}
</style>
