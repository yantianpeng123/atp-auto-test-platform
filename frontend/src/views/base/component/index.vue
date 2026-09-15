<template>
  <div class="component-page">
    <!-- ============ 列表视图 ============ -->
    <template v-if="viewMode === 'list'">
      <!-- 查询栏 -->
      <el-card shadow="never" class="filter-card">
        <el-form :inline="true" :model="query" @submit.prevent>
          <el-form-item label="组件名称">
            <el-input
              v-model="query.name"
              placeholder="按名称模糊搜索"
              clearable
              class="filter-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
          <el-form-item class="filter-right">
            <el-button type="primary" :icon="Plus" @click="openCreate">新增组件</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 列表 -->
      <el-card shadow="never" class="table-card">
        <el-table :data="records" v-loading="loading" border stripe empty-text="暂无组合组件">
          <el-table-column label="组件名称" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="comp-name">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column label="描述" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.description" class="comp-desc">{{ row.description }}</span>
              <span v-else class="comp-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="步骤数" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.stepCount ?? 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170" prop="updateTime" />
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadList"
            @current-change="loadList"
          />
        </div>
      </el-card>
    </template>

    <!-- ============ 编辑视图（基础信息页） ============ -->
    <template v-else>
      <div class="comp-editor">
        <!-- 顶部操作栏 -->
        <div class="editor-header">
          <el-button :icon="ArrowLeft" @click="backToList">返回</el-button>
          <span class="editor-title">{{ editingId ? '编辑组件' : '新增组件' }}</span>
          <div class="editor-header-right">
            <el-button :loading="saving" @click="backToList">取消</el-button>
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          </div>
        </div>

        <!-- 基础信息 -->
        <el-card shadow="never" class="editor-card">
          <template #header>
            <span class="section-title">基础信息</span>
          </template>
          <el-form ref="formEl" :model="form" :rules="rules" label-width="110px" class="basic-form">
            <el-form-item label="组件名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入组件名称" clearable class="form-input" />
            </el-form-item>
            <el-form-item label="组件描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="2"
                placeholder="组件用途说明（可选）"
              />
            </el-form-item>
            <el-form-item label="归属模块">
              <el-select
                v-model="form.moduleId"
                placeholder="可选，按模块归类"
                clearable
                filterable
                class="form-input"
              >
                <el-option v-for="m in moduleOptions" :key="m.id" :label="m.name" :value="m.id" />
              </el-select>
            </el-form-item>

            <!-- 组件级字段：仅单接口组件生效；多接口组件禁用 -->
            <el-form-item label="返回变量名">
              <el-input
                v-model="compReturnVarModel"
                :disabled="!isSingleInterface"
                placeholder="如 loginResp，留空则不保存"
                clearable
                class="form-input"
              />
            </el-form-item>
            <el-form-item label="是否禁用">
              <el-switch
                v-model="compIsDisabledModel"
                :disabled="!isSingleInterface"
                :active-value="1"
                :inactive-value="0"
              />
            </el-form-item>
            <el-form-item label="失败继续">
              <el-switch
                v-model="compContinueOnFailModel"
                :disabled="!isSingleInterface"
                :active-value="1"
                :inactive-value="0"
              />
            </el-form-item>
          </el-form>

          <el-alert
            v-if="!isSingleInterface"
            type="info"
            :closable="false"
            class="level-hint"
            title="当前为多接口（或含嵌套）组件：返回变量名 / 是否禁用 / 失败继续 由各接口步骤独立配置，基础信息中的以上三项不可编辑。"
          />
        </el-card>

        <!-- 接口步骤 -->
        <el-card shadow="never" class="editor-card">
          <template #header>
            <div class="steps-editor-header">
              <span class="section-title">接口步骤（按顺序执行）</span>
              <div class="steps-editor-actions">
                <el-button size="small" type="primary" :icon="Plus" @click="addStep(1)">添加接口</el-button>
                <el-button
                  size="small"
                  :icon="Share"
                  :disabled="componentOptions.length === 0"
                  @click="addStep(2)"
                >
                  添加嵌套组件
                </el-button>
              </div>
            </div>
          </template>

          <el-alert
            v-if="componentOptions.length === 0"
            type="info"
            :closable="false"
            class="steps-hint"
            title="当前项目还没有其它组合组件，无法嵌套；可先添加“接口步骤”，后续再编辑为嵌套结构。"
          />

          <!-- 文本框卡片样式 -->
          <div v-if="form.steps.length" class="step-cards">
            <div v-for="(step, i) in form.steps" :key="step._id" class="step-card">
              <div class="step-card-head">
                <span class="step-index">#{{ i + 1 }}</span>
                <el-tag size="small" :type="step.stepType === 1 ? 'success' : 'warning'">
                  {{ step.stepType === 1 ? '单接口' : '嵌套组件' }}
                </el-tag>
                <div class="step-card-ops">
                  <el-button link type="primary" :icon="Top" size="small" :disabled="i === 0" @click="moveStep(i, -1)" />
                  <el-button link type="primary" :icon="Bottom" size="small" :disabled="i === form.steps.length - 1" @click="moveStep(i, 1)" />
                  <el-button link type="primary" :icon="Edit" size="small" @click="editStep(i)" />
                  <el-button link type="danger" :icon="Delete" size="small" @click="removeStep(i)" />
                </div>
              </div>

              <div class="step-card-body">
                <div class="field">
                  <label>类型</label>
                  <el-input :model-value="step.stepType === 1 ? '单接口' : '嵌套组件'" readonly />
                </div>
                <div class="field field-wide">
                  <label>接口 / 组件</label>
                  <el-input
                    :model-value="displayTarget(step)"
                    readonly
                  />
                </div>
                <div class="field">
                  <label>步骤名称</label>
                  <el-input :model-value="step.stepName || '—'" readonly />
                </div>
                <div class="field">
                  <label>返回变量</label>
                  <el-input :model-value="step.responseVar || '—'" readonly />
                </div>
                <div class="field field-wide">
                  <label>Headers</label>
                  <el-input :model-value="displayHeaders(step)" type="textarea" :rows="2" readonly />
                </div>
                <div class="field field-wide">
                  <label>Body</label>
                  <el-input :model-value="displayBody(step)" type="textarea" :rows="2" readonly />
                </div>
                <div class="field">
                  <label>禁用</label>
                  <el-input :model-value="step.isDisabled === 1 ? '是' : '否'" readonly />
                </div>
                <div class="field">
                  <label>失败继续</label>
                  <el-input :model-value="step.continueOnFail === 1 ? '是' : '否'" readonly />
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="请添加接口步骤" :image-size="90" />
        </el-card>
      </div>
    </template>

    <!-- ============ 类型选择弹框 ============ -->
    <el-dialog
      v-model="typeDialogVisible"
      title="选择组件类型"
      width="460px"
      :close-on-click-modal="false"
      append-to-body
      @closed="typeChosen = ''"
    >
      <div class="type-dialog-body">
        <div class="type-card" :class="{ active: typeChosen === 'api' }" @click="typeChosen = 'api'">
          <div class="type-card-title">接口组件（推荐）</div>
          <div class="type-card-desc">由单个或多个接口按顺序组成；可配置请求头 / 请求参数 / 断言，复用性强。</div>
        </div>
        <div class="type-card disabled" @click="onOtherTypeClick">
          <div class="type-card-title">
            其他类型 · 生成随机数
            <el-tag size="small" type="info" effect="plain">待实现</el-tag>
          </div>
          <div class="type-card-desc">后续将支持生成随机数等非接口类型的扩展步骤。</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="typeChosen !== 'api'" @click="confirmType">下一步</el-button>
      </template>
    </el-dialog>

    <!-- ============ 添加 / 编辑 接口步骤弹框 ============ -->
    <el-dialog
      v-model="stepDialogVisible"
      :title="stepEditIndex === -1 ? '添加接口步骤' : '编辑接口步骤'"
      width="860px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetStepForm"
    >
      <el-form label-width="110px" class="step-form">
        <!-- 单接口：选择接口 + 配置 -->
        <template v-if="stepForm.stepType === 1">
          <el-form-item label="选择接口" prop="apiId">
            <el-select
              v-model="stepForm.apiId"
              placeholder="请选择接口（支持搜索）"
              filterable
              class="step-form-input"
              value-key="id"
              @change="onStepApiChange"
            >
              <el-option
                v-for="api in apiOptions"
                :key="api.id"
                :label="`${api.method} ${api.path} — ${api.name}`"
                :value="api.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="stepForm.apiMethod" label="接口信息">
            <div class="api-preview">
              <el-tag size="small" :type="methodTagType(stepForm.apiMethod)">{{ stepForm.apiMethod }}</el-tag>
              <span class="api-preview-path">{{ stepForm.apiPath }}</span>
            </div>
          </el-form-item>
          <el-form-item label="步骤名称">
            <el-input v-model="stepForm.stepName" placeholder="默认取接口名称" clearable class="step-form-input" />
          </el-form-item>
          <el-form-item label="返回变量名">
            <el-input v-model="stepForm.responseVar" placeholder="如 loginResp，供后续步骤引用" clearable class="step-form-input" />
          </el-form-item>

          <!-- 请求覆盖：请求头 / 请求参数 -->
          <el-form-item label="请求覆盖">
            <el-tabs v-model="stepRequestTab" class="request-tabs">
              <el-tab-pane label="请求头" name="headers">
                <el-input
                  v-model="stepForm.requestHeaders"
                  type="textarea"
                  :rows="4"
                  @blur="formatJsonField('requestHeaders')"
                  placeholder='JSON，覆盖接口默认请求头；如 {"Authorization":"Bearer ${token}"}'
                  class="code-textarea"
                />
              </el-tab-pane>
              <el-tab-pane v-if="showStepBody" label="请求参数" name="body">
                <el-input
                  v-model="stepForm.requestBody"
                  type="textarea"
                  :rows="4"
                  @blur="formatJsonField('requestBody')"
                  placeholder='JSON，覆盖接口默认请求参数(body)；如 {"userId":"${userId}","page":1}'
                  class="code-textarea"
                />
              </el-tab-pane>
            </el-tabs>
            <el-alert
              v-if="!showStepBody"
              type="info"
              :closable="false"
              class="override-body-hint"
              title="当前接口方法（GET / HEAD / OPTIONS 等）无请求体，无需填写请求参数。"
            />
          </el-form-item>

          <!-- 断言 -->
          <el-form-item label="断言规则">
            <div class="assertion-list">
              <div v-for="(a, idx) in stepForm.assertions" :key="idx" class="assertion-row">
                <el-select v-model="a.type" class="assertion-type">
                  <el-option label="状态码" value="status" />
                  <el-option label="JSONPath" value="jsonPath" />
                  <el-option label="响应头" value="header" />
                  <el-option label="响应体包含" value="body" />
                </el-select>
                <el-input
                  v-if="a.type === 'jsonPath' || a.type === 'header'"
                  v-model="a.path"
                  placeholder="路径，如 $.code"
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
                <el-button link type="danger" :icon="Delete" @click="removeStepAssertion(idx)" />
              </div>
              <el-button type="primary" link :icon="Plus" @click="addStepAssertion">添加断言</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 嵌套组件：仅基础字段 -->
        <template v-else>
          <el-form-item label="选择组件" prop="childComponentId">
            <el-select
              v-model="stepForm.childComponentId"
              placeholder="请选择组合组件"
              filterable
              class="step-form-input"
              @change="onStepChildChange"
            >
              <el-option
                v-for="c in availableComponentOptions"
                :key="c.id"
                :label="c.name"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="步骤名称">
            <el-input v-model="stepForm.stepName" placeholder="默认取组件名称" clearable class="step-form-input" />
          </el-form-item>
          <el-form-item label="返回变量名">
            <el-input v-model="stepForm.responseVar" placeholder="如 compResp" clearable class="step-form-input" />
          </el-form-item>
          <el-form-item label="是否禁用">
            <el-switch v-model="stepForm.isDisabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="失败继续">
            <el-switch v-model="stepForm.continueOnFail" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStep">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ArrowLeft,
  Bottom,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  Share,
  Top
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getApiList, getModuleOptions } from '@/api/base'
import {
  getComponentList,
  getComponentDetail,
  createComponent,
  updateComponent,
  deleteComponent
} from '@/api/component'
import { useProjectStore } from '@/stores/project'
import type {
  ApiComponentInfo,
  ApiComponentSaveParams,
  ApiInfo,
  AssertionItem,
  ComponentStepSaveParams,
  OptionItem
} from '@/api/types'

