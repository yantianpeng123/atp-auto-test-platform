import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Project } from '@/api/types'

const PROJECT_KEY = 'atp_current_project'

function readProject(): Project | null {
  try {
    const raw = localStorage.getItem(PROJECT_KEY)
    return raw ? (JSON.parse(raw) as Project) : null
  } catch {
    return null
  }
}

export const useProjectStore = defineStore('project', () => {
  const currentProject = ref<Project | null>(readProject())

  const hasProject = computed(() => Boolean(currentProject.value))

  function setCurrentProject(project: Project | null) {
    currentProject.value = project
    if (project) {
      localStorage.setItem(PROJECT_KEY, JSON.stringify(project))
    } else {
      localStorage.removeItem(PROJECT_KEY)
    }
  }

  return { currentProject, hasProject, setCurrentProject }
})
