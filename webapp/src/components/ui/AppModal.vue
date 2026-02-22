<script setup lang="ts">
import { watch } from 'vue'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()

watch(() => props.open, (val) => {
  document.body.style.overflow = val ? 'hidden' : ''
})

function onBackdrop() {
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="onBackdrop"
      >
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="onBackdrop" />
        <div
          class="relative w-full max-w-lg bg-[var(--color-bg-elevated)] border border-[var(--color-border)] rounded-2xl shadow-2xl p-6"
        >
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 200ms ease;
}
.modal-enter-active > div:last-child,
.modal-leave-active > div:last-child {
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1), opacity 200ms ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from > div:last-child {
  transform: scale(0.96) translateY(8px);
  opacity: 0;
}
.modal-leave-to > div:last-child {
  transform: scale(0.98);
  opacity: 0;
}
</style>
