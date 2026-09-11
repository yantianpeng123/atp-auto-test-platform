/**
 * 测试用例 - 接口层（对接后端 /api/case/**）
 */
import request from './request'
import type {
  CaseCreateParams,
  CaseInfo,
  CaseQuery,
  CaseStepInfo,
  CaseUpdateParams,
  PageResult,
  Result
} from './types'

/** 用例分页查询 */
export function getCaseList(query: CaseQuery): Promise<PageResult<CaseInfo>> {
  return request
    .get<unknown, Result<PageResult<CaseInfo>>>('/case/list', { params: query })
    .then((res) => res.data)
}

/** 用例详情（含步骤列表） */
export function getCaseDetail(id: number): Promise<CaseInfo> {
  return request.get<unknown, Result<CaseInfo>>(`/case/${id}`).then((res) => res.data)
}

/** 查询用例步骤列表 */
export function getCaseSteps(caseId: number): Promise<CaseStepInfo[]> {
  return request
    .get<unknown, Result<CaseStepInfo[]>>(`/case/${caseId}/steps`)
    .then((res) => res.data)
}

/** 新增用例（caseName 映射为后端 name 字段） */
export function createCase(data: CaseCreateParams): Promise<null> {
  const { caseName, ...rest } = data
  return request
    .post<unknown, Result<null>>('/case', { ...rest, name: caseName })
    .then((res) => res.data)
}

/** 编辑用例（caseName 映射为后端 name 字段） */
export function updateCase(data: CaseUpdateParams): Promise<null> {
  const { caseName, ...rest } = data
  return request
    .put<unknown, Result<null>>('/case', { ...rest, name: caseName })
    .then((res) => res.data)
}

/** 删除用例 */
export function deleteCase(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/case/${id}`).then((res) => res.data)
}

/** 启停用用例 */
export function updateCaseStatus(id: number, status: number): Promise<null> {
  return request
    .put<unknown, Result<null>>(`/case/${id}/status`, null, { params: { status } })
    .then((res) => res.data)
}

/** HAR 包导入用例，返回导入数量 */
export function importCaseHar(
  moduleId: number,
  caseName: string,
  harFile: File
): Promise<number> {
  const formData = new FormData()
  formData.append('moduleId', String(moduleId))
  formData.append('caseName', caseName)
  formData.append('har', harFile)
  return request
    .post<unknown, Result<number>>('/case/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    .then((res) => res.data)
}
