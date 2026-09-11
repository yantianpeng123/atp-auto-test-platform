<template>
  <div class="auth-page">
    <!-- 左侧品牌展示区 -->
    <section class="brand-panel">
      <div class="brand-deco deco-1" />
      <div class="brand-deco deco-2" />

      <div class="brand-top">
        <div class="logo">
          <span class="logo-mark">ATP</span>
          <span class="logo-text">自动化测试平台</span>
        </div>
      </div>

      <div class="brand-main">
        <h1 class="brand-title">让回归测试<br />不再靠人肉</h1>
        <p class="brand-subtitle">
          接口用例集中管理，定时任务自动执行，失败归因一目了然。
        </p>

        <ul class="feature-list">
          <li>
            <span class="feature-dot" />
            <div>
              <strong>接口自动化</strong>
              <span>参数化请求 + 多维度断言，一次录入反复复用</span>
            </div>
          </li>
          <li>
            <span class="feature-dot" />
            <div>
              <strong>定时回归</strong>
              <span>Cron 调度与 CI 流水线触发，无人值守跑批</span>
            </div>
          </li>
          <li>
            <span class="feature-dot" />
            <div>
              <strong>可视报告</strong>
              <span>通过率趋势、耗时分布、失败明细追根溯源</span>
            </div>
          </li>
        </ul>
      </div>

      <div class="brand-footer">© 2026 ATP · Auto Test Platform</div>
    </section>

    <!-- 右侧表单区 -->
    <section class="form-panel">
      <div class="form-wrapper">
        <div class="mobile-logo">
          <span class="logo-mark">ATP</span>
          <span>自动化测试平台</span>
        </div>

        <h2 class="form-title">欢迎回来</h2>
        <p class="form-desc">登录后继续管理你的测试资产</p>

        <el-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          size="large"
          @submit.prevent
        >
          <el-form-item prop="username">
            <el-input
              v-model="formState.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="formState.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item v-if="captchaEnabled" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="formState.captchaCode"
                placeholder="请输入验证码"
                :prefix-icon="Key"
                clearable
                @keyup.enter="handleLogin"
              />
              <img
                v-if="captcha.imageBase64"
                :src="captcha.imageBase64"
                class="captcha-img"
                alt="验证码"
                title="点击刷新"
                @click="refreshCaptcha"
              />
              <div v-else class="captcha-img captcha-placeholder" @click="refreshCaptcha">
                点击获取
              </div>
            </div>
          </el-form-item>

          <div class="form-extra">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" :underline="false">忘记密码？</el-link>
          </div>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="submitting"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="link">立即注册</router-link>
        </div>

        <div class="demo-tip">
          <span class="demo-label">演示账号</span>
          <el-button link type="primary" size="small" @click="fillDemo">管理员一键填充</el-button>
          <el-button link type="primary" size="small" @click="fillDemonot">非管理员一键填充</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Key, Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'
import type { CaptchaData, LoginParams } from '@/api/types'

