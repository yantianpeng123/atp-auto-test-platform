<template>
  <el-dialog
    :model-value="modelValue"
    :title="`数据项 — ${dataset?.name || ''}`"
    width="900px"
    :close-on-click-modal="false"
    append-to-body
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div class="items-toolbar">
      <el-button type="primary" size="small" :icon="Plus" @click="addDataRow">新增数据行</el-button>
      <el-text type="info" size="small" class="items-hint">
        每行即一轮执行的参数，用例执行时会按顺序取值替换 ${变量名}
      </el-text>
    </div>

    <el-table v-if="dataColumns.length > 0" :data="dataRows" border size="small" max-height="420" class="items-table">
      <el-table-column type="index" label="#" width="50" align="center" />

      <el-table-column v-for="col in dataColumns" :key="col" :label="col" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row[col]" size="small" placeholder="取值" />
        </template>
      </el-table-column>

      <el-table-column label="操作" width="70" align="center" fixed="right">
        <template #default="{ $index }">
          <el-button link type="danger" size="small" :icon="Delete" @click="removeDataRow($index)" />
        </template>
      </el-table-column>

      <template #empty>
        <div class="items-empty">暂无数据项，点击「新增数据行」开始编排</div>
      </template>
    </el-table>

    <el-empty v-else description="请先在「编辑数据源」中定义字段(key)" :image-size="80" />

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="dataColumns.length === 0" @click="saveDataItems">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDatasetDetail, updateDataset } from '@/api/dataset'
import type { CaseDatasetInfo } from '@/api/types'

const props = defineProps<{
  modelValue: boolean
  dataset: CaseDatasetInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const saving = ref(false)
const loading = ref(false)
/** 原始 keys（含 desc，保存时回传避免描述丢失） */
const detailKeys = ref<string | null>(null)
/** 字段名（来自模板 keys，只读） */
const dataColumns = ref<string[]>([])
/** 数据行（每行为 字段名 -> 字符串值） */
const dataRows = ref<Record<string, string>[]>([])

/** 从 keys JSON 数组解析字段名列表 */
function parseKeys(keys: string | null | undefined): string[] {
  if (!keys) return []
  try {
    const arr = JSON.parse(keys)
    if (Array.isArray(arr)) {
      return arr.map((f) => String(f.key || '')).filter((k) => k)
    }
  } catch {
    // 忽略非法 JSON
  }
  return []
}

/** 从 items 解析数据行 */
function parseItems(items: { data: string | null }[] | undefined): Record<string, string>[] {
  if (!items || items.length === 0) return []
  const cols = dataColumns.value
  return items.map((it) => {
    const row: Record<string, string> = {}
    cols.forEach((c) => (row[c] = ''))
    if (it.data) {
      try {
        const obj = JSON.parse(it.data)
        Object.entries(obj).forEach(([k, v]) => {
          row[k] = v === null || v === undefined ? '' : String(v)
        })
      } catch {
        // 忽略非法 JSON
      }
    }
    return row
  })
}

async function loadItems() {
  if (!props.dataset) return
  loading.value = true
  try {
    const detail = await getDatasetDetail(props.dataset.id)
    detailKeys.value = detail.keys
    dataColumns.value = parseKeys(detail.keys)
    dataRows.value = parseItems(detail.items)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      loadItems()
    }
  }
)

function addDataRow() {
  const item: Record<string, string> = {}
  dataColumns.value.forEach((c) => (item[c] = ''))
  dataRows.value.push(item)
}

function removeDataRow(index: number) {
  dataRows.value.splice(index, 1)
}

async function saveDataItems() {
  if (!props.dataset) return
  saving.value = true
  try {
    const items = dataRows.value.map((row) => ({ data: JSON.stringify(row) }))
    await updateDataset({
      id: props.dataset.id,
      caseId: props.dataset.caseId,
      name: props.dataset.name,
      creatorName: props.dataset.creatorName || undefined,
      keys: detailKeys.value || undefined,
      items
    })
    ElMessage.success('数据项保存成功')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
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

.items-empty {
  padding: 32px 0;
  color: #9ca3af;
  font-size: 13px;
}
</style>
