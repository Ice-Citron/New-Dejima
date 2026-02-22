<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Menu } from 'lucide-vue-next'

const route = useRoute()
const appStore = useAppStore()

const pageTitle = computed(() => {
  const name = route.name as string
  if (!name) return ''
  return name.charAt(0).toUpperCase() + name.slice(1)
})
</script>

<template>
  <header class="h-16 flex items-center justify-between px-6 border-b border-[var(--color-border)] bg-[var(--color-bg-elevated)] flex-shrink-0">
    <div class="flex items-center gap-3">
      <button
        class="md:hidden p-2 rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)] transition-colors"
        @click="appStore.toggleSidebar()"
        aria-label="Toggle sidebar"
      >
        <Menu :size="20" />
      </button>
      <h1 class="text-lg font-semibold tracking-tight text-[var(--color-text)]">{{ pageTitle }}</h1>
    </div>

    <div class="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
      <span class="w-1.5 h-1.5 rounded-full bg-emerald-500" />
      All systems operational
    </div>
  </header>
</template>
