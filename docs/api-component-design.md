# 组合组件（可复用公共接口）设计文档

> 目标：在"用例管理 / 新增·编辑用例步骤"中，把接口做成可**直接引用**的公共组件；并支持把**多个接口打包成可多层嵌套的组合组件**复用。
> 状态：设计评审稿（待评审后实现）

---

## 1. 背景与已具备的底座

平台已经存在"接口库"概念，是本次需求的天然底座：

- **接口库** `tb_api_definition`（`base` 模块）：存 `method / path / headers / body` 模板，前端在「接口列表」页面管理。
- **用例步骤** `tb_case_step`：已通过 `api_id` **引用**接口库，步骤本身只存三样东西：`request_override`（覆盖 headers/body/params）、`assertions`（断言）、`response_var`（响应变量）。`method/path` 在步骤里是只读展示接口库的。
- **单接口用例** `tb_test_case.api_id`：直接挂接口库。
- **执行引擎** `ExecuteServiceImpl`：已实现"三层合并"（环境 env → 接口库 api → 步骤覆盖 step），交给唯一的 `HttpExecutor`（OkHttp 单例）发请求、跑 `AssertionEngine` 断言。
- **变量池**：每个用例执行按"轮次"创建 `Variables variables` + `VariableResolver`，同一轮内所有步骤**顺序共享**该变量池（`response_var` 写入、`${var}` 引用），天然支持跨步骤传参。

结论：数据模型已支持"单接口复用"。本设计聚焦两件事——**① 组件选择器 UX 增强** 与 **② 组合组件（多接口 + 多层嵌套复用）**。

---

## 2. 已锁定的关键决策（评审基线）

| 决策点 | 结论 |
| --- | --- |
| 引用语义 | **强引用（联动）**：接口库改 `method/path/headers`，所有引用方（用例步骤、组合组件）同步生效，保证单一真相源 |
| 共享边界 | **项目内共享**：组合组件挂在现有 工程→版本→模块 体系下，仅所属项目可见复用 |
| 组件嵌套 | **允许多层嵌套**：组合组件内部可再引用其他组合组件（需防环 + 深度守卫） |
| 变量透传 | **可见（透传）**：组件内 `response_var` 写入与宿主用例共享的同一变量池，后续任意层步骤均可 `${var}` 引用 |
| 管理入口 | **独立菜单「组合组件」**（与「接口列表」并列，复用 module 树） |
| 本轮范围 | 组件选择器增强 + 组合组件（**不含**单接口调试入口、不含"就地另存为公共接口"） |
| 推进方式 | 先出设计文档，评审后再实现（后端先行 → 前端 → 联调） |

---

## 3. 数据模型

### 3.1 新增表：`tb_api_component`（组合组件头表）

```sql
DROP TABLE IF EXISTS `tb_api_component`;
CREATE TABLE `tb_api_component`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `project_id`  BIGINT       NOT NULL COMMENT '项目ID（项目隔离）',
    `module_id`   BIGINT       DEFAULT NULL COMMENT '所属模块ID（便于按 module 树组织）',
    `name`        VARCHAR(100) NOT NULL COMMENT '组合组件名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `create_by`   BIGINT       DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    -- 项目内同名（含逻辑删除位）唯一
    UNIQUE KEY `uk_project_name_deleted` (`project_id`, `name`, `deleted`),
    KEY `idx_project` (`project_id`),
    KEY `idx_module` (`module_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='组合组件表';
```

### 3.2 新增表：`tb_api_component_step`（组合组件子步骤）

> 与 `tb_case_step` 同构，但引用目标泛化：`step_type` 区分"单接口"还是"嵌套组件"。

```sql
DROP TABLE IF EXISTS `tb_api_component_step`;
CREATE TABLE `tb_api_component_step`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `component_id`       BIGINT       NOT NULL COMMENT '所属组合组件ID',
    `step_type`          TINYINT      NOT NULL DEFAULT 1 COMMENT '1-单接口 2-嵌套组件',
    `api_id`             BIGINT       DEFAULT NULL COMMENT 'step_type=1 时必填：关联接口ID',
    `child_component_id` BIGINT       DEFAULT NULL COMMENT 'step_type=2 时必填：嵌套的组合组件ID',
    `sort_order`         INT          NOT NULL DEFAULT 0 COMMENT '执行顺序(从1开始)',
    `step_name`          VARCHAR(200) DEFAULT NULL COMMENT '步骤名称',
    `request_override`   JSON         DEFAULT NULL COMMENT '请求覆盖内容(headers/body/params)',
    `assertions`         JSON         DEFAULT NULL COMMENT '步骤断言规则',
    `response_var`       VARCHAR(100) DEFAULT NULL COMMENT '响应变量名',
    `create_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (`id`),
    KEY `idx_component` (`component_id`),
    KEY `idx_api` (`api_id`),
    KEY `idx_child` (`child_component_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='组合组件子步骤表';
