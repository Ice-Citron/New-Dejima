<script setup lang="ts">
interface Props {
  hover?: boolean
  padding?: 'sm' | 'md' | 'lg' | 'none'
}

withDefaults(defineProps<Props>(), {
  hover: false,
  padding: 'md',
})
</script>

<template>
  <div
    :class="[
      'bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl transition-all duration-200',
      {
        'hover:border-[var(--color-border-strong)] hover:shadow-lg hover:-translate-y-[1px]': hover,
        'p-4': padding === 'sm',
        'p-6': padding === 'md',
        'p-8': padding === 'lg',
        '': padding === 'none',
      }
    ]"
  >
    <slot />
  </div>
</template>
