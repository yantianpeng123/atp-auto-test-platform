/**
 * 组合组件 - 接口层（对接后端 /api/component/**）
 */
import request from './request'
import type {
  ApiComponentInfo,
  ApiComponentQuery,
  ApiComponentSaveParams,
  PageResult,
  Result
} from './types'

/** 组合组件分页查询 */
export function getComponentList(query: ApiComponentQuery): Promise<PageResult<ApiComponentInfo>> {
  return request
    .get<unknown, Result<PageResult<ApiComponentInfo>>>('/component/list', { params: query })
    .then((res) => res.data)
}

/** 组件详情（含子步骤） */
export function getComponentDetail(id: number): Promise<ApiComponentInfo> {
  return request.get<unknown, Result<ApiComponentInfo>>(`/component/${id}`).then((res) => res.data)
}

/** 新增组件 */
export function createComponent(data: ApiComponentSaveParams): Promise<null> {
  return request.post<unknown, Result<null>>('/component', data).then((res) => res.data)
}

/** 编辑组件 */
export function updateComponent(data: ApiComponentSaveParams): Promise<null> {
  return request.put<unknown, Result<null>>('/component', data).then((res) => res.data)
}

/** 删除组件 */
export function deleteComponent(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/component/${id}`).then((res) => res.data)
}
