<script setup lang="ts">
import { useScrollReveal } from '@/composables/useScrollReveal'

const { el, isVisible } = useScrollReveal()

const steps = [
  { number: '01', title: 'Describe your app', description: 'Write a short brief — target audience, core feature, monetization strategy.' },
  { number: '02', title: 'Agent builds it', description: 'An AI agent writes code, generates assets, runs tests, and produces an APK.' },
  { number: '03', title: 'Review & deploy', description: 'Quality gates verify the build. One click pushes it to the Play Store.' },
  { number: '04', title: 'Track economics', description: 'Monitor costs, revenue, and profit in real-time. The system pays for itself.' },
]
</script>

<template>
  <section id="how-it-works" class="py-20 md:py-28 bg-[var(--color-bg-subtle)]" ref="el">
    <div class="max-w-6xl mx-auto px-6">
      <div class="text-center max-w-2xl mx-auto mb-16">
        <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-[var(--color-text)]">
          How it works
        </h2>
        <p class="mt-4 text-[var(--color-text-secondary)]">
          Four steps from idea to revenue-generating app.
        </p>
      </div>

      <div class="relative max-w-3xl mx-auto">
        <!-- Timeline line -->
        <div class="absolute left-6 md:left-8 top-0 bottom-0 w-px bg-[var(--color-border)]" />

        <div class="space-y-12">
          <div
            v-for="(step, i) in steps"
            :key="step.number"
            :class="[
              'relative flex gap-6 md:gap-8 transition-all duration-500 ease-out',
              isVisible ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-4',
            ]"
            :style="{ transitionDelay: `${i * 120}ms` }"
          >
            <!-- Number dot -->
            <div class="relative z-10 flex-shrink-0 w-12 h-12 md:w-16 md:h-16 rounded-2xl bg-[var(--color-bg-elevated)] border border-[var(--color-border)] flex items-center justify-center">
              <span class="text-sm md:text-base font-bold text-[var(--color-accent)]">{{ step.number }}</span>
            </div>

            <div class="pt-2 md:pt-4">
              <h3 class="text-lg font-semibold text-[var(--color-text)]">{{ step.title }}</h3>
              <p class="mt-1 text-sm text-[var(--color-text-secondary)] leading-relaxed">{{ step.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
