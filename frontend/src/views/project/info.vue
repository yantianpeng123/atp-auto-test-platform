<template>
  <div class="page">
    <el-empty v-if="!project" description="尚未选择项目" />

    <template v-else>
      <!-- 项目资料 -->
      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-header">
            <span class="card-title">项目信息</span>
            <el-button :icon="Refresh" @click="loadRole">刷新</el-button>
          </div>
        </template>

        <el-descriptions :column="2" border class="desc">
          <el-descriptions-item label="项目名称">{{ project.name }}</el-descriptions-item>
          <el-descriptions-item label="所属团队">
            {{ project.team || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="项目ID">{{ project.id }}</el-descriptions-item>
          <el-descriptions-item label="你在项目中的角色">
            <el-tag :type="roleTagType" effect="light">{{ roleLabel }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 成员管理（合并自 members.vue） -->
      <el-card shadow="never" class="card">
        <template #header>
          <div class="card-header">
            <span class="card-title">项目成员</span>
            <div class="header-actions">
              <el-button v-if="canManage" type="primary" :icon="Plus" @click="openAdd">
                添加成员
              </el-button>
              <el-button :icon="Refresh" @click="loadList">刷新</el-button>
            </div>
          </div>
        </template>

        <el-alert
          v-if="!canManage"
          type="info"
          :closable="false"
          class="tip"
          title="你在该项目中没有成员管理权限，仅可查看成员列表。"
        />

        <el-table :data="list" v-loading="loading" empty-text="暂无成员" style="width: 100%">
          <el-table-column prop="username" label="用户名" min-width="140" />
          <el-table-column prop="nickname" label="昵称" min-width="140" show-overflow-tooltip />
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.role"
                size="small"
                :disabled="!canManage || row.role === 'OWNER'"
                class="role-select"
                @change="(val: ProjectRole) => handleRoleChange(row, val)"
              >
                <el-option
                  v-for="opt in ROLE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="inviterName" label="邀请人" min-width="140">
            <template #default="{ row }">
              <span v-if="row.inviterName">{{ row.inviterName }}</span>
              <span v-else class="text-muted">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="加入时间" min-width="180" />
          <el-table-column label="操作" width="120" fixed="right" v-if="canManage">
            <template #default="{ row }">
              <el-button
                link
                type="danger"
                :icon="Delete"
                :disabled="row.role === 'OWNER'"
                @click="handleRemove(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 添加成员弹框 -->
      <el-dialog v-model="addVisible" title="添加成员" width="460px" append-to-body @closed="resetAddForm">
        <el-form :model="addForm" label-width="80px">
          <el-form-item label="成员" required>
            <el-select
              v-model="addForm.username"
              placeholder="请选择要添加的用户"
              filterable
              clearable
              :loading="userLoading"
              class="user-select"
              style="width: 100%"
            >
              <el-option
                v-for="opt in availableUsers"
                :key="opt.username"
                :label="opt.nickname ? `${opt.username}（${opt.nickname}）` : opt.username"
                :value="opt.username"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="角色" required>
            <el-select v-model="addForm.role" class="role-select">
              <el-option
                v-for="opt in ADD_ROLE_OPTIONS"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="addVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleAdd">确定</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addMember,
  getMembers,
  removeMember,
  updateMemberRole
} from '@/api/projectMember'
import { getUserList } from '@/api/user'
import type { ProjectMember, ProjectRole, UserInfo } from '@/api/types'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'

const projectStore = useProjectStore()
const userStore = useUserStore()

const project = computed(() => projectStore.currentProject)
const role = computed(() => projectStore.currentProjectRole)

const ROLE_LABELS: Record<string, string> = {
  OWNER: '拥有者',
  MAINTAINER: '维护者',
  DEVELOPER: '开发者',
  VIEWER: '只读'
}

const roleLabel = computed(() => (role.value ? ROLE_LABELS[role.value] || role.value : '未知'))
const roleTagType = computed(() => {
  switch (role.value) {
    case 'OWNER':
      return 'danger'
    case 'MAINTAINER':
      return 'warning'
    case 'DEVELOPER':
      return 'success'
    case 'VIEWER':
      return 'info'
    default:
      return 'info'
  }
})

function loadRole() {
  if (project.value) {
    projectStore.loadMyRole(project.value.id)
  }
}

/* ---- 成员管理（合并自 members.vue） ---- */
const projectId = computed(() => projectStore.currentProject?.id)

/** 是否可管理成员：全局 ADMIN 或项目 OWNER/MAINTAINER */
const canManage = computed(
  () => userStore.isAdmin || projectStore.canManageProject
)

const ROLE_OPTIONS: { value: ProjectRole; label: string }[] = [
  { value: 'OWNER', label: '拥有者' },
  { value: 'MAINTAINER', label: '维护者' },
  { value: 'DEVELOPER', label: '开发者' },
  { value: 'VIEWER', label: '只读' }
]
// 添加成员时不允许直接指定为 OWNER（避免多 OWNER 引发权限混乱，由创建者持有）
const ADD_ROLE_OPTIONS = ROLE_OPTIONS.filter((o) => o.value !== 'OWNER')

const loading = ref(false)
const list = ref<ProjectMember[]>([])

async function loadList() {
  if (!projectId.value) {
    list.value = []
    return
  }
  loading.value = true
  try {
    list.value = await getMembers(projectId.value)
  } catch {
    // 后端未实现 / 无权限：保持空列表，错误提示由请求拦截器统一处理
    list.value = []
  } finally {
    loading.value = false
  }
}

/** 行内修改角色 */
async function handleRoleChange(row: ProjectMember, val: ProjectRole) {
  try {
    await updateMemberRole({ id: row.id, role: val })
    ElMessage.success('角色已更新')
  } catch {
    // 失败回滚显示值
    await loadList()
  }
}

/** 移除成员 */
async function handleRemove(row: ProjectMember) {
  try {
    await ElMessageBox.confirm(
      `确定将成员「${row.nickname || row.username}」移出该项目吗？`,
      '提示',
      { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await removeMember(row.id)
  ElMessage.success('已移除成员')
  loadList()
}

/* ---- 添加成员 ---- */
const addVisible = ref(false)
const submitting = ref(false)
const addForm = reactive<{ username: string; role: ProjectRole }>({
  username: '',
  role: 'DEVELOPER'
})

/** 用户下拉数据源：平台所有用户 */
const userOptions = ref<UserInfo[]>([])
const userLoading = ref(false)

async function loadUserOptions() {
  userLoading.value = true
  try {
    userOptions.value = await getUserList()
  } catch {
    userOptions.value = []
  } finally {
    userLoading.value = false
  }
}

/** 已存在于本项目内的用户，从下拉中排除 */
const memberUsernames = computed(() => new Set(list.value.map((m) => m.username)))
const availableUsers = computed(() =>
  userOptions.value.filter((u) => !memberUsernames.value.has(u.username))
)

function openAdd() {
  addVisible.value = true
  loadUserOptions()
}

function resetAddForm() {
  addForm.username = ''
  addForm.role = 'DEVELOPER'
}

async function handleAdd() {
  if (!projectId.value) return
  if (!addForm.username) {
    ElMessage.warning('请选择要添加的成员')
    return
  }
  submitting.value = true
  try {
    await addMember({
      projectId: projectId.value,
      username: addForm.username.trim(),
      role: addForm.role
    })
    ElMessage.success('成员已添加')
    addVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadRole()
  loadList()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
}

.card {
  margin-bottom: 16px;
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
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.desc {
  margin-bottom: 16px;
}

.tip {
  margin-bottom: 16px;
}

.role-select {
  width: 120px;
}

.text-muted {
  color: #9ca3af;
}
</style>
