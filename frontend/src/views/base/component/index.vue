<template>
  <div class="component-page">
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

    <!-- 新增 / 编辑弹框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑组合组件' : '新增组合组件'"
      width="880px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetForm"
    >
      <el-form ref="formEl" :model="form" :rules="rules" label-width="90px">
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
      </el-form>

      <div class="steps-editor">
        <div class="steps-editor-header">
          <span class="section-title">子步骤（单接口 / 嵌套组件，按顺序执行）</span>
          <div class="steps-editor-actions">
            <el-button size="small" :icon="Link" @click="addApiStep">添加接口步骤</el-button>
            <el-button
              size="small"
              :icon="Share"
              :disabled="componentOptions.length === 0"
              @click="addComponentStep"
            >
              添加嵌套组件
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="componentOptions.length === 0"
          type="info"
          :closable="false"
          class="steps-hint"
          title="当前项目还没有其它组合组件，无法嵌套；可先添加“接口步骤”，后续再编辑为嵌套结构。"
        />
        <el-alert
          v-else
          type="warning"
          :closable="false"
          class="steps-hint"
          title="嵌套组件支持多层复用；若选择的组件间接引用了本组件，保存时将由服务端拦截（防环校验）。"
        />

        <el-table :data="form.steps" border size="small" empty-text="请添加子步骤">
          <el-table-column label="#" width="48" align="center">
            <template #default="{ $index }">{{ $index + 1 }}</template>
          </el-table-column>
          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }">
              <el-select v-model="row.stepType" size="small" class="cell-select" @change="onStepTypeChange(row)">
                <el-option :value="1" label="单接口" />
                <el-option :value="2" label="嵌套组件" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="接口 / 组件" min-width="240">
            <template #default="{ row }">
              <el-select
                v-if="row.stepType === 1"
                v-model="row.apiId"
                placeholder="选择接口"
                filterable
                size="small"
                class="cell-select"
                value-key="id"
                @change="(id: number) => onApiChange(row, id)"
              >
                <el-option
                  v-for="api in apiOptions"
                  :key="api.id"
                  :value="api.id"
                  :label="`${api.method} ${api.path}`"
                />
              </el-select>
              <el-select
                v-else
                v-model="row.childComponentId"
                placeholder="选择组合组件"
                filterable
                size="small"
                class="cell-select"
                @change="(id: number) => onChildChange(row, id)"
              >
                <el-option
                  v-for="c in availableComponentOptions"
                  :key="c.id"
                  :value="c.id"
                  :label="c.name"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="步骤名称" width="150">
            <template #default="{ row }">
              <el-input v-model="row.stepName" size="small" placeholder="默认取接口/组件名" />
            </template>
          </el-table-column>
          <el-table-column label="返回变量" width="130">
            <template #default="{ row }">
              <el-input v-model="row.responseVar" size="small" placeholder="如 token" />
            </template>
          </el-table-column>
          <el-table-column label="禁用" width="64" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isDisabled" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="失败继续" width="72" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.continueOnFail" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.description" size="small" placeholder="可选" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ $index }">
              <el-button link type="primary" :icon="Top" size="small" :disabled="$index === 0" @click="moveStep($index, -1)" />
              <el-button link type="primary" :icon="Bottom" size="small" :disabled="$index === form.steps.length - 1" @click="moveStep($index, 1)" />
              <el-button link type="danger" :icon="Delete" size="small" @click="removeStep($index)" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Bottom,
  Delete,
  Edit,
  Link,
  Plus,
  Refresh,
  Search,
  Share,
  Top
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getApiList } from '@/api/base'
import { getComponentList, getComponentDetail, createComponent, updateComponent, deleteComponent } from '@/api/component'
import { useProjectStore } from '@/stores/project'
import type {
  ApiComponentInfo,
  ApiComponentSaveParams,
  ApiInfo,
  ComponentStepSaveParams
} from '@/api/types'

const projectStore = useProjectStore()

const loading = ref(false)
const records = ref<ApiComponentInfo[]>([])
const total = ref(0)

const query = reactive({
  name: '',
  page: 1,
  size: 10
})

/* ---- 列表加载 ---- */
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

/* ---- 弹框 / 表单 ---- */
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formEl = ref<FormInstance>()

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
}

