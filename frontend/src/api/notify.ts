/**
 * 通知中心 - 接口层（对接后端 /api/notify/**）
 *
 * 端点契约与 docs/phase5-features-design.md §3 / §4 保持一致。
 * 注意：后端尚未实现，下列调用目前在运行时返回错误，前端已做空数据兜底。
 */
import request from './request'
import type {
  NotifyChannel,
  NotifyChannelSaveParams,
  NotifyRule,
  NotifyRuleSaveParams,
  NotifyLog,
  NotifyMessage,
  PageResult,
  Result
} from './types'

/* ---------------- 渠道 ---------------- */

/** 当前项目的通知渠道列表（OWNER/MAINTAINER 可见） */
export function getChannels(projectId: number): Promise<NotifyChannel[]> {
  return request
    .get<unknown, Result<NotifyChannel[]>>('/notify/channel', { params: { projectId } })
    .then((res) => res.data)
}

/** 新增渠道 */
export function createChannel(data: NotifyChannelSaveParams): Promise<null> {
  return request.post<unknown, Result<null>>('/notify/channel', data).then((res) => res.data)
}

/** 修改渠道（名称 / 启用状态 / 配置） */
export function updateChannel(
  id: number,
  data: Partial<NotifyChannelSaveParams>
): Promise<null> {
  return request.put<unknown, Result<null>>(`/notify/channel/${id}`, data).then((res) => res.data)
}

/** 删除渠道（逻辑删除） */
export function deleteChannel(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/notify/channel/${id}`).then((res) => res.data)
}

/* ---------------- 规则 ---------------- */

/** 当前项目的通知规则列表 */
export function getRules(projectId: number): Promise<NotifyRule[]> {
  return request
    .get<unknown, Result<NotifyRule[]>>('/notify/rule', { params: { projectId } })
    .then((res) => res.data)
}

/** 新增规则 */
export function createRule(data: NotifyRuleSaveParams): Promise<null> {
  return request.post<unknown, Result<null>>('/notify/rule', data).then((res) => res.data)
}

/** 删除规则 */
export function deleteRule(id: number): Promise<null> {
  return request.delete<unknown, Result<null>>(`/notify/rule/${id}`).then((res) => res.data)
}

/* ---------------- 发送日志 ---------------- */

/** 发送日志（分页） */
export function getLogs(
  projectId: number,
  page = 1,
  size = 20
): Promise<PageResult<NotifyLog>> {
  return request
    .get<unknown, Result<PageResult<NotifyLog>>>('/notify/log', {
      params: { projectId, page, size }
    })
    .then((res) => res.data)
}

/* ---------------- 站内信收件箱（全员） ---------------- */

/** 我的站内信；unread 为 true 时仅返回未读 */
export function getMessages(projectId: number, unread?: boolean): Promise<NotifyMessage[]> {
  return request
    .get<unknown, Result<NotifyMessage[]>>('/notify/messages', {
      params: { projectId, unread: unread === undefined ? undefined : unread ? 1 : 0 }
    })
    .then((res) => res.data)
}

/** 标记已读：ids 为单条/多条，或传 'all' 标记全部已读 */
export function markMessagesRead(
  ids: number[] | 'all',
  projectId: number
): Promise<null> {
  return request
    .post<unknown, Result<null>>('/notify/messages/read', {
      projectId,
      ids: ids === 'all' ? [] : ids,
      all: ids === 'all'
    })
    .then((res) => res.data)
}

/** 未读角标数（顶栏轮询） */
export function getUnreadCount(projectId: number): Promise<number> {
  return request
    .get<unknown, Result<number>>('/notify/unread-count', { params: { projectId } })
    .then((res) => res.data)
}
