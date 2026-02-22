import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getProjects, type Project } from '@/services/mock-data'

export const useProjectsStore = defineStore('projects', () => {
  const projects = ref<Project[]>(getProjects())
  const search = ref('')
  const statusFilter = ref<string>('all')

  const filtered = computed(() => {
    let list = projects.value
    if (search.value) {
      const q = search.value.toLowerCase()
      list = list.filter(p => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q))
    }
    if (statusFilter.value !== 'all') {
      list = list.filter(p => p.status === statusFilter.value)
    }
    return list
  })

  function addProject(name: string, description: string) {
    projects.value.unshift({
      id: 'proj_' + Date.now(),
      name,
      description,
      type: 'android',
      status: 'building',
      model: 'claude-sonnet-4',
      cost: 0,
      revenue: 0,
      builds: 0,
      createdAt: new Date().toISOString(),
    })
  }

  return { projects, search, statusFilter, filtered, addProject }
})
