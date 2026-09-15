<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="1000px"
    :close-on-click-modal="false"
    append-to-body
    class="dataset-select-dialog"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <!-- 查询 + 新增 -->
    <div class="ds-toolbar">
      <el-form :inline="true" :model="query" class="ds-filter">
        <el-form-item label="数据源名称">
          <el-input
            v-model="query.name"
            placeholder="请输入数据源名称"
            clearable
            size="small"
            class="ds-input"
            @keyup.enter="loadList"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" :icon="Search" @click="loadList">查询</el-button>
          <el-button size="small" :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" size="small" :icon="Plus" @click="openCreate">新增数据源</el-button>
    </div>

    <el-table :data="list" v-loading="loading" size="small" border empty-text="暂无数据源">
      <el-table-column label="数据源名称" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="ds-name">{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="creatorName" label="添加人" width="120" v-show="false">
        <template #default="{ row }">
          <span v-if="row.creatorName">{{ row.creatorName }}</span>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="修改时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openItems(row)">数据项</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[5, 10, 20]"
      layout="total, sizes, prev, pager, next"
      class="ds-pagination"
      @size-change="loadList"
      @current-change="loadList"
    />

    <!-- 复用：数据项编辑 / 表单 -->
    <DatasetItemsDialog
      v-model="itemsVisible"
      :dataset="currentRow"
      @saved="loadList"
    />
    <DatasetFormDialog
      v-model="formVisible"
      :dataset="currentRow"
      :default-case-id="caseId"
      :case-id-locked="!!caseId"
      @saved="loadList"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDataset, getDatasetList } from '@/api/dataset'
import type { CaseDatasetInfo } from '@/api/types'
import DatasetItemsDialog from './DatasetItemsDialog.vue'
import DatasetFormDialog from './DatasetFormDialog.vue'

const props = defineProps<{
  modelValue: boolean
  /** 当前用例ID：传入后只展示该用例的数据源（用例编辑页场景） */
  caseId?: number
  /** 当前用例名称，用于标题展示 */
  caseName?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const title = computed(() =>
  props.caseName ? `数据源选择 — ${props.caseName}` : '数据源选择'
)

const loading = ref(false)
const list = ref<CaseDatasetInfo[]>([])
const total = ref(0)

const query = ref({
  name: '',
  page: 1,
  size: 5
})

const itemsVisible = ref(false)
const formVisible = ref(false)
const currentRow = ref<CaseDatasetInfo | null>(null)

async function loadList() {
  loading.value = true
  try {
    const result = await getDatasetList({
      caseId: props.caseId,
      name: query.value.name || undefined,
      page: query.value.page,
      size: query.value.size
    })
    list.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.value.name = ''
  query.value.page = 1
  loadList()
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    query.value.page = 1
    loadList()
  }
)

function openCreate() {
  currentRow.value = null
  formVisible.value = true
}

function openEdit(row: CaseDatasetInfo) {
  currentRow.value = row
  formVisible.value = true
}

function openItems(row: CaseDatasetInfo) {
  currentRow.value = row
  itemsVisible.value = true
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
</script>

<style scoped>
.ds-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.ds-filter {
  flex: 1;
}

.ds-filter :deep(.el-form-item) {
  margin-right: 12px;
  margin-bottom: 0;
}

.ds-input {
  width: 200px;
}

.ds-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.ds-name {
  font-weight: 500;
  color: #1f2937;
}

.text-muted {
  color: #9ca3af;
}
</style>
