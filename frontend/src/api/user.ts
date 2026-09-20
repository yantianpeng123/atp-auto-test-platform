import request from './request'
import type { Result, UserInfo } from './types'

/**
 * 平台所有用户列表（用于「添加成员」下拉框等选择场景）
 * 对应后端 GET /api/user/list
 */
export function getUserList(): Promise<UserInfo[]> {
  return request.get<unknown, Result<UserInfo[]>>('/user/list').then((res) => res.data)
}
