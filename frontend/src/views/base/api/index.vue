<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">接口列表</span>
          <el-dropdown trigger="click" @command="handleCreateCommand">
            <el-button type="primary">
              接口新增
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="manual">手动新增</el-dropdown-item>
                <el-dropdown-item command="jar">Jar包导入</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>

      <!-- 查询条件区 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="工程名称">
            <el-select
              v-model="query.applicationId"
              placeholder="请选择工程"
              clearable
              filterable
              class="filter-select"
              @change="handleApplicationChange"
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

          <el-form-item label="接口名称">
            <el-input
              v-model="query.name"
              placeholder="请输入接口名称"
              clearable
              class="filter-input"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="请求路径">
            <el-input
              v-model="query.path"
              placeholder="请输入请求路径"
              clearable
              class="filter-input"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 列表 -->
      <el-table :data="list" v-loading="loading" empty-text="暂无接口数据" style="width: 100%">
        <el-table-column prop="method" label="方法" width="90">
          <template #default="{ row }">
            <el-tag :type="methodTagType(row.method)" size="small" effect="plain">{{ row.method }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="接口名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="path" label="请求路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applicationName" label="所属工程" min-width="120" />
        <el-table-column prop="versionName" label="所属版本" min-width="120" />
        <el-table-column prop="moduleName" label="所属模块" min-width="120" />
        <el-table-column prop="sourceFlag" label="接口来源" width="170" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleOpenEdit(row)">编辑</el-button>
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

    <!-- 新增/编辑接口弹框（分标签页） -->
    <el-dialog
      v-model="manualVisible"
      :title="isEdit ? '编辑接口' : '手动新增接口'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="manualFormEl" :model="manualForm" :rules="manualRules" label-width="90px">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-form-item v-if="!isEdit" label="所属工程" prop="applicationId">
              <el-select
                v-model="manualForm.applicationId"
                placeholder="请选择工程"
                clearable
                filterable
                class="dialog-select"
                @change="handleManualApplicationChange"
              >
                <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="!isEdit" label="所属版本" prop="versionId">
              <el-select
                v-model="manualForm.versionId"
                placeholder="请选择版本"
                clearable
                filterable
                class="dialog-select"
                @change="handleManualVersionChange"
              >
                <el-option v-for="item in manualVersionOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="!isEdit" label="所属模块" prop="moduleId">
              <el-select
                v-model="manualForm.moduleId"
                placeholder="请选择模块"
                clearable
                filterable
                class="dialog-select"
              >
                <el-option v-for="item in manualModuleOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="接口名称" prop="name">
              <el-input v-model="manualForm.name" placeholder="请输入接口名称" clearable maxlength="100" />
            </el-form-item>

            <el-form-item label="请求方法" prop="method">
              <el-select v-model="manualForm.method" placeholder="请选择请求方法" class="dialog-select">
                <el-option v-for="m in methods" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>

            <el-form-item label="请求路径" prop="path">
              <el-input v-model="manualForm.path" placeholder="请输入请求路径" clearable maxlength="500" />
            </el-form-item>

            <el-form-item label="描述">
              <el-input
                v-model="manualForm.description"
                type="textarea"
                :rows="3"
                placeholder="请输入描述"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-tab-pane>

          <!-- 请求头模板 -->
          <el-tab-pane label="请求头模板" name="headers">
            <el-form-item label="请求头模板">
              <el-input
                v-model="manualForm.headers"
                type="textarea"
                :rows="14"
                placeholder='JSON 格式，如：&#10;{&#10;  "Content-Type": "application/json",&#10;  "Authorization": "Bearer ${token}"&#10;}'
                class="code-textarea"
              />
            </el-form-item>
            <div class="json-hint">
              <el-text type="info" size="small">
                请求头模板为 JSON 格式，第三阶段执行引擎发起请求时自动注入
              </el-text>
            </div>
          </el-tab-pane>

          <!-- 请求体模板 -->
          <el-tab-pane label="请求体模板" name="body">
            <el-form-item label="请求体模板">
              <el-input
                v-model="manualForm.body"
                type="textarea"
                :rows="14"
                placeholder='JSON 格式，如：&#10;{&#10;  "username": "admin",&#10;  "password": "123456"&#10;}'
                class="code-textarea"
              />
            </el-form-item>
            <div class="json-hint">
              <el-text type="info" size="small">
                请求体模板为 JSON 格式，用例可引用此模板并覆盖字段
              </el-text>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualSubmitting" @click="handleManualSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Jar包导入弹框 -->
    <el-dialog v-model="jarVisible" title="Jar包导入" width="560px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="所属工程">
          <el-select
            v-model="jarForm.applicationId"
            placeholder="请选择工程"
            clearable
            filterable
            class="dialog-select"
            @change="handleJarApplicationChange"
          >
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属版本">
          <el-select
            v-model="jarForm.versionId"
            placeholder="请选择版本"
            clearable
            filterable
            class="dialog-select"
            @change="handleJarVersionChange"
          >
            <el-option v-for="item in jarVersionOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属模块">
          <el-select
            v-model="jarForm.moduleId"
            placeholder="请选择模块"
            clearable
            filterable
            class="dialog-select"
          >
            <el-option v-for="item in jarModuleOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="Jar包">
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            accept=".jar"
            :limit="1"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将 Jar 包拖到此处，或<em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .jar 文件，大小不超过 100MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="jarVisible = false">取消</el-button>
        <el-button type="primary" :loading="jarSubmitting" @click="handleJarImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ArrowDown, Delete, Edit, Refresh, Search, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createApi,
  deleteApi,
  getApiList,
  getModuleOptions,
  getProjectOptions,
  getVersionOptions,
  importApiJar,
  updateApi
} from '@/api/base'
import type { ApiInfo, OptionItem } from '@/api/types'
import { useProjectStore } from '@/stores/project'

