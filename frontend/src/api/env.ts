/**
 * 测试环境 - 接口层（对接后端 /api/env/**）
 */
import request from './request'
import type {
  EnvCreateParams,
  EnvInfo,
  EnvQuery,
  EnvUpdateParams,
  PageResult,
  Result
} from './types'

/** 环境分页查询 */
export function getEnvList(query: EnvQuery): Promise<PageResult<EnvInfo>> {
  return request
    .get<unknown, Result<PageResult<EnvInfo>>>('/env/list', { params: query })
    .then((res) => res.data)
}

/** 新增环境 */
export function createEnv(data: EnvCreateParams): Promise<null> {
  return request.post<unknown, Result<null>>('/env', data).then((res) => res.data)
}

/** 编辑环境 */
export function updateEnv(data: EnvUpdateParams): Promise<null> {
  return request.put<unknown, Result<null>>('/env', data).then((res) => res.data)
}

/** 删除环境 */
export function deleteEnv(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/env/${id}`).then((res) => res.data)
}
