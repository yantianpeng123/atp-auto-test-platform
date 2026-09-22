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
          def status = 'RUNNING'
          timeout(time: 30, unit: 'MINUTES') {
            while (status == 'RUNNING') {
              sleep 10
              def resResp = sh(script: "curl -s ${ATP_BASE}/api/ci/result/${runId} -H 'X-CI-Token: ${ATP_TOKEN}'", returnStdout: true).trim()
              def res = new groovy.json.JsonSlurper().parseText(resResp)
              status = res.status
              def passed = res.passed ?: 0
              def failed = res.failed ?: 0
              echo "status=${status} passed=${passed} failed=${failed}"
              if (status == 'FAILED' || (failed as int) > 0) { error("ATP 回归失败: 通过 ${passed} / 失败 ${failed}") }
            }
          }
        }
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
2. 新增 `tb_ci_config` 表 + `POST /api/ci/trigger` + `GET /api/ci/result/{runId}`（带 `X-CI-Token` 校验）。
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
