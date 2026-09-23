<template>
  <div class="ci-config">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">CI 集成配置</span>
          <span class="card-sub">将平台测试批次接入 Jenkins / GitLab CI 等外部流水线</span>
        </div>
      </template>

      <el-alert
        v-if="!canManage"
        type="warning"
        :closable="false"
        show-icon
        title="当前角色无权访问 CI 集成配置（需项目 OWNER / MAINTAINER 或全局管理员）"
      />

      <template v-else>
        <div v-loading="loading">
          <!-- 1) CI 令牌 -->
          <el-card shadow="never" class="sub-card">
            <template #header><span class="sub-title">CI 令牌</span></template>

            <el-alert
              v-if="notConfigured"
              type="info"
              :closable="false"
              show-icon
              title="尚未启用 CI 集成"
              description="启用后平台会生成一个专属令牌，外部 CI 须携带该令牌调用触发接口。令牌仅展示一次，请妥善保存。"
            />

            <template v-else>
              <!-- 一次性明文令牌（新建 / 重新生成后展示） -->
              <el-alert
                v-if="revealedToken"
                type="success"
                :closable="false"
                show-icon
                class="token-alert"
              >
                <template #title>已生成新令牌（仅此一次展示，请立即复制保存）</template>
                <div class="token-box">
                  <code class="token-text">{{ revealedToken }}</code>
                  <el-button :icon="DocumentCopy" size="small" @click="copyRevealed">
                    复制
                  </el-button>
                </div>
              </el-alert>

              <!-- 已有配置：仅展示尾号，可重新生成 -->
              <div v-else class="token-row">
                <div class="token-meta">
                  <span class="token-label">当前令牌尾号</span>
                  <code class="token-hint">…{{ config?.tokenHint || '—' }}</code>
                  <el-tag :type="config?.enabled === 0 ? 'info' : 'success'" size="small">
                    {{ config?.enabled === 0 ? '已停用' : '启用中' }}
                  </el-tag>
                </div>
                <el-button
                  :icon="Refresh"
                  :loading="regenerating"
                  @click="regenerate"
                >
                  重新生成令牌
                </el-button>
              </div>
              <p class="tip-text">
                重新生成后旧令牌立即失效，需在外部 CI 中同步更新凭据（Jenkins 凭据 <code>atp-ci-token</code>）。
              </p>
            </template>
          </el-card>

          <!-- 2) 基础设置 -->
          <el-card shadow="never" class="sub-card">
            <template #header><span class="sub-title">基础设置</span></template>

            <el-form label-width="110px" class="setting-form">
              <el-form-item label="默认执行环境">
                <el-select
                  v-model="form.defaultEnvId"
                  placeholder="可选择默认环境"
                  clearable
                  filterable
                  style="width: 320px"
                >
                  <el-option
                    v-for="e in envOptions"
                    :key="e.id"
                    :label="e.name"
                    :value="e.id"
                  />
                </el-select>
                <span class="form-hint">外部 CI 未显式传 envId 时使用</span>
              </el-form-item>

              <el-form-item label="默认批次">
                <el-select
                  v-model="form.defaultBatchId"
                  placeholder="可选择默认批次"
                  clearable
                  filterable
                  style="width: 320px"
                >
                  <el-option
                    v-for="b in batchOptions"
                    :key="b.id"
                    :label="b.name"
                    :value="b.id"
                  />
                </el-select>
                <span class="form-hint">外部 CI 未显式传 batchId 时使用</span>
              </el-form-item>

              <el-form-item label="完成回调地址">
                <el-input
                  v-model="form.callbackUrl"
                  placeholder="https://your-ci.example.com/webhook（可选，批次结束后回调）"
                  clearable
                  style="max-width: 520px"
                />
              </el-form-item>

              <el-form-item label="启用">
                <el-switch v-model="form.enabled" />
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  :icon="notConfigured ? Plus : undefined"
                  :loading="saving"
                  @click="save"
                >
                  {{ notConfigured ? '保存并生成 CI 令牌' : '保存配置' }}
                </el-button>
                <el-button :icon="Refresh" :loading="loading" @click="reloadAll">刷新</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 3) 集成说明 & 示例 -->
          <el-card shadow="never" class="sub-card">
            <template #header><span class="sub-title">集成说明 &amp; 示例</span></template>

            <el-alert type="info" :closable="false" class="info-alert">
              <div class="endpoint-line">
                <span class="endpoint-label">触发地址</span>
                <code class="endpoint-url">{{ triggerUrl }}</code>
                <el-button link type="primary" :icon="DocumentCopy" @click="copyText(triggerUrl)">复制</el-button>
              </div>
              <div class="endpoint-line">
                <span class="endpoint-label">结果轮询</span>
                <code class="endpoint-url">{{ resultUrl }}</code>
                <el-button link type="primary" :icon="DocumentCopy" @click="copyText(resultUrl)">复制</el-button>
              </div>
              <p class="tip-text">
                外部 CI 需在请求头携带 <code>X-CI-Token: &lt;你的令牌&gt;</code>。建议将令牌存为 CI 凭据
                （Jenkins Secret Text，ID 固定为 <code>atp-ci-token</code>），不要在脚本中明文写死。
              </p>
            </el-alert>

            <el-form-item label="平台 API 地址" label-width="110px" class="url-input-row">
              <el-input v-model="baseUrl" placeholder="如 http://192.168.1.10:8080/api" style="max-width: 520px" />
              <span class="form-hint">用于生成下方示例，请填写外部 CI 可访问的地址（默认取当前页面源）</span>
            </el-form-item>

            <el-tabs v-model="exampleTab" class="example-tabs">
              <el-tab-pane label="Jenkinsfile" name="jenkins">
                <div class="code-toolbar">
                  <span class="code-title">适用于 Jenkins Pipeline（需安装 HTTP Request 插件）</span>
                  <el-button :icon="DocumentCopy" size="small" @click="copyText(jenkinsfileText, 'Jenkinsfile 已复制')">
                    复制 Jenkinsfile
                  </el-button>
                </div>
                <pre class="code-block"><code>{{ jenkinsfileText }}</code></pre>
              </el-tab-pane>

              <el-tab-pane label="curl 命令" name="curl">
                <div class="code-toolbar">
                  <span class="code-title">适用于 GitLab CI / 任意 Shell（将令牌写入环境变量 ATP_CI_TOKEN）</span>
                  <el-button :icon="DocumentCopy" size="small" @click="copyText(curlText, 'curl 命令已复制')">
                    复制命令
                  </el-button>
                </div>
                <pre class="code-block"><code>{{ curlText }}</code></pre>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy, Plus, Refresh } from '@element-plus/icons-vue'
