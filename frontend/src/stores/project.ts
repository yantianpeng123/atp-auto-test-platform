import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { Project, ProjectRole } from '@/api/types'
import { getMyRole } from '@/api/projectMember'

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
  /** 当前登录用户在 currentProject 中的角色；后端未实现前为 null */
  const currentProjectRole = ref<ProjectRole | null>(null)

  const hasProject = computed(() => Boolean(currentProject.value))

  /** 是否可管理当前项目（成员管理 / 项目设置）：全局 ADMIN 或项目 OWNER/MAINTAINER */
  const canManageProject = computed(() => {
    const role = currentProjectRole.value
    return role === 'OWNER' || role === 'MAINTAINER'
  })

  function setCurrentProject(project: Project | null) {
    currentProject.value = project
    if (project) {
      localStorage.setItem(PROJECT_KEY, JSON.stringify(project))
      // 切项目后重新拉取当前用户在该项目的角色
      void loadMyRole(project.id)
    } else {
      localStorage.removeItem(PROJECT_KEY)
      currentProjectRole.value = null
    }
  }

  /** 拉取当前用户在指定项目中的角色；后端未实现/无权限时回退为 null（菜单按角色隐藏，ADMIN 仍可见） */
  async function loadMyRole(projectId?: number): Promise<ProjectRole | null> {
    const pid = projectId ?? currentProject.value?.id
    if (!pid) {
      currentProjectRole.value = null
      return null
    }
    try {
      currentProjectRole.value = await getMyRole(pid)
    } catch {
      currentProjectRole.value = null
    }
    return currentProjectRole.value
  }

  return {
    currentProject,
    currentProjectRole,
    hasProject,
    canManageProject,
    setCurrentProject,
    loadMyRole
  }
})