```

### 3.3 改造表：`tb_case_step`（支持引用组合组件）

```sql
-- 1) api_id 由 NOT NULL 改为可空（组件引用步骤无直接 api_id）
ALTER TABLE `tb_case_step` MODIFY COLUMN `api_id` BIGINT DEFAULT NULL COMMENT '关联接口ID（step_type=1 时必填）';
-- 2) 新增步骤类型与组合组件引用
ALTER TABLE `tb_case_step` ADD COLUMN `step_type`    TINYINT   NOT NULL DEFAULT 1 COMMENT '1-单接口 2-组合组件' AFTER `api_id`;
ALTER TABLE `tb_case_step` ADD COLUMN `component_id` BIGINT   DEFAULT NULL COMMENT 'step_type=2 时必填：关联组合组件ID' AFTER `step_type`;
ALTER TABLE `tb_case_step` ADD KEY `idx_component` (`component_id`);
```

> 说明：`CaseStep.apiId` 实体字段保留；新增 `stepType`、`componentId` 字段；`apiId` 在 `step_type=1` 时必填，`componentId` 在 `step_type=2` 时必填。

### 3.4 改造表：`tb_execution_detail`（记录嵌套来源，支撑报告树）

```sql
ALTER TABLE `tb_execution_detail` ADD COLUMN `component_id`    BIGINT DEFAULT NULL COMMENT '来源组合组件ID（用于报告分组）' AFTER `step_id`;
ALTER TABLE `tb_execution_detail` ADD COLUMN `parent_step_id`  BIGINT DEFAULT NULL COMMENT '父步骤ID（嵌套树）' AFTER `component_id`;
ALTER TABLE `tb_execution_detail` ADD COLUMN `nest_level`      INT    DEFAULT 0     COMMENT '嵌套层级 0=用例直接步骤' AFTER `parent_step_id`;
ALTER TABLE `tb_execution_detail` ADD KEY `idx_component` (`component_id`);
```

### 3.5 新增表：`tb_favorite`（组件选择器"收藏"）

```sql
DROP TABLE IF EXISTS `tb_favorite`;
CREATE TABLE `tb_favorite`
(
    `id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`   BIGINT       NOT NULL COMMENT '用户ID',
    `ref_type`  VARCHAR(20)  NOT NULL COMMENT '收藏类型 API / COMPONENT',
    `ref_id`    BIGINT       NOT NULL COMMENT '被收藏对象ID',
    `create_time` DATETIME   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_ref` (`user_id`, `ref_type`, `ref_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收藏表';
```

### 3.6 实体类（新增 / 改造）

- 新增 `ApiComponent.java`（`@TableName("tb_api_component")`）：`id, projectId, moduleId, name, description, createBy, createTime, updateTime, deleted`。
- 新增 `ApiComponentStep.java`（`@TableName("tb_api_component_step")`）：`id, componentId, stepType, apiId, childComponentId, sortOrder, stepName, requestOverride, assertions, responseVar, ...`。
- 改造 `CaseStep.java`：新增 `stepType`、`componentId`；`apiId` 保持但语义变为可空。
- 改造 `ExecutionDetail.java`：新增 `componentId`、`parentStepId`、`nestLevel`。
- `CaseStepVO.java`：新增 `stepType`、`componentId`（供执行展开与报告使用）。
- `StepExecuteVO.java`：新增 `componentId`、`parentStepId`、`nestLevel`（供前端报告树渲染）。

---

## 4. 后端接口契约

### 4.1 组合组件 CRUD（`ApiComponentController`，基址 `/api/component`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/component/list` | 分页列表：`projectId`(必填) / `moduleId` / `name` / `page` / `size` |
| GET | `/api/component/{id}` | 详情（**含子步骤 steps**） |
| POST | `/api/component` | 新增（含子步骤） |
| PUT | `/api/component` | 编辑（**整表替换子步骤**：先逻辑删旧步、再插新步，复用用例的步骤替换策略） |
| DELETE | `/api/component/{id}` | 逻辑删除组件 + 其子步骤；**删除前检查是否被其他组件/用例引用，有引用则拒绝并提示先解除** |

**新增/编辑入参 `ApiComponentSaveRequest`**

```json
{
  "id": 0,
  "projectId": 12,
  "moduleId": 34,
  "name": "登录前置",
  "description": "获取 token 并写入变量",
  "steps": [
    {
      "id": 0,
      "stepType": 1,
      "apiId": 88,
      "childComponentId": null,
      "sortOrder": 1,
      "stepName": "获取 token",
      "requestOverride": "{\"headers\":{},\"body\":{\"username\":\"${u}\"}}",
      "assertions": "[{\"type\":\"jsonPath\",\"path\":\"$.code\",\"operator\":\"eq\",\"expected\":\"0\"}]",
      "responseVar": "loginResp"
    },
    {
      "stepType": 2,
      "childComponentId": 7,
      "sortOrder": 2,
      "stepName": "刷新会话"
    }
  ]
}
```

**出参 `ApiComponentVO`**：基础字段 + `steps: List<ApiComponentStepVO>`（子步骤列表，按 `sort_order` 排序）。

### 4.2 防环校验（保存时）

- `ApiComponentService.validateNoCycle(componentId, childComponentId)`：
  - 递归收集 `childComponentId` 的所有后代组件 id；若集合中包含 `componentId`（自身），则抛 `BizException(COMPONENT_CYCLE)`。
  - 同时在 `save` 时做 `depth` 上限校验（见 4.4）。
- 前端在"嵌套组件"选择对话框中同步禁用会成环的选项（后端为最终兜底）。

### 4.3 用例保存契约变更

现有 `CaseCreateRequest` / `CaseUpdateRequest` 内嵌的 `StepDTO` 增加两个字段（其余不变）：

```java
private Integer stepType;     // 1-单接口 2-组合组件
private Long componentId;     // stepType=2 时必填
```

`CaseServiceImpl.replaceSteps(...)`：按 `step_type` 写 `api_id` / `component_id`，保持整表替换策略。

### 4.4 执行引擎改造（`ExecuteServiceImpl`）

1. **步骤展开** `List<ResolvedStep> expandSteps(List<CaseStepVO> topSteps)`：
   - 遍历顶层步骤；`step_type=1` 直接生成 `ResolvedStep`（取自 `CaseStepVO`）。
   - `step_type=2` 调用 `expandComponent(componentId, depth=1)` 递归展开。
2. **递归展开** `List<ResolvedStep> expandComponent(Long componentId, int depth)`：
   - `if (depth > MAX_DEPTH /*=10*/) throw BizException(COMPONENT_DEPTH_EXCEED)`（双保险防无限递归）。
   - 查 `tb_api_component_step`（按 `component_id` + `sort_order` 升序）。
   - `step_type=1` → 生成 `ResolvedStep`（含 `apiId`、覆盖、断言、变量、`componentId`/`parentStepId`/`nestLevel` 标记）。
   - `step_type=2` → 递归 `expandComponent(childComponentId, depth+1)`，把结果压平插入，**逐层递增 `nestLevel`、串联 `parentStepId`**。
   - 返回扁平有序步骤列表。
3. **变量透传**：展开后的扁平列表在 `executeCase` 的"轮次循环"内顺序执行，**复用同一 `variables` / `resolver`**（现有机制），组件内 `response_var` 自然对后续任意层步骤可见——满足"透传"。
4. **`executeStep` 适配**：入参由 `CaseStepVO` 改为统一的 `ResolvedStep`（字段同 `CaseStepVO` 的可用部分 + 嵌套标记）；内部 `apiDefinitionMapper.selectById(resolvedStep.getApiId())` 不变。
5. **落库与报告**：
   - `persistExecution`：迭代扁平 `ResolvedStep` 列表，写 `ExecutionDetail` 时带上 `componentId` / `parentStepId` / `nestLevel`。
   - `buildRounds`：从 `ExecutionDetail` 读回嵌套三列，写入 `StepExecuteVO` 对应字段。
   - 前端 `report.vue` 依据这三列构建可折叠的嵌套步骤树。

> 新增 `ResultCode`：`COMPONENT_NOT_FOUND(3105)`、`COMPONENT_CYCLE(3106)`、`COMPONENT_DEPTH_EXCEED(3107)`、`COMPONENT_REFERENCED(3108)`（删除被引用组件时）。

---

## 5. 前端设计

### 5.1 独立菜单「组合组件」
- 路由 `/component`（列表）、`/component/edit/:id?`（编辑），在 `BasicLayout` 菜单中新增独立入口（与「接口列表」并列，置于"接口管理"分组下）。

### 5.2 组合组件管理页
- **列表页**：项目过滤（复用 `useProjectStore`）+ module 树筛选 + 名称搜索 + 增删改入口。
- **编辑页**：复用 `case/edit.vue` 的"步骤编辑器"组件（步骤列表 + 单接口/嵌套组件切换 + 排序 + 断言 + 变量）。
  - 步骤类型切换：`stepType=1` 走现有"选择接口"；`stepType=2` 弹"选择组合组件"对话框，并**禁用会成环的选项**（前端预计算后代集合）。
  - 保存时调用 `POST/PUT /api/component`，后端防环兜底。

### 5.3 用例步骤选择器增强（`case/edit.vue`）
改造现有"选择接口 / 添加步骤"对话框为"**公共组件**"对话框：
- 双 Tab：**接口库**（单接口）/ **组合组件**。
- 左侧 工程→版本→模块 树（复用「接口列表」页现有级联）。
- 顶部关键词搜索（name / method / path）。
- 卡片预览：选中项展示 `method`、`path`、`headers` 概要。
- **最近使用**：`localStorage` 按 `projectId` 缓存最近引用（无需后端）。
- **收藏**：调 `/api/favorite/**`（基于 `tb_favorite`），星标接口/组件。
- 选中接口 → 生成 `stepType=1` 步骤；选中组合组件 → 生成 `stepType=2` 步骤（`componentId` 填充）。

### 5.4 报告页嵌套步骤树（`report.vue`）
- 按 `parentStepId` / `nestLevel` 递归渲染可折叠的步骤树：顶层用例步骤 → 组合组件（折叠显示其通过/失败汇总）→ 子步骤 → 嵌套组件……
- 失败步骤高亮 + 重试逻辑保持（重试粒度仍为用例级，组合组件内步骤随用例重跑）。

---

## 6. 实施计划

**Phase 1 — 后端基础（可独立编译验证）**
1. `schema.sql` 追加新表 DDL + `tb_case_step` / `tb_execution_detail` 的 `ALTER` 语句；同步已有库（提供迁移脚本）。
2. 实体 `ApiComponent` / `ApiComponentStep`；`CaseStep` / `ExecutionDetail` / `CaseStepVO` / `StepExecuteVO` 加字段。
3. Mapper + `ApiComponentService` / `Impl`：CRUD、按 `projectId`/`moduleId` 过滤、含子步骤、保存防环。
4. `ApiComponentController`（`/api/component/**`）+ `FavoriteController`（收藏）。
5. `CaseStepDTO` 加 `stepType` / `componentId`；`CaseServiceImpl.replaceSteps` 适配。
6. `ExecuteServiceImpl`：`expandSteps` + `expandComponent` 递归 + 共享变量池；`persistExecution` / `buildRounds` 写回嵌套三列；新增相关 `ResultCode`。

**Phase 2 — 前端**
7. 独立菜单「组合组件」：路由 + 布局菜单项。
8. 组合组件管理页（列表 + 编辑，复用步骤编辑器，嵌套引用 + 防环提示）。
9. `case/edit.vue` 选择器升级：双 Tab + module 树 + 搜索 + 卡片预览 + 最近使用 + 收藏；步骤支持 `stepType`。
10. `report.vue` 渲染嵌套步骤树。

**Phase 3 — 联调验证**
11. 后端 `mvn -q compile` + 前端 `vue-tsc --noEmit` 通过。
12. 造一个"登录前置"多层嵌套组件（含 `response_var` 透传）跑通，验证执行展开、变量透传、报告树。

---

## 7. 风险与取舍

- **删除被嵌套引用的组件**：执行时该步骤报"组件不存在"。策略——删除前检查引用并拒绝（返回 `COMPONENT_REFERENCED`），引导先解除引用。
- **强引用联动**：接口库或子组件被修改，所有引用方同步生效（符合决策，但需在前端提示"该接口被 N 处引用"以减少误改）。
- **多层嵌套深度**：`MAX_DEPTH=10` 守卫防爆炸，超深直接失败并提示。
- **`api_id` 改可空**：影响历史数据与现有表，必须提供 `ALTER` 迁移脚本并验证本地/测试库。
- **变量名冲突**：不同组件内同名 `response_var` 会被后写者覆盖（共享池语义）。建议文档约定组件内变量加前缀（如 `login_*`）。

---

## 8. 验收标准

1. 接口库中的接口，在用例步骤选择器中可被搜索、按 module 树筛选、收藏、并"直接引用"生成步骤。
2. 可新建组合组件，子步骤既可是单接口也可是其他组合组件（多层嵌套）。
3. 保存组合组件时，若存在环引用被拒绝并明确提示。
4. 用例步骤引用组合组件后，执行时按嵌套顺序展开，组件内 `response_var` 对宿主用例后续步骤可见并正确解析。
5. 执行报告按嵌套层级渲染可折叠步骤树，展开能看到每个子步骤的请求/响应/断言。
6. 后端编译通过、前端类型检查通过；提供一个"登录前置"嵌套组件作为回归样例。
