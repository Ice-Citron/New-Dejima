<script setup lang="ts">
import { ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppButton from '@/components/ui/AppButton.vue'
import { useProjectsStore } from '@/stores/projects'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()

const store = useProjectsStore()
const name = ref('')
const description = ref('')

function submit() {
  if (!name.value.trim()) return
  store.addProject(name.value.trim(), description.value.trim())
  name.value = ''
  description.value = ''
  emit('close')
}
</script>

<template>
  <AppModal :open="open" @close="emit('close')">
    <h2 class="text-lg font-semibold text-[var(--color-text)] mb-1">Create project</h2>
    <p class="text-sm text-[var(--color-text-muted)] mb-6">Describe the app you want to build.</p>

    <form @submit.prevent="submit" class="space-y-4">
      <AppInput v-model="name" label="Project name" placeholder="e.g., Golf Deals" />
      <div class="flex flex-col gap-1.5">
        <label class="text-sm font-medium text-[var(--color-text-secondary)]">Description</label>
        <textarea
          v-model="description"
          placeholder="AI-powered golf equipment price comparison app..."
          rows="3"
          class="w-full px-4 py-2.5 text-sm bg-[var(--color-bg)] border border-[var(--color-border)] rounded-xl
                 text-[var(--color-text)] placeholder:text-[var(--color-text-muted)]
                 focus:outline-none focus:ring-2 focus:ring-[var(--color-accent)] focus:border-transparent
                 transition-all duration-200 resize-none"
        />
      </div>
      <div class="flex justify-end gap-3 pt-2">
        <AppButton variant="ghost" type="button" @click="emit('close')">Cancel</AppButton>
        <AppButton type="submit" :disabled="!name.trim()">Create project</AppButton>
      </div>
    </form>
  </AppModal>
</template>
