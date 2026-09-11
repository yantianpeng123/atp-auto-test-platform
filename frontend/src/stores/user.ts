import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  getUserInfo,
  login as loginApi,
  logout as logoutApi,
  register as registerApi
} from '@/api/auth'
import type { LoginParams, RegisterParams, UserInfo } from '@/api/types'
import { clearAuth, getToken, setToken } from '@/utils/auth'
import { useProjectStore } from './project'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref(false)

  const isLogin = computed(() => Boolean(token.value))
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || '未登录'
  )
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  /** 登录：保存令牌与用户信息 */
  async function login(params: LoginParams) {
    loading.value = true
    try {
      const data = await loginApi(params)
      token.value = data.token
      userInfo.value = data.userInfo
      setToken(data.token)
      return data
    } finally {
      loading.value = false
    }
  }

  /** 注册 */
  async function register(params: RegisterParams) {
    loading.value = true
    try {
      await registerApi(params)
    } finally {
      loading.value = false
    }
  }

  /** 拉取当前用户信息（刷新页面后恢复登录态） */
  async function loadUserInfo() {
    if (!token.value) {
      return null
    }
    try {
      const info = await getUserInfo()
      userInfo.value = info
      return info
    } catch (error) {
      token.value = ''
      clearAuth()
      throw error
    }
  }

  /** 登出 */
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      // 后端登出失败不影响前端清理
      console.warn('logout api failed', error)
    } finally {
      token.value = ''
      userInfo.value = null
      clearAuth()
      useProjectStore().setCurrentProject(null)
      router.push('/login')
    }
  }

  return {
    token,
    userInfo,
    loading,
    isLogin,
    displayName,
    isAdmin,
    login,
    register,
    loadUserInfo,
    logout
  }
})
