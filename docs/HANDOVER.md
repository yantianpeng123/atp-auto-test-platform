# ATP 自动化测试平台 · 项目交接文档

> 更新时间：2026-09-16（本轮：数据生成器前端全链路 + 组合组件交互重构 + 3 项缺陷修复）
> 定位：接口自动化 / 用例编排 / 调试执行平台（前后端分离）
> 代码仓库：git@github.com:yantianpeng123/atp-auto-test-platform.git（分支 `main`）
> 当前 HEAD：`d46647c`，与 `origin/main` 同步，工作区干净

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
| 第三阶段 | **用例管理 + 步骤编排 + 执行引擎 + 数据源 + 测试计划** | ✅ 完成 |
| 第四阶段 | 定时任务批次、**报告中心（前后端）**、图表看板、通知 | ✅ 批次/报告前后端完成；图表看板/通知未开始 |
| 第五阶段 | CI 集成、项目级 RBAC、并发执行 | ⬜ 未开始 |

### 4.0 本轮进度快照（2026-09-16）

本轮（commit `4ffd93a` → `d46647c`，共 8 次提交）聚焦**数据生成器**与**组合组件**两块，全部为**前端改动，后端未动**：

| # | 事项 | 类型 | commit | 状态 |
| --- | --- | --- | --- | --- |
| 1 | 数据生成器设计文档 + 前端全链路（mock 运行时） | 新增 | `4ffd93a` / `39cdfb3` | ✅ |
| 2 | 生成器管理并入「组合组件」页签 + 卡片直弹窗（保留选择已有） | 重构 | `3439a70` | ✅ |
| 3 | 文档同步（设计文档 §9、HANDOVER 4.1/9.2） | 文档 | `324cff8` | ✅ |
| 4 | 修复：选择接口弹窗下拉为空（改为打开时显式加载） | 缺陷 | `1bd73cd` | ✅ |
| 5 | 修复：接口数据未回显到步骤详情（首轮，方向有误） | 缺陷 | `e66ffc7` | ⚠️ 未命中根因 |
| 6 | 修复：同上，**定位真实根因**（`@click` 误传 MouseEvent） | 缺陷 | `d46647c` | ✅ |
| 7 | 页面改名：「接口组件」页首个页签标签「组合组件」→「接口组件」（仅 1 行） | 样式 | `3d115e2` | ✅ |

**一句话结论**：数据生成器前端已可独立跑通（不依赖后端），后端 `tb_data_generator` + `GeneratorEngine` + `step_type=3` 执行分支**仍未实现**，是当前最大的功能缺口。

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
| 测试计划 | ✅ | ✅ | 计划 CRUD、关联用例、Cron 调度、启停、手动执行；项目隔离 + 同名校验 |
| 组合组件（菜单「接口组件」） | ✅ | ✅ | `tb_api_component`/`_step` 两张表；组件 CRUD（`/api/component/**`）；步骤支持单接口(step_type=1)/嵌套组件(2)/生成变量(3，前端已落地、后端未持久化)；执行引擎 `expandSteps/expandOne` 递归展开、防环(深度10)、`component_id/parent_step_id/nest_level` 落 `tb_execution_detail`；前端编辑页支持调试运行（含结果抽屉）。**09-16 起为双页签结构**：列表态用 `el-tabs` 分「接口组件」/「数据生成器」两个页签（`3d115e2` 将首个页签标签由「组合组件」改为「接口组件」） |
| 定时任务批次 | ✅ | ✅ | 批次 CRUD、关联多计划、批次级 Cron 轮询调度、并行/串行策略、立即执行 |
| 执行记录落库 | ✅ | ✅（抽屉/报告） | `tb_execution`/`_detail`/`_assertion` 持久化，历史查询接口 |
| 执行报告/报告中心 | ✅ | ✅ | 报告详情（轮次分组/仅看失败/JSON 美化/重试）+ 报告列表，后端 `GET /api/execute/{executionId}` + `/list` 已于 09-14 补齐，前端已接真实接口 |
| 数据生成器（前端） | ⬜ | ✅ | 前端全链路已实现：生成器管理并入「接口组件」页的「数据生成器」页签（`component/index.vue` 用 `el-tabs` 嵌入改造后的 `generator/index.vue`）；新增 `GeneratorSelectDialog.vue` 选择已有生成器；组合组件「生成变量」卡片直接弹出（新建 `GeneratorFormDialog` / 选择已有）写入 `stepType=3` 步骤；客户端生成运行时 `api/generator.ts`（mock，含 GB11643 身份证校验）；用例扩展「生成变量（stepType=3）」接入、表格「生成变量」标签均已落地；已移除隐藏路由 `/base/generator` 与跨页回传 store。已用真实浏览器（admin/admin123，项目 3 模块 "3mm"）跑通「新增组件 → 选模块 → 添加步骤 → 选接口 → 确定」全链路。**后端 `tb_data_generator` 表 + CRUD + `GeneratorEngine` + `step_type=3` 执行分支待实现**（详见 `docs/data-generator-and-expression-design.md` §9） |

