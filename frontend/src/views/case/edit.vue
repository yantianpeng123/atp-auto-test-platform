<template>
  <div class="case-edit-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <!-- 第一行：用例名称 + 工程/版本/模块 -->
      <div class="top-bar-row">
        <div class="top-bar-fields">
          <el-form-item label="工程" prop="applicationId" class="bar-form-item" required>
            <el-select
              v-model="form.applicationId"
              placeholder="请选择工程"
              filterable
              class="bar-select"
              @change="onApplicationChange"
            >
              <el-option v-for="item in appOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="版本" prop="versionId" class="bar-form-item" required>
            <el-select
              v-model="form.versionId"
              placeholder="请选择版本"
              filterable
              :disabled="!form.applicationId"
              class="bar-select"
              @change="onVersionChange"
            >
              <el-option v-for="item in versionOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模块" prop="moduleId" class="bar-form-item" required>
            <el-select
              v-model="form.moduleId"
              placeholder="请选择模块"
              filterable
              :disabled="!form.versionId"
              class="bar-select"
              @change="onModuleChange"
            >
              <el-option v-for="item in moduleOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="用例名称" prop="caseName" class="bar-form-item" required>
            <el-input
              v-model="form.caseName"
              placeholder="请输入用例名称"
              clearable
              class="bar-input-name"
            />
          </el-form-item>
        </div>
      </div>
      <!-- 第二行：优先级 + 状态 + 操作按钮 -->
      <div class="top-bar-row">
        <div class="top-bar-fields">
          <el-form-item label="优先级" prop="level" class="bar-form-item" required>
            <el-select v-model="form.level" class="bar-select-sm">
              <el-option :value="1" label="P0（核心）" />
              <el-option :value="2" label="P1（重要）" />
              <el-option :value="3" label="P2（一般）" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" class="bar-form-item">
            <el-switch v-model="form.statusBool" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
        <div class="top-bar-right">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="edit-body">
      <el-form
        ref="formEl"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="left"
        class="edit-form"
      >
        <!-- 左侧：步骤列表 -->
        <div class="left-panel">
          <el-card shadow="never" class="section-card steps-section">
            <template #header>
              <div class="steps-card-header">
                
                <div class="steps-header-actions">
                  <el-button type="primary" size="small" :icon="Plus" @click="openApiDialog">添加步骤</el-button>
                </div>
                <div class="debug-bar">
                  <el-select v-model="debugEnvId" placeholder="选择环境" size="small" class="debug-env-select" clearable>
                    <el-option v-for="env in envOptions" :key="env.id" :label="env.name" :value="env.id" />
                  </el-select>
                  <el-button
                    type="warning"
                    size="small"
                    :loading="debugging"
                    :disabled="!debugEnvId"
                    @click="handleDebug"
                  >
                    调试运行
                  </el-button>
                </div>
              </div>
            </template>

            <div v-if="form.steps.length === 0" class="steps-empty">
              <el-empty description="暂无步骤，点击添加" :image-size="80" />
            </div>

            <div v-else class="steps-list">
              <div
                v-for="(step, index) in form.steps"
                :key="index"
                class="step-card"
                :class="{ 'step-active': activeStepIndex === index }"
                @click="selectStep(index)"
              >
                <!-- 步骤头部 -->
                <div class="step-header">
                  <span class="step-order">{{ index + 1 }}</span>

                  <!-- 已选接口信息展示 -->
                  <div v-if="getStepApiInfo(step)" class="step-api-badge">
                    <el-tag size="small" :type="methodTagType(getStepApiInfo(step)!.method)" class="method-tag">
                      {{ getStepApiInfo(step)!.method }}
                    </el-tag>
                    <span class="step-api-path">{{ getStepApiInfo(step)!.path }}</span>
                  </div>
                  <span v-else class="step-api-missing">未选择接口</span>

                  <div class="step-actions" @click.stop>
                    <el-tooltip content="上移" placement="top">
                      <el-button
                        :icon="Top"
                        link
                        size="small"
                        :disabled="index === 0"
                        @click="moveStep(index, -1)"
                      />
                    </el-tooltip>
                    <el-tooltip content="下移" placement="top">
                      <el-button
                        :icon="Bottom"
                        link
                        size="small"
                        :disabled="index === form.steps.length - 1"
                        @click="moveStep(index, 1)"
                      />
                    </el-tooltip>
                    <el-tooltip content="删除步骤" placement="top">
                      <el-button
                        :icon="Delete"
                        link
                        type="danger"
                        size="small"
                        @click="removeStep(index)"
                      />
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 右侧：当前接口 + 前置扩展 -->
        <div class="right-panel">
          <!-- 步骤详情 -->
          <el-card v-if="activeStep" shadow="never" class="section-card detail-section">
            <template #header>
              <div class="detail-card-header">
                <span class="section-title">当前接口:</span>
                <el-tag size="small" :type="methodTagType(getStepApiInfo(activeStep)!.method)" class="method-tag">
                    {{ getStepApiInfo(activeStep)!.method }}
                  </el-tag>
                <span v-if="getStepApiInfo(activeStep)" class="detail-api-path">{{ getStepApiInfo(activeStep)!.path }}</span>
                <el-button type="primary" link size="small" @click="openApiDialogForStep(activeStepIndex!)">更换接口</el-button>
                <el-button type="primary" link size="small" @click="toggleAssertions">断言</el-button>
                <el-button type="primary" link size="small" @click="toggleSetupScript">前置扩展</el-button>
                <el-button type="primary" link size="small" @click="openDatasetDialog">数据源选择</el-button>
              </div>
            </template>

            <!-- 前置脚本 -->
            <div v-if="showSetupScript" class="step-form-item">
              <div class="field-label">前置扩展</div>
              <el-input
                v-model="form.setupScript"
                type="textarea"
                :rows="4"
                placeholder="用例执行前运行的脚本（如参数预处理、数据准备），执行引擎支持"
                class="code-textarea"
              />
            </div>

            <!-- 响应变量名 -->
            <div class="step-form-item">
              <div class="field-label">响应变量名</div>
              <el-input
                v-model="activeStep.responseVar"
                placeholder="给该接口响应数据命名，供后续步骤引用（如 loginResp），留空则不保存"
                clearable
              />
            </div>

            <!-- 请求覆盖（拆分为请求头 / 请求参数） -->
            <div  class="request-override-section">
              <el-tabs v-model="requestTab" class="request-tabs">
                <el-tab-pane label="请求头" name="headers">
                  <el-input
                    v-model="activeStep.requestHeaders"
                    type="textarea"
                    @blur="formatJsonField('requestHeaders')"
                    :rows="4"
                    placeholder='JSON格式，覆盖接口默认请求头&#10;可用${varName}引用上一步提取的变量&#10;如：{"Authorization":"Bearer ${token}","Content-Type":"application/json"}'
                    class="code-textarea"
                  />
                </el-tab-pane>
                <el-tab-pane label="请求参数" name="params">
                  <el-input
                    v-model="activeStep.requestParams"
                    type="textarea"
                    @blur="formatJsonField('requestParams')"
                    :rows="4"
                    placeholder='JSON格式，覆盖接口默认请求参数(body)&#10;可用${varName}引用上一步提取的变量&#10;如：{"userId":"${userId}","page":1}'
                    class="code-textarea"
                  />
                </el-tab-pane>
              </el-tabs>
            </div>

            <!-- 断言规则 -->
            <div v-if="showAssertions" class="step-form-item">
              <div class="field-label">断言规则</div>
              <div class="assertion-list">
                <div v-for="(a, idx) in activeStep.assertions" :key="idx" class="assertion-row">
                  <el-select v-model="a.type" class="assertion-type">
                    <el-option label="状态码" value="status" />
                    <el-option label="JSONPath" value="jsonPath" />
                    <el-option label="响应头" value="header" />
                    <el-option label="响应体包含" value="body" />
                  </el-select>

                  <el-input
                    v-if="a.type === 'jsonPath'"
                    v-model="a.path"
                    placeholder="JSONPath，如 $.code"
                    class="assertion-path"
                  />
                  <el-input
                    v-else-if="a.type === 'header'"
                    v-model="a.path"
                    placeholder="响应头名，如 Content-Type"
                    class="assertion-path"
                  />

                  <el-select
                    v-if="a.type === 'jsonPath' || a.type === 'header'"
                    v-model="a.operator"
                    placeholder="操作符"
                    class="assertion-operator"
                  >
                    <el-option label="等于" value="eq" />
                    <el-option label="不等于" value="notEq" />
                    <el-option label="包含" value="contains" />
                    <el-option label="存在" value="exists" />
                  </el-select>

                  <el-input
                    v-if="showExpected(a)"
                    v-model="a.expected"
                    :placeholder="assertionExpectedPlaceholder(a)"
                    class="assertion-expected"
                  />

                  <el-button link type="danger" :icon="Delete" @click="removeAssertion(idx)" />
                </div>
                <el-button type="primary" link :icon="Plus" @click="addAssertion">添加断言</el-button>
              </div>
            </div>
          </el-card>

          <!-- 未选中步骤时的占位 -->
          <el-card v-else shadow="never" class="section-card detail-section detail-empty">
            <el-empty description="请点击左侧步骤查看详情" :image-size="100" />
          </el-card>

        </div>
      </el-form>
    </div>

    <!-- 接口选择弹框 -->
    <el-dialog
      v-model="apiDialogVisible"
      title="选择接口"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
    >
      <div class="api-dialog-body">
        <el-form label-width="80px">
          <el-form-item label="选择接口">
            <el-select
              v-model="apiDialogSelectedId"
              placeholder="请选择接口（支持搜索）"
              filterable
              class="api-dialog-select"
              value-key="id"
            >
              <el-option
                v-for="api in apiOptions"
                :key="api.id"
                :label="`${api.method} ${api.path} — ${api.name}`"
                :value="api.id"
              >
                <div class="api-select-option">
                  <el-tag size="small" :type="methodTagType(api.method)" class="method-tag">
                    {{ api.method }}
                  </el-tag>
                  <span class="api-path">{{ api.path }}</span>
                  <span class="api-option-name">{{ api.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <!-- 选中接口的详细信息预览 -->
          <div v-if="apiDialogPreview" class="api-dialog-preview">
            <div class="preview-row">
              <span class="preview-label">请求方法</span>
              <el-tag size="small" :type="methodTagType(apiDialogPreview.method)">{{ apiDialogPreview.method }}</el-tag>
            </div>
            <div class="preview-row">
              <span class="preview-label">接口路径</span>
              <span class="preview-value">{{ apiDialogPreview.path }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">接口名称</span>
              <span class="preview-value">{{ apiDialogPreview.name }}</span>
            </div>
            <div v-if="apiDialogPreview.description" class="preview-row">
              <span class="preview-label">描述</span>
              <span class="preview-value text-muted">{{ apiDialogPreview.description }}</span>
            </div>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="apiDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!apiDialogSelectedId" @click="confirmApiSelect">确定</el-button>
      </template>
    </el-dialog>

    <!-- 数据源选择弹窗（列表 + 数据项 / 编辑 / 删除） -->
    <DatasetSelectDialog
      v-model="datasetDialogVisible"
      :case-id="editingId || undefined"
      :case-name="form.caseName || undefined"
    />

    <!-- 调试结果抽屉 -->
    <el-drawer v-model="debugVisible" title="调试结果" size="60%">
      <div v-if="debugResult" class="debug-result">
        <div class="debug-summary">
          <el-tag :type="debugResult.status === 'SUCCESS' ? 'success' : 'danger'" size="large">
            {{ debugResult.status === 'SUCCESS' ? '执行成功' : '执行失败' }}
          </el-tag>
          <span class="debug-meta">
            共 {{ debugResult.totalRounds }} 轮 · 通过 {{ debugResult.passedRounds }} · 失败
            {{ debugResult.failedRounds }} · 耗时 {{ debugResult.durationMs }}ms
          </span>
        </div>

        <div v-for="(round, ri) in debugResult.rounds" :key="round.roundIndex ?? ri" class="debug-round">
          <div class="debug-round-header">
            <el-tag size="small" :type="round.status === 'SUCCESS' ? 'success' : 'danger'">
              第 {{ round.roundIndex }} 轮
            </el-tag>
            <span class="debug-step-time">{{ round.durationMs }}ms</span>
          </div>
          <pre v-if="round.params && Object.keys(round.params).length" class="debug-pre debug-round-params">{{ formatJson(JSON.stringify(round.params)) }}</pre>

          <div v-for="(step, idx) in round.steps" :key="step.stepId ?? idx" class="debug-step">
            <div class="debug-step-header">
              <el-tag size="small" :type="stepStatusType(step.status)">{{ step.status }}</el-tag>
              <span class="debug-step-title">{{ idx + 1 }}. {{ step.stepName || '步骤' + (idx + 1) }}</span>
              <span class="debug-step-api">{{ step.method }} {{ step.url }}</span>
              <span class="debug-step-time">{{ step.durationMs }}ms</span>
            </div>
            <div v-if="step.errorMsg" class="debug-error">{{ step.errorMsg }}</div>

            <el-collapse v-if="step.statusCode != null">
              <el-collapse-item title="请求详情" :name="'req' + ri + '-' + idx">
                <div v-if="step.requestHeaders" class="debug-kv">
                  <div class="debug-kv-label">请求头</div>
                  <pre class="debug-pre">{{ formatJson(step.requestHeaders) }}</pre>
                </div>
                <div v-if="step.requestBody" class="debug-kv">
                  <div class="debug-kv-label">请求体</div>
                  <pre class="debug-pre">{{ formatJson(step.requestBody) }}</pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="响应详情" :name="'resp' + ri + '-' + idx">
                <div class="debug-kv">
                  <div class="debug-kv-label">状态码</div>
                  <div class="debug-status">{{ step.statusCode }}</div>
                </div>
                <div v-if="step.responseHeaders" class="debug-kv">
                  <div class="debug-kv-label">响应头</div>
                  <pre class="debug-pre">{{ formatJson(step.responseHeaders) }}</pre>
                </div>
                <div v-if="step.responseBody" class="debug-kv">
                  <div class="debug-kv-label">响应体</div>
                  <pre class="debug-pre">{{ formatJson(step.responseBody) }}</pre>
                </div>
              </el-collapse-item>
              <el-collapse-item v-if="step.assertResults && step.assertResults.length" title="断言结果" :name="'assert' + ri + '-' + idx">
                <div v-for="(a, ai) in step.assertResults" :key="ai" class="debug-assert-row">
                  <el-tag size="small" :type="a.passed ? 'success' : 'danger'">{{ a.passed ? '通过' : '失败' }}</el-tag>
                  <span>{{ a.message }}</span>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bottom,
  Delete,
  Plus,
  Top
} from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createCase, getCaseDetail, updateCase } from '@/api/case'
import { getApiList, getModuleOptions, getProjectOptions, getVersionOptions } from '@/api/base'
import { getEnvList } from '@/api/env'
import { executeCase } from '@/api/execute'
import DatasetSelectDialog from '@/views/dataset/components/DatasetSelectDialog.vue'
import type {
  ApiInfo,
  AssertionItem,
  CaseExecuteResult,
  EnvInfo,
  OptionItem,
  StepExecuteResult,
  StepParams
} from '@/api/types'
import { useProjectStore } from '@/stores/project'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const formEl = ref<FormInstance>()
const submitting = ref(false)
const apiOptions = ref<ApiInfo[]>([])
const editingId = ref<number | null>(null)

// 级联下拉选项
const appOptions = ref<OptionItem[]>([])
const versionOptions = ref<OptionItem[]>([])
const moduleOptions = ref<OptionItem[]>([])

const isEdit = computed(() => editingId.value !== null)

/** 当前激活的步骤索引 */
const activeStepIndex = ref<number | null>(null)

/** 请求覆盖 Tab 当前激活标签 */
const requestTab = ref('headers')

/** 是否显示断言规则 */
const showAssertions = ref(false)
const showSetupScript = ref(false)

/** 当前激活的步骤对象 */
const activeStep = computed(() => {
  if (activeStepIndex.value === null || activeStepIndex.value >= form.steps.length) return null
  return form.steps[activeStepIndex.value]
})

/** 步骤表单内部类型（含 UI 扩展字段） */
interface StepFormItem {
  apiId: number
  /** 接口名称（回显保留，避免依赖 apiOptions 查找） */
  apiName?: string
  /** 接口请求方法 */
  apiMethod?: string
  /** 接口路径 */
  apiPath?: string
  sortOrder?: number
  stepName?: string
  /** 响应变量名（为空则不保存该接口响应） */
  responseVar?: string
  requestOverride?: string
  /** 断言规则（结构化，提交时序列化为 JSON） */
  assertions: AssertionItem[]
  _expanded: boolean
  /** UI 拆分：请求头（从 requestOverride.headers 解析） */
  requestHeaders: string
  /** UI 拆分：请求参数（从 requestOverride.body/params 解析） */
  requestParams: string
}

const form = reactive({
  caseName: '',
  applicationId: null as number | null,
  versionId: null as number | null,
  moduleId: null as number | null,
  level: 2,
  statusBool: true,
  setupScript: '',
  steps: [] as StepFormItem[]
})

/** 数据源选择弹窗开关 */
const datasetDialogVisible = ref(false)

function openDatasetDialog() {
  datasetDialogVisible.value = true
}

/** 调试相关状态 */
const envOptions = ref<EnvInfo[]>([])
const debugEnvId = ref<number | null>(null)
const debugging = ref(false)
const debugVisible = ref(false)
const debugResult = ref<CaseExecuteResult | null>(null)

const rules: FormRules = {
  caseName: [{ required: true, message: '请输入用例名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
  versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  moduleId: [{ required: true, message: '请选择模块', trigger: 'change' }]
}

/* ---- 接口选择弹框状态 ---- */
const apiDialogVisible = ref(false)
const apiDialogSelectedId = ref<number | null>(null)
/** 记录弹框是为哪个步骤打开的（null = 新增步骤，数字 = 更换第 N 步的接口） */
const apiDialogTargetIndex = ref<number | null>(null)

/** 弹框中选中的接口详情（用于预览） */
const apiDialogPreview = computed(() => {
  if (!apiDialogSelectedId.value) return null
  return apiOptions.value.find((a) => a.id === apiDialogSelectedId.value) || null
})

/** 打开弹框 — 新增步骤 */
function openApiDialog() {
  apiDialogTargetIndex.value = null
  apiDialogSelectedId.value = null
  apiDialogVisible.value = true
}

/** 打开弹框 — 更换已有步骤的接口 */
function openApiDialogForStep(index: number) {
  apiDialogTargetIndex.value = index
  apiDialogSelectedId.value = null
  apiDialogVisible.value = true
}

/** 确认选择接口 */
function confirmApiSelect() {
  if (!apiDialogSelectedId.value) return
  const api = apiOptions.value.find((a) => a.id === apiDialogSelectedId.value)
  if (!api) return

  if (apiDialogTargetIndex.value !== null) {
    // 更换已有步骤的接口
    const step = form.steps[apiDialogTargetIndex.value]
    step.apiId = api.id
    step.apiName = api.name
    step.apiMethod = api.method
    step.apiPath = api.path
    if (!step.stepName) {
      step.stepName = api.name
    }
  } else {
    // 新增步骤
    form.steps.push({
      apiId: api.id,
      apiName: api.name,
      apiMethod: api.method,
      apiPath: api.path,
      sortOrder: form.steps.length + 1,
      stepName: api.name,
      requestOverride: '',
      requestHeaders: '',
      requestParams: '',
      assertions: [],
      _expanded: false
    })
  }
  // 选中新添加/更换的步骤
  const targetIdx = apiDialogTargetIndex.value !== null ? apiDialogTargetIndex.value : form.steps.length - 1
  activeStepIndex.value = targetIdx
  apiDialogVisible.value = false
}

/** 点击步骤选中 */
function selectStep(index: number) {
  activeStepIndex.value = index
}

/** 请求方法 -> 标签颜色 */
function methodTagType(method: string | null): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (!method) return 'info'
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    GET: 'success',
    POST: '',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'info'
  }
  return map[method.toUpperCase()] || 'info'
}

function goBack() {
  router.push('/case')
}

/* ---- 调试 ---- */
async function loadEnvOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    envOptions.value = []
    return
  }
  try {
    const result = await getEnvList({ projectId, page: 1, size: 200 })
    envOptions.value = result.records
    if (envOptions.value.length > 0 && debugEnvId.value === null) {
      debugEnvId.value = envOptions.value[0].id
    }
  } catch {
    envOptions.value = []
  }
}