import { getCiConfig, regenerateCiToken, upsertCiConfig } from '@/api/ci'
import { getEnvList } from '@/api/env'
import { getBatchList } from '@/api/planBatch'
import type { CiConfig, CiConfigSaveParams, EnvInfo } from '@/api/types'
import type { PlanBatchInfo } from '@/api/planBatch'
import { useProjectStore } from '@/stores/project'
import { useUserStore } from '@/stores/user'

const projectStore = useProjectStore()
const userStore = useUserStore()

const canManage = computed(() => userStore.isAdmin || projectStore.canManageProject)
const projectId = computed(() => projectStore.currentProject?.id ?? 0)

/* ---------------- 状态 ---------------- */
const loading = ref(false)
const saving = ref(false)
const regenerating = ref(false)

const config = ref<CiConfig | null>(null)
const notConfigured = ref(false)
/** 一次性明文令牌（新建/重新生成后展示，刷新页面后丢失） */
const revealedToken = ref<string | null>(null)

const envOptions = ref<EnvInfo[]>([])
const batchOptions = ref<PlanBatchInfo[]>([])

const form = reactive<{
  defaultEnvId: number | null
  defaultBatchId: number | null
  callbackUrl: string
  enabled: boolean
}>({
  defaultEnvId: null,
  defaultBatchId: null,
  callbackUrl: '',
  enabled: true
})

/* ---------------- 示例模板 ---------------- */
const baseUrl = ref(
  typeof window !== 'undefined' ? `${window.location.origin}/api` : 'http://localhost:8080/api'
)
const exampleTab = ref<'jenkins' | 'curl'>('jenkins')

