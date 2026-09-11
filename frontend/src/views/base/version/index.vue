<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">工程版本信息</span>
          <el-button type="primary" :icon="Plus" @click="handleOpenCreate">新增</el-button>
        </div>
      </template>

      <!-- 查询条件区 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="工程名称">
            <el-select
              v-model="query.applicationId"
              placeholder="请选择工程"
              clearable
              filterable
              class="filter-select"
              @change="handleProjectChange"
            >
              <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="版本名称">
            <el-select
              v-model="query.versionId"
              placeholder="请选择版本"
              clearable
              filterable
              class="filter-select"
              @change="handleVersionChange"
            >
              <el-option v-for="item in versionOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="模块名称">
            <el-select
              v-model="query.moduleId"
              placeholder="请选择模块"
              clearable
              filterable
              class="filter-select"
              @change="handleSearch"
            >
              <el-option v-for="item in moduleOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 结果列表 -->
      <el-table :data="list" v-loading="loading" empty-text="暂无数据，请调整查询条件后重试" style="width: 100%">
        <el-table-column prop="moduleId" label="ID" width="80" />
        <el-table-column prop="applicationName" label="工程名称" min-width="140" />
        <el-table-column prop="versionName" label="版本名称" min-width="120">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">{{ row.versionName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="moduleName" label="模块名称" min-width="140" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
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

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增"
      width="520px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formEl" :model="form" :rules="rules" label-width="90px" class="create-form">
        <el-form-item label="新增类型">
          <el-radio-group v-model="form.type" @change="handleTypeChange">
            <el-radio v-for="item in typeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.type === 'PROJECT'" label="工程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入工程名称" clearable maxlength="100" />
        </el-form-item>

        <el-form-item v-if="form.type !== 'PROJECT'" label="工程名称" prop="applicationId">
          <el-select
            v-model="form.applicationId"
            placeholder="请选择工程"
            clearable
            filterable
            class="filter-select"
            @change="handleDialogProjectChange"
          >
            <el-option v-for="item in projectOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="工程描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入工程描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item v-if="form.type === 'VERSION'" label="版本名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入版本名称" clearable maxlength="100" />
        </el-form-item>

        <el-form-item v-if="form.type === 'MODULE'" label="版本名称" prop="versionId">
          <el-select
            v-model="form.versionId"
            placeholder="请选择版本"
            clearable
            filterable
            class="filter-select"
          >
            <el-option v-for="item in dialogVersionOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.type === 'MODULE'" label="模块名称" prop="moduleName">
          <div class="module-add">
            <el-input
              v-model="form.moduleName"
              placeholder="请输入模块名称"
              clearable
              maxlength="100"
              @keyup.enter="handleAddModule"
            />
            <el-button type="primary" plain :icon="Plus" @click="handleAddModule">添加模块</el-button>
          </div>
          <div v-if="moduleList.length" class="module-list">
            <div v-for="(_, index) in moduleList" :key="index" class="module-row">
              <el-input v-model="moduleList[index]" maxlength="100" placeholder="模块名称" />
              <el-button link type="danger" :icon="Delete" @click="handleRemoveModule(index)">删除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createModules,
  createProject,
  createVersion,
  getModuleOptions,
  getProjectOptions,
  getVersionList,
  getVersionOptions
} from '@/api/base'
import type { OptionItem, VersionInfo } from '@/api/types'
import { useProjectStore } from '@/stores/project'

const loading = ref(false)
const list = ref<VersionInfo[]>([])
const total = ref(0)

const projectStore = useProjectStore()

// 下拉选项数据（从接口获取）
const projectOptions = ref<OptionItem[]>([])
const versionOptions = ref<OptionItem[]>([])
const moduleOptions = ref<OptionItem[]>([])

const query = reactive({
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  moduleId: undefined as number | undefined,
  page: 1,
  size: 10
})

async function loadProjectOptions() {
  projectOptions.value = await getProjectOptions(projectStore.currentProject?.id)
}

async function loadVersionOptions(applicationId?: number) {
  versionOptions.value = await getVersionOptions(applicationId)
}

async function loadModuleOptions(versionId?: number) {
  moduleOptions.value = await getModuleOptions(versionId)
}