function stepStatusType(status: StepExecuteResult['status']): 'success' | 'danger' | 'info' {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

/** JSON 字符串格式化为缩进展示；非 JSON 原样返回 */
function formatJson(text: string | null | undefined): string {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

/** 输入框失焦时，若内容为合法 JSON 则格式化为缩进 */
function formatJsonField(field: 'requestHeaders' | 'requestParams') {
  const step = activeStep.value
  if (!step) return
  const text = step[field]
  if (!text || !text.trim()) return
  try {
    step[field] = JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    // 非法 JSON 保持原样
  }
}

async function handleDebug() {
  if (editingId.value === null) {
    ElMessage.warning('请先保存用例，再进行调试')
    return
  }
  if (debugEnvId.value === null) {
    ElMessage.warning('请先选择调试环境')
    return
  }
  debugging.value = true
  try {
    debugResult.value = await executeCase(editingId.value, debugEnvId.value, true)
    debugVisible.value = true
  } catch {
    // 错误提示已由拦截器处理
  } finally {
    debugging.value = false
  }
}

/* ---- 级联下拉逻辑 ---- */
async function loadAppOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    appOptions.value = []
    return
  }
  try {
    appOptions.value = await getProjectOptions(projectId)
  } catch {
    appOptions.value = []
  }
}

