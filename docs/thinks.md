# 思考过程记录

> 本文件记录每轮问题处理时的思考过程、分析与决策，便于回溯设计脉络。
> 格式：按「日期 + 主题」分节，每轮处理完成后 append。

---

## [2026-09-09] 用例「响应变量名」机制

### 问题
用例步骤之间如何传递参数？最初是「参数提取(extractors) + ${varName} 引用」两步分离，较繁琐；
且跨步骤引用（第1步响应在第3步用）时「上一步响应」语义会取错。

### 思考
- 引入「响应变量名」：每个步骤可给响应数据命名，执行后把完整响应（status/header/body）存入变量上下文。
- 引用语法：`${varName.path}` 默认从 body 根解析 JSONPath；`${varName.header.x}` 取响应头；`${varName.status}` 取状态码。
- 响应变量名可为空，为空则不保存该接口响应。

### 决策
- 前端已加「响应变量名」输入框；后端字段/执行引擎待实现。
- 「参数提取(extractors)」功能已被响应变量名完整覆盖，前端输入框已移除；后端 extractors 字段/逻辑暂保留以兼容旧数据。

---

## [2026-09-09] 思考过程记录机制

### 问题
用户要求：每轮问题处理后，把思考过程写入 docs/thinks.md。

### 决策
- 建立本文件，采用「日期 + 主题」分节，每轮处理完成后 append。
- 后续每轮回答/处理问题时，将本轮的思考过程追加到本文件末尾。

---

## [2026-09-09] 响应变量名后端实现与落库

### 问题
实现「响应变量名」的后端落库与执行引擎引用，并验证全链路。

### 思考
- 落库：`tb_case_step` 加 `response_var` 字段，实体/VO/DTO/Mapper SQL/saveSteps 同步。
- 执行引擎：请求成功后若 `responseVar` 非空，把 `{status, header, body}` 完整响应存进 Variables。
- 变量解析：`VariableResolver` 识别 `${varName.path}`，按 `status`/`header.x`/默认 body 根 三路解析（复用 Jayway JsonPath）。
- 数据库已建表，不能 DROP，用 `ALTER TABLE ADD COLUMN` 增量加字段。

### 踩坑
- `stringify` 对 Integer 用 `JSONUtil.toJsonStr` 会得到 `{}`，需对 Number/Boolean 走 `String.valueOf`。
- 验证时把中文字符（如 `${loginResp.message}`="操作成功"）放进 HTTP 请求头，触发 OkHttp「非 ASCII 头」校验异常；这是测试设计问题，非机制 bug。

### 决策
- 响应变量保存完整响应对象（status/header/body），不是只存 body。
- `${varName.path}` 默认 body 根、`${varName.header.x}` 取响应头、`${varName.status}` 取状态码。
- 正则扩展为支持 `[]`，可引用 `${varName.list[0].id}` 数组下标。

### 验证结果
- 落库：responseVar 保存 + 查询通过。
- 引用：`${loginResp.status}`=200、`${loginResp.message}`、`${loginResp.success}`、`${loginResp.header.Content-Type}` 均正确替换。

---

## [2026-09-09] 后端同步清理 extractors

### 问题
响应变量名机制已完整覆盖「参数提取」，后端 extractors 相关代码需同步清理（不保留字段兼容旧数据）。

### 决策
- 删除 `ExtractorEngine`、`ExtractResultVO` 两个类。
- 删除 `CaseStep`/`CaseStepVO`/`StepDTO` 的 `extractors` 字段、Mapper SQL、saveSteps、执行引擎 runExtractors、StepExecuteVO.extractResults。
- 数据库 `ALTER TABLE tb_case_step DROP COLUMN extractors`。
- 前端同步删除 `ExtractResultItem`/`ExtractorRule`/`extractors`/`extractResults` 及抽屉展示、CSS。

### 踩坑
- 删除 edit.vue 校验块时 new_text 写错，留下孤立的 `activeStepIndex.value = i; return; }`，导致 TS1128 编译错误；修正后恢复。

### 验证结果
- 后端编译通过、前端 build 通过、后端启动正常。
- 用例步骤接口不再返回 extractors 字段；response_var 联查正常。

---

## [2026-09-09] 数据源模块拆分（模板 + 数据项）

### 问题
数据源模块需拆分为「数据源模板」和「数据项」两个表：模板只存字段 key（含描述），数据项存具体数据。

### 决策
- 表拆分：`tb_dataset_template`（case_id/name/keys/creator_name）+ `tb_dataset_item`（template_id/data/sort_order），废弃 `tb_case_dataset`。
- keys 存储 JSON 数组 `[{"key":"username","desc":"用户名"}]`，key 与 desc 都落库。
- 执行引擎：按 case_id 查模板 → 合并所有模板数据项 → 多轮执行。
- 后端新增 `/api/dataset/template/**` CRUD；前端字段用 textarea（每行 `key:描述`），数据项表格列只读、行可增删、整体提交。

### 踩坑
- `keys` 是 MySQL 保留字，MyBatis-Plus 生成 SQL 语法错误 → 加 `@TableField("`keys`")`。
- 数据项弹窗保存时曾重建 keys（desc 置空），导致字段描述丢失 → 改为缓存并回传原始 keys。

### 验证结果
- 新增模板（keys + 2 数据项）落库、列表/详情查询、执行用例多轮（totalRounds=2）均通过。