const JENKINSFILE_TEMPLATE = `pipeline {
    agent any
    parameters {
        string(name: 'ATP_BASE_URL', defaultValue: '{{BASE_URL}}', description: 'ATP 平台 API 基址，如 http://192.168.1.10:8080/api')
        string(name: 'PROJECT_ID', defaultValue: '{{PROJECT_ID}}', description: 'ATP 项目 ID')
        string(name: 'BATCH_ID', defaultValue: '', description: '批次 ID（留空则使用平台默认批次）')
        string(name: 'ENV_ID', defaultValue: '', description: '环境 ID（留空则使用平台默认环境）')
    }
    stages {
        stage('Trigger ATP Test') {
            steps {
                withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
                    script {
                        def body = [ projectId: params.PROJECT_ID, batchId: params.BATCH_ID ?: null, envId: params.ENV_ID ?: null ]
                        def resp = httpRequest(
                            url: "\${params.ATP_BASE_URL}/ci/trigger",
                            httpMode: 'POST',
                            contentType: 'APPLICATION_JSON',
                            customHeaders: [[name: 'X-CI-Token', value: env.ATP_TOKEN]],
                            body: groovy.json.JsonOutput.toJson(body)
                        )
                        def json = new groovy.json.JsonSlurper().parseText(resp.content)
                        env.RUN_ID = json.data.runId.toString()
                        echo "已触发运行 runId=\${env.RUN_ID}，轮询地址：\${params.ATP_BASE_URL}\${json.data.statusUrl}"
                    }
                }
            }
        }
        stage('Wait & Report') {
            steps {
                withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
                    script {
                        def status = 'RUNNING'
                        def maxRetry = 180
                        for (int i = 0; i < maxRetry && status == 'RUNNING'; i++) {
                            sleep(time: 10, unit: 'SECONDS')
                            def resp = httpRequest(
                                url: "\${params.ATP_BASE_URL}/ci/result/\${env.RUN_ID}",
                                httpMode: 'GET',
                                customHeaders: [[name: 'X-CI-Token', value: env.ATP_TOKEN]]
                            )
                            def json = new groovy.json.JsonSlurper().parseText(resp.content)
                            status = json.data.status
                            echo "轮询 #\${i + 1}: status=\${status} passed=\${json.data.passed} failed=\${json.data.failed}"
                        }
                        if (status == 'SUCCESS') {
                            currentBuild.result = 'SUCCESS'
                        } else if (status in ['FAILED', 'PARTIAL_FAILED']) {
                            currentBuild.result = 'FAILURE'
                        } else {
                            error("CI 执行超时未完成，请到平台查看 runId=\${env.RUN_ID}")
                        }
                    }
                }
            }
        }
    }
}`

const CURL_TEMPLATE = `# 1) 触发批次执行（返回 runId）
curl -X POST '{{BASE_URL}}/ci/trigger' \\
  -H 'Content-Type: application/json' \\
  -H "X-CI-Token: $ATP_CI_TOKEN" \\
  -d '{"projectId": {{PROJECT_ID}}, "batchId": <批次ID>, "envId": <环境ID>}'

# 2) 轮询执行结果（status=SUCCESS 表示全部通过；PARTIAL_FAILED / FAILED 表示存在失败）
curl -X GET '{{BASE_URL}}/ci/result/<runId>' \\
  -H "X-CI-Token: $ATP_CI_TOKEN"`

function fillTemplate(tpl: string): string {
  const base = baseUrl.value.replace(/\/+$/, '')
  return tpl
    .split('{{BASE_URL}}')
    .join(base)
    .split('{{PROJECT_ID}}')
    .join(String(projectId.value))
}

const jenkinsfileText = computed(() => fillTemplate(JENKINSFILE_TEMPLATE))
const curlText = computed(() => fillTemplate(CURL_TEMPLATE))
const triggerUrl = computed(() => `${baseUrl.value.replace(/\/+$/, '')}/ci/trigger`)
const resultUrl = computed(() => `${baseUrl.value.replace(/\/+$/, '')}/ci/result/{runId}`)

/* ---------------- 加载 ---------------- */
function syncForm(c: CiConfig) {
  form.defaultEnvId = c.defaultEnvId ?? null
  form.defaultBatchId = c.defaultBatchId ?? null
  form.callbackUrl = c.callbackUrl ?? ''
  form.enabled = c.enabled !== 0
}

