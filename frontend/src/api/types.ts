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
 * 项目级 RBAC（成员与角色）
 */

/** 项目内角色（与后端 tb_project_member.role 对齐） */
export type ProjectRole = 'OWNER' | 'MAINTAINER' | 'DEVELOPER' | 'VIEWER'

/** 项目成员出参 */
export interface ProjectMember {
  id: number
  projectId: number
  userId: number
  username: string
  nickname: string
  /** 项目内角色 */
  role: ProjectRole
  /** 邀请人昵称（展示用，可为空） */
  inviterName?: string | null
  createTime: string
}

/** 项目成员查询入参 */
export interface ProjectMemberQuery {
  projectId?: number
  page?: number
  size?: number
}

/** 新增/邀请成员入参 */
export interface AddMemberParams {
  projectId: number
  /** 被邀请人用户名 */
  username: string
  role: ProjectRole
}

/** 修改成员角色入参 */
export interface UpdateMemberRoleParams {
  id: number
  role: ProjectRole
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
  /** 步骤阶段：pre-前置 / main-主步骤 / post-后置 */
  phase?: string
  /** 步骤类型：1-单接口 2-组合组件 3-生成变量 */
  stepType?: number
  /** 组合组件ID（stepType=2 时引用） */
  componentId?: number
  /** 生成变量步骤关联的数据生成器ID（stepType=3） */
  generatorId?: number | null
  /** 生成变量步骤关联生成器名称（展示用） */
  generatorName?: string | null
  /** 生成变量步骤的输出变量名（stepType=3） */
  variableName?: string | null
  /** 生成变量步骤：每次执行是否重新生成（stepType=3） */
  regenEachRun?: number
  apiName: string | null
  apiMethod: string | null
  apiPath: string | null
  sortOrder: number
  stepName: string | null
  requestOverride: string | null
  assertions: string | null
  responseVar: string | null
  isDisabled?: number
  promoteGlobal?: number
  continueOnFail?: number
  description?: string | null
}

/** 步骤入参（嵌套在用例创建/编辑中） */
export interface StepParams {
  apiId?: number
  /** 步骤阶段 */
  phase?: string
  /** 步骤类型 */
  stepType?: number
  /** 组合组件ID */
  componentId?: number
  sortOrder?: number
  stepName?: string
  requestOverride?: string
  assertions?: string
  responseVar?: string
  isDisabled?: number
  promoteGlobal?: number
  continueOnFail?: number
  description?: string
  /** 生成变量步骤（stepType=3）：关联的数据生成器 */
  generatorId?: number
  /** 生成变量步骤的输出变量名（供后续步骤 ${varName} 引用） */
  variableName?: string
  /** 生成变量步骤：每次执行是否重新生成，1-是 0-否 */
  regenEachRun?: number
}

/** 组合组件出参（含子步骤） */
export interface ApiComponentInfo {
  id: number
  projectId: number
  moduleId: number | null
  name: string
  description: string | null
  createBy?: number | null
  createTime?: string
  updateTime?: string
  /** 子步骤（详情返回；列表为 null） */
  steps?: ComponentStepInfo[] | null
}

/** 组合组件步骤出参（复用 CaseStepVO 结构，详情接口返回） */
export interface ComponentStepInfo {
  id: number
  stepType?: number
  apiId?: number | null
  /** 嵌套组件ID（stepType=2 时引用；后端经 CaseStepVO.componentId 透传） */
  componentId?: number | null
  /** 生成变量步骤（stepType=3）：关联的数据生成器 */
  generatorId?: number | null
  /** 生成变量步骤关联生成器名称（展示用） */
  generatorName?: string | null
  /** 生成变量步骤的输出变量名（stepType=3） */
  variableName?: string | null
  /** 生成变量步骤：每次执行是否重新生成，1-是 0-否 */
  regenEachRun?: number
  sortOrder: number
  stepName: string | null
  requestOverride: string | null
  assertions: string | null
  responseVar: string | null
  isDisabled?: number
  continueOnFail?: number
  description?: string | null
  apiName?: string | null
  apiMethod?: string | null
  apiPath?: string | null
}

/** 组合组件查询入参 */
export interface ApiComponentQuery {
  projectId?: number
  moduleId?: number
  name?: string
  page?: number
  size?: number
}

/** 组合组件新增/编辑入参 */
export interface ApiComponentSaveParams {
  id?: number
  projectId: number
  moduleId?: number | null
  name: string
  description?: string
  steps?: ComponentStepSaveParams[]
}

/** 组合组件步骤入参 */
export interface ComponentStepSaveParams {
  id?: number
  stepType?: number
  apiId?: number | null
  childComponentId?: number | null
  sortOrder?: number
  stepName?: string
  requestOverride?: string
  assertions?: string
  responseVar?: string
  isDisabled?: number
  continueOnFail?: number
  description?: string
}

/** 用例步骤扩展（前置/后置），用于扩展表格展示 */
export interface CaseExtensionStep {
  _uid: number
  phase: string
  stepType: number
  componentId?: number | null
  /** 组合组件名称（UI 展示） */
  componentName?: string
  stepName?: string
  responseVar?: string
  isDisabled: number
  promoteGlobal: number
  continueOnFail: number
  description?: string
  /** 生成变量步骤（stepType=3） */
  generatorId?: number | null
  /** 生成变量步骤关联的生成器名称（UI 展示） */
  generatorName?: string
  /** 生成变量步骤的输出变量名 */
  variableName?: string
  /** 生成变量步骤：每次执行是否重新生成 */
  regenEachRun?: number
}

