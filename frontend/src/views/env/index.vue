<template>
  <div class="page">
    <el-card shadow="never" class="card">
      <template #header>
        <div class="card-header">
          <span class="card-title">环境配置</span>
          <el-button type="primary" :icon="Plus" @click="handleOpenCreate">新增环境</el-button>
        </div>
      </template>

      <!-- 查询条件区 -->
      <div class="filter-bar">
        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="环境名称">
            <el-input
              v-model="query.name"
              placeholder="请输入环境名称"
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

      <el-table :data="list" v-loading="loading" empty-text="暂无环境，点击右上角新增" style="width: 100%">
        <el-table-column prop="name" label="环境名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="baseUrl" label="基础域名" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.baseUrl || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="全局请求头" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ headerSummary(row.headers) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据库配置" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ dbSummary(row.dbConfig) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleOpenEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑环境' : '新增环境'"
      width="620px"
      :close-on-click-modal="false"
    >
      <el-form ref="formEl" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="环境名称" prop="name">
          <el-input v-model="form.name" placeholder="如：测试环境 / 预发环境" clearable maxlength="50" />
        </el-form-item>

        <el-form-item label="基础域名" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如：https://test.example.com" clearable maxlength="255" />
        </el-form-item>

        <el-form-item label="全局请求头">
          <div class="kv-editor">
            <div v-for="(_, index) in form.headers" :key="index" class="kv-row">
              <el-input v-model="form.headers[index].key" placeholder="名称" class="kv-key" />
              <el-input v-model="form.headers[index].value" placeholder="值" class="kv-value" />
              <el-button link type="danger" :icon="Delete" @click="handleRemoveHeader(index)">删除</el-button>
            </div>
            <el-button link type="primary" :icon="Plus" @click="handleAddHeader">添加请求头</el-button>
          </div>
        </el-form-item>

        <el-form-item label="数据库配置">
          <div class="db-editor">
            <div class="db-row">
              <el-input v-model="form.db.host" placeholder="主机地址" />
              <el-input v-model="form.db.port" placeholder="端口" class="db-port" />
            </div>
            <div class="db-row">
              <el-input v-model="form.db.dbName" placeholder="库名" />
              <el-input v-model="form.db.username" placeholder="用户名" />
            </div>
            <el-input v-model="form.db.password" placeholder="密码" show-password class="db-pwd" />
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createEnv, deleteEnv, getEnvList, updateEnv } from '@/api/env'
import type { DbConfig, EnvInfo } from '@/api/types'
import { useProjectStore } from '@/stores/project'

interface HeaderRow {
  key: string
  value: string
}

const projectStore = useProjectStore()

const loading = ref(false)
const submitting = ref(false)
const list = ref<EnvInfo[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formEl = ref<FormInstance>()
const editingId = ref<number | null>(null)

const isEdit = computed(() => editingId.value !== null)

const query = reactive({
  name: '',
  page: 1,
  size: 10
})

const form = reactive({
  name: '',
  baseUrl: '',
  headers: [] as HeaderRow[],
  db: { host: '', port: '', dbName: '', username: '', password: '' } as DbConfig
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入环境名称', trigger: 'blur' }]
}

/** 请求头 JSON → 表格行；解析失败按空处理，不阻塞编辑 */
function parseHeaders(json: string | null): HeaderRow[] {
  if (!json) return []
  try {
    const obj = JSON.parse(json) as Record<string, unknown>
    return Object.entries(obj).map(([key, value]) => ({ key, value: String(value) }))
  } catch {
    return []
  }
}

function headerSummary(json: string | null): string {
  const rows = parseHeaders(json)
  if (rows.length === 0) return '—'
  return rows.map((r) => r.key).join('、')
}

function dbSummary(json: string | null): string {
  if (!json) return '—'
  try {
    const db = JSON.parse(json) as Partial<DbConfig>
    if (!db.host) return '—'
    const port = db.port ? `:${db.port}` : ''
    const name = db.dbName ? `/${db.dbName}` : ''
    return `${db.host}${port}${name}`
  } catch {
    return '—'
  }
}

async function loadList() {
  loading.value = true
  try {
    const result = await getEnvList({
      projectId: projectStore.currentProject?.id,
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

function resetForm() {
  form.name = ''
  form.baseUrl = ''
  form.headers = []
  form.db = { host: '', port: '', dbName: '', username: '', password: '' }
}

function handleOpenCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  formEl.value?.clearValidate()
}

function handleOpenEdit(row: EnvInfo) {
  editingId.value = row.id
  form.name = row.name
  form.baseUrl = row.baseUrl || ''
  form.headers = parseHeaders(row.headers)
  try {
    form.db = { host: '', port: '', dbName: '', username: '', password: '', ...(row.dbConfig ? JSON.parse(row.dbConfig) : {}) }
  } catch {
    form.db = { host: '', port: '', dbName: '', username: '', password: '' }
  }
  dialogVisible.value = true
  formEl.value?.clearValidate()
}

function handleAddHeader() {
  form.headers.push({ key: '', value: '' })
}

function handleRemoveHeader(index: number) {
  form.headers.splice(index, 1)
}

function buildHeadersJson(): string | undefined {
  const obj: Record<string, string> = {}
  for (const row of form.headers) {
    const key = row.key.trim()
    if (key) {
      obj[key] = row.value
    }
  }
  return Object.keys(obj).length > 0 ? JSON.stringify(obj) : undefined
}

function buildDbJson(): string | undefined {
  const { host, port, dbName, username, password } = form.db
  const hasValue = [host, port, dbName, username, password].some((v) => v && String(v).trim())
  if (!hasValue) return undefined
  return JSON.stringify({
    host: String(host || '').trim(),
    port: String(port || '').trim(),
    dbName: String(dbName || '').trim(),
    username: String(username || '').trim(),
    password: String(password ?? '')
  })
}

async function handleSubmit() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name,
      baseUrl: form.baseUrl || undefined,
      headers: buildHeadersJson(),
      dbConfig: buildDbJson()
    }
    if (isEdit.value && editingId.value !== null) {
      await updateEnv({ id: editingId.value, ...payload })
      ElMessage.success('环境修改成功')
    } else {
      await createEnv({ projectId, ...payload })
      ElMessage.success('环境新增成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: EnvInfo) {
  try {
    await ElMessageBox.confirm(`确定删除环境「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteEnv(row.id)
  ElMessage.success('环境删除成功')
  loadList()
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

/* ---------- 请求头键值编辑 ---------- */
.kv-editor {
  width: 100%;
}

.kv-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.kv-key {
  width: 160px;
  flex: none;
}

.kv-value {
  flex: 1;
  min-width: 0;
}

/* ---------- 数据库配置 ---------- */
.db-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.db-row {
  display: flex;
  gap: 8px;
}

.db-row > * {
  flex: 1;
  min-width: 0;
}

.db-port {
  flex: none;
  width: 110px;
}

.db-pwd {
  width: 100%;
}
</style>
