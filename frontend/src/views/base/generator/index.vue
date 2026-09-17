<template>
  <div class="generator-page">
    <!-- 查询栏 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="名称">
          <el-input
            v-model="query.name"
            placeholder="按名称模糊搜索"
            clearable
            class="filter-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable class="filter-select">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item class="filter-right">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增组件生成器</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="records" v-loading="loading" border stripe empty-text="暂无生成器">
        <el-table-column label="名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.name }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="参数摘要" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ paramsSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="示例值" min-width="160">
          <template #default="{ row }">
            <code class="sample">{{ samples[row.id] ?? '—' }}</code>
            <el-button size="small" link type="primary" @click="refreshSample(row)">换一个</el-button>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
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

    <!-- 新增 / 编辑弹窗 -->
    <GeneratorFormDialog
      v-model="dialogVisible"
      :project-id="projectId"
      :edit-data="editData"
      @saved="onSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteGenerator, getGeneratorList, previewGenerator } from '@/api/generator'
import type { DataGeneratorInfo, GeneratorType } from '@/api/types'
import { useProjectStore } from '@/stores/project'
import GeneratorFormDialog from './GeneratorFormDialog.vue'

const projectStore = useProjectStore()
const projectId = computed(() => projectStore.currentProject?.id ?? 0)

const typeOptions: { label: string; value: GeneratorType }[] = [
  { label: '随机数', value: 'RANDOM' },
  { label: '手机号', value: 'PHONE' },
  { label: '身份证号', value: 'IDCARD' },
  { label: '姓名', value: 'NAME' },
  { label: '枚举', value: 'ENUM' },
  { label: '时间戳', value: 'TIMESTAMP' },
  { label: 'UUID', value: 'UUID' },
  { label: '自定义表达式', value: 'CUSTOM' }
]

const typeLabelMap: Record<GeneratorType, string> = {
  RANDOM: '随机数',
  PHONE: '手机号',
  IDCARD: '身份证号',
  NAME: '姓名',
  ENUM: '枚举',
  TIMESTAMP: '时间戳',
  UUID: 'UUID',
  CUSTOM: '自定义'
}
function typeLabel(t: GeneratorType): string {
  return typeLabelMap[t] ?? t
}
function typeTag(t: GeneratorType): '' | 'success' | 'warning' | 'info' {
  return t === 'CUSTOM' ? 'warning' : 'info'
}

const loading = ref(false)
const records = ref<DataGeneratorInfo[]>([])
const total = ref(0)
const query = reactive<{ name: string; type: GeneratorType | ''; page: number; size: number }>({
  name: '',
  type: '',
  page: 1,
  size: 10
})

const samples = reactive<Record<number, string>>({})

/** 示例值一律向后端试生成，保证与用例执行时的真实结果同源 */
async function sampleOf(g: DataGeneratorInfo): Promise<string> {
  try {
    const res = await previewGenerator({
      projectId: projectId.value,
      type: g.type,
      params: g.params ?? {}
    })
    return res.result
  } catch {
    return '—'
  }
}
async function refreshSample(g: DataGeneratorInfo) {
  samples[g.id] = await sampleOf(g)
}

function paramsSummary(g: DataGeneratorInfo): string {
  const p = g.params ?? {}
  switch (g.type) {
    case 'RANDOM':
      return `长度 ${p.length ?? 8} / ${p.charset ?? 'digits'}${p.prefix ? ` / 前缀 ${p.prefix}` : ''}${p.suffix ? ` / 后缀 ${p.suffix}` : ''}`
    case 'ENUM':
      return `候选：${String(p.values ?? '')}`
    case 'TIMESTAMP':
      return `格式 ${p.format || 'ms'}${p.offset ? ` / 偏移 ${p.offset}` : ''}`
    case 'CUSTOM':
      return String(p.template ?? '')
    case 'PHONE':
    case 'IDCARD':
    case 'NAME':
    case 'UUID':
      return '无需参数'
    default:
      return '—'
  }
}

async function loadList() {
  if (!projectId.value) {
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getGeneratorList({
      projectId: projectId.value,
      name: query.name.trim() || undefined,
      type: query.type || undefined,
      page: query.page,
      size: query.size
    })
    records.value = res.records
    total.value = res.total
    await Promise.all(
      res.records
        .filter((g) => samples[g.id] === undefined)
        .map(async (g) => {
          samples[g.id] = await sampleOf(g)
        })
    )
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}
function resetQuery() {
  query.name = ''
  query.type = ''
  query.page = 1
  loadList()
}

/* 新增 / 编辑弹窗 */
const dialogVisible = ref(false)
const editData = ref<DataGeneratorInfo | null>(null)

function openCreate() {
  editData.value = null
  dialogVisible.value = true
}
function openEdit(row: DataGeneratorInfo) {
  editData.value = row
  dialogVisible.value = true
}
function onSaved() {
  dialogVisible.value = false
  loadList()
}

async function handleDelete(row: DataGeneratorInfo) {
  try {
    await ElMessageBox.confirm(`确定删除生成器「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteGenerator(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.generator-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-input {
  width: 200px;
}
.filter-select {
  width: 160px;
}
.filter-right {
  margin-left: auto;
}
.sample {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  color: #1677ff;
  margin-right: 8px;
}
.muted {
  color: #c0c4cc;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
