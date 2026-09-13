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
 * 单次执行报告（Execution）。后端对应 ExecutionReportVO（Execution 头 + 轮次明细）。
 * 当前后端查询接口暂未提供，前端先用 mock 渲染；接口就绪后把下方
 * getExecutionReport 内部替换为 request.get(`/execute/${executionId}`).then(r => r.data)。
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

const MOCK_REPORT_LIST: ExecutionSummary[] = [
  { executionId: 9001, planId: 1, planName: '回归测试-全量', caseId: 1, caseName: '登录接口冒烟', envId: 1, envName: '测试环境', triggerType: 'SCHEDULED', executorName: 'scheduler', status: 'FAILED', startTime: '2026-09-12 02:00:12', durationMs: 36000, totalRounds: 2, passedRounds: 1, failedRounds: 1 },
  { executionId: 9002, planId: 2, planName: '冒烟测试-核心链路', caseId: 2, caseName: '下单流程', envId: 2, envName: '预发环境', triggerType: 'MANUAL', executorName: '张三', status: 'SUCCESS', startTime: '2026-09-12 10:21:03', durationMs: 21400, totalRounds: 1, passedRounds: 1, failedRounds: 0 },
  { executionId: 9003, planId: 1, planName: '回归测试-全量', caseId: 3, caseName: '支付回调校验', envId: 1, envName: '测试环境', triggerType: 'SCHEDULED', executorName: 'scheduler', status: 'SUCCESS', startTime: '2026-09-12 02:03:44', durationMs: 12800, totalRounds: 1, passedRounds: 1, failedRounds: 0 },
  { executionId: 9004, planId: null, planName: null, caseId: 4, caseName: '商品详情页', envId: 1, envName: '测试环境', triggerType: 'CI', executorName: 'ci-runner', status: 'SUCCESS', startTime: '2026-09-12 09:15:30', durationMs: 8900, totalRounds: 1, passedRounds: 1, failedRounds: 0 },
  { executionId: 9005, planId: 2, planName: '冒烟测试-核心链路', caseId: 5, caseName: '购物车结算', envId: 2, envName: '预发环境', triggerType: 'MANUAL', executorName: '李四', status: 'FAILED', startTime: '2026-09-12 11:02:18', durationMs: 15300, totalRounds: 2, passedRounds: 1, failedRounds: 1 },
  { executionId: 9006, planId: 1, planName: '回归测试-全量', caseId: 1, caseName: '登录接口冒烟', envId: 1, envName: '测试环境', triggerType: 'SCHEDULED', executorName: 'scheduler', status: 'RUNNING', startTime: '2026-09-13 02:00:10', durationMs: 0, totalRounds: 2, passedRounds: 0, failedRounds: 0 },
  { executionId: 9007, planId: 3, planName: '接口全量校验', caseId: 2, caseName: '下单流程', envId: 1, envName: '测试环境', triggerType: 'SCHEDULED', executorName: 'scheduler', status: 'SUCCESS', startTime: '2026-09-11 01:00:00', durationMs: 19900, totalRounds: 1, passedRounds: 1, failedRounds: 0 },
  { executionId: 9008, planId: 4, planName: '核心链路巡检', caseId: 6, caseName: '消息通知链路', envId: 1, envName: '测试环境', triggerType: 'MANUAL', executorName: '王五', status: 'FAILED', startTime: '2026-09-12 14:48:55', durationMs: 23100, totalRounds: 3, passedRounds: 2, failedRounds: 1 }
]

export function getReportList(params: {
  page?: number
  size?: number
  keyword?: string
  status?: string
}): Promise<{ records: ExecutionSummary[]; total: number }> {
  // TODO(后端): 替换为 request.get<unknown, Result<{records,total}>>(`/execute/list`, { params }).then((r) => r.data)
  const kw = (params.keyword || '').trim()
  let data = MOCK_REPORT_LIST.filter((r) => {
    if (params.status && r.status !== params.status) return false
    if (kw && !(r.caseName.includes(kw) || (r.planName || '').includes(kw))) return false
    return true
  })
  const total = data.length
  const page = params.page || 1
  const size = params.size || 10
  data = data.slice((page - 1) * size, page * size)
  return Promise.resolve({ records: data, total })
}

export function getExecutionReport(executionId: number): Promise<ExecutionReport> {
  // TODO(后端): 替换为 request.get<unknown, Result<ExecutionReport>>(`/execute/${executionId}`).then((r) => r.data)
  return Promise.resolve(buildMockReport(executionId))
}

