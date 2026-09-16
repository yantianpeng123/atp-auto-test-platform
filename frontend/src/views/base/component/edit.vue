<template>
  <div class="comp-edit-page">
    <!-- 顶部操作栏：组件名称 / 组件描述 / 归属模块 / 取消 / 保存 -->
    <div class="top-bar">
      <div class="top-bar-row">
        <div class="top-bar-fields">
          <el-form-item label="组件名称" class="bar-form-item" required>
            <el-input
              v-model="form.name"
              placeholder="请输入组件名称"
              clearable
              class="bar-input-name"
            />
          </el-form-item>
          <el-form-item label="组件描述" class="bar-form-item">
            <el-input
              v-model="form.description"
              placeholder="组件用途说明（可选）"
              clearable
              class="bar-input-desc"
            />
          </el-form-item>
          <el-form-item label="归属模块" required class="bar-form-item">
            <el-select
              v-model="form.moduleId"
              placeholder="选择模块（添加步骤前必选）"
              clearable
              filterable
              class="bar-select"
            >
              <el-option v-for="m in moduleOptions" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="top-bar-right">
          <el-button :loading="saving" @click="onBack">取消</el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="edit-body">
      <div class="edit-form">
        <!-- 左侧：接口步骤列表 -->
        <div class="left-panel">
          <el-card shadow="never" class="section-card steps-section">
            <template #header>
              <div class="steps-card-header">
                <div class="steps-header-actions">
                  <el-button type="primary" size="small" :icon="Plus" @click="openApiDialog">添加步骤</el-button>
                </div>
                <div class="debug-bar">
                  <el-select
                    v-model="debugEnvId"
                    placeholder="环境列表"
                    size="small"
                    class="debug-env-select"
                    clearable
                  >
                    <el-option v-for="env in envOptions" :key="env.id" :label="env.name" :value="env.id" />
                  </el-select>
                  <el-button
                    type="warning"
                    size="small"
                    :disabled="!debugEnvId"
                    @click="handleDebug"
                  >
                    调试运行
                  </el-button>
                </div>
              </div>
            </template>

            <div v-if="form.steps.length" class="step-list">
              <div
                v-for="(step, i) in form.steps"
                :key="step._id"
                class="tree-step"
                :class="{ 'tree-step-active': step._id === activeUid }"
                @click="selectStep(step._id)"
              >
                <span class="tree-step-order">{{ i + 1 }}</span>
                <template v-if="step.stepType === 1">
                  <el-tag
                    v-if="step.apiMethod"
                    size="small"
                    :type="methodTagType(step.apiMethod)"
                    class="method-tag"
                  >
                    {{ step.apiMethod }}
                  </el-tag>
                  <span v-if="step.apiPath" class="tree-step-path">{{ step.apiPath }}</span>
                  <span v-else class="tree-step-missing">未选择接口</span>
                </template>
                <template v-else-if="step.stepType === 3">
                  <el-tag size="small" type="info" class="method-tag">变量</el-tag>
                  <span class="tree-step-path">{{ step.variableName || step.generatorName || '未配置变量名' }}</span>
                </template>
                <template v-else>
                  <el-tag size="small" type="warning" class="method-tag">组件</el-tag>
                  <span class="tree-step-path">{{ step.childComponentName || '未选择组件' }}</span>
                </template>
                <div class="tree-step-actions" @click.stop>
                  <el-tooltip content="上移" placement="top">
                    <el-button :icon="Top" link size="small" :disabled="i === 0" @click="moveStep(i, -1)" />
                  </el-tooltip>
                  <el-tooltip content="下移" placement="top">
                    <el-button
                      :icon="Bottom"
                      link
                      size="small"
                      :disabled="i === form.steps.length - 1"
                      @click="moveStep(i, 1)"
                    />
                  </el-tooltip>
                  <el-tooltip content="删除步骤" placement="top">
                    <el-button :icon="Delete" link type="danger" size="small" @click="removeStep(i)" />
                  </el-tooltip>
                </div>
              </div>
            </div>
            <el-empty v-else description="请点击「添加步骤」选择接口" :image-size="80" />
          </el-card>
        </div>

        <!-- 右侧：选中步骤详情 -->
        <div class="right-panel">
          <el-card v-if="activeStep" shadow="never" class="section-card detail-section">
            <template #header>
              <div class="detail-card-header">
                <template v-if="activeStep.stepType === 1">
                  <span class="section-title">当前接口:</span>
                  <el-tag
                    v-if="activeStep.apiMethod"
                    size="small"
                    :type="methodTagType(activeStep.apiMethod)"
                    class="method-tag"
                  >
                    {{ activeStep.apiMethod }}
                  </el-tag>
                  <span v-if="activeStep.apiPath" class="detail-api-path">{{ activeStep.apiPath }}</span>
                  <el-button type="primary" link size="small" @click="openApiDialog(activeStep._id)">更换接口</el-button>
                </template>
                <template v-else-if="activeStep.stepType === 3">
                  <span class="section-title">生成变量:</span>
                  <el-tag size="small" type="info" class="method-tag">变量</el-tag>
                  <span class="detail-api-path">{{ activeStep.generatorName || '未选择生成器' }}</span>
                </template>
                <template v-else>
                  <span class="section-title">嵌套组件:</span>
                  <span class="detail-api-path">{{ activeStep.childComponentName || '未选择组件' }}</span>
                </template>
              </div>
            </template>

            <!-- 返回变量名（单接口步骤） -->
            <div v-if="activeStep.stepType === 1" class="step-form-item">
              <div class="field-label">返回变量名</div>
              <el-input
                v-model="activeStep.responseVar"
                placeholder="给该接口响应数据命名，供后续步骤引用（如 loginResp），留空则不保存"
                clearable
              />
            </div>

            <!-- 生成变量步骤配置（stepType=3） -->
            <template v-else-if="activeStep.stepType === 3">
              <div class="step-form-item">
                <div class="field-label">输出变量名</div>
                <el-input
                  v-model="activeStep.variableName"
                  placeholder="如 phoneVar，后续步骤用 ${phoneVar} 引用"
                  clearable
                />
              </div>
              <div class="step-form-item">
                <div class="field-label">每次执行重新生成</div>
                <el-switch v-model="activeStep.regenEachRun" :active-value="1" :inactive-value="0" />
                <span class="form-hint">关闭则整个执行过程固定同一值</span>
              </div>
            </template>

            <!-- 请求覆盖：请求头 / 请求参数 -->
            <template v-if="activeStep.stepType === 1">
              <div class="request-override-section">
                <el-tabs v-model="requestTab" class="request-tabs">
                  <el-tab-pane label="请求头" name="headers">
                    <el-input
                      v-model="activeStep.requestHeaders"
                      type="textarea"
                      :rows="4"
                      @blur="formatJsonField('requestHeaders')"
                      placeholder='JSON格式，覆盖接口默认请求头&#10;可用${varName}引用上一步提取的变量&#10;如：{"Authorization":"Bearer ${token}","Content-Type":"application/json"}'
                      class="code-textarea"
                    />
                  </el-tab-pane>
                  <el-tab-pane label="请求参数" name="params">
                    <el-input
                      v-model="activeStep.requestBody"
                      type="textarea"
                      :rows="4"
                      @blur="formatJsonField('requestBody')"
                      placeholder='JSON格式，覆盖接口默认请求参数(body)&#10;可用${varName}引用上一步提取的变量&#10;如：{"userId":"${userId}","page":1}'
                      class="code-textarea"
                    />
                  </el-tab-pane>
                </el-tabs>
              </div>
            </template>
            <el-alert
              v-else-if="activeStep.stepType === 2"
              type="info"
              :closable="false"
              title="嵌套组件步骤的请求头 / 请求参数沿用组件自身配置，此处仅可设置返回变量名。"
            />
            <el-alert
              v-else
              type="info"
              :closable="false"
              title="生成变量步骤由数据生成器产出值并写入变量池，供后续步骤通过 ${变量名} 引用。"
            />
          </el-card>

          <el-card v-else shadow="never" class="section-card detail-section detail-empty">
            <el-empty description="请点击左侧「接口步骤」下的接口查看详情" :image-size="100" />
          </el-card>
        </div>
      </div>
    </div>

    <!-- ============ 类型选择弹框（新增时） ============ -->
    <el-dialog
      v-model="typeDialogVisible"
      title="选择组件类型"
      width="460px"
      :show-close="false"
      :close-on-click-modal="false"
      append-to-body
      @closed="typeChosen = ''"
    >
      <div class="type-dialog-body">
        <div class="type-card" :class="{ active: typeChosen === 'api' }" @click="typeChosen = 'api'">
          <div class="type-card-title">接口组件（推荐）</div>
          <div class="type-card-desc">由单个或多个接口按顺序组成；可配置请求头 / 请求参数 / 断言，复用性强。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="onBack">取消</el-button>
        <el-button v-if="typeChosen === 'api'" type="primary" @click="confirmType">下一步</el-button>
        <template v-else-if="typeChosen === 'gen'">
          <el-button type="primary" @click="genDialogVisible = true">新建生成器</el-button>
          <el-button @click="selectGenVisible = true">选择已有生成器</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- ============ 生成器弹窗（新建 / 选择已有） ============ -->
    <GeneratorFormDialog
      v-model="genDialogVisible"
      :project-id="projectId"
      :edit-data="null"
      @saved="onGenDialogSaved"
    />
    <GeneratorSelectDialog
      v-model="selectGenVisible"
      :project-id="projectId"
      @selected="onGenSelected"
    />

    <!-- ============ 接口选择弹框（添加步骤 / 更换接口） ============ -->
    <el-dialog
      v-model="apiDialogVisible"
      title="选择接口"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
      @closed="apiDialogSelectedId = null"
    >
      <div class="api-dialog-body">
        <el-form label-width="80px">
          <el-form-item label="选择接口">
            <el-select
              v-model="apiDialogSelectedId"
              placeholder="请选择接口（支持搜索）"
              filterable
              :loading="apiListLoading"
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Bottom, Delete, Plus, Top } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getApiList, getModuleOptions } from '@/api/base'
import { getEnvList } from '@/api/env'
import {
  getComponentList,
  getComponentDetail,
  createComponent,
  updateComponent
} from '@/api/component'
import { executeCase } from '@/api/execute'
import { useProjectStore } from '@/stores/project'
import GeneratorFormDialog from '@/views/base/generator/GeneratorFormDialog.vue'
import GeneratorSelectDialog from '@/views/base/generator/GeneratorSelectDialog.vue'
import type {
  ApiComponentInfo,
  ApiComponentSaveParams,
  ApiInfo,
  AssertionItem,
  CaseExecuteResult,
  ComponentStepSaveParams,
  DataGeneratorInfo,
  EnvInfo,
  GeneratorStepSeed,
  OptionItem,
  StepExecuteResult
} from '@/api/types'

