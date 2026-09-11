# ATP 自动化测试平台 · 项目交接文档

> 更新时间：2026-09-12
> 定位：接口自动化 / 用例编排 / 调试执行平台（前后端分离）
> 代码仓库：git@github.com:yantianpeng123/atp-auto-test-platform.git（分支 `main`）

---

## 一、项目概述

面向研发团队的**接口自动化测试平台**。核心价值：用例资产化、步骤串行编排、参数跨步骤传递、调试执行可视化。

整体为**单体分层架构**（非微服务）：后端按 `module`（业务域）划分包，模块间不横向依赖；前端 SPA + 路由守卫 + Pinia 状态管理。

---

## 二、技术栈

### 后端（`backend/`）
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17（本机 20 验证通过） | Spring Boot 3 基线 |
| Spring Boot | 3.2.5 | 单体分层 |
| Spring Security | 6.x | 无状态 JWT 鉴权 |
| jjwt | 0.12.6 | JWT 签发/解析（HS256） |
| MyBatis-Plus | 3.5.7 | CRUD + 分页 + 逻辑删除 |
| MySQL | 8.0 | 主存储（`atp` 库，账号 `root/root`） |
| Redis | 7.x | 验证码 / token 黑名单（**非强依赖**） |
| Lombok / Hutool | latest | 样板代码 / 工具 |

### 前端（`frontend/`）
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Vue | 3.5 | Composition API + `<script setup>` |
| TypeScript | 5.6 | 类型与后端 VO/DTO 对齐 |
| Vite | 5.4 | 构建 + `/api` 代理到 8080 |
| Element Plus | 2.8 | UI 组件 |
| Pinia | 2.2 | 状态管理 |
| Vue Router | 4.4 | 路由守卫 |
| Axios | 1.7 | 请求/响应拦截器 |

---

## 三、环境与启动

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| MySQL | 3306 | 库 `atp`，账号 `root/root`，脚本 `backend/src/main/resources/db/schema.sql` |
| Redis | 6379 | 可选，不启动也可运行（验证码降级隐藏） |
| 后端 | 8080 | `cd backend && mvn spring-boot:run`（本机须 `export JAVA_HOME=$(/usr/libexec/java_home -v 20)`） |
| 前端 | 5173 | `cd frontend && npm run dev` |

初始账号：`admin / admin123`（ADMIN）；注册新用户默认 TESTER。

**前端构建校验**：`NODE_OPTIONS=--max-old-space-size=4096 npx vue-tsc --noEmit`（不加内存参数会 OOM）。

---

## 四、当前进度总览

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 第一阶段 | 架构 + JWT 鉴权 + 登录注册 + 项目管理 | ✅ 完成 |
| 第二阶段 | 基础数据（工程/版本/模块）+ 接口定义 | ✅ 完成 |
| **第三阶段** | **用例管理 + 步骤编排 + 执行引擎 + 数据源** | **🔶 主体完成，报告/计划未做** |
| 第四阶段 | 报告中心、图表看板、通知 | ⬜ 未开始 |
| 第五阶段 | CI 集成、项目级 RBAC、并发执行 | ⬜ 未开始 |

### 4.1 已完成模块

| 模块 | 后端 | 前端 | 说明 |
| --- | --- | --- | --- |
| 用户认证 | ✅ | ✅ | 注册、登录、登出、图形验证码、JWT、角色体系 |
| 项目管理 | ✅ | ✅ | 项目列表、新增、选择（localStorage 持久化）、切换 |
| 基础数据 | ✅ | ✅ | 工程/版本/模块 级联查询与新增；接口列表、手动新增、Jar 包导入 |
| 环境配置 | ✅ | ✅ | 环境 CRUD（base_url / 全局 header / 数据库配置） |
| 用例管理 | ✅ | ✅ | 用例 CRUD、状态启停、**多步骤串行编排**、**HAR 导入** |
| 数据源管理 | ✅ | ✅ | 数据源模板 CRUD、字段(key) 定义、**数据项多行编辑** |
| 执行引擎 | ✅ | ✅（调试入口） | HTTP 执行、变量解析、断言引擎、多轮数据驱动 |

