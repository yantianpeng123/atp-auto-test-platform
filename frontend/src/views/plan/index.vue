<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">测试计划</span>
          <div class="card-header-actions">
            <el-button type="primary" :icon="Plus" @click="handleCreate">新建计划</el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="计划名称">
            <el-input
              v-model="query.name"
              placeholder="请输入计划名称"
              clearable
              class="filter-input"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.enabled" placeholder="全部" clearable class="filter-select">
              <el-option label="启用" :value="true" />
              <el-option label="停用" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        :data="list"
        v-loading="loading"
        empty-text="暂无测试计划，点击右上角新建"
        style="width: 100%"
      >
        <el-table-column prop="name" label="计划名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="envName" label="执行环境" min-width="120" show-overflow-tooltip />
        <el-table-column prop="caseCount" label="用例数" width="80" align="center" />
        <el-table-column label="定时任务" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.cron" type="info" size="small" effect="plain">{{ row.cron }}</el-tag>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              inline-prompt
              active-text="开"
              inactive-text="关"
              :loading="row._toggling"
              @change="(val: boolean) => handleToggle(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最近执行" min-width="170">
          <template #default="{ row }">
            <span v-if="row.lastRunTime" class="text-muted">{{ row.lastRunTime }}</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            <el-button
              link
              type="success"
              :icon="VideoPlay"
              :loading="row._executing"
              @click="handleExecute(row)"
            >
              执行计划
            </el-button>
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

    <!-- 新建 / 编辑计划弹框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetForm"
    >
      <el-form ref="formEl" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入计划名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="执行环境" prop="envId">
          <el-select
            v-model="form.envId"
            placeholder="请选择执行环境"
            filterable
            class="form-select"
          >
            <el-option v-for="e in envOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联用例" prop="caseIds">
          <el-select
            v-model="form.caseIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择关联用例"
            filterable
            class="form-select"
          >
            <el-option v-for="c in caseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="form-tip">已选 {{ form.caseIds.length }} 个用例，执行时按选择顺序依次运行</div>
        </el-form-item>
        <el-form-item label="Cron 表达式">
          <el-input
            v-model="form.cron"
            placeholder="如 0 0 2 * * ? （留空表示仅手动执行）"
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行计划结果弹框 -->
    <el-dialog v-model="resultVisible" title="执行计划结果" width="780px" append-to-body>
      <div v-if="planResult" class="result-summary">
        <div class="metric-card">
          <div class="metric-label">总用例</div>
          <div class="metric-value">{{ planResult.totalCases }}</div>
        </div>
        <div class="metric-card ok">
          <div class="metric-label">通过</div>
          <div class="metric-value">{{ planResult.passedCases }}</div>
        </div>
        <div class="metric-card bad">
          <div class="metric-label">失败</div>
          <div class="metric-value">{{ planResult.failedCases }}</div>
        </div>
        <div class="metric-card">
          <div class="metric-label">耗时</div>
          <div class="metric-value">{{ (planResult.durationMs / 1000).toFixed(1) }}s</div>
        </div>
      </div>
      <el-table v-if="planResult" :data="planResult.cases" style="width: 100%" max-height="360">
        <el-table-column prop="caseName" label="用例" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '通过' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="center">
          <template #default="{ row }">{{ (row.durationMs / 1000).toFixed(1) }}s</template>
        </el-table-column>
        <el-table-column prop="passedSteps" label="通过步骤" width="95" align="center" />
        <el-table-column prop="failedSteps" label="失败步骤" width="95" align="center" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, VideoPlay } from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import { getEnvList } from '@/api/env'
import type { EnvInfo } from '@/api/types'
import {
  createPlan,
  deletePlan,
  executePlan,
  getPlanCaseOptions,
  getPlanList,
  togglePlanEnabled,
  updatePlan
} from '@/api/plan'
import type { PlanExecuteResult, TestPlanInfo, TestPlanQuery } from '@/api/plan'

const projectStore = useProjectStore()

const loading = ref(false)
const list = ref<TestPlanInfo[]>([])
const total = ref(0)
const query = reactive<TestPlanQuery>({ name: '', enabled: undefined, page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogTitle = ref('新建计划')
const submitting = ref(false)
const formEl = ref<FormInstance>()

interface PlanFormState {
  id?: number
  name: string
  envId: number | undefined
  caseIds: number[]
  cron: string
  enabled: boolean
}
const form = reactive<PlanFormState>({
  id: undefined,
  name: '',
  envId: undefined,
  caseIds: [],
  cron: '',
  enabled: true
})

const envOptions = ref<EnvInfo[]>([])
const caseOptions = ref<{ id: number; name: string }[]>([])

const resultVisible = ref(false)
const planResult = ref<PlanExecuteResult | null>(null)

const rules: FormRules<PlanFormState> = {
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  envId: [{ required: true, message: '请选择执行环境', trigger: 'change' }]
}

onMounted(async () => {
  await loadEnv()
  await loadCases()
  await loadList()
})

async function loadEnv() {
  try {
    const res = await getEnvList({ projectId: projectStore.currentProject?.id, page: 1, size: 200 })
    envOptions.value = res.records
  } catch {
    envOptions.value = []
  }
}

async function loadCases() {
  const pid = projectStore.currentProject?.id
  if (pid) caseOptions.value = await getPlanCaseOptions(pid)
}

async function loadList() {
  loading.value = true
  try {
    // 项目隔离：仅查询当前项目下的计划
    const projectId = projectStore.currentProject?.id
    const res = await getPlanList({ ...query, projectId })
    list.value = res.records
    total.value = res.total
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
  query.enabled = undefined
  handleSearch()
}

function handleCreate() {
  dialogTitle.value = '新建计划'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: TestPlanInfo) {
  dialogTitle.value = '编辑计划'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    envId: row.envId,
    caseIds: [],
    cron: row.cron || '',
    enabled: row.enabled
  })
  dialogVisible.value = true
}

function resetForm() {
  formEl.value?.clearValidate()
  Object.assign(form, {
    id: undefined,
    name: '',
    envId: undefined,
    caseIds: [],
    cron: '',
    enabled: true
  })
}

async function handleSubmit() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      id: form.id,
      name: form.name,
      projectId: projectStore.currentProject?.id ?? 0,
      envId: form.envId!,
      caseIds: form.caseIds,
      cron: form.cron,
      enabled: form.enabled
    }
    if (form.id) await updatePlan(payload)
    else await createPlan(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: TestPlanInfo) {
  try {
    await ElMessageBox.confirm(`确定删除计划「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deletePlan(row.id)
  ElMessage.success('删除成功')
  await loadList()
}

async function handleToggle(row: TestPlanInfo, val: boolean) {
  row._toggling = true
  try {
    await togglePlanEnabled(row.id, val)
    row.enabled = val
  } finally {
    row._toggling = false
  }
}

async function handleExecute(row: TestPlanInfo) {
  row._executing = true
  try {
    planResult.value = await executePlan(row.id)
    resultVisible.value = true
  } finally {
    row._executing = false
  }
}
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

.form-select {
  width: 100%;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.text-muted {
  color: #9ca3af;
}

/* ---------- 执行结果弹框 ---------- */
.result-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 18px;
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
</style>