interface LoginForm {
  username: string
  password: string
  captchaCode: string
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
/** 记住我：预留开关，后续可接入令牌续期或长期令牌策略 */
const rememberMe = ref(true)

const formState = reactive<LoginForm>({
  username: '',
  password: '',
  captchaCode: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为 4-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/**
 * 验证码默认不展示。Redis 可用时自动开启，不可用时静默降级，
 * 避免中间件故障阻塞登录主流程。
 */
const captchaEnabled = ref(false)
const captcha = ref<CaptchaData>({ captchaKey: '', imageBase64: '', expireSeconds: 0 })

async function refreshCaptcha() {
  try {
    captcha.value = await getCaptcha()
    captchaEnabled.value = Boolean(captcha.value.imageBase64)
  } catch {
    captchaEnabled.value = false
  }
}

function fillDemo() {
  formState.username = 'admin'
  formState.password = 'admin123'
}
function fillDemonot(){
  formState.username = 'test001'
  formState.password = 'test001'
}

async function handleLogin() {
  const form = formRef.value
  if (!form) return

  try {
    await form.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const params: LoginParams = {
      username: formState.username,
      password: formState.password
    }
    if (captchaEnabled.value) {
      params.captchaKey = captcha.value.captchaKey
      params.captchaCode = formState.captchaCode
    }

    await userStore.login(params)
    ElMessage.success(`欢迎回来，${userStore.displayName}`)

    const redirect = route.query.redirect as string | undefined
    router.push(redirect || '/project/select')
  } catch {
    if (captchaEnabled.value) {
      formState.captchaCode = ''
      refreshCaptcha()
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.auth-page {
  display: flex;
  min-height: 100vh;
  background: #ffffff;
}

/* ---------- 左侧品牌区 ---------- */
.brand-panel {
  position: relative;
  width: 44%;
  max-width: 560px;
  padding: 56px 52px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  background: linear-gradient(155deg, #0b1b33 0%, #14346b 52%, #1d63d1 100%);
  color: #ffffff;
}

.brand-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  pointer-events: none;
}

.deco-1 {
  width: 320px;
  height: 320px;
  right: -120px;
  top: -80px;
}

.deco-2 {
  width: 220px;
  height: 220px;
  left: -90px;
  bottom: -60px;
  background: rgba(255, 255, 255, 0.05);
}

.brand-top,
.brand-main,
.brand-footer {
  position: relative;
  z-index: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.logo-text {
  font-size: 17px;
  font-weight: 500;
  letter-spacing: 1px;
}

.brand-title {
  margin: 0 0 18px;
  font-size: 34px;
  line-height: 1.45;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-subtitle {
  margin: 0 0 40px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.72);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.feature-list li {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.feature-dot {
  flex: none;
  width: 7px;
  height: 7px;
  margin-top: 8px;
  border-radius: 50%;
  background: #6ea8ff;
}

.feature-list strong {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.feature-list span {
  font-size: 13px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.62);
}

.brand-footer {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

/* ---------- 右侧表单区 ---------- */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 32px;
}

.form-wrapper {
  width: 100%;
  max-width: 372px;
}

.mobile-logo {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 36px;
  font-size: 16px;
  font-weight: 500;
  color: #1f2937;
}

.mobile-logo .logo-mark {
  background: #1d63d1;
  color: #ffffff;
}

.form-title {
  margin: 0 0 8px;
  font-size: 27px;
  font-weight: 600;
  color: #111827;
}

.form-desc {
  margin: 0 0 32px;
  font-size: 14px;
  color: #6b7280;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-row :deep(.el-input) {
  flex: 1;
}

.captcha-img {
  flex: none;
  width: 122px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  object-fit: cover;
  background: #f5f7fa;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #909399;
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -6px 0 22px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: #1d63d1;
  border-color: #1d63d1;
}

.submit-btn:hover,
.submit-btn:focus {
  background: #1755b8;
  border-color: #1755b8;
}

.form-footer {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
}

.link {
  color: #1d63d1;
  font-weight: 500;
  margin-left: 4px;
}

.link:hover {
  color: #1755b8;
}

.demo-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 34px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f4f7fd;
  font-size: 13px;
  color: #4b5563;
}

.demo-label {
  padding: 2px 8px;
  border-radius: 4px;
  background: #e3ecfb;
  color: #1d63d1;
  font-size: 12px;
}

.demo-tip code {
  flex: 1;
  font-family: 'SFMono-Regular', Menlo, Consolas, monospace;
  font-size: 12.5px;
  color: #374151;
}

/* 输入框风格统一 */
:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  padding: 0 12px;
  height: 44px;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1d63d1 inset;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }

  .mobile-logo {
    display: flex;
  }

  .form-panel {
    padding: 32px 24px;
    align-items: flex-start;
    padding-top: 8vh;
  }
}
</style>
