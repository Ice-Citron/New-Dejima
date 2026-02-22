<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { LayoutDashboard, FolderKanban, Bot, CreditCard, Settings, LogOut } from 'lucide-vue-next'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'

const route = useRoute()
const appStore = useAppStore()

const navItems = [
  { to: '/app/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/app/projects',  label: 'Projects',  icon: FolderKanban },
  { to: '/app/agents',    label: 'Agents',     icon: Bot },
  { to: '/app/billing',   label: 'Billing',    icon: CreditCard },
  { to: '/app/settings',  label: 'Settings',   icon: Settings },
]

function isActive(path: string) {
  return route.path === path
}

function onNavClick() {
  if (window.innerWidth < 768) {
    appStore.sidebarOpen = false
  }
}
</script>

<template>
  <aside
    :class="[
      'fixed md:relative z-40 h-full flex flex-col',
      'w-[var(--sidebar-width)] border-r border-[var(--color-border)]',
      'bg-[var(--color-bg-elevated)]',
      'transition-transform duration-250 ease-[cubic-bezier(0.16,1,0.3,1)]',
      'md:translate-x-0',
      appStore.sidebarOpen ? 'translate-x-0' : '-translate-x-full',
    ]"
  >
    <!-- Brand -->
    <div class="h-16 flex items-center px-5 border-b border-[var(--color-border)] flex-shrink-0">
      <router-link to="/" class="flex items-center gap-2.5">
        <div class="w-8 h-8 rounded-lg bg-[var(--color-accent)] flex items-center justify-center flex-shrink-0">
          <span class="text-white text-sm font-bold">N</span>
        </div>
        <span class="text-[0.95rem] font-semibold tracking-tight text-[var(--color-text)]">New Dejima</span>
      </router-link>
    </div>

    <!-- Nav -->
    <nav class="flex-1 py-4 px-3 space-y-0.5 overflow-y-auto">
      <router-link
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        :class="[
          'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150',
          isActive(item.to)
            ? 'bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
            : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)]',
        ]"
        @click="onNavClick()"
      >
        <component :is="item.icon" :size="17" :stroke-width="isActive(item.to) ? 2.5 : 1.8" />
        {{ item.label }}
      </router-link>
    </nav>

    <!-- Bottom -->
    <div class="px-3 pb-4 border-t border-[var(--color-border)] pt-4 space-y-1 flex-shrink-0">
      <div class="flex items-center justify-between px-2 mb-1">
        <span class="text-xs text-[var(--color-text-muted)] font-medium">Theme</span>
        <ThemeToggle />
      </div>
      <div class="flex items-center gap-3 px-3 py-2.5 rounded-xl">
        <div class="w-8 h-8 rounded-full bg-[var(--color-accent-subtle)] border border-[var(--color-border)] flex items-center justify-center flex-shrink-0">
          <span class="text-xs font-bold text-[var(--color-accent)]">YA</span>
        </div>
        <div class="flex-1 min-w-0">
          <p class="text-sm font-semibold text-[var(--color-text)] truncate">Yacine A.</p>
          <p class="text-xs text-[var(--color-text-muted)] truncate">Admin</p>
        </div>
        <router-link to="/" class="text-[var(--color-text-muted)] hover:text-[var(--color-text)] transition-colors">
          <LogOut :size="15" />
        </router-link>
      </div>
    </div>
  </aside>
</template>
