<template>
  <div class="component-page">
    <!-- ============ 编辑视图（独立组件，占用整个页面） ============ -->
    <ComponentEdit
      v-if="viewMode === 'edit'"
      :id="editingId"
      @back="closeEditor"
      @saved="closeEditor"
    />

    <!-- ============ 列表视图（页签：组合组件 / 数据生成器） ============ -->
    <template v-else>
      <el-tabs v-model="activeTab" class="module-tabs">
        <!-- 组合组件 -->
        <el-tab-pane label="接口组件" name="component">
          <!-- 查询栏 -->
          <el-card shadow="never" class="filter-card">
            <el-form :inline="true" :model="query" @submit.prevent>
              <el-form-item label="组件名称">
                <el-input
                  v-model="query.name"
                  placeholder="按名称模糊搜索"
                  clearable
                  class="filter-input"
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
              </el-form-item>
              <el-form-item class="filter-right">
                <el-button type="primary" :icon="Plus" @click="openCreate">新增组件</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 列表 -->
          <el-card shadow="never" class="table-card">
            <el-table :data="records" v-loading="loading" border stripe empty-text="暂无组合组件">
              <el-table-column label="组件名称" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="comp-name">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="描述" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.description" class="comp-desc">{{ row.description }}</span>
                  <span v-else class="comp-muted">—</span>
                </template>
              </el-table-column>
              <el-table-column label="步骤数" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ row.stepCount ?? 0 }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="更新时间" width="170" prop="updateTime" />
              <el-table-column label="操作" width="150" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button>
                  <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager">
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
          </el-card>
        </el-tab-pane>

        <!-- 数据生成器（内嵌管理页） -->
        <el-tab-pane label="数据生成器" name="generator">
          <GeneratorManage />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComponentList, deleteComponent } from '@/api/component'
import { useProjectStore } from '@/stores/project'
import type { ApiComponentInfo } from '@/api/types'
import ComponentEdit from './edit.vue'
import GeneratorManage from '@/views/base/generator/index.vue'

const projectStore = useProjectStore()

/* ============ 页签 ============ */
const activeTab = ref<'component' | 'generator'>('component')

/* ============ 列表 ============ */
const loading = ref(false)
const records = ref<ApiComponentInfo[]>([])
const total = ref(0)
const query = reactive({ name: '', page: 1, size: 10 })

async function loadList() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    records.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getComponentList({
      projectId,
      name: query.name.trim() || undefined,
      page: query.page,
      size: query.size
    })
    records.value = res.records.map((c) => ({ ...c, stepCount: c.steps?.length ?? 0 }))
    total.value = res.total
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
  query.page = 1
  loadList()
}

/* ============ 编辑视图切换 ============ */
const viewMode = ref<'list' | 'edit'>('list')
const editingId = ref<number | null>(null)

function openCreate() {
  activeTab.value = 'component'
  editingId.value = null
  viewMode.value = 'edit'
}

function openEdit(row: ApiComponentInfo) {
  activeTab.value = 'component'
  editingId.value = row.id
  viewMode.value = 'edit'
}

function closeEditor() {
  viewMode.value = 'list'
  editingId.value = null
  activeTab.value = 'component'
  loadList()
}

/* ============ 删除 ============ */
async function handleDelete(row: ApiComponentInfo) {
  try {
    await ElMessageBox.confirm(`确定删除组合组件「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteComponent(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.component-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.module-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-input {
  width: 220px;
}

.filter-right {
  margin-left: auto;
}

.comp-name {
  font-weight: 500;
  color: #1f2937;
}

.comp-desc {
  color: #606266;
}

.comp-muted {
  color: #c0c4cc;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
