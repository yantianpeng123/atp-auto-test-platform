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
        <h1 class="brand-title">三分钟<br />接入你的团队</h1>
        <p class="brand-subtitle">
          创建账号即可拥有独立的项目空间、用例库与执行记录。
        </p>

        <ul class="feature-list">
          <li>
            <span class="feature-dot" />
            <div>
              <strong>项目级隔离</strong>
              <span>按项目划分用例与环境，团队之间互不干扰</span>
            </div>
          </li>
          <li>
            <span class="feature-dot" />
            <div>
              <strong>角色化协作</strong>
              <span>管理员、测试工程师、只读成员三种权限</span>
            </div>
          </li>
          <li>
            <span class="feature-dot" />
            <div>
              <strong>开箱即用</strong>
              <span>注册即赠演示项目，含示例接口与用例模板</span>
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

        <h2 class="form-title">创建账号</h2>
        <p class="form-desc">填写以下信息，开启你的自动化测试之旅</p>

        <el-form ref="formRef" :model="formState" :rules="rules" size="large" @submit.prevent>
          <el-form-item prop="username">
            <el-input
              v-model="formState.username"
              placeholder="用户名，4-20 位字母、数字或下划线"
              :prefix-icon="User"
              clearable
              @blur="handleUsernameBlur"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="formState.email"
              placeholder="邮箱，用于找回密码"
              :prefix-icon="MailIcon"
              clearable
            />
          </el-form-item>

          <div class="two-col">
            <el-form-item prop="nickname">
              <el-input
                v-model="formState.nickname"
                placeholder="昵称（选填）"
                :prefix-icon="UserFilled"
                clearable
              />
            </el-form-item>

            <el-form-item prop="phone">
              <el-input
                v-model="formState.phone"
                placeholder="手机号（选填）"
                :prefix-icon="Phone"
                clearable
              />
            </el-form-item>
          </div>

          <el-form-item prop="password">
            <el-input
              v-model="formState.password"
              type="password"
              placeholder="密码，至少 6 位"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <div v-if="formState.password" class="strength">
            <div class="strength-bars">
              <span :class="['bar', { active: strength.level >= 1, [`lv${strength.level}`]: true }]" />
              <span :class="['bar', { active: strength.level >= 2, [`lv${strength.level}`]: true }]" />
              <span :class="['bar', { active: strength.level >= 3, [`lv${strength.level}`]: true }]" />
            </div>
            <span class="strength-text">{{ strength.text }}</span>
          </div>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="formState.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-form-item v-if="captchaEnabled" prop="captchaCode">
            <div class="captcha-row">
              <el-input
                v-model="formState.captchaCode"
                placeholder="请输入验证码"
                :prefix-icon="Key"
                clearable
                @keyup.enter="handleRegister"
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

          <el-form-item prop="agree" class="agree-item">
            <el-checkbox v-model="formState.agree">
              我已阅读并同意
              <el-link type="primary" :underline="false">《服务条款》</el-link>
              和
              <el-link type="primary" :underline="false">《隐私政策》</el-link>
            </el-checkbox>
          </el-form-item>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="submitting"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form>

        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="link">返回登录</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  Key,
  Lock,
  Message as MailIcon,
  Phone,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { checkUsername, getCaptcha } from '@/api/auth'
import type { CaptchaData, RegisterParams } from '@/api/types'

interface RegisterForm {
  username: string
  email: string
  nickname: string
  phone: string
  password: string
  confirmPassword: string
  captchaCode: string
  agree: boolean
}

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const formState = reactive<RegisterForm>({
  username: '',
  email: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
  agree: false
})

const validateConfirm = (_rule: unknown, value: string, callback: (e?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
    return
  }
  if (value !== formState.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const validateAgree = (_rule: unknown, value: boolean, callback: (e?: Error) => void) => {
  if (!value) {
    callback(new Error('请先阅读并同意服务条款'))
    return
  }
  callback()
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为 4-20 位', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z0-9_]+$/,
      message: '用户名只能包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '手机号格式不正确',
      trigger: 'blur'
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  agree: [{ validator: validateAgree, trigger: 'change' }]
}

/** 密码强度：弱 / 中 / 强 */
const strength = computed(() => {
  const pwd = formState.password
  if (!pwd) {
    return { level: 0, text: '' }
  }
  let score = 0
  if (pwd.length >= 6) score += 1
  if (pwd.length >= 10) score += 1
  if (/[a-zA-Z]/.test(pwd) && /\d/.test(pwd)) score += 1
  if (/[^a-zA-Z0-9]/.test(pwd)) score += 1

  if (score <= 1) return { level: 1, text: '强度：弱', color: '#e24b4a' }
  if (score === 2) return { level: 2, text: '强度：中', color: '#ba7517' }
  return { level: 3, text: '强度：强', color: '#1d9e75' }
})

/** 验证码：Redis 可用时自动开启，不可用时静默降级 */
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

/** 失焦时校验用户名是否被占用 */
async function handleUsernameBlur() {
  const username = formState.username.trim()
  if (username.length < 4 || !/^[a-zA-Z0-9_]+$/.test(username)) {
    return
  }
  try {
    const available = await checkUsername(username)
    if (!available) {
      ElMessage.warning('该用户名已被注册，换一个试试')
    }
  } catch {
    // 校验失败不阻塞注册，提交时由后端最终判定
  }
}

async function handleRegister() {
  const form = formRef.value
  if (!form) return

  try {
    await form.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const params: RegisterParams = {
      username: formState.username.trim(),
      password: formState.password,
      confirmPassword: formState.confirmPassword,
      email: formState.email.trim(),
      nickname: formState.nickname.trim() || undefined,
      phone: formState.phone.trim() || undefined
    }
    if (captchaEnabled.value) {
      params.captchaKey = captcha.value.captchaKey
      params.captchaCode = formState.captchaCode
    }

    await userStore.register(params)
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { username: formState.username } })
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
  max-width: 412px;
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
  margin: 0 0 28px;
  font-size: 14px;
  color: #6b7280;
}

.two-col {
  display: flex;
  gap: 12px;
}

.two-col :deep(.el-form-item) {
  flex: 1;
  min-width: 0;
}

.strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: -14px 0 18px;
}

.strength-bars {
  display: flex;
  gap: 5px;
  flex: 1;
}

.bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: #e5e7eb;
  transition: background 0.2s ease;
}

.bar.active.lv1 {
  background: #e24b4a;
}

.bar.active.lv2 {
  background: #ba7517;
}

.bar.active.lv3 {
  background: #1d9e75;
}

.strength-text {
  font-size: 12px;
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

.agree-item {
  margin-top: -6px;
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
  margin-bottom: 20px;
}

:deep(.agree-item .el-form-item__error) {
  padding-top: 2px;
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
    align-items: flex-start;
    padding: 32px 24px;
    padding-top: 6vh;
  }
}

@media (max-width: 520px) {
  .two-col {
    flex-direction: column;
    gap: 0;
  }
}
</style>
