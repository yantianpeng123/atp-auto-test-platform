/**
 * 基础数据管理 - 接口层（已对接后端 /api/base/**）
 */
import request from './request'
import type {
  ApiCreateParams,
  ApiInfo,
  ApiQuery,
  ApiUpdateParams,
  ApplicationCreateParams,
  ModuleCreateParams,
  OptionItem,
  PageResult,
  Result,
  VersionCreateParams,
  VersionInfo,
  VersionInfoQuery
} from './types'

/** 获取工程名称下拉选项（可按项目过滤） */
export function getProjectOptions(projectId?: number): Promise<OptionItem[]> {
  return request
    .get<unknown, Result<OptionItem[]>>('/base/project/options', { params: { projectId } })
    .then((res) => res.data)
}

/** 获取版本名称下拉选项（可按工程过滤） */
export function getVersionOptions(applicationId?: number): Promise<OptionItem[]> {
  return request
    .get<unknown, Result<OptionItem[]>>('/base/version/options', { params: { applicationId } })
    .then((res) => res.data)
}

/** 获取模块名称下拉选项（可按版本过滤） */
export function getModuleOptions(versionId?: number): Promise<OptionItem[]> {
  return request
    .get<unknown, Result<OptionItem[]>>('/base/module/options', { params: { versionId } })
    .then((res) => res.data)
}

/** 分页查询工程版本信息 */
export function getVersionList(query: VersionInfoQuery): Promise<PageResult<VersionInfo>> {
  return request
    .get<unknown, Result<PageResult<VersionInfo>>>('/base/version/list', { params: query })
    .then((res) => res.data)
}

/** 新增工程 */
export function createProject(data: ApplicationCreateParams): Promise<null> {
  return request.post<unknown, Result<null>>('/base/project', data).then((res) => res.data)
}

/** 新增版本 */
export function createVersion(data: VersionCreateParams): Promise<null> {
  return request.post<unknown, Result<null>>('/base/version', data).then((res) => res.data)
}

/** 新增模块 */
export function createModules(data: ModuleCreateParams): Promise<null> {
  return request.post<unknown, Result<null>>('/base/module', data).then((res) => res.data)
}

/** 接口列表分页查询 */
export function getApiList(query: ApiQuery): Promise<PageResult<ApiInfo>> {
  return request
    .get<unknown, Result<PageResult<ApiInfo>>>('/base/api/list', { params: query })
    .then((res) => res.data)
}

/** 新增接口 */
export function createApi(data: ApiCreateParams): Promise<null> {
  return request.post<unknown, Result<null>>('/base/api', data).then((res) => res.data)
}

/** 编辑接口 */
export function updateApi(data: ApiUpdateParams): Promise<null> {
  return request.put<unknown, Result<null>>('/base/api', data).then((res) => res.data)
}

/** 删除接口 */
export function deleteApi(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/base/api/${id}`).then((res) => res.data)
}

/** Jar 包导入接口，返回导入数量 */
export function importApiJar(moduleId: number, jarFile: File): Promise<number> {
  const formData = new FormData()
  formData.append('moduleId', String(moduleId))
  formData.append('jar', jarFile)
  return request
    .post<unknown, Result<number>>('/base/api/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    .then((res) => res.data)
}
