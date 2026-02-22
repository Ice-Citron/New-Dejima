<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { Sun, Moon } from 'lucide-vue-next'

const appStore = useAppStore()
</script>

<template>
  <button
    @click="appStore.toggleTheme()"
    class="relative p-2 rounded-xl text-[var(--color-text-secondary)] hover:text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)] transition-all duration-200"
    :aria-label="appStore.theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'"
  >
    <Transition name="theme-icon" mode="out-in">
      <Sun v-if="appStore.theme === 'dark'" :size="18" key="sun" />
      <Moon v-else :size="18" key="moon" />
    </Transition>
  </button>
</template>

<style scoped>
.theme-icon-enter-active,
.theme-icon-leave-active {
  transition: transform 200ms ease, opacity 150ms ease;
}
.theme-icon-enter-from {
  transform: rotate(-30deg) scale(0.8);
  opacity: 0;
}
.theme-icon-leave-to {
  transform: rotate(30deg) scale(0.8);
  opacity: 0;
}
</style>
