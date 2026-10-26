# 开发规范（Development Guide）

## 1. 每日分支约定（强制）

**在开始每一天的写代码之前，必须先新建一个 Git 分支**，分支命名格式：

```
atp-yyyy-MM-dd
```

- `yyyy-MM-dd` 为当天日期（本地时区）。
- 示例：2026 年 10 月 26 日的分支名为 `atp-2026-10-26`。

### 操作方式

```bash
# 基于当前 main 创建并切换到当天分支
git checkout main
git pull origin main          # 先同步远端最新代码
git checkout -b atp-2026-10-26

# 当天所有改动都提交到该分支
git add -A
git commit -m "feat: ..."
```

### 约定要点

1. **先建分支，再写代码**：不要在 `main` 上直接开发。
2. **一天一个分支**：同一天的工作集中在一个 `atp-yyyy-MM-dd` 分支；跨天后改用新日期分支。
3. **分支从最新 `main` 拉取**：建分支前先 `git pull origin main`，避免基于过期代码开发。
4. **合并/推送**：当天工作完成后，将 `atp-yyyy-MM-dd` 分支合并回 `main` 并推送到 `origin/main`（具体合并策略以团队约定为准）。
5. **若分支已存在**：说明当天已建过分支，直接 `git checkout atp-2026-10-26` 复用，不要重复创建。

---

## 2. 其他通用约定

- 全局逻辑删除：`deleted=0` 有效，使用 `@TableLogic`。
- 接口鉴权：`/api/**` 全部需要 JWT，新接口无需额外加白名单。
- 前端编译校验：`vue-tsc --noEmit`（`noUnusedLocals=true`）。
- 后端编译校验：`mvn -q compile`。
- Vue 模板陷阱：`@click="fn"` 不带括号会把原生事件对象作为首个参数传入；handler 声明了参数必须写 `@click="fn()"`。
