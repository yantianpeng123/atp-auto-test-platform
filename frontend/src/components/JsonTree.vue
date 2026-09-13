<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * 递归 JSON 美化组件：key 着色、value 按类型着色、逐层折叠。
 * 深层（depth >= 4）默认折叠，避免长 JSON 刷屏。
 */
defineOptions({ name: 'JsonTree' })

const props = defineProps<{
  data: unknown
  name?: string | number
  depth?: number
  isLast?: boolean
}>()

const isArray = computed(() => Array.isArray(props.data))
const isObject = computed(
  () => props.data !== null && typeof props.data === 'object' && !isArray.value
)
const isLeaf = computed(() => !isArray.value && !isObject.value)

const dataType = computed<'array' | 'object' | 'string' | 'number' | 'boolean' | 'null'>(() => {
  if (props.data === null) return 'null'
  if (isArray.value) return 'array'
  if (isObject.value) return 'object'
  return typeof props.data as 'string' | 'number' | 'boolean'
})

const entries = computed<{ key: string | number; value: unknown }[]>(() => {
  if (isArray.value) {
    return (props.data as unknown[]).map((v, i) => ({ key: i, value: v }))
  }
  if (isObject.value) {
    return Object.entries(props.data as Record<string, unknown>).map(([k, v]) => ({ key: k, value: v }))
  }
  return []
})

const collapsed = ref((props.depth ?? 0) >= 4)
function toggle() {
  collapsed.value = !collapsed.value
}

function valueClass(t: string): string {
  if (t === 'string') return 'jt-string'
  if (t === 'number') return 'jt-number'
  if (t === 'boolean') return 'jt-boolean'
  return 'jt-null'
}

function valueText(v: unknown): string {
  if (typeof v === 'string') return `"${v}"`
  if (v === null) return 'null'
  return String(v)
}

function keyLabel(k: string | number): string {
  return typeof k === 'number' ? `${k}` : `"${k}"`
}
</script>

<template>
  <span class="jt-node">
    <!-- 对象 / 数组：可折叠 -->
    <template v-if="!isLeaf">
      <span class="jt-toggle" @click="toggle">{{ collapsed ? '▶' : '▼' }}</span>
      <span v-if="name !== undefined" class="jt-key">{{ keyLabel(name) }}</span>
      <span class="jt-bracket">{{ isArray ? '[' : '{' }}</span>
      <span v-if="collapsed" class="jt-collapsed">
        … {{ entries.length }} {{ isArray ? 'items' : 'keys' }}
      </span>
      <template v-else>
        <span class="jt-bracket">{{ isArray ? ']' : '}' }}</span>
      </template>
      <div v-if="!collapsed" class="jt-children">
        <JsonTree
          v-for="(e, i) in entries"
          :key="i"
          :data="e.value"
          :name="e.key"
          :depth="(depth ?? 0) + 1"
          :is-last="i === entries.length - 1"
        />
      </div>
    </template>

    <!-- 叶子：key: value -->
    <template v-else>
      <span v-if="name !== undefined" class="jt-key">{{ keyLabel(name) }}</span>
      <span :class="valueClass(dataType)">{{ valueText(data) }}</span>
    </template>
  </span>
</template>

<style scoped>
.jt-node {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
  line-height: 1.7;
  color: #303133;
  white-space: normal;
  word-break: break-word;
}

.jt-toggle {
  cursor: pointer;
  color: #909399;
  margin-right: 4px;
  user-select: none;
  display: inline-block;
  width: 12px;
  font-size: 10px;
}

.jt-key {
  color: #1d63d1;
  margin-right: 4px;
}

.jt-bracket {
  color: #909399;
}

.jt-collapsed {
  color: #b0b3b8;
  font-style: italic;
  margin-left: 4px;
}

.jt-children {
  padding-left: 18px;
  border-left: 1px dashed #e8eaed;
  margin-left: 6px;
}

.jt-string {
  color: #2e7d32;
}

.jt-number {
  color: #c2410c;
}

.jt-boolean {
  color: #7c3aed;
}

.jt-null {
  color: #9ca3af;
}
</style>
