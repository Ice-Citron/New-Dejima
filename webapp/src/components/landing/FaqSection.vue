<script setup lang="ts">
import AppAccordion from '@/components/ui/AppAccordion.vue'

const faqs = [
  {
    q: 'What kinds of apps can the agents build?',
    a: 'Currently, the agents specialize in Android apps — from utility tools to content-driven apps. They handle architecture, UI, backend integration, and packaging into APKs ready for the Play Store.',
  },
  {
    q: 'How does the economics tracking work?',
    a: 'Every API call, model inference, and cloud resource is metered in real-time. Revenue from app installations, ads, and subscriptions is tracked alongside costs, giving you a live profit/loss view per project.',
  },
  {
    q: 'What AI models are used?',
    a: 'The platform supports Claude Opus 4.6, Claude Sonnet 4.6, Gemini 2.5 Flash, and other models. Agents automatically select the most cost-effective model for each task.',
  },
  {
    q: 'Can I use my own API keys?',
    a: 'Yes. You can bring your own API keys for Anthropic, Google, ElevenLabs, and other providers. This gives you full control over costs and usage.',
  },
  {
    q: 'Is there a free tier?',
    a: 'Yes — the Starter plan includes 1 project, 1 agent, and 100K tokens per month at no cost. No credit card required.',
  },
  {
    q: 'How do agents deploy to the Play Store?',
    a: 'After quality gates pass (automated tests, build verification, code review), you can trigger a one-click deployment. The agent handles signing, bundling, and the Play Store submission process.',
  },
]
</script>

<template>
  <section id="faq" class="py-20 md:py-28 bg-[var(--color-bg-subtle)]">
    <div class="max-w-3xl mx-auto px-6">
      <div class="text-center mb-16">
        <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-[var(--color-text)]">
          Questions & answers
        </h2>
      </div>

      <div>
        <AppAccordion v-for="faq in faqs" :key="faq.q" :title="faq.q">
          {{ faq.a }}
        </AppAccordion>
      </div>
    </div>
  </section>
</template>
