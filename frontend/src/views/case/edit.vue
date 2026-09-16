<template>
  <div class="case-edit-page">
    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <!-- 第一行：用例名称 + 工程/版本/模块 -->
      <div class="top-bar-row">
        <div class="top-bar-fields">
          <el-form-item label="工程" prop="applicationId" class="bar-form-item" required>
            <el-select
              v-model="form.applicationId"
              placeholder="请选择工程"
              filterable
              class="bar-select"
              @change="onApplicationChange"
            >
              <el-option v-for="item in appOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="版本" prop="versionId" class="bar-form-item" required>
            <el-select
              v-model="form.versionId"
              placeholder="请选择版本"
              filterable
              :disabled="!form.applicationId"
              class="bar-select"
              @change="onVersionChange"
            >
              <el-option v-for="item in versionOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="模块" prop="moduleId" class="bar-form-item" required>
            <el-select
              v-model="form.moduleId"
              placeholder="请选择模块"
              filterable
              :disabled="!form.versionId"
              class="bar-select"
              @change="onModuleChange"
            >
              <el-option v-for="item in moduleOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="用例名称" prop="caseName" class="bar-form-item" required>
            <el-input
              v-model="form.caseName"
              placeholder="请输入用例名称"
              clearable
              class="bar-input-name"
            />
          </el-form-item>
        </div>
      </div>
      <!-- 第二行：优先级 + 状态 + 操作按钮 -->
      <div class="top-bar-row">
        <div class="top-bar-fields">
          <el-form-item label="优先级" prop="level" class="bar-form-item" required>
            <el-select v-model="form.level" class="bar-select-sm">
              <el-option :value="1" label="P0（核心）" />
              <el-option :value="2" label="P1（重要）" />
              <el-option :value="3" label="P2（一般）" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" class="bar-form-item">
            <el-switch v-model="form.statusBool" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
        <div class="top-bar-right">
          <el-button @click="goBack">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="edit-body">
      <el-form
        ref="formEl"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="left"
        class="edit-form"
      >
        <!-- 左侧：步骤导航树（前置 / 用例 / 后置） -->
        <div class="left-panel">
          <el-card shadow="never" class="section-card steps-section">
            <template #header>
              <div class="steps-card-header">
                <div class="steps-header-actions">
                  <el-button  type="primary" size="small" :icon="Plus" @click="openApiDialog">添加步骤</el-button>
                </div>
                <div class="debug-bar">
                  <el-select v-model="debugEnvId" placeholder="选择环境" size="small" class="debug-env-select" clearable>
                    <el-option v-for="env in envOptions" :key="env.id" :label="env.name" :value="env.id" />
                  </el-select>
                  <el-button
                    type="warning"
                    size="small"
                    :loading="debugging"
                    :disabled="!debugEnvId"
                    @click="handleDebug"
                  >
                    调试运行
                  </el-button>
                </div>
              </div>
            </template>

            <el-tree
              v-if="treeData.length"
              :data="treeData"
              node-key="key"
              :default-expanded-keys="['g-main']"
              :expand-on-click-node="false"
              class="step-tree"
              @node-click="handleNodeClick"
            >
              <template #default="{ data }">
                <!-- 分组节点 -->
                <div v-if="data.type === 'group'" class="tree-group" :class="{ 'tree-group-active': isGroupActive(data) }">
                  <span class="tree-group-label">{{ data.label }}</span>
                  <el-badge v-if="data.count !== undefined" :value="data.count" :max="99" class="tree-group-badge" type="primary" />
                </div>
                <!-- 用例步骤子节点 -->
                <div v-else class="tree-step" :class="{ 'tree-step-active': data.uid === currentMainUid }">
                  <span class="tree-step-order">{{ data.order }}</span>
                  <template v-if="stepByUid(data.uid)?.stepType === 2">
                    <el-tag size="small" type="warning" class="method-tag">组件</el-tag>
                    <span class="tree-step-path">{{ componentNameOf(stepByUid(data.uid)!) || '未选择组件' }}</span>
                  </template>
                  <template v-else-if="stepByUid(data.uid) && getStepApiInfo(stepByUid(data.uid)!)">
                    <el-tag size="small" :type="methodTagType(getStepApiInfo(stepByUid(data.uid)!)!.method)" class="method-tag">
                      {{ getStepApiInfo(stepByUid(data.uid)!)!.method }}
                    </el-tag>
                    <span class="tree-step-path">{{ getStepApiInfo(stepByUid(data.uid)!)!.path }}</span>
                  </template>
                  <span v-else class="tree-step-missing">未选择接口</span>
                  <div class="tree-step-actions" @click.stop>
                    <el-tooltip content="上移" placement="top">
                      <el-button :icon="Top" link size="small" :disabled="isFirstInPhase(data.uid)" @click="moveWithinPhase(data.uid, -1)" />
                    </el-tooltip>
                    <el-tooltip content="下移" placement="top">
                      <el-button :icon="Bottom" link size="small" :disabled="isLastInPhase(data.uid)" @click="moveWithinPhase(data.uid, 1)" />
                    </el-tooltip>
                    <el-tooltip content="删除步骤" placement="top">
                      <el-button :icon="Delete" link type="danger" size="small" @click="removeStepByUid(data.uid)" />
                    </el-tooltip>
                  </div>
                </div>
              </template>
            </el-tree>
            <el-empty v-else description="暂无步骤" :image-size="80" />
          </el-card>
        </div>

        <!-- 右侧：前置/后置扩展表格 或 用例步骤详情 -->
        <div class="right-panel">
          <!-- 前置扩展 -->
          <el-card v-if="viewMode === 'pre'" shadow="never" class="section-card extension-section">
            <template #header>
              <div class="steps-card-header">
                <span class="section-title">前置扩展</span>
                <el-button type="primary" size="small" :icon="Plus" @click="openExtensionDialog('pre')">新增前置扩展</el-button>
              </div>
            </template>
            <extension-table
              :steps="preSteps"
              :component-map="componentMap"
              :generator-map="generatorMap"
              @add="openExtensionDialog('pre')"
              @edit="(s) => openExtensionDialog('pre', s)"
              @delete="removeStepByUid"
            />
          </el-card>

          <!-- 后置扩展 -->
          <el-card v-else-if="viewMode === 'post'" shadow="never" class="section-card extension-section">
            <template #header>
              <div class="steps-card-header">
                <span class="section-title">后置扩展</span>
                <el-button type="primary" size="small" :icon="Plus" @click="openExtensionDialog('post')">新增后置扩展</el-button>
              </div>
            </template>
            <extension-table
              :steps="postSteps"
              :component-map="componentMap"
              :generator-map="generatorMap"
              @add="openExtensionDialog('post')"
              @edit="(s) => openExtensionDialog('post', s)"
              @delete="removeStepByUid"
            />
          </el-card>

          <!-- 用例步骤详情（保持原有样式） -->
          <template v-else>
            <el-card v-if="activeStep" shadow="never" class="section-card detail-section">
              <template #header>
                <div class="detail-card-header">
                  <template v-if="activeStep.stepType === 2">
                    <span class="section-title">当前组件:</span>
                    <el-tag size="small" type="warning" class="method-tag">组件</el-tag>
                    <span class="detail-api-path">{{ componentNameOf(activeStep) || '未选择组件' }}</span>
                    <el-button type="primary" link size="small" @click="openApiDialogForStep(currentMainUid!)">更换组件</el-button>
                  </template>
                  <template v-else>
                    <span class="section-title">当前接口:</span>
                    <template v-if="getStepApiInfo(activeStep)">
                      <el-tag size="small" :type="methodTagType(getStepApiInfo(activeStep)!.method)" class="method-tag">
                        {{ getStepApiInfo(activeStep)!.method }}
                      </el-tag>
                      <span class="detail-api-path">{{ getStepApiInfo(activeStep)!.path }}</span>
                    </template>
                    <el-button type="primary" link size="small" @click="openApiDialogForStep(currentMainUid!)">更换接口</el-button>
                    <el-button type="primary" link size="small" @click="toggleAssertions">断言</el-button>
                    <el-button type="primary" link size="small" @click="openDatasetDialog">数据源选择</el-button>
                  </template>
                </div>
              </template>

              <!-- 组合组件步骤：请求/断言沿用组件自身配置，此处仅作说明 -->
              <el-alert
                v-if="activeStep.stepType === 2"
                type="info"
                :closable="false"
                title="组合组件步骤的请求头 / 请求参数 / 断言沿用组件自身配置，变量与组件内部步骤共享同一变量池。"
                class="component-step-tip"
              />

              <!-- 响应变量名 -->
              <div v-if="activeStep.stepType !== 2" class="step-form-item">
                <div class="field-label">响应变量名</div>
                <el-input
                  v-model="activeStep.responseVar"
                  placeholder="给该接口响应数据命名，供后续步骤引用（如 loginResp），留空则不保存"
                  clearable
                />
              </div>

              <!-- 请求覆盖（拆分为请求头 / 请求参数） -->
              <div v-if="activeStep.stepType !== 2" class="request-override-section">
                <el-tabs v-model="requestTab" class="request-tabs">
                  <el-tab-pane label="请求头" name="headers">
                    <el-input
                      v-model="activeStep.requestHeaders"
                      type="textarea"
                      @blur="formatJsonField('requestHeaders')"
                      :rows="4"
                      placeholder='JSON格式，覆盖接口默认请求头&#10;可用${varName}引用上一步提取的变量&#10;如：{"Authorization":"Bearer ${token}","Content-Type":"application/json"}'
                      class="code-textarea"
                    />
                  </el-tab-pane>
                  <el-tab-pane label="请求参数" name="params">
                    <el-input
                      v-model="activeStep.requestParams"
                      type="textarea"
                      @blur="formatJsonField('requestParams')"
                      :rows="4"
                      placeholder='JSON格式，覆盖接口默认请求参数(body)&#10;可用${varName}引用上一步提取的变量&#10;如：{"userId":"${userId}","page":1}'
                      class="code-textarea"
                    />
                  </el-tab-pane>
                </el-tabs>
              </div>

              <!-- 断言规则 -->
              <div v-if="showAssertions && activeStep.stepType !== 2" class="step-form-item">
                <div class="field-label">断言规则</div>
                <div class="assertion-list">
                  <div v-for="(a, idx) in activeStep.assertions" :key="idx" class="assertion-row">
                    <el-select v-model="a.type" class="assertion-type">
                      <el-option label="状态码" value="status" />
                      <el-option label="JSONPath" value="jsonPath" />
                      <el-option label="响应头" value="header" />
                      <el-option label="响应体包含" value="body" />
                    </el-select>

                    <el-input
                      v-if="a.type === 'jsonPath'"
                      v-model="a.path"
                      placeholder="JSONPath，如 $.code"
                      class="assertion-path"
                    />
                    <el-input
                      v-else-if="a.type === 'header'"
                      v-model="a.path"
                      placeholder="响应头名，如 Content-Type"
                      class="assertion-path"
                    />

                    <el-select
                      v-if="a.type === 'jsonPath' || a.type === 'header'"
                      v-model="a.operator"
                      placeholder="操作符"
                      class="assertion-operator"
                    >
                      <el-option label="等于" value="eq" />
                      <el-option label="不等于" value="notEq" />
                      <el-option label="包含" value="contains" />
                      <el-option label="存在" value="exists" />
                    </el-select>

                    <el-input
                      v-if="showExpected(a)"
                      v-model="a.expected"
                      :placeholder="assertionExpectedPlaceholder(a)"
                      class="assertion-expected"
                    />

                    <el-button link type="danger" :icon="Delete" @click="removeAssertion(idx)" />
                  </div>
                  <el-button type="primary" link :icon="Plus" @click="addAssertion">添加断言</el-button>
                </div>
              </div>
            </el-card>

            <el-card v-else shadow="never" class="section-card detail-section detail-empty">
              <el-empty description="请点击左侧「用例步骤」下的接口查看详情" :image-size="100" />
            </el-card>
          </template>
        </div>
      </el-form>
    </div>

    <!-- 步骤选择弹框（新增/更换用例步骤：单接口 / 组合组件） -->
    <el-dialog
      v-model="apiDialogVisible"
      :title="apiDialogStepType === 2 ? '选择组合组件' : '选择接口'"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
    >
      <div class="api-dialog-body">
        <el-form label-width="80px">
          <el-form-item label="步骤类型">
            <el-radio-group v-model="apiDialogStepType" @change="onApiDialogTypeChange">
              <el-radio :value="1">单接口</el-radio>
              <el-radio :value="2">组合组件</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="apiDialogStepType === 1" label="选择接口">
            <el-select
              v-model="apiDialogSelectedId"
              placeholder="请选择接口（支持搜索）"
              filterable
              class="api-dialog-select"
              value-key="id"
            >
              <el-option
                v-for="api in apiOptions"
                :key="api.id"
                :label="`${api.method} ${api.path} — ${api.name}`"
                :value="api.id"
              >
                <div class="api-select-option">
                  <el-tag size="small" :type="methodTagType(api.method)" class="method-tag">
                    {{ api.method }}
                  </el-tag>
                  <span class="api-path">{{ api.path }}</span>
                  <span class="api-option-name">{{ api.name }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item v-else label="选择组件">
            <el-select
              v-model="apiDialogSelectedComponentId"
              placeholder="请选择组合组件（支持搜索）"
              filterable
              class="api-dialog-select"
              value-key="id"
            >
              <el-option v-for="c in componentOptions" :key="c.id" :label="c.name" :value="c.id">
                <div class="api-select-option">
                  <el-tag size="small" type="warning" class="method-tag">组件</el-tag>
                  <span class="api-path">{{ c.name }}</span>
                  <span v-if="c.description" class="api-option-name">{{ c.description }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <div v-if="apiDialogStepType === 1 && apiDialogPreview" class="api-dialog-preview">
            <div class="preview-row">
              <span class="preview-label">请求方法</span>
              <el-tag size="small" :type="methodTagType(apiDialogPreview.method)">{{ apiDialogPreview.method }}</el-tag>
            </div>
            <div class="preview-row">
              <span class="preview-label">接口路径</span>
              <span class="preview-value">{{ apiDialogPreview.path }}</span>
            </div>
            <div class="preview-row">
              <span class="preview-label">接口名称</span>
              <span class="preview-value">{{ apiDialogPreview.name }}</span>
            </div>
            <div v-if="apiDialogPreview.description" class="preview-row">
              <span class="preview-label">描述</span>
              <span class="preview-value text-muted">{{ apiDialogPreview.description }}</span>
            </div>
          </div>

          <div v-else-if="apiDialogStepType === 2 && apiDialogComponentPreview" class="api-dialog-preview">
            <div class="preview-row">
              <span class="preview-label">组件名称</span>
              <span class="preview-value">{{ apiDialogComponentPreview.name }}</span>
            </div>
            <div v-if="apiDialogComponentPreview.description" class="preview-row">
              <span class="preview-label">描述</span>
              <span class="preview-value text-muted">{{ apiDialogComponentPreview.description }}</span>
            </div>
          </div>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="apiDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="apiDialogStepType === 1 ? !apiDialogSelectedId : !apiDialogSelectedComponentId"
          @click="confirmApiSelect"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 前置/后置扩展弹框（选择公共接口组件） -->
    <el-dialog
      v-model="extDialogVisible"
      :title="extDialogTitle"
      width="560px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form label-width="110px">
        <el-form-item label="扩展类型" required>
          <el-select v-model="extForm.type" class="ext-dialog-select" @change="onExtTypeChange">
            <el-option :value="2" label="公共接口组件" />
            <el-option :value="3" label="生成变量（数据生成器）" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="extForm.type === 3" label="选择生成器" required>
          <el-select
            v-model="extForm.generatorId"
            placeholder="请选择数据生成器"
            filterable
            class="ext-dialog-select"
            @change="onExtGeneratorChange"
          >
            <el-option v-for="g in generatorOptions" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="extForm.type === 3" label="输出变量名" required>
          <el-input v-model="extForm.variableName" placeholder="如 phoneVar，后续步骤用 ${phoneVar} 引用" clearable />
        </el-form-item>
        <el-form-item v-if="extForm.type === 3" label="每次执行重新生成">
          <el-switch v-model="extForm.regenEachRun" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item v-if="extForm.type !== 3" label="选择组件" required>
          <el-select
            v-model="extForm.componentId"
            placeholder="请选择公共接口组件"
            filterable
            class="ext-dialog-select"
            @change="onExtComponentChange"
          >
            <el-option v-for="c in componentOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="extForm.type !== 3" label="扩展名称">
          <el-input v-model="extForm.stepName" placeholder="默认取组件名称" clearable />
        </el-form-item>
        <el-form-item v-if="extForm.type !== 3" label="返回数据变量">
          <el-input v-model="extForm.responseVar" placeholder="如 token，供后续步骤 ${token} 引用" clearable />
        </el-form-item>
        <el-form-item label="是否禁用">
          <el-switch v-model="extForm.isDisabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="提升全局变量">
          <el-switch v-model="extForm.promoteGlobal" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="失败继续执行">
          <el-switch v-model="extForm.continueOnFail" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="扩展说明">
          <el-input v-model="extForm.description" type="textarea" :rows="2" placeholder="扩展说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="extForm.type === 3 ? !extForm.generatorId : !extForm.componentId"
          @click="confirmExtension"
        >确定</el-button>
      </template>
    </el-dialog>

    <!-- 数据源选择弹窗（列表 + 数据项 / 编辑 / 删除） -->
    <DatasetSelectDialog
      v-model="datasetDialogVisible"
      :case-id="editingId || undefined"
      :case-name="form.caseName || undefined"
    />

    <!-- 调试结果抽屉 -->
    <el-drawer v-model="debugVisible" title="调试结果" size="60%">
      <div v-if="debugResult" class="debug-result">
        <div class="debug-summary">
          <el-tag :type="debugResult.status === 'SUCCESS' ? 'success' : 'danger'" size="large">
            {{ debugResult.status === 'SUCCESS' ? '执行成功' : '执行失败' }}
          </el-tag>
          <span class="debug-meta">
            共 {{ debugResult.totalRounds }} 轮 · 通过 {{ debugResult.passedRounds }} · 失败
            {{ debugResult.failedRounds }} · 耗时 {{ debugResult.durationMs }}ms
          </span>
        </div>

        <div v-for="(round, ri) in debugResult.rounds" :key="round.roundIndex ?? ri" class="debug-round">
          <div class="debug-round-header">
            <el-tag size="small" :type="round.status === 'SUCCESS' ? 'success' : 'danger'">
              第 {{ round.roundIndex }} 轮
            </el-tag>
            <span class="debug-step-time">{{ round.durationMs }}ms</span>
          </div>
          <pre v-if="round.params && Object.keys(round.params).length" class="debug-pre debug-round-params">{{ formatJson(JSON.stringify(round.params)) }}</pre>

          <div v-for="(step, idx) in round.steps" :key="step.stepId ?? idx" class="debug-step">
            <div class="debug-step-header">
              <el-tag size="small" :type="stepStatusType(step.status)">{{ step.status }}</el-tag>
              <span class="debug-step-title">{{ idx + 1 }}. {{ step.stepName || '步骤' + (idx + 1) }}</span>
              <span class="debug-step-api">{{ step.method }} {{ step.url }}</span>
              <span class="debug-step-time">{{ step.durationMs }}ms</span>
            </div>
            <div v-if="step.errorMsg" class="debug-error">{{ step.errorMsg }}</div>

            <el-collapse v-if="step.statusCode != null">
              <el-collapse-item title="请求详情" :name="'req' + ri + '-' + idx">
                <div v-if="step.requestHeaders" class="debug-kv">
                  <div class="debug-kv-label">请求头</div>
                  <pre class="debug-pre">{{ formatJson(step.requestHeaders) }}</pre>
                </div>
                <div v-if="step.requestBody" class="debug-kv">
                  <div class="debug-kv-label">请求体</div>
                  <pre class="debug-pre">{{ formatJson(step.requestBody) }}</pre>
                </div>
              </el-collapse-item>
              <el-collapse-item title="响应详情" :name="'resp' + ri + '-' + idx">
                <div v-if="step.responseHeaders" class="debug-kv">
                  <div class="debug-kv-label">响应头</div>
                  <pre class="debug-pre">{{ formatJson(step.responseHeaders) }}</pre>
                </div>
                <div v-if="step.responseBody" class="debug-kv">
                  <div class="debug-kv-label">响应体</div>
                  <pre class="debug-pre">{{ formatJson(step.responseBody) }}</pre>
                </div>
              </el-collapse-item>
              <el-collapse-item v-if="step.assertResults && step.assertResults.length" title="断言结果" :name="'assert' + ri + '-' + idx">
                <div v-for="(a, ai) in step.assertResults" :key="ai" class="debug-assert-row">
                  <el-tag size="small" :type="a.passed ? 'success' : 'danger'">{{ a.passed ? '通过' : '失败' }}</el-tag>
                  <span>{{ a.message }}</span>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bottom,
  Delete,
  Plus,
  Top
} from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createCase, getCaseDetail, updateCase } from '@/api/case'
import { getApiList, getModuleOptions, getProjectOptions, getVersionOptions } from '@/api/base'
import { getEnvList } from '@/api/env'
import { getComponentList } from '@/api/component'
import { getGeneratorList } from '@/api/generator'
import { executeCase } from '@/api/execute'
import DatasetSelectDialog from '@/views/dataset/components/DatasetSelectDialog.vue'
import ExtensionTable from '@/views/case/ExtensionTable.vue'
import type {
  ApiComponentInfo,
  ApiInfo,
  AssertionItem,
  CaseExecuteResult,
  CaseExtensionStep,
  DataGeneratorInfo,
  EnvInfo,
  OptionItem,
  StepExecuteResult,
  StepParams
} from '@/api/types'
import { useProjectStore } from '@/stores/project'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const formEl = ref<FormInstance>()
const submitting = ref(false)
const apiOptions = ref<ApiInfo[]>([])
const editingId = ref<number | null>(null)