async function loadVersionOptions(applicationId: number) {
  try {
    versionOptions.value = await getVersionOptions(applicationId)
  } catch {
    versionOptions.value = []
  }
}

async function loadModuleOptions(versionId: number) {
  try {
    moduleOptions.value = await getModuleOptions(versionId)
  } catch {
    moduleOptions.value = []
  }
}

/** 工程变更 → 重置版本和模块，加载版本选项 */
function onApplicationChange(appId: number) {
  form.versionId = null
  form.moduleId = null
  versionOptions.value = []
  moduleOptions.value = []
  if (appId) {
    loadVersionOptions(appId)
  }
  loadApiOptions()
}

/** 版本变更 → 重置模块，加载模块选项 */
function onVersionChange(versionId: number) {
  form.moduleId = null
  moduleOptions.value = []
  if (versionId) {
    loadModuleOptions(versionId)
  }
  loadApiOptions()
}

/** 模块变更 → 按模块过滤接口列表 */
function onModuleChange() {
  loadApiOptions()
}

async function loadApiOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    apiOptions.value = []
    return
  }
  try {
    const result = await getApiList({
      projectId,
      moduleId: form.moduleId || undefined,
      applicationId: form.applicationId || undefined,
      versionId: form.versionId || undefined,
      page: 1,
      size: 500
    })
    apiOptions.value = result.records
  } catch {
    apiOptions.value = []
  }
}

