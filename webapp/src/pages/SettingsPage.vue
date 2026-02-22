<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { Sun, Moon } from 'lucide-vue-next'

const appStore = useAppStore()
const name = ref('Yacine A.')
const email = ref('yacine@newdejima.com')
const saved = ref(false)

function save() {
  saved.value = true
  setTimeout(() => saved.value = false, 2000)
}
</script>

<template>
  <div class="space-y-6 max-w-2xl">
    <!-- Appearance -->
    <AppCard>
      <h3 class="text-sm font-semibold text-[var(--color-text)] mb-4">Appearance</h3>
      <div class="flex gap-3">
        <button
          @click="appStore.theme === 'dark' && appStore.toggleTheme()"
          :class="[
            'flex-1 flex items-center justify-center gap-2 py-3 rounded-xl border text-sm font-medium transition-all duration-200',
            appStore.theme === 'light'
              ? 'border-[var(--color-accent)] bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
              : 'border-[var(--color-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-border-strong)]',
          ]"
        >
          <Sun :size="16" />
          Light
        </button>
        <button
          @click="appStore.theme === 'light' && appStore.toggleTheme()"
          :class="[
            'flex-1 flex items-center justify-center gap-2 py-3 rounded-xl border text-sm font-medium transition-all duration-200',
            appStore.theme === 'dark'
              ? 'border-[var(--color-accent)] bg-[var(--color-accent-subtle)] text-[var(--color-accent)]'
              : 'border-[var(--color-border)] text-[var(--color-text-secondary)] hover:border-[var(--color-border-strong)]',
          ]"
        >
          <Moon :size="16" />
          Dark
        </button>
      </div>
    </AppCard>

    <!-- Profile -->
    <AppCard>
      <h3 class="text-sm font-semibold text-[var(--color-text)] mb-4">Profile</h3>
      <form @submit.prevent="save" class="space-y-4">
        <AppInput v-model="name" label="Name" placeholder="Your name" />
        <AppInput v-model="email" label="Email" type="email" placeholder="you@example.com" />
        <div class="flex items-center gap-3 pt-2">
          <AppButton type="submit">Save changes</AppButton>
          <Transition name="page">
            <span v-if="saved" class="text-sm text-emerald-600 dark:text-emerald-400">Saved!</span>
          </Transition>
        </div>
      </form>
    </AppCard>

    <!-- Danger zone -->
    <AppCard>
      <h3 class="text-sm font-semibold text-red-600 dark:text-red-400 mb-2">Danger zone</h3>
      <p class="text-sm text-[var(--color-text-muted)] mb-4">Permanently delete your account and all associated data.</p>
      <AppButton variant="danger" size="sm">Delete account</AppButton>
    </AppCard>
  </div>
</template>
