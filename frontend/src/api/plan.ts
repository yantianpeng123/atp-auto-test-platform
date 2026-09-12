/**
 * 测试计划 - 接口层（对接后端 /api/plan/**）
 *
 * ⚠️ 当前后端 TestPlan 模块尚未实现，前端骨架以本地 mock 驱动
 *    （见 views/plan/index.vue 顶部的 USE_MOCK 开关）。
 *    后端就绪后：将 USE_MOCK 改为 false 即可自动切换到本文件真实接口。
 */
import request from './request'
import type { Result, PageResult } from './types'

/** 测试计划（列表/详情） */
export interface TestPlanInfo {
  id: number
  name: string
  projectId: number
  projectName?: string
  envId: number
  envName?: string
  caseCount: number
  cron: string | null
  enabled: boolean
  lastRunId: number | null
  lastRunTime: string | null
  createTime: string
  updateTime: string
  // —— UI 临时态字段（非后端字段）——
  _toggling?: boolean
  _executing?: boolean
}

/** 计划查询参数（projectId 由前端按当前项目注入，实现「项目隔离」） */
export interface TestPlanQuery {
  name?: string
  enabled?: boolean
  projectId?: number
  page: number
  size: number
}

/** 新建/编辑表单（projectId 标记计划归属的项目） */
export interface TestPlanForm {
  id?: number
  name: string
  projectId: number
  envId: number
  caseIds: number[]
  cron: string
  enabled: boolean
}

/** 单个用例的执行结果（计划执行返回） */
export interface PlanExecuteCaseResult {
  caseId: number
  caseName: string
  status: 'SUCCESS' | 'FAILED'
  durationMs: number
  passedSteps: number
  failedSteps: number
}

/** 计划执行汇总结果 */
export interface PlanExecuteResult {
  planId: number
  planName: string
  totalCases: number
  passedCases: number
  failedCases: number
  durationMs: number
  cases: PlanExecuteCaseResult[]
}

/** 计划分页列表 */
export function getPlanList(query: TestPlanQuery): Promise<PageResult<TestPlanInfo>> {
  return request
    .get<unknown, Result<PageResult<TestPlanInfo>>>('/plan/list', { params: query })
    .then((res) => res.data)
}

/** 新建计划 */
export function createPlan(data: TestPlanForm): Promise<null> {
  return request.post<unknown, Result<null>>('/plan', data).then((res) => res.data)
}

/** 编辑计划 */
export function updatePlan(data: TestPlanForm): Promise<null> {
  return request.put<unknown, Result<null>>('/plan', data).then((res) => res.data)
}

/** 删除计划 */
export function deletePlan(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/plan/${id}`).then((res) => res.data)
}

/** 启/停用计划 */
export function togglePlanEnabled(id: number, enabled: boolean): Promise<null> {
  return request
    .put<unknown, Result<null>>(`/plan/${id}/enabled`, null, { params: { enabled } })
    .then((res) => res.data)
}

/**
 * 拉取「关联用例」候选项（按项目过滤）。
 * 后端未单独提供时，复用用例列表接口；此处占位实现。
 */
export function getPlanCaseOptions(projectId: number): Promise<{ id: number; name: string }[]> {
  return request
    .get<unknown, Result<{ id: number; name: string }[]>>('/case/list', {
      params: { projectId, page: 1, size: 200 }
    })
    .then((res) => res.data)
}

/** 按计划执行 */
export function executePlan(id: number): Promise<PlanExecuteResult> {
  return request
    .post<unknown, Result<PlanExecuteResult>>(`/plan/${id}/execute`)
    .then((res) => res.data)
}
