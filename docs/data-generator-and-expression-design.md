# 数据生成器 + 表达式模板 — 实现思路（设计文档）

> 状态：设计阶段，尚未编码。
> 关联文档：`api-component-design.md`（组合组件设计）、`HANDOVER.md`。
> 关联代码：`backend/.../execute/core/VariableResolver.java`、`Variables.java`、`ExecuteServiceImpl.java`；前端 `base/component/edit.vue`、`case/edit.vue`。

---

## 0. 背景与决策

### 0.1 由来
- 用例/组件的前置（pre）、后置（post）步骤里，经常需要**随机参数**（随机手机号、身份证号、订单号、时间戳偏移等）。
- 设计文档已预留 `step_type=3` = "其他类型（生成随机数）"，但此前仅是 `component/edit.vue` 中一个 `disabled` 占位卡片（点击只弹 toast）。
- 用户已确认的方向：
  1. 做**独立的"数据生成器"页面**（挂在「基础数据」菜单下，路由 `/base/generator`），可按类型查询、可新增生成器类型。
  2. 采用**「表达式模板 + 生成器」双轨制**：既能用结构化表单配生成器（照顾非技术用户），也能用一行表达式模板任意拼接（满足"固定长度随机数"等灵活需求）。
  3. 表达式模板为最灵活形态，前端可指定扩展（如固定长度随机数），不强制改前后端两边代码。

### 0.2 关键事实（已核实现有代码）
- 变量池机制：`execute/core/Variables.java` + `VariableResolver.java`。
- 唯一替换入口：`VariableResolver.resolve(String input)`，使用正则 `\${([\w.\[\]-]+)}` 把 `${name}` / `${name.path}` 替换为变量池值（支持 JSONPath、header、status）。
- **该正则不含 `(` `)`，因此 `${func(args)}` 当前不会被当作变量**——这正是接入函数语法的天然切点：新增一个"生成器解析遍"处理 `${func(...)}`，再交给现有 `VariableResolver` 处理 `${var}`。
- 变量池为"按轮次（round）共享"：同一轮内所有步骤顺序共享同一 `Variables`，组件内 `response_var` 与宿主用例共享同一池，天然支持跨步骤传参。

---

## 1. 总体架构

三层结构，函数语法与变量语法共用同一条解析流水线：

