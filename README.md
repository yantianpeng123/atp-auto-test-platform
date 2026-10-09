# ATP 自动化测试平台

前后端分离的接口自动化测试平台。前端 Vue3 + TypeScript + Vite，后端 Java 17 + Spring Boot 3 + MyBatis-Plus。

> 当前进度：**核心功能已基本完成**
> 用户认证、项目管理（含项目级 RBAC 成员）、基础数据、接口定义、组合组件、数据生成器、环境配置、用例编排、数据源、执行引擎、执行记录落库、测试计划与批次（Cron 调度）、报告中心、通知中心（钉钉 / 163 邮件 / 站内信）、CI 集成、仪表盘均已实现并推送到 `main`。
> 仍存在的少量缺口见文末「已知缺口 / 待办」。
>
> 完整进度、表结构、接口清单与已知问题见 → [`docs/HANDOVER.md`](docs/HANDOVER.md)

---

## 一、已实现功能

| 模块 | 能力 |
| --- | --- |
| 用户认证 | 注册、登录、登出、图形验证码、JWT 鉴权、角色体系（ADMIN/TESTER/VIEWER） |
| 项目管理 | 项目卡片列表、新增项目、登录后选择项目（localStorage 持久化）、切换项目 |
| 项目级 RBAC | 项目成员管理（OWNER/MAINTAINER/DEVELOPER/VIEWER）、`my-role`、成员增删改角色、保底至少 1 名 OWNER |
| 基础数据 | 工程 / 版本 / 模块三级级联；接口定义列表、手动新增、**Jar 包导入**（解析 Spring MVC 注解） |
| 组合组件 | 公共接口组件（强引用联动、多层嵌套、防环、变量透传、三阶段 pre/main/post 编排） |
| 数据生成器 | 表达式引擎（9 个函数）、生成器 CRUD、用例/组件内 `stepType=3` 生成变量写入变量池 |
| 环境配置 | 环境 CRUD：base_url、全局 header、数据库配置 |
| 用例管理 | 用例 CRUD、启停、**多步骤串行编排**、**HAR 包导入生成用例** |
| 数据源管理 | 数据源模板 CRUD、字段(key) 定义、数据项多行编辑（用例执行时按行替换变量） |
| 执行引擎 | HTTP 执行、变量解析、断言校验、多轮数据驱动；用例编辑页提供「调试运行」 |
| 执行记录 | 执行过程落库（`tb_execution` / `tb_execution_detail`），支持嵌套组合组件树还原 |
| 测试计划 | 测试计划 / 批次编排、Cron 调度（`PlanBatchScheduler`）、批次执行与完成事件 |
| 报告中心 | 执行报告、图表看板（`report.vue` / `reportCenter.vue` / `dashboard`） |
| 通知中心 | 通知渠道（钉钉 / 163 邮件 / 站内信）、通知规则、发送日志、顶栏消息中心与未读角标 |
| CI 集成 | `POST /api/ci/trigger`（X-CI-Token 免 JWT）、结果查询、配置管理、JUnit XML 报告、运行记录页 |
| 仪表盘 | 工作台概览 |

**核心数据层级**

```
项目 Project → 工程 Application → 版本 Version → 模块 Module → 接口 ApiDefinition
                                                              ↓
                                          用例 TestCase → 步骤 CaseStep（或引用组合组件 / 生成变量）
                                                              ↓
                                          测试计划 TestPlan → 批次 PlanBatch → 批次项 PlanBatchItem
```

---

## 二、目录结构

