<script setup lang="ts">
import { useScrollReveal } from '@/composables/useScrollReveal'
import { Bot, BarChart3, Rocket, Shield, Zap } from 'lucide-vue-next'
import AppCard from '@/components/ui/AppCard.vue'

const { el, isVisible } = useScrollReveal()

const features = [
  { icon: Bot, title: 'Autonomous Agents', description: 'AI agents build complete Android apps from a single prompt — architecture, UI, backend, tests.' },
  { icon: BarChart3, title: 'Real-time Economics', description: 'Track every API call, every dollar spent, every dollar earned. Full cost transparency.' },
  { icon: Rocket, title: 'One-click Deploy', description: 'Automated Play Store submissions. From code to live app in minutes, not weeks.' },
  { icon: Shield, title: 'Quality Gates', description: 'Automated testing, code review, and build verification before any deployment.' },
  { icon: Zap, title: 'Self-sustaining', description: 'Revenue from apps pays for the AI that builds them. The system funds itself.' },
]
</script>

<template>
  <section id="product" class="py-20 md:py-28" ref="el">
    <div class="max-w-6xl mx-auto px-6">
      <div class="text-center max-w-2xl mx-auto mb-16">
        <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-[var(--color-text)]">
          Everything you need to ship
        </h2>
        <p class="mt-4 text-[var(--color-text-secondary)]">
          A complete platform for autonomous app creation with full economic visibility.
        </p>
      </div>

      <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <AppCard
          v-for="(f, i) in features"
          :key="f.title"
          hover
          :class="[
            'transition-all duration-500 ease-out',
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6',
          ]"
          :style="{ transitionDelay: `${i * 80}ms` }"
        >
          <div class="w-10 h-10 rounded-xl bg-[var(--color-accent-subtle)] flex items-center justify-center mb-4">
            <component :is="f.icon" :size="20" class="text-[var(--color-accent)]" />
          </div>
          <h3 class="text-base font-semibold text-[var(--color-text)] mb-2">{{ f.title }}</h3>
          <p class="text-sm text-[var(--color-text-secondary)] leading-relaxed">{{ f.description }}</p>
        </AppCard>
      </div>
    </div>
  </section>
</template>