### 4.2 尚未实现

- **测试计划**（`tb_test_plan` 表已建，后端模块与页面均无）
- **执行记录落库**（`tb_execution` / `tb_execution_detail` 表已建，**无 Entity/Mapper**，执行结果仅返回 VO 不持久化）
- **报告中心 / 图表看板**
- **Cron 调度**
- **CI 集成、项目级 RBAC**

前端菜单中「测试计划」「报告中心」为 `disabled` 占位态。

---

## 五、项目结构

### 5.1 后端（`backend/src/main/java/com/atp/`）

```
common/
  result/     Result.java        # 统一返回体 {code,message,data,timestamp}
              ResultCode.java    # 错误码枚举
  exception/  BizException.java / GlobalExceptionHandler.java
  util/       IpUtils.java
config/
  SecurityConfig.java            # 无状态 JWT + 白名单
  CorsConfig.java / MybatisPlusConfig.java / JacksonConfig.java
security/
  JwtTokenProvider.java          # HS256，载荷 userId/username/role
  JwtAuthenticationFilter.java   # 每请求解析 token + Redis 黑名单校验
  UserPrincipal.java
module/
  user/       (13)  用户模块
  project/    (7)   项目模块
  base/       (27)  基础数据：工程/版本/模块/接口定义 + JarApiParser
  env/        (8)   环境配置
  testcase/   (19)  用例 + 步骤 + HAR 导入
  dataset/    (7)   数据源模板与数据项
  execute/    (13)  执行引擎（HTTP/断言/变量解析）
```

**分层约定**（每个 module 内）：`controller`（参数校验 + 结果装配）→ `service`/`impl`（业务编排、`@Transactional` 只在此层）→ `mapper`（数据访问）；另有 `entity`（与表一一对应）、`dto`（入参）、`vo`（出参）。

### 5.2 前端（`frontend/src/`）

```
api/      request.ts   # axios 拦截器（token 注入、code 拆包、401 跳登录）
          auth.ts / user.ts / project.ts / base.ts
          case.ts / dataset.ts / env.ts / execute.ts
          types.ts     # 与后端 VO/DTO 对齐的类型
router/   index.ts     # 路由表 + 登录守卫 + 项目选择守卫
stores/   user.ts / project.ts / tabs.ts
layout/   BasicLayout.vue   # 侧边栏 + 顶栏 + 当前项目
views/
  login/ register/       登录 / 注册
  project/select.vue     项目选择
  dashboard/             工作台
  base/version/          工程版本管理
  base/api/              接口列表（含 Jar 导入）
  env/                   环境配置
  case/index.vue         用例列表（含 **HAR 导入**）
  case/edit.vue          用例编辑（**多步骤串行编排**，约 1600 行）
  dataset/               数据源管理（列表 + 数据项 + 编辑）
  dataset/components/    DatasetItemsDialog / DatasetFormDialog / DatasetSelectDialog
utils/    auth.ts        # token 存取（localStorage key: atp_token）
```

---

## 六、数据库设计

### 已建表（15 张）

| 表 | 用途 | 状态 |
| --- | --- | --- |
| `sys_user` | 用户 | ✅ 使用中 |
| `tb_project` | 项目 | ✅ 使用中 |
| `tb_application` / `tb_application_version` / `tb_application_module` | 工程 / 版本 / 模块 | ✅ 使用中 |
| `tb_api_definition` | 接口定义 | ✅ 使用中 |
| `tb_test_case` | 用例 | ✅ 使用中 |
| `tb_case_step` | 用例步骤（含 `response_var`、`request_override`、`assertions`） | ✅ 使用中 |
| `tb_dataset_template` / `tb_dataset_item` | 数据源模板 / 数据项 | ✅ 使用中 |
| `tb_test_env` | 环境配置 | ✅ 使用中 |
| `tb_execution` / `tb_execution_detail` | 执行记录 / 明细 | ⬜ **已建表，代码未实现** |
| `tb_test_plan` | 测试计划 | ⬜ **已建表，代码未实现** |