/** 删除步骤 */
function removeStep(index: number) {
  form.steps.splice(index, 1)
  form.steps.forEach((s, i) => {
    s.sortOrder = i + 1
  })
  // 调整激活步骤索引
  if (form.steps.length === 0) {
    activeStepIndex.value = null
  } else if (activeStepIndex.value !== null) {
    if (activeStepIndex.value >= form.steps.length) {
      activeStepIndex.value = form.steps.length - 1
    } else if (activeStepIndex.value > index) {
      activeStepIndex.value--
    } else if (activeStepIndex.value === index) {
      activeStepIndex.value = Math.min(index, form.steps.length - 1)
    }
  }
}

/** 移动步骤 */
function moveStep(index: number, direction: number) {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= form.steps.length) return
  const temp = form.steps[index]
  form.steps[index] = form.steps[targetIndex]
  form.steps[targetIndex] = temp
  form.steps.forEach((s, i) => {
    s.sortOrder = i + 1
  })
  // 如果移动的是当前激活步骤，更新索引
  if (activeStepIndex.value === index) {
    activeStepIndex.value = targetIndex
  } else if (activeStepIndex.value === targetIndex) {
    activeStepIndex.value = index
  }
}

/** 获取步骤对应的接口信息 */
function getStepApiInfo(step: StepFormItem): ApiInfo | undefined {
  if (step.apiMethod || step.apiPath) {
    return {
      id: step.apiId,
      name: step.apiName || '',
      method: step.apiMethod || '',
      path: step.apiPath || ''
    } as ApiInfo
  }
  return apiOptions.value.find((a) => a.id === step.apiId)
}

