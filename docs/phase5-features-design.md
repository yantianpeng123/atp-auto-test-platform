# 第五阶段功能设计：图表看板 / 通知 / CI 集成 / 项目级 RBAC

> 状态：设计稿（尚未实现，未写代码）。用户于 2026-09-20 要求"先给思路和 UI 图，先不写代码"。
> 定位：在现有单体分层架构（Spring Boot 3.2 + MyBatis-Plus + Vue3 + Element Plus + Pinia）上扩展 4 项功能，全部以 `project_id` 做数据隔离基线。

---

## 0. 现状与依赖

| 现状 | 说明 |
| --- | --- |
| 业务隔离 | 几乎所有业务表已带 `project_id`（用例/计划/批次/执行记录/生成器），数据层隔离已具备 |
| 用户角色 | `sys_user.role` 仅有**全局**角色 `ADMIN/TESTER/VIEWER`，**无项目成员关系表** |
| 执行落库 | `tb_execution`（`status`/`passed_steps`/`failed_steps`/`total_steps`/`duration_ms`/`trigger_type`/`start_time`…）+ `tb_execution_detail` + `tb_execution_assertion` 均已落库，足以支撑看板聚合 |
| 批次执行 | 当前为**同步**（`PlanBatchServiceImpl.executeBatch` 无线程池），长批次会 HTTP 超时（P1 缺陷，建议先做异步化，否则"执行完成即通知"体验差） |
| 菜单 | 已有 `dashboard`（工作台）、`report`（报告中心）；`batch`/`plan`/`case`/`dataset` 等均已就位 |

### 落地顺序建议
1. **项目级 RBAC**（地基：看板/通知/CI 都按项目成员过滤，成员权限决定能否看到/操作）
2. **图表看板**（只读聚合，依赖 RBAC 的项目成员过滤）
3. **通知**（依赖执行完成事件；建议同步完成 P1 批次异步化）
4. **CI 集成**（依赖 RBAC 的 token 鉴权 + 执行引擎异步）

---

## 1. 项目级 RBAC

### 1.1 数据模型（新增）
```sql
CREATE TABLE tb_project_member (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  role        VARCHAR(20) NOT NULL COMMENT 'OWNER/MAINTAINER/DEVELOPER/VIEWER',
  inviter_id  BIGINT,
  deleted     TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_proj_user (project_id, user_id, deleted)
);
```

### 1.2 角色权限矩阵（文档约定，非表）
| 能力 | OWNER | MAINTAINER | DEVELOPER | VIEWER |
| --- | --- | --- | --- | --- |
| 看板 / 报告查看 | ✅ | ✅ | ✅ | ✅ |
| 用例 / 计划 / 批次 CRUD | ✅ | ✅ | ✅（用例/计划） | ❌ |
| 通知 / CI 配置 | ✅ | ✅ | ❌ | ❌ |
| 成员管理 / 项目设置 / 删除项目 | ✅ | ❌ | ❌ | ❌ |

### 1.3 后端
- `ProjectMemberService`：邀请（按用户名直接添加 / 邀请码）、移除、改角色、列表。
- `ProjectAuthInterceptor`（或 Spring AOP + `@RequireProjectRole` 注解）：对 `project_id` 作用域接口校验"当前用户是否为成员且角色满足"，失败返回 403 + 业务码。
- **全局 `ADMIN` 视为超管**，绕过成员校验、可跨所有项目。
- 项目列表 / 切换下拉改为"我参与的"；存量项目：创建者自动写入 `tb_project_member(role=OWNER)`，其余用户无成员关系即不可见（保持"无成员=看不到"）。

### 1.4 前端
- 路由 `/project/members`（OWNER/MAINTAINER 可见）；项目切换下拉改为从成员关系拉取。
- 权限指令 `v-role` / 全局 `useAuth()` 控制按钮显隐。

### 1.5 复用点
- `projectStore.currentProject` 已存在，成员过滤在其上叠加；`User` 实体已有 `role` 字段，新增 `projectRole` 概念仅在成员表与上下文里。

---

## 2. 图表看板

### 2.1 数据模型
- **无需新表**，全部复用 `tb_execution` 聚合；可选 `tb_dashboard_cache`（TTL 5min）后续再加，先按 `GROUP BY` 实时算。

### 2.2 后端接口（全部带 `project_id` 过滤，受 RBAC 约束）
| 方法 | 路径 | 返回 |
| --- | --- | --- |
| GET | `/api/dashboard/summary?range=30d&envId=` | `{caseCount, execCount, passRate, failRate, avgDurationMs}` |
| GET | `/api/dashboard/trend?range=30d` | `[{date, execCount, passRate}]` |
| GET | `/api/dashboard/distribution?dim=env\|plan\|module` | `[{name, value}]` |
| GET | `/api/dashboard/topFailed?limit=10` | `[{caseId, caseName, failCount}]` |
| GET | `/api/dashboard/recent?limit=10` | 最近执行（复用 execution list） |

