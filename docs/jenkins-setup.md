# Jenkins 安装与 Pipeline 配置指南（对接 ATP 平台）

> 适用：本机已装好 Jenkins（端口 9000），需要创建 Pipeline 流水线对接 ATP 平台的 `/api/ci/*` 接口触发回归测试。
> 前置：Jenkins 9000 端口是 **Jenkins 自己的 UI 端口**，与 ATP 后端端口（如 8080 / 51139）无关。

---

## 一、安装 Pipeline 插件（确切点击路径）

### 方式 A：初始化时已装（推荐插件集）
如果当初创建管理员账号时选了 **"Install suggested plugins"**，Pipeline 已经包含在推荐插件里，**直接跳到 第四节验证**。

### 方式 B：手动安装
1. Jenkins 首页 → 左侧菜单 **Manage Jenkins（系统管理）**
2. 点击 **Plugins（插件管理）**（旧版叫 Manage Plugins）
3. 切换到 **Available（可选插件）** 标签页
4. 搜索框输入 **`Pipeline`** 并回车
5. 在结果里勾选 **Pipeline**（第一项，描述含 *Build Pipeline* / *workflow*；勾选它会自动连带安装 `workflow-aggregator`、`Pipeline: Stage View` 等依赖）
   - 也可直接搜 **`workflow-aggregator`** 装聚合包，效果一样
6. 点击 **Install without restart**（安装且不重启）**或**
   **Download now and install after restart**（下载并重启后生效）
   - 建议选后者，并勾选 **"Restart Jenkins when installation is complete and no jobs are running"**
7. 等待页面进度条走到 100%、状态变 Success

> 可选增强插件（后续用例级报告用）：**JUnit Plugin**（Jenkins 内置一般已有）、**HTTP Request Plugin**（pipeline 用 `httpRequest` 调用 ATP 接口时用到；本文 Jenkinsfile 用 `curl` 故非必须）。

---

## 二、重启 Jenkins

- 若上一步勾选了「安装后重启」，会自动重启，等待 ~30s 重新可访问即可。
- 手动重启方式（任选）：
  - Docker 起的话：`docker restart jenkins`
  - 原生/brew 起的话：浏览器访问 `http://localhost:9000/restart`（或管理页点 **Safe Restart**）
- 重启完成后重新登录。

---

## 三、重启后验证插件生效

1. 首页 → **New Item（新建任务）**
2. 输入任务名（如 `atp-regression`）
3. **类型列表中应出现「Pipeline」选项**（蓝色流水线图标）→ 选中 → 点 **OK**
4. 进入任务后，点左侧 **Configure（配置）**
5. 滚动到页面**最底部**，应看到 **Pipeline** 区块，内含：
   - `Definition`（定义）下拉，有两个选项：
     - **Pipeline script**（内联粘贴脚本，联调最快，推荐先用这个）
     - **Pipeline script from SCM**（从 Git 仓库读 Jenkinsfile，长期推荐）
6. 能看到该区块 = 插件安装成功 ✅

> 如果 New Item 里**根本没有 Pipeline 类型**，或 Configure 底部**没有 Pipeline 区块**，说明插件没装上 → 回到第一节重装并确认重启成功。

---

## 四、创建最小可跑 Pipeline（先验证网络，再接 ATP）

### 步骤 1：先只放一个 Smoke stage，确认 Jenkins 能访问到 ATP
在 Configure → Pipeline → Definition 选 **Pipeline script**，粘贴：

```groovy
pipeline {
  agent any
  environment {
    ATP_BASE = 'http://localhost:8080'   // 改成 ATP 后端真实端口（Docker 起的 Jenkins 用 http://host.docker.internal:8080）
  }
  stages {
    stage('Smoke: ATP reachable') {
      steps {
        script {
          // 只探测连通性，连不通也继续（打印状态码，不中断流水线）
          def code = sh(script: "curl -s -o /dev/null -w '%{http_code}' ${ATP_BASE}/api/auth/login || echo 000", returnStdout: true).trim()
          echo "ATP login HTTP ${code}"
          if (code == '000' || code.startsWith('4') || code.startsWith('5')) {
            echo "WARN: ATP 不可达或返回错误码，请检查 ATP_BASE 与端口/网络"
          }
        }
      }
    }
  }
}
```

