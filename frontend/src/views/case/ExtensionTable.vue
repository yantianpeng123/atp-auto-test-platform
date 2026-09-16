<template>
  <div class="extension-table-wrap">
    <el-table :data="steps" size="small" border stripe empty-text="暂无扩展，点击右上角新增">
      <el-table-column label="扩展名称" min-width="160">
        <template #default="{ row }">
          <span class="ext-name">{{ extName(row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="返回数据变量" min-width="120">
        <template #default="{ row }">
          <span v-if="row.responseVar" class="ext-var">{{ row.responseVar }}</span>
          <span v-else-if="row.stepType === 3 && row.variableName" class="ext-var">{{ row.variableName }}</span>
          <span v-else class="ext-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="130">
        <template #default="{ row }">
          <el-tag v-if="row.stepType === 2" type="warning" size="small">公共接口组件</el-tag>
          <el-tag v-else-if="row.stepType === 3" type="success" size="small">生成变量</el-tag>
          <el-tag v-else size="small">其他类型</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否禁用" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isDisabled === 1 ? 'info' : 'success'" size="small">
            {{ row.isDisabled === 1 ? '已禁用' : '启用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提升全局变量" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.promoteGlobal === 1 ? 'primary' : 'info'" size="small">
            {{ row.promoteGlobal === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="扩展说明" min-width="140">
        <template #default="{ row }">
          <span v-if="row.description" class="ext-desc">{{ row.description }}</span>
          <span v-else class="ext-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="失败继续执行" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.continueOnFail === 1 ? 'success' : 'info'" size="small">
            {{ row.continueOnFail === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="$emit('edit', row)">编辑</el-button>
          <el-button type="danger" link size="small" @click="$emit('delete', row._uid)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { CaseExtensionStep } from '@/api/types'

const props = defineProps<{
  steps: CaseExtensionStep[]
  componentMap?: Record<number, string>
  generatorMap?: Record<number, string>
}>()

defineEmits<{
  (e: 'add'): void
  (e: 'edit', step: CaseExtensionStep): void
  (e: 'delete', uid: number): void
}>()

function extName(row: CaseExtensionStep): string {
  if (row.stepName) return row.stepName
  if (row.stepType === 3 && row.generatorId) {
    return props.generatorMap?.[row.generatorId] || `生成器 #${row.generatorId}`
  }
  if (row.componentId) {
    return props.componentMap?.[row.componentId] || `组件 #${row.componentId}`
  }
  return '未命名扩展'
}
</script>

<style scoped>
.extension-table-wrap {
  padding: 4px 0;
}
.ext-name {
  font-weight: 500;
  color: #1f2937;
}
.ext-var {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #1677ff;
}
.ext-desc {
  color: #606266;
}
.ext-muted {
  color: #c0c4cc;
}
</style>