### 2.3 实现要点
- 趋势：`SELECT DATE(start_time) d, COUNT(*) c, SUM(passed_steps)/NULLIF(SUM(total_steps),0) r FROM tb_execution WHERE project_id=? AND start_time>=? GROUP BY d`。
- 模块分布：`execution.case_id → tb_test_case.module_id → tb_application_module.name`（或在 case 上冗余 `module_name` 快照，避免三表 JOIN 慢查询）。
- 性能：`tb_execution.idx_project` 已建；趋势限制 `range<=90d`。
- 图表库：前端引入 `echarts` / `vue-echarts`（趋势用折线双轴、分布用饼图，与 mockup 一致）。

### 2.4 前端
- 路由：原 `/dashboard`（工作台，快捷入口）保留；新增 `/board`（看板）。或把"工作台"改造为看板——建议**新增 `/board`**，避免覆盖现有入口。
- 组件：`KpiCards` / `TrendChart` / `DistPie` / `RecentTable` / `TopFailedTable` + 顶部筛选条（日期范围 + 环境）。

---

## 3. 通知

### 3.1 数据模型（新增）
```sql
CREATE TABLE tb_notify_channel (
  id, project_id, type VARCHAR(20) COMMENT 'INAPP/DINGTALK/EMAIL_163',
  name, config JSON, enabled TINYINT, deleted, create_time
);
CREATE TABLE tb_notify_rule (
  id, project_id, name, event VARCHAR(20) COMMENT 'EXEC_DONE/EXEC_FAIL/BATCH_DONE',
  channel_ids JSON, condition JSON COMMENT '如 {onlyFail:true}', enabled, deleted, create_time
);
CREATE TABLE tb_notify_log (
  id, project_id, rule_id, channel_id, event, status, content, error, create_time
);
CREATE TABLE tb_notify_message (   -- 站内信收件箱
  id, user_id, project_id, title, content, read TINYINT, link_url VARCHAR(255), create_time
);
```

> **渠道 `config` JSON 结构（2026-09-20 锁定选型：Webhook=钉钉，邮件=163）**
> - `type=INAPP`：无需 config，直接写库给项目内可接收角色成员。
> - `type=DINGTALK`（钉钉群机器人 webhook）：
>   ```json
>   {
>     "platform": "DINGTALK",
>     "webhook": "https://oapi.dingtalk.com/robot/send?access_token=xxxxx",
>     "secret": "SECxxxx（加签密钥，可空=不加签）",
>     "atMobiles": ["13800138000"],
>     "msgtype": "markdown"
>   }
>   ```
>   发送器 `DingTalkSender` 拼装钉钉报文 `{"msgtype":"markdown","markdown":{"title":...,"text":...},"at":{"atMobiles":[...]}}`；加签 = `HMAC-SHA256(timestamp+"\n"+secret)` Base64 后拼 `&timestamp=&sign=`。单机器人限频 20 条/分钟。
> - `type=EMAIL_163`（网易 163 邮箱 SMTP）：
>   ```json
>   {
>     "platform": "EMAIL_163",
>     "host": "smtp.163.com",
>     "port": 465,
>     "username": "xxx@163.com",
>     "authCode": "XXXX（授权码，非登录密码）",
>     "from": "xxx@163.com",
>     "ssl": true
>   }
>   ```
>   发送器 `Mail163Sender` 用 `JavaMailSender`：`mail.smtp.auth=true`、`mail.smtp.ssl.enable=true`、端口 465；`from` 须与认证账号一致。仅适合开发联调/小团队，生产建议换阿里云邮件推送。`config.platform` 字段为以后扩展企业微信/飞书留口。

### 3.2 后端
- `NotifyService.dispatch(event, payload)`：按 `project_id + event + enabled` 命中规则 → 逐 `channel_ids` 发送 → 每渠道写 `tb_notify_log`（失败记 error，重试 1 次，避免静默丢失）。
- 事件源：执行完成钩子（`executeCase` 单接口落库后 / 批次 run 完成 `run.setStatus` 后）`ApplicationEventPublisher.publishEvent(new NotifyEvent(...))`，配合 `@Async` 监听器异步派发，主流程零阻塞。
- 渠道实现（两发送器 + 站内信）：
  - `DingTalkSender`：钉钉群机器人 webhook（`RestTemplate` POST），支持加签与 @手机号，markdown 报文。
  - `Mail163Sender`：`JavaMailSender` + 163 SMTP（授权码鉴权、SSL 465）。
  - `INAPP`：直接写 `tb_notify_message` 给项目内 `OWNER/MAINTAINER/DEVELOPER`（按 `tb_project_member` 取成员）。
