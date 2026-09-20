<template>
  <div class="notify-config">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">通知配置</span>
        </div>
      </template>

      <el-alert
        v-if="!canManage"
        type="warning"
        :closable="false"
        show-icon
        title="当前角色无权访问通知配置（需项目 OWNER / MAINTAINER 或全局管理员）"
      />

      <template v-else>
        <el-tabs v-model="activeTab" @tab-change="onTabChange">
          <!-- 渠道 -->
          <el-tab-pane label="通知渠道" name="channel">
            <div class="toolbar">
              <el-button type="primary" :icon="Plus" @click="openChannelCreate">新增渠道</el-button>
              <el-button :icon="Refresh" :loading="channelLoading" @click="loadChannels">刷新</el-button>
            </div>
            <el-table :data="channels" v-loading="channelLoading" empty-text="暂无渠道">
              <el-table-column prop="name" label="名称" min-width="160" />
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="channelTypeTag(row.type)">{{ channelTypeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="启用" width="90">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.enabled"
                    @change="(val: boolean) => toggleChannel(row, val)"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="170" />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openChannelEdit(row)">编辑</el-button>
                  <el-button link type="danger" @click="removeChannel(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 规则 -->
          <el-tab-pane label="通知规则" name="rule">
            <div class="toolbar">
              <el-button type="primary" :icon="Plus" @click="openRuleCreate">新增规则</el-button>
              <el-button :icon="Refresh" :loading="ruleLoading" @click="loadRules">刷新</el-button>
            </div>
            <el-table :data="rules" v-loading="ruleLoading" empty-text="暂无规则">
              <el-table-column label="触发事件" width="140">
                <template #default="{ row }">
                  <el-tag>{{ eventLabel(row.event) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="通知渠道" min-width="220">
                <template #default="{ row }">
                  <el-tag
                    v-for="cid in row.channelIds"
                    :key="cid"
                    class="ch-tag"
                    type="info"
                    effect="plain"
                  >
                    {{ channelName(cid) }}
                  </el-tag>
                  <span v-if="!row.channelIds || row.channelIds.length === 0" class="text-muted">未选择</span>
                </template>
              </el-table-column>
              <el-table-column label="条件" width="120">
                <template #default="{ row }">
                  <span v-if="row.condition && row.condition.onlyFail">仅失败时</span>
                  <span v-else class="text-muted">总是</span>
                </template>
              </el-table-column>
              <el-table-column label="启用" width="90">
                <template #default="{ row }">
                  <el-switch :model-value="row.enabled" disabled />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button link type="danger" @click="removeRule(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 发送日志 -->
          <el-tab-pane label="发送日志" name="log">
            <div class="toolbar">
              <el-button :icon="Refresh" :loading="logLoading" @click="loadLogs">刷新</el-button>
            </div>
            <el-table :data="logs" v-loading="logLoading" empty-text="暂无发送记录">
              <el-table-column prop="createTime" label="时间" width="170" />
              <el-table-column label="事件" width="120">
                <template #default="{ row }">
                  <el-tag>{{ eventLabel(row.event) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="渠道" width="100">
                <template #default="{ row }">{{ channelTypeLabel(row.channelType) }}</template>
              </el-table-column>
              <el-table-column prop="target" label="目标" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
                    {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
              <el-table-column prop="error" label="错误信息" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.error" class="err-text">{{ row.error }}</span>
                  <span v-else class="text-muted">—</span>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              class="pager"
              layout="total, prev, pager, next"
              :total="logTotal"
              :page-size="logSize"
              :current-page="logPage"
              @current-change="(p: number) => { logPage = p; loadLogs() }"
            />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-card>

    <!-- 新增/编辑渠道 -->
    <el-dialog
      v-model="channelDialogVisible"
      :title="channelForm.id ? '编辑渠道' : '新增渠道'"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="96px">
        <el-form-item label="渠道类型" required>
          <el-select v-model="channelForm.type" :disabled="!!channelForm.id" @change="onChannelTypeChange">
            <el-option label="站内信" value="INAPP" />
            <el-option label="钉钉 Webhook" value="DINGTALK" />
            <el-option label="163 邮件" value="EMAIL_163" />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道名称" required>
          <el-input v-model="channelForm.name" placeholder="如：钉钉-交易群" maxlength="50" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="channelForm.enabled" />
        </el-form-item>

        <template v-if="channelForm.type === 'INAPP'">
          <el-alert type="info" :closable="false" title="站内信无需额外配置，将站内推送给项目成员" />
        </template>

        <template v-else-if="channelForm.type === 'DINGTALK'">
          <el-form-item label="Webhook" required>
            <el-input v-model="chWebhook" placeholder="https://oapi.dingtalk.com/robot/send?access_token=xxx" />
          </el-form-item>
          <el-form-item label="加签密钥">
            <el-input v-model="chSecret" placeholder="SECxxx（可不填，不加签）" />
          </el-form-item>
          <el-form-item label="提及手机号">
            <el-input v-model="chAtMobiles" placeholder="多个用逗号分隔，如 13800138000,13900139000" />
          </el-form-item>
        </template>

        <template v-else-if="channelForm.type === 'EMAIL_163'">
          <el-form-item label="SMTP 主机">
            <el-input v-model="chHost" />
          </el-form-item>
          <el-form-item label="端口">
            <el-input-number v-model="chPort" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="邮箱账号" required>
            <el-input v-model="chUsername" placeholder="xxx@163.com" />
          </el-form-item>
          <el-form-item label="授权码" required>
            <el-input v-model="chAuthCode" type="password" show-password placeholder="邮箱设置中开启 SMTP 后获取的授权码" />
          </el-form-item>
          <el-form-item label="发件人">
            <el-input v-model="chFrom" placeholder="默认同邮箱账号" />
          </el-form-item>
          <el-form-item label="SSL">
            <el-switch v-model="chSsl" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="channelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="channelSaving" @click="saveChannel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增规则 -->
    <el-dialog
      v-model="ruleDialogVisible"
      title="新增规则"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-width="96px">
        <el-form-item label="触发事件" required>
          <el-select v-model="ruleForm.event">
            <el-option label="批次执行完成" value="BATCH_DONE" />
            <el-option label="单接口执行完成" value="EXEC_DONE" />
            <el-option label="单接口执行失败" value="EXEC_FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知渠道" required>
          <el-select
            v-model="ruleForm.channelIds"
            multiple
            collapse-tags
            placeholder="选择一个或多个渠道"
            style="width: 100%"
          >
            <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件">
          <el-checkbox v-model="ruleForm.onlyFail">仅当执行失败时通知</el-checkbox>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="saveRule">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  createChannel,
  createRule,
  deleteChannel,
  deleteRule,
  getChannels,
  getLogs,
  getRules,
  updateChannel
} from '@/api/notify'
import type { NotifyChannel, NotifyChannelType, NotifyEvent, NotifyRule } from '@/api/types'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'

const projectStore = useProjectStore()
const userStore = useUserStore()

const canManage = computed(() => userStore.isAdmin || projectStore.canManageProject)
const projectId = computed(() => projectStore.currentProject?.id ?? 0)

const activeTab = ref<'channel' | 'rule' | 'log'>('channel')

/* ---------------- 渠道 ---------------- */
const channels = ref<NotifyChannel[]>([])
const channelLoading = ref(false)
const channelDialogVisible = ref(false)
const channelSaving = ref(false)
const channelForm = reactive<{
  id: number | null
  type: NotifyChannelType
  name: string
  enabled: boolean
}>({
  id: null,
  type: 'DINGTALK',
  name: '',
  enabled: true
})

// 渠道配置的可编辑字段（避免直接操作 unknown 类型的 config 对象）
const chWebhook = ref('')
const chSecret = ref('')
const chAtMobiles = ref('')
const chHost = ref('smtp.163.com')
const chPort = ref(465)
const chUsername = ref('')
const chAuthCode = ref('')
const chFrom = ref('')
const chSsl = ref(true)

function resetChannelConfigFields() {
  chWebhook.value = ''
  chSecret.value = ''
  chAtMobiles.value = ''
  chHost.value = 'smtp.163.com'
  chPort.value = 465
  chUsername.value = ''
  chAuthCode.value = ''
  chFrom.value = ''
  chSsl.value = true
}

function buildChannelConfig(): Record<string, unknown> {
  if (channelForm.type === 'DINGTALK') {
    return {
      platform: 'DINGTALK',
      webhook: chWebhook.value.trim(),
      secret: chSecret.value.trim(),
      atMobiles: chAtMobiles.value
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
      msgtype: 'markdown'
    }
  }
  if (channelForm.type === 'EMAIL_163') {
    return {
      platform: 'EMAIL_163',
      host: chHost.value,
      port: chPort.value,
      username: chUsername.value.trim(),
      authCode: chAuthCode.value,
      from: chFrom.value.trim() || chUsername.value.trim(),
      ssl: chSsl.value
    }
  }
  return {}
}

async function loadChannels() {
  if (!projectId.value) return
  channelLoading.value = true
  try {
    channels.value = await getChannels(projectId.value)
  } catch {
    channels.value = [] // 后端未实现时兜底
  } finally {
    channelLoading.value = false
  }
}

function openChannelCreate() {
  channelForm.id = null
  channelForm.type = 'DINGTALK'
  channelForm.name = ''
  channelForm.enabled = true
  resetChannelConfigFields()
  channelDialogVisible.value = true
}

function openChannelEdit(row: NotifyChannel) {
  channelForm.id = row.id
  channelForm.type = row.type
  channelForm.name = row.name
  channelForm.enabled = row.enabled
  resetChannelConfigFields()
  const c = row.config || {}
  if (row.type === 'DINGTALK') {
    chWebhook.value = String(c.webhook || '')
    chSecret.value = String(c.secret || '')
    chAtMobiles.value = Array.isArray(c.atMobiles) ? (c.atMobiles as string[]).join(', ') : ''
  } else if (row.type === 'EMAIL_163') {
    chHost.value = String(c.host || 'smtp.163.com')
    chPort.value = Number(c.port || 465)
    chUsername.value = String(c.username || '')
    chAuthCode.value = String(c.authCode || '')
    chFrom.value = String(c.from || '')
    chSsl.value = c.ssl !== false
  }
  channelDialogVisible.value = true
}

function onChannelTypeChange() {
  // 切换类型时重置配置字段（仅在新增时允许切换）
  resetChannelConfigFields()
}

async function saveChannel() {
  if (!channelForm.name.trim()) {
    ElMessage.warning('请输入渠道名称')
    return
  }
  if (channelForm.type === 'DINGTALK' && !chWebhook.value.trim()) {
    ElMessage.warning('请输入钉钉 Webhook 地址')
    return
  }
  if (
    channelForm.type === 'EMAIL_163' &&
    (!chUsername.value.trim() || !chAuthCode.value)
  ) {
    ElMessage.warning('请输入 163 邮箱与授权码')
    return
  }
  channelSaving.value = true
  try {
    const config = buildChannelConfig()
    if (channelForm.id) {
      await updateChannel(channelForm.id, {
        name: channelForm.name,
        enabled: channelForm.enabled,
        config
      })
    } else {
      await createChannel({
        projectId: projectId.value,
        type: channelForm.type,
        name: channelForm.name,
        enabled: channelForm.enabled,
        config
      })
    }
    ElMessage.success('已保存')
    channelDialogVisible.value = false
    await loadChannels()
  } catch {
    // 后端未实现时静默兜底
  } finally {
    channelSaving.value = false
  }
}

async function toggleChannel(row: NotifyChannel, val: boolean) {
  try {
    await updateChannel(row.id, { enabled: val })
    row.enabled = val
  } catch {
    // 后端未实现时静默兜底
  }
}

async function removeChannel(row: NotifyChannel) {
  try {
    await ElMessageBox.confirm(`确定删除渠道「${row.name}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteChannel(row.id)
    ElMessage.success('已删除')
    await loadChannels()
  } catch {
    // 后端未实现时静默兜底
  }
}

/* ---------------- 规则 ---------------- */
const rules = ref<NotifyRule[]>([])
const ruleLoading = ref(false)
const ruleDialogVisible = ref(false)
const ruleSaving = ref(false)
const ruleForm = reactive({
  event: 'BATCH_DONE' as NotifyEvent,
  channelIds: [] as number[],
  onlyFail: false,
  enabled: true
})

async function loadRules() {
  if (!projectId.value) return
  ruleLoading.value = true
  try {
    rules.value = await getRules(projectId.value)
  } catch {
    rules.value = []
  } finally {
    ruleLoading.value = false
  }
}

function openRuleCreate() {
  ruleForm.event = 'BATCH_DONE'
  ruleForm.channelIds = []
  ruleForm.onlyFail = false
  ruleForm.enabled = true
  ruleDialogVisible.value = true
}

async function saveRule() {
  if (ruleForm.channelIds.length === 0) {
    ElMessage.warning('请至少选择一个通知渠道')
    return
  }
  ruleSaving.value = true
  try {
    await createRule({
      projectId: projectId.value,
      event: ruleForm.event,
      channelIds: ruleForm.channelIds,
      condition: ruleForm.onlyFail ? { onlyFail: true } : {},
      enabled: ruleForm.enabled
    })
    ElMessage.success('已保存')
    ruleDialogVisible.value = false
    await loadRules()
  } catch {
    // 后端未实现时静默兜底
  } finally {
    ruleSaving.value = false
  }
}

async function removeRule(row: NotifyRule) {
  try {
    await ElMessageBox.confirm('确定删除该通知规则吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteRule(row.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch {
    // 后端未实现时静默兜底
  }
}

/* ---------------- 发送日志 ---------------- */
const logs = ref<
  Array<{
    id: number
    event: NotifyEvent
    channelType: NotifyChannelType
    target: string
    status: 'SUCCESS' | 'FAILED'
    content: string
    error: string | null
    createTime: string
  }>
>([])
const logLoading = ref(false)
const logPage = ref(1)
const logSize = ref(20)
const logTotal = ref(0)

async function loadLogs() {
  if (!projectId.value) return
  logLoading.value = true
  try {
    const data = await getLogs(projectId.value, logPage.value, logSize.value)
    logs.value = data.records
    logTotal.value = data.total
  } catch {
    logs.value = []
    logTotal.value = 0
  } finally {
    logLoading.value = false
  }
}

function onTabChange(name: string | number) {
  if (name === 'rule') void loadRules()
  if (name === 'log') void loadLogs()
}

/* ---------------- 展示辅助 ---------------- */
const channelTypeMap: Record<NotifyChannelType, { label: string; tag: '' | 'success' | 'warning' | 'info' | 'danger' }> = {
  INAPP: { label: '站内信', tag: 'info' },
  DINGTALK: { label: '钉钉', tag: 'success' },
  EMAIL_163: { label: '163 邮件', tag: 'warning' }
}
function channelTypeLabel(t: NotifyChannelType) {
  return channelTypeMap[t]?.label || t
}
function channelTypeTag(t: NotifyChannelType) {
  return channelTypeMap[t]?.tag || 'info'
}
const eventMap: Record<NotifyEvent, string> = {
  BATCH_DONE: '批次完成',
  EXEC_DONE: '单接口完成',
  EXEC_FAIL: '单接口失败'
}
function eventLabel(e: NotifyEvent) {
  return eventMap[e] || e
}
function channelName(id: number) {
  return channels.value.find((c) => c.id === id)?.name || `#${id}`
}

/* ---------------- 生命周期 ---------------- */
onMounted(() => {
  if (canManage.value && projectId.value) void loadChannels()
})

watch(
  () => projectId.value,
  (pid) => {
    if (canManage.value && pid) {
      void loadChannels()
      if (activeTab.value === 'rule') void loadRules()
      if (activeTab.value === 'log') void loadLogs()
    }
  }
)
</script>

<style scoped>
.notify-config {
  padding: 4px;
}
.page-card {
  border-radius: 12px;
}
.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.card-sub {
  font-size: 13px;
  color: #909399;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.ch-tag {
  margin-right: 6px;
}
.text-muted {
  color: #c0c4cc;
}
.err-text {
  color: #f56c6c;
}
.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