/** 将 requestOverride JSON 拆分为 headers 和 params */
function parseRequestOverride(override: string | undefined | null): { headers: string; params: string } {
  if (!override?.trim()) return { headers: '', params: '' }
  try {
    const obj = JSON.parse(override)
    const headers = obj.headers ? JSON.stringify(obj.headers, null, 2) : ''
    const params = obj.body ? JSON.stringify(obj.body, null, 2) : (obj.params ? JSON.stringify(obj.params, null, 2) : '')
    return { headers, params }
  } catch {
    return { headers: '', params: '' }
  }
}

/** 将 headers 和 params 合并为 requestOverride JSON */
function buildRequestOverride(headers: string, params: string): string {
  const obj: Record<string, unknown> = {}
  let hasContent = false
  if (headers?.trim()) {
    try { obj.headers = JSON.parse(headers); hasContent = true } catch { /* ignore */ }
  }
  if (params?.trim()) {
    try { obj.body = JSON.parse(params); hasContent = true } catch { /* ignore */ }
  }
  return hasContent ? JSON.stringify(obj) : ''
}

/** JSON 格式校验 */
function isValidJson(str: string): boolean {
  if (!str.trim()) return true
  try {
    JSON.parse(str)
    return true
  } catch {
    return false
  }
}

/** 断言序列化：结构化列表 → JSON 字符串 */
function serializeAssertions(list: AssertionItem[]): string | undefined {
  const valid = list.filter((a) => a.type)
  return valid.length > 0 ? JSON.stringify(valid) : undefined
}