const props = defineProps<{ id?: number | null }>()
const emit = defineEmits<{ (e: 'back'): void; (e: 'saved'): void }>()

const projectStore = useProjectStore()

/** 当前项目 ID（生成器弹窗需传） */
const projectId = computed(() => projectStore.currentProject?.id ?? 0)

/* ============ 类型选择 ============ */
const typeDialogVisible = ref(false)
const typeChosen = ref<'' | 'api' | 'gen'>('')

/* 生成器弹窗（新建 / 选择已有） */
const genDialogVisible = ref(false)
const selectGenVisible = ref(false)

function seedFromGenerator(g: DataGeneratorInfo): GeneratorStepSeed {
  const p = g.params ?? {}
  return {
    generatorId: g.id,
    generatorName: g.name,
    variableName: String(p.variableName ?? g.name),
    regenEachRun: p.regenEachRun === false ? false : true
  }
}

function onGenDialogSaved(info: DataGeneratorInfo) {
  addGeneratorStep(seedFromGenerator(info))
  genDialogVisible.value = false
  typeDialogVisible.value = false
}

function onGenSelected(info: DataGeneratorInfo) {
  addGeneratorStep(seedFromGenerator(info))
  selectGenVisible.value = false
  typeDialogVisible.value = false
}

function confirmType() {
  if (typeChosen.value !== 'api') return
  typeDialogVisible.value = false
  openEditor(null)
}

