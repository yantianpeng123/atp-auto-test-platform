<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">ATP</span>
        <span class="brand-name">自动化测试平台</span>
      </div>

      <el-menu :default-active="activePath" :default-openeds="defaultOpeneds" class="menu" router>
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <el-sub-menu index="asset">
          <template #title>
            <el-icon><FolderOpened /></el-icon>
            <span>测试资产</span>
          </template>
          <el-menu-item index="/case">
            <el-icon><Document /></el-icon>
            <span>用例管理</span>
          </el-menu-item>
          <el-menu-item index="/dataset">
            <el-icon><Coin /></el-icon>
            <span>数据源管理</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="base">
          <template #title>
            <el-icon><Grid /></el-icon>
            <span>基础数据管理</span>
          </template>
          <el-menu-item index="/base/version">
            <el-icon><Collection /></el-icon>
            <span>工程版本管理</span>
          </el-menu-item>
          <el-menu-item index="/base/api">
            <el-icon><Link /></el-icon>
            <span>接口列表</span>
          </el-menu-item>
          <el-menu-item index="/base/component">
            <el-icon><Share /></el-icon>
            <span>组合组件</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="run">
          <template #title>
            <el-icon><VideoPlay /></el-icon>
            <span>执行与报告</span>
          </template>
          <el-menu-item index="/plan">
            <el-icon><Timer /></el-icon>
            <span>测试计划</span>
          </el-menu-item>
          <el-menu-item index="/batch">
            <el-icon><Clock /></el-icon>
            <span>定时任务</span>
          </el-menu-item>
          <el-menu-item index="/report">
            <el-icon><DataLine /></el-icon>
            <span>报告中心</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/env">
          <el-icon><Setting /></el-icon>
          <span>环境配置</span>
        </el-menu-item>

        <el-menu-item v-if="canManageProject" index="/project/info">
          <el-icon><Folder /></el-icon>
          <span>项目管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-tag type="warning" size="small" effect="plain">第一阶段</el-tag>
      </div>
    </aside>

    <div class="main">
      <header class="header">
        <div class="header-left">
          <span v-if="projectStore.currentProject" class="project-tag">
            当前项目：{{ projectStore.currentProject.name }}
          </span>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-entry">
              <el-avatar :size="30" class="avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="role" divided>
                     {{ roleText }}
                </el-dropdown-item>
                <el-dropdown-item command="project" divided>
                  <el-icon><Switch /></el-icon>
                  切换项目
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 标签栏 -->
      <div class="tabs-bar">
        <div class="tabs-scroll">
          <div
            v-for="tab in tabsStore.tabs"
            :key="tab.path"
            class="tab-item"
            :class="{ active: tab.path === tabsStore.activeTab }"
            @click="tabsStore.switchTab(tab.path)"
            @contextmenu.prevent="openTabMenu($event, tab)"
          >
            <el-icon v-if="tab.icon" class="tab-icon"><component :is="tab.icon" /></el-icon>
            <span class="tab-title">{{ tab.title }}</span>
            <el-icon
              v-if="tab.closable"
              class="tab-close"
              @click.stop="handleCloseTab(tab.path)"
            >
              <Close />
            </el-icon>
          </div>
        </div>

        <!-- 右键菜单 -->
        <div
          v-if="tabMenuVisible"
          class="tab-context-menu"
          :style="{ left: tabMenuLeft + 'px', top: tabMenuTop + 'px' }"
        >
          <div class="menu-item" @click="handleCloseOther">关闭其他</div>
          <div class="menu-item" @click="handleCloseAll">关闭所有</div>
        </div>
      </div>

      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Close,
  Coin,
  Collection,
  DataLine,
  Document,
  Folder,
  FolderOpened,
  Grid,
  Link,
  Odometer,
  Setting,
  Share,
  Switch,
  SwitchButton,
  Timer,
  VideoPlay,
  Clock
} from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import type { TabItem } from '@/stores/tabs'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()
const tabsStore = useTabsStore()

const activePath = computed(() => {
  if (route.path.startsWith('/case')) return '/case'
  if (route.path.startsWith('/batch')) return '/batch'
  if (route.path.startsWith('/base')) return route.path
  return route.path
})

const defaultOpeneds = computed(() => {
  const path = route.path
  if (path.startsWith('/base')) return ['base']
  if (path.startsWith('/api') || path.startsWith('/case') || path.startsWith('/dataset')) return ['asset']
  if (path.startsWith('/plan') || path.startsWith('/report') || path.startsWith('/batch')) return ['run']
  return []
})

/** 是否展示「项目管理」菜单：全局 ADMIN 或项目 OWNER/MAINTAINER */
const canManageProject = computed(
  () => userStore.isAdmin || projectStore.canManageProject
)

/** 进入布局即加载当前用户在项目中的角色（store 初始化读 localStorage 不会自动触发） */
function refreshProjectRole() {
  if (projectStore.currentProject) {
    projectStore.loadMyRole(projectStore.currentProject.id)
  }
}

