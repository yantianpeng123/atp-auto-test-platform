<template>
  <div class="ci-detail">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-head">
          <el-button :icon="ArrowLeft" link @click="goBack">返回</el-button>
          <span class="card-title"> #{{ runId }}</span>
          <el-tag v-if="run" :type="statusTag(run.status)" size="small" effect="dark">
            {{ statusText(run.status) }}
          </el-tag>
          <span v-if="run && run.batchName" class="card-sub">批次：{{ run.batchName }}</span>
          <div class="head-actions">
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
            <el-button :icon="Download" :disabled="!reportReady" @click="handleDownload">
              下载 XML
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading">
        <el-alert
          v-if="!run"
          type="info"
          :closable="false"
          title="未找到该运行记录，或您无权访问"
        />

        <template v-else>
          <!-- 元信息卡 -->
          <div class="meta-row">
            <div class="meta-card">
              <div class="meta-label">开始时间</div>
              <div class="meta-value">{{ run.startTime || '--' }}</div>
            </div>
            <div class="meta-card">
              <div class="meta-label">结束时间</div>
              <div class="meta-value">{{ run.endTime || (run.status === 'RUNNING' ? '进行中' : '--') }}</div>
            </div>
            <div class="meta-card">
              <div class="meta-label">耗时</div>
              <div class="meta-value">{{ formatDuration(run.durationMs) }}</div>
            </div>
            <div class="meta-card">
              <div class="meta-label">触发方式</div>
              <div class="meta-value">{{ run.triggerType || 'CI' }}</div>
            </div>
          </div>

          <!-- 通过率 -->
          <div class="rate-block">
            <div class="rate-head">
              <span>通过率</span>
              <span class="rate-num">
                <span class="ok-text">{{ run.passed ?? 0 }}</span>
                <span class="text-muted"> / {{ run.total ?? 0 }} 用例</span>
                <span v-if="run.failed" class="fail-text">（失败 {{ run.failed }}）</span>
              </span>
            </div>
            <el-progress
              :percentage="passRate"
              :status="run.status === 'FAILED' ? 'exception' : undefined"
              :stroke-width="14"
            />
          </div>

          <!-- 计划明细 -->
          <div class="section-title">计划执行明细</div>
          <el-table :data="run.items || []" empty-text="暂无计划" style="width: 100%">
            <el-table-column prop="planName" label="计划" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="itemStatusTag(row.status)" size="small" effect="plain">
                  {{ itemStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Execution ID" width="130" align="center">
              <template #default="{ row }">
                <span v-if="row.executionId" class="mono">{{ row.executionId }}</span>
                <span v-else class="text-muted">--</span>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="120">
              <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
            </el-table-column>
            <el-table-column label="错误信息" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.errorMsg" class="fail-text">{{ row.errorMsg }}</span>
                <span v-else class="text-muted">--</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 用例执行明细（来自 JUnit 报告） -->
          <div class="section-title">
            用例执行明细
            <span class="text-muted">（每条步骤的断言明细，来自 JUnit 报告）</span>
          </div>

          <div v-if="run.status === 'RUNNING'" class="empty-tip">运行进行中，报告将在执行完成后生成</div>
          <div v-else-if="reportError" class="empty-tip fail-text">{{ reportError }}</div>
          <div v-else-if="suites.length === 0" class="empty-tip">暂无用例执行明细</div>

          <el-collapse v-else v-model="activeSuites">
            <el-collapse-item v-for="(s, i) in suites" :key="i" :name="i">
              <template #title>
                <div class="suite-title">
                  <span class="suite-name">{{ s.name }}</span>
                  <el-tag size="small" effect="plain">共 {{ s.tests }} 步</el-tag>
                  <el-tag v-if="s.failures > 0" type="danger" size="small" effect="plain">
                    {{ s.failures }} 步失败
                  </el-tag>
                </div>
              </template>
              <el-table :data="s.steps" empty-text="无步骤" style="width: 100%">
                <el-table-column label="步骤" min-width="200" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.name }}</template>
                </el-table-column>
                <el-table-column label="耗时(s)" width="110" align="center">
                  <template #default="{ row }">{{ row.time }}</template>
                </el-table-column>
                <el-table-column label="结果" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.failed ? 'danger' : 'success'" size="small" effect="plain">
                      {{ row.failed ? '失败' : '通过' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="断言明细" min-width="280" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span v-if="row.failed" class="fail-text pre">{{ row.detail || row.message }}</span>
                    <span v-else class="text-muted">全部断言通过</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>

          <!-- 原始 XML -->
          <div v-if="reportReady" class="raw-toggle">
            <el-button link :icon="Document" @click="showRaw = !showRaw">
              {{ showRaw ? '隐藏原始 XML' : '查看原始 XML' }}
            </el-button>
            <pre v-if="showRaw" class="raw-xml">{{ rawXml }}</pre>
          </div>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download, Refresh, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getCiReportXml,
  downloadCiReport,
  type CiRunStatus
} from '@/api/ci'
import { getBatchRun, type PlanBatchRun, type PlanBatchRunItem } from '@/api/planBatch'

const route = useRoute()
const router = useRouter()
const runId = computed(() => Number(route.params.id))

const run = ref<PlanBatchRun | null>(null)
const loading = ref(false)
const rawXml = ref('')
const reportError = ref('')
const showRaw = ref(false)
const activeSuites = ref<number[]>([])

interface ReportStep {
  name: string
  time: string
  failed: boolean
  message: string
  detail: string
}
interface ReportSuite {
  name: string
  tests: number
  failures: number
  steps: ReportStep[]
}

const suites = computed<ReportSuite[]>(() => parseReport(rawXml.value))
const reportReady = computed(() => suites.value.length > 0)
const passRate = computed(() => {
  if (!run.value || !run.value.total) return 0
  const p = ((run.value.passed ?? 0) / run.value.total) * 100
  return Math.round(p)
})

function statusTag(status: CiRunStatus): 'success' | 'danger' | 'warning' | 'info' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'PARTIAL_FAILED':
      return 'warning'
    default:
      return 'info'
  }
}
function statusText(status: CiRunStatus): string {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    case 'PARTIAL_FAILED':
      return '部分失败'
    default:
      return '运行中'
  }
}
function itemStatusTag(status: PlanBatchRunItem['status']): 'success' | 'danger' | 'warning' | 'info' {
  switch (status) {
    case 'SUCCESS':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'SKIPPED':
      return 'info'
    default:
      return 'warning'
  }
}
function itemStatusText(status: PlanBatchRunItem['status']): string {
  switch (status) {
    case 'SUCCESS':
      return '成功'
    case 'FAILED':
      return '失败'
    case 'RUNNING':
      return '运行中'
    case 'QUEUED':
      return '排队中'
    default:
      return '已跳过'
  }
}
function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return '--'
  if (ms < 1000) return `${ms} ms`
  return `${(ms / 1000).toFixed(2)} s`
}

