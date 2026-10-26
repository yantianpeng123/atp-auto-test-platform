/**
 * 工作台统计 - 接口层（对接后端 /api/dashboard/**）
 */
import request from './request'
import type { DashboardStats, Result } from './types'

/** 获取工作台统计卡片数据（全平台聚合） */
export function getDashboardStats(): Promise<DashboardStats> {
  return request
    .get<unknown, Result<DashboardStats>>('/dashboard/stats')
    .then((res) => res.data)
}