// 级联下拉选项
const appOptions = ref<OptionItem[]>([])
const versionOptions = ref<OptionItem[]>([])
const moduleOptions = ref<OptionItem[]>([])

const isEdit = computed(() => editingId.value !== null)

/** 步骤表单内部类型（含 UI 扩展字段） */
interface StepFormItem {
  _uid: number
  apiId: number
  /** 接口名称（回显保留，避免依赖 apiOptions 查找） */
  apiName?: string
  /** 接口请求方法 */
  apiMethod?: string
  /** 接口路径 */
  apiPath?: string
  /** 步骤阶段：pre / main / post */
  phase: string
  /** 步骤类型：1 单接口 2 组合组件 3 其他类型 */
  stepType: number
  /** 组合组件ID（stepType=2） */
  componentId?: number | null
  /** 组合组件名称（UI 展示，不落库） */
  componentName?: string
  /** 生成变量步骤（stepType=3）关联的数据生成器 */
  generatorId?: number | null
  /** 生成变量步骤关联生成器名称（UI 展示） */
  generatorName?: string
  /** 生成变量步骤输出变量名 */
  variableName?: string
  /** 生成变量步骤：每次执行是否重新生成 */
  regenEachRun?: number
  /** 全局组件映射（id → 名称），用于扩展表格展示 */
  sortOrder?: number
  stepName?: string
  /** 响应变量名 */
  responseVar?: string
  requestOverride?: string
  /** 断言规则（结构化，提交时序列化为 JSON） */
  assertions: AssertionItem[]
  isDisabled: number
  promoteGlobal: number
  continueOnFail: number
  description?: string
  /** UI 拆分：请求头 */
  requestHeaders: string
  /** UI 拆分：请求参数 */
  requestParams: string
}

