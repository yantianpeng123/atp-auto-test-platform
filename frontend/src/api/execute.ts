/**
 * 用例执行 - 接口层（对接后端 /api/execute/**）
 */
import request from './request'
import type { CaseExecuteResult, Result } from './types'

/** 单用例调试执行；debug=true 时不落库（仅正式执行记录） */
export function executeCase(caseId: number, envId: number, debug = false): Promise<CaseExecuteResult> {
  return request
    .post<unknown, Result<CaseExecuteResult>>(`/execute/case/${caseId}`, { envId, debug }, { timeout: 120000 })
    .then((res) => res.data)
}

/** 查询用例最近一次执行记录（持久化数据，刷新不丢） */
export function getExecutionHistory(caseId: number): Promise<CaseExecuteResult | null> {
  return request
    .get<unknown, Result<CaseExecuteResult | null>>(`/execute/history/${caseId}`)
    .then((res) => res.data)
}