```
auto-test-platform/
├── docs/
│   ├── ARCHITECTURE.md      # 架构设计（技术选型、分层、表结构、接口规范）
│   ├── HANDOVER.md          # 交接文档：进度 / 接口清单 / 已知问题 / 下一步
│   ├── jenkins-setup.md     # Jenkins + GitLab Webhook 对接指南
│   └── thinks.md            # 每轮问题处理的思考过程与决策记录
├── backend/                 # Spring Boot 3
│   └── src/main/
│       ├── java/com/atp/
│       │   ├── common/      # 统一返回体、异常、工具类
│       │   ├── config/      # Security / 跨域 / MyBatis-Plus / Jackson 配置
│       │   ├── security/    # JWT 签发、认证过滤器、登录主体
│       │   └── module/      # 业务域，互不横向依赖
│       │       ├── user/       # 认证、用户
│       │       ├── project/    # 项目管理 + 项目级 RBAC 成员
│       │       ├── base/       # 工程/版本/模块、接口定义、组合组件、数据生成器
│       │       ├── env/        # 环境配置
│       │       ├── testcase/   # 用例管理、步骤
│       │       ├── dataset/    # 数据源模板
│       │       ├── execute/    # 执行引擎、执行记录落库、报告
│       │       ├── plan/       # 测试计划、批次、Cron 调度
│       │       ├── ci/         # CI 触发/结果/配置/JUnit 报告
│       │       └── notify/     # 通知渠道/规则/日志/站内信/钉钉/163 邮件
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql + notify_tables.sql
└── frontend/                # Vue 3
    └── src/
        ├── api/             # request.ts 拦截器 + 各业务 API
        ├── layout/          # 框架布局（侧边栏 + 顶栏 + 消息中心）
        ├── router/          # 路由与登录守卫
        ├── stores/          # Pinia 状态（user / project / tabs / generatorSelect）
        ├── views/           # login / register / dashboard / base / case / dataset /
        │                   #   env / execute / plan / ci / notify / project
        └── utils/           # 令牌存取
```

---

## 三、环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 本机 20 已验证通过 |
| Maven | 3.6.3+ | |
| Node.js | 18+ | 本机 22 已验证通过 |
| MySQL | 8.0 | 需要能本地连接 |
| Redis | 6+ | 验证码与令牌黑名单，**非强依赖** |

---

## 四、启动步骤

### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
mysql -u root -p atp < backend/src/main/resources/db/notify_tables.sql
```

脚本会创建 `atp` 库、全部业务表（含 `tb_notify_*`），并插入初始管理员账号。

> 若 MySQL 账号密码不是 `root/root`，请同步修改 `backend/src/main/resources/application.yml`。

### 2. 启动 Redis（可选）

```bash
redis-server
```

不启动时平台仍可运行：验证码自动隐藏，令牌黑名单降级为仅前端清除，登录/注册/鉴权全部正常。

### 3. 启动后端

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 20)   # macOS 需指定 JDK 20
mvn spring-boot:run
```

服务监听 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，`/api` 请求由 Vite 代理转发到 8080。

---

## 五、演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 平台管理员（ADMIN） |

登录页提供「一键填充」按钮。注册的新用户默认角色为 `TESTER`。

---

## 六、接口概览