/** 断言反序列化：JSON 字符串 → 结构化列表 */
function parseAssertions(json: string | null | undefined): AssertionItem[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

/** 添加断言 */
function addAssertion() {
  if (!activeStep.value) return
  activeStep.value.assertions.push({ type: 'status' })
}

/** 切换断言规则显示 */
function toggleAssertions() {
  showAssertions.value = !showAssertions.value
}

function toggleSetupScript(){
  showSetupScript.value=!showSetupScript.value
}
/** 删除断言 */
function removeAssertion(index: number) {
  if (!activeStep.value) return
  activeStep.value.assertions.splice(index, 1)
}

/** 是否显示期望值输入框 */
function showExpected(a: AssertionItem): boolean {
  if (a.type === 'jsonPath' || a.type === 'header') {
    return a.operator !== 'exists'
  }
  return true
}

/** 期望值占位提示 */
function assertionExpectedPlaceholder(a: AssertionItem): string {
  switch (a.type) {
    case 'status':
      return '期望状态码，如 200'
    case 'body':
      return '期望包含的文本'
    case 'time':
      return '最大耗时(ms)，如 2000'
    default:
      return '期望值'
  }
}

async function handleSubmit() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  // 步骤校验
  for (let i = 0; i < form.steps.length; i++) {
    const step = form.steps[i]
    if (!step.apiId) {
      ElMessage.error(`第 ${i + 1} 步未选择接口`)
      activeStepIndex.value = i
      return
    }
    if (step.requestHeaders?.trim() && !isValidJson(step.requestHeaders)) {
      ElMessage.error(`第 ${i + 1} 步请求头不是合法的 JSON`)
      activeStepIndex.value = i
      return
    }
    if (step.requestParams?.trim() && !isValidJson(step.requestParams)) {
      ElMessage.error(`第 ${i + 1} 步请求参数不是合法的 JSON`)
      activeStepIndex.value = i
      return
    }
  }

  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }

  submitting.value = true
  try {
    const stepsPayload: StepParams[] = form.steps.map((s, i) => ({
      apiId: s.apiId,
      sortOrder: i + 1,
      stepName: s.stepName?.trim() || undefined,
      requestOverride: buildRequestOverride(s.requestHeaders, s.requestParams) || undefined,
      assertions: serializeAssertions(s.assertions),
      responseVar: s.responseVar?.trim() || undefined
    }))

    const payload = {
      applicationId: form.applicationId!,
      versionId: form.versionId!,
      moduleId: form.moduleId!,
      caseName: form.caseName.trim(),
      level: form.level,
      setupScript: form.setupScript.trim() || undefined,
      status: form.statusBool ? 1 : 0,
      steps: stepsPayload.length > 0 ? stepsPayload : undefined
    }

    if (isEdit.value && editingId.value !== null) {
      await updateCase({ id: editingId.value, ...payload })
      ElMessage.success('用例修改成功')
    } else {
      await createCase({ projectId, ...payload })
      ElMessage.success('用例新增成功')
    }
    goBack()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载工程下拉
  await loadAppOptions()
  // 加载环境下拉（调试用）
  await loadEnvOptions()

  // 判断是编辑还是新增
  const caseId = route.query.id
  if (caseId) {
    editingId.value = Number(caseId)
    try {
      const detail = await getCaseDetail(editingId.value)
      form.caseName = detail.name
      form.applicationId = detail.applicationId
      form.versionId = detail.versionId
      form.moduleId = detail.moduleId
      form.level = detail.level
      form.statusBool = detail.status === 1
      form.setupScript = detail.setupScript || ''
      form.steps = (detail.steps || []).map((s) => {
        const { headers, params } = parseRequestOverride(s.requestOverride)
        return {
          apiId: s.apiId,
          apiName: s.apiName || undefined,
          apiMethod: s.apiMethod || undefined,
          apiPath: s.apiPath || undefined,
          sortOrder: s.sortOrder,
          stepName: s.stepName || '',
          requestOverride: s.requestOverride || '',
          responseVar: s.responseVar || undefined,
          requestHeaders: headers,
          requestParams: params,
          assertions: parseAssertions(s.assertions),
          _expanded: false
        }
      })

      // 默认选中第一个步骤
      if (form.steps.length > 0) {
        activeStepIndex.value = 0
      }

      // 级联加载版本和模块选项
      if (detail.applicationId) {
        await loadVersionOptions(detail.applicationId)
      }
      if (detail.versionId) {
        await loadModuleOptions(detail.versionId)
      }
    } catch {
      ElMessage.error('加载用例详情失败')
    }
  }

  // 加载接口选项
  await loadApiOptions()
})
</script>

