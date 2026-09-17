<template>
  <el-dialog
    :model-value="modelValue"
    title="选择数据生成器"
    width="680px"
    :close-on-click-modal="false"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form :inline="true" :model="query" @submit.prevent class="sel-filter">
      <el-form-item label="名称">
        <el-input
          v-model="query.name"
          placeholder="按名称模糊搜索"
          clearable
          class="sel-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部类型" clearable class="sel-select">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="records"
      v-loading="loading"
      border
      stripe
      empty-text="暂无生成器"
      max-height="320"
    >
      <el-table-column label="名称" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.name }}</template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="参数摘要" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ paramsSummary(row) }}</template>
      </el-table-column>
      <el-table-column label="示例值" min-width="120">
        <template #default="{ row }">
          <code class="sel-sample">{{ samples[row.id] ?? '—' }}</code>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="onSelect(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="sel-pager">
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
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getGeneratorList, previewGenerator } from '@/api/generator'
import type { DataGeneratorInfo, GeneratorType } from '@/api/types'

const props = defineProps<{ modelValue: boolean; projectId: number }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'selected', info: DataGeneratorInfo): void
}>()

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
      projectId: props.projectId,
      type: g.type,
      params: g.params ?? {}
    })
    return res.result
  } catch {
    return '—'
  }
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
    default:
      return '无需参数'
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getGeneratorList({
      projectId: props.projectId,
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
function onSelect(row: DataGeneratorInfo) {
  emit('selected', row)
  emit('update:modelValue', false)
}

onMounted(loadList)
</script>

<style scoped>
.sel-filter :deep(.el-form-item) {
  margin-bottom: 0;
}
.sel-input {
  width: 180px;
}
.sel-select {
  width: 150px;
}
.sel-sample {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  color: #1677ff;
}
.sel-pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