const projectStore = useProjectStore()

/* ============ 列表 ============ */
const loading = ref(false)
const records = ref<ApiComponentInfo[]>([])
const total = ref(0)
const query = reactive({ name: '', page: 1, size: 10 })

async function loadList() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getComponentList({
      projectId,
      name: query.name.trim() || undefined,
      page: query.page,
      size: query.size
    })
    records.value = res.records.map((c) => ({ ...c, stepCount: c.steps?.length ?? 0 }))
    total.value = res.total
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function resetQuery() {
  query.name = ''
  query.page = 1
  loadList()
}

/* ============ 视图切换 ============ */
const viewMode = ref<'list' | 'edit'>('list')
const editingId = ref<number | null>(null)

function backToList() {
  viewMode.value = 'list'
  editingId.value = null
  loadList()
}

/* ============ 类型选择 ============ */
const typeDialogVisible = ref(false)
const typeChosen = ref<'' | 'api'>('')

function openCreate() {
  typeChosen.value = ''
  typeDialogVisible.value = true
}

function onOtherTypeClick() {
  ElMessage.info('其他类型（生成随机数）暂未实现')
}

function confirmType() {
  if (typeChosen.value !== 'api') return
  typeDialogVisible.value = false
  openEditor(null)
}

/* ============ 编辑表单（基础信息 + 步骤） ============ */
const formEl = ref<FormInstance>()
const saving = ref(false)

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

