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
        <el-table-column prop="name" label="用例名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="applicationName" label="工程" min-width="80" show-overflow-tooltip />
        <el-table-column prop="versionName" label="版本" min-width="80" show-overflow-tooltip />
        <el-table-column prop="moduleName" label="模块" min-width="80" show-overflow-tooltip />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" size="small" effect="dark">
              P{{ row.level - 1 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="75" align="center">
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
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            <el-button link type="success" :icon="VideoPlay" @click="handleExecute(row)">执行</el-button>
            <el-button link type="primary" :icon="DataLine" @click="handleOpenResult(row)">查看结果</el-button>
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

    <!-- 执行用例 - 选择环境 -->
    <el-dialog
      v-model="executeVisible"
      title="执行用例"
      width="460px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetExecuteDialog"
    >
      <el-descriptions :column="1" border size="small" class="execute-desc">
        <el-descriptions-item label="用例名称">{{ executeTarget?.name }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-width="80px" class="execute-form">
        <el-form-item label="执行环境" required>
          <el-select
            v-model="envId"
            placeholder="请选择执行环境"
            filterable
            :loading="envLoading"
            class="execute-select"
          >
            <el-option
              v-for="env in envOptions"
              :key="env.id"
              :label="env.name"
              :value="env.id"
            />
          </el-select>
          <div v-if="selectedEnvBaseUrl" class="env-base-url">baseUrl：{{ selectedEnvBaseUrl }}</div>
          <div v-else-if="envOptions.length === 0" class="env-empty-tip">
            当前工程下暂无环境，请先到「环境配置」页新增
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" :loading="executeSubmitting" @click="handleExecuteSubmit">开始执行</el-button>
      </template>
    </el-dialog>

    <!-- 执行结果抽屉 -->
    <el-drawer
      v-model="resultVisible"
      :title="`执行结果 · ${resultTarget?.name ?? ''}`"
      size="600px"
      append-to-body
      @closed="handleResultClosed"
    >
      <div v-if="!currentResult" class="result-empty">
        <el-empty description="该用例暂无执行结果，请先点击「执行」" />
      </div>
      <div v-else class="result-body">
        <div class="metric-row">
          <div class="metric-card" :class="currentResult.status === 'SUCCESS' ? 'ok' : 'bad'">
            <div class="metric-label">状态</div>
            <div class="metric-value">{{ currentResult.status === 'SUCCESS' ? '成功' : '失败' }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">耗时</div>
            <div class="metric-value">{{ (currentResult.durationMs / 1000).toFixed(2) }}s</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">轮次</div>
            <div class="metric-value">{{ currentResult.passedRounds }} / {{ currentResult.totalRounds }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">步骤</div>
            <div class="metric-value">{{ resultStepPassed }} / {{ resultStepTotal }}</div>
          </div>
        </div>

        <el-collapse v-model="activeRounds" class="round-collapse">
          <el-collapse-item
            v-for="round in currentResult.rounds"
            :key="round.roundIndex"
            :name="round.roundIndex"
          >
            <template #title>
              <span class="round-title">轮次 {{ round.roundIndex + 1 }}</span>
              <el-tag size="small" :type="round.status === 'SUCCESS' ? 'success' : 'danger'" effect="light">
                {{ round.status === 'SUCCESS' ? '通过' : '失败' }}
              </el-tag>
              <span class="round-meta">步骤 {{ round.passedSteps }} / {{ round.steps.length }}</span>
            </template>

            <div v-for="step in round.steps" :key="step.stepId + '-' + step.sortOrder" class="step-block">
              <div class="step-head">
                <el-tag size="small" :type="statusBadge(step.status).type" effect="light">{{ statusBadge(step.status).text }}</el-tag>
                <span class="step-code" :class="step.statusCode && step.statusCode < 400 ? 'ok' : 'bad'">{{ step.statusCode }}</span>
                <span class="step-duration">{{ step.durationMs }}ms</span>
              </div>

              <div class="detail-block">
                <div class="detail-label">请求路径</div>
                <div class="path-line">
                  <el-tag size="small" :type="methodTagType(step.method)" effect="dark" class="method-inline">{{ step.method }}</el-tag>
                  <span class="path-text">{{ step.url }}</span>
                </div>
              </div>

              <div class="detail-block">
                <div class="detail-label">
                  <span>请求体明细</span>
                  <span v-if="formatBody(step.requestBody).isJson" class="json-hint" @click="copyText(formatBody(step.requestBody).text, '请求体')">复制</span>
                </div>
                <pre v-if="formatBody(step.requestBody).text" class="body-pre">{{ formatBody(step.requestBody).text }}</pre>
                <div v-else class="body-empty">（无请求体）</div>
              </div>

              <div class="detail-block">
                <div class="detail-label">
                  <span>响应体明细</span>
                  <span v-if="formatBody(step.responseBody).isJson" class="json-hint" @click="copyText(formatBody(step.responseBody).text, '响应体')">复制</span>
                </div>
                <pre v-if="formatBody(step.responseBody).text" class="body-pre">{{ formatBody(step.responseBody).text }}</pre>
                <div v-else class="body-empty">（无响应体）</div>
              </div>

              <div v-if="step.assertResults.length" class="detail-block">
                <div class="detail-label">断言结果明细</div>
                <table class="assert-table">
                  <thead>
                    <tr>
                      <th>类型</th>
                      <th>路径</th>
                      <th>操作符</th>
                      <th>期望</th>
                      <th>实际</th>
                      <th class="col-result">结果</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(a, i) in step.assertResults" :key="i">
                      <td>{{ a.type }}</td>
                      <td>{{ a.path || '-' }}</td>
                      <td>{{ a.operator }}</td>
                      <td>{{ a.expected }}</td>
                      <td>{{ a.actual }}</td>
                      <td class="col-result" :class="a.passed ? 'ok' : 'bad'">{{ a.passed ? '✓' : '✗' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div v-if="step.errorMsg" class="step-error">{{ step.errorMsg }}</div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DataLine, Delete, Edit, Plus, Refresh, Search, Upload, VideoPlay } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile, type UploadInstance } from 'element-plus'
import { deleteCase, getCaseList, importCaseHar, updateCaseStatus } from '@/api/case'
import { getModuleOptions, getProjectOptions, getVersionOptions } from '@/api/base'
import { executeCase, getExecutionHistory } from '@/api/execute'
import { getEnvList } from '@/api/env'
import type { CaseExecuteResult, CaseInfo, EnvInfo, OptionItem, StepExecuteResult } from '@/api/types'
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

/* ================= 执行用例 ================= */
const executeVisible = ref(false)
const executeSubmitting = ref(false)
const envLoading = ref(false)
const envOptions = ref<EnvInfo[]>([])
const envId = ref<number | undefined>(undefined)
const executeTarget = ref<CaseInfo | null>(null)

/** 选中环境的 baseUrl（自动跟随） */
const selectedEnvBaseUrl = computed(() => {
  const env = envOptions.value.find((x) => x.id === envId.value)
  return env?.baseUrl || ''
})

/** 点击「执行」：记录目标用例并加载环境选项 */
async function handleExecute(row: CaseInfo) {
  executeTarget.value = row
  envId.value = undefined
  executeVisible.value = true
  await loadEnvOptions()
}

/** 按当前工程加载环境列表 */
async function loadEnvOptions() {
  envLoading.value = true
  try {
    const res = await getEnvList({
      projectId: projectStore.currentProject?.id,
      size: 200
    })
    envOptions.value = res.records
  } catch {
    envOptions.value = []
  } finally {
    envLoading.value = false
  }
}

/** 确认执行 */
async function handleExecuteSubmit() {
  if (!envId.value) {
    ElMessage.warning('请先选择执行环境')
    return
  }
  if (!executeTarget.value) return
  executeSubmitting.value = true
  try {
    const result = await executeCase(executeTarget.value.id, envId.value)
    resultCache.set(executeTarget.value.id, result)
    ElMessage.success('执行完成')
    executeVisible.value = false
  } catch {
    // 接口层已弹出错误提示
  } finally {
    executeSubmitting.value = false
  }
}

/** 关闭弹框重置 */
function resetExecuteDialog() {
  envId.value = undefined
  executeTarget.value = null
}

/* ================= 执行结果 ================= */
/** 用例 id -> 最近一次执行结果（会话级缓存，刷新即清空） */
const resultCache = reactive(new Map<number, CaseExecuteResult>())
const resultVisible = ref(false)
const resultLoading = ref(false)
const resultTarget = ref<CaseInfo | null>(null)
const activeRounds = ref<number[]>([])

/** 当前 drawer 对应的执行结果 */
const currentResult = computed<CaseExecuteResult | null>(() => {
  if (!resultTarget.value) return null
  return resultCache.get(resultTarget.value.id) ?? null
})

/** 步骤总数（跨轮汇总） */
const resultStepTotal = computed(() => {
  if (!currentResult.value) return 0
  return currentResult.value.rounds.reduce((sum, r) => sum + r.steps.length, 0)
})

/** 步骤通过数（跨轮汇总） */
const resultStepPassed = computed(() => {
  if (!currentResult.value) return 0
  return currentResult.value.rounds.reduce((sum, r) => sum + r.passedSteps, 0)
})

/** 点击「查看结果」：从后端读取最近一次执行记录（持久化数据，刷新不丢） */
async function handleOpenResult(row: CaseInfo) {
  resultTarget.value = row
  activeRounds.value = []
  resultVisible.value = true
  resultLoading.value = true
  try {
    const res = await getExecutionHistory(row.id)
    // 仅在有记录时覆盖缓存；无记录则保留本次会话内刚执行的结果（如有）
    if (res) {
      resultCache.set(row.id, res)
    }
  } catch {
    // 接口层已弹出错误提示；保留缓存中的结果（如有）
  } finally {
    resultLoading.value = false
  }
}

/** 关闭抽屉时清空目标用例 */
function handleResultClosed() {
  resultTarget.value = null
}

/** 步骤方法 -> 标签颜色 */
function methodTagType(method: string | null | undefined): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch ((method || '').toUpperCase()) {
    case 'GET': return 'success'
    case 'POST': return 'warning'
    case 'PUT': return 'primary'
    case 'DELETE': return 'danger'
    default: return 'info'
  }
}

/** 步骤状态 -> 展示文本与颜色 */
function statusBadge(status: StepExecuteResult['status']): { text: string; type: 'success' | 'danger' | 'warning' } {
  if (status === 'PASSED') return { text: '成功', type: 'success' }
  if (status === 'FAILED') return { text: '失败', type: 'danger' }
  return { text: '异常', type: 'warning' }
}

/** 格式化请求/响应体：尝试 JSON 美化，失败原样返回；空值返回空文本 */
function formatBody(raw: string | null | undefined): { text: string; isJson: boolean } {
  if (raw === null || raw === undefined || raw.trim() === '') {
    return { text: '', isJson: false }
  }
  try {
    const parsed = JSON.parse(raw)
    return { text: JSON.stringify(parsed, null, 2), isJson: true }
  } catch {
    return { text: raw, isJson: false }
  }
}

/** 复制文本到剪贴板并提示 */
function copyText(text: string, label: string) {
  if (!text) return
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text)
      .then(() => ElMessage.success(`已复制${label}`))
      .catch(() => ElMessage.error('复制失败'))
  } else {
    ElMessage.error('当前环境不支持复制')
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

/* ---------- 执行用例弹框 ---------- */
.execute-desc {
  margin-bottom: 16px;
}

.execute-select {
  width: 100%;
}

.env-base-url {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.env-empty-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #e6a23c;
}

/* ---------- 执行结果抽屉 ---------- */
.result-empty {
  padding: 40px 0;
}

.result-loading-text {
  font-size: 13px;
  color: #909399;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.metric-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 14px;
}

.metric-card.ok .metric-value {
  color: #3b6d11;
}

.metric-card.bad .metric-value {
  color: #a32d2d;
}

.metric-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.metric-value {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

.round-title {
  font-weight: 500;
  margin-right: 10px;
}

.round-meta {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.step-block {
  padding: 12px 0;
  border-bottom: 1px solid #f0f1f3;
}

.step-block:last-child {
  border-bottom: none;
}

.step-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.step-code {
  font-size: 13px;
  font-weight: 600;
}

.step-code.ok {
  color: #3b6d11;
}

.step-code.bad {
  color: #a32d2d;
}

.step-duration {
  font-size: 12px;
  color: #909399;
}

.step-error {
  margin-top: 8px;
  font-size: 12px;
  color: #f56c6c;
  word-break: break-all;
}

.detail-block {
  margin-top: 10px;
}

.detail-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.json-hint {
  color: #3b6d11;
  font-size: 11px;
  cursor: pointer;
  user-select: none;
}

.json-hint:hover {
  text-decoration: underline;
}

.path-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.method-inline {
  flex: none;
}

.path-text {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  word-break: break-all;
}

.body-pre {
  margin: 0;
  padding: 8px 10px;
  background: #f7f8fa;
  border-radius: 6px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow: auto;
}

.body-empty {
  font-size: 12px;
  color: #c0c4cc;
  font-style: italic;
  padding: 4px 0;
}

.assert-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  table-layout: fixed;
}

.assert-table th,
.assert-table td {
  text-align: left;
  padding: 4px 6px;
  border-bottom: 1px solid #f0f1f3;
  word-break: break-all;
}

.assert-table th {
  color: #909399;
  font-weight: 500;
}

.assert-table .col-result {
  width: 48px;
  text-align: center;
}

.assert-table td.ok {
  color: #3b6d11;
  font-weight: 600;
}

.assert-table td.bad {
  color: #a32d2d;
  font-weight: 600;
}
</style>
