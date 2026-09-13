<template>
  <div class="page">
    <!-- 头部 -->
    <div class="report-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
        <span class="title">执行报告</span>
        <el-tag :type="statusTagType(report.status)" effect="dark">{{ statusText(report.status) }}</el-tag>
      </div>
      <div class="header-right">
        <el-button :icon="Refresh" :loading="rerunLoading" @click="handleRerun">重新执行</el-button>
        <el-button
          type="warning"
          :icon="RefreshRight"
          :disabled="totalFailed === 0"
          :loading="retryLoading"
          @click="handleRetryFailed"
        >
          失败重试
        </el-button>
      </div>
    </div>

    <!-- 概览 -->
    <el-card shadow="never" class="card">
      <div class="overview">
        <div class="ov-item">
          <div class="ov-label">用例</div>
          <div class="ov-value">{{ report.caseName || '—' }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">所属计划</div>
          <div class="ov-value">{{ report.planName || '—' }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">环境</div>
          <div class="ov-value">{{ report.envName || '—' }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">触发类型</div>
          <div class="ov-value">{{ triggerText(report.triggerType) }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">执行人</div>
          <div class="ov-value">{{ report.executorName || '—' }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">开始时间</div>
          <div class="ov-value">{{ report.startTime || '—' }}</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">耗时</div>
          <div class="ov-value">{{ (report.durationMs / 1000).toFixed(1) }}s</div>
        </div>
        <div class="ov-item">
          <div class="ov-label">轮次通过</div>
          <div class="ov-value" :class="report.failedRounds > 0 ? 'bad' : 'ok'">
            {{ report.passedRounds }}/{{ report.totalRounds }}
          </div>
        </div>
        <div class="ov-item">
          <div class="ov-label">步骤通过率</div>
          <div class="ov-value" :class="passRate < 100 ? 'bad' : 'ok'">{{ passRate }}%</div>
        </div>
      </div>
    </el-card>

    <!-- 筛选栏 -->
    <div class="toolbar">
      <el-switch v-model="onlyFailed" active-text="仅看失败" />
      <span class="toolbar-tip">共 {{ totalSteps }} 个步骤，失败 {{ totalFailed }} 个</span>
    </div>

    <!-- 轮次分组 -->
    <el-card
      v-for="round in visibleRounds"
      :key="round.roundIndex"
      shadow="never"
      class="card round-card"
    >
      <template #header>
        <div class="round-header">
          <span class="round-title">
            第 {{ round.roundIndex }} 轮
            <span v-if="round.params && Object.keys(round.params).length" class="round-param">
              · {{ paramText(round.params) }}
            </span>
          </span>
          <el-tag :type="round.status === 'SUCCESS' ? 'success' : 'danger'" size="small" effect="plain">
            {{ round.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
          <span class="round-meta">
            {{ round.passedSteps }}/{{ round.steps.length }} 步通过 · {{ (round.durationMs / 1000).toFixed(1) }}s
          </span>
        </div>
      </template>

      <!-- 步骤列表 -->
      <div
        v-for="step in visibleSteps(round)"
        :key="step.stepId"
        class="step"
        :class="step.status.toLowerCase()"
      >
        <div class="step-head" @click="toggleStep(stepKey(round, step))">
          <span class="step-caret">{{ expandedSteps.has(stepKey(round, step)) ? '▼' : '▶' }}</span>
          <el-tag :type="methodTagType(step.method)" size="small" effect="plain">{{ step.method }}</el-tag>
          <span class="step-name">{{ step.stepName }}</span>
          <span class="step-url">{{ step.url }}</span>
          <span class="step-status" :class="step.status.toLowerCase()">{{ stepStatusText(step.status) }}</span>
          <span class="step-dur">{{ (step.durationMs / 1000).toFixed(1) }}s</span>
          <el-button
            v-if="step.status !== 'PASSED'"
            link
            type="warning"
            size="small"
            class="step-retry"
            @click.stop="handleRetryStep(step)"
          >
            重试
          </el-button>
        </div>

        <div v-if="expandedSteps.has(stepKey(round, step))" class="step-body">
          <el-tabs>
            <el-tab-pane label="请求">
              <div class="kv">
                <span class="kv-label">Method / URL</span>
                <span class="kv-val">{{ step.method }} {{ step.url }}</span>
              </div>
              <div class="kv"><span class="kv-label">Headers</span></div>
              <div class="code-block">
                <template v-if="jsonData(step.requestHeaders) !== undefined"><JsonTree :data="jsonData(step.requestHeaders)" /></template>
                <span v-else class="text-muted">{{ step.requestHeaders || '（无）' }}</span>
              </div>
              <div class="kv"><span class="kv-label">Body</span></div>
              <div class="code-block">
                <template v-if="jsonData(step.requestBody) !== undefined"><JsonTree :data="jsonData(step.requestBody)" /></template>
                <span v-else class="text-muted">{{ step.requestBody || '（无）' }}</span>
              </div>
            </el-tab-pane>

            <el-tab-pane label="响应">
              <div class="kv">
                <span class="kv-label">StatusCode</span>
                <span class="kv-val" :class="step.statusCode === 200 ? 'ok' : 'bad'">{{ step.statusCode }}</span>
              </div>
              <div class="kv"><span class="kv-label">Headers</span></div>
              <div class="code-block">
                <template v-if="jsonData(step.responseHeaders) !== undefined"><JsonTree :data="jsonData(step.responseHeaders)" /></template>
                <span v-else class="text-muted">{{ step.responseHeaders || '（无）' }}</span>
              </div>
              <div class="kv"><span class="kv-label">Body</span></div>
              <div class="code-block">
                <template v-if="jsonData(step.responseBody) !== undefined"><JsonTree :data="jsonData(step.responseBody)" /></template>
                <span v-else class="text-muted">{{ step.responseBody || '（无）' }}</span>
              </div>
              <div v-if="step.errorMsg" class="error-msg">错误：{{ step.errorMsg }}</div>
            </el-tab-pane>

            <el-tab-pane label="断言">
              <el-table :data="step.assertResults" size="small" border empty-text="无断言">
                <el-table-column prop="type" label="类型" width="100" />
                <el-table-column prop="path" label="路径" min-width="120" show-overflow-tooltip />
                <el-table-column prop="operator" label="运算符" width="90" />
                <el-table-column prop="expected" label="期望值" min-width="110" show-overflow-tooltip />
                <el-table-column prop="actual" label="实际值" min-width="110" show-overflow-tooltip />
                <el-table-column label="结果" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.passed ? 'success' : 'danger'" size="small">
                      {{ row.passed ? '通过' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-card>

    <el-empty v-if="visibleRounds.length === 0" description="没有匹配的步骤" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh, RefreshRight } from '@element-plus/icons-vue'
import JsonTree from '@/components/JsonTree.vue'
import { executeCase, getExecutionReport } from '@/api/execute'
import type { ExecutionReport } from '@/api/execute'
import type { RoundExecuteResult, StepExecuteResult } from '@/api/types'

const route = useRoute()
const router = useRouter()

const executionId = Number(route.params.id)

/** 初始空壳，加载完成后被真实（mock）报告覆盖 */
function emptyReport(): ExecutionReport {
  return {
    executionId,
    planId: null,
    planName: null,
    caseId: 0,
    caseName: '',
    envId: 0,
    envName: '',
    triggerType: 'MANUAL',
    executorId: null,
    executorName: null,
    status: 'RUNNING',
    startTime: '',
    endTime: null,
    durationMs: 0,
    totalRounds: 0,
    passedRounds: 0,
    failedRounds: 0,
    rounds: []
  }
}

const report = ref<ExecutionReport>(emptyReport())
const onlyFailed = ref(false)
const expandedSteps = ref<Set<string>>(new Set())
const rerunLoading = ref(false)
const retryLoading = ref(false)

onMounted(loadReport)

async function loadReport() {
  report.value = await getExecutionReport(executionId)
}

const totalSteps = computed(() =>
  report.value.rounds.reduce((s, r) => s + r.steps.length, 0)
)
const totalFailed = computed(() =>
  report.value.rounds.reduce((s, r) => s + r.steps.filter((st) => st.status !== 'PASSED').length, 0)
)
const passRate = computed(() => {
  const steps = totalSteps.value
  if (steps === 0) return 0
  return Math.round(((steps - totalFailed.value) / steps) * 100)
})

const visibleRounds = computed(() => {
  if (!onlyFailed.value) return report.value.rounds
  return report.value.rounds.filter((r) => r.steps.some((st) => st.status !== 'PASSED'))
})
function visibleSteps(round: RoundExecuteResult) {
  if (!onlyFailed.value) return round.steps
  return round.steps.filter((st) => st.status !== 'PASSED')
}

function stepKey(round: RoundExecuteResult, step: StepExecuteResult): string {
  return `${round.roundIndex}-${step.stepId}`
}
function toggleStep(key: string) {
  const next = new Set(expandedSteps.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expandedSteps.value = next
}

/** 解析 JSON 字符串；非 JSON / 空 返回 undefined */
function jsonData(str: string | null): unknown {
  if (!str) return undefined
  try {
    return JSON.parse(str)
  } catch {
    return undefined
  }
}

/* ---------------- 文本/状态映射 ---------------- */
function statusText(s: string): string {
  return s === 'SUCCESS' ? '成功' : s === 'FAILED' ? '失败' : '运行中'
}
function statusTagType(s: string): 'success' | 'danger' | 'info' {
  return s === 'SUCCESS' ? 'success' : s === 'FAILED' ? 'danger' : 'info'
}
function triggerText(t: string): string {
  return t === 'SCHEDULED' ? '定时' : t === 'MANUAL' ? '手动' : 'CI'
}
function stepStatusText(s: string): string {
  return s === 'PASSED' ? '成功' : s === 'FAILED' ? '失败' : '异常'
}
function methodTagType(m: string | null): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  switch ((m || '').toUpperCase()) {
    case 'GET':
      return 'success'
    case 'POST':
      return 'warning'
    case 'PUT':
      return 'primary'
    case 'DELETE':
      return 'danger'
    default:
      return 'info'
  }
}
function paramText(p: Record<string, unknown>): string {
  return Object.entries(p)
    .map(([k, v]) => `${k}=${v}`)
    .join('，')
}

/* ---------------- 执行 / 重试 ---------------- */
async function runCase() {
  if (!report.value.caseId) return
  await executeCase(report.value.caseId, report.value.envId)
}
async function handleRerun() {
  rerunLoading.value = true
  try {
    await runCase()
    ElMessage.success('已触发重新执行，将在新报告中查看结果')
  } catch {
    /* 接口层已提示 */
  } finally {
    rerunLoading.value = false
  }
}
async function handleRetryFailed() {
  retryLoading.value = true
  try {
    await runCase()
    ElMessage.success('已触发失败步骤重试（当前为整用例重跑，后续支持精确单步）')
  } catch {
    /* 接口层已提示 */
  } finally {
    retryLoading.value = false
  }
}
async function handleRetryStep(step: StepExecuteResult) {
  retryLoading.value = true
  try {
    await runCase()
    ElMessage.success(`已重试步骤「${step.stepName}」（当前为整用例重跑，后续支持精确单步）`)
  } catch {
    /* 接口层已提示 */
  } finally {
    retryLoading.value = false
  }
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.report-header {
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
.title {
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
}

.card :deep(.el-card__header) {
  padding: 12px 18px;
  border-bottom: 1px solid #f0f1f3;
}

/* 概览 */
.overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px 20px;
}
.ov-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.ov-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.ov-value.ok {
  color: #3b6d11;
}
.ov-value.bad {
  color: #a32d2d;
}

/* 筛选栏 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 4px;
}
.toolbar-tip {
  font-size: 13px;
  color: #909399;
}

/* 轮次 */
.round-card :deep(.el-card__header) {
  background: #fafbfc;
}
.round-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.round-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
}
.round-param {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}
.round-meta {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

/* 步骤 */
.step {
  border: 1px solid #f0f1f3;
  border-radius: 8px;
  margin-bottom: 10px;
  overflow: hidden;
}
.step.failed,
.step.error {
  border-color: #f3c2c2;
  background: #fdf6f6;
}
.step-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  background: #fafbfc;
}
.step-caret {
  color: #909399;
  font-size: 11px;
  width: 12px;
}
.step-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}
.step-url {
  flex: 1;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.step-status {
  font-size: 12px;
}
.step-status.passed {
  color: #3b6d11;
}
.step-status.failed,
.step-status.error {
  color: #a32d2d;
}
.step-dur {
  font-size: 12px;
  color: #909399;
}
.step-retry {
  margin-left: 4px;
}

.step-body {
  padding: 12px 14px;
  border-top: 1px solid #f0f1f3;
}

.kv {
  margin-bottom: 6px;
}
.kv-label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  display: block;
  margin-bottom: 4px;
}
.kv-val {
  font-size: 13px;
  color: #303133;
}
.kv-val.ok {
  color: #3b6d11;
}
.kv-val.bad {
  color: #a32d2d;
}

.code-block {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 6px;
  padding: 12px 14px;
  margin: 0 0 12px;
  overflow-x: auto;
  font-size: 12.5px;
  line-height: 1.7;
}
.code-block :deep(.jt-node) {
  color: #e2e8f0;
}

.error-msg {
  margin-top: 8px;
  font-size: 13px;
  color: #a32d2d;
  background: #fdf0f0;
  border-radius: 6px;
  padding: 8px 12px;
}

.text-muted {
  color: #9ca3af;
}
</style>
