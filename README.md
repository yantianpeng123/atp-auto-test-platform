# ATP 自动化测试平台

前后端分离的自动化测试平台。前端 Vue3 + TypeScript + Vite，后端 Java 17 + Spring Boot 3。

> 当前进度：**第一阶段** —— 架构梳理 + 后端骨架 + JWT 鉴权 + 登录/注册页面

---

## 一、目录结构

```
auto-test-platform/
├── docs/
│   └── ARCHITECTURE.md          # 架构设计文档（技术选型、分层、表结构、接口规范）
├── backend/                     # 后端 Spring Boot 3
│   └── src/main/
│       ├── java/com/atp/
│       │   ├── common/          # 统一返回体、异常、工具类
│       │   ├── config/          # Security / 跨域 / MyBatis-Plus 配置
│       │   ├── security/        # JWT 签发、认证过滤器、登录主体
│       │   └── module/user/     # 用户模块（entity/mapper/service/controller/dto/vo）
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql    # 建库建表脚本
└── frontend/                    # 前端 Vue 3
    └── src/
        ├── api/                 # 接口封装（request.ts 拦截器 + 各业务 API）
        ├── layout/              # 框架布局（侧边栏 + 顶栏）
        ├── router/              # 路由与登录守卫
        ├── stores/              # Pinia 状态（user.ts）
        ├── views/               # 登录 / 注册 / 工作台
        └── utils/               # 令牌存取
```

---

## 二、环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 本机 20 已验证通过 |
| Maven | 3.6.3+ | |
| Node.js | 18+ | 本机 22 已验证通过 |
| MySQL | 8.0 | 需要能本地连接 |
| Redis | 6+ | 用于验证码与令牌黑名单，**非强依赖** |

---

## 三、启动步骤

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

Redis 不启动时平台仍可运行：验证码会自动隐藏，令牌黑名单降级为仅前端清除。
登录、注册、鉴权全部正常可用。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

服务监听 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。`/api` 请求已通过 Vite 代理转发到 8080。

---

## 四、演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 平台管理员（ADMIN） |

登录页提供「一键填充」按钮。注册的新用户默认角色为 `TESTER`。

---

## 五、第一阶段接口清单

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 否 | 用户注册 |
| POST | `/api/auth/login` | 否 | 用户登录，返回 JWT |
| POST | `/api/auth/logout` | 是 | 登出，令牌加入黑名单 |
| GET | `/api/auth/captcha` | 否 | 图形验证码（Base64） |
| GET | `/api/user/info` | 是 | 当前登录用户信息 |
| GET | `/api/user/check-username` | 否 | 用户名是否被占用 |

统一返回体：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1756780800000
}
```

---

## 六、关键设计说明

**为什么验证码不是必填？**
验证码依赖 Redis 存储。若强制校验，Redis 故障会直接堵死注册和登录。
因此后端策略是：前端传了 `captchaKey` + `captchaCode` 才校验，不传则跳过；
前端在挂载时尝试拉取验证码，成功才展示验证码输入框，失败则静默隐藏。
生产环境如需强制开启，把 `AuthServiceImpl.verifyCaptcha()` 中的空值判断改为抛异常即可。

**密码安全**
BCrypt 加盐哈希，永不明文存储、永不通过接口返回。用户信息统一走 `UserInfoVO` 脱敏。

**令牌方案**
JWT 载荷含 `userId` / `username` / `role`，有效期 2 小时，密钥在 `application.yml` 的 `atp.jwt.secret`（生产环境务必改为环境变量注入）。
登出时把令牌写入 Redis 黑名单，TTL 为其剩余有效期。

---

## 七、后续阶段

| 阶段 | 内容 |
| --- | --- |
| 第二阶段 | 项目管理、环境管理、接口定义、用例增删改查 |
| 第三阶段 | HTTP 执行引擎、断言校验、测试计划、Cron 调度 |
| 第四阶段 | 报告中心、图表看板、邮件与企业微信通知 |
| 第五阶段 | CI 集成、项目级 RBAC、并发执行与性能优化 |

详见 `docs/ARCHITECTURE.md`。
