<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑数据源' : '新增数据源'"
    width="560px"
    :close-on-click-modal="false"
    append-to-body
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <el-form ref="formEl" :model="form" :rules="rules" label-width="95px">
      <el-form-item label="数据源名称" prop="name" required>
        <el-input v-model="form.name" placeholder="请输入数据源名称" clearable maxlength="100" />
      </el-form-item>
      <el-form-item label="关联用例" prop="caseId" required>
        <el-select
          v-model="form.caseId"
          placeholder="请选择关联用例"
          filterable
          :disabled="caseIdLocked"
          class="form-select"
        >
          <el-option v-for="c in caseOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="添加人" prop="creatorName">
        <el-input v-model="form.creatorName" placeholder="请输入添加人" clearable maxlength="50" />
      </el-form-item>
      <el-form-item label="字段(key)" prop="keysText">
        <el-input
          v-model="form.keysText"
          type="textarea"
          :rows="4"
          placeholder="每行一个字段，格式 key:描述&#10;如：username:用户名&#10;password:密码"
          class="keys-textarea"
        />
        <div class="keys-hint">字段即参数变量名，用例中用 ${字段} 引用</div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createDataset, updateDataset } from '@/api/dataset'
import { getCaseList } from '@/api/case'
import type { CaseDatasetInfo, CaseDatasetSaveParams, OptionItem } from '@/api/types'
import { useProjectStore } from '@/stores/project'

const props = defineProps<{
  modelValue: boolean
  /** 编辑时传入；为空表示新增 */
  dataset: CaseDatasetInfo | null
  /** 新增时默认关联的用例ID（用例编辑页会锁定为当前用例） */
  defaultCaseId?: number
  /** 是否锁定关联用例不可修改 */
  caseIdLocked?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const projectStore = useProjectStore()
const formEl = ref<FormInstance>()
const saving = ref(false)
const caseOptions = ref<OptionItem[]>([])

const isEdit = computed(() => !!props.dataset)

const form = ref({
  name: '',
  caseId: undefined as number | undefined,
  creatorName: '',
  keysText: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  caseId: [{ required: true, message: '请选择关联用例', trigger: 'change' }]
}

/** 把 keys JSON 数组字符串格式化为 textarea 文本（每行 key:描述） */
function formatKeys(keys: string | null | undefined): string {
  if (!keys) return ''
  try {
    const arr = JSON.parse(keys)
    if (Array.isArray(arr)) {
      return arr
        .map((f) => `${f.key || ''}${f.desc ? ':' + f.desc : ''}`)
        .join('\n')
    }
  } catch {
    // 忽略非法 JSON
  }
  return ''
}

/** 把 textarea 文本解析为 keys JSON 数组字符串 */
function parseKeys(text: string): string | undefined {
  const fields = text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line)
    .map((line) => {
      const idx = line.indexOf(':')
      if (idx > 0) {
        return { key: line.slice(0, idx).trim(), desc: line.slice(idx + 1).trim() }
      }
      return { key: line, desc: '' }
    })
  return fields.length > 0 ? JSON.stringify(fields) : undefined
}

async function loadCaseOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    caseOptions.value = []
    return
  }
  try {
    const result = await getCaseList({ projectId, page: 1, size: 500 })
    caseOptions.value = result.records
  } catch {
    caseOptions.value = []
  }
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    formEl.value?.clearValidate()
    await loadCaseOptions()
    if (props.dataset) {
      form.value = {
        name: props.dataset.name,
        caseId: props.dataset.caseId,
        creatorName: props.dataset.creatorName || '',
        keysText: formatKeys(props.dataset.keys)
      }
    } else {
      form.value = {
        name: '',
        caseId: props.defaultCaseId,
        creatorName: '',
        keysText: ''
      }
    }
  }
)

async function handleSave() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload: CaseDatasetSaveParams = {
      id: props.dataset?.id,
      caseId: form.value.caseId!,
      name: form.value.name,
      creatorName: form.value.creatorName || undefined,
      keys: parseKeys(form.value.keysText)
    }
    if (isEdit.value && props.dataset) {
      await updateDataset(payload)
      ElMessage.success('数据源修改成功')
    } else {
      await createDataset(payload)
      ElMessage.success('数据源新增成功')
    }
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.form-select {
  width: 100%;
}

.keys-textarea :deep(.el-textarea__inner) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
}

.keys-hint {
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
