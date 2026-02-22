<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown } from 'lucide-vue-next'

defineProps<{ title: string }>()
const isOpen = ref(false)
</script>

<template>
  <div class="border-b border-[var(--color-border)]">
    <button
      class="w-full flex items-center justify-between py-5 text-left text-[var(--color-text)] hover:text-[var(--color-accent)] transition-colors duration-200"
      @click="isOpen = !isOpen"
      :aria-expanded="isOpen"
    >
      <span class="text-base font-medium pr-4">{{ title }}</span>
      <ChevronDown
        :size="18"
        class="flex-shrink-0 text-[var(--color-text-muted)] transition-transform duration-200"
        :class="{ 'rotate-180': isOpen }"
      />
    </button>
    <Transition name="accordion">
      <div v-show="isOpen" class="overflow-hidden">
        <div class="pb-5 text-[var(--color-text-secondary)] text-sm leading-relaxed">
          <slot />
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.accordion-enter-active,
.accordion-leave-active {
  transition: max-height 250ms ease, opacity 200ms ease;
  max-height: 200px;
}
.accordion-enter-from,
.accordion-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