### 主键约定
- **新建表一律用 `IdType.AUTO`（自增）+ 表加 `AUTO_INCREMENT`**。
- 早期表（`sys_user` 等）为雪花 ID；`tb_application*`、`tb_api_definition` 已改为自增。
- 全局 `id-type: assign_id`，实体上 `@TableId(type=IdType.AUTO)` 优先级更高。
- `keys` 是 MySQL 保留字，写 SQL 时必须用反引号 `` `keys` ``。

---

## 七、接口清单

### 认证与用户
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/captcha` | 验证码 |
| GET | `/api/user/info` | 当前用户 |
| GET | `/api/user/check-username` | 用户名查重 |

### 项目
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/project/list` | 我的项目 |
| POST | `/api/project` | 新增项目（仅 ADMIN） |

### 基础数据
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/base/project/options` | 工程下拉（按项目） |
| GET | `/api/base/version/options` | 版本下拉（按工程） |
| GET | `/api/base/module/options` | 模块下拉（按版本） |
| POST | `/api/base/project` / `/version` / `/module` | 新增工程/版本/模块 |
| GET | `/api/base/version/list` | 工程版本信息分页 |
| GET | `/api/base/api/list` | 接口列表分页 |
| POST | `/api/base/api` | 新增接口 |
| POST | `/api/base/api/import` | Jar 包导入接口 |

### 用例
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/case/list` | 用例列表分页 |
| GET | `/api/case/{id}` | 用例详情 |
| GET | `/api/case/{caseId}/steps` | 用例步骤 |
| POST | `/api/case` | 新增用例 |
| PUT | `/api/case` | 修改用例 |
| DELETE | `/api/case/{id}` | 删除用例 |
| PUT | `/api/case/{id}/status` | 启停用例 |
| POST | `/api/case/import` | **HAR 包导入生成用例**（multipart） |

### 数据源
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/dataset/template/list` | 模板分页 |
| GET | `/api/dataset/template/{id}` | 模板详情（含字段与数据项） |
| POST | `/api/dataset/template` | 新增模板 |
| PUT | `/api/dataset/template` | 修改模板（数据项整体替换） |
| DELETE | `/api/dataset/template/{id}` | 删除模板（级联删除数据项） |

### 环境
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/env/list` | 环境列表 |
| POST | `/api/env` | 新增环境 |
| PUT | `/api/env` | 修改环境 |
| DELETE | `/api/env/{id}` | 删除环境 |