async function handleSearch() {
  loading.value = true
  try {
    const result = await getVersionList({
      projectId: projectStore.currentProject?.id,
      applicationId: query.applicationId,
      versionId: query.versionId,
      moduleId: query.moduleId,
      page: query.page,
      size: query.size
    })
    list.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleProjectChange() {
  query.versionId = undefined
  query.moduleId = undefined
  versionOptions.value = []
  moduleOptions.value = []
  loadVersionOptions(query.applicationId)
  handleSearch()
}

function handleVersionChange() {
  query.moduleId = undefined
  moduleOptions.value = []
  loadModuleOptions(query.versionId)
  handleSearch()
}

function handleReset() {
  query.applicationId = undefined
  query.versionId = undefined
  query.moduleId = undefined
  query.page = 1
  versionOptions.value = []
  moduleOptions.value = []
  handleSearch()
}

/* ==================== 新增 ==================== */

const dialogVisible = ref(false)
const submitting = ref(false)
const formEl = ref<FormInstance>()

const typeOptions = [
  { label: '工程', value: 'PROJECT' },
  { label: '版本', value: 'VERSION' },
  { label: '模块', value: 'MODULE' }
]

const form = reactive({
  type: 'PROJECT' as 'PROJECT' | 'VERSION' | 'MODULE',
  applicationId: undefined as number | undefined,
  versionId: undefined as number | undefined,
  name: '',
  description: '',
  moduleName: ''
})

// 弹窗内版本下拉选项（随工程联动加载）
const dialogVersionOptions = ref<OptionItem[]>([])

const typeLabel = computed(() => {
  const map: Record<string, string> = { PROJECT: '工程', VERSION: '版本', MODULE: '模块' }
  return map[form.type] || ''
})

// 必填校验：被 v-if 隐藏的字段不参与 validate，规则按显示与否自动生效。
const rules = computed<FormRules>(() => {
  if (form.type === 'PROJECT') {
    return { name: [{ required: true, message: '请输入工程名称', trigger: 'blur' }] }
  }
  if (form.type === 'VERSION') {
    return {
      applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
      name: [{ required: true, message: '请输入版本名称', trigger: 'blur' }]
    }
  }
  return {
    applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
    versionId: [{ required: true, message: '请选择版本', trigger: 'change' }]
  }
})

const moduleList = ref<string[]>([])

function handleAddModule() {
  const name = form.moduleName.trim()
  if (!name) {
    ElMessage.warning('模块名称不能为空')
    return
  }
  moduleList.value.push(name)
  form.moduleName = ''
}

function handleRemoveModule(index: number) {
  moduleList.value.splice(index, 1)
}

function resetForm() {
  form.applicationId = undefined
  form.versionId = undefined
  form.name = ''
  form.description = ''
  form.moduleName = ''
  moduleList.value = []
  dialogVersionOptions.value = []
}

function handleOpenCreate() {
  form.type = 'PROJECT'
  resetForm()
  dialogVisible.value = true
}

function handleTypeChange() {
  // 切换类型时清空已填内容并重置校验状态
  resetForm()
  formEl.value?.clearValidate()
}

function handleDialogClosed() {
  resetForm()
  formEl.value?.clearValidate()
}

// 弹窗内：选择工程后联动加载版本下拉
async function handleDialogProjectChange() {
  form.versionId = undefined
  dialogVersionOptions.value = []
  if (form.applicationId) {
    dialogVersionOptions.value = await getVersionOptions(form.applicationId)
  }
}

async function handleSubmit() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  if (form.type === 'MODULE' && moduleList.value.length === 0) {
    ElMessage.warning('请至少添加一个模块名称')
    return
  }

  submitting.value = true
  try {
    if (form.type === 'PROJECT') {
      await createProject({ projectId: projectStore.currentProject!.id, name: form.name, description: form.description })
    } else if (form.type === 'VERSION') {
      await createVersion({ applicationId: form.applicationId!, name: form.name, description: form.description })
    } else {
      await createModules({ versionId: form.versionId!, names: moduleList.value, description: form.description })
    }
    ElMessage.success(`${typeLabel.value}新增成功`)
    dialogVisible.value = false
    await loadProjectOptions()
    handleSearch()
  } finally {
    submitting.value = false
  }
}

// 页面加载时：拉取工程下拉选项（版本/模块选项随工程/版本联动加载）
onMounted(async () => {
  await loadProjectOptions()
  handleSearch()
})
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

/* ---------- 查询条件区 ---------- */
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

.filter-select {
  width: 200px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

/* ---------- 新增弹窗 ---------- */
.create-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.module-add {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.module-add :deep(.el-input) {
  flex: 1;
}

.module-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.module-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.module-row :deep(.el-input) {
  flex: 1;
}
</style>