const avatarText = computed(() => userStore.displayName.charAt(0).toUpperCase())

const roleMap: Record<string, string> = {
  ADMIN: '管理员',
  TESTER: '测试工程师',
  VIEWER: '只读成员'
}
const roleText = computed(() => roleMap[userStore.userInfo?.role || ''] || '未知')

async function handleCommand(command: string | number | object) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出登录')
    return
  }

  if (command === 'role') {
    router.push('/dashboard')
  }
  if (command === 'project') {
    router.push('/project/select')
  }
}

/* ---- 标签栏逻辑 ---- */
// 右键菜单
const tabMenuVisible = ref(false)
const tabMenuLeft = ref(0)
const tabMenuTop = ref(0)
const tabMenuTarget = ref<TabItem | null>(null)

function openTabMenu(e: MouseEvent, tab: TabItem) {
  tabMenuTarget.value = tab
  tabMenuLeft.value = e.clientX
  tabMenuTop.value = e.clientY
  tabMenuVisible.value = true
}

function handleCloseTab(path: string) {
  const nextPath = tabsStore.closeTab(path)
  if (nextPath !== tabsStore.activeTab || path === route.path) {
    router.push(nextPath)
  }
}

function handleCloseOther() {
  if (tabMenuTarget.value) {
    tabsStore.closeOtherTabs(tabMenuTarget.value.path)
    router.push(tabMenuTarget.value.path)
  }
  tabMenuVisible.value = false
}

function handleCloseAll() {
  tabsStore.closeAllTabs()
  tabMenuVisible.value = false
}

// 点击其他区域关闭右键菜单
document.addEventListener('click', () => {
  tabMenuVisible.value = false
})

// 监听路由变化，自动添加标签
watch(
  () => route.path,
  (path) => {
    if (path === '/project/select' || path === '/login' || path === '/register') return
    const title = (route.meta.title as string) || ''
    const icon = (route.meta.icon as string) || undefined
    if (title) {
      tabsStore.addTab(path, title, icon)
    }
  },
  { immediate: true }
)

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.loadUserInfo().catch(() => {
      router.push('/login')
    })
  }
  refreshProjectRole()
})

// 项目切换（store.setCurrentProject 已触发加载，这里兜底防止遗漏）
watch(
  () => projectStore.currentProject?.id,
  () => refreshProjectRole()
)
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  display: flex;
  flex-direction: column;
  width: 216px;
  flex: none;
  background: #0f2444;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  padding: 0 18px;
  flex: none;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #1d63d1;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.brand-name {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
  letter-spacing: 0.5px;
}

.menu {
  flex: 1;
  border-right: none;
  background: transparent;
  overflow-y: auto;
}

.menu :deep(.el-menu) {
  background: transparent;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  height: 46px;
  line-height: 46px;
  color: #b6c2d4;
  font-size: 14px;
}

.menu :deep(.el-menu-item:hover),
.menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #ffffff;
}

.menu :deep(.el-menu-item.is-active) {
  background: #1d63d1;
  color: #ffffff;
}

.menu :deep(.el-menu-item.is-disabled) {
  opacity: 0.42;
  cursor: not-allowed;
}

.sidebar-footer {
  padding: 16px;
  flex: none;
}

/* ---------- 主区域 ---------- */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #ebedf0;
  flex: none;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
}

.project-tag {
  margin-left: 700px;
  font-size: 30px;
  font-weight: 500;
  color: #1d63d1;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  color: #374151;
}

.avatar {
  background: #1d63d1;
  font-size: 13px;
}

.user-name {
  font-size: 14px;
}

/* ---------- 标签栏 ---------- */
.tabs-bar {
  flex: none;
  display: flex;
  align-items: center;
  height: 38px;
  background: #ffffff;
  border-bottom: 1px solid #e8eaed;
  padding: 0 8px;
  position: relative;
}

.tabs-scroll {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  height: 100%;
}

.tabs-scroll::-webkit-scrollbar {
  height: 0;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  background: #f4f5f7;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.15s ease;
  user-select: none;
}

.tab-item:hover {
  color: #1d63d1;
  background: #e8f0fe;
}

.tab-item.active {
  color: #1d63d1;
  background: #dbeafe;
  font-weight: 500;
}

.tab-icon {
  font-size: 13px;
}

.tab-close {
  font-size: 12px;
  border-radius: 50%;
  padding: 1px;
  margin-left: 2px;
  transition: all 0.15s;
}

.tab-close:hover {
  background: #c6ccd6;
  color: #1f2937;
}

/* 右键菜单 */
.tab-context-menu {
  position: fixed;
  z-index: 9999;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 120px;
}

.tab-context-menu .menu-item {
  padding: 7px 16px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: background 0.1s;
}

.tab-context-menu .menu-item:hover {
  background: #f0f5ff;
  color: #1d63d1;
}

/* ---------- 内容区 ---------- */
.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

@media (max-width: 768px) {
  .sidebar {
    width: 64px;
  }

  .brand-name,
  .sidebar-footer {
    display: none;
  }
}
</style>
