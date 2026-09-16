<template>
  <el-dialog
    :model-value="modelValue"
    title="新增组件生成器"
    width="620px"
    :close-on-click-modal="false"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
    @open="onOpen"
  >
    <el-form label-width="110px" class="gen-form">
      <el-form-item label="生成器名称" required>
        <el-input v-model="name" placeholder="如 默认手机号 / 8位订单号" maxlength="100" clearable />
      </el-form-item>

      <el-form-item label="类型" required>
        <el-select v-model="type" class="gen-type-select" @change="onTypeChange">
          <el-option label="随机数" value="RANDOM" />
          <el-option label="手机号" value="PHONE" />
          <el-option label="身份证号" value="IDCARD" />
          <el-option label="姓名" value="NAME" />
          <el-option label="枚举" value="ENUM" />
          <el-option label="时间戳" value="TIMESTAMP" />
          <el-option label="UUID" value="UUID" />
          <el-option label="自定义表达式" value="CUSTOM" />
        </el-select>
      </el-form-item>

      <!-- 随机数参数 -->
      <template v-if="type === 'RANDOM'">
        <el-form-item label="长度">
          <el-input-number v-model="randomLength" :min="1" :max="64" />
        </el-form-item>
        <el-form-item label="字符集">
          <el-select v-model="randomCharset" class="gen-type-select">
            <el-option label="纯数字" value="digits" />
            <el-option label="纯字母" value="alpha" />
            <el-option label="字母数字" value="alnum" />
          </el-select>
        </el-form-item>
        <el-form-item label="前缀">
          <el-input v-model="randomPrefix" placeholder="如 ORD-" clearable />
        </el-form-item>
        <el-form-item label="后缀">
          <el-input v-model="randomSuffix" placeholder="如 -END" clearable />
        </el-form-item>
      </template>

      <!-- 枚举参数 -->
      <el-form-item v-else-if="type === 'ENUM'" label="候选值">
        <el-input
          v-model="enumValues"
          type="textarea"
          :rows="3"
          placeholder="逗号分隔，如 A,B,C 或 成功,失败"
        />
      </el-form-item>

      <!-- 时间戳参数 -->
      <template v-else-if="type === 'TIMESTAMP'">
        <el-form-item label="格式">
          <el-input v-model="tsFormat" placeholder="默认 ms；或 yyyyMMdd HH:mm:ss" clearable />
        </el-form-item>
        <el-form-item label="偏移量">
          <el-input v-model="tsOffset" placeholder="如 +1d / -2h（可选）" clearable />
        </el-form-item>
      </template>

      <!-- 自定义表达式 -->
      <template v-else-if="type === 'CUSTOM'">
        <el-form-item label="表达式模板" required>
          <el-input
            v-model="customTemplate"
            type="textarea"
            :rows="3"
            placeholder="如 NO-${randomInt(6)}-${enum(A,B)}"
          />
        </el-form-item>
        <el-form-item label="可用函数">
          <div class="func-help">
            <el-tag
              v-for="f in functions"
              :key="f.name"
              size="small"
              class="func-tag"
              @click="insertFunc(f)"
            >
              {{ f.name }}{{ f.args }}
            </el-tag>
          </div>
          <div class="func-desc">点击函数名插入到模板末尾</div>
        </el-form-item>
      </template>

      <el-form-item v-else label="参数">
        <el-alert type="info" :closable="false" title="该类型无需额外参数，生成器将直接产出对应值。" />
      </el-form-item>

      <!-- 输出变量名（作为步骤时的默认绑定） -->
      <el-form-item label="输出变量名">
        <el-input
          v-model="variableName"
          placeholder="如 phoneVar，后续步骤用 ${phoneVar} 引用"
          clearable
        />
      </el-form-item>
      <el-form-item label="每次执行重新生成">
        <el-switch v-model="regenEachRun" />
        <span class="form-hint">关闭则整个执行过程固定同一值（多步骤引用同一随机值）</span>
      </el-form-item>

      <el-form-item label="描述">
        <el-input v-model="description" type="textarea" :rows="2" placeholder="可选" />
      </el-form-item>

      <!-- 实时预览 -->
      <el-form-item label="实时预览">
        <div class="preview-box">
          <code>{{ previewResult || '点击「试生成」查看结果' }}</code>
          <el-button size="small" type="primary" link :loading="previewing" @click="runPreview">试生成</el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">确定添加</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createGenerator,
  getGeneratorFunctions,
  previewGenerator,
  updateGenerator
} from '@/api/generator'
import type {
  DataGeneratorInfo,
  GeneratorFunctionInfo,
  GeneratorType
} from '@/api/types'

