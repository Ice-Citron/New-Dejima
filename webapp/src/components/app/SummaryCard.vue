<script setup lang="ts">
import type { Component } from 'vue'
import { TrendingUp, TrendingDown } from 'lucide-vue-next'

interface Props {
  label: string
  value: string
  trend?: number
  icon?: Component
  iconCss?: string
}

defineProps<Props>()
</script>

<template>
  <div class="bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl p-5 transition-all duration-200 hover:border-[var(--color-border-strong)]" style="box-shadow: var(--shadow-sm)">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">{{ label }}</p>
        <p class="mt-2 text-2xl font-bold tracking-tight text-[var(--color-text)]">{{ value }}</p>
      </div>
      <div
        v-if="icon"
        :class="['w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0', iconCss || 'icon-stat-blue']"
      >
        <component :is="icon" :size="20" />
      </div>
    </div>
    <div v-if="trend !== undefined" class="mt-3 flex items-center gap-1.5 text-xs font-medium">
      <component :is="trend >= 0 ? TrendingUp : TrendingDown" :size="13" :class="trend >= 0 ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'" />
      <span :class="trend >= 0 ? 'text-[var(--color-success)]' : 'text-[var(--color-danger)]'">
        {{ trend >= 0 ? '+' : '' }}{{ trend }}%
      </span>
      <span class="text-[var(--color-text-muted)]">vs last week</span>
    </div>
  </div>
</template>
