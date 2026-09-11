/**
 * 数据源（模板 + 数据项）- 接口层（对接后端 /api/dataset/**）
 */
import request from './request'
import type {
  CaseDatasetInfo,
  CaseDatasetQuery,
  CaseDatasetSaveParams,
  PageResult,
  Result
} from './types'

/** 数据源模板分页查询 */
export function getDatasetList(query: CaseDatasetQuery): Promise<PageResult<CaseDatasetInfo>> {
  return request
    .get<unknown, Result<PageResult<CaseDatasetInfo>>>('/dataset/template/list', { params: query })
    .then((res) => res.data)
}

/** 数据源模板详情（含字段与数据项） */
export function getDatasetDetail(id: number): Promise<CaseDatasetInfo> {
  return request
    .get<unknown, Result<CaseDatasetInfo>>(`/dataset/template/${id}`)
    .then((res) => res.data)
}

/** 新增数据源模板（含字段与数据项） */
export function createDataset(data: CaseDatasetSaveParams): Promise<null> {
  return request.post<unknown, Result<null>>('/dataset/template', data).then((res) => res.data)
}

/** 编辑数据源模板（含字段与数据项，整体替换） */
export function updateDataset(data: CaseDatasetSaveParams): Promise<null> {
  return request.put<unknown, Result<null>>('/dataset/template', data).then((res) => res.data)
}

/** 删除数据源模板 */
export function deleteDataset(id: number): Promise<null> {
  return request
    .delete<unknown, Result<null>>(`/dataset/template/${id}`)
    .then((res) => res.data)
}
