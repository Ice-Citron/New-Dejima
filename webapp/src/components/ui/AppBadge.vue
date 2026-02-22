<script setup lang="ts">
interface Props {
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info'
}

withDefaults(defineProps<Props>(), {
  variant: 'default',
})
</script>

<template>
  <span
    :class="[
      'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
      {
        'bg-[var(--color-bg-subtle)] text-[var(--color-text-secondary)]': variant === 'default',
        'bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400': variant === 'success',
        'bg-amber-50 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400': variant === 'warning',
        'bg-red-50 text-red-700 dark:bg-red-500/10 dark:text-red-400': variant === 'danger',
        'bg-blue-50 text-blue-700 dark:bg-blue-500/10 dark:text-blue-400': variant === 'info',
      }
    ]"
  >
    <slot />
  </span>
</template>