let uidSeq = 1

const form = reactive({
  caseName: '',
  applicationId: null as number | null,
  versionId: null as number | null,
  moduleId: null as number | null,
  level: 2,
  statusBool: true,
  setupScript: '',
  steps: [] as StepFormItem[]
})

/** 数据源选择弹窗开关 */
const datasetDialogVisible = ref(false)
function openDatasetDialog() {
  datasetDialogVisible.value = true
}

/** 调试相关状态 */
const envOptions = ref<EnvInfo[]>([])
const debugEnvId = ref<number | null>(null)
const debugging = ref(false)
const debugVisible = ref(false)
const debugResult = ref<CaseExecuteResult | null>(null)

const rules: FormRules = {
  caseName: [{ required: true, message: '请输入用例名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择优先级', trigger: 'change' }],
  applicationId: [{ required: true, message: '请选择工程', trigger: 'change' }],
  versionId: [{ required: true, message: '请选择版本', trigger: 'change' }],
  moduleId: [{ required: true, message: '请选择模块', trigger: 'change' }]
}

/* ---- 步骤导航树 ---- */
const currentGroup = ref<'pre' | 'main' | 'post'>('main')
const currentMainUid = ref<number | null>(null)
const viewMode = computed<'pre' | 'post' | 'detail'>(() => {
  if (currentGroup.value === 'pre') return 'pre'
  if (currentGroup.value === 'post') return 'post'
  return 'detail'
})

