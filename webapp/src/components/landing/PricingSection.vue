<script setup lang="ts">
import { useScrollReveal } from '@/composables/useScrollReveal'
import { Check } from 'lucide-vue-next'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'

const { el, isVisible } = useScrollReveal()

const tiers = [
  {
    name: 'Starter',
    price: '$0',
    period: '/month',
    description: 'Perfect for trying things out.',
    features: ['1 active project', '1 agent', 'Community support', '100K tokens/month'],
    cta: 'Start free',
    highlighted: false,
  },
  {
    name: 'Pro',
    price: '$49',
    period: '/month',
    description: 'For builders shipping real apps.',
    features: ['10 active projects', '4 agents', 'Priority support', '2M tokens/month', 'Custom domains', 'Analytics dashboard'],
    cta: 'Get started',
    highlighted: true,
  },
  {
    name: 'Enterprise',
    price: 'Custom',
    period: '',
    description: 'For teams at scale.',
    features: ['Unlimited projects', 'Unlimited agents', 'Dedicated support', 'Unlimited tokens', 'SSO & SAML', 'Custom SLAs', 'On-prem option'],
    cta: 'Contact sales',
    highlighted: false,
  },
]
</script>

<template>
  <section id="pricing" class="py-20 md:py-28" ref="el">
    <div class="max-w-6xl mx-auto px-6">
      <div class="text-center max-w-2xl mx-auto mb-16">
        <h2 class="text-3xl md:text-4xl font-bold tracking-tight text-[var(--color-text)]">
          Simple pricing
        </h2>
        <p class="mt-4 text-[var(--color-text-secondary)]">
          Start free. Scale when you're ready.
        </p>
      </div>

      <div class="grid md:grid-cols-3 gap-6 max-w-5xl mx-auto">
        <AppCard
          v-for="(tier, i) in tiers"
          :key="tier.name"
          :class="[
            'flex flex-col transition-all duration-500 ease-out',
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-6',
            tier.highlighted ? 'ring-2 ring-[var(--color-accent)] relative' : '',
          ]"
          :style="{ transitionDelay: `${i * 100}ms` }"
        >
          <div v-if="tier.highlighted" class="absolute -top-3 left-1/2 -translate-x-1/2">
            <span class="px-3 py-1 text-xs font-medium bg-[var(--color-accent)] text-white rounded-full">Most popular</span>
          </div>

          <div class="mb-6">
            <h3 class="text-lg font-semibold text-[var(--color-text)]">{{ tier.name }}</h3>
            <p class="mt-1 text-sm text-[var(--color-text-secondary)]">{{ tier.description }}</p>
          </div>

          <div class="mb-6">
            <span class="text-4xl font-bold tracking-tight text-[var(--color-text)]">{{ tier.price }}</span>
            <span v-if="tier.period" class="text-sm text-[var(--color-text-muted)]">{{ tier.period }}</span>
          </div>

          <ul class="space-y-3 mb-8 flex-1">
            <li v-for="f in tier.features" :key="f" class="flex items-start gap-2.5 text-sm text-[var(--color-text-secondary)]">
              <Check :size="16" class="flex-shrink-0 mt-0.5 text-[var(--color-accent)]" />
              {{ f }}
            </li>
          </ul>

          <AppButton :variant="tier.highlighted ? 'primary' : 'secondary'" class="w-full">
            {{ tier.cta }}
          </AppButton>
        </AppCard>
      </div>
    </div>
  </section>
</template>