统一返回体：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1756780800000
}
```

| 分组 | 路径前缀 | 主要接口 |
| --- | --- | --- |
| 认证 | `/api/auth` | register / login / logout / captcha |
| 用户 | `/api/user` | info / check-username |
| 项目 | `/api/project` | list / 新增 / my-role / members / 成员角色管理 |
| 基础数据 | `/api/base` | 工程·版本·模块 options 与新增、version/list、api/list、api、api/import（Jar） |
| 组合组件 | `/api/component` | 组合组件 CRUD、步骤编排、引用展开 |
| 生成器 | `/api/generator` | 数据生成器 CRUD、/preview、/functions |
| 用例 | `/api/case` | list / {id} / {caseId}/steps / 新增 / 修改 / 删除 / {id}/status / import（HAR） |
| 数据源 | `/api/dataset/template` | list / {id} / 新增 / 修改 / 删除 |
| 环境 | `/api/env` | list / 新增 / 修改 / 删除 |
| 执行 | `/api/execute/*` | case 调试执行、批次执行、报告 |
| 测试计划 | `/api/plan` / `/api/plan/batch/*` | 计划 CRUD、批次、调度、run/{runId} |
| CI | `/api/ci/*` | trigger / result / config / runs / report.xml |
| 通知 | `/api/notify/*` | channel / rule / log / messages / unread-count |

> 完整接口的方法、路径与说明见 `docs/HANDOVER.md`。

---

## 七、关键设计说明

**统一返回体与鉴权**
axios 请求拦截器自动注入 `Authorization: Bearer <token>`；响应拦截器在 `code === 200` 时拆包返回 `Result`，业务层再取 `.data`。401 自动清令牌并跳登录页。

**验证码为什么不是必填？**
验证码依赖 Redis。若强制校验，Redis 故障会直接堵死注册和登录。
因此策略是：前端传了 `captchaKey` + `captchaCode` 才校验，不传则跳过；前端挂载时尝试拉取验证码，成功才展示输入框，失败则静默隐藏。
生产环境如需强制开启，把 `AuthServiceImpl.verifyCaptcha()` 中的空值判断改为抛异常即可。

**密码安全**
BCrypt 加盐哈希，永不明文存储、永不通过接口返回。用户信息统一走 `UserInfoVO` 脱敏。

**令牌方案**
JWT 载荷含 `userId` / `username` / `role`，有效期 2 小时，密钥在 `application.yml` 的 `atp.jwt.secret`（生产环境务必改为环境变量注入）。登出时把令牌写入 Redis 黑名单，TTL 为其剩余有效期。

**步骤编排与变量传递**
用例由多个步骤串行执行，步骤间通过「响应变量名」传递参数：
`${varName.path}` 从 body 根解析、`${varName.header.x}` 取响应头、`${varName.status}` 取状态码。
数据源（数据项）按行做多轮驱动，每行数据替换一轮 `${变量}`。
组合组件支持多层嵌套（防环 + 深度守卫 `MAX_COMPONENT_DEPTH=10`），执行时递归展开；`stepType=3` 步骤由数据生成器产出变量写入共享变量池。

**HAR 导入**
解析 `log.entries[]`，剥离敏感 header（Authorization / Cookie / Proxy-Authorization），
按 `module_id + method + path` 去重（已存在跳过、不存在则新建接口，`name` 填空串、`source_flag` 标记来源），
按时间排序生成步骤并自动带状态码断言。

**通知与 CI**
通知中心支持钉钉机器人、163 邮件、站内信三种渠道，可按规则（如批次完成）触发，发送记录入 `tb_notify_log`，站内信入 `tb_notify_message` 并在顶栏展示未读角标。
CI 模块暴露 `POST /api/ci/trigger`（Jenkins 凭 `X-CI-Token` 免 JWT 调用），后端异步执行并返回 `runId`，Jenkins 轮询结果或拉取 JUnit XML 报告。

---

## 八、阶段完成情况

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 第一阶段 ~ 第二阶段 | 架构、鉴权、项目管理、基础数据、接口定义、环境、用例编排、数据源、执行引擎 | ✅ 已完成 |
| 第三阶段 | 执行记录落库（`tb_execution` / `tb_execution_detail`） | ✅ 已完成 |
| 第四阶段 | 报告中心、图表看板、测试计划与 Cron 调度 | ✅ 已完成 |
| 第五阶段 | CI 集成、项目级 RBAC、组合组件、数据生成器、通知中心 | ✅ 已完成 |

---

## 九、已知缺口 / 待办

以下功能/优化项**尚未完成**，按优先级排列：

1. **`regenEachRun=0`（跨轮固定值）语义未实现** —— `ExecuteServiceImpl` 中变量池每轮重建、永远每轮重算，`regen_each_run` 列已落库但不参与判断；UI 开关当前无效。
2. **测试计划删除未级联清理** —— `TestPlanServiceImpl.delete()` 仅逻辑删除计划本身，未清理 `tb_plan_batch` / `tb_plan_batch_item`，会残留孤儿数据（历史上"测试计划不存在"类报错的根因之一）。
3. **执行日志未落地** —— 按天滚动文件日志（`tb_execution_log`）方案尚未写代码，执行过程缺乏服务器侧可追溯日志。

另：部分前端文件（`api/notify.ts`、`notify/config.vue`、`BasicLayout.vue` 等）仍残留"后端未实现时兜底"的过期注释/分支，实际后端接口已可用，建议在清理时一并移除。

---

## 十、开发注意事项

1. **Vite 开发服务器会假死**：表现为端口能连上但 HTTP 无响应，列表还在而弹窗数据全空。
   确认方式：`nc -z 127.0.0.1 5173` 通、`curl http://127.0.0.1:5173/` 超时。
   处理：`lsof -ti:5173 | xargs kill -9` 后重新 `npm run dev` 并硬刷新浏览器。**改业务代码无效。**
2. **`keys` 是 MySQL 保留字**，手写 SQL 必须加反引号 `` `keys` ``。
3. **前端类型校验需加大内存**：`NODE_OPTIONS=--max-old-space-size=4096 npx vue-tsc --noEmit`，否则 OOM。
4. **LocalDateTime 序列化**统一 `yyyy-MM-dd HH:mm:ss`；`spring.jackson.date-format` 只对 `Date` 生效。
5. 命名区分：项目 = `project`、工程 = `application`、版本 = `version`、模块 = `module`、接口 = `api`。
6. 本人（开发者）手动编辑过的代码，后续接手者请勿擅自改动；需调整时先沟通确认。

详见 [`docs/HANDOVER.md`](docs/HANDOVER.md)。
