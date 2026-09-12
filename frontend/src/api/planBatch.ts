/**
 * 定时任务（批次） - 接口层（对接后端 /api/plan/batch/**）
 *
 * ⚠️ 当前后端 PlanBatch 模块尚未实现，前端骨架以本地 mock 驱动
 *    （见 views/plan/batch/* 顶部的 USE_MOCK 开关）。
 *    后端就绪后：将 USE_MOCK 改为 false 即可自动切换到本文件真实接口。
 */
import request from './request'
import type { Result, PageResult } from './types'

/** 执行策略 */
export type BatchStrategy = 'SERIAL' | 'PARALLEL'
/** 运行触发方式 */
export type RunTriggerType = 'MANUAL' | 'SCHEDULED'
/** 运行/批次状态 */
export type RunStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_FAILED' | 'FAILED'
/** 单个计划运行状态 */
export type RunItemStatus = 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'

/** 批次（列表/详情） */
export interface PlanBatchInfo {
  id: number
  projectId: number
  name: string
  strategy: BatchStrategy
  failContinue: boolean
  maxConcurrency: number
  cron: string | null
  enabled: boolean
  lastRunId: number | null
  lastRunTime: string | null
  lastRunStatus: RunStatus | null
  createTime: string
  updateTime: string
  // —— UI 临时态字段（非后端字段）——
  _toggling?: boolean
  _executing?: boolean
}

/** 批次查询参数（projectId 由前端按当前项目注入，实现「项目隔离」） */
export interface PlanBatchQuery {
  name?: string
  enabled?: boolean
  projectId?: number
  page: number
  size: number
}

/** 新建/编辑表单（projectId 标记批次归属的项目） */
export interface PlanBatchForm {
  id?: number
  name: string
  projectId: number
  strategy: BatchStrategy
  failContinue: boolean
  maxConcurrency: number
  planIds: number[]
  cron: string
  enabled: boolean
}

/** 批次运行 - 单个计划明细 */
export interface PlanBatchRunItem {
  id: number
  runId: number
  planId: number
  planName: string
  sortOrder: number
  status: RunItemStatus
  executionId: number | null
  durationMs: number | null
  errorMsg: string | null
  startTime: string | null
  endTime: string | null
}

/** 批次运行实例 */
export interface PlanBatchRun {
  id: number
  batchId: number
  triggerType: RunTriggerType
  status: RunStatus
  total: number
  passed: number
  failed: number
  running: number
  queued: number
  startTime: string
  endTime: string | null
  durationMs: number | null
  items: PlanBatchRunItem[]
}

/** 批次分页列表 */
export function getBatchList(query: PlanBatchQuery): Promise<PageResult<PlanBatchInfo>> {
  return request
    .get<unknown, Result<PageResult<PlanBatchInfo>>>('/plan/batch/list', { params: query })
    .then((res) => res.data)
}

/** 新建批次 */
export function createBatch(data: PlanBatchForm): Promise<null> {
  return request.post<unknown, Result<null>>('/plan/batch', data).then((res) => res.data)
}

/** 编辑批次 */
export function updateBatch(data: PlanBatchForm): Promise<null> {
  return request.put<unknown, Result<null>>('/plan/batch', data).then((res) => res.data)
}

/** 删除批次 */
export function deleteBatch(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/plan/batch/${id}`).then((res) => res.data)
}

/** 启/停用批次 */
export function toggleBatchEnabled(id: number, enabled: boolean): Promise<null> {
  return request
    .put<unknown, Result<null>>(`/plan/batch/${id}/enabled`, null, { params: { enabled } })
    .then((res) => res.data)
}

/** 立即执行批次（手动触发） */
export function executeBatch(id: number): Promise<PlanBatchRun> {
  return request
    .post<unknown, Result<PlanBatchRun>>(`/plan/batch/${id}/execute`)
    .then((res) => res.data)
}

/** 轮询：获取某次运行的实时状态 */
export function getBatchRun(runId: number): Promise<PlanBatchRun> {
  return request
    .get<unknown, Result<PlanBatchRun>>(`/plan/batch/run/${runId}`)
    .then((res) => res.data)
}

/** 某批次的运行历史 */
export function getBatchRuns(batchId: number): Promise<PlanBatchRun[]> {
  return request
    .get<unknown, Result<PlanBatchRun[]>>(`/plan/batch/${batchId}/runs`)
    .then((res) => res.data)
}
