<template>
  <div class="project-page">
    <!-- 顶部 -->
    <header class="page-header">
      <div class="brand">
        <span class="brand-mark">ATP</span>
        <span class="brand-name">自动化测试平台</span>
      </div>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-entry">
          <span class="user-name">{{ userStore.displayName }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <main class="page-main">
      <div class="title-row">
        <h2 class="page-title">选择项目</h2>
        <el-button v-if="userStore.isAdmin" type="primary" :icon="Plus" @click="handleOpenCreate">
          新增项目
        </el-button>
      </div>

      <div v-loading="loading" class="card-grid">
        <el-empty
          v-if="!loading && projects.length === 0"
          description="暂无项目"
        />
        <el-card
          v-for="p in projects"
          :key="p.id"
          shadow="hover"
          class="project-card"
        >
          <div class="card-title">{{ p.name }}</div>
          <div class="card-meta">
            <div class="meta-row">
              <span class="meta-label">项目ID</span>
              <span class="meta-value">{{ p.id }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">所属团队</span>
              <span class="meta-value">{{ p.team || '未设置' }}</span>
            </div>
          </div>
          <el-button type="primary" class="enter-btn" @click="handleEnter(p)">进入项目</el-button>
        </el-card>
      </div>
    </main>

    <!-- 新增项目弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增项目" width="480px" :close-on-click-modal="false">
      <el-form ref="formEl" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" clearable maxlength="100" />
        </el-form-item>
        <el-form-item label="所属团队" prop="team">
          <el-input v-model="form.team" placeholder="请输入团队名称" clearable maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown, Plus } from '@element-plus/icons-vue'
import { createProject, listProjects } from '@/api/project'
import type { Project } from '@/api/types'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const projectStore = useProjectStore()

const loading = ref(false)
const projects = ref<Project[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formEl = ref<FormInstance>()

const form = reactive({
  name: '',
  team: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
}

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    loading.value = false
  }
}

function handleEnter(project: Project) {
  projectStore.setCurrentProject(project)
  ElMessage.success(`已进入项目：${project.name}`)
  router.push('/dashboard')
}

function handleOpenCreate() {
  form.name = ''
  form.team = ''
  dialogVisible.value = true
}

async function handleCreate() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createProject({ name: form.name, team: form.team })
    ElMessage.success('项目新增成功')
    dialogVisible.value = false
    loadProjects()
  } finally {
    submitting.value = false
  }
}

async function handleCommand(command: string | number | object) {
  if (command === 'logout') {
    await userStore.logout()
  }
}

onMounted(loadProjects)
</script>

<style scoped>
.project-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: #0f2444;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #1d63d1;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.brand-name {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  color: #e5e7eb;
}

.user-name {
  font-size: 14px;
}

.page-main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 24px;
}

.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.project-card {
  display: flex;
  flex-direction: column;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 14px;
}

.card-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  flex: 1;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.meta-label {
  color: #909399;
}

.meta-value {
  color: #374151;
}

.enter-btn {
  width: 100%;
}
</style>
