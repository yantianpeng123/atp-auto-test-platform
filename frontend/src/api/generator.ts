/**
 * 数据生成器 - 接口层（对接后端 /api/base/generator/**）
 *
 * 曾为「前端先行」的 mock 实现，阶段 2 后端落地后已全部替换为真实 HTTP 调用，
 * 客户端生成运行时（genValue / genFromTemplate）随之移除：
 * 试生成一律走 `/preview`，保证列表里展示的示例值与用例执行时的真实生成结果同源。
 */
import request from './request'
import type {
  DataGeneratorInfo,
  DataGeneratorQuery,
  DataGeneratorSaveParams,
  GeneratorFunctionInfo,
  GeneratorPreviewParams,
  PageResult,
  Result
} from './types'

/** 分页查询（按项目 / 名称 / 类型过滤） */
export function getGeneratorList(query: DataGeneratorQuery): Promise<PageResult<DataGeneratorInfo>> {
  return request
    .get<unknown, Result<PageResult<DataGeneratorInfo>>>('/base/generator/list', { params: query })
    .then((res) => res.data)
}

/** 生成器详情 */
export function getGeneratorDetail(id: number): Promise<DataGeneratorInfo> {
  return request.get<unknown, Result<DataGeneratorInfo>>(`/base/generator/${id}`).then((res) => res.data)
}

/** 新增生成器，返回保存后的完整信息 */
export function createGenerator(data: DataGeneratorSaveParams): Promise<DataGeneratorInfo> {
  return request.post<unknown, Result<DataGeneratorInfo>>('/base/generator', data).then((res) => res.data)
}

/** 编辑生成器，返回保存后的完整信息 */
export function updateGenerator(data: DataGeneratorSaveParams): Promise<DataGeneratorInfo> {
  return request.put<unknown, Result<DataGeneratorInfo>>('/base/generator', data).then((res) => res.data)
}

/** 删除生成器（后端逻辑删除） */
export function deleteGenerator(id: number): Promise<void> {
  return request.delete<unknown, Result<null>>(`/base/generator/${id}`).then(() => undefined)
}

/** 可用生成函数清单（前端语法提示） */
export function getGeneratorFunctions(): Promise<GeneratorFunctionInfo[]> {
  return request
    .get<unknown, Result<GeneratorFunctionInfo[]>>('/base/generator/functions')
    .then((res) => res.data)
}

/** 试生成，返回 { result } */
export function previewGenerator(payload: GeneratorPreviewParams): Promise<{ result: string }> {
  return request
    .post<unknown, Result<{ result: string }>>('/base/generator/preview', payload)
    .then((res) => res.data)
}
