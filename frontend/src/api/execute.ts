/**
 * 用例执行 - 接口层（对接后端 /api/execute/**）
 */
import request from './request'
import type { CaseExecuteResult, Result } from './types'

/** 单用例调试执行 */
export function executeCase(caseId: number, envId: number): Promise<CaseExecuteResult> {
  return request
    .post<unknown, Result<CaseExecuteResult>>(`/execute/case/${caseId}`, { envId }, { timeout: 120000 })
    .then((res) => res.data)
}
