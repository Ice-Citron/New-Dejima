<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppButton from '@/components/ui/AppButton.vue'
import { ArrowRight } from 'lucide-vue-next'

const visible = ref(false)
onMounted(() => { setTimeout(() => visible.value = true, 80) })

const floatingCards = [
  { label: 'Build',      delay: '0ms',   barWidth: '72%',  barColor: '#4f8ef7' },
  { label: 'Track cost', delay: '120ms', barWidth: '58%',  barColor: '#4ade80' },
  { label: 'Deploy',     delay: '240ms', barWidth: '85%',  barColor: '#a78bfa' },
]
</script>

<template>
  <section class="relative overflow-hidden">
    <!-- Background gradient -->
    <div class="absolute inset-0 pointer-events-none">
      <div class="absolute top-[-10%] left-[10%] w-[500px] h-[500px] rounded-full opacity-[0.07] blur-[80px]"
           style="background: radial-gradient(circle, #4f8ef7 0%, transparent 70%)" />
      <div class="absolute top-[20%] right-[5%] w-[300px] h-[300px] rounded-full opacity-[0.05] blur-[60px]"
           style="background: radial-gradient(circle, #a78bfa 0%, transparent 70%)" />
    </div>

    <div class="max-w-6xl mx-auto px-6 pt-28 pb-24 md:pt-36 md:pb-32">
      <div class="grid md:grid-cols-2 gap-12 lg:gap-20 items-center">
        <!-- Left copy -->
        <div
          :class="['transition-all duration-700 ease-out', visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-5']"
        >
          <p class="text-xs font-semibold uppercase tracking-widest text-[var(--color-accent)] mb-4">AI-powered platform</p>
          <h1 class="text-4xl md:text-5xl lg:text-[3.25rem] font-bold tracking-tight leading-[1.08] text-[var(--color-text)]">
            Autonomous app creation,<br>
            <span class="text-[var(--color-accent)]">tracked economics.</span>
          </h1>
          <p class="mt-5 text-[1.05rem] text-[var(--color-text-secondary)] leading-relaxed max-w-md">
            AI agents that build, deploy, and market Android apps — while you track every dollar in and out.
          </p>
          <div class="mt-8 flex flex-wrap gap-3">
            <router-link to="/app/dashboard">
              <AppButton size="lg">
                Get started
                <ArrowRight :size="16" />
              </AppButton>
            </router-link>
            <a href="#faq">
              <AppButton variant="secondary" size="lg">View docs</AppButton>
            </a>
          </div>
          <p class="mt-5 text-xs text-[var(--color-text-muted)]">
            No credit card required. Free tier available.
          </p>
        </div>

        <!-- Right visual -->
        <div class="relative flex items-center justify-center py-8">
          <div class="relative flex flex-col gap-3 w-full max-w-[280px] mx-auto">
            <div
              v-for="(card, i) in floatingCards"
              :key="card.label"
              :class="[
                'px-5 py-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-bg-elevated)]',
                'transition-all duration-700 ease-out',
                visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8',
              ]"
              :style="{
                transitionDelay: `${250 + i * 100}ms`,
                marginLeft: `${i * 20}px`,
                boxShadow: 'var(--shadow-md)',
              }"
            >
              <div class="flex items-center justify-between mb-3">
                <span class="text-sm font-semibold text-[var(--color-text)]">{{ card.label }}</span>
                <span class="text-xs font-mono text-[var(--color-text-muted)]">{{ card.barWidth }}</span>
              </div>
              <div class="h-1.5 rounded-full bg-[var(--color-border)] overflow-hidden">
                <div
                  class="h-full rounded-full transition-all duration-1000 ease-out"
                  :style="{
                    width: visible ? card.barWidth : '0%',
                    background: card.barColor,
                    transitionDelay: `${500 + i * 120}ms`,
                  }"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