点 **Save** → **Build Now**。控制台输出应看到 `ATP login HTTP 200`（或能连通的状态码），说明网络通了。

### 步骤 2：网络通后，替换为完整触发脚本（依赖平台侧 `/api/ci/*` 已就绪）

> ⚠️ **前置条件**：以下脚本依赖 ATP 平台已实现 `POST /api/ci/trigger` + `GET /api/ci/result/{runId}`（含批次执行异步化）。**该接口尚未实现前请勿运行此阶段**，否则会 404 / 解析失败导致 Stage Failure。实现前先用步骤 1 的 Smoke 阶段验证网络即可。

完整脚本（用 Groovy 内置 `JsonSlurper` 解析，无需容器内装 python3）：

```groovy
pipeline {
  agent any
  environment {
    ATP_BASE   = 'http://localhost:8080'      // Docker 起的 Jenkins 用 http://host.docker.internal:8080
    PROJECT_ID = '3'
    BATCH_ID   = '1'
    ATP_TOKEN  = credentials('atp-ci-token')   // 在 Jenkins Credentials 里建的 Secret text
  }
  stages {
    stage('Trigger ATP regression') {
      steps {
        script {
          def trigResp = sh(script: "curl -s -X POST ${ATP_BASE}/api/ci/trigger -H 'X-CI-Token: ${ATP_TOKEN}' -H 'Content-Type: application/json' -d '{\"projectId\":${PROJECT_ID},\"batchId\":${BATCH_ID}}'", returnStdout: true).trim()
          echo "trigger resp: ${trigResp}"
          def trig = new groovy.json.JsonSlurper().parseText(trigResp)
          def runId = trig.runId
          echo "runId=${runId}"
          env.RUN_ID = runId.toString()
          def status = 'RUNNING'
          timeout(time: 30, unit: 'MINUTES') {
            while (status == 'RUNNING') {
              sleep 10
              def resResp = sh(script: "curl -s ${ATP_BASE}/api/ci/result/${runId} -H 'X-CI-Token: ${ATP_TOKEN}'", returnStdout: true).trim()
              def res = new groovy.json.JsonSlurper().parseText(resResp)
              status = res.status
              echo "status=${status} passed=${res.passed ?: 0} failed=${res.failed ?: 0}"
            }
          }
        }
      }
    }
    stage('Fetch JUnit report & publish') {
      steps {
        // 拉取 JUnit 格式报告（含用例级与每条步骤的断言明细），交给原生 junit 步骤渲染并判定红绿
        withCredentials([string(credentialsId: 'atp-ci-token', variable: 'ATP_TOKEN')]) {
          sh "curl -s ${ATP_BASE}/api/ci/report/${env.RUN_ID}.xml -H 'X-CI-Token: ${ATP_TOKEN}' -o atp-report.xml"
        }
        junit 'atp-report.xml'
      }
    }
  }
}
```

---

## 五、凭据配置（接 ATP 时用到）

1. 首页 → **Manage Jenkins → Credentials（凭据）** → **System → Global credentials**
2. 点 **Add Credentials**
3. Kind 选 **Secret text**，ID 填 `atp-ci-token`，Secret 填平台侧生成的 CI Token
4. 保存。Jenkinsfile 里用 `credentials('atp-ci-token')` 引用，**不要硬编码** Token。

---

## 六、网络连通要点（最容易踩坑）

| 场景 | ATP_BASE 取值 |
| --- | --- |
| Jenkins 原生安装（与 ATP 同机） | `http://localhost:<atp端口>` |
| Jenkins 跑在 Docker 容器里 | `http://host.docker.internal:<atp端口>` |
| ATP 经 WorkBuddy 隧道运行 | 用实际端口（如 51139），容器场景用 `host.docker.internal:51139` |

验证命令：`docker exec jenkins curl -s http://host.docker.internal:8080/api/auth/login`（Docker 场景能通即代表网络打通）。

---

## 七、与 ATP 平台侧的衔接

Jenkins 侧跑通的前提是 **ATP 平台已实现 CI 入站接口**，核心依赖：