/* ============ 编辑表单（基础信息 + 步骤） ============ */
const saving = ref(false)
const editingId = ref<number | null>(null)

interface StepEdit {
  _id: number
  stepType: number
  apiId: number | null
  childComponentId: number | null
  stepName: string
  responseVar: string
  isDisabled: number
  continueOnFail: number
  description: string
  apiName?: string
  apiMethod?: string
  apiPath?: string
  childComponentName?: string
  /** 生成变量步骤（stepType=3）：关联生成器 */
  generatorId?: number | null
  generatorName?: string
  /** 生成变量步骤的输出变量名（供后续步骤 ${varName} 引用） */
  variableName?: string
  /** 生成变量步骤：每次执行是否重新生成 */
  regenEachRun?: number
  requestHeaders: string
  requestBody: string
  assertions: AssertionItem[]
}

const form = reactive({
  name: '',
  description: '',
  moduleId: null as number | null,
  steps: [] as StepEdit[]
})

let stepSeq = 1

const apiOptions = ref<ApiInfo[]>([])
/** 接口列表加载中（选择接口弹窗） */
const apiListLoading = ref(false)
const componentOptions = ref<ApiComponentInfo[]>([])
const componentMap = ref<Record<number, string>>({})
const moduleOptions = ref<OptionItem[]>([])
const envOptions = ref<EnvInfo[]>([])

