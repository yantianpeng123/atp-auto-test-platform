/**
 * CI 集成 - 接口层（对接后端 /api/ci/**）
 *
 * 配置端点（对接 /api/ci/config/**）：
 *  - GET  /api/ci/config?projectId=  查询配置（未配置时返回 data=null，不会抛错）
 *  - POST /api/ci/config             新增/更新配置（新建时返回一次性明文令牌）
 *  - POST /api/ci/config/{projectId}/token  重新生成令牌（返回新明文令牌，旧令牌立即失效）
 *
 * 运行记录（平台内查看，走 JWT）：
 *  - GET  /api/ci/runs?projectId=&page=&size=  按项目分页列出 CI 运行记录
 *  - GET  /api/ci/report/{runId}.xml           JUnit 格式 XML 报告（外部 CI 用 X-CI-Token；平台内走 JWT）
 *
 * 注：对外触发/轮询接口 /api/ci/trigger、/api/ci/result/** 走免登录的 X-CI-Token 校验，
 * 由外部 CI（Jenkins 等）调用，前端无需对接。
 */
import request from './request'
import type { CiConfig, CiConfigSaveParams, Result, PageResult } from './types'

/** 运行/批次状态 */
export type CiRunStatus = 'RUNNING' | 'SUCCESS' | 'PARTIAL_FAILED' | 'FAILED'

/** CI 运行记录列表项 */
export interface CiRunItem {
  /** 运行实例ID（即批次运行 runId） */
  runId: number
  /** 批次ID */
  batchId: number
  /** 批次名称 */
  batchName: string | null
  /** 触发方式，固定为 CI */
  triggerType: string
  status: CiRunStatus
  total: number | null
  passed: number | null
  failed: number | null
  startTime: string | null
  durationMs: number | null
}

/** 按项目查询 CI 配置（未配置时返回 null） */
export function getCiConfig(projectId: number): Promise<CiConfig | null> {
  return request
    .get<unknown, Result<CiConfig | null>>('/ci/config', { params: { projectId } })
    .then((res) => res.data)
}

/** 新增/更新 CI 配置（新建时返回一次性明文令牌） */
export function upsertCiConfig(data: CiConfigSaveParams): Promise<CiConfig> {
  return request.post<unknown, Result<CiConfig>>('/ci/config', data).then((res) => res.data)
}

/** 重新生成令牌（旧令牌立即失效，返回新明文令牌） */
export function regenerateCiToken(projectId: number): Promise<CiConfig> {
  return request
    .post<unknown, Result<CiConfig>>(`/ci/config/${projectId}/token`)
    .then((res) => res.data)
}

/** 按项目分页列出 CI 运行记录（平台内运行历史） */
export function getCiRuns(
  projectId: number,
  page = 1,
  size = 10
): Promise<PageResult<CiRunItem>> {
  return request
    .get<unknown, Result<PageResult<CiRunItem>>>('/ci/runs', {
      params: { projectId, page, size }
    })
    .then((res) => res.data)
}

/** 获取某次运行的 JUnit XML 报告文本（平台内渲染/下载；走登录态） */
export function getCiReportXml(runId: number): Promise<string> {
  return request
    .get(`/ci/report/${runId}.xml`, { responseType: 'text' })
    .then((res) => (res as unknown as { data: string }).data)
}

/** 下载某次运行的 JUnit XML 报告文件（走登录态） */
export function downloadCiReport(runId: number): Promise<Blob> {
  return request
    .get(`/ci/report/${runId}.xml`, { responseType: 'blob' })
    .then((res) => (res as unknown as { data: Blob }).data)
}
