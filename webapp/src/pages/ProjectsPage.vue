<script setup lang="ts">
import { ref, computed } from 'vue'
import { useProjectsStore } from '@/stores/projects'
import { getProjects } from '@/services/mock-data'
import AppButton from '@/components/ui/AppButton.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import CreateProjectModal from '@/components/app/CreateProjectModal.vue'
import { Plus, Search, Smartphone, Globe } from 'lucide-vue-next'

const store = useProjectsStore()
const showModal = ref(false)
const projects = getProjects()

const typeFilter = ref<'all' | 'android' | 'webapp'>('all')

const statusVariants: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'default'> = {
  live: 'success', building: 'info', paused: 'warning', failed: 'danger',
}
const statuses = ['all', 'live', 'building', 'paused', 'failed']

// Use local filtering since store may not have all new fields
const filtered = computed(() => {
  let list = projects
  if (store.statusFilter && store.statusFilter !== 'all') {
    list = list.filter(p => p.status === store.statusFilter)
  }
  if (typeFilter.value !== 'all') {
    list = list.filter(p => p.type === typeFilter.value)
  }
  if (store.search) {
    const q = store.search.toLowerCase()
    list = list.filter(p => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q))
  }
  return list
})

function fmtUsd(v: number) { return '$' + v.toFixed(2) }
function fmtDate(d: string) { return new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) }
function roi(cost: number, revenue: number) {
  if (!cost || !revenue) return null
  return Math.round((revenue / cost) * 100) + '%'
}
</script>

<template>
  <div class="space-y-5">
    <!-- Toolbar -->
    <div class="flex flex-col sm:flex-row gap-3 sm:items-center sm:justify-between">
      <div class="flex flex-wrap items-center gap-2">
        <!-- Search -->
        <div class="relative">
          <Search :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)] pointer-events-none" />
          <input
            v-model="store.search"
            placeholder="Search projects…"
            class="w-52 pl-9 pr-4 py-2 text-sm bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-xl
                   text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]
                   focus:outline-none focus:ring-2 focus:ring-[var(--color-accent)]/30 focus:border-[var(--color-accent)] transition-all"
          />
        </div>

        <!-- Status filter pill group -->
        <div class="flex items-center p-1 bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-xl gap-0.5">
          <button
            v-for="s in statuses" :key="s" @click="store.statusFilter = s"
            :class="[
              'px-3 py-1.5 text-xs font-medium rounded-lg capitalize transition-all duration-150',
              store.statusFilter === s
                ? 'bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
                : 'text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)]',
            ]"
          >{{ s }}</button>
        </div>

        <!-- Type toggle -->
        <div class="flex items-center p-1 bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-xl gap-0.5">
          <button @click="typeFilter = 'all'"
                  :class="['px-3 py-1.5 text-xs font-medium rounded-lg transition-all duration-150',
                    typeFilter === 'all' ? 'bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
                    : 'text-[var(--color-text-muted)] hover:bg-[var(--color-bg-subtle)]']">All</button>
          <button @click="typeFilter = 'android'"
                  :class="['flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg transition-all duration-150',
                    typeFilter === 'android' ? 'bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
                    : 'text-[var(--color-text-muted)] hover:bg-[var(--color-bg-subtle)]']">
            <Smartphone :size="11" />Android
          </button>
          <button @click="typeFilter = 'webapp'"
                  :class="['flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg transition-all duration-150',
                    typeFilter === 'webapp' ? 'bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
                    : 'text-[var(--color-text-muted)] hover:bg-[var(--color-bg-subtle)]']">
            <Globe :size="11" />Web App
          </button>
        </div>
      </div>

      <AppButton size="sm" @click="showModal = true" class="self-start sm:self-auto flex-shrink-0">
        <Plus :size="14" /> Create project
      </AppButton>
    </div>

    <!-- Table -->
    <div class="bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-[var(--color-border)]">
            <th class="th">Project</th>
            <th class="th hidden sm:table-cell">Type</th>
            <th class="th hidden sm:table-cell">Status</th>
            <th class="th hidden md:table-cell">AI Model</th>
            <th class="th text-right hidden lg:table-cell">Cost</th>
            <th class="th text-right hidden lg:table-cell">Revenue</th>
            <th class="th text-right hidden lg:table-cell">ROI</th>
            <th class="th text-right hidden md:table-cell">Created</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-[var(--color-border)]">
          <tr v-for="p in filtered" :key="p.id"
              class="hover:bg-[var(--color-bg-subtle)] transition-colors duration-100">
            <td class="px-5 py-4">
              <p class="font-semibold text-[var(--color-text)]">{{ p.name }}</p>
              <p class="text-xs text-[var(--color-text-muted)] mt-0.5 truncate max-w-xs">{{ p.description }}</p>
            </td>
            <td class="px-5 py-4 hidden sm:table-cell">
              <span class="flex items-center gap-1.5 text-xs text-[var(--color-text-secondary)]">
                <component :is="p.type === 'android' ? Smartphone : Globe" :size="13" class="text-[var(--color-text-muted)]" />
                {{ p.type === 'android' ? 'Android' : 'Web App' }}
              </span>
            </td>
            <td class="px-5 py-4 hidden sm:table-cell">
              <AppBadge :variant="statusVariants[p.status] || 'default'">{{ p.status }}</AppBadge>
            </td>
            <td class="px-5 py-4 text-[var(--color-text-secondary)] hidden md:table-cell font-mono text-xs">{{ p.model }}</td>
            <td class="px-5 py-4 text-right text-[var(--color-text-secondary)] hidden lg:table-cell font-mono text-xs">{{ fmtUsd(p.cost) }}</td>
            <td class="px-5 py-4 text-right hidden lg:table-cell font-mono text-xs"
                :style="{ color: p.revenue > 0 ? 'var(--color-success)' : 'var(--color-text-muted)' }">
              {{ p.revenue > 0 ? fmtUsd(p.revenue) : '—' }}
            </td>
            <td class="px-5 py-4 text-right hidden lg:table-cell font-mono text-xs text-[var(--color-text-muted)]">
              {{ roi(p.cost, p.revenue) ?? '—' }}
            </td>
            <td class="px-5 py-4 text-right text-[var(--color-text-muted)] text-xs hidden md:table-cell">{{ fmtDate(p.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="!filtered.length" class="px-5 py-12 text-center text-sm text-[var(--color-text-muted)]">
        No projects match your filters.
      </div>
    </div>

    <CreateProjectModal :open="showModal" @close="showModal = false" />
  </div>
</template>

<style scoped>
.th {
  text-align: left;
  padding: 10px 20px;
  font-size: 10px; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.08em;
  color: var(--color-text-muted);
}
</style>
