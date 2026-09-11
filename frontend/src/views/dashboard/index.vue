<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <section class="welcome">
      <div class="welcome-text">
        <h2>你好，{{ userStore.displayName }}</h2>
        <p>
          第一阶段（架构 + 用户认证）已跑通，下面可以开始搭建测试资产模块了。
        </p>
        <div class="welcome-meta">
          <el-tag size="small" effect="dark" type="primary">{{ roleText }}</el-tag>
          <span>上次登录：{{ lastLoginText }}</span>
        </div>
      </div>
      <div class="welcome-badge">
        <div class="badge-num">1 / 5</div>
        <div class="badge-label">阶段进度</div>
      </div>
    </section>

    <!-- 统计卡片 -->
    <section class="stats">
      <el-card v-for="item in statCards" :key="item.label" shadow="never" class="stat-card">
        <div class="stat-icon" :style="{ background: item.bg }">
          <el-icon :size="20" :style="{ color: item.color }">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
        <el-tag size="small" type="info" effect="plain">待接入</el-tag>
      </el-card>
    </section>

    <div class="grid">
      <!-- 路线图 -->
      <el-card shadow="never" class="card">
        <template #header>
          <span class="card-title">开发路线图</span>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="stage in stages"
            :key="stage.name"
            :type="stage.type"
            :hollow="stage.hollow"
            :timestamp="stage.period"
            placement="top"
          >
            <div class="stage-name">{{ stage.name }}</div>
            <div class="stage-desc">{{ stage.desc }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <!-- 已就绪接口 -->
      <el-card shadow="never" class="card">
        <template #header>
          <span class="card-title">已就绪接口（第一阶段）</span>
        </template>
        <el-table :data="apiList" size="small" style="width: 100%">
          <el-table-column prop="method" label="方法" width="76">
            <template #default="{ row }">
              <el-tag :type="row.method === 'GET' ? 'success' : 'primary'" size="small" effect="plain">
                {{ row.method }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" min-width="180" />
          <el-table-column prop="desc" label="说明" min-width="110" />
        </el-table>

        <div class="tech-stack">
          <span class="tech-label">技术栈</span>
          <el-tag v-for="tag in techStack" :key="tag" size="small" effect="plain" class="tech-tag">
            {{ tag }}
          </el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Connection,
  DataAnalysis,
  Files,
  Timer
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const roleMap: Record<string, string> = {
  ADMIN: '平台管理员',
  TESTER: '测试工程师',
  VIEWER: '只读成员'
}
const roleText = computed(() => roleMap[userStore.userInfo?.role || ''] || '未知角色')

const lastLoginText = computed(() => {
  const time = userStore.userInfo?.lastLoginTime
  if (!time) return '首次登录'
  return String(time).replace('T', ' ').slice(0, 19)
})

const statCards = [
  { label: '项目总数', value: '--', icon: Files, color: '#1d63d1', bg: '#e8f0fd' },
  { label: '用例总数', value: '--', icon: Connection, color: '#0f6e56', bg: '#e2f4ee' },
  { label: '本月执行', value: '--', icon: Timer, color: '#854f0b', bg: '#fbf0dc' },
  { label: '通过率', value: '--', icon: DataAnalysis, color: '#993556', bg: '#fbe9f0' }
]

type StageType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

interface Stage {
  name: string
  desc: string
  period: string
  type: StageType
  hollow: boolean
}

const stages: Stage[] = [
  {
    name: '第一阶段：架构梳理 + 用户认证',
    desc: '前后端工程骨架、统一返回体、JWT 鉴权、登录注册页',
    period: '已完成',
    type: 'success',
    hollow: false
  },
  {
    name: '第二阶段：项目管理 + 接口与用例',
    desc: '项目 CRUD、环境管理、接口定义、用例增删改查',
    period: '待开始',
    type: 'primary',
    hollow: true
  },
  {
    name: '第三阶段：执行引擎 + 测试计划',
    desc: 'HTTP 执行器、断言校验、Cron 调度、并发执行',
    period: '待开始',
    type: 'info',
    hollow: true
  },
  {
    name: '第四阶段：报告中心 + 通知',
    desc: '执行明细、通过率趋势图、邮件与企业微信推送',
    period: '待开始',
    type: 'info',
    hollow: true
  },
  {
    name: '第五阶段：CI 集成 + 权限细化',
    desc: 'Jenkins/GitLab 触发、项目级 RBAC、性能优化',
    period: '待开始',
    type: 'info',
    hollow: true
  }
]

const apiList = [
  { method: 'POST', path: '/api/auth/register', desc: '用户注册' },
  { method: 'POST', path: '/api/auth/login', desc: '用户登录' },
  { method: 'POST', path: '/api/auth/logout', desc: '退出登录' },
  { method: 'GET', path: '/api/auth/captcha', desc: '图形验证码' },
  { method: 'GET', path: '/api/user/info', desc: '当前用户信息' },
  { method: 'GET', path: '/api/user/check-username', desc: '用户名查重' }
]

const techStack = ['Vue 3', 'TypeScript', 'Vite', 'Element Plus', 'Pinia', 'Spring Boot 3', 'MyBatis-Plus', 'MySQL', 'Redis', 'JWT']
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 欢迎横幅 ---------- */
.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  border-radius: 12px;
  color: #ffffff;
  background: linear-gradient(120deg, #0f2444 0%, #1d63d1 100%);
}

.welcome-text h2 {
  margin: 0 0 8px;
  font-size: 21px;
  font-weight: 600;
}

.welcome-text p {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.78);
}

.welcome-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.68);
}

.welcome-badge {
  text-align: center;
  padding: 14px 22px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.14);
  flex: none;
}

.badge-num {
  font-size: 22px;
  font-weight: 600;
}

.badge-label {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

/* ---------- 统计卡片 ---------- */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  flex: none;
}

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
}

.stat-label {
  margin-top: 3px;
  font-size: 13px;
  color: #6b7280;
}

/* ---------- 主体两栏 ---------- */
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f1f3;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: #1f2937;
}

.stage-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 4px;
}

.stage-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #f0f1f3;
}

.tech-label {
  font-size: 13px;
  color: #6b7280;
  margin-right: 2px;
}

@media (max-width: 1100px) {
  .stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .welcome {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome-badge {
    align-self: stretch;
  }
}
</style>
