<script setup lang="ts">
import { getActivities } from '@/services/mock-data'
import { Rocket, DollarSign, Hammer, Bot, CreditCard } from 'lucide-vue-next'
import type { Component } from 'vue'

const activities = getActivities()

const typeConfig: Record<string, { icon: Component; css: string }> = {
  build:   { icon: Hammer,      css: 'icon-blue'    },
  deploy:  { icon: Rocket,      css: 'icon-violet'  },
  revenue: { icon: DollarSign,  css: 'icon-emerald' },
  cost:    { icon: CreditCard,  css: 'icon-red'     },
  agent:   { icon: Bot,         css: 'icon-amber'   },
}

function timeAgo(ts: string) {
  const diff = Date.now() - new Date(ts).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  return `${Math.floor(hours / 24)}d ago`
}
</script>

<template>
  <div class="bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl overflow-hidden">
    <div class="px-5 py-4 border-b border-[var(--color-border)]">
      <h3 class="text-sm font-semibold text-[var(--color-text)]">Recent activity</h3>
    </div>
    <div class="divide-y divide-[var(--color-border)]">
      <div
        v-for="a in activities"
        :key="a.id"
        class="flex items-center gap-3.5 px-5 py-3.5 hover:bg-[var(--color-bg-subtle)] transition-colors duration-150"
      >
        <div :class="['w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0', typeConfig[a.type]?.css]">
          <component :is="typeConfig[a.type]?.icon" :size="14" />
        </div>
        <p class="flex-1 text-sm text-[var(--color-text-secondary)] truncate">{{ a.message }}</p>
        <span class="text-xs text-[var(--color-text-muted)] flex-shrink-0">{{ timeAgo(a.timestamp) }}</span>
      </div>
    </div>
  </div>
</template>