/* 组件级字段（仅单接口组件生效）：与唯一接口步骤实时双向同步 */
const compReturnVar = ref('')
const compIsDisabled = ref(0)
const compContinueOnFail = ref(0)

const compReturnVarModel = computed({
  get: () => (singleStep.value ? singleStep.value.responseVar : compReturnVar.value),
  set: (v: string) => {
    if (singleStep.value) singleStep.value.responseVar = v
    else compReturnVar.value = v
  }
})
const compIsDisabledModel = computed({
  get: () => (singleStep.value ? (singleStep.value.isDisabled as number) : compIsDisabled.value),
  set: (v: number) => {
    if (singleStep.value) singleStep.value.isDisabled = v
    else compIsDisabled.value = v
  }
})
const compContinueOnFailModel = computed({
  get: () => (singleStep.value ? (singleStep.value.continueOnFail as number) : compContinueOnFail.value),
  set: (v: number) => {
    if (singleStep.value) singleStep.value.continueOnFail = v
    else compContinueOnFail.value = v
  }
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入组件名称', trigger: 'blur' }]
}

let stepSeq = 1

const apiOptions = ref<ApiInfo[]>([])
const componentOptions = ref<ApiComponentInfo[]>([])
const componentMap = ref<Record<number, string>>({})
const moduleOptions = ref<OptionItem[]>([])

/** 接口步骤数量 / 嵌套步骤数量，用于判定单接口组件 */
const interfaceStepCount = computed(() => form.steps.filter((s) => s.stepType === 1).length)
const nestedStepCount = computed(() => form.steps.filter((s) => s.stepType === 2).length)
/** 单接口组件：恰好 1 个接口步骤且无嵌套 */
const isSingleInterface = computed(() => interfaceStepCount.value === 1 && nestedStepCount.value === 0)
/** 单接口步骤（用于组件级字段回写） */
const singleStep = computed(() => (isSingleInterface.value ? form.steps[0] : null))

const availableComponentOptions = computed(() =>
  componentOptions.value.filter((c) => c.id !== editingId.value)
)

async function loadApiOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    apiOptions.value = []
    return
  }
  try {
    const res = await getApiList({ projectId, page: 1, size: 1000 })
    apiOptions.value = res.records
  } catch {
    apiOptions.value = []
  }
}

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
  compReturnVar.value = ''
  compIsDisabled.value = 0
  compContinueOnFail.value = 0
  form.steps = []
  editingId.value = null
  formEl.value?.clearValidate()
}

