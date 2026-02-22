<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { watchEffect } from 'vue'

const appStore = useAppStore()

watchEffect(() => {
  document.documentElement.classList.toggle('dark', appStore.theme === 'dark')
})
</script>

<template>
  <router-view v-slot="{ Component, route }">
    <transition name="page" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>