async function loadConfig() {
  if (!projectId.value) return
  loading.value = true
  try {
    const cfg = await getCiConfig(projectId.value)
    config.value = cfg
    if (cfg) {
      notConfigured.value = false
      syncForm(cfg)
    } else {
      notConfigured.value = true
    }
  } catch {
    // 任何异常均视作尚未配置（保持首屏干净）
    config.value = null
    notConfigured.value = true
  } finally {
    loading.value = false
  }
}

async function loadEnvs() {
  if (!projectId.value) return
  try {
    const data = await getEnvList({ projectId: projectId.value, page: 1, size: 200 })
    envOptions.value = data.records
  } catch {
    envOptions.value = []
  }
}

async function loadBatches() {
  if (!projectId.value) return
  try {
    const data = await getBatchList({ projectId: projectId.value, page: 1, size: 200 })
    batchOptions.value = data.records
  } catch {
    batchOptions.value = []
  }
}

async function reloadAll() {
  await Promise.all([loadConfig(), loadEnvs(), loadBatches()])
}

/* ---------------- 操作 ---------------- */
async function save() {
  if (!projectId.value) return
  const isCreate = !config.value
  saving.value = true
  try {
    const payload: CiConfigSaveParams = {
      projectId: projectId.value,
      defaultEnvId: form.defaultEnvId,
      defaultBatchId: form.defaultBatchId,
      callbackUrl: form.callbackUrl.trim() || null,
      enabled: form.enabled
    }
    if (config.value?.id) payload.id = config.value.id
    const cfg = await upsertCiConfig(payload)
    config.value = cfg
    revealedToken.value = cfg.token // 新建时后端返回一次性明文令牌
    notConfigured.value = false
    ElMessage.success(isCreate ? '已启用 CI 集成，令牌已生成' : '配置已保存')
  } catch {
    // 错误提示已由响应拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function regenerate() {
  if (!projectId.value) return
  regenerating.value = true
  try {
    const cfg = await regenerateCiToken(projectId.value)
    config.value = cfg
    revealedToken.value = cfg.token // 一次性明文令牌
    ElMessage.success('已重新生成令牌，旧令牌立即失效')
  } catch {
    // 错误提示已由响应拦截器统一处理
  } finally {
    regenerating.value = false
  }
}

async function copyText(text: string, tip = '已复制到剪贴板') {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(tip)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    try {
      document.execCommand('copy')
      ElMessage.success(tip)
    } catch {
      ElMessage.error('复制失败，请手动复制')
    } finally {
      document.body.removeChild(ta)
    }
  }
}

function copyRevealed() {
  if (revealedToken.value) void copyText(revealedToken.value, '令牌已复制')
}

/* ---------------- 生命周期 ---------------- */
onMounted(() => {
  if (canManage.value && projectId.value) void reloadAll()
})

watch(
  () => projectId.value,
  (pid) => {
    if (canManage.value && pid) {
      revealedToken.value = null
      void reloadAll()
    }
  }
)
</script>

<style scoped>
.ci-config {
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
.sub-card {
  border-radius: 10px;
  margin-bottom: 16px;
}
.sub-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}
.token-alert {
  margin-bottom: 4px;
}
.token-box {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}
.token-text {
  background: #0f2444;
  color: #7ee787;
  padding: 8px 12px;
  border-radius: 6px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  word-break: break-all;
  flex: 1;
}
.token-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.token-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.token-label {
  font-size: 13px;
  color: #606266;
}
.token-hint {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 14px;
  color: #1f2937;
  background: #f4f5f7;
  padding: 4px 10px;
  border-radius: 6px;
}
.tip-text {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin: 8px 0 0;
}
.tip-text code,
.endpoint-url code {
  background: #f4f5f7;
  padding: 1px 5px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.setting-form {
  margin-top: 4px;
}
.form-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: 10px;
}
.info-alert {
  margin-bottom: 12px;
}
.endpoint-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.endpoint-label {
  font-size: 13px;
  color: #606266;
  width: 64px;
  flex: none;
}
.endpoint-url {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  color: #1d63d1;
  background: #f4f5f7;
  padding: 4px 10px;
  border-radius: 6px;
  word-break: break-all;
  flex: 1;
}
.url-input-row {
  margin: 4px 0 12px;
}
.example-tabs {
  margin-top: 4px;
}
.code-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.code-title {
  font-size: 13px;
  color: #606266;
}
.code-block {
  background: #0f2444;
  color: #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre;
  margin: 0;
}
</style>