async function openEditor(row: ApiComponentInfo | null) {
  await Promise.all([loadApiOptions(), loadComponentOptions(), loadModuleOptions()])
  resetForm()
  if (row) {
    editingId.value = row.id
    form.name = row.name
    form.description = row.description || ''
    form.moduleId = row.moduleId ?? null
    try {
      const detail = await getComponentDetail(row.id)
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
      // 单接口组件：基础信息组件级字段通过 computed 直接读写唯一接口步骤，无需额外回填
    } catch {
      form.steps = []
    }
  }
  viewMode.value = 'edit'
}

async function openEdit(row: ApiComponentInfo) {
  await openEditor(row)
}

/* ============ 步骤增删改 ============ */
function addStep(stepType: number) {
  const blank: StepEdit = {
    _id: stepSeq++,
    stepType,
    apiId: null,
    childComponentId: null,
    stepName: '',
    responseVar: '',
    isDisabled: 0,
    continueOnFail: 0,
    description: '',
    requestHeaders: '',
    requestBody: '',
    assertions: []
  }
  form.steps.push(blank)
  editStep(form.steps.length - 1)
}

function editStep(index: number) {
  const s = form.steps[index]
  stepEditIndex.value = index
  stepForm.stepType = s.stepType
  stepForm.apiId = s.apiId
  stepForm.childComponentId = s.childComponentId
  stepForm.stepName = s.stepName
  stepForm.responseVar = s.responseVar
  stepForm.isDisabled = s.isDisabled
  stepForm.continueOnFail = s.continueOnFail
  stepForm.description = s.description
  stepForm.apiName = s.apiName || ''
  stepForm.apiMethod = s.apiMethod || ''
  stepForm.apiPath = s.apiPath || ''
  stepForm.childComponentName = s.childComponentName || ''
  stepForm.requestHeaders = s.requestHeaders
  stepForm.requestBody = s.requestBody
  stepForm.assertions = s.assertions.map((a) => ({ ...a }))
  stepRequestTab.value = 'headers'
  stepDialogVisible.value = true
}