- 顶栏消息中心：`GET /api/notify/messages?unread=1` 轮询；`GET /api/notify/unread-count` 返回未读角标。

### 3.3 前端
- `/notify/config`（OWNER/MAINTAINER）：渠道卡片（新增/启用/配置）+ 规则表（事件选择 + 渠道多选 + 条件）+ 发送日志。
- 顶栏铃铛：未读角标 + 消息中心抽屉（标记已读）。

### 3.4 依赖
- 建议先做 **P1 批次异步化**：否则"执行完成通知"只能在同步结束时触发（也能用，但长批次会卡在返回前）。

---

## 4. CI 集成

### 4.1 数据模型（新增）
```sql
CREATE TABLE tb_ci_token (
  id, project_id, name, token VARCHAR(64), scope VARCHAR(20) COMMENT 'plan/batch/case',
  enabled TINYINT, last_used DATETIME, deleted, create_time
);
-- 可选审计：tb_ci_run(id, project_id, token_id, trigger, ref_id, run_id, status)
```

### 4.2 后端接口
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ci/trigger` | Body `{projectKey, planId|batchId, envId, token}` → 校验 token（scoped 到 project & plan）→ **立即返回 `{runId}`** → 后台异步执行（复用执行引擎 / 批次异步） |
| GET | `/api/ci/status/{runId}` | 进度 / 结果（复用 execution / batch-run 状态） |
| GET | `/api/ci/report/{runId}.xml` | **JUnit 格式 XML**：遍历 `tb_execution_detail`，按 case 聚合成 `<testsuite><testcase>` |
| GET/POST/DELETE | `/api/ci/token` | token 管理（OWNER/MAINTAINER） |

### 4.3 鉴权
- CI 走 **token**，不走 JWT；`@ProjectTokenAuth` 拦截器解析 token → 映射 `project + scope`，与 RBAC 解耦但复用 `tb_project` 归属。
- `dryRun` 参数：仅校验 token 与权限范围，不真正执行（对应前端"测试连接"）。

### 4.4 前端
- `/ci/config`：token 列表（显示/隐藏/复制、生成、权限范围、最近调用）+ 代码片断页签（`cURL` / `Jenkinsfile` / `.gitlab-ci.yml`，带 `projectKey+planId+token` 占位）+ "测试连接"按钮。

---

## 5. 文件清单（实现阶段参考）

### 后端（module 划分，模块间不横向依赖）
- RBAC：`controller/ProjectMemberController`、`service/ProjectMemberService`、`interceptor/ProjectAuthInterceptor`、`entity/TbProjectMember`、`mapper/ProjectMemberMapper`、`@RequireProjectRole` 注解 + AOP。
- 看板：`controller/DashboardController`、`service/DashboardService`（聚合 Mapper 自定义 SQL）。
- 通知：`controller/NotifyController`、`service/NotifyService` + `channel/DingTalkSender`、`channel/Mail163Sender`、`entity/*`、`mapper/*`、`event/NotifyEvent` + `NotifyEventListener`。
- CI：`controller/CiController`、`service/CiService`、`interceptor/ProjectTokenAuthInterceptor`、`entity/TbCiToken`、`mapper/*`。

### 前端（新增路由 + 页面 + api）
- `router/index.ts`：新增 `/board`、`/notify/config`、`/ci/config`、`/project/members`。
- `views/board/index.vue`、`views/notify/config.vue`、`views/ci/config.vue`、`views/project/members.vue`。
- `api/dashboard.ts` / `api/notify.ts` / `api/ci.ts` / `api/projectMember.ts`。
- 顶栏：消息中心铃铛 + 未读角标；项目切换下拉改为成员过滤；`directives/role.ts`。

### 顺序与工作量（粗估）
| 顺序 | 功能 | 后端 | 前端 | 备注 |
| --- | --- | --- | --- | --- |
| 1 | 项目级 RBAC | 中 | 中 | 地基，其他三项都依赖 |
| 2 | 图表看板 | 小 | 中 | 只读聚合，引入 echarts |
| 3 | 通知 | 中 | 小 | 顺带 P1 批次异步化 |
| 4 | CI 集成 | 中 | 小 | 依赖 token 鉴权 + 异步执行 |

---

## 6. 入口与菜单位置决策（RBAC 入口）

> 用户于 2026-09-20 追问："先做 RBAC 入口配到哪里？独立菜单还是挂项目管理下？" 以下为分析结论（尚未实现）。

### 6.1 现状关键发现
- 侧边栏 `frontend/src/layout/BasicLayout.vue` 中已存在一个**被禁用的占位菜单**：`el-sub-menu index="asset"` 内的 `<el-menu-item index="/project" disabled>` 写着"项目管理"（第 20 行附近），与"用例管理/数据源管理"并列。即"挂在项目管理下"这一选项，前端骨架已半做好，仅被 `disabled` 关闭。
- 顶栏已常驻显示 `当前项目：{{ projectStore.currentProject.name }}`，且 dropdown 提供"切换项目"——`currentProject` 在应用内全局可用。
- 路由层已有 `/project/select`（项目选择闸口页），但**无"项目管理"业务页**。

### 6.2 入口位置结论
**启用并升级侧边栏的「项目管理」(`/project`)**：从"禁用的叶子菜单"升级为 `el-sub-menu`，下挂两个子项：
- `项目信息` → `/project/info`
- **`成员管理` → `/project/members`** ← RBAC 主入口（成员列表 + 角色分配）

依据：项目级 RBAC 本质是"某一项目的成员与角色"，必须绑定 `currentProject`；而 `currentProject` 进应用后全局可用，放入项目作用域菜单语义自洽、零新增顶层结构。

可选增强：顶栏"切换项目"旁加「项目设置」快捷按钮，点击直达 `/project/members`（主入口仍在侧边栏，避免入口分散）。

### 6.3 独立菜单 vs 挂项目管理下
**结论：挂在「项目管理」下，不做与"用例管理"平级的独立"RBAC/成员管理"顶级菜单。**
1. 现有代码已预留 `/project` disabled 位，直接复用，零新增顶层结构；
2. 项目级权限属于 project-meta（配置类），与"接口/用例/计划"等 per-project 功能模块在抽象层级上不应并列；
3. 后续通知配置、CI 配置也属项目级配置，可一并归入「项目管理」下，形成统一"项目设置中心"，避免侧边栏膨胀。

改造后侧边栏结构（示意）：
```
侧边栏（改造后）
├─ 工作台
├─ 项目管理 ▾            ← 启用原 disabled 位，建议独立成 el-sub-menu
│   ├─ 项目信息          /project/info
│   └─ 成员管理          /project/members   ← RBAC 主入口（仅 OWNER/MAINTAINER 可见）
├─ 资产 ▾
│   ├─ 用例管理
│   └─ 数据源管理
├─ 基础 ▾
│   ├─ 工程版本信息
│   ├─ 接口列表
│   └─ 组合组件
├─ 运行 ▾
│   ├─ 测试计划
│   ├─ 定时任务
│   └─ 报告中心
└─ 环境配置
```
> 现 `/project` 被塞在 `asset` 子菜单内（与用例/数据源并列）。启用后建议把它**独立成 `el-sub-menu index="project"`**——"项目管理"是配置中枢，与"资产/基础/运行"三类功能模块不是同一抽象层级，平级更清晰。

### 6.4 落地前置依赖（启用入口前）
- **角色守卫（前端）**：`成员管理` 菜单项与页面按"当前用户在该项目角色"条件渲染——仅 `OWNER/MAINTAINER` 可见可进；`VIEWER/DEVELOPER` 不可见。后端 `@RequireProjectRole` 兜底。
- **依赖接口**：①`GET /api/project/members`（列表+角色）②`GET /api/project/my-role`（侧边栏渲染用，判断当前人能否看到"成员管理"）。
- **存量迁移风险点**：现有项目在 `tb_project_member` 无任何记录。若按"无成员=无权限"判定，存量项目会变成**无人能管理的死项目**。必须先做迁移：每个项目创建者回填 `OWNER`。
- **全局 ADMIN 超管**：`sys_user.role=ADMIN` 视为跨项目超管，任何项目"成员管理"对其始终可见可读写，不受 `tb_project_member` 限制。
- **顺序**：先建表 + 迁移脚本 + `my-role` 接口（前端能判断显隐）→ 再启用侧边栏 `/project` 子菜单 → 最后做成员管理页增删改。

---

## 7. 风险与取舍
- **RBAC 迁移**：存量项目需回填 `OWNER`，否则旧用户看不到项目——提供"首次访问旧项目自动认领为 OWNER"的兜底。
- **看板性能**：实时 `GROUP BY` 在数据量大时可能慢，先限 `range<=90d`，必要时加 `tb_dashboard_cache`。
- **通知可靠性**：Webhook/邮件失败要有重试与 `tb_notify_log` 留痕，避免静默丢失。
- **CI token 安全**：token 仅展示一次（生成后前端脱敏 `ci_•••••xxxx`），`last_used` 审计；支持吊销。