const preSteps = computed(() => form.steps.filter((s) => s.phase === 'pre'))
const mainSteps = computed(() => form.steps.filter((s) => s.phase === 'main'))
const postSteps = computed(() => form.steps.filter((s) => s.phase === 'post'))

const activeStep = computed<StepFormItem | null>(() => {
  if (currentGroup.value !== 'main' || currentMainUid.value == null) return null
  return form.steps.find((s) => s._uid === currentMainUid.value) || null
})

const treeData = computed(() => [
  {
    key: 'g-pre',
    type: 'group',
    label: '前置步骤',
    phase: 'pre',
    count: preSteps.value.length,
    children: []
  },
  {
    key: 'g-main',
    type: 'group',
    label: '用例步骤',
    phase: 'main',
    count: mainSteps.value.length,
    children: mainSteps.value.map((s, i) => ({
      key: 'm-' + s._uid,
      type: 'step',
      uid: s._uid,
      order: i + 1
    }))
  },
  {
    key: 'g-post',
    type: 'group',
    label: '后置步骤',
    phase: 'post',
    count: postSteps.value.length,
    children: []
  }
])

function isGroupActive(data: { type: string; phase: string }): boolean {
  return data.type === 'group' && data.phase === currentGroup.value
}

function stepByUid(uid: number): StepFormItem | undefined {
  return form.steps.find((s) => s._uid === uid)
}

function handleNodeClick(data: { type: string; phase?: string; uid?: number }) {
  if (data.type === 'group') {
    currentGroup.value = (data.phase as 'pre' | 'main' | 'post') || 'main'
    currentMainUid.value = null
  } else if (data.type === 'step' && data.uid != null) {
    currentGroup.value = 'main'
    currentMainUid.value = data.uid
  }
}

function isFirstInPhase(uid: number): boolean {
  const s = stepByUid(uid)
  if (!s) return true
  const list = form.steps.filter((x) => x.phase === s.phase)
  return list[0]?._uid === uid
}
function isLastInPhase(uid: number): boolean {
  const s = stepByUid(uid)
  if (!s) return true
  const list = form.steps.filter((x) => x.phase === s.phase)
  return list[list.length - 1]?._uid === uid
}

