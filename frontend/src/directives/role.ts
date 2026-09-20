/**
 * v-role 指令：按当前用户在「当前项目」中的角色控制元素显隐。
 *
 * 用法：
 *   <el-button v-role="['OWNER','MAINTAINER']">添加成员</el-button>
 *   <el-button v-role="'OWNER'">删除项目</el-button>
 *
 * 规则：
 * - 全局 ADMIN（userStore.isAdmin）恒为可见（超管跨项目）。
 * - 未传值时默认任意角色可见（即仅做登录态校验）。
 * - 当前角色为 null（后端未实现 / 尚未加载）时一律隐藏，避免越权暴露。
 */
import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'
import { useProjectStore } from '@/stores/project'
import type { ProjectRole } from '@/api/types'

function resolveRequired(binding: DirectiveBinding): ProjectRole[] | null {
  const v = binding.value as ProjectRole[] | ProjectRole | undefined
  if (v == null) return null
  return (Array.isArray(v) ? v : [v]) as ProjectRole[]
}

function isVisible(binding: DirectiveBinding): boolean {
  const userStore = useUserStore()
  // 全局超管恒可见
  if (userStore.isAdmin) return true

  const required = resolveRequired(binding)
  // 未指定角色要求：只要已登录即可见
  if (required === null) return true

  const role = useProjectStore().currentProjectRole
  if (!role) return false
  return required.includes(role)
}

export const roleDirective: Directive = {
  mounted(el: HTMLElement, binding) {
    if (!isVisible(binding)) el.style.display = 'none'
  },
  updated(el: HTMLElement, binding) {
    if (!isVisible(binding)) el.style.display = 'none'
    else el.style.display = ''
  }
}
