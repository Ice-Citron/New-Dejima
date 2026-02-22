import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const theme = ref<'light' | 'dark'>(
    (typeof localStorage !== 'undefined' && localStorage.getItem('nd-theme') as 'light' | 'dark') || 'dark'
  )
  const sidebarOpen = ref(true)

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    localStorage.setItem('nd-theme', theme.value)
  }

  function toggleSidebar() {
    sidebarOpen.value = !sidebarOpen.value
  }

  return { theme, sidebarOpen, toggleTheme, toggleSidebar }
})