const form = reactive({
  name: '',
  description: '',
  steps: [] as StepEdit[]
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入组件名称', trigger: 'blur' }]
}

let stepSeq = 1

const apiOptions = ref<ApiInfo[]>([])
const componentOptions = ref<ApiComponentInfo[]>([])
const componentMap = ref<Record<number, string>>({})

/** 嵌套组件下拉：编辑时排除自身，避免直接自引用 */
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

function resetForm() {
  form.name = ''
  form.description = ''
  form.steps = []
  editingId.value = null
  formEl.value?.clearValidate()
}

async function openCreate() {
  await Promise.all([loadApiOptions(), loadComponentOptions()])
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row: ApiComponentInfo) {
  await Promise.all([loadApiOptions(), loadComponentOptions()])
  editingId.value = row.id
  form.name = row.name
  form.description = row.description || ''
  try {
    const detail = await getComponentDetail(row.id)
    form.steps = (detail.steps || []).map((s) => {
      const isChild = s.stepType === 2
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
        childComponentName: isChild ? componentMap.value[s.componentId as number] : undefined
      } as StepEdit
    })
  } catch {
    form.steps = []
  }
  dialogVisible.value = true
}

/* ---- 子步骤编辑 ---- */
function addApiStep() {
  form.steps.push({
    _id: stepSeq++,
    stepType: 1,
    apiId: null,
    childComponentId: null,
    stepName: '',
    responseVar: '',
    isDisabled: 0,
    continueOnFail: 0,
    description: ''
  })
}

function addComponentStep() {
  form.steps.push({
    _id: stepSeq++,
    stepType: 2,
    apiId: null,
    childComponentId: null,
    stepName: '',
    responseVar: '',
    isDisabled: 0,
    continueOnFail: 0,
    description: ''
  })
}

function onStepTypeChange(row: StepEdit) {
  // 切换类型时清空另一侧的选择
  if (row.stepType === 1) {
    row.childComponentId = null
    row.childComponentName = undefined
  } else {
    row.apiId = null
    row.apiName = undefined
    row.apiMethod = undefined
    row.apiPath = undefined
  }
}

function onApiChange(row: StepEdit, id: number) {
  const api = apiOptions.value.find((a) => a.id === id)
  row.apiName = api?.name
  row.apiMethod = api?.method
  row.apiPath = api?.path
  if (!row.stepName) row.stepName = api?.name || ''
}

function onChildChange(row: StepEdit, id: number) {
  const c = availableComponentOptions.value.find((x) => x.id === id)
  row.childComponentName = c?.name
  if (!row.stepName) row.stepName = c?.name || ''
}

function moveStep(index: number, dir: number) {
  const target = index + dir
  if (target < 0 || target >= form.steps.length) return
  ;[form.steps[index], form.steps[target]] = [form.steps[target], form.steps[index]]
}

function removeStep(index: number) {
  form.steps.splice(index, 1)
}

/* ---- 保存 ---- */
async function handleSave() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  // 步骤校验
  for (let i = 0; i < form.steps.length; i++) {
    const s = form.steps[i]
    if (s.stepType === 1 && !s.apiId) {
      ElMessage.error(`第 ${i + 1} 步未选择接口`)
      return
    }
    if (s.stepType === 2) {
      if (!s.childComponentId) {
        ElMessage.error(`第 ${i + 1} 步未选择嵌套组件`)
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

  const stepsPayload: ComponentStepSaveParams[] = form.steps.map((s, i) => ({
    stepType: s.stepType,
    apiId: s.stepType === 2 ? null : s.apiId,
    childComponentId: s.stepType === 2 ? s.childComponentId : null,
    sortOrder: i + 1,
    stepName: s.stepName.trim() || undefined,
    responseVar: s.responseVar.trim() || undefined,
    isDisabled: s.isDisabled,
    continueOnFail: s.continueOnFail,
    description: s.description.trim() || undefined
  }))

  const payload: ApiComponentSaveParams = {
    id: editingId.value || undefined,
    projectId,
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
    dialogVisible.value = false
    loadList()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

/* ---- 删除 ---- */
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

/* 弹框内子步骤编辑 */
.steps-editor {
  margin-top: 18px;
  border-top: 1px dashed #e5e7eb;
  padding-top: 14px;
}

.steps-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.steps-editor-actions {
  display: flex;
  gap: 8px;
}

.steps-hint {
  margin-bottom: 10px;
}

.cell-select {
  width: 100%;
}

.form-input {
  max-width: 360px;
}
</style>
