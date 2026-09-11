# 自动化测试平台（ATP）架构设计文档

> Auto Test Platform · 前后端分离 · Vue3 + Java
> 版本：v0.1（第一阶段）　更新时间：2026-09-02

---

## 一、定位与目标

一个面向研发团队的**接口自动化 / 用例管理 / 持续回归**平台。核心价值：

| 目标 | 说明 |
| --- | --- |
| 用例资产化 | 接口定义、用例、断言、测试数据统一入库，可复用、可版本化 |
| 执行无人化 | 支持定时调度、CI 触发、一键回归，执行结果自动落库 |
| 报告可视化 | 通过率趋势、失败归因、耗时分布，直接推送到 IM |
| 权限清晰化 | 项目级 RBAC，谁建的用例、谁能执行、谁能改环境 |

---

## 二、技术选型

### 2.1 后端

| 选型 | 版本 | 理由 |
| --- | --- | --- |
| JDK | 17（本机 20 兼容） | Spring Boot 3 基线 |
| Spring Boot | 3.2.x | 生态成熟，Native/虚拟线程可平滑演进 |
| Spring Security | 6.x | 与 JWT 组合做无状态鉴权 |
| JWT（jjwt） | 0.12.x | 无状态 token，便于前后端分离与水平扩展 |
| MyBatis-Plus | 3.5.x | 单表 CRUD 零 XML，复杂查询仍可写 XML |
| MySQL | 8.0 | 主存储，JSON 字段存用例断言/请求体 |
| Redis | 7.x | token 黑名单、验证码、执行任务幂等锁 |
| Lombok / Hutool | latest | 减少样板代码 |
| Knife4j | 4.x | OpenAPI3 接口文档 |

> **为什么第一阶段是单体分层，不是微服务？**
> 自动化测试平台的核心复杂度在**执行引擎的可靠性**，不在服务拆分。过早微服务化会带来分布式事务、链路追踪、多套部署的负担，而团队规模没到那个量级。当前采用**单体 + 清晰模块边界**（按 package 隔离），未来若执行引擎需要独立扩容，可无损拆出 `atp-executor` 独立进程。

### 2.2 前端

| 选型 | 版本 | 理由 |
| --- | --- | --- |
| Vue | 3.4（Composition API + `<script setup>`） | 响应式与 TS 体验最佳 |
| TypeScript | 5.x | 接口字段多，类型即文档 |
| Vite | 5.x | 冷启动与 HMR 快 |
| Element Plus | 2.x | 后台组件齐全，中文生态好 |
| Pinia | 2.x | 轻量状态管理，替代 Vuex |
| Vue Router | 4.x | 路由守卫做登录拦截 |
| Axios | 1.x | 请求/响应拦截器统一处理 token 与错误 |

---

## 三、整体分层

```
┌──────────────────────────────────────────────────────┐
│  前端接入层   Vue3 控制台 · Knife4j 文档 · CI 钩子     │
├──────────────────────────────────────────────────────┤
│  网关与安全   Nginx 反代 · JWT + Spring Security       │
├──────────────────────────────────────────────────────┤
│  业务服务层   用户权限 项目管理 接口用例                │
│              测试计划 执行调度 报告中心                 │
├──────────────────────────────────────────────────────┤
│  执行引擎层   用例执行器 · 任务调度器 · 通知服务        │
├──────────────────────────────────────────────────────┤
│  数据中间件   MySQL · Redis · 对象存储 · 消息队列       │
└──────────────────────────────────────────────────────┘
```

---

## 四、后端工程结构

```
backend/
└── src/main/java/com/atp/
    ├── AtpApplication.java
    ├── common/                     # 通用能力（无业务）
    │   ├── result/Result.java          # 统一返回体
    │   ├── result/ResultCode.java      # 错误码枚举
    │   ├── exception/BizException.java # 业务异常
    │   ├── exception/GlobalExceptionHandler.java
    │   └── page/PageResult.java
    ├── config/                     # 配置类
    │   ├── SecurityConfig.java
    │   ├── CorsConfig.java
    │   ├── MybatisPlusConfig.java   # 分页插件 + 自动填充
    │   └── JacksonConfig.java
    ├── security/                   # 安全体系
    │   ├── JwtTokenProvider.java
    │   ├── JwtAuthenticationFilter.java
    │   └── UserPrincipal.java
    └── module/                     # 业务模块（按域划分，互相不横向依赖）
        ├── user/     entity/ mapper/ service/ controller/ dto/ vo/
        ├── project/                          # 第二阶段
        ├── apicase/                          # 第二阶段
        ├── plan/                             # 第三阶段
        ├── execute/                          # 第三阶段
        └── report/                           # 第四阶段
```

**分层约定**

```
Controller  →  只做参数校验与结果装配，不写业务逻辑
Service     →  业务编排、事务边界（@Transactional 只在此层）
Mapper      →  只做数据访问，不写业务判断
entity      →  与表一一对应
dto         →  接收前端入参（带校验注解）
vo          →  返回前端出参（脱敏，如密码永不出现在 VO 中）
```

---

## 五、前端工程结构