/* ============ 数据生成器（前端先行，后端待补） ============ */

/** 生成器类型 */
export type GeneratorType =
  | 'RANDOM'
  | 'PHONE'
  | 'IDCARD'
  | 'NAME'
  | 'ENUM'
  | 'TIMESTAMP'
  | 'UUID'
  | 'CUSTOM'

/** 数据生成器出参 */
export interface DataGeneratorInfo {
  id: number
  projectId: number
  name: string
  type: GeneratorType
  /** 结构化参数（随 type）或 CUSTOM 的 template；前端以 Record 透传 */
  params: Record<string, unknown> | null
  description?: string | null
  createTime?: string
}

/** 数据生成器查询入参 */
export interface DataGeneratorQuery {
  projectId?: number
  name?: string
  type?: GeneratorType
  page?: number
  size?: number
}

/** 数据生成器新增/编辑入参 */
export interface DataGeneratorSaveParams {
  id?: number
  projectId: number
  name: string
  type: GeneratorType
  params: Record<string, unknown>
  description?: string
}

/** 生成器函数帮助信息（/functions 返回） */
export interface GeneratorFunctionInfo {
  name: string
  args: string
  desc: string
  example: string
}

/** 试生成入参 */
export interface GeneratorPreviewParams {
  projectId: number
  type: GeneratorType
  params: Record<string, unknown>
}

/** 生成变量步骤种子（组件页从生成器页"选择"带回，写入 stepType=3） */
export interface GeneratorStepSeed {
  generatorId: number
  generatorName: string
  variableName: string
  regenEachRun: boolean
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

/**
 * 通知中心（与后端 /api/notify/** 对齐，设计见 docs/phase5-features-design.md §3）
 */

/** 渠道类型：站内信 / 钉钉 Webhook / 163 邮件 */
export type NotifyChannelType = 'INAPP' | 'DINGTALK' | 'EMAIL_163'

/** 触发事件 */
export type NotifyEvent = 'EXEC_DONE' | 'EXEC_FAIL' | 'BATCH_DONE'

/** 通知渠道出参 */
export interface NotifyChannel {
  id: number
  projectId: number
  /** 渠道类型 */
  type: NotifyChannelType
  /** 渠道显示名（如「钉钉-交易群」） */
  name: string
  /** 是否启用 */
  enabled: boolean
  /** 渠道配置 JSON（结构见设计文档 §3.1）：DINGTALK=webhook/secret/atMobiles；EMAIL_163=host/port/username/authCode/from/ssl */
  config: Record<string, unknown> | null
  createTime: string
}

/** 新增/编辑渠道入参 */
export interface NotifyChannelSaveParams {
  projectId: number
  type: NotifyChannelType
  name: string
  enabled?: number
  config: Record<string, unknown>
}

/** 通知规则出参 */
export interface NotifyRule {
  id: number
  projectId: number
  /** 触发事件 */
  event: NotifyEvent
  /** 命中的渠道 id 列表（后端存 JSON 列） */
  channelIds: number[]
  /** 附加条件，如 { onlyFail: true } */
  condition: Record<string, unknown> | null
  enabled: boolean
  createTime: string
}

/** 新增规则入参 */
export interface NotifyRuleSaveParams {
  projectId: number
  event: NotifyEvent
  channelIds: number[]
  condition?: Record<string, unknown>
  enabled?: number
}

/** 发送日志出参 */
export interface NotifyLog {
  id: number
  projectId: number
  event: NotifyEvent
  channelType: NotifyChannelType
  /** 发送目标（webhook url / 收件人 / 站内信用户） */
  target: string
  /** SUCCESS / FAILED */
  status: 'SUCCESS' | 'FAILED'
  /** 发送内容摘要 */
  content: string
  /** 失败原因 */
  error: string | null
  createTime: string
}

/** 站内信收件箱出参 */
export interface NotifyMessage {
  id: number
  userId: number
  projectId: number
  title: string
  content: string
  /** 是否已读 */
  Isread: boolean
  /** 点击跳转的报告/详情 URL */
  linkUrl: string | null
  createTime: string
}

/**
 * CI 集成配置（与后端 /api/ci/config/** 对齐）
 */

/** CI 配置出参（token 仅在「新建 / 重新生成」时返回一次明文，之后查询为 null） */
export interface CiConfig {
  /** 配置ID（未配置时为 null） */
  id: number | null
  projectId: number
  /** 默认执行环境ID */
  defaultEnvId: number | null
  /** 默认批次ID */
  defaultBatchId: number | null
  /** 批次完成回调地址（可选） */
  callbackUrl: string | null
  /** 1 启用 / 0 停用（未配置时为 null） */
  enabled: number | null
  /** 一次性明文令牌（仅新建/重新生成时非空） */
  token: string | null
  /** 令牌后缀提示（如 ...a1b2），便于在无明文时辨认 */
  tokenHint: string | null
  createTime: string | null
  updateTime: string | null
}

/** CI 配置保存入参（新增/更新；令牌由后端生成，不在请求体中传入） */
export interface CiConfigSaveParams {
  /** 配置ID（更新时传入，新增时留空） */
  id?: number
  /** 项目ID（必填） */
  projectId: number
  /** 默认执行环境ID */
  defaultEnvId?: number | null
  /** 默认批次ID */
  defaultBatchId?: number | null
  /** 批次完成回调地址（可选） */
  callbackUrl?: string | null
  /** 是否启用：true 启用（新建时由后端强制置 1） */
  enabled?: boolean | null
}
