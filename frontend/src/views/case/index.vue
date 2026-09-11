<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">用例管理</span>
          <div class="card-header-actions">
            <el-button type="primary" :icon="Plus" @click="handleCreate">新增用例</el-button>
            <el-button type="primary" :icon="Upload" @click="openImportDialog">导入案例</el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件区 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="用例名称">
            <el-input
              v-model="query.name"
              placeholder="请输入用例名称"
              clearable
              class="filter-input"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="优先级">
            <el-select v-model="query.level" placeholder="全部" clearable class="filter-select">
              <el-option label="P0" :value="1" />
              <el-option label="P1" :value="2" />
              <el-option label="P2" :value="3" />
            </el-select>
          </el-form-item>

          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable class="filter-select">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="list" v-loading="loading" empty-text="暂无用例，点击右上角新增" style="width: 100%">
        <el-table-column prop="name" label="用例名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="applicationName" label="工程" min-width="120" show-overflow-tooltip />
        <el-table-column prop="versionName" label="版本" min-width="120" show-overflow-tooltip />
        <el-table-column prop="moduleName" label="模块" min-width="120" show-overflow-tooltip />
        <el-table-column label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" size="small" effect="dark">
              P{{ row.level - 1 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              inline-prompt
              active-text="启用"
              inactive-text="停用"
              @change="(val: boolean) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="修改时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
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

    <!-- 导入案例弹框 -->
    <el-dialog
      v-model="importVisible"
      title="导入案例"
      width="560px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetImportForm"
    >
      <el-form
        ref="importFormEl"
        :model="importForm"
        :rules="importRules"
        label-width="90px"
      >
        <el-form-item label="工程" prop="applicationId" required>
          <el-select
            v-model="importForm.applicationId"
            placeholder="请选择工程"
            filterable
            class="import-select"
            @change="onImportAppChange"
          >
            <el-option v-for="item in appOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="版本" prop="versionId" required>
          <el-select
            v-model="importForm.versionId"
            placeholder="请选择版本"
            filterable
            :disabled="!importForm.applicationId"
            class="import-select"
            @change="onImportVersionChange"
          >
            <el-option v-for="item in versionOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="模块" prop="moduleId" required>
          <el-select
            v-model="importForm.moduleId"
            placeholder="请选择模块"
            filterable
            :disabled="!importForm.versionId"
            class="import-select"
          >
            <el-option v-for="item in moduleOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="用例名称" prop="caseName" required>
          <el-input
            v-model="importForm.caseName"
            placeholder="用例名称"
            clearable
            maxlength="50"
            class="import-input"
          />
        </el-form-item>

        <el-form-item label="har包" prop="harFile" required>
          <el-upload
            ref="uploadEl"
            :auto-upload="false"
            :limit="1"
            accept=".har"
            :on-change="onHarFileChange"
            :on-remove="onHarFileRemove"
            class="import-upload"
          >
            <el-button :icon="Upload">选择har包文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .har 格式文件，最多上传 1 个</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importSubmitting" @click="handleImportSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, Edit, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile, type UploadInstance } from 'element-plus'
import { deleteCase, getCaseList, importCaseHar, updateCaseStatus } from '@/api/case'
import { getModuleOptions, getProjectOptions, getVersionOptions } from '@/api/base'
import type { CaseInfo, OptionItem } from '@/api/types'
import { useProjectStore } from '@/stores/project'

const router = useRouter()
const projectStore = useProjectStore()

const loading = ref(false)
const list = ref<CaseInfo[]>([])
const total = ref(0)

const query = reactive({
  name: '',
  level: undefined as number | undefined,
  status: undefined as number | undefined,
  page: 1,
  size: 10
})

/** 优先级 -> 标签颜色 */
function levelTagType(level: number): '' | 'danger' | 'warning' | 'info' {
  if (level === 1) return 'danger'
  if (level === 2) return 'warning'
  return 'info'
}

async function loadList() {
  loading.value = true
  try {
    const result = await getCaseList({
      projectId: projectStore.currentProject?.id,
      name: query.name || undefined,
      level: query.level,
      status: query.status,
      page: query.page,
      size: query.size
    })
    list.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  query.name = ''
  query.level = undefined
  query.status = undefined
  query.page = 1
  loadList()
}

/** 新增用例 -> 跳转编辑页 */
function handleCreate() {
  router.push('/case/edit')
}

/** 编辑用例 -> 跳转编辑页（带 id） */
function handleEdit(row: CaseInfo) {
  router.push({ path: '/case/edit', query: { id: String(row.id) } })
}

async function handleToggleStatus(row: CaseInfo, val: boolean) {
  const newStatus = val ? 1 : 0
  try {
    await updateCaseStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 1 ? '用例已启用' : '用例已停用')
  } catch {
    // 接口层已弹出错误提示
  }
}

async function handleDelete(row: CaseInfo) {
  try {
    await ElMessageBox.confirm(`确定删除用例「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteCase(row.id)
  ElMessage.success('用例删除成功')
  loadList()
}

/* ================= 导入案例 ================= */
const importVisible = ref(false)
const importSubmitting = ref(false)
const importFormEl = ref<FormInstance>()
const uploadEl = ref<UploadInstance>()

const appOptions = ref<OptionItem[]>([])
const versionOptions = ref<OptionItem[]>([])
const moduleOptions = ref<OptionItem[]>([])

const importForm = reactive({
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  moduleId: undefined as number | undefined,
  caseName: '',
  harFile: null as File | null
})

const importRules: FormRules = {
  applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
  versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  moduleId: [{ required: true, message: '请选择模块', trigger: 'change' }],
  caseName: [{ required: true, message: '请输入用例名称', trigger: 'blur' }]
}

/** 打开导入弹框，加载工程选项 */
async function openImportDialog() {
  importVisible.value = true
  if (appOptions.value.length === 0) {
    try {
      appOptions.value = await getProjectOptions(projectStore.currentProject?.id)
    } catch {
      appOptions.value = []
    }
  }
}

/** 选择工程 -> 加载版本，重置版本和模块 */
async function onImportAppChange() {
  importForm.versionId = undefined
  importForm.moduleId = undefined
  versionOptions.value = []
  moduleOptions.value = []
  if (!importForm.applicationId) return
  try {
    versionOptions.value = await getVersionOptions(importForm.applicationId)
  } catch {
    versionOptions.value = []
  }
}

/** 选择版本 -> 加载模块，重置模块 */
async function onImportVersionChange() {
  importForm.moduleId = undefined
  moduleOptions.value = []
  if (!importForm.versionId) return
  try {
    moduleOptions.value = await getModuleOptions(importForm.versionId)
  } catch {
    moduleOptions.value = []
  }
}

/** 选择 har 文件（手动上传模式，仅暂存文件） */
function onHarFileChange(file: UploadFile) {
  if (!file.name.toLowerCase().endsWith('.har')) {
    ElMessage.error('仅支持 .har 格式文件')
    uploadEl.value?.clearFiles()
    importForm.harFile = null
    return
  }
  importForm.harFile = (file.raw as File) || null
}

/** 移除 har 文件 */
function onHarFileRemove() {
  importForm.harFile = null
}

/** 重置导入表单 */
function resetImportForm() {
  importForm.applicationId = undefined
  importForm.versionId = undefined
  importForm.moduleId = undefined
  importForm.caseName = ''
  importForm.harFile = null
  versionOptions.value = []
  moduleOptions.value = []
  uploadEl.value?.clearFiles()
  importFormEl.value?.clearValidate()
}

/** 提交导入 */
async function handleImportSubmit() {
  if (!importFormEl.value) return
  const valid = await importFormEl.value.validate().catch(() => false)
  if (!valid) return

  if (!importForm.harFile) {
    ElMessage.warning('请选择要上传的 har 包文件')
    return
  }

  importSubmitting.value = true
  try {
    const count = await importCaseHar(
      importForm.moduleId!,
      importForm.caseName.trim(),
      importForm.harFile
    )
    ElMessage.success(`导入成功，共导入 ${count} 个用例`)
    importVisible.value = false
    handleSearch()
  } catch {
    // 接口层已弹出错误提示
  } finally {
    importSubmitting.value = false
  }
}

onMounted(loadList)
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

.card-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---------- 导入案例弹框 ---------- */
.import-select,
.import-input {
  width: 100%;
}

.import-upload {
  width: 100%;
}

.import-upload :deep(.el-upload__tip) {
  font-size: 12px;
  color: #9ca3af;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

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

.filter-input {
  width: 200px;
}

.filter-select {
  width: 120px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.method-tag {
  margin-right: 6px;
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
</style>