/** 在当前阶段内上移/下移步骤（保持分组顺序） */
function moveWithinPhase(uid: number, dir: number) {
  const s = stepByUid(uid)
  if (!s) return
  const list = form.steps.filter((x) => x.phase === s.phase)
  const idx = list.findIndex((x) => x._uid === uid)
  const target = idx + dir
  if (target < 0 || target >= list.length) return
  ;[list[idx], list[target]] = [list[target], list[idx]]
  rebuildGrouped()
}

/** 按 前置→主→后置 重新规整数组顺序 */
function rebuildGrouped() {
  form.steps = [...preSteps.value, ...mainSteps.value, ...postSteps.value]
}

function removeStepByUid(uid: number) {
  const idx = form.steps.findIndex((s) => s._uid === uid)
  if (idx < 0) return
  const phase = form.steps[idx].phase
  form.steps.splice(idx, 1)
  if (phase === 'main' && currentMainUid.value === uid) {
    const remain = mainSteps.value
    currentMainUid.value = remain.length ? remain[0]._uid : null
  }
}

/** 请求方法 -> 标签颜色 */
function methodTagType(method: string | null): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (!method) return 'info'
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    GET: 'success',
    POST: '',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'info'
  }
  return map[method.toUpperCase()] || 'info'
}

function goBack() {
  router.push('/case')
}

/* ---- 步骤选择弹框（用例步骤：单接口 / 组合组件） ---- */
const apiDialogVisible = ref(false)
const apiDialogSelectedId = ref<number | null>(null)
const apiDialogSelectedComponentId = ref<number | null>(null)
const apiDialogTargetUid = ref<number | null>(null)
/** 步骤类型：1-单接口 2-组合组件 */
const apiDialogStepType = ref<1 | 2>(1)

const apiDialogPreview = computed(() => {
  if (!apiDialogSelectedId.value) return null
  return apiOptions.value.find((a) => a.id === apiDialogSelectedId.value) || null
})

const apiDialogComponentPreview = computed(() => {
  if (!apiDialogSelectedComponentId.value) return null
  return componentOptions.value.find((c) => c.id === apiDialogSelectedComponentId.value) || null
})

/** 切换步骤类型时清空已选项 */
function onApiDialogTypeChange() {
  apiDialogSelectedId.value = null
  apiDialogSelectedComponentId.value = null
}

function openApiDialog() {
  apiDialogTargetUid.value = null
  apiDialogStepType.value = 1
  apiDialogSelectedId.value = null
  apiDialogSelectedComponentId.value = null
  apiDialogVisible.value = true
}

function openApiDialogForStep(uid: number) {
  const step = stepByUid(uid)
  apiDialogTargetUid.value = uid
  apiDialogStepType.value = step?.stepType === 2 ? 2 : 1
  apiDialogSelectedId.value = apiDialogStepType.value === 1 ? (step?.apiId || null) : null
  apiDialogSelectedComponentId.value = apiDialogStepType.value === 2 ? (step?.componentId ?? null) : null
  apiDialogVisible.value = true
}

function confirmApiSelect() {
  // 组合组件步骤
  if (apiDialogStepType.value === 2) {
    const comp = componentOptions.value.find((c) => c.id === apiDialogSelectedComponentId.value)
    if (!comp) return

    if (apiDialogTargetUid.value !== null) {
      const step = stepByUid(apiDialogTargetUid.value)
      if (!step) return
      const switchedFromApi = step.stepType !== 2
      step.stepType = 2
      step.componentId = comp.id
      step.componentName = comp.name
      step.apiId = 0
      step.apiName = undefined
      step.apiMethod = undefined
      step.apiPath = undefined
      step.requestHeaders = ''
      step.requestParams = ''
      step.assertions = []
      step.responseVar = undefined
      if (switchedFromApi || !step.stepName) step.stepName = comp.name
    } else {
      form.steps.push({
        _uid: uidSeq++,
        apiId: 0,
        apiName: undefined,
        apiMethod: undefined,
        apiPath: undefined,
        phase: 'main',
        stepType: 2,
        componentId: comp.id,
        componentName: comp.name,
        sortOrder: 0,
        stepName: comp.name,
        requestOverride: '',
        requestHeaders: '',
        requestParams: '',
        assertions: [],
        isDisabled: 0,
        promoteGlobal: 0,
        continueOnFail: 0,
        description: ''
      })
    }
    apiDialogVisible.value = false
    return
  }

  if (!apiDialogSelectedId.value) return
  const api = apiOptions.value.find((a) => a.id === apiDialogSelectedId.value)
  if (!api) return

  if (apiDialogTargetUid.value !== null) {
    const step = stepByUid(apiDialogTargetUid.value)
    if (!step) return
    const switchedFromComponent = step.stepType === 2
    step.stepType = 1
    step.componentId = null
    step.componentName = undefined
    step.apiId = api.id
    step.apiName = api.name
    step.apiMethod = api.method
    step.apiPath = api.path
    if (switchedFromComponent || !step.stepName) step.stepName = api.name
  } else {
    form.steps.push({
      _uid: uidSeq++,
      apiId: api.id,
      apiName: api.name,
      apiMethod: api.method,
      apiPath: api.path,
      phase: 'main',
      stepType: 1,
      componentId: null,
      sortOrder: 0,
      stepName: api.name,
      requestOverride: '',
      requestHeaders: '',
      requestParams: '',
      assertions: [],
      isDisabled: 0,
      promoteGlobal: 0,
      continueOnFail: 0,
      description: ''
    })
  }
  apiDialogVisible.value = false
}

/* ---- 前置/后置扩展弹框 ---- */
const extDialogVisible = ref(false)
const extDialogPhase = ref<'pre' | 'post'>('pre')
const extDialogTitle = computed(() => (extDialogPhase.value === 'pre' ? '新增前置扩展' : '新增后置扩展'))
const componentOptions = ref<ApiComponentInfo[]>([])
const componentMap = ref<Record<number, string>>({})
const generatorOptions = ref<DataGeneratorInfo[]>([])
const generatorMap = ref<Record<number, string>>({})
const editingExtUid = ref<number | null>(null)

const extForm = reactive({
  type: 2,
  componentId: null as number | null,
  generatorId: null as number | null,
  generatorName: '',
  variableName: '',
  regenEachRun: 1,
  stepName: '',
  responseVar: '',
  isDisabled: 0,
  promoteGlobal: 0,
  continueOnFail: 0,
  description: ''
})

