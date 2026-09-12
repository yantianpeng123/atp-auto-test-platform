import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/auth'

const CURRENT_PROJECT_KEY = 'atp_current_project'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/project/select',
    name: 'ProjectSelect',
    component: () => import('@/views/project/select.vue'),
    meta: { title: '选择项目' }
  },
  {
    path: '/',
    component: () => import('@/layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'base/version',
        name: 'ProjectVersion',
        component: () => import('@/views/base/version/index.vue'),
        meta: { title: '工程版本信息', icon: 'Collection' }
      },
      {
        path: 'base/api',
        name: 'ApiList',
        component: () => import('@/views/base/api/index.vue'),
        meta: { title: '接口列表', icon: 'Link' }
      },
      {
        path: 'env',
        name: 'EnvConfig',
        component: () => import('@/views/env/index.vue'),
        meta: { title: '环境配置', icon: 'Setting' }
      },
      {
        path: 'case',
        name: 'CaseList',
        component: () => import('@/views/case/index.vue'),
        meta: { title: '用例管理', icon: 'Document' }
      },
      {
        path: 'case/edit',
        name: 'CaseEdit',
        component: () => import('@/views/case/edit.vue'),
        meta: { title: '用例编辑', icon: 'Document', hidden: true }
      },
      {
        path: 'dataset',
        name: 'DatasetList',
        component: () => import('@/views/dataset/index.vue'),
        meta: { title: '数据源管理', icon: 'Coin' }
      },
      {
        path: 'plan',
        name: 'PlanList',
        component: () => import('@/views/plan/index.vue'),
        meta: { title: '测试计划', icon: 'Timer' }
      },
      {
        path: 'batch',
        name: 'PlanBatchList',
        component: () => import('@/views/plan/batch/index.vue'),
        meta: { title: '定时任务', icon: 'Clock' }
      },
      {
        path: 'batch/:id',
        name: 'PlanBatchDetail',
        component: () => import('@/views/plan/batch/detail.vue'),
        meta: { title: '批次详情', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to, _from, next) => {
  const token = getToken()
  const title = (to.meta.title as string) || ''
  document.title = title ? `${title} - ATP 自动化测试平台` : 'ATP 自动化测试平台'

  if (to.meta.public) {
    // 已登录用户访问登录页则直接进项目选择页
    if (token && to.path === '/login') {
      next('/project/select')
      return
    }
    next()
    return
  }

  if (!token) {
    next({ path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined })
    return
  }

  // 已登录但尚未选择项目：除项目选择页外，统一先去选项目
  if (to.path !== '/project/select' && !localStorage.getItem(CURRENT_PROJECT_KEY)) {
    next('/project/select')
    return
  }

  next()
})

export default router