1. **批次执行异步化**（P1 阻塞项）：`POST /api/plan/batch/{id}/execute` 当前是**同步**跑完才返回，长批次会 HTTP 超时。需改造为「触发即返回 runId + 后台执行」。（平台侧 `/api/ci/result/{runId}` 轮询端点基于现有 `/api/plan/batch/run/{runId}`，可复用。）
2. 新增 `tb_ci_config` 表 + `POST /api/ci/trigger` + `GET /api/ci/result/{runId}` + `GET /api/ci/report/{runId}.xml`（均带 `X-CI-Token` 校验；report 端点吐 JUnit 标准 XML，供 Jenkins 原生 `junit` 步骤渲染用例级报告与每条步骤的断言明细）。
3. `SecurityConfig` 白名单放行 `/api/ci/**`。

> 平台侧最小闭环未实现前，可临时用「Jenkins 先 login 拿 JWT → 调现有同步批次执行接口」做验证，但长批次仍会超时，故异步化是硬前置。

---

## 八、排错速查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| New Item 没有 Pipeline 类型 | 插件未装 | 回第一节重装 + 重启 |
| Configure 底部无 Pipeline 区块 | 插件未生效/未重启 | 确认重启成功，重进配置页 |
| Smoke stage curl 连不通 ATP | 端口/网络错 | 核对 ATP_BASE；Docker 用 host.docker.internal |
| 触发接口超时 | 批次同步执行 | 先做平台侧异步化（第七节） |
| `credentials` 报错 | 凭据 ID 不匹配 | 确认 Credentials 里 ID 是 `atp-ci-token` |
| Stage Step Failure + `python3: command not found` | 容器无 python3 | 改用 Groovy `JsonSlurper` 解析（步骤 2 已修正，无需改脚本） |
| Stage Step Failure + `JsonSlurper` 抛异常 / 返回 HTML | 平台 `/api/ci/*` 未实现（404） | 该阶段前置未满足，先只跑 Smoke 阶段；待平台接口就绪再用 |
| Stage 红但无明细 | 未看 Console Output | 点失败阶段 → Logs，第一行即根因 |

---

## 九、配置「代码推送自动触发」流水线（Build Triggers + Webhook）

> 第四节的 Pipeline 之前是手动点 **Build Now**。要让 push / merge 自动跑，需要在**两处**配合：Jenkins 任务里的「构建触发器」 + 代码仓库里的「Webhook」（或改用轮询，无需公网）。

### 9.1 Jenkins 任务里要配的 3 处
1. **Pipeline → Definition 选 `Pipeline script from SCM`**（关键）：Pipeline 类型任务**没有顶部独立的「Source Code Management」区块**，Git 配置就藏在 Pipeline 定义里。把 Definition 从默认的 `Pipeline script` 改成 `Pipeline script from SCM` 后，区块内才会出现 **Git** 的 SCM 配置（Repository URL + 凭据 + Branch）。
   > 注意：如果 Jenkinsfile 是内联脚本（直接 curl 调 ATP、不需要仓库代码），**可以跳过这一步**，保持 `Pipeline script` 即可——Webhook / 定时触发照样能工作。只有当 Jenkinsfile 放在仓库里管理时才需要配 Git。
2. **Build Triggers**（按仓库类型选一种）：
   - **GitHub 仓库**：勾选 **GitHub hook trigger for GITScm polling**（需装 `GitHub` 插件，推荐插件集已含）。
   - **GitLab 仓库**：勾选 **Build when a change is pushed to GitLab**（需装 `GitLab` 插件，并在「高级」里生成 Secret token）。
   - **不想把 Jenkins 暴露到公网**：勾选 **Poll SCM**，填 cron 如 `*/5 * * * *`（每 5 分钟查一次，有延迟但无需公网可达）。
3. **Global Credentials 里的 `atp-ci-token`**（第五节已建，这里复用）：Jenkinsfile 用 `credentials('atp-ci-token')` 引用，无需改动。