function openExtensionDialog(phase: 'pre' | 'post', step?: CaseExtensionStep) {
  extDialogPhase.value = phase
  editingExtUid.value = step ? step._uid : null
  if (step) {
    extForm.type = step.stepType || 2
    extForm.componentId = step.componentId ?? null
    extForm.generatorId = step.generatorId ?? null
    extForm.generatorName = step.generatorName || ''
    extForm.variableName = step.variableName || ''
    extForm.regenEachRun = step.regenEachRun ?? 1
    extForm.stepName = step.stepName || ''
    extForm.responseVar = step.responseVar || ''
    extForm.isDisabled = step.isDisabled
    extForm.promoteGlobal = step.promoteGlobal
    extForm.continueOnFail = step.continueOnFail
    extForm.description = step.description || ''
  } else {
    extForm.type = 2
    extForm.componentId = null
    extForm.generatorId = null
    extForm.generatorName = ''
    extForm.variableName = ''
    extForm.regenEachRun = 1
    extForm.stepName = ''
    extForm.responseVar = ''
    extForm.isDisabled = 0
    extForm.promoteGlobal = 0
    extForm.continueOnFail = 0
    extForm.description = ''
  }
  extDialogVisible.value = true
}

function onExtTypeChange() {
  // 切换扩展类型时清空另一分支的已选值，避免脏数据落库
  extForm.componentId = null
  extForm.generatorId = null
  extForm.generatorName = ''
  extForm.variableName = ''
  extForm.stepName = ''
}

function onExtComponentChange(id: number) {
  const c = componentOptions.value.find((x) => x.id === id)
  extForm.stepName = c ? c.name : ''
}

function onExtGeneratorChange(id: number) {
  const g = generatorOptions.value.find((x) => x.id === id)
  if (g) {
    extForm.generatorName = g.name
    extForm.stepName = g.name
    if (!extForm.variableName) extForm.variableName = g.name
  }
}

function confirmExtension() {
  const phase = extDialogPhase.value
  let name = ''
  let generatorName = ''
  let variableName: string | undefined
  let componentId: number | null = null
  let componentName: string | undefined

  if (extForm.type === 3) {
    if (!extForm.generatorId) return
    const g = generatorOptions.value.find((x) => x.id === extForm.generatorId!)
    generatorName = g?.name || ''
    name = extForm.stepName || generatorName
    variableName = extForm.variableName.trim() || undefined
  } else {
    if (!extForm.componentId) return
    const c = componentOptions.value.find((x) => x.id === extForm.componentId!)
    componentId = extForm.componentId
    componentName = c?.name
    name = extForm.stepName || c?.name || ''
  }

  if (editingExtUid.value != null) {
    const step = stepByUid(editingExtUid.value)
    if (step) {
      step.stepType = extForm.type
      step.componentId = componentId
      step.componentName = componentName
      step.generatorId = extForm.type === 3 ? extForm.generatorId : null
      step.generatorName = generatorName
      step.variableName = variableName
      step.regenEachRun = extForm.regenEachRun
      step.stepName = name
      step.responseVar = extForm.responseVar.trim() || undefined
      step.isDisabled = extForm.isDisabled
      step.promoteGlobal = extForm.promoteGlobal
      step.continueOnFail = extForm.continueOnFail
      step.description = extForm.description.trim() || undefined
    }
  } else {
    form.steps.push({
      _uid: uidSeq++,
      apiId: 0,
      apiName: undefined,
      apiMethod: undefined,
      apiPath: undefined,
      phase,
      stepType: extForm.type,
      componentId,
      componentName,
      generatorId: extForm.type === 3 ? extForm.generatorId : null,
      generatorName,
      variableName,
      regenEachRun: extForm.regenEachRun,
      sortOrder: 0,
      stepName: name,
      requestOverride: '',
      requestHeaders: '',
      requestParams: '',
      assertions: [],
      responseVar: extForm.responseVar.trim() || undefined,
      isDisabled: extForm.isDisabled,
      promoteGlobal: extForm.promoteGlobal,
      continueOnFail: extForm.continueOnFail,
      description: extForm.description.trim() || undefined
    })
  }
  extDialogVisible.value = false
}

/* ---- 调试 ---- */
async function loadEnvOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    envOptions.value = []
    return
  }
  try {
    const result = await getEnvList({ projectId, page: 1, size: 200 })
    envOptions.value = result.records
    if (envOptions.value.length > 0 && debugEnvId.value === null) {
      debugEnvId.value = envOptions.value[0].id
    }
  } catch {
    envOptions.value = []
  }
}

function stepStatusType(status: StepExecuteResult['status']): 'success' | 'danger' | 'info' {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

/** JSON 字符串格式化为缩进展示；非 JSON 原样返回 */
function formatJson(text: string | null | undefined): string {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

/** 输入框失焦时，若内容为合法 JSON 则格式化为缩进 */
function formatJsonField(field: 'requestHeaders' | 'requestParams') {
  const step = activeStep.value
  if (!step) return
  const text = step[field]
  if (!text || !text.trim()) return
  try {
    step[field] = JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    // 非法 JSON 保持原样
  }
}

async function handleDebug() {
  if (editingId.value === null) {
    ElMessage.warning('请先保存用例，再进行调试')
    return
  }
  if (debugEnvId.value === null) {
    ElMessage.warning('请先选择调试环境')
    return
  }
  debugging.value = true
  try {
    debugResult.value = await executeCase(editingId.value, debugEnvId.value, true)
    debugVisible.value = true
  } catch {
    // 错误提示已由拦截器处理
  } finally {
    debugging.value = false
  }
}

/* ---- 级联下拉逻辑 ---- */
async function loadAppOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    appOptions.value = []
    return
  }
  try {
    appOptions.value = await getProjectOptions(projectId)
  } catch {
    appOptions.value = []
  }
}

async function loadVersionOptions(applicationId: number) {
  try {
    versionOptions.value = await getVersionOptions(applicationId)
  } catch {
    versionOptions.value = []
  }
}

async function loadModuleOptions(versionId: number) {
  try {
    moduleOptions.value = await getModuleOptions(versionId)
  } catch {
    moduleOptions.value = []
  }
}

function onApplicationChange(appId: number) {
  form.versionId = null
  form.moduleId = null
  versionOptions.value = []
  moduleOptions.value = []
  if (appId) {
    loadVersionOptions(appId)
  }
  loadApiOptions()
}

function onVersionChange(versionId: number) {
  form.moduleId = null
  moduleOptions.value = []
  if (versionId) {
    loadModuleOptions(versionId)
  }
  loadApiOptions()
}

function onModuleChange() {
  loadApiOptions()
}

async function loadApiOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    apiOptions.value = []
    return
  }
  try {
    const result = await getApiList({
      projectId,
      moduleId: form.moduleId || undefined,
      applicationId: form.applicationId || undefined,
      versionId: form.versionId || undefined,
      page: 1,
      size: 500
    })
    apiOptions.value = result.records
  } catch {
    apiOptions.value = []
  }
}