function moveStep(index: number, dir: number) {
  const target = index + dir
  if (target < 0 || target >= form.steps.length) return
  ;[form.steps[index], form.steps[target]] = [form.steps[target], form.steps[index]]
}

function removeStep(index: number) {
  form.steps.splice(index, 1)
}

/* ============ 步骤编辑弹框 ============ */
const stepDialogVisible = ref(false)
const stepEditIndex = ref(-1)
const stepRequestTab = ref('headers')

const stepForm = reactive({
  stepType: 1 as number,
  apiId: null as number | null,
  childComponentId: null as number | null,
  stepName: '',
  responseVar: '',
  isDisabled: 0 as number,
  continueOnFail: 0 as number,
  description: '',
  apiName: '' as string,
  apiMethod: '' as string,
  apiPath: '' as string,
  childComponentName: '' as string,
  requestHeaders: '',
  requestBody: '',
  assertions: [] as AssertionItem[]
})

const BODY_METHODS = ['POST', 'PUT', 'PATCH', 'DELETE']
const showStepBody = computed(() => !!stepForm.apiMethod && BODY_METHODS.includes(stepForm.apiMethod))

function resetStepForm() {
  stepEditIndex.value = -1
  stepForm.stepType = 1
  stepForm.apiId = null
  stepForm.childComponentId = null
  stepForm.stepName = ''
  stepForm.responseVar = ''
  stepForm.isDisabled = 0
  stepForm.continueOnFail = 0
  stepForm.description = ''
  stepForm.apiName = ''
  stepForm.apiMethod = ''
  stepForm.apiPath = ''
  stepForm.childComponentName = ''
  stepForm.requestHeaders = ''
  stepForm.requestBody = ''
  stepForm.assertions = []
}

function onStepApiChange(id: number) {
  const api = apiOptions.value.find((a) => a.id === id)
  stepForm.apiName = api?.name || ''
  stepForm.apiMethod = api?.method || ''
  stepForm.apiPath = api?.path || ''
  if (!stepForm.stepName) stepForm.stepName = api?.name || ''
}

function onStepChildChange(id: number) {
  const c = availableComponentOptions.value.find((x) => x.id === id)
  stepForm.childComponentName = c?.name || ''
  if (!stepForm.stepName) stepForm.stepName = c?.name || ''
}

function addStepAssertion() {
  stepForm.assertions.push({ type: 'status' })
}

function removeStepAssertion(index: number) {
  stepForm.assertions.splice(index, 1)
}

function showExpected(a: AssertionItem): boolean {
  if (a.type === 'jsonPath' || a.type === 'header') return a.operator !== 'exists'
  return true
}

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

