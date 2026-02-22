<script setup lang="ts">
import { getSummaryStats, getBilling } from '@/services/mock-data'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppCard from '@/components/ui/AppCard.vue'

const stats = getSummaryStats()
const billing = getBilling()

function fmtUsd(v: number) { return '$' + v.toFixed(2) }
</script>

<template>
  <div class="space-y-6 max-w-4xl">
    <!-- Overview -->
    <div class="grid sm:grid-cols-3 gap-4">
      <AppCard>
        <p class="text-xs font-medium uppercase tracking-wider text-[var(--color-text-muted)]">Total spend</p>
        <p class="mt-1 text-2xl font-bold text-[var(--color-text)]">{{ fmtUsd(stats.totalSpend) }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs font-medium uppercase tracking-wider text-[var(--color-text-muted)]">Total revenue</p>
        <p class="mt-1 text-2xl font-bold text-emerald-600 dark:text-emerald-400">{{ fmtUsd(stats.totalRevenue) }}</p>
      </AppCard>
      <AppCard>
        <p class="text-xs font-medium uppercase tracking-wider text-[var(--color-text-muted)]">Net profit</p>
        <p class="mt-1 text-2xl font-bold text-[var(--color-text)]">{{ fmtUsd(stats.profit) }}</p>
      </AppCard>
    </div>

    <!-- Billing history -->
    <div class="bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl overflow-hidden">
      <div class="px-5 py-4 border-b border-[var(--color-border)]">
        <h3 class="text-sm font-semibold text-[var(--color-text)]">Billing history</h3>
      </div>
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-[var(--color-border)]">
            <th class="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Date</th>
            <th class="text-left px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Description</th>
            <th class="text-right px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Amount</th>
            <th class="text-right px-5 py-3 text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Status</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[var(--color-border)]">
          <tr v-for="b in billing" :key="b.id" class="hover:bg-[var(--color-bg-subtle)] transition-colors duration-100">
            <td class="px-5 py-3.5 text-[var(--color-text-muted)] text-xs">{{ b.date }}</td>
            <td class="px-5 py-3.5 text-[var(--color-text)]">{{ b.description }}</td>
            <td class="px-5 py-3.5 text-right font-mono text-xs text-[var(--color-text)]">{{ fmtUsd(b.amount) }}</td>
            <td class="px-5 py-3.5 text-right">
              <AppBadge :variant="b.status === 'paid' ? 'success' : b.status === 'pending' ? 'warning' : 'danger'">{{ b.status }}</AppBadge>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
