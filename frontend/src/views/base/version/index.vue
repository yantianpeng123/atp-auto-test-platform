<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">工程版本信息</span>
          <el-button type="primary" :icon="Plus" @click="handleOpenCreate">新增</el-button>
        </div>
      </template>

      <!-- 查询条件区（保持不变） -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="工程名称">
            <el-select
              v-model="query.applicationId"
              placeholder="请选择工程"
              clearable
              filterable
              class="filter-select"
              @change="handleProjectChange"
            >
              <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="版本名称">
            <el-select
              v-model="query.versionId"
              placeholder="请选择版本"
              clearable
              filterable
              class="filter-select"
              @change="handleVersionChange"
            >
              <el-option v-for="item in versionOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="模块名称">
            <el-select
              v-model="query.moduleId"
              placeholder="请选择模块"
              clearable
              filterable
              class="filter-select"
              @change="handleSearch"
            >
              <el-option v-for="item in moduleOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 结果列表：工程/版本跨行合并 -->
      <el-table
        :data="list"
        v-loading="loading"
        empty-text="暂无数据，请调整查询条件后重试"
        style="width: 100%"
        :span-method="objectSpanMethod"
        border
      >
        <el-table-column prop="applicationName" label="工程名称" min-width="160" />
        <el-table-column prop="versionName" label="版本名称" min-width="140">
          <template #default="{ row }">
            <span v-if="row.versionName">{{ row.versionName }}</span>
            <span v-else class="empty-cell">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="moduleName" label="模块名称" min-width="160">
          <template #default="{ row }">
            <span v-if="row.moduleName">{{ row.moduleName }}</span>
            <span v-else class="empty-cell">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>

    <!-- 新增 / 编辑工程 共用弹窗（编辑时类型为「工程」且置灰） -->
    <el-dialog
      v-model="dialogVisible"
      :title="editMode ? '编辑工程' : '新增'"
      width="520px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formEl" :model="form" :rules="rules" label-width="90px" class="create-form">
        <el-form-item label="新增类型">
          <el-radio-group v-model="form.type" :disabled="editMode" @change="handleTypeChange">
            <el-radio v-for="item in typeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.type === 'PROJECT'" label="工程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入工程名称" clearable maxlength="100" />
        </el-form-item>

        <el-form-item v-if="form.type !== 'PROJECT'" label="工程名称" prop="applicationId">
          <el-select
            v-model="form.applicationId"
            placeholder="请选择工程"
            clearable
            filterable
            class="filter-select"
            @change="handleDialogProjectChange"
          >
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="工程描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入工程描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item v-if="form.type === 'VERSION'" label="版本名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入版本名称" clearable maxlength="100" />
        </el-form-item>

        <el-form-item v-if="form.type === 'MODULE'" label="版本名称" prop="versionId">
          <el-select
            v-model="form.versionId"
            placeholder="请选择版本"
            clearable
            filterable
            class="filter-select"
          >
            <el-option v-for="item in dialogVersionOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.type === 'MODULE'" label="模块名称" prop="moduleName">
          <div class="module-add">
            <el-input
              v-model="form.moduleName"
              placeholder="请输入模块名称"
              clearable
              maxlength="100"
              @keyup.enter="handleAddModule"
            />
            <el-button type="primary" plain :icon="Plus" @click="handleAddModule">添加模块</el-button>
          </div>
          <div v-if="moduleList.length" class="module-list">
            <div v-for="(_, index) in moduleList" :key="index" class="module-row">
              <el-input v-model="moduleList[index]" maxlength="100" placeholder="模块名称" />
              <el-button link type="danger" :icon="Delete" @click="handleRemoveModule(index)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗（工程/版本/模块通用） -->
    <el-dialog
      v-model="renameVisible"
      :title="renameTitle"
      width="480px"
      :close-on-click-modal="false"
      @closed="renameForm.name = ''"
    >
      <el-form label-width="90px" class="create-form">
        <el-form-item label="模块名称" prop="name">
          <el-input v-model="renameForm.name" placeholder="请输入模块名称" maxlength="100" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="renameLoading" @click="handleRenameConfirm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createModules,
  createProject,
  createVersion,
  deleteModule,
  deleteProject,
  deleteVersion,
  getModuleOptions,
  getProjectOptions,
  getVersionList,
  getVersionOptions,
  updateModule,
  updateProject,
  updateVersion
} from '@/api/base'
import type { OptionItem, VersionInfo } from '@/api/types'
import { useProjectStore } from '@/stores/project'