```
frontend/
└── src/
    ├── api/            # 接口封装，一个域一个文件
    │   ├── request.ts      # Axios 实例 + 拦截器
    │   ├── auth.ts
    │   └── types.ts        # 与后端 VO/DTO 对齐的类型
    ├── router/         # 路由表 + 全局前置守卫
    ├── stores/         # Pinia：user.ts / app.ts
    ├── views/          # 页面
    │   ├── login/index.vue
    │   ├── register/index.vue
    │   └── dashboard/index.vue
    ├── layout/         # 框架布局（侧边栏 + 顶栏）
    ├── components/     # 通用组件
    ├── utils/          # token 存取、校验规则
    └── styles/         # 全局变量与重置样式
```

---

## 六、数据库设计（第一阶段落库部分）

### 6.1 用户表 `sys_user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 雪花 ID |
| username | VARCHAR(50) UK | 登录账号，4-20 位 |
| password | VARCHAR(100) | BCrypt 密文，永不回传 |
| nickname | VARCHAR(50) | 显示名 |
| email | VARCHAR(100) UK | 邮箱，用于找回密码 |
| phone | VARCHAR(20) | 手机号，选填 |
| avatar | VARCHAR(255) | 头像 URL |
| status | TINYINT | 0 禁用 / 1 正常 |
| role | VARCHAR(20) | ADMIN / TESTER / VIEWER |
| last_login_ip | VARCHAR(50) | 最近登录 IP |
| last_login_time | DATETIME | 最近登录时间 |
| create_time / update_time | DATETIME | MyBatis-Plus 自动填充 |
| deleted | TINYINT | 逻辑删除 |

### 6.2 后续阶段预留（本期只建表不实现）

| 表 | 用途 |
| --- | --- |
| `tb_project` | 项目 |
| `tb_api_definition` | 接口定义（URL/方法/参数/请求体模板） |
| `tb_test_case` | 用例（关联接口 + 断言 JSON + 前置脚本） |
| `tb_test_env` | 环境（域名变量、全局 header、数据库配置） |
| `tb_test_plan` | 测试计划（关联用例集合 + cron 表达式） |
| `tb_execution` | 执行记录（触发方式、总数、通过数） |
| `tb_execution_detail` | 执行明细（单条用例的请求/响应/断言结果） |

---

## 七、鉴权设计

### 7.1 认证流程

```
注册/登录
   ↓
校验账号密码（BCrypt matches）
   ↓
签发 JWT（载荷：userId, username, role；有效期 2h）
   ↓
前端存 localStorage（key: atp_token），axios 请求头自动携带 Authorization: Bearer xxx
   ↓
JwtAuthenticationFilter 每次请求解析 token → 构建 Authentication → 存入 SecurityContext
   ↓
登出：token 写入 Redis 黑名单（剩余有效期 TTL），网关层拦截
```

### 7.2 安全要点

- 密码必须 BCrypt 加盐，**禁止 MD5**
- 登录失败 5 次锁定 10 分钟（Redis 计数）——第二阶段补
- 注册接口带图形验证码，防止批量注册——本期提供接口位
- JWT 密钥走配置文件，生产环境用环境变量注入
- 所有 `/api/**` 除白名单外必须携带 token

### 7.3 白名单

```
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/captcha
GET  /doc.html, /webjars/**, /v3/api-docs/**
```

---

## 八、接口规范

### 8.1 统一返回体

```json
{
  "code": 200,
  "message": "success",
  "data": { },
  "timestamp": 1756780800000
}
```

### 8.2 错误码分段

| 区间 | 含义 |
| --- | --- |
| 200 | 成功 |
| 400-499 | 客户端错误（400 参数错、401 未登录、403 无权限、404 不存在） |
| 1001-1999 | 用户模块业务错误 |
| 2001-2999 | 项目模块 |
| 3001-3999 | 用例模块 |
| 500 | 服务端异常 |

### 8.3 第一阶段接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录，返回 token |
| POST | `/api/auth/logout` | 登出，token 拉黑 |
| GET | `/api/auth/captcha` | 图形验证码（Base64） |
| GET | `/api/user/info` | 当前登录用户信息 |
| GET | `/api/user/check-username` | 用户名重复性校验 |

---

## 九、开发路线图

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 第一阶段 | 架构梳理 + 后端骨架 + 注册/登录页 + JWT 鉴权 | ✅ 进行中 |
| 第二阶段 | 项目管理 + 环境管理 + 接口定义 + 用例增删改查 | 待开始 |
| 第三阶段 | 用例执行引擎（HTTP 断言）+ 测试计划 + 定时调度 | 待开始 |
| 第四阶段 | 报告中心 + 图表看板 + 通知推送 | 待开始 |
| 第五阶段 | CI 集成 + 权限细化 + 并发执行 + 性能优化 | 待开始 |

---

## 十、本地运行依赖

| 组件 | 端口 | 备注 |
| --- | --- | --- |
| MySQL | 3306 | 数据库 `atp`，执行 `backend/src/main/resources/db/schema.sql` |
| Redis | 6379 | 无密码默认配置 |
| 后端 | 8080 | `mvn spring-boot:run` |
| 前端 | 5173 | `npm run dev`，已配置 `/api` 代理到 8080 |
