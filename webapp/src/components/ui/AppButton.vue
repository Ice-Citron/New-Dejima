<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
}

withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
})
</script>

<template>
  <button
    :disabled="disabled || loading"
    :class="[
      'inline-flex items-center justify-center font-medium transition-all duration-200 rounded-xl',
      'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-accent)]',
      'disabled:opacity-50 disabled:cursor-not-allowed',
      'hover:-translate-y-[1px] hover:shadow-md active:translate-y-0 active:shadow-sm',
      {
        'bg-[var(--color-accent)] text-white hover:bg-[var(--color-accent-hover)]': variant === 'primary',
        'bg-[var(--color-bg-elevated)] text-[var(--color-text)] border border-[var(--color-border)] hover:border-[var(--color-border-strong)]': variant === 'secondary',
        'bg-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text)] hover:bg-[var(--color-bg-subtle)]': variant === 'ghost',
        'bg-[var(--color-danger)] text-white hover:bg-red-700': variant === 'danger',
        'px-3 py-1.5 text-sm gap-1.5': size === 'sm',
        'px-5 py-2.5 text-sm gap-2': size === 'md',
        'px-7 py-3 text-base gap-2.5': size === 'lg',
      }
    ]"
  >
    <svg v-if="loading" class="animate-spin -ml-1 mr-2 h-4 w-4" viewBox="0 0 24 24" fill="none">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
    <slot />
  </button>
</template>
