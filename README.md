# ATP 自动化测试平台

前后端分离的接口自动化测试平台。前端 Vue3 + TypeScript + Vite，后端 Java 17 + Spring Boot 3。

> 当前进度：**第三阶段（中后段）**
> 用户认证、项目管理、基础数据、接口定义、环境配置、用例编排、数据源、执行引擎已完成；
> 执行记录落库、测试计划、报告中心尚未实现。
>
> 完整进度、表结构、接口清单与已知问题见 → [`docs/HANDOVER.md`](docs/HANDOVER.md)

---

## 一、已实现功能

| 模块 | 能力 |
| --- | --- |
| 用户认证 | 注册、登录、登出、图形验证码、JWT 鉴权、角色体系（ADMIN/TESTER/VIEWER） |
| 项目管理 | 项目卡片列表、新增项目、登录后选择项目（localStorage 持久化）、切换项目 |
| 基础数据 | 工程 / 版本 / 模块三级级联；接口定义列表、手动新增、**Jar 包导入**（解析 Spring MVC 注解） |
| 环境配置 | 环境 CRUD：base_url、全局 header、数据库配置 |
| 用例管理 | 用例 CRUD、启停、**多步骤串行编排**、**HAR 包导入生成用例** |
| 数据源管理 | 数据源模板 CRUD、字段(key) 定义、数据项多行编辑（用例执行时按行替换变量） |
| 执行引擎 | HTTP 执行、变量解析、断言校验、多轮数据驱动；用例编辑页提供「调试运行」 |

**核心数据层级**

```
项目 Project → 工程 Application → 版本 Version → 模块 Module → 接口 ApiDefinition
                                                              ↓
                                                    用例 TestCase → 步骤 CaseStep
```

---

## 二、目录结构

```
auto-test-platform/
├── docs/
│   ├── ARCHITECTURE.md      # 架构设计（技术选型、分层、表结构、接口规范）
│   ├── HANDOVER.md          # 交接文档：进度 / 接口清单 / 已知问题 / 下一步
│   └── thinks.md            # 每轮问题处理的思考过程与决策记录
├── backend/                 # Spring Boot 3
│   └── src/main/
│       ├── java/com/atp/
│       │   ├── common/      # 统一返回体、异常、工具类
│       │   ├── config/      # Security / 跨域 / MyBatis-Plus / Jackson 配置
│       │   ├── security/    # JWT 签发、认证过滤器、登录主体
│       │   └── module/      # 业务域，互不横向依赖
│       │       ├── user/     project/    base/
│       │       ├── env/      testcase/   dataset/   execute/
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql
└── frontend/                # Vue 3
    └── src/
        ├── api/             # request.ts 拦截器 + 各业务 API
        ├── layout/          # 框架布局（侧边栏 + 顶栏）
        ├── router/          # 路由与登录守卫
        ├── stores/          # Pinia 状态（user / project / tabs）
        ├── views/           # 登录 / 注册 / 工作台 / 基础数据 / 用例 / 数据源 / 环境
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
```

脚本会创建 `atp` 库、全部业务表，并插入初始管理员账号。

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
| 项目 | `/api/project` | list / 新增 |
| 基础数据 | `/api/base` | 工程·版本·模块 options 与新增、version/list、api/list、api、api/import（Jar） |
| 用例 | `/api/case` | list / {id} / {caseId}/steps / 新增 / 修改 / 删除 / {id}/status / import（HAR） |
| 数据源 | `/api/dataset/template` | list / {id} / 新增 / 修改 / 删除 |
| 环境 | `/api/env` | list / 新增 / 修改 / 删除 |
| 执行 | `/api/execute/case/{caseId}` | 调试执行用例 |

> 完整 30 个接口的方法、路径与说明见 `docs/HANDOVER.md` 第七节。

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

**HAR 导入**
解析 `log.entries[]`，剥离敏感 header（Authorization / Cookie / Proxy-Authorization），
按 `module_id + method + path` 去重（已存在跳过、不存在则新建接口，`name` 填空串、`source_flag` 标记来源），
按时间排序生成步骤并自动带状态码断言。

---

## 八、后续阶段

| 阶段 | 内容 | 状态 |
| --- | --- | --- |
| 第一阶段 ~ 第二阶段 | 架构、鉴权、项目管理、基础数据、接口定义、环境、用例编排、数据源、执行引擎 | ✅ 已完成 |
| 第三阶段 | **执行记录落库**（`tb_execution` / `tb_execution_detail` 表已建，代码未实现） | 🔶 待补 |
| 第四阶段 | 报告中心、图表看板、测试计划与 Cron 调度 | ⬜ 未开始 |
| 第五阶段 | CI 集成、项目级 RBAC、并发执行与性能优化 | ⬜ 未开始 |

前端菜单中「测试计划」「报告中心」为 disabled 占位态。

---

## 九、开发注意事项

1. **Vite 开发服务器会假死**：表现为端口能连上但 HTTP 无响应，列表还在而弹窗数据全空。
   确认方式：`nc -z 127.0.0.1 5173` 通、`curl http://127.0.0.1:5173/` 超时。
   处理：`lsof -ti:5173 | xargs kill -9` 后重新 `npm run dev` 并硬刷新浏览器。**改业务代码无效。**
2. **`keys` 是 MySQL 保留字**，手写 SQL 必须加反引号 `` `keys` ``。
3. **前端类型校验需加大内存**：`NODE_OPTIONS=--max-old-space-size=4096 npx vue-tsc --noEmit`，否则 OOM。
4. **LocalDateTime 序列化**统一 `yyyy-MM-dd HH:mm:ss`；`spring.jackson.date-format` 只对 `Date` 生效。
5. 命名区分：项目 = `project`、工程 = `application`、版本 = `version`、模块 = `module`、接口 = `api`。
6. 本人（开发者）手动编辑过的代码，后续接手者请勿擅自改动；需调整时先沟通确认。

详见 [`docs/HANDOVER.md`](docs/HANDOVER.md)。
