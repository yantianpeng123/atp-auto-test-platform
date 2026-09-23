/**
 * CI 集成配置 - 接口层（对接后端 /api/ci/config/**）
 *
 * 端点契约：
 *  - GET  /api/ci/config?projectId=  查询配置（未配置时返回 data=null，不会抛错）
 *  - POST /api/ci/config             新增/更新配置（新建时返回一次性明文令牌）
 *  - POST /api/ci/config/{projectId}/token  重新生成令牌（返回新明文令牌，旧令牌立即失效）
 *
 * 注：对外触发/轮询接口 /api/ci/trigger、/api/ci/result/** 走免登录的 X-CI-Token 校验，
 * 由外部 CI（Jenkins 等）调用，前端无需对接。
 */
import request from './request'
import type { CiConfig, CiConfigSaveParams, Result } from './types'

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
