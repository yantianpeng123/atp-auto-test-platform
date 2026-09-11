import { defineStore } from 'pinia'
import { ref } from 'vue'
import router from '@/router'

export interface TabItem {
  path: string
  title: string
  icon?: string
  /** 是否可关闭（工作台不可关闭） */
  closable: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([
    { path: '/dashboard', title: '工作台', icon: 'Odometer', closable: false }
  ])
  const activeTab = ref('/dashboard')

  /** 路由切换时调用：确保标签存在并激活 */
  function addTab(path: string, title: string, icon?: string) {
    // 不给隐藏路由（如 case/edit）自动添加固定标签，但仍需激活
    const closable = path !== '/dashboard'
    const exist = tabs.value.find(t => t.path === path)
    if (!exist) {
      tabs.value.push({ path, title, icon, closable })
    }
    activeTab.value = path
  }

  /** 关闭标签，返回下一个应激活的路径 */
  function closeTab(path: string): string {
    const idx = tabs.value.findIndex(t => t.path === path)
    if (idx === -1) return activeTab.value

    tabs.value.splice(idx, 1)

    // 如果关闭的是当前激活的标签，切换到相邻标签
    if (activeTab.value === path) {
      const next = tabs.value[Math.min(idx, tabs.value.length - 1)]
      activeTab.value = next?.path || '/dashboard'
      return activeTab.value
    }
    return activeTab.value
  }

  /** 关闭其他标签 */
  function closeOtherTabs(path: string) {
    tabs.value = tabs.value.filter(t => !t.closable || t.path === path)
    activeTab.value = path
  }

  /** 关闭所有可关闭标签 */
  function closeAllTabs() {
    tabs.value = tabs.value.filter(t => !t.closable)
    activeTab.value = '/dashboard'
    router.push('/dashboard')
  }

  /** 点击标签 */
  function switchTab(path: string) {
    activeTab.value = path
    router.push(path)
  }

  return { tabs, activeTab, addTab, closeTab, closeOtherTabs, closeAllTabs, switchTab }
})
