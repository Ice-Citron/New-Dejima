<script setup lang="ts">
import { computed } from 'vue'
import { getChartData } from '@/services/mock-data'

const data = getChartData()
const maxVal = computed(() => Math.max(...data.map(d => Math.max(d.cost, d.revenue))) * 1.2)

function yPercent(val: number) {
  return (1 - val / maxVal.value) * 100
}
</script>

<template>
  <div class="bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl p-6">
    <div class="flex items-center justify-between mb-6">
      <h3 class="text-sm font-semibold text-[var(--color-text)]">Cost vs Revenue</h3>
      <div class="flex items-center gap-4 text-xs text-[var(--color-text-muted)]">
        <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-red-400" />Cost</span>
        <span class="flex items-center gap-1.5"><span class="w-2 h-2 rounded-full bg-emerald-400" />Revenue</span>
      </div>
    </div>

    <div class="relative h-48">
      <svg class="w-full h-full" viewBox="0 0 700 200" preserveAspectRatio="none">
        <!-- Grid lines -->
        <line v-for="i in 4" :key="i" :x1="0" :y1="i * 50" :x2="700" :y2="i * 50" stroke="var(--color-border)" stroke-dasharray="4,4" />

        <!-- Cost area -->
        <polygon
          :points="`0,200 ${data.map((d, i) => `${i * 100 + 50},${yPercent(d.cost) * 2}`).join(' ')} 700,200`"
          fill="rgba(239,68,68,0.08)"
        />
        <!-- Cost line -->
        <polyline
          :points="data.map((d, i) => `${i * 100 + 50},${yPercent(d.cost) * 2}`).join(' ')"
          fill="none" stroke="#f87171" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
        />

        <!-- Revenue area -->
        <polygon
          :points="`0,200 ${data.map((d, i) => `${i * 100 + 50},${yPercent(d.revenue) * 2}`).join(' ')} 700,200`"
          fill="rgba(34,197,94,0.08)"
        />
        <!-- Revenue line -->
        <polyline
          :points="data.map((d, i) => `${i * 100 + 50},${yPercent(d.revenue) * 2}`).join(' ')"
          fill="none" stroke="#22c55e" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
        />

        <!-- Dots -->
        <circle
          v-for="(d, i) in data" :key="'c'+i"
          :cx="i * 100 + 50" :cy="yPercent(d.cost) * 2" r="3"
          fill="var(--color-bg-elevated)" stroke="#f87171" stroke-width="2"
        />
        <circle
          v-for="(d, i) in data" :key="'r'+i"
          :cx="i * 100 + 50" :cy="yPercent(d.revenue) * 2" r="3"
          fill="var(--color-bg-elevated)" stroke="#22c55e" stroke-width="2"
        />
      </svg>

      <!-- X-axis labels -->
      <div class="flex justify-between mt-2 px-2">
        <span v-for="d in data" :key="d.label" class="text-[10px] text-[var(--color-text-muted)]">{{ d.label }}</span>
      </div>
    </div>
  </div>
</template>
