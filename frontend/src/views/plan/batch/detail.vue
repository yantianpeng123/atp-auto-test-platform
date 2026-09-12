<template>
  <div class="page">
    <!-- 头部 -->
    <div class="detail-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
        <span class="batch-name">{{ batch?.name || '批次详情' }}</span>
        <el-switch
          v-if="batch"
          :model-value="batch.enabled"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
          :loading="batch._toggling"
          @change="(val: boolean) => handleToggle(val)"
        />
      </div>
      <div class="header-right">
        <el-button
          type="primary"
          :icon="VideoPlay"
          :loading="executing"
          @click="handleExecute"
        >
          立即执行
        </el-button>
        <el-button :icon="Edit" @click="handleEdit">编辑</el-button>
      </div>
    </div>

    <!-- 编排配置 -->
    <el-card shadow="never" class="card">
      <template #header>
        <span class="card-title">编排配置</span>
      </template>

      <div class="config-grid">
        <div class="config-left">
          <div class="block-label">关联测试计划（顺序即执行顺序）</div>
          <el-table :data="planItems" border size="small" style="width: 100%">
            <el-table-column label="#" width="56" align="center">
              <template #default="{ row }">
                <div class="order-cell">
                  <span class="order-num">{{ row.sortOrder }}</span>
                  <div class="order-btns">
                    <el-button
                      link
                      :icon="CaretTop"
                      :disabled="row.sortOrder <= 1"
                      @click="movePlan(row, -1)"
                    />
                    <el-button
                      link
                      :icon="CaretBottom"
                      :disabled="row.sortOrder >= planItems.length"
                      @click="movePlan(row, 1)"
                    />
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="计划名称" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>

        <div class="config-right">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="执行策略">
              <el-tag :type="batch?.strategy === 'PARALLEL' ? 'warning' : 'info'" effect="plain">
                {{ batch?.strategy === 'PARALLEL' ? '并行' : '串行' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="失败继续">
              {{ batch?.failContinue ? '继续' : '中断' }}
            </el-descriptions-item>
            <el-descriptions-item label="最大并发数">
              {{ batch?.maxConcurrency }}
            </el-descriptions-item>
            <el-descriptions-item label="Cron 表达式">
              <span v-if="batch?.cron">{{ batch.cron }}</span>
              <span v-else class="text-muted">--（仅手动）</span>
            </el-descriptions-item>
            <el-descriptions-item label="下次执行时间">
              <span class="text-muted">依据 Cron 表达式由后端计算</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-card>

    <!-- 执行看板 -->
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">执行看板（最近一次运行）</span>
          <el-button
            v-if="currentRun && currentRun.status !== 'RUNNING'"
            type="warning"
            size="small"
            :icon="RefreshRight"
            @click="rerunFailed"
          >
            重跑失败
          </el-button>
        </div>
      </template>

      <div v-if="currentRun" class="dashboard">
        <div class="metric-row">
          <div class="metric-card">
            <div class="metric-label">总计划</div>
            <div class="metric-value">{{ currentRun.total }}</div>
          </div>
          <div class="metric-card ok">
            <div class="metric-label">成功</div>
            <div class="metric-value">{{ currentRun.passed }}</div>
          </div>
          <div class="metric-card bad">
            <div class="metric-label">失败</div>
            <div class="metric-value">{{ currentRun.failed }}</div>
          </div>
          <div class="metric-card warn">
            <div class="metric-label">进行中</div>
            <div class="metric-value">{{ currentRun.running }}</div>
          </div>
        </div>

        <div class="progress-wrap">
          <el-progress
            :percentage="progressPercent"
            :status="currentRun.status === 'RUNNING' ? undefined : (currentRun.failed > 0 ? 'exception' : 'success')"
            :stroke-width="10"
          />
        </div>

        <div class="run-items">
          <div
            v-for="item in currentRun.items"
            :key="item.id"
            class="run-item"
            :class="item.status.toLowerCase()"
          >
            <span class="dot" />
            <span class="run-item-name">{{ item.planName }}</span>
            <span class="run-item-status">{{ runItemStatusText(item.status) }}</span>
            <span v-if="item.durationMs" class="run-item-dur">{{ (item.durationMs / 1000).toFixed(1) }}s</span>
            <el-button
              v-if="item.status === 'FAILED'"
              link
              type="warning"
              size="small"
              @click="rerunItem(item)"
            >
              重跑
            </el-button>
            <el-button
              v-if="item.executionId"
              link
              type="primary"
              size="small"
              @click="viewExecution(item)"
            >
              报告
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无运行记录，点击右上角「立即执行」开始" />
    </el-card>

    <!-- 运行历史 -->
    <el-card shadow="never" class="card">
      <template #header>
        <span class="card-title">运行历史</span>
      </template>

      <el-table :data="runHistory" empty-text="暂无运行历史" style="width: 100%">
        <el-table-column prop="id" label="批次号" width="120" />
        <el-table-column label="触发" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.triggerType === 'SCHEDULED' ? 'info' : 'primary'" size="small" effect="plain">
              {{ row.triggerType === 'SCHEDULED' ? '定时' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" min-width="170" />
        <el-table-column label="结果" min-width="120">
          <template #default="{ row }">
            <span>{{ row.passed }}/{{ row.total }} 成功</span>
            <span v-if="row.failed" class="text-bad"> · {{ row.failed }} 失败</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'SUCCESS' ? 'success' : (row.status === 'PARTIAL_FAILED' ? 'warning' : row.status === 'FAILED' ? 'danger' : 'info')"
              size="small"
            >
              {{ runStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewRun(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑批次弹框 -->
    <el-dialog
      v-model="editVisible"
      title="编辑批次"
      width="620px"
      :close-on-click-modal="false"
      append-to-body
      @closed="resetEditForm"
    >
      <el-form ref="editFormEl" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="批次名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入批次名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="执行策略" prop="strategy">
          <el-radio-group v-model="editForm.strategy">
            <el-radio value="PARALLEL">并行</el-radio>
            <el-radio value="SERIAL">串行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="失败继续">
          <el-switch v-model="editForm.failContinue" active-text="继续" inactive-text="中断" />
        </el-form-item>
        <el-form-item label="最大并发数">
          <el-input-number v-model="editForm.maxConcurrency" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="Cron 表达式">
          <el-input v-model="editForm.cron" placeholder="如 0 0 2 * * ? （留空表示仅手动执行）" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editForm.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowLeft,
  CaretBottom,
  CaretTop,
  Edit,
  RefreshRight,
  VideoPlay
} from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import type {
  PlanBatchInfo,
  PlanBatchRun,
  PlanBatchRunItem,
  RunItemStatus,
  RunStatus
} from '@/api/planBatch'

/**
 * ⚠️ 后端 PlanBatch 模块尚未实现。此处以本地 mock 驱动演示；
 *    后端就绪后把 USE_MOCK 改为 false，并把 mockRun 轮询逻辑替换为
 *    每 3 秒调用 getBatchRun(runId) 拉取真实状态。
 */
const USE_MOCK = true

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const batchId = Number(route.params.id)
const batch = ref<PlanBatchInfo | null>(null)
const planItems = ref<{ id: number; name: string; sortOrder: number }[]>([])

const currentRun = ref<PlanBatchRun | null>(null)
const runHistory = ref<PlanBatchRun[]>([])
const executing = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

/* ----- 编辑弹框 ----- */
const editVisible = ref(false)
const submitting = ref(false)
const editFormEl = ref<FormInstance>()
const editForm = reactive({
  name: '',
  strategy: 'PARALLEL' as 'SERIAL' | 'PARALLEL',
  failContinue: true,
  maxConcurrency: 3,
  cron: '',
  enabled: true
})
const editRules: FormRules<typeof editForm> = {
  name: [{ required: true, message: '请输入批次名称', trigger: 'blur' }]
}

/* ----------------- mock 数据 ----------------- */
const MOCK_PLANS = [
  { id: 1, name: '登录与鉴权用例组' },
  { id: 2, name: '订单核心链路' },
  { id: 3, name: '支付回调校验' },
  { id: 4, name: '消息通知链路' },
  { id: 5, name: '报表导出校验' }
]
const MOCK_BATCH_MAP: Record<number, Omit<PlanBatchInfo, 'projectId'>> = {
  1: {
    id: 1,
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
  2: {
    id: 2,
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
  3: {
    id: 3,
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
  4: {
    id: 4,
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
}

const progressPercent = computed(() => {
  if (!currentRun.value || currentRun.value.total === 0) return 0
  const done = currentRun.value.passed + currentRun.value.failed
  return Math.round((done / currentRun.value.total) * 100)
})

function runStatusText(s: string): string {
  return s === 'SUCCESS' ? '成功' : s === 'PARTIAL_FAILED' ? '部分失败' : s === 'FAILED' ? '失败' : '进行中'
}
function runItemStatusText(s: RunItemStatus): string {
  return s === 'SUCCESS'
    ? '成功'
    : s === 'FAILED'
      ? '失败'
      : s === 'RUNNING'
        ? '运行中'
        : s === 'SKIPPED'
          ? '跳过'
          : '排队中'
}
function nowStr(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(() => {
  loadBatch()
})

onBeforeUnmount(() => stopPolling())

function loadBatch() {
  const base = MOCK_BATCH_MAP[batchId]
  if (!base) {
    batch.value = null
    return
  }
  batch.value = { ...base, projectId: projectStore.currentProject?.id || 1 }
  planItems.value = MOCK_PLANS.map((p, i) => ({ id: p.id, name: p.name, sortOrder: i + 1 }))
  // mock：为每个批次造一条最近运行历史
  runHistory.value = [buildMockRun(batch.value, batch.value.lastRunStatus as RunStatus | null, true)]
  currentRun.value = runHistory.value[0]
}

function buildMockRun(
  b: PlanBatchInfo,
  finalStatus: RunStatus | null,
  finished: boolean
): PlanBatchRun {
  const items: PlanBatchRunItem[] = planItems.value.map((p, i) => {
    let status: RunItemStatus = finished ? (p.id % 4 === 0 ? 'FAILED' : 'SUCCESS') : 'QUEUED'
    return {
      id: i + 1,
      runId: 0,
      planId: p.id,
      planName: p.name,
      sortOrder: p.sortOrder,
      status,
      executionId: finished ? 9000 + i : null,
      durationMs: finished ? 600 + i * 200 : null,
      errorMsg: status === 'FAILED' ? '断言失败：预期 200，实际 500' : null,
      startTime: finished ? nowStr() : null,
      endTime: finished ? nowStr() : null
    }
  })
  const passed = items.filter((i) => i.status === 'SUCCESS').length
  const failed = items.filter((i) => i.status === 'FAILED').length
  return {
    id: b.lastRunId ?? 1000,
    batchId: b.id,
    triggerType: 'SCHEDULED',
    status: finalStatus ?? 'SUCCESS',
    total: items.length,
    passed,
    failed,
    running: finished ? 0 : items.length,
    queued: finished ? 0 : items.length,
    startTime: b.lastRunTime ?? nowStr(),
    endTime: finished ? b.lastRunTime ?? nowStr() : null,
    durationMs: finished ? items.reduce((s, i) => s + (i.durationMs ?? 0), 0) : null,
    items
  }
}

function movePlan(row: { sortOrder: number }, dir: number) {
  const idx = planItems.value.findIndex((p) => p.sortOrder === row.sortOrder)
  const swapIdx = idx + dir
  if (swapIdx < 0 || swapIdx >= planItems.value.length) return
  const a = planItems.value[idx]
  const b = planItems.value[swapIdx]
  const tmp = a.sortOrder
  a.sortOrder = b.sortOrder
  b.sortOrder = tmp
  planItems.value.sort((x, y) => x.sortOrder - y.sortOrder)
}

/* ---------------- mock 运行引擎（轮询演示） ---------------- */
function startMockRun() {
  if (!batch.value) return
  const run: PlanBatchRun = {
    id: Date.now(),
    batchId: batch.value.id,
    triggerType: 'MANUAL',
    status: 'RUNNING',
    total: planItems.value.length,
    passed: 0,
    failed: 0,
    running: 0,
    queued: planItems.value.length,
    startTime: nowStr(),
    endTime: null,
    durationMs: null,
    items: planItems.value.map((p, i) => ({
      id: i + 1,
      runId: 0,
      planId: p.id,
      planName: p.name,
      sortOrder: p.sortOrder,
      status: 'QUEUED',
      executionId: null,
      durationMs: null,
      errorMsg: null,
      startTime: null,
      endTime: null
    }))
  }
  currentRun.value = run
  runHistory.value = [run, ...runHistory.value]
  executing.value = true
  startPolling()
}

function tickMockRun() {
  const run = currentRun.value
  if (!run || run.status !== 'RUNNING') {
    stopPolling()
    return
  }
  const maxConc = batch.value?.maxConcurrency ?? 1
  // 1) 随机结束一个正在运行的计划
  const runningItems = run.items.filter((i) => i.status === 'RUNNING')
  if (runningItems.length) {
    const it = runningItems[Math.floor(Math.random() * runningItems.length)]
    const fail = it.planId % 4 === 0
    it.status = fail ? 'FAILED' : 'SUCCESS'
    it.durationMs = 600 + Math.floor(Math.random() * 1400)
    it.endTime = nowStr()
    it.executionId = 9000 + it.id
    if (fail) {
      it.errorMsg = '断言失败：预期 200，实际 500'
      run.failed++
    } else {
      run.passed++
    }
    run.running--
  }
  // 2) 按并发上限启动排队中的计划
  while (run.running < maxConc && run.items.some((i) => i.status === 'QUEUED')) {
    const q = run.items.find((i) => i.status === 'QUEUED')!
    q.status = 'RUNNING'
    q.startTime = nowStr()
    run.running++
    run.queued--
  }
  // 3) 完成判定
  if (run.running === 0 && run.queued === 0) {
    run.status = run.failed > 0 ? 'PARTIAL_FAILED' : 'SUCCESS'
    run.endTime = nowStr()
    run.durationMs = run.items.reduce((s, i) => s + (i.durationMs ?? 0), 0)
    if (batch.value) {
      batch.value.lastRunId = run.id
      batch.value.lastRunTime = run.endTime
      batch.value.lastRunStatus = run.status
    }
    executing.value = false
    stopPolling()
  }
}

function startPolling() {
  stopPolling()
  if (USE_MOCK) {
    pollTimer = setInterval(tickMockRun, 1200)
  } else {
    // 真实模式：每 3 秒拉取运行实例的最新状态
    pollTimer = setInterval(async () => {
      if (!currentRun.value) return stopPolling()
      const { getBatchRun } = await import('@/api/planBatch')
      const run = await getBatchRun(currentRun.value.id)
      currentRun.value = run
      executing.value = run.status === 'RUNNING'
      if (run.status !== 'RUNNING') stopPolling()
    }, 3000)
  }
}
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleExecute() {
  if (!batch.value) return
  if (USE_MOCK) {
    startMockRun()
    ElMessage.success('（mock）已开始执行，看板实时刷新')
  } else {
    void import('@/api/planBatch').then(async ({ executeBatch }) => {
      executing.value = true
      const run = await executeBatch(batchId)
      currentRun.value = run
      runHistory.value = [run, ...runHistory.value]
      executing.value = false
      startPolling()
    })
  }
}

function rerunFailed() {
  const run = currentRun.value
  if (!run) return
  // 失败项重新置为排队，重置汇总，重新轮询
  run.items.forEach((i) => {
    if (i.status === 'FAILED') {
      i.status = 'QUEUED'
      i.errorMsg = null
      i.durationMs = null
      i.executionId = null
      i.startTime = null
      i.endTime = null
    }
  })
  run.failed = 0
  run.running = 0
  run.queued = run.items.filter((i) => i.status === 'QUEUED').length
  run.status = 'RUNNING'
  run.endTime = null
  executing.value = true
  startPolling()
  ElMessage.success('（mock）已重跑失败计划')
}

function rerunItem(item: PlanBatchRunItem) {
  const run = currentRun.value
  if (!run) return
  item.status = 'QUEUED'
  item.errorMsg = null
  item.durationMs = null
  item.executionId = null
  item.startTime = null
  item.endTime = null
  run.failed = Math.max(0, run.failed - 1)
  run.queued++
  run.status = 'RUNNING'
  executing.value = true
  startPolling()
}

function viewExecution(item: PlanBatchRunItem) {
  ElMessage.info(`跳转到执行报告（executionId=${item.executionId}），后端接口待对接`)
}

function viewRun(run: PlanBatchRun) {
  currentRun.value = run
}

/* ---------------- 启停 / 编辑 ---------------- */
async function handleToggle(val: boolean) {
  if (!batch.value) return
  batch.value._toggling = true
  try {
    if (!USE_MOCK) {
      const { toggleBatchEnabled } = await import('@/api/planBatch')
      await toggleBatchEnabled(batch.value.id, val)
    }
    batch.value.enabled = val
  } finally {
    if (batch.value) batch.value._toggling = false
  }
}

function handleEdit() {
  if (!batch.value) return
  editForm.name = batch.value.name
  editForm.strategy = batch.value.strategy
  editForm.failContinue = batch.value.failContinue
  editForm.maxConcurrency = batch.value.maxConcurrency
  editForm.cron = batch.value.cron || ''
  editForm.enabled = batch.value.enabled
  editVisible.value = true
}

function resetEditForm() {
  editFormEl.value?.clearValidate()
}

async function handleEditSubmit() {
  if (!editFormEl.value || !batch.value) return
  const valid = await editFormEl.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (USE_MOCK) {
      batch.value.name = editForm.name
      batch.value.strategy = editForm.strategy
      batch.value.failContinue = editForm.failContinue
      batch.value.maxConcurrency = editForm.maxConcurrency
      batch.value.cron = editForm.cron || null
      batch.value.enabled = editForm.enabled
      ElMessage.success('（mock）保存成功')
      editVisible.value = false
    } else {
      const { updateBatch } = await import('@/api/planBatch')
      await updateBatch({
        id: batch.value.id,
        name: editForm.name,
        projectId: projectStore.currentProject?.id ?? 0,
        strategy: editForm.strategy,
        failContinue: editForm.failContinue,
        maxConcurrency: editForm.maxConcurrency,
        planIds: planItems.value.map((p) => p.id),
        cron: editForm.cron,
        enabled: editForm.enabled
      })
      ElMessage.success('保存成功')
      editVisible.value = false
    }
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push('/batch')
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 1px solid #ebedf0;
  border-radius: 8px;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-name {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
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

.config-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}

.block-label {
  font-size: 13px;
  color: #374151;
  font-weight: 500;
  margin-bottom: 8px;
}

.order-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: center;
}

.order-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #eef2ff;
  color: #1d63d1;
  font-size: 12px;
}

.order-btns {
  display: flex;
  flex-direction: column;
  line-height: 0;
}

.order-btns :deep(.el-button) {
  margin: 0;
  padding: 0;
  height: 14px;
}

/* 执行看板 */
.metric-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
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

.metric-card.warn .metric-value {
  color: #854f0b;
}

.metric-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.metric-value {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
}

.progress-wrap {
  margin-bottom: 18px;
}

.run-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.run-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fafbfc;
  border: 1px solid #f0f1f3;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
}

.run-item .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
  background: #c0c4cc;
}

.run-item.success .dot {
  background: #639922;
}

.run-item.failed .dot {
  background: #e24b4a;
}

.run-item.running .dot {
  background: #ef9f27;
}

.run-item-name {
  flex: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-item-status {
  color: #909399;
}

.run-item-dur {
  color: #909399;
}

.text-muted {
  color: #9ca3af;
}

.text-bad {
  color: #a32d2d;
}
</style>
