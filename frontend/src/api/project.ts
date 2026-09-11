import request from './request'
import type { Project, ProjectCreateParams, Result } from './types'

/** 当前用户可用项目列表 */
export function listProjects(): Promise<Project[]> {
  return request.get<unknown, Result<Project[]>>('/project/list').then((res) => res.data)
}

/** 新增项目（仅管理员） */
export function createProject(data: ProjectCreateParams): Promise<null> {
  return request.post<unknown, Result<null>>('/project', data).then((res) => res.data)
}