<style scoped>
.case-edit-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ---------- 顶部操作栏 ---------- */
.top-bar {
  display: flex;
  flex-direction: column;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  gap: 10px;
}

.top-bar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-bar-fields {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-right: 16px;
  flex-shrink: 0;
}

/* 顶部栏表单项样式 */
.bar-form-item {
  margin-bottom: 0 !important;
}

.bar-form-item :deep(.el-form-item__label) {
  font-size: 13px;
  color: #374151;
  padding-right: 6px;
}

.bar-form-item :deep(.el-form-item__content) {
  flex: 1;
}

.bar-select {
  width: 160px;
}

.bar-input-name {
  width: 220px;
}

.bar-select-sm {
  width: 130px;
}

/* ---------- 主体 ---------- */
.edit-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f5f7fa;
}

.edit-form {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.left-panel {
  width: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  border-radius: 8px;
}

.section-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f1f3;
  background: #fafbfc;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* ---------- 步骤列表 ---------- */
.steps-section {
  min-height: 400px;
}

.steps-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.steps-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.steps-hint {
  color: #9ca3af;
}

.steps-empty {
  padding: 40px 0;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.step-card:hover {
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border-color: #c0c4cc;
}

.step-card.step-active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}

.step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
}

.step-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-card.step-active .step-order {
  background: #1677ff;
}