/* ============ 步骤选中 / 详情 ============ */
const activeUid = ref<number | null>(null)
const requestTab = ref('headers')


const activeStep = computed(() => form.steps.find((s) => s._id === activeUid.value) ?? null)

function selectStep(uid: number) {
  activeUid.value = uid
  requestTab.value = 'headers'
}

/* ============ 环境列表 / 调试运行 ============ */
const debugEnvId = ref<number | null>(null)
const debugging = ref(false)
const debugVisible = ref(false)
const debugResult = ref<CaseExecuteResult | null>(null)

async function loadEnvOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    envOptions.value = []
    return
  }
  try {
    const res = await getEnvList({ projectId, page: 1, size: 1000 })
    envOptions.value = res.records
  } catch {
    envOptions.value = []
  }
}

/** 调试运行：调用 /api/execute/case/{id}（debug=true）；后端对组件 ID 回退为组件步骤展开执行，不落库 */
async function handleDebug() {
  if (editingId.value === null) {
    ElMessage.warning('请先保存组件，再进行调试')
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

/* ============ 选项加载 ============ */
async function loadApiOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId || form.moduleId == null) {
    apiOptions.value = []
    return
  }
  apiListLoading.value = true
  try {
    const res = await getApiList({ projectId, moduleId: form.moduleId, page: 1, size: 1000 })
    apiOptions.value = res.records
  } catch {
    apiOptions.value = []
  } finally {
    apiListLoading.value = false
  }
}

/** 模块变化时重新拉取该模块下的接口列表 */
watch(
  () => form.moduleId,
  () => {
    loadApiOptions()
  }
)

async function loadComponentOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    componentOptions.value = []
    return
  }
  try {
    const res = await getComponentList({ projectId, page: 1, size: 1000 })
    componentOptions.value = res.records
    const map: Record<number, string> = {}
    res.records.forEach((c) => {
      map[c.id] = c.name
    })
    componentMap.value = map
  } catch {
    componentOptions.value = []
  }
}