async function loadComponentOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    componentOptions.value = []
    return
  }
  try {
    const result = await getComponentList({ projectId, page: 1, size: 500 })
    componentOptions.value = result.records
    const map: Record<number, string> = {}
    result.records.forEach((c) => {
      map[c.id] = c.name
    })
    componentMap.value = map
  } catch {
    componentOptions.value = []
  }
}

async function loadGeneratorOptions() {
  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    generatorOptions.value = []
    return
  }
  try {
    const result = await getGeneratorList({ projectId, page: 1, size: 500 })
    generatorOptions.value = result.records
    const map: Record<number, string> = {}
    result.records.forEach((g) => {
      map[g.id] = g.name
    })
    generatorMap.value = map
  } catch {
    generatorOptions.value = []
  }
}

/** 步骤展示用的组合组件名称（优先回显名，其次查组件映射） */
function componentNameOf(step: StepFormItem): string {
  if (step.componentName) return step.componentName
  if (step.componentId) return componentMap.value[step.componentId] || ''
  return ''
}

/** 获取步骤对应的接口信息 */
function getStepApiInfo(step: StepFormItem): Pick<ApiInfo, 'id' | 'name' | 'method' | 'path'> | undefined {
  if (step.apiMethod || step.apiPath) {
    return {
      id: step.apiId,
      name: step.apiName || '',
      method: step.apiMethod || '',
      path: step.apiPath || ''
    }
  }
  const found = apiOptions.value.find((a) => a.id === step.apiId)
  if (!found) return undefined
  return { id: found.id, name: found.name, method: found.method, path: found.path }
}

/** 将 requestOverride JSON 拆分为 headers 和 params */
function parseRequestOverride(override: string | undefined | null): { headers: string; params: string } {
  if (!override?.trim()) return { headers: '', params: '' }
  try {
    const obj = JSON.parse(override)
    const headers = obj.headers ? JSON.stringify(obj.headers, null, 2) : ''
    const params = obj.body ? JSON.stringify(obj.body, null, 2) : (obj.params ? JSON.stringify(obj.params, null, 2) : '')
    return { headers, params }
  } catch {
    return { headers: '', params: '' }
  }
}

/** 将 headers 和 params 合并为 requestOverride JSON */
function buildRequestOverride(headers: string, params: string): string {
  const obj: Record<string, unknown> = {}
  let hasContent = false
  if (headers?.trim()) {
    try { obj.headers = JSON.parse(headers); hasContent = true } catch { /* ignore */ }
  }
  if (params?.trim()) {
    try { obj.body = JSON.parse(params); hasContent = true } catch { /* ignore */ }
  }
  return hasContent ? JSON.stringify(obj) : ''
}

function isValidJson(str: string): boolean {
  if (!str.trim()) return true
  try {
    JSON.parse(str)
    return true
  } catch {
    return false
  }
}

function serializeAssertions(list: AssertionItem[]): string | undefined {
  const valid = list.filter((a) => a.type)
  return valid.length > 0 ? JSON.stringify(valid) : undefined
}

function parseAssertions(json: string | null | undefined): AssertionItem[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function addAssertion() {
  if (!activeStep.value) return
  activeStep.value.assertions.push({ type: 'status' })
}

function toggleAssertions() {
  showAssertions.value = !showAssertions.value
}

function removeAssertion(index: number) {
  if (!activeStep.value) return
  activeStep.value.assertions.splice(index, 1)
}

function showExpected(a: AssertionItem): boolean {
  if (a.type === 'jsonPath' || a.type === 'header') {
    return a.operator !== 'exists'
  }
  return true
}

function assertionExpectedPlaceholder(a: AssertionItem): string {
  switch (a.type) {
    case 'status':
      return '期望状态码，如 200'
    case 'body':
      return '期望包含的文本'
    case 'time':
      return '最大耗时(ms)，如 2000'
    default:
      return '期望值'
  }
}

/** 请求覆盖 Tab 当前激活标签 */
const requestTab = ref('headers')
const showAssertions = ref(false)

async function handleSubmit() {
  if (!formEl.value) return
  const valid = await formEl.value.validate().catch(() => false)
  if (!valid) return

  // 步骤校验
  for (let i = 0; i < form.steps.length; i++) {
    const step = form.steps[i]
    if (step.stepType === 2) {
      if (!step.componentId) {
        ElMessage.error(`第 ${i + 1} 步未选择组合组件`)
        return
      }
    } else if (step.phase === 'main') {
      if (!step.apiId) {
        ElMessage.error(`第 ${i + 1} 步未选择接口`)
        return
      }
    }
    if (step.requestHeaders?.trim() && !isValidJson(step.requestHeaders)) {
      ElMessage.error(`第 ${i + 1} 步请求头不是合法的 JSON`)
      return
    }
    if (step.requestParams?.trim() && !isValidJson(step.requestParams)) {
      ElMessage.error(`第 ${i + 1} 步请求参数不是合法的 JSON`)
      return
    }
  }

  const projectId = projectStore.currentProject?.id
  if (!projectId) {
    ElMessage.warning('请先选择项目')
    return
  }

  // 按 前置→主→后置 顺序提交，统一 sortOrder
  rebuildGrouped()
  const stepsPayload: StepParams[] = form.steps.map((s, i) => ({
    apiId: s.stepType === 1 ? s.apiId : undefined,
    phase: s.phase,
    stepType: s.stepType,
    componentId: s.stepType === 2 ? s.componentId ?? undefined : undefined,
    generatorId: s.stepType === 3 ? s.generatorId ?? undefined : undefined,
    variableName: s.stepType === 3 ? s.variableName?.trim() || undefined : undefined,
    regenEachRun: s.stepType === 3 ? s.regenEachRun === 1 : undefined,
    sortOrder: i + 1,
    stepName: s.stepName?.trim() || undefined,
    requestOverride: buildRequestOverride(s.requestHeaders, s.requestParams) || undefined,
    assertions: serializeAssertions(s.assertions),
    responseVar: s.responseVar?.trim() || undefined,
    isDisabled: s.isDisabled,
    promoteGlobal: s.promoteGlobal,
    continueOnFail: s.continueOnFail,
    description: s.description?.trim() || undefined
  }))

  submitting.value = true
  try {
    const payload = {
      applicationId: form.applicationId!,
      versionId: form.versionId!,
      moduleId: form.moduleId!,
      caseName: form.caseName.trim(),
      level: form.level,
      setupScript: form.setupScript.trim() || undefined,
      status: form.statusBool ? 1 : 0,
      steps: stepsPayload.length > 0 ? stepsPayload : undefined
    }

    if (isEdit.value && editingId.value !== null) {
      await updateCase({ id: editingId.value, ...payload })
      ElMessage.success('用例修改成功')
    } else {
      await createCase({ projectId, ...payload })
      ElMessage.success('用例新增成功')
    }
    goBack()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadAppOptions()
  await loadEnvOptions()
  await loadComponentOptions()
  await loadGeneratorOptions()

  const caseId = route.query.id
  if (caseId) {
    editingId.value = Number(caseId)
    try {
      const detail = await getCaseDetail(editingId.value)
      form.caseName = detail.name
      form.applicationId = detail.applicationId
      form.versionId = detail.versionId
      form.moduleId = detail.moduleId
      form.level = detail.level
      form.statusBool = detail.status === 1
      form.setupScript = detail.setupScript || ''
      form.steps = (detail.steps || []).map((s) => {
        const { headers, params } = parseRequestOverride(s.requestOverride)
        return {
          _uid: uidSeq++,
          apiId: s.apiId,
          apiName: s.apiName || undefined,
          apiMethod: s.apiMethod || undefined,
          apiPath: s.apiPath || undefined,
          phase: s.phase || 'main',
          stepType: s.stepType || 1,
          componentId: s.componentId ?? null,
          componentName: s.componentId ? componentMap.value[s.componentId] : undefined,
          generatorId: s.generatorId ?? null,
          generatorName: s.generatorId ? generatorMap.value[s.generatorId] : undefined,
          variableName: s.variableName || undefined,
          regenEachRun: s.regenEachRun ?? 0,
          sortOrder: s.sortOrder,
          stepName: s.stepName || '',
          requestOverride: s.requestOverride || '',
          responseVar: s.responseVar || undefined,
          requestHeaders: headers,
          requestParams: params,
          assertions: parseAssertions(s.assertions),
          isDisabled: s.isDisabled ?? 0,
          promoteGlobal: s.promoteGlobal ?? 0,
          continueOnFail: s.continueOnFail ?? 0,
          description: s.description || undefined
        } as StepFormItem
      })

      const mains = mainSteps.value
      if (mains.length > 0) {
        currentGroup.value = 'main'
        currentMainUid.value = mains[0]._uid
      } else if (preSteps.value.length > 0) {
        currentGroup.value = 'pre'
        currentMainUid.value = null
      } else if (postSteps.value.length > 0) {
        currentGroup.value = 'post'
        currentMainUid.value = null
      }

      if (detail.applicationId) {
        await loadVersionOptions(detail.applicationId)
      }
      if (detail.versionId) {
        await loadModuleOptions(detail.versionId)
      }
    } catch {
      ElMessage.error('加载用例详情失败')
    }
  }

  await loadApiOptions()
})
</script>

<style scoped>
.case-edit-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ---------- 顶部操作栏 ---------- */
.top-bar {
  display: flex;
  flex-direction: column;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
  gap: 10px;
}

.top-bar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-bar-fields {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-form-item {
  margin-bottom: 0 !important;
}

.bar-form-item :deep(.el-form-item__label) {
  font-size: 13px;
  color: #374151;
  padding-right: 6px;
}

.bar-form-item :deep(.el-form-item__content) {
  flex: 1;
}

.bar-select {
  width: 160px;
}

.bar-input-name {
  width: 220px;
}

.bar-select-sm {
  width: 130px;
}

/* ---------- 主体 ---------- */
.edit-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f5f7fa;
}

.edit-form {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.left-panel {
  width: 420px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.right-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  border-radius: 8px;
}

.section-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f1f3;
  background: #fafbfc;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* ---------- 步骤导航树 ---------- */
.steps-section {
  min-height: 400px;
}

.steps-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.steps-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.debug-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.debug-env-select {
  width: 160px;
}

.step-tree {
  margin-top: 6px;
}

.step-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 4px 0;
}

.tree-group {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  padding: 6px 8px;
  border-radius: 6px;
}

.tree-group.tree-group-active {
  background: #ecf5ff;
  color: #1677ff;
}

.tree-group-icon {
  font-size: 16px;
}

.tree-group-badge {
  margin-left: 4px;
}

.tree-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
}

