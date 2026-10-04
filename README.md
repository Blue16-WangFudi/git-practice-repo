# Sentinel Monitor

Sentinel Monitor 是一个用于学习部署、性能分析、服务器巡检、缓存、消息队列和前后端协作的小型平台。

当前已完成的基础版本包含：

- Spring Boot 监控 API
- Python 指标采集器
- Vue 公共 Status Page 状态页
- Docker Compose 基础设施编排
- Nginx 反向代理配置
- k6 压测脚本
- 项目设计与问题答案文档

第二阶段已开始接入：

- Docker Profile 下的 MySQL 快照持久化
- Redis Cache-Aside 最新状态缓存
- MySQL 不可用时的内存降级路径
- 状态事件记录、订阅保存和 RabbitMQ 异步告警
- GitHub Actions 后端、Web、Flutter 和 Compose 校验流程
- Spring Boot 服务目录 CRUD 和 Vue 服务管理页面
- Redis 命中与未命中延迟对比脚本

## 目录

```text
sentinel-monitor/
├── backend/                 Spring Boot API
├── collector/               服务器指标采集器
├── monitor-web/              Vue 监控面板
├── clients/flutter-status/   Flutter Android/Web 状态页客户端
├── infra/                    Docker 与 Nginx 配置
├── tests/k6/                 压测脚本
└── docs/                     架构设计与问题答案
```

## 本地运行方式

Windows 本地开发默认使用内存存储，不要求先启动 MySQL 或 Redis：

```text
cd D:\学习\sentinel-monitor\backend
java -jar target\sentinel-monitor-api-0.1.0-SNAPSHOT.jar
```

后端地址：`http://localhost:8080`

监控面板：

```text
cd monitor-web
npm install
npm run dev
```

面板地址：`http://localhost:5173`

如果需要启动 MySQL、Redis、RabbitMQ 和 Nginx 全套环境：

```text
cd D:\学习\sentinel-monitor
docker-compose up --build
```

容器化入口：`http://localhost:8088`

运维、SSH Tunnel、服务器巡检、日报和压测步骤见 [运维部署与压测手册](docs/运维部署与压测手册.md)。性能指标接口为 `GET /api/v1/metrics/summary?windowSeconds=300`，返回 QPS、5xx、P50、P90、P99 和 P999。

Redis Cache-Aside 统计接口：`GET /api/v1/metrics/cache`。

状态页下方的“服务目录管理”可以直接新增、编辑和删除被监控服务；这是教学项目中的管理入口，正式环境还需要补充登录鉴权和操作审计。

Spring Boot 完整 CRUD 接口和练习说明见 [SpringBoot CRUD 实践](docs/SpringBoot%20CRUD实践.md)。

MongoDB Compass 连接地址：`mongodb://root:change-me@127.0.0.1:27019/?authSource=admin`。

MongoDB 操作练习见 [MongoDB Compass 实践](docs/MongoDB%20Compass实践.md)，当前已创建 `sentinel_demo.server_snapshots` 示例数据。

公共状态页通过 `GET /api/v1/status` 获取整体状态、服务分组、可用率和最近检查历史；页面不再把这些内容写死在前端。

Flutter 客户端位于 `clients/flutter-status`：

```text
cd D:\学习\sentinel-monitor\clients\flutter-status
flutter pub get
flutter run -d chrome --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

Android 真机或模拟器需要把 `API_BASE_URL` 改成电脑在局域网中的地址；Android 模拟器通常使用 `http://10.0.2.2:8080`。

采集器：

```text
cd collector
python collector.py --server-id local-dev --api-url http://localhost:8080
```

## 设计文档

完整架构、实施计划以及第一波和第二波问题答案见：

[项目架构与问题答案](docs/项目架构与问题答案.md)

开发环境、依赖缓存和本机工具说明见：

[开发环境与依赖说明](docs/开发环境与依赖说明.md)

## 当前边界

当前已经跑通“采集 → 接收 → 面板展示 → 服务目录维护”的闭环，并完成 MySQL 持久化、Redis 缓存命中对比和 RabbitMQ 告警的本地验证。COS、EdgeOne、Flutter 正式打包和日报通知仍需要真实云账号、域名或邮件配置后再做上线验证。