/* 步骤头部接口信息展示 */
.step-api-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.step-api-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-api-name {
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-api-missing {
  color: #f56c6c;
  font-size: 13px;
  flex: 1;
}

.step-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  flex-shrink: 0;
}

/* ---------- 请求覆盖 Tabs ---------- */
.request-override-section {
  margin-bottom: 14px;
}

.request-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

.request-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.request-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  font-weight: 600;
}

/* ---------- 右侧步骤详情 ---------- */
.detail-section {
  min-height: 300px;
}

.detail-empty {
  min-height: 200px;
}

.detail-empty :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
}

.detail-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-api-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.step-form-item {
  margin-bottom: 14px;
}

.field-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.assertion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.assertion-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assertion-type {
  width: 110px;
  flex-shrink: 0;
}

.assertion-path {
  width: 150px;
}

.assertion-operator {
  width: 90px;
}

.assertion-expected {
  flex: 1;
}

.step-extract-hint {
  margin-top: -6px;
  margin-bottom: 14px;
  padding-left: 0;
}

/* ---------- 接口标签 ---------- */
.method-tag {
  margin-right: 0;
  font-size: 12px;
  font-weight: 600;
  min-width: 48px;
  text-align: center;
}

.api-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.text-muted {
  color: #9ca3af;
}

/* ---------- 接口选择弹框 ---------- */
.api-dialog-body {
  padding: 0;
}

.api-dialog-select {
  width: 100%;
}

.api-select-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-option-name {
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-dialog-preview {
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafbfc;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.preview-row:last-child {
  margin-bottom: 0;
}

.preview-label {
  width: 70px;
  flex-shrink: 0;
  font-size: 13px;
  color: #6b7280;
}

.preview-value {
  font-size: 13px;
  color: #1f2937;
}

/* ---------- 代码文本框 ---------- */
.code-textarea :deep(.el-textarea__inner) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
}

/* ---------- 调试工具栏 ---------- */
.debug-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.debug-env-select {
  width: 160px;
}

/* ---------- 调试结果 ---------- */
.debug-result {
  padding: 0 4px;
}

.debug-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.debug-meta {
  color: #6b7280;
  font-size: 13px;
}

.debug-round {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.debug-round-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.debug-round-params {
  margin-bottom: 8px;
}

.debug-step {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.debug-step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.debug-step-title {
  font-weight: 600;
  color: #1f2937;
}

.debug-step-api {
  color: #374151;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

.debug-step-time {
  margin-left: auto;
  color: #9ca3af;
  font-size: 12px;
}

.debug-error {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 12px;
  white-space: pre-wrap;
}

.debug-pre {
  margin: 0;
  padding: 10px;
  border-radius: 6px;
  background: #f9fafb;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.debug-kv {
  margin-bottom: 12px;
}

.debug-kv:last-child {
  margin-bottom: 0;
}

.debug-kv-label {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.debug-status {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.debug-assert-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

/* ---------- 参数化数据 ---------- */
.dataset-section {
  margin-top: 12px;
}

.dataset-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dataset-tip {
  color: #9ca3af;
  font-size: 12px;
  margin-bottom: 8px;
}

.dataset-entry {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dataset-summary {
  color: #374151;
  font-size: 13px;
}

.dataset-empty {
  color: #9ca3af;
  font-size: 13px;
}

.dataset-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
}

.dataset-th,
.dataset-td {
  padding: 4px;
  border: 1px solid #e5e7eb;
}

.dataset-th-inner {
  display: flex;
  align-items: center;
  gap: 2px;
}

.dataset-op-col {
  width: 60px;
  text-align: center;
}

.dataset-actions {
  display: flex;
  gap: 8px;
}
</style>