### 4.2 尚未实现

- **执行报告"重跑"精度**：报告页"失败重试"当前降级为整用例重跑（调 `executeCase`），精确单步重跑需执行引擎后续支持。
- **批次执行异步化**：`executeBatch` 当前为同步执行（HTTP 返回即跑完），长批次可能超时，建议改"立即返回 runId + 后台异步"。
- **图表看板、通知、CI 集成、项目级 RBAC、并发执行** 均未开始。

前端菜单现状：「组合组件」「测试计划」「定时任务」「报告中心」均已启用（非 disabled）。

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
  execute/    (16)  执行引擎（HTTP/断言/变量解析）+ Execution/ExecutionDetail/ExecutionAssertion 落库
  plan/       (28)  测试计划（TestPlan/TestPlanCase）+ 定时任务批次（PlanBatch* 4 表/4 Mapper/VO/Service）+ PlanScheduler + PlanBatchScheduler
```

**分层约定**（每个 module 内）：`controller`（参数校验 + 结果装配）→ `service`/`impl`（业务编排、`@Transactional` 只在此层）→ `mapper`（数据访问）；另有 `entity`（与表一一对应）、`dto`（入参）、`vo`（出参）。

### 5.2 前端（`frontend/src/`）

```
api/      request.ts   # axios 拦截器（token 注入、code 拆包、401 跳登录）
          auth.ts / user.ts / project.ts / base.ts
          case.ts / dataset.ts / env.ts / execute.ts
          component.ts / plan.ts / planBatch.ts
          generator.ts  # **数据生成器**：客户端生成运行时（mock，待替换为后端 HTTP 调用）
          types.ts      # 与后端 VO/DTO 对齐的类型
router/   index.ts     # 路由表 + 登录守卫 + 项目选择守卫
stores/   user.ts / project.ts / tabs.ts
layout/   BasicLayout.vue   # 侧边栏 + 顶栏 + 当前项目
views/
  login/ register/       登录 / 注册
  project/select.vue     项目选择
  dashboard/             工作台
  base/version/          工程版本管理
  base/api/              接口列表（含 Jar 导入）
  base/component/
    index.vue            **接口组件**（原「组合组件」）：列表态 `el-tabs` 双页签
                         —— 「接口组件」/「数据生成器」（后者内嵌 generator/index.vue）
    edit.vue             组件编辑器（非路由，由 index.vue 的 viewMode 切换挂载）
                         支持 stepType 1 单接口 / 2 嵌套组件 / 3 生成变量
  base/generator/
    index.vue            生成器管理（可内嵌页签，也可独立使用）
    GeneratorFormDialog.vue   新建/编辑生成器（类型动态参数 + 实时预览）
    GeneratorSelectDialog.vue 选择已有生成器
  env/                   环境配置
  case/index.vue         用例列表（含 **HAR 导入**）
  case/edit.vue          用例编辑（**多步骤串行编排**，约 1600 行）
  case/ExtensionTable.vue 前置/后置扩展步骤表格（支持 stepType 2/3 标签）
  dataset/               数据源管理（列表 + 数据项 + 编辑）
  dataset/components/    DatasetItemsDialog / DatasetFormDialog / DatasetSelectDialog
  plan/index.vue         测试计划列表（CRUD/关联用例/Cron/启停/执行）
  plan/batch/index.vue   定时任务批次列表
  plan/batch/detail.vue  批次详情（执行看板/运行历史/计划排序）
  execute/report.vue     执行报告详情（V2：轮次分组/仅看失败/JSON 美化/重试）
  execute/reportCenter.vue 报告中心列表（搜索/筛选/分页）
components/
  JsonTree.vue           JSON 树形美化（key/value 着色 + 折叠）
