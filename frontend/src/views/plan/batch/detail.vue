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
  VideoPlay
} from '@element-plus/icons-vue'
import { useProjectStore } from '@/stores/project'
import {
  executeBatch,
  getBatchDetail,
  getBatchRuns,
  getBatchRun,
  toggleBatchEnabled,
  updateBatch
} from '@/api/planBatch'
import type {
  PlanBatchInfo,
  PlanBatchRun,
  PlanBatchRunItem,
  RunItemStatus
} from '@/api/planBatch'

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

onMounted(() => {
  loadBatch()
})

onBeforeUnmount(() => stopPolling())

/** 拉取批次详情 + 运行历史 */
async function loadBatch() {
  const info = await getBatchDetail(batchId)
  batch.value = info
  planItems.value = (info.plans || []).map((p) => ({ id: p.id, name: p.name, sortOrder: p.sortOrder }))
  const runs = await getBatchRuns(batchId)
  runHistory.value = runs
  if (runs.length > 0) {
    currentRun.value = runs[0]
    if (currentRun.value.status === 'RUNNING') {
      executing.value = true
      startPolling()
    }
  } else {
    currentRun.value = null
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

function startPolling() {
  stopPolling()
  // 每 3 秒拉取运行实例的最新状态
  pollTimer = setInterval(async () => {
    if (!currentRun.value) return stopPolling()
    const run = await getBatchRun(currentRun.value.id)
    currentRun.value = run
    executing.value = run.status === 'RUNNING'
    if (run.status !== 'RUNNING') stopPolling()
  }, 3000)
}
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleExecute() {
  if (!batch.value) return
  executing.value = true
  const run = await executeBatch(batchId)
  currentRun.value = run
  runHistory.value = [run, ...runHistory.value]
  executing.value = false
  // 后端 executeBatch 为同步执行，正常返回即已完成；若返回 RUNNING（如被调度接管）才轮询
  if (run.status === 'RUNNING') startPolling()
}

function viewExecution(item: PlanBatchRunItem) {
  // executionId 指向 tb_execution，可执行记录查看页（前端报告页待补充），先提示
  ElMessage.info(`执行报告 executionId=${item.executionId}（报告查看页前端待补充）`)
}

function viewRun(run: PlanBatchRun) {
  currentRun.value = run
}

/* ---------------- 启停 / 编辑 ---------------- */
async function handleToggle(val: boolean) {
  if (!batch.value) return
  batch.value._toggling = true
  try {
    await toggleBatchEnabled(batch.value.id, val)
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
