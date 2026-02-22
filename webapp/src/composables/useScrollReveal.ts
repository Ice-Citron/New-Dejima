import { ref, onMounted, onUnmounted } from 'vue'

export function useScrollReveal(threshold = 0.15) {
  const el = ref<HTMLElement | null>(null)
  const isVisible = ref(false)
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (!el.value) return
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReducedMotion) {
      isVisible.value = true
      return
    }
    observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          isVisible.value = true
          observer?.disconnect()
        }
      },
      { threshold }
    )
    observer.observe(el.value)
  })

  onUnmounted(() => observer?.disconnect())

  return { el, isVisible }
}
