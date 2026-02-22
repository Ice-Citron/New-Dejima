<script setup lang="ts">
import AppSidebar from '@/components/app/AppSidebar.vue'
import AppTopbar from '@/components/app/AppTopbar.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<template>
  <div class="h-screen flex overflow-hidden bg-[var(--color-bg)]">
    <!-- Sidebar -->
    <AppSidebar />

    <!-- Mobile overlay -->
    <Transition name="fade">
      <div
        v-if="appStore.sidebarOpen"
        class="fixed inset-0 z-30 bg-black/40 md:hidden"
        @click="appStore.toggleSidebar()"
      />
    </Transition>

    <!-- Main area -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <AppTopbar />
      <main class="flex-1 overflow-y-auto p-5 md:p-7">
        <router-view v-slot="{ Component, route }">
          <transition name="page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 200ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
