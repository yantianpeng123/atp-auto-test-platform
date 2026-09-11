/**
 * 与后端 VO / DTO 对齐的类型定义
 */

/** 统一响应体 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 用户信息出参 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  role: 'ADMIN' | 'TESTER' | 'VIEWER'
  status: number
  lastLoginTime: string
  createTime: string
}

/** 登录返回 */
export interface LoginData {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

/** 图形验证码 */
export interface CaptchaData {
  captchaKey: string
  imageBase64: string
  expireSeconds: number
}

/** 登录入参 */
export interface LoginParams {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}

/** 注册入参 */
export interface RegisterParams {
  username: string
  password: string
  confirmPassword: string
  email: string
  nickname?: string
  phone?: string
  captchaKey?: string
  captchaCode?: string
}

/**
 * 基础数据管理（工程版本信息）
 */

/** 下拉选项项（工程名称 / 版本名称 / 模块名称） */
export interface OptionItem {
  id: number
  name: string
}

/** 工程版本信息出参（模块 → 版本 → 工程 联查结果） */
export interface VersionInfo {
  moduleId: number
  moduleName: string
  versionId: number
  versionName: string
  applicationId: number
  applicationName: string
  updateTime: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 工程版本信息查询入参 */
export interface VersionInfoQuery {
  projectId?: number
  applicationId?: number
  versionId?: number
  moduleId?: number
  page?: number
  size?: number
}

/** 新增工程入参 */
export interface ApplicationCreateParams {
  projectId: number
  name: string
  description?: string
}

/** 新增版本入参 */
export interface VersionCreateParams {
  applicationId: number
  name: string
  description?: string
}

/** 新增模块入参 */
export interface ModuleCreateParams {
  versionId: number
  names: string[]
  description?: string
}

/**
 * 项目
 */

/** 项目出参 */
export interface Project {
  id: number
  name: string
  team: string | null
  ownerId: number | null
}

/** 新增项目入参 */
export interface ProjectCreateParams {
  name: string
  team?: string
}

/**
 * 接口定义
 */

/** 接口列表出参（接口 → 模块 → 版本 → 工程 联查结果） */
export interface ApiInfo {
  id: number
  name: string
  method: string
  path: string
  headers: string | null
  body: string | null
  description: string | null
  moduleId: number
  moduleName: string
  versionId: number
  versionName: string
  applicationId: number
  applicationName: string
  updateTime: string
  sourceFlag: string
}

/** 接口列表查询入参 */
export interface ApiQuery {
  projectId?: number
  applicationId?: number
  versionId?: number
  moduleId?: number
  name?: string
  path?: string
  page?: number
  size?: number
}

/** 新增接口入参 */
export interface ApiCreateParams {
  moduleId: number
  name: string
  method: string
  path: string
  headers?: string
  body?: string
  description?: string
}

/** 编辑接口入参 */
export interface ApiUpdateParams {
  id: number
  name: string
  method: string
  path: string
  headers?: string
  body?: string
  description?: string
}

/**
 * 测试环境
 */

/** 环境出参（headers / dbConfig 为 JSON 字符串，前端自行解析展示） */
export interface EnvInfo {
  id: number
  projectId: number
  name: string
  baseUrl: string | null
  headers: string | null
  dbConfig: string | null
  createTime: string
  updateTime: string
}

/** 环境查询入参 */
export interface EnvQuery {
  projectId?: number
  name?: string
  page?: number
  size?: number
}

/** 新增环境入参 */
export interface EnvCreateParams {
  projectId: number
  name: string
  baseUrl?: string
  headers?: string
  dbConfig?: string
}

/** 编辑环境入参 */
export interface EnvUpdateParams {
  id: number
  name: string
  baseUrl?: string
  headers?: string
  dbConfig?: string
}

/** 数据库配置（dbConfig 解析后的结构） */
export interface DbConfig {
  host: string
  port: string
  dbName: string
  username: string
  password: string
}

/**
 * 测试用例
 */

/** 用例出参（含关联接口的简要信息 + 步骤列表） */
export interface CaseInfo {
  id: number
  projectId: number
  applicationId: number
  versionId: number
  moduleId: number
  applicationName: string | null
  versionName: string | null
  moduleName: string | null
  apiId: number | null
  apiName: string | null
  apiMethod: string | null
  apiPath: string | null
  name: string
  level: number
  request: string | null
  assertions: string | null
  setupScript: string | null
  dataset: string | null
  status: number
  createTime: string
  updateTime: string
  steps: CaseStepInfo[] | null
}

/** 用例步骤出参 */
export interface CaseStepInfo {
  id: number
  caseId: number
  apiId: number
  apiName: string | null
  apiMethod: string | null
  apiPath: string | null
  sortOrder: number
  stepName: string | null
  requestOverride: string | null
  assertions: string | null
  responseVar: string | null
}

/** 步骤入参（嵌套在用例创建/编辑中） */
export interface StepParams {
  apiId: number
  sortOrder?: number
  stepName?: string
  requestOverride?: string
  assertions?: string
  responseVar?: string
}

/** 断言规则项 */
export interface AssertionItem {
  type: 'status' | 'jsonPath' | 'header' | 'body' | 'time'
  path?: string
  operator?: 'eq' | 'notEq' | 'contains' | 'exists'
  expected?: string
}

/** 用例查询入参 */
export interface CaseQuery {
  projectId?: number
  apiId?: number
  name?: string
  level?: number
  status?: number
  page?: number
  size?: number
}

/** 新增用例入参（caseName 发送时映射为后端 name 字段） */
export interface CaseCreateParams {
  projectId: number
  applicationId: number
  versionId: number
  moduleId: number
  apiId?: number
  caseName: string
  level?: number
  request?: string
  assertions?: string
  setupScript?: string
  dataset?: string
  status?: number
  steps?: StepParams[]
}

/** 编辑用例入参（caseName 发送时映射为后端 name 字段） */
export interface CaseUpdateParams {
  id: number
  applicationId: number
  versionId: number
  moduleId: number
  apiId?: number
  caseName: string
  level?: number
  request?: string
  assertions?: string
  setupScript?: string
  dataset?: string
  status?: number
  steps?: StepParams[]
}

/**
 * 用例数据源（参数化数据集）
 */

/** 数据源字段定义（keys 数组元素） */
export interface DatasetField {
  key: string
  desc: string
}

/** 数据项出参 */
export interface DatasetItemInfo {
  id: number
  templateId: number
  data: string | null
  sortOrder: number
}

/** 数据源出参（模板，详情时含 items） */
export interface CaseDatasetInfo {
  id: number
  caseId: number
  /** 数据源名称 */
  name: string
  /** 用例名称（联查 tb_test_case.name） */
  caseName: string | null
  /** 添加人 */
  creatorName: string | null
  /** 字段定义（JSON 数组字符串，如 [{"key":"username","desc":"用户名"}]） */
  keys: string | null
  createTime: string
  updateTime: string
  /** 数据项（仅详情返回） */
  items?: DatasetItemInfo[]
}

/** 数据源查询入参 */
export interface CaseDatasetQuery {
  caseId?: number
  name?: string
  page?: number
  size?: number
}

/** 数据项入参 */
export interface DatasetItemParams {
  data: string
  sortOrder?: number
}

/** 数据源新增/编辑入参（统一，编辑时带 id） */
export interface CaseDatasetSaveParams {
  id?: number
  caseId: number
  name: string
  creatorName?: string
  keys?: string
  items?: DatasetItemParams[]
}

/**
 * 用例执行
 */

/** 断言执行结果 */
export interface AssertionResultItem {
  type: string
  path: string | null
  operator: string | null
  expected: string | null
  actual: string | null
  passed: boolean
  message: string | null
}

/** 步骤执行明细 */
export interface StepExecuteResult {
  stepId: number
  stepName: string | null
  sortOrder: number | null
  method: string | null
  url: string | null
  requestHeaders: string | null
  requestBody: string | null
  statusCode: number | null
  responseHeaders: string | null
  responseBody: string | null
  assertResults: AssertionResultItem[]
  status: 'PASSED' | 'FAILED' | 'ERROR'
  errorMsg: string | null
  durationMs: number
}

/** 单轮执行结果 */
export interface RoundExecuteResult {
  roundIndex: number
  params: Record<string, unknown>
  passedSteps: number
  failedSteps: number
  status: 'SUCCESS' | 'FAILED'
  durationMs: number
  steps: StepExecuteResult[]
}

/** 用例执行汇总结果 */
export interface CaseExecuteResult {
  caseId: number
  caseName: string
  envId: number
  totalRounds: number
  passedRounds: number
  failedRounds: number
  status: 'SUCCESS' | 'FAILED'
  durationMs: number
  rounds: RoundExecuteResult[]
}
