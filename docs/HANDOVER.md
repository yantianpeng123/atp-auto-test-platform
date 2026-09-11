# ATP 自动化测试平台 · 项目交接文档

> 更新时间：2026-09-04
> 定位：接口自动化 / 用例管理 / 持续回归平台（前后端分离）

---

## 一、项目概述

一个面向研发团队的**接口自动化测试平台**，核心价值：用例资产化、执行无人化、报告可视化、权限清晰化。

整体架构为**单体分层**（非微服务）：后端按 `module`（业务域）划分包，模块间不横向依赖；前端 SPA + 路由守卫 + Pinia 状态管理。

---

## 二、技术栈

### 后端（backend/）
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（本机 20） | Spring Boot 3 基线 |
| Spring Boot | 3.2.5 | 单体分层 |
| Spring Security | 6.x | 无状态 JWT 鉴权 |
| jjwt | 0.12.6 | JWT 签发/解析 |
| MyBatis-Plus | 3.5.7 | 单表 CRUD + 分页 + 自动填充 |
| MySQL | 8.0 | 主存储（JSON 字段存断言/请求体） |
| Redis | 7.x | 验证码 / token 黑名单（**非强依赖**） |
| Lombok / Hutool | latest | 样板代码 / 工具 |

### 前端（frontend/）
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Vue | 3.5 | Composition API + `<script setup>` |
| TypeScript | 5.6 | 类型与后端 VO/DTO 对齐 |
| Vite | 5.4 | 构建 + `/api` 代理 |
| Element Plus | 2.8 | UI 组件 |
| Pinia | 2.2 | 状态管理 |
| Vue Router | 4.4 | 路由守卫 |
| Axios | 1.7 | 请求/响应拦截器 |

---

## 三、环境与启动

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| MySQL | 3306 | 库 `atp`，账号 `root/root`，执行 `backend/src/main/resources/db/schema.sql` |
| Redis | 6379 | 可选，不启动也可运行（验证码降级隐藏） |
| 后端 | 8080 | `cd backend && mvn spring-boot:run`（本机需 `export JAVA_HOME=$(/usr/libexec/java_home -v 20)`） |
| 前端 | 5173 | `cd frontend && npm run dev`，`/api` 代理到 8080 |

初始账号：`admin / admin123`（角色 ADMIN）；注册新用户默认 TESTER。

---

## 四、项目结构

### 4.1 后端（`backend/src/main/java/com/atp/`）

```
AtpApplication.java              # 启动类（@MapperScan 扫描 module.**.mapper）
common/
  result/     Result.java        # 统一返回体 {code,message,data,timestamp}
              ResultCode.java    # 错误码枚举（分段）
  exception/  BizException.java  # 业务异常
              GlobalExceptionHandler.java
  util/       IpUtils.java
config/
  SecurityConfig.java            # 无状态 JWT + 白名单
  CorsConfig.java                # 跨域
  MybatisPlusConfig.java         # 分页插件 + createTime/updateTime 自动填充
  JacksonConfig.java             # LocalDateTime 统一 yyyy-MM-dd HH:mm:ss
security/
  JwtTokenProvider.java          # 签发/解析 JWT（HS256，载荷 userId/username/role）
  JwtAuthenticationFilter.java   # 每请求解析 token + Redis 黑名单校验
  UserPrincipal.java             # 登录主体（实现 UserDetails）
module/                          # 业务域，互不横向依赖
  user/                          # 用户模块（entity/mapper/service/controller/dto/vo）
  project/                       # 项目模块
  base/                          # 基础数据模块（工程/版本/模块/接口定义）
    util/JarApiParser.java       # Jar 包 Spring MVC 注解解析器
```

**分层约定**（每个 module 内）：
- `controller`：只做参数校验（`@Valid`）与结果装配
- `service` / `service/impl`：业务编排、事务边界（`@Transactional` 只在此层）
- `mapper`：只做数据访问
- `entity`：与表一一对应（`@TableName` / `@TableId` / `@TableLogic` / `@TableField(fill)`）
- `dto`：接收前端入参（带校验注解）
- `vo`：返回前端出参（脱敏）

### 4.2 前端（`frontend/src/`）

```
api/        request.ts   # axios 实例 + 请求/响应拦截器（token 注入、code 拆包、401 跳登录）
            auth.ts      # 认证接口
            project.ts   # 项目接口
            base.ts      # 工程/版本/模块/接口 接口
            types.ts     # 与后端 VO/DTO 对齐的类型
router/     index.ts     # 路由表 + 登录守卫 + 项目选择守卫
stores/     user.ts      # 登录态（token/userInfo）
            project.ts   # 当前项目（localStorage 持久化）
layout/     BasicLayout.vue  # 主布局（侧边栏 + 顶栏 + 当前项目展示）
views/
  login/    注册/登录
  project/select.vue     # 项目选择页（卡片，管理员可新增）
  dashboard/             # 工作台
  base/version/          # 工程版本信息（工程/版本/模块级联 + 新增弹框）
  base/api/              # 接口列表（级联查询 + 手动新增弹框 + Jar 导入弹框）
utils/      auth.ts / token.ts  # token 存取（token.ts 为遗留冗余）
```

---

## 五、前后端关系（调用链）

**认证链路**：
登录 → `POST /api/auth/login` 返回 JWT → 前端存 `localStorage.atp_token` → axios 请求拦截器自动带 `Authorization: Bearer <token>` → 后端 `JwtAuthenticationFilter` 解析并构建 `SecurityContext` → 白名单外接口须鉴权。

**业务链路（核心数据层级）**：
```
项目 Project → 工程 Application → 版本 Version → 模块 Module → 接口 ApiDefinition
```