const loading = ref(false)
const list = ref<VersionInfo[]>([])
const total = ref(0)

const projectStore = useProjectStore()

// 下拉选项数据（从接口获取）
const projectOptions = ref<OptionItem[]>([])
const versionOptions = ref<OptionItem[]>([])
const moduleOptions = ref<OptionItem[]>([])

const query = reactive({
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  moduleId: undefined as number | undefined,
  page: 1,
  size: 10
})

/* ==================== 合并单元格 ==================== */

// 合并单元格所需的 rowspan 数组（与 list 下标一一对应）
const appSpan = ref<number[]>([])
const versionSpan = ref<number[]>([])

// 排序键：null 视为最大，保证分组相邻且稳定
function sortKey(v: number | null | undefined): number {
  return v == null ? Number.MAX_SAFE_INTEGER : v
}

// 按 工程 → 版本 → 模块 排序，保证 span-method 合并时相邻
function sortForMerge(rows: VersionInfo[]): VersionInfo[] {
  return [...rows].sort(
    (a, b) =>
      sortKey(a.applicationId) - sortKey(b.applicationId) ||
      sortKey(a.versionId) - sortKey(b.versionId) ||
      sortKey(a.moduleId) - sortKey(b.moduleId)
  )
}

// 计算工程列、版本列的 rowspan
function computeSpans(rows: VersionInfo[]): { app: number[]; ver: number[] } {
  const app = new Array(rows.length).fill(1)
  const ver = new Array(rows.length).fill(1)
  let i = 0
  while (i < rows.length) {
    // 同一工程分组
    let j = i
    while (j < rows.length && rows[j].applicationId === rows[i].applicationId) j++
    app[i] = j - i
    for (let k = i + 1; k < j; k++) app[k] = 0
    // 工程内按版本分组
    let v = i
    while (v < j) {
      let w = v
      while (w < j && rows[w].versionId === rows[v].versionId) w++
      ver[v] = w - v
      for (let k = v + 1; k < w; k++) ver[k] = 0
      v = w
    }
    i = j
  }
  return { app, ver }
}

// el-table span-method：第 0 列合并工程，第 1 列合并版本
function objectSpanMethod({ rowIndex, columnIndex }: any): number[] {
  if (columnIndex === 0) return [appSpan.value[rowIndex] ?? 1, 1]
  if (columnIndex === 1) return [versionSpan.value[rowIndex] ?? 1, 1]
  return [1, 1]
}

async function loadProjectOptions() {
  projectOptions.value = await getProjectOptions(projectStore.currentProject?.id)
}

async function loadVersionOptions(applicationId?: number) {
  versionOptions.value = await getVersionOptions(applicationId)
}

async function loadModuleOptions(versionId?: number) {
  moduleOptions.value = await getModuleOptions(versionId)
}