function saveStep() {
  if (stepForm.stepType === 1 && !stepForm.apiId) {
    ElMessage.error('请选择接口')
    return
  }
  if (stepForm.stepType === 2) {
    if (!stepForm.childComponentId) {
      ElMessage.error('请选择嵌套组件')
      return
    }
    if (stepForm.childComponentId === editingId.value) {
      ElMessage.error('组合组件不能嵌套自身')
      return
    }
  }
  if (stepForm.stepType === 1) {
    if (!isValidJson(stepForm.requestHeaders)) {
      ElMessage.error('请求头不是合法 JSON')
      return
    }
    if (showStepBody.value && !isValidJson(stepForm.requestBody)) {
      ElMessage.error('请求参数不是合法 JSON')
      return
    }
  }

  const target: StepEdit = {
    _id: stepEditIndex.value === -1 ? stepSeq++ : form.steps[stepEditIndex.value]._id,
    stepType: stepForm.stepType,
    apiId: stepForm.stepType === 2 ? null : stepForm.apiId,
    childComponentId: stepForm.stepType === 2 ? stepForm.childComponentId : null,
    stepName: stepForm.stepName,
    responseVar: stepForm.responseVar,
    isDisabled: stepForm.isDisabled,
    continueOnFail: stepForm.continueOnFail,
    description: stepForm.description,
    apiName: stepForm.apiName,
    apiMethod: stepForm.apiMethod,
    apiPath: stepForm.apiPath,
    childComponentName: stepForm.childComponentName,
    requestHeaders: stepForm.requestHeaders,
    requestBody: stepForm.requestBody,
    assertions: stepForm.assertions.map((a) => ({ ...a }))
  }

  if (stepEditIndex.value === -1) {
    form.steps.push(target)
  } else {
    form.steps[stepEditIndex.value] = target
  }
  stepDialogVisible.value = false
}

/* ============ 展示辅助 ============ */
function displayTarget(step: StepEdit): string {
  if (step.stepType === 1) {
    if (step.apiMethod && step.apiPath) return `${step.apiMethod} ${step.apiPath}`
    return step.apiName || '未选择接口'
  }
  return step.childComponentName || '未选择组件'
}

function displayHeaders(step: StepEdit): string {
  if (step.stepType !== 1) return '—（沿用组件自身）'
  return step.requestHeaders?.trim() || '—'
}

function displayBody(step: StepEdit): string {
  if (step.stepType !== 1) return '—（沿用组件自身）'
  return step.requestBody?.trim() || '—'
}

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
  const val = stepForm[field]
  if (!val?.trim()) return
  try {
    stepForm[field] = JSON.stringify(JSON.parse(val), null, 2)
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

/* ============ 保存 ============ */
async function handleSave() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

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
  }

  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }

  // 组件级字段仅单接口组件生效：已通过 computed 实时写入唯一接口步骤，无需此处回写

  const stepsPayload: ComponentStepSaveParams[] = form.steps.map((s, i) => ({
    stepType: s.stepType,
    apiId: s.stepType === 2 ? null : s.apiId,
    childComponentId: s.stepType === 2 ? s.childComponentId : null,
    sortOrder: i + 1,
    stepName: s.stepName.trim() || undefined,
    responseVar: s.responseVar.trim() || undefined,
    requestOverride: buildRequestOverride(s.requestHeaders, s.requestBody) || undefined,
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
    backToList()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

/* ============ 删除 ============ */
async function handleDelete(row: ApiComponentInfo) {
  try {
    await ElMessageBox.confirm(`确定删除组合组件「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteComponent(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.component-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-input {
  width: 220px;
}

.filter-right {
  margin-left: auto;
}

.comp-name {
  font-weight: 500;
  color: #1f2937;
}

.comp-desc {
  color: #606266;
}

.comp-muted {
  color: #c0c4cc;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

/* 编辑视图 */
.editor-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.editor-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.editor-header-right {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.editor-card {
  margin-bottom: 4px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.basic-form {
  max-width: 760px;
}

.form-input {
  max-width: 360px;
}

.level-hint {
  margin-top: 6px;
}

/* 接口步骤头 */
.steps-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.steps-editor-actions {
  display: flex;
  gap: 8px;
}

.steps-hint {
  margin-bottom: 10px;
}

/* 文本框卡片 */
.step-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px 14px;
  background: #fff;
}

.step-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.step-index {
  font-weight: 600;
  color: #409eff;
}

.step-card-ops {
  margin-left: auto;
  display: flex;
  gap: 2px;
}

.step-card-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-wide {
  grid-column: 1 / -1;
}

.field label {
  font-size: 12px;
  color: #909399;
}

/* 类型选择 */
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

/* 步骤弹框 */
.step-form-input {
  max-width: 560px;
}

.api-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-preview-path {
  color: #606266;
  word-break: break-all;
}

.request-tabs {
  width: 560px;
  max-width: 100%;
}

.code-textarea :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.override-body-hint {
  margin-top: 4px;
  width: 560px;
  max-width: 100%;
}

.assertion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 720px;
}

.assertion-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assertion-type {
  width: 120px;
}

.assertion-path {
  width: 200px;
}

.assertion-operator {
  width: 110px;
}

.assertion-expected {
  width: 200px;
}
</style>
