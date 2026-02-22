<script setup lang="ts">
import { ref } from 'vue'
import { Menu, X } from 'lucide-vue-next'
import AppButton from '@/components/ui/AppButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'

const mobileOpen = ref(false)

const links = [
  { label: 'Product', href: '#product' },
  { label: 'How it works', href: '#how-it-works' },
  { label: 'Pricing', href: '#pricing' },
  { label: 'Docs', href: '#faq' },
]
</script>

<template>
  <header class="sticky top-0 z-40 bg-[var(--color-bg)]/80 backdrop-blur-xl border-b border-[var(--color-border)]">
    <nav class="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
      <!-- Logo -->
      <router-link to="/" class="text-lg font-semibold tracking-tight text-[var(--color-text)]">
        New Dejima
      </router-link>

      <!-- Desktop links -->
      <div class="hidden md:flex items-center gap-1">
        <a
          v-for="link in links"
          :key="link.label"
          :href="link.href"
          class="px-3.5 py-2 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text)] rounded-lg transition-colors duration-150"
        >
          {{ link.label }}
        </a>
      </div>

      <!-- Right side -->
      <div class="hidden md:flex items-center gap-3">
        <ThemeToggle />
        <router-link to="/app/dashboard">
          <AppButton variant="ghost" size="sm">Sign in</AppButton>
        </router-link>
        <router-link to="/app/dashboard">
          <AppButton size="sm">Get started</AppButton>
        </router-link>
      </div>

      <!-- Mobile toggle -->
      <button
        class="md:hidden p-2 text-[var(--color-text)]"
        @click="mobileOpen = !mobileOpen"
        :aria-label="mobileOpen ? 'Close menu' : 'Open menu'"
      >
        <X v-if="mobileOpen" :size="20" />
        <Menu v-else :size="20" />
      </button>
    </nav>

    <!-- Mobile menu -->
    <Transition name="mobile-menu">
      <div v-if="mobileOpen" class="md:hidden border-t border-[var(--color-border)] bg-[var(--color-bg)]">
        <div class="px-6 py-4 flex flex-col gap-1">
          <a
            v-for="link in links"
            :key="link.label"
            :href="link.href"
            class="px-3 py-2.5 text-sm text-[var(--color-text-secondary)] hover:text-[var(--color-text)] rounded-lg transition-colors"
            @click="mobileOpen = false"
          >
            {{ link.label }}
          </a>
          <hr class="my-2 border-[var(--color-border)]" />
          <div class="flex items-center gap-3 pt-1">
            <ThemeToggle />
            <router-link to="/app/dashboard" class="flex-1" @click="mobileOpen = false">
              <AppButton size="sm" class="w-full">Get started</AppButton>
            </router-link>
          </div>
        </div>
      </div>
    </Transition>
  </header>
</template>

<style scoped>
.mobile-menu-enter-active,
.mobile-menu-leave-active {
  transition: opacity 200ms ease, max-height 250ms ease;
  max-height: 400px;
  overflow: hidden;
}
.mobile-menu-enter-from,
.mobile-menu-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
