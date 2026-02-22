<script setup lang="ts">
import LandingNav from '@/components/landing/LandingNav.vue'
import LandingFooter from '@/components/landing/LandingFooter.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col bg-[var(--color-bg)]">
    <LandingNav />
    <main class="flex-1">
      <router-view />
    </main>
    <LandingFooter />
  </div>
</template>
