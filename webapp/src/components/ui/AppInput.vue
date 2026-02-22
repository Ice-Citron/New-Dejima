<script setup lang="ts">
interface Props {
  modelValue?: string
  placeholder?: string
  label?: string
  type?: string
}

withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '',
  label: '',
  type: 'text',
})

defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <div class="flex flex-col gap-1.5">
    <label v-if="label" class="text-sm font-medium text-[var(--color-text-secondary)]">{{ label }}</label>
    <input
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      class="w-full px-4 py-2.5 text-sm bg-[var(--color-bg)] border border-[var(--color-border)] rounded-xl
             text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]
             focus:outline-none focus:ring-2 focus:ring-[var(--color-accent)] focus:border-transparent
             transition-all duration-200"
    />
  </div>
</template>