const props = defineProps<{
  modelValue: boolean
  projectId: number
  /** 编辑时传入完整数据；新增为 null */
  editData: DataGeneratorInfo | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved', info: DataGeneratorInfo): void
}>()

const name = ref('')
const type = ref<GeneratorType>('RANDOM')
const description = ref('')
const variableName = ref('')
const regenEachRun = ref(true)

/* 各类型参数（本地 ref，保存时组装为 params） */
const randomLength = ref(8)
const randomCharset = ref<'digits' | 'alpha' | 'alnum'>('digits')
const randomPrefix = ref('')
const randomSuffix = ref('')
const enumValues = ref('')
const tsFormat = ref('')
const tsOffset = ref('')
const customTemplate = ref('')

const functions = ref<GeneratorFunctionInfo[]>([])
const previewResult = ref('')
const previewing = ref(false)
const saving = ref(false)

function buildParams(): Record<string, unknown> {
  switch (type.value) {
    case 'RANDOM':
      return {
        length: randomLength.value,
        charset: randomCharset.value,
        prefix: randomPrefix.value,
        suffix: randomSuffix.value
      }
    case 'ENUM':
      return { values: enumValues.value }
    case 'TIMESTAMP':
      return { format: tsFormat.value, offset: tsOffset.value }
    case 'CUSTOM':
      return { template: customTemplate.value }
    default:
      return {}
  }
}

function applyParams(p: Record<string, unknown> | null | undefined) {
  randomLength.value = Number(p?.length ?? 8)
  randomCharset.value = (p?.charset as 'digits' | 'alpha' | 'alnum') ?? 'digits'
  randomPrefix.value = String(p?.prefix ?? '')
  randomSuffix.value = String(p?.suffix ?? '')
  enumValues.value = String(p?.values ?? '')
  tsFormat.value = String(p?.format ?? '')
  tsOffset.value = String(p?.offset ?? '')
  customTemplate.value = String(p?.template ?? '')
}

function onTypeChange() {
  // 切换类型时清空参数，避免脏数据
  applyParams({})
}

function onOpen() {
  getGeneratorFunctions().then((f) => (functions.value = f))
  if (props.editData) {
    const d = props.editData
    name.value = d.name
    type.value = d.type
    description.value = d.description ?? ''
    variableName.value = String(d.params?.variableName ?? '')
    regenEachRun.value = d.params?.regenEachRun === false ? false : true
    applyParams(d.params)
  } else {
    name.value = ''
    type.value = 'RANDOM'
    description.value = ''
    variableName.value = ''
    regenEachRun.value = true
    applyParams({})
  }
  previewResult.value = ''
}

function insertFunc(f: GeneratorFunctionInfo) {
  const snippet = f.example ? f.example : `\${${f.name}()}`
  customTemplate.value = customTemplate.value ? `${customTemplate.value}${snippet}` : snippet
}

async function runPreview() {
  if (type.value === 'CUSTOM' && !customTemplate.value.trim()) {
    ElMessage.warning('请先填写表达式模板')
    return
  }
  previewing.value = true
  try {
    const res = await previewGenerator({ projectId: props.projectId, type: type.value, params: buildParams() })
    previewResult.value = res.result
  } finally {
    previewing.value = false
  }
}

async function handleSave() {
  if (!name.value.trim()) {
    ElMessage.error('请填写生成器名称')
    return
  }
  if (type.value === 'CUSTOM' && !customTemplate.value.trim()) {
    ElMessage.error('请填写表达式模板')
    return
  }
  const params = buildParams()
  params.variableName = variableName.value.trim() || undefined
  params.regenEachRun = regenEachRun.value
  const payload = {
    id: props.editData?.id,
    projectId: props.projectId,
    name: name.value.trim(),
    type: type.value,
    params,
    description: description.value.trim() || undefined
  }
  saving.value = true
  try {
    let info: DataGeneratorInfo
    if (props.editData) {
      info = await updateGenerator(payload)
      ElMessage.success('生成器已更新')
    } else {
      info = await createGenerator(payload)
      ElMessage.success('生成器已创建')
    }
    emit('update:modelValue', false)
    emit('saved', info)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.gen-type-select {
  width: 240px;
}
.func-help {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.func-tag {
  cursor: pointer;
}
.func-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
.preview-box {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 12px;
  min-width: 320px;
}
.preview-box code {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  color: #1677ff;
}
</style>