async function loadModuleOptions() {
  try {
    moduleOptions.value = await getModuleOptions()
  } catch {
    moduleOptions.value = []
  }
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.moduleId = null
  form.steps = []
  editingId.value = null
  activeUid.value = null
  apiOptions.value = []
}

async function openEditor(id: number | null) {
  await Promise.all([loadComponentOptions(), loadModuleOptions(), loadEnvOptions()])
  resetForm()
  if (id != null) {
    editingId.value = id
    try {
      const detail = await getComponentDetail(id)
      form.name = detail.name
      form.description = detail.description || ''
      form.moduleId = detail.moduleId ?? null
      form.steps = (detail.steps || []).map((s) => {
        const isChild = s.stepType === 2
        const parsed = parseRequestOverride(s.requestOverride)
        return {
          _id: stepSeq++,
          stepType: s.stepType || 1,
          apiId: isChild ? null : (s.apiId ?? null),
          childComponentId: isChild ? (s.componentId ?? null) : null,
          stepName: s.stepName || '',
          responseVar: s.responseVar || '',
          isDisabled: s.isDisabled ?? 0,
          continueOnFail: s.continueOnFail ?? 0,
          description: s.description || '',
          apiName: s.apiName || undefined,
          apiMethod: s.apiMethod || undefined,
          apiPath: s.apiPath || undefined,
          childComponentName: isChild ? componentMap.value[s.componentId as number] : undefined,
          requestHeaders: parsed.headers,
          requestBody: parsed.body,
          assertions: parseAssertions(s.assertions)
        } as StepEdit
      })
      if (form.steps.length) activeUid.value = form.steps[0]._id
    } catch {
      form.steps = []
    }
  }
}

/* ============ 步骤排序 / 删除 ============ */
function moveStep(index: number, dir: number) {
  const target = index + dir
  if (target < 0 || target >= form.steps.length) return
  ;[form.steps[index], form.steps[target]] = [form.steps[target], form.steps[index]]
}

function removeStep(index: number) {
  const removed = form.steps[index]
  form.steps.splice(index, 1)
  if (removed._id === activeUid.value) {
    activeUid.value = form.steps.length ? form.steps[Math.min(index, form.steps.length - 1)]._id : null
  }
}

/* ============ 接口选择弹框（添加步骤 / 更换接口） ============ */
const apiDialogVisible = ref(false)
const apiDialogSelectedId = ref<number | null>(null)
/** 更换接口时记录目标步骤 _id；为 null 表示添加新步骤 */
const replacingUid = ref<number | null>(null)

const apiDialogPreview = computed(
  () => apiOptions.value.find((a) => a.id === apiDialogSelectedId.value) ?? null
)

async function openApiDialog(uid?: number) {
  if (!projectStore.currentProject?.id) {
    ElMessage.warning('当前未选择项目，无法加载接口列表')
    return
  }
  if (form.moduleId == null) {
    ElMessage.warning('请先在顶部选择归属模块')
    return
  }
  replacingUid.value = uid ?? null
  apiDialogSelectedId.value = null
  apiDialogVisible.value = true
  // 显式拉取接口列表，避免依赖 moduleId watch 的副作用导致下拉为空
  await loadApiOptions()
}

function confirmApiSelect() {
  const api = apiOptions.value.find((a) => a.id === apiDialogSelectedId.value)
  if (!api) return
  if (replacingUid.value != null) {
    const step = form.steps.find((s) => s._id === replacingUid.value)
    if (step) {
      step.apiId = api.id
      step.apiName = api.name
      step.apiMethod = api.method
      step.apiPath = api.path
      if (!step.stepName) step.stepName = api.name
    }
  } else {
    const step: StepEdit = {
      _id: stepSeq++,
      stepType: 1,
      apiId: api.id,
      childComponentId: null,
      stepName: api.name,
      responseVar: '',
      isDisabled: 0,
      continueOnFail: 0,
      description: '',
      apiName: api.name,
      apiMethod: api.method,
      apiPath: api.path,
      requestHeaders: '',
      requestBody: '',
      assertions: []
    }
    form.steps.push(step)
    activeUid.value = step._id
  }
  apiDialogVisible.value = false
}