**关键约定**：
- 前端 axios 响应拦截器在 `code === 200` 时返回统一响应体 `Result`，业务层再取 `.data`（即 `res.data` 是业务数据）。
- 下拉选项接口：`getProjectOptions(projectId)`（按项目过滤工程）、`getVersionOptions(applicationId)`（按工程过滤版本）、`getModuleOptions(versionId)`（按版本过滤模块）。
- 列表/联查接口通过 `@Select` 注解写 JOIN SQL（如接口列表：`tb_api_definition → module → version → application`）。
- 当前项目存前端 `localStorage.atp_current_project`，路由守卫保证未选项目时先跳 `/project/select`。

---

## 六、数据库设计（`db/schema.sql`）

### 已实现表

| 表 | 用途 | 关联 |
| --- | --- | --- |
| `sys_user` | 用户 | 角色 ADMIN/TESTER/VIEWER |
| `tb_project` | 项目 | 含 team（团队）、owner_id（负责人） |
| `tb_application` | 工程 | `project_id` → tb_project |
| `tb_application_version` | 版本 | `application_id` → tb_application |
| `tb_application_module` | 模块 | `version_id` → tb_application_version |
| `tb_api_definition` | 接口定义 | `module_id` → tb_application_module；含 name/method/path/headers/body |

### 预留未实现表

| 表 | 用途 |
| --- | --- |
| `tb_test_env` | 环境（域名变量、全局 header、数据库配置） |
| `tb_test_case` | 用例（关联接口 + 断言 JSON + 前置脚本） |
| `tb_test_plan` | 测试计划（关联用例 + cron） |
| `tb_execution` | 执行记录 |
| `tb_execution_detail` | 执行明细 |

### 主键约定
- **以后新建表用 `IdType.AUTO`（自增）+ 表加 `AUTO_INCREMENT`**。
- 早期表（`sys_user` 等）用雪花 ID；`tb_application*`、`tb_api_definition` 已改为自增。
- 全局 `id-type: assign_id`，实体 `@TableId(type=IdType.AUTO)` 优先级更高。

---

## 七、当前进度

### 已完成

| 模块 | 功能 |
| --- | --- |
| 用户认证 | 注册、登录、登出、图形验证码、JWT 鉴权、角色体系 |
| 项目管理 | 项目卡片列表、管理员新增项目、登录后选择项目（localStorage 持久化）、切换项目 |
| 基础数据 | 工程/版本/模块 的级联查询与新增（弹框三级联动） |
| 接口定义 | 接口列表级联查询（工程/版本/模块 + 名称/路径）、手动新增（弹框）、Jar 包导入（Spring MVC 注解解析：`@GetMapping` 等提取真实路径和请求方式） |

### 已完成接口清单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/captcha` | 验证码 |
| GET | `/api/user/info` | 当前用户 |
| GET | `/api/user/check-username` | 用户名查重 |
| GET | `/api/project/list` | 我的项目 |
| POST | `/api/project` | 新增项目（仅 ADMIN） |
| GET | `/api/base/project/options` | 工程下拉（按项目） |
| GET | `/api/base/version/options` | 版本下拉（按工程） |
| GET | `/api/base/module/options` | 模块下拉（按版本） |
| POST | `/api/base/project` | 新增工程 |
| POST | `/api/base/version` | 新增版本 |
| POST | `/api/base/module` | 新增模块 |
| GET | `/api/base/version/list` | 工程版本信息分页 |
| GET | `/api/base/api/list` | 接口列表分页 |
| POST | `/api/base/api` | 新增接口 |
| POST | `/api/base/api/import` | Jar 包导入接口 |

---

## 八、下一步计划（按规划）

当前处于**第二阶段（中后段）**：项目管理、基础数据（工程/版本/模块）、接口定义已完成，**待补**：

1. **环境管理**（`tb_test_env`）：环境 CRUD，关联项目，维护域名变量/全局 header/数据库配置。
2. **用例管理**（`tb_test_case`）：用例 CRUD，关联接口 + 断言 JSON + 前置脚本（这是自动化的核心资产）。
3. **接口定义的参数/请求体模板**：完善 `tb_api_definition` 的 headers/body 编辑（目前仅列表/新增/导入）。

之后进入第三阶段：

| 阶段 | 内容 |
| --- | --- |
| 第三阶段 | HTTP 执行引擎、断言校验、测试计划、Cron 调度 |
| 第四阶段 | 报告中心、图表看板、邮件/企业微信通知 |
| 第五阶段 | CI 集成、项目级 RBAC、并发执行与性能优化 |

---

## 九、关键注意事项

1. **编译后端需 JDK**：本机 mvn 默认走 JDK8 会报「不支持发行版本 17」，须 `export JAVA_HOME=$(/usr/libexec/java_home -v 20)`。
2. **Redis 非强依赖**：不启动时验证码自动隐藏、token 黑名单降级，不影响登录/鉴权。
3. **文件上传限制**：已在 `application.yml` 配置 `spring.servlet.multipart` 为 100MB（Jar 包导入需要）。
4. **LocalDateTime 序列化**：统一 `yyyy-MM-dd HH:mm:ss`（`JacksonConfig`），`spring.jackson.date-format` 只对 `Date` 生效、对 `LocalDateTime` 无效。
5. **命名区分**：项目 = `project`、工程 = `application`、版本 = `version`、模块 = `module`、接口 = `api`；参数名 `projectId` 表示项目、`applicationId` 表示工程（避免混淆）。
6. **遗留冗余**：前端 `utils/token.ts` 与 `utils/auth.ts` 重复（实际用 `auth.ts`），可后续清理。
7. **开发协作**：本人（开发者）在工作过程中手动编辑过的代码，后续接手者请勿擅自改动；需调整时先沟通确认。