### 执行
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/execute/case/{caseId}` | 调试执行用例（body: `{envId}`） |

---

## 八、核心设计约定

### 8.1 HAR 导入（`CaseServiceImpl.importHar`）
1. 解析 HAR 的 `log.entries[]`，URL 去掉协议/域名/端口/query，只留 path。
2. **敏感 header 在解析入口即剥离**（Authorization / Cookie / Proxy-Authorization，大小写不敏感）。
3. 按 `module_id + method + path` 预加载已有接口到 Map 去重：
   - 已存在 → 跳过；
   - 不存在 → 新增到 `tb_api_definition`，`name` 填空串 `""`，`source_flag` 固定为 `"har包导入"`。
4. 生成一个 `TestCase`（P2、启用）+ N 个 `CaseStep`，按 `startedDateTime` 排序，默认带状态码断言 `[{"type":"status","expected":N}]`。
5. 整体 `@Transactional`。

### 8.2 步骤编排与变量传递
- 用例 = 多个步骤串行执行，步骤间通过「**响应变量名**」传递参数。
- 引用语法：`${varName.path}` 从 body 根解析；`${varName.header.x}` 取响应头；`${varName.status}` 取状态码。
- 「参数提取 extractors」已被响应变量名完整覆盖，前端输入框已移除，后端字段保留兼容旧数据。
- 执行引擎按数据源行数做**多轮执行**（`loadDatasetRows`），每轮用一行数据替换 `${变量}`。

### 8.3 前端/后端字段映射
- 前端用例表单用 `caseName`，在 `api/case.ts` 发送时映射为后端 `name`；后端与数据库不改。
- axios 响应拦截器在 `code === 200` 时返回 `Result`，业务层再取 `.data`。
- 当前项目存 `localStorage.atp_current_project`，token 存 `localStorage.atp_token`。

### 8.4 命名区分
项目 = `project`、工程 = `application`、版本 = `version`、模块 = `module`、接口 = `api`。
参数名 `projectId` 指项目、`applicationId` 指工程，避免混淆。

---

## 九、已知问题与待办

### 9.1 待修复缺陷
| 优先级 | 问题 | 位置 |
| --- | --- | --- |
| 高 | `DatasetFormDialog` 的 watch 先 `await loadCaseOptions()` 再给 form 赋值；请求挂起时**表单完全不回显** | `views/dataset/components/DatasetFormDialog.vue` |
| 高 | 编辑弹窗的「关联用例」依赖 `projectStore.currentProject`；数据源管理页无工程上下文，且用例可能属于其他工程 → 下拉为空、只显示裸 `caseId` | 同上 |
| 中 | `DatasetItemsDialog.loadItems()` 无 `catch`，接口失败静默变空表、无任何提示 | `views/dataset/components/DatasetItemsDialog.vue` |
| 中 | 数据源管理页缺少「新增数据源」入口，只能从用例编辑页创建 | `views/dataset/index.vue` |
| 低 | `DatasetItemsDialog` 内 `loading` 变量声明但未绑定 `v-loading` | 同上 |
| 低 | 前端 `utils/token.ts` 与 `utils/auth.ts` 重复（实际用 `auth.ts`） | `frontend/src/utils/` |
| 低 | 侧边栏底部标签仍显示「第一阶段」 | `layout/BasicLayout.vue` |
| 低 | `README.md` 进度描述停留在第一阶段，未同步 | `README.md` |

### 9.2 功能缺口
- 执行结果不落库：`tb_execution` / `tb_execution_detail` 已建表，缺 Entity/Mapper/Service。
- 测试计划、Cron 调度、报告中心、图表看板均未实现。
- 用例列表页无「执行」入口，调试仅能在用例编辑页进行。

---

## 十、开发环境注意事项（重要）

1. **编译后端需 JDK 20**：`export JAVA_HOME=$(/usr/libexec/java_home -v 20)`，否则报「不支持发行版本 17」。
2. **Vite 开发服务器会假死**（2026-09-12 已复现一次）：
   - 现象：端口能连上（TCP 握手成功）但 HTTP 无任何响应；页面列表还在（卡死前加载的），弹窗能打开但**所有新请求全部挂起**，表现为「数据不回显」。
   - 排查：`nc -z 127.0.0.1 5173` 通 + `curl http://127.0.0.1:5173/` 超时即可确认。
   - 处理：`lsof -ti:5173 | xargs kill -9` 后重新 `npm run dev`，浏览器硬刷新（⌘⇧R）。**改代码无效，别往业务代码里找原因。**
3. **Redis 非强依赖**：不启动时验证码自动隐藏、token 黑名单降级。
4. **文件上传限制**：`spring.servlet.multipart` 已配 100MB（Jar / HAR 导入需要）。
5. **LocalDateTime 序列化**：统一 `yyyy-MM-dd HH:mm:ss`（`JacksonConfig`）；`spring.jackson.date-format` 只对 `Date` 生效，对 `LocalDateTime` 无效。
6. **`keys` 是 MySQL 保留字**，手写 SQL 必须加反引号。
7. **开发协作**：本人（开发者）在工作过程中手动编辑过的代码，后续接手者请勿擅自改动；需调整时先沟通确认。

---

## 十一、下一步建议（按优先级）

1. **执行记录落库**：补 `Execution` / `ExecutionDetail` Entity + Mapper，执行后写 `tb_execution` / `tb_execution_detail`，为报告中心打基础。
2. **报告中心**：基于落库数据做执行明细页 + 通过率图表。
3. **测试计划 + Cron 调度**：`tb_test_plan` 已建表，补模块与页面。
4. **修复数据源弹窗缺陷**（第九节 9.1 高优先级两项）。
5. **用例列表页补「执行」入口**，并支持选择环境。
