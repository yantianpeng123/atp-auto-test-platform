<template>
  <div class="ci-runs">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">CI 运行记录</span>
          <span class="card-sub">查看由外部 CI（Jenkins / GitLab CI 等）触发产生的批次运行历史与结果</span>
        </div>
      </template>

      <!-- 统计卡 -->
      <div class="stat-row">
        <div class="stat-card">
          <div class="stat-label">总运行</div>
          <div class="stat-value">{{ list.length }}</div>
        </div>
        <div class="stat-card stat-success">
          <div class="stat-label">成功</div>
          <div class="stat-value">{{ countByStatus('SUCCESS') }}</div>
        </div>
        <div class="stat-card stat-partial">
          <div class="stat-label">部分失败</div>
          <div class="stat-value">{{ countByStatus('PARTIAL_FAILED') }}</div>
        </div>
        <div class="stat-card stat-failed">
          <div class="stat-label">失败</div>
          <div class="stat-value">{{ countByStatus('FAILED') }}</div>
        </div>
      </div>

      <el-table
        :data="list"
        v-loading="loading"
        empty-text="暂无 CI 运行记录，外部流水线触发后将在此展示"
        style="width: 100%; margin-top: 16px"
      >
        <el-table-column prop="runId" label="Run ID" width="100" />
        <el-table-column label="批次" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.batchName">{{ row.batchName }}</span>
            <span v-else class="text-muted">批次 #{{ row.batchId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用例" width="130" align="center">
          <template #default="{ row }">
            <span v-if="row.total != null">
              <span class="ok-text">{{ row.passed ?? 0 }}</span>
              <span class="text-muted"> / {{ row.total }}</span>
            </span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="触发方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ triggerText(row.triggerType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">
            <span v-if="row.startTime">{{ row.startTime }}</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row.runId)">查看</el-button>
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
        @size-change="load"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCiRuns, type CiRunItem, type CiRunStatus } from '@/api/ci'
import { useProjectStore } from '@/stores/project'

const router = useRouter()
const projectStore = useProjectStore()

const projectId = computed(() => projectStore.currentProject?.id ?? 0)
const loading = ref(false)
const list = ref<CiRunItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

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

function triggerText(t: string | null): string {
  if (t === 'CI') return 'CI'
  return t || 'CI'
}

function countByStatus(status: CiRunStatus): number {
  return list.value.filter((r) => r.status === status).length
}

function openDetail(runId: number) {
  router.push(`/ci/runs/${runId}`)
}

async function load() {
  if (!projectId.value) {
    ElMessage.warning('请先在顶部选择一个项目')
    return
  }
  loading.value = true
  try {
    const data = await getCiRuns(projectId.value, query.page, query.size)
    list.value = data.records
    total.value = data.total
  } catch {
    // 错误已由拦截器统一提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.ci-runs {
  padding: 16px;
}
.page-card {
  border-radius: 8px;
}
.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
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
.stat-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1 1 140px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;
  background: #fafafa;
}
.stat-label {
  font-size: 12px;
  color: #909399;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  margin-top: 4px;
}
.stat-success .stat-value {
  color: #67c23a;
}
.stat-partial .stat-value {
  color: #e6a23c;
}
.stat-failed .stat-value {
  color: #f56c6c;
}
.text-muted {
  color: #c0c4cc;
}
.ok-text {
  color: #67c23a;
  font-weight: 600;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