async function handleSearch() {
  loading.value = true
  try {
    const result = await getVersionList({
      projectId: projectStore.currentProject?.id,
      applicationId: query.applicationId,
      versionId: query.versionId,
      moduleId: query.moduleId,
      page: query.page,
      size: query.size
    })
    const sorted = sortForMerge(result.records || [])
    const spans = computeSpans(sorted)
    appSpan.value = spans.app
    versionSpan.value = spans.ver
    list.value = sorted
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleProjectChange() {
  query.versionId = undefined
  query.moduleId = undefined
  versionOptions.value = []
  moduleOptions.value = []
  loadVersionOptions(query.applicationId)
  handleSearch()
}

function handleVersionChange() {
  query.moduleId = undefined
  moduleOptions.value = []
  loadModuleOptions(query.versionId)
  handleSearch()
}

function handleReset() {
  query.applicationId = undefined
  query.versionId = undefined
  query.moduleId = undefined
  query.page = 1
  versionOptions.value = []
  moduleOptions.value = []
  handleSearch()
}

/* ==================== 编辑 / 删除 ==================== */

const renameVisible = ref(false)
const renameLoading = ref(false)
const renameForm = reactive({ id: 0, name: '', level: 'module' as RowLevel })

// 行内最具体层级：模块 > 版本 > 工程
type RowLevel = 'project' | 'version' | 'module'
const levelLabelMap: Record<RowLevel, string> = {
  project: '工程',
  version: '版本',
  module: '模块'
}

function rowLevel(row: VersionInfo): RowLevel {
  if (row.moduleId != null) return 'module'
  if (row.versionId != null) return 'version'
  return 'project'
}

const renameTitle = computed(() => `编辑${levelLabelMap[renameForm.level]}`)

function handleEdit(row: VersionInfo) {
  const level = rowLevel(row)
  if (level === 'project') {
    // 仅工程：复用「新增」弹窗，类型锁定为「工程」并置灰，名称/描述可改
    resetForm()
    editMode.value = true
    editId.value = row.applicationId as number
    form.type = 'PROJECT'
    form.name = row.applicationName
    form.description = row.applicationDescription ?? ''
    dialogVisible.value = true
    return
  }
  renameForm.id =
    (level === 'module' ? row.moduleId : row.versionId) as number
  renameForm.name =
    level === 'module' ? row.moduleName : row.versionName
  renameForm.level = level
  renameVisible.value = true
}

async function handleRenameConfirm() {
  const name = renameForm.name.trim()
  if (!name) {
    ElMessage.warning(`${levelLabelMap[renameForm.level]}名称不能为空`)
    return
  }
  renameLoading.value = true
  try {
    const level = renameForm.level
    if (level === 'module') {
      await updateModule({ id: renameForm.id, name })
    } else if (level === 'version') {
      await updateVersion({ id: renameForm.id, name })
    } else {
      await updateProject({ id: renameForm.id, name })
    }
    ElMessage.success(`${levelLabelMap[level]}更新成功`)
    renameVisible.value = false
    await handleSearch()
  } finally {
    renameLoading.value = false
  }
}

async function handleDelete(row: VersionInfo) {
  const level = rowLevel(row)
  const id =
    (level === 'module' ? row.moduleId : level === 'version' ? row.versionId : row.applicationId) as number
  const label = levelLabelMap[level]
  const name =
    level === 'module' ? row.moduleName : level === 'version' ? row.versionName : row.applicationName
  try {
    await ElMessageBox.confirm(`确认删除${label}「${name}」？删除后不可恢复。`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  if (level === 'module') {
    await deleteModule(id)
  } else if (level === 'version') {
    await deleteVersion(id)
  } else {
    await deleteProject(id)
  }
  ElMessage.success(`${label}删除成功`)
  await handleSearch()
}

/* ==================== 新增 / 编辑工程 共用弹窗 ==================== */

const dialogVisible = ref(false)
const submitting = ref(false)
const formEl = ref<FormInstance>()

// 编辑工程模式：复用新增弹窗，类型锁定为「工程」且置灰
const editMode = ref(false)
const editId = ref<number | null>(null)

const typeOptions = [
  { label: '工程', value: 'PROJECT' },
  { label: '版本', value: 'VERSION' },
  { label: '模块', value: 'MODULE' }
]

const form = reactive({
  type: 'PROJECT' as 'PROJECT' | 'VERSION' | 'MODULE',
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  name: '',
  description: '',
  moduleName: ''
})

// 弹窗内版本下拉选项（随工程联动加载）
const dialogVersionOptions = ref<OptionItem[]>([])

const typeLabel = computed(() => {
  const map: Record<string, string> = { PROJECT: '工程', VERSION: '版本', MODULE: '模块' }
  return map[form.type] || ''
})

// 必填校验：被 v-if 隐藏的字段不参与 validate，规则按显示与否自动生效。
const rules = computed<FormRules>(() => {
  if (form.type === 'PROJECT') {
    return { name: [{ required: true, message: '请输入工程名称', trigger: 'blur' }] }
  }
  if (form.type === 'VERSION') {
    return {
      applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
      name: [{ required: true, message: '请输入版本名称', trigger: 'blur' }]
    }
  }
  return {
    applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
    versionId: [{ required: true, message: '请选择版本', trigger: 'change' }]
  }
})

const moduleList = ref<string[]>([])

function handleAddModule() {
  const name = form.moduleName.trim()
  if (!name) {
    ElMessage.warning('模块名称不能为空')
    return
  }
  moduleList.value.push(name)
  form.moduleName = ''
}

function handleRemoveModule(index: number) {
  moduleList.value.splice(index, 1)
}

function resetForm() {
  form.applicationId = undefined
  form.versionId = undefined
  form.name = ''
  form.description = ''
  form.moduleName = ''
  moduleList.value = []
  dialogVersionOptions.value = []
}

function handleOpenCreate() {
  editMode.value = false
  form.type = 'PROJECT'
  resetForm()
  dialogVisible.value = true
}

function handleTypeChange() {
  // 切换类型时清空已填内容并重置校验状态
  editMode.value = false
  resetForm()
  formEl.value?.clearValidate()
}

function handleDialogClosed() {
  editMode.value = false
  resetForm()
  formEl.value?.clearValidate()
}

// 弹窗内：选择工程后联动加载版本下拉
async function handleDialogProjectChange() {
  form.versionId = undefined
  dialogVersionOptions.value = []
  if (form.applicationId) {
    dialogVersionOptions.value = await getVersionOptions(form.applicationId)
  }
}

async function handleSubmit() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  if (form.type === 'MODULE' && moduleList.value.length === 0) {
    ElMessage.warning('请至少添加一个模块名称')
    return
  }

  submitting.value = true
  try {
    if (form.type === 'PROJECT') {
      if (editMode.value) {
        await updateProject({ id: editId.value as number, name: form.name, description: form.description })
      } else {
        await createProject({ projectId: projectStore.currentProject!.id, name: form.name, description: form.description })
      }
    } else if (form.type === 'VERSION') {
      await createVersion({ applicationId: form.applicationId!, name: form.name, description: form.description })
    } else {
      await createModules({ versionId: form.versionId!, names: moduleList.value, description: form.description })
    }
    ElMessage.success(editMode.value ? '工程更新成功' : `${typeLabel.value}新增成功`)
    dialogVisible.value = false
    await loadProjectOptions()
    handleSearch()
  } finally {
    submitting.value = false
  }
}

// 页面加载时：拉取工程下拉选项（版本/模块选项随工程/版本联动加载）
onMounted(async () => {
  await loadProjectOptions()
  handleSearch()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
}

.card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f1f3;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

/* ---------- 查询条件区 ---------- */
.filter-bar {
  padding: 16px 16px 0;
  margin-bottom: 16px;
  background: #fafbfc;
  border: 1px solid #f0f1f3;
  border-radius: 8px;
}

.filter-form :deep(.el-form-item) {
  margin-right: 18px;
  margin-bottom: 16px;
}

.filter-form :deep(.el-form-item__label) {
  color: #374151;
  font-weight: 500;
}

.filter-select {
  width: 200px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

/* ---------- 新增弹窗 ---------- */
.create-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.module-add {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.module-add :deep(.el-input) {
  flex: 1;
}

.module-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.module-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.module-row :deep(.el-input) {
  flex: 1;
}

/* 列表空值占位 */
.empty-cell {
  color: #c0c4cc;
}
</style>