const projectStore = useProjectStore()

const loading = ref(false)
const list = ref<ApiInfo[]>([])
const total = ref(0)

const projectOptions = ref<OptionItem[]>([])
const versionOptions = ref<OptionItem[]>([])
const moduleOptions = ref<OptionItem[]>([])

const methods = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']

const methodTagMap: Record<string, 'success' | 'primary' | 'warning' | 'danger' | 'info'> = {
  GET: 'success',
  POST: 'primary',
  PUT: 'warning',
  DELETE: 'danger',
  PATCH: 'info'
}

function methodTagType(method: string) {
  return methodTagMap[method] || 'info'
}

const query = reactive({
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  moduleId: undefined as number | undefined,
  name: '',
  path: '',
  page: 1,
  size: 10
})

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
    const result = await getApiList({
      projectId: projectStore.currentProject?.id,
      applicationId: query.applicationId,
      versionId: query.versionId,
      moduleId: query.moduleId,
      name: query.name || undefined,
      path: query.path || undefined,
      page: query.page,
      size: query.size
    })
    list.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleApplicationChange() {
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
  query.name = ''
  query.path = ''
  query.page = 1
  versionOptions.value = []
  moduleOptions.value = []
  handleSearch()
}

/* ==================== 接口新增 / 编辑（弹框） ==================== */

const manualVisible = ref(false)
const jarVisible = ref(false)
const manualSubmitting = ref(false)
const jarSubmitting = ref(false)
const manualFormEl = ref<FormInstance>()
const editingId = ref<number | null>(null)
const activeTab = ref('basic')

const manualVersionOptions = ref<OptionItem[]>([])
const manualModuleOptions = ref<OptionItem[]>([])
const jarVersionOptions = ref<OptionItem[]>([])
const jarModuleOptions = ref<OptionItem[]>([])

const isEdit = computed(() => editingId.value !== null)

const manualForm = reactive({
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  moduleId: undefined as number | undefined,
  name: '',
  method: 'GET',
  path: '',
  headers: '',
  body: '',
  description: ''
})

const jarForm = reactive({
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  moduleId: undefined as number | undefined
})

const jarFile = ref<File>()

const manualRules: FormRules = {
  applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
  versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  moduleId: [{ required: true, message: '请选择模块', trigger: 'change' }],
  name: [{ required: true, message: '请输入接口名称', trigger: 'blur' }],
  method: [{ required: true, message: '请选择请求方法', trigger: 'change' }],
  path: [{ required: true, message: '请输入请求路径', trigger: 'blur' }]
}

function resetManualForm() {
  manualForm.applicationId = undefined
  manualForm.versionId = undefined
  manualForm.moduleId = undefined
  manualForm.name = ''
  manualForm.method = 'GET'
  manualForm.path = ''
  manualForm.headers = ''
  manualForm.body = ''
  manualForm.description = ''
  manualVersionOptions.value = []
  manualModuleOptions.value = []
  activeTab.value = 'basic'
}

function resetJarForm() {
  jarForm.applicationId = undefined
  jarForm.versionId = undefined
  jarForm.moduleId = undefined
  jarVersionOptions.value = []
  jarModuleOptions.value = []
}

function handleCreateCommand(command: string | number | object) {
  if (command === 'manual') {
    editingId.value = null
    resetManualForm()
    manualVisible.value = true
    manualFormEl.value?.clearValidate()
  } else if (command === 'jar') {
    resetJarForm()
    jarVisible.value = true
  }
}