utils/    auth.ts        # token 存取（localStorage key: atp_token）
```

---

## 六、数据库设计

### 已建表（22 张）

| 表 | 用途 | 状态 |
| --- | --- | --- |
| `sys_user` | 用户 | ✅ 使用中 |
| `tb_project` | 项目 | ✅ 使用中 |
| `tb_application` / `tb_application_version` / `tb_application_module` | 工程 / 版本 / 模块 | ✅ 使用中 |
| `tb_api_definition` | 接口定义 | ✅ 使用中 |
| `tb_test_case` | 用例 | ✅ 使用中 |
| `tb_case_step` | 用例步骤（含 `response_var`、`request_override`、`assertions`） | ✅ 使用中 |
| `tb_api_component` / `tb_api_component_step` | 组合组件 / 组件步骤（`step_type` 1单接口/2嵌套组件，防环；复用 `CaseStepVO` 结构展开） | ✅ 使用中 |
| `tb_dataset_template` / `tb_dataset_item` | 数据源模板 / 数据项 | ✅ 使用中 |
| `tb_test_env` | 环境配置 | ✅ 使用中 |
| `tb_test_plan` / `tb_plan_case` | 测试计划 / 计划关联用例 | ✅ 使用中 |
| `tb_execution` / `tb_execution_detail` / `tb_execution_assertion` | 执行记录 / 明细 / 断言 | ✅ 使用中（execute 模块落库） |
| `tb_plan_batch` / `tb_plan_batch_item` | 批次 / 批次关联计划 | ✅ 使用中 |
| `tb_plan_batch_run` / `tb_plan_batch_run_item` | 批次运行实例 / 运行项 | ✅ 使用中 |

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

### 组合组件
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/component/list` | 组合组件分页（按项目） |
| GET | `/api/component/{id}` | 组件详情（含步骤树） |
| POST | `/api/component` | 新增组件（含步骤） |
| PUT | `/api/component` | 修改组件 |
| DELETE | `/api/component/{id}` | 删除组件 |

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
| POST | `/api/execute/case/{caseId}` | 调试执行用例（body: `{envId, debug, planId}`） |
| GET | `/api/execute/history/{caseId}` | 查询用例最近一次执行记录（持久化，刷新不丢） |

### 测试计划
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/plan/list` | 计划分页（projectId/name/enabled 过滤） |
| POST | `/api/plan` | 新建计划（含关联 caseIds） |
| PUT | `/api/plan` | 修改计划 |
| DELETE | `/api/plan/{id}` | 删除计划 |
| PUT | `/api/plan/{id}/enabled` | 启/停用计划 |
| POST | `/api/plan/{id}/execute` | 按计划执行（返回 PlanExecuteResult） |

### 定时任务批次
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/plan/batch/list` | 批次分页（projectId/name/enabled 过滤） |
| POST | `/api/plan/batch` | 新建批次（含关联 planIds、strategy、cron） |
| PUT | `/api/plan/batch` | 修改批次 |
| DELETE | `/api/plan/batch/{id}` | 删除批次 |
| PUT | `/api/plan/batch/{id}/enabled` | 启/停用批次 |
| POST | `/api/plan/batch/{id}/execute` | 立即执行批次（triggerType=MANUAL） |
| GET | `/api/plan/batch/{id}` | 批次详情（含关联计划） |
| GET | `/api/plan/batch/{id}/runs` | 批次运行历史 |
| GET | `/api/plan/batch/run/{runId}` | 某次运行实时状态（前端轮询） |