.tree-step.tree-step-active {
  background: #ecf5ff;
}

.tree-step-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.tree-step-api {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.tree-step-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-step-missing {
  color: #f56c6c;
  font-size: 13px;
  flex: 1;
}

.tree-step-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  flex-shrink: 0;
}

/* ---------- 前置/后置扩展 ---------- */
.extension-section {
  min-height: 300px;
}

/* ---------- 请求覆盖 Tabs ---------- */
.request-override-section {
  margin-bottom: 14px;
}

.request-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

.request-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.request-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  font-weight: 600;
}

/* ---------- 右侧步骤详情 ---------- */
.detail-section {
  min-height: 300px;
}

.component-step-tip {
  margin-bottom: 14px;
}

.detail-empty {
  min-height: 200px;
}

.detail-empty :deep(.el-card__body) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
}

.detail-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-api-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.step-form-item {
  margin-bottom: 14px;
}

.field-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.assertion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.assertion-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assertion-type {
  width: 110px;
  flex-shrink: 0;
}

.assertion-path {
  width: 150px;
}

.assertion-operator {
  width: 90px;
}

.assertion-expected {
  flex: 1;
}

/* ---------- 接口标签 ---------- */
.method-tag {
  margin-right: 0;
  font-size: 12px;
  font-weight: 600;
  min-width: 48px;
  text-align: center;
}

.api-path {
  color: #374151;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.text-muted {
  color: #9ca3af;
}

/* ---------- 接口选择弹框 ---------- */
.api-dialog-body {
  padding: 0;
}

.api-dialog-select {
  width: 100%;
}

.api-select-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-option-name {
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-dialog-preview {
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafbfc;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.preview-row:last-child {
  margin-bottom: 0;
}

.preview-label {
  width: 70px;
  flex-shrink: 0;
  font-size: 13px;
  color: #6b7280;
}

.preview-value {
  font-size: 13px;
  color: #1f2937;
}

/* 扩展弹框 */
.ext-dialog-select {
  width: 100%;
}

/* ---------- 代码文本框 ---------- */
.code-textarea :deep(.el-textarea__inner) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
}

/* ---------- 调试工具栏 ---------- */
.debug-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.debug-env-select {
  width: 160px;
}

/* ---------- 调试结果 ---------- */
.debug-result {
  padding: 0 4px;
}

.debug-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.debug-meta {
  color: #6b7280;
  font-size: 13px;
}

.debug-round {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.debug-round-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.debug-round-params {
  margin-bottom: 8px;
}

.debug-step {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.debug-step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.debug-step-title {
  font-weight: 600;
  color: #1f2937;
}

.debug-step-api {
  color: #374151;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

.debug-step-time {
  margin-left: auto;
  color: #9ca3af;
  font-size: 12px;
}

.debug-error {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fef2f2;
  color: #dc2626;
  font-size: 12px;
  white-space: pre-wrap;
}

.debug-pre {
  margin: 0;
  padding: 10px;
  border-radius: 6px;
  background: #f9fafb;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.debug-kv {
  margin-bottom: 12px;
}

.debug-kv:last-child {
  margin-bottom: 0;
}

.debug-kv-label {
  margin-bottom: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.debug-status {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.debug-assert-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}
</style>
