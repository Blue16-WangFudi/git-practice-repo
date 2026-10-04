# Git 协作实践

本项目按“主分支稳定、功能分支开发、Pull Request 合并”的方式练习。当前本地仓库已经建立 `main` 主分支，并准备使用功能分支提交后续改动。

## 推荐分支

- `main`：可发布的稳定代码，只接受经过检查的 Pull Request。
- `develop`：多人协作时的集成分支；本项目规模较小时，可以暂时只使用 `main`。
- `release/vx.y.z`：发布候选版本，只做版本号、配置和阻断问题修复。
- `feat/xxx`：新功能，例如 `feat/service-management`。
- `fix/xxx`：缺陷修复，例如 `fix/redis-cache-key`。
- `docs/xxx`：文档改动，例如 `docs/git-practice`。

## 一次完整流程

```powershell
# 1. 从最新 main 创建功能分支
git switch main
git pull origin main
git switch -c feat/service-management

# 2. 开发后查看变更
git status
git diff

# 3. 在本地验证
git diff --check
cd backend
mvn -s maven-settings.xml test
cd ..\\monitor-web
npm run build
cd ..

# 4. 提交
git add backend monitor-web docs
git commit -m "feat: add service management"

# 5. 推送功能分支
git push -u origin feat/service-management
```

然后在 GitHub 创建 Pull Request：

1. base 选择 `main`，compare 选择功能分支。
2. 标题写清楚动词和范围，例如 `feat: add service management`。
3. 描述改了什么、为什么改、如何验证、是否有数据库或配置变更。
4. 等 GitHub Actions 通过，再请求代码审查。
5. 审查通过后使用 Squash merge，删除已经合并的功能分支。

## 不要提交的内容

`.env`、真实密码、Token、证书私钥、`node_modules`、Maven 缓存、Flutter 构建产物和 Docker 数据卷都不应提交。项目中的 `.env.example` 只保留变量名和演示值。

## 本地练习检查表

- 是否从最新的 `main` 创建了 `feat/` 或 `fix/` 分支？
- 提交是否只包含一个主题？
- 是否运行后端测试和前端构建？
- 是否检查了密钥、构建产物和无关文件？
- PR 是否写明验证结果和风险？