### 数据生成器（⬜ 后端待实现，前端 mock 已对接）
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/base/generator/list` | 按 type/name 查询 + 分页（对应管理页查询） |
| POST | `/api/base/generator` | 新增生成器 |
| PUT | `/api/base/generator/{id}` | 编辑 |
| DELETE | `/api/base/generator/{id}` | 删除（逻辑删除，沿用 `deleted`） |
| GET | `/api/base/generator/functions` | 函数名+参数+说明+示例（前端帮助/语法提示） |
| POST | `/api/base/generator/preview` | 传 template 或 type+params，返回试生成结果 |

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
- ~~报告中心**后端接口**~~：已于 2026-09-14 补齐（详见 4.1「执行报告/报告中心」行），前端由 mock 切真实接口。
- **数据生成器 + 表达式模板（前端已实现，后端待实现）**：详见 `docs/data-generator-and-expression-design.md`。前端全链路已完成：生成器管理并入「组合组件」模块「数据生成器」页签（改造 `generator/index.vue` 内嵌、移除隐藏路由 `/base/generator`）、新增 `GeneratorSelectDialog.vue` 选择已有生成器、组合组件「生成变量」卡片直接弹出（新建/选择已有）写入 `stepType=3` 步骤、客户端生成运行时 `api/generator.ts`（mock，含 GB11643 身份证校验）、用例扩展「生成变量（stepType=3）」接入与表格标签；已删除跨页回传 store，当前可独立跑通 UI 与试生成、不依赖后端。待实现：新增 `tb_data_generator` 表 + `step_type=3`（生成变量步骤）+ `GeneratorEngine` 表达式解析器（白名单、禁 eval），并将 `src/api/generator.ts` 的 mock 函数替换为对后端 §3.5 接口的 HTTP 调用；现有 `VariableResolver.resolve()` 仅匹配 `${var}`（不含 `()`），新增生成器解析遍处理 `${func(args)}` 后交回变量池。
- 批次执行同步化（长批次 HTTP 超时风险，建议改异步）。
- 报告页"失败重试"精度（当前整用例重跑降级）。
- 图表看板、通知、CI 集成、项目级 RBAC、并发执行均未开始。
- 用例列表页无「执行」入口，调试仅能在用例编辑页进行。
- 数据源弹窗缺陷见 9.1（高优先级两项仍待修）。

### 9.3 本轮已修复缺陷（2026-09-16，均在 `base/component/edit.vue`）

| commit | 问题 | 根因 | 修复 |
| --- | --- | --- | --- |
| `1bd73cd` | 「接口选择弹框」打开后下拉列表为空 | `openApiDialog()` 从不主动加载列表，只依赖 `form.moduleId` 的 watch 副作用；且 `loadApiOptions` 在 `projectId`/`moduleId` 缺失时静默清空，无 loading 无提示 | 改为打开即 `await loadApiOptions()`；新增 `apiListLoading` 绑定 `el-select :loading`；缺失依赖改为明确 warning |
| `e66ffc7` | 选择接口点确定后数据未回显（**首轮修复，方向错误**） | 当时误判为 `el-select` 把数值 value 强转成字符串导致 `find` 失败 | 加了 `String()` 比较 + warning + headers/body 回显。**并未命中根因**（见下） |
| `d46647c` | 同上，真实根因 | **`@click="openApiDialog"` 缺括号**，Vue 把原生 MouseEvent 当首参传入 → `replacingUid` 被赋成 MouseEvent → `confirmApiSelect` 误入「更换接口」分支 → `find(s => s._id === MouseEvent)` 永远匹配不到 → 静默失败（弹窗关、列表无变化、零报错） | `@click="openApiDialog()"`；并防御性改为 `openApiDialog(uid?: number \| Event)` + `const targetUid = typeof uid === 'number' ? uid : null` |

**排查方法论（复用于同类"静默失败"）**：静态读码看不出时，用 `playwright-core` + 系统 Chrome（`channel:'chrome'`，免下载 Chromium）驱动真实应用，dump Vue 组件 `setupState` 比对 Before/After。本次即靠抓到 `replacingUidRaw: { isTrusted: true, _vts: ... }`（原生事件标志）锁定根因。注意 `setupState` 中 ref **已自动解包**，勿再取 `.value`。

**全仓同类隐患**：`case/edit.vue:90` 同为 `@click="openApiDialog"`，但该函数**不接参数**（更换接口走 `openApiDialogForStep(uid)`），Event 被忽略，**无害、未修改**。

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
7. **Vue 模板 `@click` 传参陷阱（2026-09-16 踩坑）**：`@click="fn"`（不带括号）时，Vue 会把原生 `MouseEvent` 作为**第一个实参**传入。凡 handler 声明了参数（如 `fn(uid?: number)`），模板必须写 `@click="fn()"`；更稳妥的是在函数首行做类型归一化（`typeof uid === 'number' ? uid : null`）。否则会产生「无报错、无变化」的静默失败，静态读码极难发现。排查正则：`@(click|change|input)="[A-Za-z_$][A-Za-z0-9_$]*"`，命中后**逐个确认该 handler 是否声明了参数**（同名无参函数是无害的）。
8. **前端 Bug 运行时定位**：静态读码查不出时，用 `playwright-core` + 系统 Chrome（`channel:'chrome'`，免下载 ~500MB Chromium）驱动真实应用并 dump Vue `setupState`。账号 `admin/admin123`；**接口数据在项目 3、模块 "3mm"**（项目 1 为 0 条接口，选错模块会误判为"列表为空"）。详见 §9.3。
9. **开发协作**：本人（开发者）在工作过程中手动编辑过的代码，后续接手者请勿擅自改动；需调整时先沟通确认。

---

## 十一、下一步建议（按优先级）

1. **数据生成器后端落地（前端已完成）**：按 `docs/data-generator-and-expression-design.md` §3 落地后端——新增 `tb_data_generator` 表 + CRUD（§3.3/§3.5）、`GeneratorEngine` 表达式解析器 + 函数注册表（§3.1/§3.2）、`step_type=3` 执行分支（§3.4）；并将 `src/api/generator.ts` 的 mock 函数替换为对后端接口的 HTTP 调用（签名保持不变）。优先级最高的「阶段 1」`GeneratorEngine` + `/preview` 接口可在**零前端改动**下让 `${randomInt(8)}` / `${phone()}` 等直接在参数中生效。
2. **批次执行异步化**：`executeBatch` 改为"立即返回 runId + 后台线程执行"，避免长批次 HTTP 超时。
3. **修复数据源弹窗缺陷**（第九节 9.1 高优先级两项）。
4. **用例列表页补「执行」入口**，并支持选择环境。
5. **图表看板 / 通知 / CI 集成 / 项目级 RBAC / 并发执行**（长期规划）。

> 报告中心后端接口已于 2026-09-14 补齐，原下一步第 1 项已完成并移除。