```
┌─────────────────────────────────────────────────────────────┐
│  1. 表达式解析引擎  GeneratorEngine（纯后端，无状态）          │
│     - 白名单函数注册表 Map<name, GeneratorFunc>               │
│     - 两遍解析：先替换 ${变量} → 再执行 ${func(args)}        │
│     - 安全：手写解析，绝不用 eval / ScriptEngine              │
└───────────────────────────┬─────────────────────────────────┘
                            │ 被调用
┌───────────────────────────┴─────────────────────────────────┐
│  2. 生成器实体  tb_data_generator（结构化配置，存库）          │
│     - type: RANDOM/PHONE/IDCARD/NAME/ENUM/TIMESTAMP/UUID/CUSTOM│
│     - CUSTOM 类型即存一段表达式模板，执行时交给 GeneratorEngine│
└───────────────────────────┬─────────────────────────────────┘
                            │ 被引用
┌───────────────────────────┴─────────────────────────────────┐
│  3. 步骤接入  step_type = 3（"生成变量"步骤）                 │
│     - 执行引擎 expandOne 遇 step_type=3 → 调生成器 → 写变量池  │
│     - 后续步骤用 ${varName} 引用                              │
│     - 与现有 ${var} 替换点"合流"，pre/main/post 全部可用      │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 表达式模板语法

- 函数调用形式：`${funcName(arg1, arg2, ...)}`
- 参数支持：数字、引号字符串、以及嵌套 `${...}`（引用前面已生成的变量）。
- 可自由与变量混排，例如：
  - `NO-${randomInt(6)}` → `NO-482173`
  - `PRE-${randomString(8,alnum)}-${enum(A,B,C)}` → `PRE-k3J9xQ2m-B`
  - `${orderId}-${timestamp(yyyyMMdd)}` → 引用前面步骤生成的变量
- 与现有 `${var}` 不冲突：变量名只允许 `[\w.\[\]-]+`（无括号），函数调用必带 `()`，解析器据此区分两者。

---

## 3. 后端实现思路

### 3.1 函数注册表（白名单）
- 定义接口/抽象：`GeneratorFunc { String name(); String apply(List<String> rawArgs); }`
- 维护 `Map<String, GeneratorFunc> registry`，仅注册表内的函数名才可执行。
- **初版内置函数**（见第 5 节表格），后续新增函数只改注册表一处，前端通过 `/functions` 自动感知。

### 3.2 表达式解析器（GeneratorEngine）
- 正则提取函数调用：`\${([a-zA-Z_]\w*)\s*\(([^}]*)\)}`
- 参数解析：按逗号切分；每项 trim 后判断是数字 / 引号字符串 / 嵌套 `${...}`，嵌套项先递归解析。
- **两遍解析顺序**：
  1. 第一遍：把模板里 `${已存在变量}` 用 `VariableResolver` 换成变量池当前值（支持复用前置步骤结果）。
  2. 第二遍：扫描所有 `${func(args)}`，查注册表执行，参数经类型/范围校验后返回字符串，拼回模板。
- **安全与边界（必做）**：
  1. 仅认注册表函数名；未知函数抛「不支持的函数 xxx」，天然杜绝任意代码执行。
  2. **绝不使用 `eval` / `GroovyShell` / `ScriptEngine`**——手写解析器。
  3. 递归/循环引用检测：`depth ≤ 10`（与现有 `VariableResolver.MAX_DEPTH` 一致），模板引用自身变量要拦截防死循环。
  4. 参数上下限：`randomInt` 的 `max>min`、`randomString` 的 `length ≤ 64`（防 `${randomString(99999999)}` 爆内存）、字符串长度上限。
  5. 解析失败给精确报错（哪一段、什么错）。
- **与现有替换点合流**：在 `ExecuteServiceImpl` 调用 `variableResolver.resolve(...)` 处，先跑 `GeneratorEngine.parse(...)`，再交给 `VariableResolver`。这样方案 A（请求参数里直接写 `${randomInt(8)}`）零前端改动即生效，方案 B（生成器步骤）只是把模板存进配置。

### 3.3 生成器表（持久化）
```sql
CREATE TABLE `tb_data_generator` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `project_id`  BIGINT       NOT NULL COMMENT '项目隔离',
  `name`        VARCHAR(100) NOT NULL COMMENT '生成器名称',
  `type`        VARCHAR(20)  NOT NULL COMMENT 'RANDOM/PHONE/IDCARD/NAME/ENUM/TIMESTAMP/UUID/CUSTOM',
  `params`      JSON         DEFAULT NULL COMMENT '结构化参数(随type)或CUSTOM的template',
  `description` VARCHAR(500) DEFAULT NULL,
  `create_by`   VARCHAR(50)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据生成器表';
