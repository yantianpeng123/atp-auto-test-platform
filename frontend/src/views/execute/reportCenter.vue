<template>
  <div class="report-center">
    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索用例 / 计划名称"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.status" placeholder="执行状态" clearable style="width: 140px" @change="handleSearch">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="运行中" value="RUNNING" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column prop="executionId" label="报告ID" width="100" />
        <el-table-column prop="caseName" label="用例" min-width="160" show-overflow-tooltip />
        <el-table-column prop="planName" label="所属计划" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.planName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="envName" label="环境" width="120" />
        <el-table-column label="触发类型" width="100">
          <template #default="{ row }">
            <el-tag :type="triggerTagType(row.triggerType)" size="small" effect="plain">
              {{ triggerText(row.triggerType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="轮次通过" width="110" align="center">
          <template #default="{ row }">
            <span>{{ row.passedRounds }}/{{ row.totalRounds }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="110" align="center">
          <template #default="{ row }">
            <span>{{ formatDuration(row.durationMs) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="goReport(row)">查看报告</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无报告" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectStore } from '@/stores/project'
import { ElMessage } from 'element-plus'
import { Refresh, Search, View } from '@element-plus/icons-vue'
import { getReportList } from '@/api/execute'
import type { ExecutionSummary } from '@/api/execute'

const router = useRouter()
const projectStore = useProjectStore()

const loading = ref(false)
const list = ref<ExecutionSummary[]>([])
const total = ref(0)
const query = reactive<{ keyword: string; status: string; page: number; size: number }>({
  keyword: '',
  status: '',
  page: 1,
  size: 10
})

onMounted(() => loadList())

async function loadList() {
  loading.value = true
  try {
    const res = await getReportList({
      page: query.page,
      size: query.size,
      keyword: query.keyword,
      status: query.status,
      projectId: projectStore.currentProject?.id ?? null
    })
    list.value = res.records
    total.value = res.total
  } catch {
    ElMessage.error('加载报告列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  loadList()
}

function goReport(row: ExecutionSummary) {
  router.push(`/execution/${row.executionId}`)
}

function statusText(s: string): string {
  return s === 'SUCCESS' ? '成功' : s === 'FAILED' ? '失败' : '运行中'
}
function statusTagType(s: string): 'success' | 'danger' | 'warning' {
  return s === 'SUCCESS' ? 'success' : s === 'FAILED' ? 'danger' : 'warning'
}
function triggerText(t: string): string {
  return t === 'MANUAL' ? '手动' : t === 'SCHEDULED' ? '定时' : 'CI'
}
function triggerTagType(t: string): 'info' | 'warning' | 'primary' {
  return t === 'MANUAL' ? 'primary' : t === 'SCHEDULED' ? 'warning' : 'info'
}
function formatDuration(ms: number): string {
  if (!ms) return '—'
  return (ms / 1000).toFixed(1) + 's'
}
</script>

<style scoped>
.report-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.toolbar-card :deep(.el-card__body) {
  padding: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.table-card :deep(.el-card__body) {
  padding: 16px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