function handleOpenEdit(row: ApiInfo) {
  editingId.value = row.id
  manualForm.applicationId = row.applicationId
  manualForm.versionId = row.versionId
  manualForm.moduleId = row.moduleId
  manualForm.name = row.name
  manualForm.method = row.method
  manualForm.path = row.path
  manualForm.headers = row.headers || ''
  manualForm.body = row.body || ''
  manualForm.description = row.description || ''
  activeTab.value = 'basic'
  // 加载弹窗内版本/模块选项
  if (row.applicationId) {
    getVersionOptions(row.applicationId).then((v) => (manualVersionOptions.value = v))
  }
  if (row.versionId) {
    getModuleOptions(row.versionId).then((v) => (manualModuleOptions.value = v))
  }
  manualVisible.value = true
  manualFormEl.value?.clearValidate()
}

async function handleManualApplicationChange() {
  manualForm.versionId = undefined
  manualForm.moduleId = undefined
  manualVersionOptions.value = []
  manualModuleOptions.value = []
  if (manualForm.applicationId) {
    manualVersionOptions.value = await getVersionOptions(manualForm.applicationId)
  }
}

async function handleManualVersionChange() {
  manualForm.moduleId = undefined
  manualModuleOptions.value = []
  if (manualForm.versionId) {
    manualModuleOptions.value = await getModuleOptions(manualForm.versionId)
  }
}

async function handleJarApplicationChange() {
  jarForm.versionId = undefined
  jarForm.moduleId = undefined
  jarVersionOptions.value = []
  jarModuleOptions.value = []
  if (jarForm.applicationId) {
    jarVersionOptions.value = await getVersionOptions(jarForm.applicationId)
  }
}

async function handleJarVersionChange() {
  jarForm.moduleId = undefined
  jarModuleOptions.value = []
  if (jarForm.versionId) {
    jarModuleOptions.value = await getModuleOptions(jarForm.versionId)
  }
}

function handleFileChange(file: { raw?: File }) {
  jarFile.value = file.raw || undefined
}

/** 简单 JSON 格式校验 */
function isValidJson(str: string): boolean {
  if (!str.trim()) return true
  try {
    JSON.parse(str)
    return true
  } catch {
    return false
  }
}

async function handleManualSubmit() {
  if (!manualFormEl.value) return
  const valid = await manualFormEl.value.validate().catch(() => false)
  if (!valid) return

  // JSON 格式校验
  if (manualForm.headers.trim() && !isValidJson(manualForm.headers)) {
    ElMessage.error('请求头模板不是合法的 JSON')
    activeTab.value = 'headers'
    return
  }
  if (manualForm.body.trim() && !isValidJson(manualForm.body)) {
    ElMessage.error('请求体模板不是合法的 JSON')
    activeTab.value = 'body'
    return
  }

  manualSubmitting.value = true
  try {
    if (isEdit.value && editingId.value !== null) {
      await updateApi({
        id: editingId.value,
        name: manualForm.name,
        method: manualForm.method,
        path: manualForm.path,
        headers: manualForm.headers.trim() || undefined,
        body: manualForm.body.trim() || undefined,
        description: manualForm.description.trim() || undefined
      })
      ElMessage.success('接口修改成功')
    } else {
      await createApi({
        moduleId: manualForm.moduleId!,
        name: manualForm.name,
        method: manualForm.method,
        path: manualForm.path,
        headers: manualForm.headers.trim() || undefined,
        body: manualForm.body.trim() || undefined,
        description: manualForm.description.trim() || undefined
      })
      ElMessage.success('接口新增成功')
    }
    manualVisible.value = false
    handleSearch()
  } finally {
    manualSubmitting.value = false
  }
}

async function handleDelete(row: ApiInfo) {
  try {
    await ElMessageBox.confirm(`确定删除接口「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteApi(row.id)
  ElMessage.success('接口删除成功')
  handleSearch()
}

async function handleJarImport() {
  if (!jarForm.moduleId) {
    ElMessage.warning('请选择所属模块')
    return
  }
  if (!jarFile.value) {
    ElMessage.warning('请选择 Jar 包文件')
    return
  }
  jarSubmitting.value = true
  try {
    const count = await importApiJar(jarForm.moduleId, jarFile.value)
    ElMessage.success(`成功导入 ${count} 个接口`)
    jarVisible.value = false
    handleSearch()
  } finally {
    jarSubmitting.value = false
  }
}

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
  width: 180px;
}

.filter-input {
  width: 200px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.dialog-select {
  width: 100%;
}

/* ---------- 表单标签页 ---------- */
.code-textarea :deep(.el-textarea__inner) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.json-hint {
  margin-top: -8px;
  margin-bottom: 12px;
  padding-left: 4px;
}

:deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
</style>
