/**
 * 项目级 RBAC - 接口层（对接后端 /api/project/**）
 *
 * 端点契约与 docs/phase5-features-design.md §1 / §6 保持一致。
 */
import request from './request'
import type {
  AddMemberParams,
  ProjectMember,
  ProjectRole,
  Result,
  UpdateMemberRoleParams
} from './types'

/** 当前项目的成员列表（OWNER/MAINTAINER 可见） */
export function getMembers(projectId: number): Promise<ProjectMember[]> {
  return request
    .get<unknown, Result<ProjectMember[]>>('/project/members', { params: { projectId } })
    .then((res) => res.data)
}

/** 当前登录用户在指定项目中的角色（侧边栏显隐、权限指令依赖） */
export function getMyRole(projectId: number): Promise<ProjectRole> {
  return request
    .get<unknown, Result<ProjectRole>>('/project/my-role', { params: { projectId } })
    .then((res) => res.data)
}

/** 邀请成员加入项目（按用户名直接添加） */
export function addMember(data: AddMemberParams): Promise<null> {
  return request.post<unknown, Result<null>>('/project/member', data).then((res) => res.data)
}

/** 修改成员在项目内的角色 */
export function updateMemberRole(data: UpdateMemberRoleParams): Promise<null> {
  return request.put<unknown, Result<null>>('/project/member/role', data).then((res) => res.data)
}

/** 将成员移出项目（逻辑删除） */
export function removeMember(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/project/member/${id}`).then((res) => res.data)
}
