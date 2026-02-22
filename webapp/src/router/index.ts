import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        { path: '', name: 'landing', component: () => import('@/pages/LandingPage.vue') },
      ],
    },
    {
      path: '/app',
      component: () => import('@/layouts/AppLayout.vue'),
      children: [
        { path: '', redirect: '/app/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: () => import('@/pages/DashboardPage.vue') },
        { path: 'projects', name: 'projects', component: () => import('@/pages/ProjectsPage.vue') },
        { path: 'agents', name: 'agents', component: () => import('@/pages/AgentsPage.vue') },
        { path: 'billing', name: 'billing', component: () => import('@/pages/BillingPage.vue') },
        { path: 'settings', name: 'settings', component: () => import('@/pages/SettingsPage.vue') },
      ],
    },
  ],
})

export default router