function buildMockReport(executionId: number): ExecutionReport {
  const requestBody = JSON.stringify(
    { username: 'tester01', password: '******', captcha: '8K2P', remember: true },
    null,
    2
  )
  const responseOk = JSON.stringify(
    {
      code: 0,
      message: 'success',
      data: {
        token: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxx.yyy',
        userId: 1024,
        roles: ['tester', 'viewer'],
        expireAt: '2026-09-14T23:00:00Z'
      }
    },
    null,
    2
  )
  const responseErr = JSON.stringify(
    { code: 500, message: 'Internal Server Error', data: null, traceId: 'a1b2c3d4-5e6f-7890' },
    null,
    2
  )

  return {
    executionId,
    planId: 1,
    planName: '回归测试-全量',
    caseId: 1,
    caseName: '登录接口冒烟',
    envId: 1,
    envName: '测试环境',
    triggerType: 'SCHEDULED',
    executorId: 7,
    executorName: 'scheduler',
    status: 'FAILED',
    startTime: '2026-09-12 02:00:12',
    endTime: '2026-09-12 02:00:48',
    durationMs: 36000,
    totalRounds: 2,
    passedRounds: 1,
    failedRounds: 1,
    rounds: [
      {
        roundIndex: 1,
        params: { dataset: '正常账号' },
        passedSteps: 3,
        failedSteps: 0,
        status: 'SUCCESS',
        durationMs: 18000,
        steps: [
          {
            stepId: 1,
            stepName: '获取登录验证码',
            sortOrder: 1,
            method: 'GET',
            url: 'https://api.test.com/captcha',
            requestHeaders: JSON.stringify({ Accept: 'application/json' }, null, 2),
            requestBody: null,
            statusCode: 200,
            responseHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            responseBody: JSON.stringify({ code: 0, data: { captchaId: 'C-9981' } }, null, 2),
            assertResults: [
              { type: 'status', path: null, operator: 'eq', expected: '200', actual: '200', passed: true, message: null },
              { type: 'jsonPath', path: '$.code', operator: 'eq', expected: '0', actual: '0', passed: true, message: null }
            ],
            status: 'PASSED',
            errorMsg: null,
            durationMs: 3200
          },
          {
            stepId: 2,
            stepName: '提交登录',
            sortOrder: 2,
            method: 'POST',
            url: 'https://api.test.com/login',
            requestHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            requestBody,
            statusCode: 200,
            responseHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            responseBody: responseOk,
            assertResults: [
              { type: 'status', path: null, operator: 'eq', expected: '200', actual: '200', passed: true, message: null },
              { type: 'jsonPath', path: '$.code', operator: 'eq', expected: '0', actual: '0', passed: true, message: null },
              { type: 'jsonPath', path: '$.data.token', operator: 'exists', expected: null, actual: 'exists', passed: true, message: null }
            ],
            status: 'PASSED',
            errorMsg: null,
            durationMs: 5400
          },
          {
            stepId: 3,
            stepName: '查询用户信息',
            sortOrder: 3,
            method: 'GET',
            url: 'https://api.test.com/user/info',
            requestHeaders: JSON.stringify({ Authorization: 'Bearer <token>' }, null, 2),
            requestBody: null,
            statusCode: 200,
            responseHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            responseBody: JSON.stringify({ code: 0, data: { nickName: 'tester01', level: 3 } }, null, 2),
            assertResults: [
              { type: 'jsonPath', path: '$.data.nickName', operator: 'eq', expected: 'tester01', actual: 'tester01', passed: true, message: null }
            ],
            status: 'PASSED',
            errorMsg: null,
            durationMs: 3100
          }
        ]
      },
      {
        roundIndex: 2,
        params: { dataset: '错误密码' },
        passedSteps: 1,
        failedSteps: 1,
        status: 'FAILED',
        durationMs: 21000,
        steps: [
          {
            stepId: 4,
            stepName: '提交登录(错误密码)',
            sortOrder: 1,
            method: 'POST',
            url: 'https://api.test.com/login',
            requestHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            requestBody: JSON.stringify({ username: 'tester01', password: 'wrong', captcha: '8K2P' }, null, 2),
            statusCode: 500,
            responseHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            responseBody: responseErr,
            assertResults: [
              { type: 'status', path: null, operator: 'eq', expected: '200', actual: '500', passed: false, message: '状态码断言失败：预期 200，实际 500' },
              { type: 'jsonPath', path: '$.code', operator: 'eq', expected: '0', actual: '500', passed: false, message: '业务码断言失败：预期 0，实际 500' }
            ],
            status: 'FAILED',
            errorMsg: '断言失败：预期 200，实际 500',
            durationMs: 12000
          },
          {
            stepId: 5,
            stepName: '查询用户信息',
            sortOrder: 2,
            method: 'GET',
            url: 'https://api.test.com/user/info',
            requestHeaders: JSON.stringify({ Authorization: 'Bearer <token>' }, null, 2),
            requestBody: null,
            statusCode: 200,
            responseHeaders: JSON.stringify({ 'Content-Type': 'application/json' }, null, 2),
            responseBody: JSON.stringify({ code: 0, data: { nickName: 'tester01' } }, null, 2),
            assertResults: [
              { type: 'jsonPath', path: '$.code', operator: 'eq', expected: '0', actual: '0', passed: true, message: null }
            ],
            status: 'PASSED',
            errorMsg: null,
            durationMs: 2600
          }
        ]
      }
    ]
  }
}