### 9.2 代码仓库里加 Webhook（仅 Webhook 方式需要）
- **GitHub**：仓库 **Settings → Webhooks → Add webhook** → Payload URL 填 `http://<jenkins地址>/github-webhook/`（注意结尾斜杠）→ Content type 选 `application/json` → Which events 选 **Push**（PR / merge 按需再加）→ Add webhook。
- **GitLab**：项目 **Settings → Webhooks** → URL 填 `http://<jenkins>/project/<任务名>` → 勾选 **Push events**（Merge 按需）→ Secret token 填与 9.1 生成的一致 → Add webhook。

### 9.3 关键前提：Jenkins 必须能被仓库访问到
- GitHub / GitLab 在云端，若 Jenkins 跑在本机 `localhost:9000`，云端仓库**访问不到**，Webhook 永远不会触发。
- 解决三选一：
  1. **内网穿透**（ngrok / cloudflared）给 Jenkins 一个公网 URL，Webhook 填这个地址。
  2. 改用 **Poll SCM**（9.1 第 2 点），无需公网，接受几分钟延迟。
  3. Jenkins 与仓库同处一个可达内网 / VPN。

### 9.4 验证
- **Webhook 方式**：push 一次分支，回 Jenkins 看任务是否自动出现新 Build；仓库 Webhooks 页可看 Recent Deliveries 状态（200 = 成功）。
- **Poll SCM 方式**：等一个 cron 周期（如 5 分钟）看是否自动触发。
- Console Output 第一行应是拉取代码，随后进入 **Trigger ATP regression** 阶段。

---

## 十、内联脚本 + GitHub Webhook 最简触发清单（照勾即可）

> 适用：Jenkinsfile 直接 `curl` 调 ATP，不需要拉仓库代码。全程内联，最省事。

### 10.1 Jenkins 侧（任务 Configure 页，逐项勾）
- [ ] **New Item**：任务名自取（如 `atp-regression`）→ 类型选 **Pipeline** → OK
- [ ] **General**：默认，无需改动
- [ ] **Build Triggers**：勾 **GitHub hook trigger for GITScm polling**
- [ ] **Pipeline 区块（页面最底部）**：
  - Definition 选 **Pipeline script**
  - Script 框粘贴平台「CI 集成」页的示例 Jenkinsfile（或本文第四节步骤 2 脚本）
  - 核对 environment：`ATP_BASE`=平台地址、`PROJECT_ID`/`BATCH_ID`=对应 ID、`ATP_TOKEN = credentials('atp-ci-token')`
- [ ] 点 **Save**

### 10.2 GitHub 侧（仓库 Webhooks）
- [ ] 仓库 **Settings → Webhooks → Add webhook**
- [ ] Payload URL：`http://<jenkins公网地址>/github-webhook/`（**结尾斜杠必带**）
- [ ] Content type：`application/json`
- [ ] Which events：选 **Push**（PR / merge 按需再加）
- [ ] 勾 **Active** → **Add webhook**

### 10.3 前提：Jenkins 必须公网可达
- 本地 `localhost:9000` 云端仓库访问不到 → 用 **ngrok / cloudflared** 穿透，Webhook 填穿透后的地址。
- 不想穿透：把 10.1 的 Build Triggers 换成 **Poll SCM**，cron 填 `*/5 * * * *`，GitHub 侧省略 10.2。

### 10.4 潜在坑：内联脚本 + GitHub hook 可能不触发
GitHub plugin 的触发器最稳的匹配前提是任务配了 Git SCM。若按 10.1 用纯内联脚本、未配 SCM，部分版本下 Webhook 收得到但任务不自动跑。两个零成本备选：
- **备选 1（推荐，最通用）**：装 **Generic Webhook Trigger** 插件，Build Triggers 勾它并设 Token（如 `atp`），GitHub Webhook URL 改成 `http://<jenkins>/generic-webhook-trigger/invoke?token=atp`。不依赖 SCM，任意仓库都能触发。
- **备选 2**：把 Definition 改成 `Pipeline script from SCM`，把 Jenkinsfile 提交进仓库并配好 Git（第九节 9.1 第 1 点），GitHub hook 即可 100% 匹配。

### 10.5 验证
- push 一次分支 → 回 Jenkins 看任务是否自动冒出 Build。
- Console Output 进入 **Trigger ATP regression** 阶段 = 触发成功。
- GitHub Webhooks → Recent Deliveries 显示 200 = Webhook 已送达。
