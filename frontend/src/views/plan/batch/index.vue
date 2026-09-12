<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">定时任务</span>
          <div class="card-header-actions">
            <el-button type="primary" :icon="Plus" @click="handleCreate">新建批次</el-button>
          </div>
        </div>
      </template>

      <!-- 查询条件 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="批次名称">
            <el-input
              v-model="query.name"
              placeholder="请输入批次名称"
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
        empty-text="暂无定时任务批次，点击右上角新建"
        style="width: 100%"
      >
        <el-table-column prop="name" label="批次名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="策略" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.strategy === 'PARALLEL' ? 'warning' : 'info'" size="small" effect="plain">
              {{ row.strategy === 'PARALLEL' ? '并行' : '串行' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Cron" min-width="150">
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
        <el-table-column label="最近执行" min-width="180">
          <template #default="{ row }">
            <template v-if="row.lastRunTime">
              <div class="text-muted">{{ row.lastRunTime }}</div>
              <el-tag
                v-if="row.lastRunStatus"
                :type="row.lastRunStatus === 'SUCCESS' ? 'success' : (row.lastRunStatus === 'PARTIAL_FAILED' ? 'warning' : 'danger')"
                size="small"
                effect="plain"
              >
                {{ runStatusText(row.lastRunStatus) }}
              </el-tag>
            </template>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="goDetail(row)">详情</el-button>
            <el-button
              link
              type="success"
              :icon="VideoPlay"
              :loading="row._executing"
              @click="handleExecute(row)"
            >
              执行
            </el-button>
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

    <!-- 新建 / 编辑批次弹框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="620px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetForm"
    >
      <el-form ref="formEl" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="批次名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入批次名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="执行策略" prop="strategy">
          <el-radio-group v-model="form.strategy">
            <el-radio value="PARALLEL">并行</el-radio>
            <el-radio value="SERIAL">串行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="失败继续">
          <el-switch v-model="form.failContinue" active-text="继续" inactive-text="中断" />
          <div class="form-tip">失败后是否继续执行后续计划（主要影响串行模式）</div>
        </el-form-item>
        <el-form-item label="最大并发数">
          <el-input-number v-model="form.maxConcurrency" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="关联计划" prop="planIds">
          <el-select
            v-model="form.planIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择关联测试计划"
            filterable
            class="form-select"
          >
            <el-option v-for="p in planOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <div class="form-tip">已选 {{ form.planIds.length }} 个计划，执行时按选择顺序运行</div>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, VideoPlay, View } from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import type {
  BatchStrategy,
  PlanBatchInfo,
  PlanBatchQuery
} from '@/api/planBatch'

/**
 * ⚠️ 后端 PlanBatch 模块尚未实现。此处以本地 mock 驱动页面演示；
 *    后端就绪后把 USE_MOCK 改为 false，页面自动切换到 @/api/planBatch 真实接口。
 */
const USE_MOCK = true

const router = useRouter()
const projectStore = useProjectStore()

const loading = ref(false)
const list = ref<PlanBatchInfo[]>([])
const total = ref(0)
const query = reactive<PlanBatchQuery>({ name: '', enabled: undefined, page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogTitle = ref('新建批次')
const submitting = ref(false)
const formEl = ref<FormInstance>()

interface BatchFormState {
  id?: number
  name: string
  strategy: BatchStrategy
  failContinue: boolean
  maxConcurrency: number
  planIds: number[]
  cron: string
  enabled: boolean
}
const form = reactive<BatchFormState>({
  id: undefined,
  name: '',
  strategy: 'PARALLEL',
  failContinue: true,
  maxConcurrency: 3,
  planIds: [],
  cron: '',
  enabled: true
})

const planOptions = ref<{ id: number; name: string }[]>([])

const rules: FormRules<BatchFormState> = {
  name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }],
  planIds: [{ required: true, message: '请至少选择一个测试计划', trigger: 'change' }]
}

/* ----------------- mock 演示数据 ----------------- */
const MOCK_PLANS = [
  { id: 1, name: '登录与鉴权用例组' },
  { id: 2, name: '订单核心链路' },
  { id: 3, name: '支付回调校验' },
  { id: 4, name: '消息通知链路' },
  { id: 5, name: '报表导出校验' }
]
const MOCK_BATCHES: PlanBatchInfo[] = [
  {
    id: 1,
    projectId: 1,
    name: '每日回归批次',
    strategy: 'PARALLEL',
    failContinue: true,
    maxConcurrency: 3,
    cron: '0 0 2 * * ?',
    enabled: true,
    lastRunId: 1009,
    lastRunTime: '2026-09-12 02:00:12',
    lastRunStatus: 'PARTIAL_FAILED',
    createTime: '',
    updateTime: ''
  },
  {
    id: 2,
    projectId: 1,
    name: '冒烟测试批次',
    strategy: 'SERIAL',
    failContinue: false,
    maxConcurrency: 1,
    cron: '0 30 9 * * ?',
    enabled: false,
    lastRunId: null,
    lastRunTime: null,
    lastRunStatus: null,
    createTime: '',
    updateTime: ''
  },
  {
    id: 3,
    projectId: 1,
    name: '接口全量校验',
    strategy: 'PARALLEL',
    failContinue: true,
    maxConcurrency: 2,
    cron: '0 0 1 * * ?',
    enabled: true,
    lastRunId: 1005,
    lastRunTime: '2026-09-11 01:00:00',
    lastRunStatus: 'SUCCESS',
    createTime: '',
    updateTime: ''
  },
  {
    id: 4,
    projectId: 1,
    name: '核心链路巡检',
    strategy: 'SERIAL',
    failContinue: false,
    maxConcurrency: 1,
    cron: '0 0 4 * * ?',
    enabled: false,
    lastRunId: null,
    lastRunTime: null,
    lastRunStatus: null,
    createTime: '',
    updateTime: ''
  }
]

onMounted(async () => {
  planOptions.value = MOCK_PLANS
  await loadList()
})

function runStatusText(s: string): string {
  return s === 'SUCCESS' ? '成功' : s === 'PARTIAL_FAILED' ? '部分失败' : s === 'FAILED' ? '失败' : '进行中'
}

async function loadList() {
  loading.value = true
  try {
    const projectId = projectStore.currentProject?.id
    if (USE_MOCK) {
      let data = MOCK_BATCHES
      if (projectId) data = data.filter((b) => b.projectId === projectId)
      if (query.name) data = data.filter((b) => b.name.includes(query.name!))
      if (query.enabled !== undefined) data = data.filter((b) => b.enabled === query.enabled)
      list.value = data
      total.value = data.length
    } else {
      const res = await (await import('@/api/planBatch')).getBatchList({ ...query, projectId })
      list.value = res.records
      total.value = res.total
    }
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

function goDetail(row: PlanBatchInfo) {
  router.push(`/batch/${row.id}`)
}

function handleCreate() {
  dialogTitle.value = '新建批次'
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: PlanBatchInfo) {
  dialogTitle.value = '编辑批次'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    strategy: row.strategy,
    failContinue: row.failContinue,
    maxConcurrency: row.maxConcurrency,
    planIds: [],
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
    strategy: 'PARALLEL',
    failContinue: true,
    maxConcurrency: 3,
    planIds: [],
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
    if (USE_MOCK) {
      if (form.id) {
        const t = list.value.find((b) => b.id === form.id)
        if (t) {
          t.name = form.name
          t.strategy = form.strategy
          t.failContinue = form.failContinue
          t.maxConcurrency = form.maxConcurrency
          t.cron = form.cron || null
          t.enabled = form.enabled
        }
      } else {
        const newId = Math.max(0, ...list.value.map((b) => b.id)) + 1
        list.value.unshift({
          id: newId,
          projectId: projectStore.currentProject?.id || 0,
          name: form.name,
          strategy: form.strategy,
          failContinue: form.failContinue,
          maxConcurrency: form.maxConcurrency,
          cron: form.cron || null,
          enabled: form.enabled,
          lastRunId: null,
          lastRunTime: null,
          lastRunStatus: null,
          createTime: '',
          updateTime: ''
        })
        total.value = list.value.length
      }
      ElMessage.success('（mock）保存成功')
      dialogVisible.value = false
    } else {
      const { createBatch, updateBatch } = await import('@/api/planBatch')
      const payload = {
        id: form.id,
        name: form.name,
        projectId: projectStore.currentProject?.id ?? 0,
        strategy: form.strategy,
        failContinue: form.failContinue,
        maxConcurrency: form.maxConcurrency,
        planIds: form.planIds,
        cron: form.cron,
        enabled: form.enabled
      }
      if (form.id) await updateBatch(payload)
      else await createBatch(payload)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      await loadList()
    }
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: PlanBatchInfo) {
  try {
    await ElMessageBox.confirm(`确定删除批次「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  if (USE_MOCK) {
    list.value = list.value.filter((b) => b.id !== row.id)
    total.value = list.value.length
    ElMessage.success('（mock）删除成功')
  } else {
    const { deleteBatch } = await import('@/api/planBatch')
    await deleteBatch(row.id)
    ElMessage.success('删除成功')
    await loadList()
  }
}

async function handleToggle(row: PlanBatchInfo, val: boolean) {
  row._toggling = true
  try {
    if (USE_MOCK) {
      row.enabled = val
    } else {
      const { toggleBatchEnabled } = await import('@/api/planBatch')
      await toggleBatchEnabled(row.id, val)
      row.enabled = val
    }
  } finally {
    row._toggling = false
  }
}

async function handleExecute(row: PlanBatchInfo) {
  row._executing = true
  try {
    if (USE_MOCK) {
      await new Promise((r) => setTimeout(r, 800))
      ElMessage.success('（mock）已触发执行，请到详情页查看进度')
      router.push(`/batch/${row.id}`)
    } else {
      const { executeBatch } = await import('@/api/planBatch')
      await executeBatch(row.id)
      ElMessage.success('已触发执行')
      router.push(`/batch/${row.id}`)
    }
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
</style>