```
- `type=RANDOM`：`params = {length, charset(digits|alpha|alnum), prefix, suffix}`
- `type=CUSTOM`：`params = {template: "NO-${randomInt(6)}"}`
- 其余类型存各自参数 JSON（手机号/身份证号地区码、枚举候选列表、时间戳格式+偏移等）。

### 3.4 步骤接入（step_type = 3）
- 扩展 `tb_api_component_step` 与 `tb_case_step` 的 `step_type` 枚举：新增 `3`（生成变量）。
- 新增步骤字段（建议复用 `request_override` JSON 列，避免改表结构）：
  `{ generatorId, variableName, regenEachRun }`
  或新增独立列 `generator_id BIGINT`、`variable_name VARCHAR(100)`、`regen_each_run TINYINT`。
- 执行引擎 `expandOne` 遇 `step_type=3`：
  1. 查 `tb_data_generator` 取配置；
  2. 调 `GeneratorEngine` 产出值；
  3. 写入 `Variables` 变量池 `put(variableName, value)`（与 `response_var` 同一机制）；
  4. 后续步骤用 `${variableName}` 引用。
- **"每次执行重新生成"开关** `regenEachRun`：
  - `true`（默认）：每次都现生成（最常用，保证每轮不同）；
  - `false`：整个执行过程固定同一值（缓存一次，适合多步骤引用同一随机值的一致性场景）。

### 3.5 后端接口（Controller）
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/base/generator/list` | 按 `type` / `name` 查询 + 分页（对应列表页查询） |
| POST | `/api/base/generator` | 新增生成器（对应"添加生成器"） |
| PUT | `/api/base/generator/{id}` | 编辑 |
| DELETE | `/api/base/generator/{id}` | 删除（逻辑删除，沿用 `deleted`） |
| GET | `/api/base/generator/functions` | 返回函数名+参数+说明+示例（前端帮助/语法提示） |
| POST | `/api/base/generator/preview` | 传 template 或 type+params，返回试生成结果 |

---

## 4. 前端实现思路

### 4.1 路由与菜单
- 新增路由 `/base/generator`，挂在左侧「基础数据」菜单下，与「接口库」同级。
- 复用现有列表/表单页骨架（参考 `base/component/index.vue` + `base/component/edit.vue`）。

### 4.2 列表 / 查询页（`generator/index.vue`）
- 顶部：`类型`下拉（手机号/姓名/身份证号/随机数/时间戳/枚举/UUID/自定义）+ `名称`输入框 + 查询按钮。
- 表格列：名称、类型、参数摘要、**示例值**（每次点「试生成」刷新一次，确认格式合规）、描述、操作（编辑/删除）。
- 右上「+ 添加生成器」进入表单页。

### 4.3 新增 / 编辑表单页（`generator/edit.vue`）
- 字段：名称、**类型**、描述。
- **类型相关参数区（随类型动态切换）**：
  - RANDOM：长度 length + 字符集 charset（纯数字/字母/字母数字）+ 前缀/后缀 → **直接覆盖"固定长度随机数"需求**。
  - PHONE / IDCARD / NAME / UUID：基本无参数（身份证号可选项：地区码/出生日期/性别）。
  - ENUM：候选值列表（多行输入）。
  - TIMESTAMP：格式 + 偏移量（如 `+1d`）。
  - **CUSTOM**：模板输入框 + 右侧「可用函数」清单（点一下插入）+ 「试生成」按钮。
- 右侧「实时预览」区：调 `/preview` 展示生成结果，与设计一致。
- **schema 驱动（预留）**：参数区由后端 `/functions` 返回的 schema 动态渲染，未来加新类型只改后端，前端自动长出表单（对应"表达式模板"可扩展性）。

### 4.4 组合组件编辑页接入（`base/component/edit.vue`）
- 第 200–227 行「选择组件类型」弹框的第二张卡「其他类型·生成随机数」（`disabled` + `onOtherTypeClick` toast）：
  - 去 `disabled`，改文案为「**生成变量（数据生成器）**」+「可用」标签；
  - 点击跳转 `/base/generator`（携带"返回组件页"参数）；
  - 选/建完成后返回，向 `form.steps` 推一条 `stepType:3`（引用 `generatorId` + 指定 `variableName` + `regenEachRun`）；
  - 左栏步骤树对 `stepType=3` 显示紫色「变量」标签 + 变量名（与用例页一致）。

### 4.5 用例编辑页接入（`case/edit.vue`）
- 「添加步骤」弹框类型选项从「单接口 / 组合组件」扩展为「单接口 / 组合组件 / **生成变量**」。
- `stepType=3` 详情区：生成器下拉（调 `/list`）+ 输出变量名 + 「每次执行重新生成」开关。
- 保存载荷 `StepParams` 增加 `stepType=3` 分支。