function parseReport(xml: string): ReportSuite[] {
  if (!xml) return []
  try {
    const doc = new DOMParser().parseFromString(xml, 'application/xml')
    if (doc.querySelector('parsererror')) return []
    const suiteEls = Array.from(doc.getElementsByTagName('testsuite'))
    return suiteEls.map((ts) => {
      const steps = Array.from(ts.getElementsByTagName('testcase')).map((tc) => {
        const failure = tc.getElementsByTagName('failure')[0]
        return {
          name: tc.getAttribute('name') || '',
          time: tc.getAttribute('time') || '',
          failed: !!failure,
          message: failure?.getAttribute('message') || '',
          detail: (failure?.textContent || '').trim()
        }
      })
      return {
        name: ts.getAttribute('name') || '',
        tests: Number(ts.getAttribute('tests') || 0),
        failures: Number(ts.getAttribute('failures') || 0),
        steps
      }
    })
  } catch {
    return []
  }
}

async function loadReport() {
  if (run.value?.status === 'RUNNING') return
  try {
    rawXml.value = await getCiReportXml(runId.value)
    reportError.value = ''
  } catch {
    reportError.value = '报告获取失败'
  }
}

async function load() {
  loading.value = true
  try {
    run.value = await getBatchRun(runId.value)
    reportError.value = ''
    rawXml.value = ''
    await loadReport()
  } catch {
    run.value = null
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/ci/runs')
}

function handleDownload() {
  downloadCiReport(runId.value)
    .then((blob) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ci-report-${runId.value}.xml`
      a.click()
      URL.revokeObjectURL(url)
    })
    .catch(() => ElMessage.error('下载失败'))
}

// 运行中自动轮询，直到结束
let timer: number | undefined
function startPolling() {
  stopPolling()
  timer = window.setInterval(() => {
    if (run.value && run.value.status === 'RUNNING') {
      void load()
    } else {
      stopPolling()
    }
  }, 3000)
}
function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = undefined
  }
}

watch(
  () => run.value?.status,
  (s) => {
    if (s === 'RUNNING') startPolling()
    else stopPolling()
  }
)

onMounted(load)
onUnmounted(stopPolling)
</script>

<style scoped>
.ci-detail {
  padding: 16px;
}
.page-card {
  border-radius: 8px;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.card-sub {
  font-size: 12px;
  color: #909399;
}
.head-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.meta-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.meta-card {
  flex: 1 1 160px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 16px;
  background: #fafafa;
}
.meta-label {
  font-size: 12px;
  color: #909399;
}
.meta-value {
  font-size: 15px;
  font-weight: 600;
  margin-top: 4px;
}
.rate-block {
  margin: 16px 0;
}
.rate-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 6px;
}
.rate-num {
  font-weight: 600;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 18px 0 10px;
}
.text-muted {
  color: #c0c4cc;
}
.ok-text {
  color: #67c23a;
}
.fail-text {
  color: #f56c6c;
}
.mono {
  font-family: monospace;
}
.pre {
  white-space: pre-wrap;
  word-break: break-all;
}
.empty-tip {
  padding: 16px;
  text-align: center;
  color: #c0c4cc;
  background: #fafafa;
  border-radius: 6px;
}
.suite-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.suite-name {
  font-weight: 600;
}
.raw-toggle {
  margin-top: 14px;
}
.raw-xml {
  margin-top: 8px;
  max-height: 360px;
  overflow: auto;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
