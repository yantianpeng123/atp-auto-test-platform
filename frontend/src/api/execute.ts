/**
 * 用例执行 - 接口层（对接后端 /api/execute/**）
 */
import request from './request'
import type { CaseExecuteResult, Result, RoundExecuteResult } from './types'

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

/**
 * 单次执行报告（Execution）。对应后端 ExecutionReportVO（Execution 头 + 轮次明细）。
 * GET /api/execute/{executionId}
 */
export interface ExecutionReport {
  executionId: number
  planId: number | null
  planName: string | null
  caseId: number
  caseName: string
  envId: number
  envName: string
  triggerType: 'MANUAL' | 'SCHEDULED' | 'CI'
  executorId: number | null
  executorName: string | null
  status: 'RUNNING' | 'SUCCESS' | 'FAILED'
  startTime: string
  endTime: string | null
  durationMs: number
  totalRounds: number
  passedRounds: number
  failedRounds: number
  rounds: RoundExecuteResult[]
}

/** 报告中心列表项（执行记录概要，对应后端未来 GET /api/execute/list） */
export interface ExecutionSummary {
  executionId: number
  planId: number | null
  planName: string | null
  caseId: number
  caseName: string
  envId: number
  envName: string
  triggerType: 'MANUAL' | 'SCHEDULED' | 'CI'
  executorName: string | null
  status: 'RUNNING' | 'SUCCESS' | 'FAILED'
  startTime: string
  durationMs: number
  totalRounds: number
  passedRounds: number
  failedRounds: number
}

export function getReportList(params: {
  page?: number
  size?: number
  keyword?: string
  status?: string
  projectId?: number | null
}): Promise<{ records: ExecutionSummary[]; total: number }> {
  return request
    .get<unknown, Result<{ records: ExecutionSummary[]; total: number }>>('/execute/list', { params })
    .then((res) => res.data)
}

export function getExecutionReport(executionId: number): Promise<ExecutionReport> {
  return request
    .get<unknown, Result<ExecutionReport>>(`/execute/${executionId}`)
    .then((res) => res.data)
}