### 4.6 共享子组件
- 抽 `StepGeneratorForm.vue`：生成变量配置表单在 `case/edit.vue` 与 `component/edit.vue` 两处共用，避免重复逻辑。

---

## 5. 函数清单（初版）

| 函数 | 参数 | 说明 | 示例 |
|---|---|---|---|
| `randomInt` | (min,max) | 范围内整数 | `${randomInt(1000,9999)}` |
| `randomFloat` | (min,max[,scale]) | 浮点数 | `${randomFloat(0,1,2)}` |
| `randomString` | (len,charset) | 定长随机串；charset=digits/alpha/alnum | `${randomString(8,alnum)}` |
| `uuid` | () | UUID | `${uuid()}` |
| `phone` | () | 合法 11 位手机号（号段随机、格式合规） | `${phone()}` |
| `idCard` | ([region,birth,gender]) | 带 **GB11643 校验位**的 18 位身份证（可选地区码/出生日期/性别） | `${idCard()}` |
| `name` | () | 随机中文姓名 | `${name()}` |
| `enum` | (a,b,c...) | 从候选集随机取一个 | `${enum(A,B,C)}` |
| `timestamp` | ([format,offset]) | 当前时间；format 默认 ms，可 `yyyyMMdd HH:mm:ss`；offset 如 `+1d` | `${timestamp(yyyyMMdd)}` |

> 身份证号务必带合法校验位，否则被测接口的合法性校验会直接失败。

---

## 6. 安全性与边界（汇总）
- 白名单函数 + 手写解析，**禁用任何脚本引擎**。
- 参数上下限校验（防 OOM / 死循环）。
- 循环/递归引用检测（depth ≤ 10）。
- 解析失败精确报错，便于用户修正模板。
- 生成器按 `project_id` 隔离，避免跨项目串数据。

---

## 7. 落地阶段（建议顺序）

| 阶段 | 内容 | 前端改动 | 对应方案 |
|---|---|---|---|
| **阶段 1** | `GeneratorEngine` + 函数注册表 + `/preview` 接口，挂到 `ExecuteServiceImpl` 替换点 | **零**（参数框里直接写 `${randomInt(8)}` 即可） | 方案 A |
| **阶段 2** | `tb_data_generator` 表 + CRUD 接口（list/create/edit/delete） | 列表页 + 表单页（结构化类型） | 方案 B 结构化 |
| **阶段 3** | `step_type=3` 执行分支（写变量池 + `regenEachRun` 开关）+ 步骤表字段 | 组件页/用例页接入 stepType=3 | 方案 B 步骤级 |
| **阶段 4** | `CUSTOM` 类型 + `/functions` 接口 + 前端模板表单（schema 动态渲染 + 试生成） | 表单页 CUSTOM 分支 | 方案 B 表达式型 |
| **阶段 5** | 组件编辑页「其他类型」卡片跳转生成器页；用例页"生成变量"入口 | 卡片去 disabled + 返回写入步骤 | 衔接闭环 |

> 阶段 1 即可让"随机参数"在 pre/post/main 立即生效，风险最低，建议最先落地验证。

---

## 8. 待确认项
1. **随机值作用域**：默认"每次现生成"（推荐），是否需要"整个执行固定同一值"由 `regenEachRun` 开关控制——默认开还是关？
2. **可复现性**：是否需要随机种子（便于调试回放）？默认纯随机。
3. **charset 范围**：`digits / alpha / alnum` 是否够用，是否要加 `upper / lower` 细分。
4. **生成器隔离粒度**：按 `project_id` 隔离（推荐），还是也允许跨项目共享"公共生成器"。
5. **步骤字段落库方式**：复用 `request_override` JSON 列，还是新增独立列——倾向新增独立列，语义更清晰。