/** 从生成器页"选择"带回：写入一条 stepType=3 生成变量步骤 */
function addGeneratorStep(seed: GeneratorStepSeed) {
  const step: StepEdit = {
    _id: stepSeq++,
    stepType: 3,
    apiId: null,
    childComponentId: null,
    stepName: seed.generatorName,
    responseVar: '',
    isDisabled: 0,
    continueOnFail: 0,
    description: '',
    generatorId: seed.generatorId,
    generatorName: seed.generatorName,
    variableName: seed.variableName,
    regenEachRun: seed.regenEachRun ? 1 : 0,
    requestHeaders: '',
    requestBody: '',
    assertions: []
  }
  form.steps.push(step)
  activeUid.value = step._id
}

/* ============ 展示辅助 ============ */
function methodTagType(method: string): 'success' | 'warning' | 'danger' | 'primary' | 'info' {
  switch ((method || '').toUpperCase()) {
    case 'GET':
      return 'success'
    case 'POST':
      return 'warning'
    case 'DELETE':
      return 'danger'
    case 'PUT':
    case 'PATCH':
      return 'primary'
    default:
      return 'info'
  }
}

/* ============ JSON / 断言 工具 ============ */
function isValidJson(str: string): boolean {
  if (!str?.trim()) return true
  try {
    JSON.parse(str)
    return true
  } catch {
    return false
  }
}

function formatJsonField(field: 'requestHeaders' | 'requestBody') {
  const step = activeStep.value
  if (!step) return
  const val = step[field]
  if (!val?.trim()) return
  try {
    step[field] = JSON.stringify(JSON.parse(val), null, 2)
  } catch {
    ElMessage.warning(field === 'requestHeaders' ? '请求头不是合法 JSON，未格式化' : '请求参数不是合法 JSON，未格式化')
  }
}

function parseRequestOverride(override: string | undefined | null): { headers: string; body: string } {
  if (!override?.trim()) return { headers: '', body: '' }
  try {
    const obj = JSON.parse(override)
    const headers = obj.headers ? JSON.stringify(obj.headers, null, 2) : ''
    const body = obj.body ? JSON.stringify(obj.body, null, 2) : ''
    return { headers, body }
  } catch {
    return { headers: '', body: '' }
  }
}

function buildRequestOverride(headers: string, body: string): string {
  const obj: Record<string, unknown> = {}
  let hasContent = false
  if (headers?.trim()) {
    try { obj.headers = JSON.parse(headers); hasContent = true } catch { /* ignore */ }
  }
  if (body?.trim()) {
    try { obj.body = JSON.parse(body); hasContent = true } catch { /* ignore */ }
  }
  return hasContent ? JSON.stringify(obj) : ''
}

function serializeAssertions(list: AssertionItem[]): string | undefined {
  const valid = list.filter((a) => a.type)
  return valid.length > 0 ? JSON.stringify(valid) : undefined
}

