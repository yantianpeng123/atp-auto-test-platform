<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">数据源管理</span>
          <el-button type="primary" :icon="Refresh" @click="loadList">刷新</el-button>
        </div>
      </template>

      <!-- 查询条件区 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="数据源名称">
            <el-input
              v-model="query.name"
              placeholder="请输入数据源名称"
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

      <el-table :data="list" v-loading="loading" empty-text="暂无数据源" style="width: 100%">
        <el-table-column prop="name" label="数据源名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="caseName" label="用例名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.caseName">{{ row.caseName }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Tickets" @click="handleDataItems(row)">数据项</el-button>
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            <el-button link type="info" :icon="View" @click="handleView(row)">查看</el-button>
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

    <!-- 数据源详情弹框 -->
    <el-dialog v-model="detailVisible" title="数据源详情" width="720px" append-to-body>
      <el-descriptions :column="2" border class="detail-desc">
        <el-descriptions-item label="数据源名称">{{ currentRow?.name || '—' }}</el-descriptions-item>
        <el-descriptions-item label="关联用例">{{ currentRow?.caseName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow?.createTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentRow?.updateTime || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div class="content-block">
        <div class="content-label">字段(key)</div>
        <div class="fields-line">{{ detailFieldText || '—' }}</div>
      </div>

      <div class="content-block">
        <div class="content-label">数据项</div>
        <div v-if="detailRows.length > 0" class="content-table-wrapper">
          <el-table :data="detailRows" border size="small" max-height="360">
            <el-table-column
              v-for="col in detailColumns"
              :key="col"
              :prop="col"
              :label="col"
              min-width="140"
              show-overflow-tooltip
            />
          </el-table>
        </div>
        <div v-else class="content-empty">暂无数据项</div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 数据项编辑弹框（共享组件） -->
    <DatasetItemsDialog v-model="itemsVisible" :dataset="currentRow" @saved="loadList" />

    <!-- 新增/编辑数据源弹框（共享组件） -->
    <DatasetFormDialog
      v-model="editVisible"
      :dataset="currentRow"
      @saved="loadList"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Delete,
  Edit,
  Refresh,
  Search,
  Tickets,
  View
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDataset, getDatasetDetail, getDatasetList } from '@/api/dataset'
import type { CaseDatasetInfo } from '@/api/types'

const loading = ref(false)
const list = ref<CaseDatasetInfo[]>([])
const total = ref(0)

const query = reactive({
  name: '',
  page: 1,
  size: 10
})

/* ---- 详情弹框 ---- */
const detailVisible = ref(false)
const currentRow = ref<CaseDatasetInfo | null>(null)

/** 解析字段（keys → {key, desc}[]） */
const detailFields = computed<{ key: string; desc: string }[]>(() => {
  const raw = currentRow.value?.keys
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr.map((f) => ({ key: String(f.key || ''), desc: String(f.desc || '') })) : []
  } catch {
    return []
  }
})

const detailFieldText = computed(() =>
  detailFields.value.map((f) => (f.desc ? `${f.key}(${f.desc})` : f.key)).join(', ')
)

const detailColumns = computed(() => detailFields.value.map((f) => f.key).filter((k) => k))

/** 解析数据项为行数组 */
const detailRows = computed<Record<string, unknown>[]>(() => {
  const items = currentRow.value?.items || []
  return items.map((it) => {
    if (!it.data) return {}
    try {
      return JSON.parse(it.data)
    } catch {
      return {}
    }
  })
})

async function loadList() {
  loading.value = true
  try {
    const result = await getDatasetList({
      name: query.name || undefined,
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
  query.page = 1
  loadList()
}

async function handleView(row: CaseDatasetInfo) {
  currentRow.value = row
  detailVisible.value = true
  try {
    currentRow.value = await getDatasetDetail(row.id)
  } catch {
    // 详情加载失败时保留列表行数据
  }
}

async function handleDelete(row: CaseDatasetInfo) {
  try {
    await ElMessageBox.confirm(`确定删除数据源「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteDataset(row.id)
  ElMessage.success('数据源删除成功')
  loadList()
}

/* ---- 数据项编辑 ---- */
const itemsVisible = ref(false)

function handleDataItems(row: CaseDatasetInfo) {
  currentRow.value = row
  itemsVisible.value = true
}

/* ---- 编辑数据源 ---- */
const editVisible = ref(false)
function handleEdit(row: CaseDatasetInfo) {
  currentRow.value = row
  editVisible.value = true
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

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.text-muted {
  color: #9ca3af;
}

/* ---------- 详情弹框 ---------- */
.detail-desc {
  margin-bottom: 18px;
}

.content-block {
  margin-top: 4px;
}

.content-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.fields-line {
  font-size: 13px;
  color: #1f2937;
  line-height: 1.6;
  word-break: break-all;
}

.content-table-wrapper {
  border-radius: 6px;
  overflow: hidden;
}

.content-empty {
  padding: 28px 0;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  background: #fafbfc;
  border: 1px dashed #e5e7eb;
  border-radius: 6px;
}

/* ---------- 数据项弹框 ---------- */
.items-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.items-hint {
  color: #9ca3af;
  margin-left: 4px;
}

.items-table {
  width: 100%;
}

.col-header {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.col-name {
  font-weight: 600;
}

.col-edit {
  color: #409eff;
  cursor: pointer;
}

.col-edit:hover {
  opacity: 0.75;
}

.col-remove {
  color: #f56c6c;
  cursor: pointer;
}

.col-remove:hover {
  opacity: 0.75;
}

.items-empty {
  padding: 32px 0;
  color: #9ca3af;
  font-size: 13px;
}

/* ---------- 编辑弹框 ---------- */
.edit-select {
  width: 100%;
}
</style>