function parseAssertions(json: string | null | undefined): AssertionItem[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

/* ============ 保存 / 返回 ============ */
async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.error('请输入组件名称')
    return
  }

  if (form.steps.length === 0) {
    ElMessage.error('请至少添加一个接口步骤')
    return
  }
  for (let i = 0; i < form.steps.length; i++) {
    const s = form.steps[i]
    if (s.stepType === 1 && !s.apiId) {
      ElMessage.error(`第 ${i + 1} 个步骤未选择接口`)
      return
    }
    if (s.stepType === 1) {
      if (!isValidJson(s.requestHeaders)) {
        ElMessage.error(`第 ${i + 1} 个步骤的请求头不是合法 JSON`)
        return
      }
      if (!isValidJson(s.requestBody)) {
        ElMessage.error(`第 ${i + 1} 个步骤的请求参数不是合法 JSON`)
        return
      }
    }
    if (s.stepType === 2) {
      if (!s.childComponentId) {
        ElMessage.error(`第 ${i + 1} 个步骤未选择嵌套组件`)
        return
      }
      if (s.childComponentId === editingId.value) {
        ElMessage.error('组合组件不能嵌套自身')
        return
      }
    }
    if (s.stepType === 3) {
      if (!s.generatorId) {
        ElMessage.error(`第 ${i + 1} 个步骤未选择数据生成器`)
        return
      }
    }
  }

  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }

  const stepsPayload: ComponentStepSaveParams[] = form.steps.map((s, i) => ({
    stepType: s.stepType,
    apiId: s.stepType === 2 ? null : s.apiId,
    childComponentId: s.stepType === 2 ? s.childComponentId : null,
    generatorId: s.stepType === 3 ? s.generatorId ?? null : null,
    variableName: s.stepType === 3 ? s.variableName?.trim() || undefined : undefined,
    regenEachRun: s.stepType === 3 ? (s.regenEachRun === 1 ? 1 : 0) : undefined,
    sortOrder: i + 1,
    stepName: s.stepName.trim() || undefined,
    responseVar: s.responseVar.trim() || undefined,
    requestOverride: s.stepType === 1 ? (buildRequestOverride(s.requestHeaders, s.requestBody) || undefined) : undefined,
    assertions: serializeAssertions(s.assertions),
    isDisabled: s.isDisabled,
    continueOnFail: s.continueOnFail,
    description: s.description.trim() || undefined
  }))

  const payload: ApiComponentSaveParams = {
    id: editingId.value || undefined,
    projectId,
    moduleId: form.moduleId,
    name: form.name.trim(),
    description: form.description.trim() || undefined,
    steps: stepsPayload
  }

  saving.value = true
  try {
    if (editingId.value) {
      await updateComponent(payload)
      ElMessage.success('组件修改成功')
    } else {
      await createComponent(payload)
      ElMessage.success('组件新增成功')
    }
    emit('saved')
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function onBack() {
  emit('back')
}

onMounted(() => {
  if (props.id != null) {
    openEditor(props.id)
  } else {
    typeChosen.value = ''
    typeDialogVisible.value = true
  }
})
</script>

<style scoped>
.comp-edit-page {
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

.bar-input-name {
  width: 220px;
}

.bar-input-desc {
  width: 300px;
}

.bar-select {
  width: 180px;
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

/* ---------- 接口步骤列表 ---------- */
.steps-section {
  min-height: 400px;
}

.steps-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.steps-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.debug-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.debug-env-select {
  width: 140px;
}

.step-list {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tree-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}

.tree-step.tree-step-active {
  background: #ecf5ff;
}

.tree-step-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.tree-step-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.tree-step-missing {
  color: #f56c6c;
  font-size: 13px;
  flex: 1;
}

.tree-step-actions {
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

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
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

/* ---------- 接口标签 ---------- */
.method-tag {
  margin-right: 0;
  flex-shrink: 0;
}

.code-textarea :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

/* ---------- 接口选择弹框 ---------- */
.api-dialog-body {
  min-height: 120px;
}

.api-dialog-select {
  width: 100%;
}

.api-select-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-path {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  color: #374151;
}

.api-option-name {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}

.api-dialog-preview {
  margin-top: 12px;
  padding: 12px 14px;
  background: #f9fafb;
  border: 1px solid #f0f1f3;
  border-radius: 6px;
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
  width: 60px;
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}

.preview-value {
  font-size: 13px;
  color: #374151;
  word-break: break-all;
}

.text-muted {
  color: #909399;
}

/* ---------- 类型选择弹框 ---------- */
.type-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.type-card {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.15s;
}

.type-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.type-card.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.type-card-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.type-card-desc {
  font-size: 13px;
  color: #606266;
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

.debug-assert-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}
</style>
