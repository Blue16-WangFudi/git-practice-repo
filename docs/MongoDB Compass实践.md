# MongoDB Compass 实践

## 1. 连接

```text
mongodb://root:change-me@127.0.0.1:27019/?authSource=admin
```

这是本机 Docker MongoDB 的练习连接。`27019` 是 Windows 本机端口，容器内部端口仍然是 `27017`。

## 2. 创建示例数据

项目已经提供 CRUD 脚本：

```text
ops/mongo-crud-demo.js
```

运行脚本后会创建：

```text
数据库：sentinel_demo
集合：server_snapshots
```

脚本使用 `updateOne + upsert`，重复运行不会无限生成重复服务器记录。

## 3. Compass 中练习 CRUD

### Create

在 `sentinel_demo.server_snapshots` 中新增文档：

```json
{
  "serverId": "compass-demo",
  "hostname": "my-machine",
  "platform": "Windows",
  "cpuUsagePercent": 18.5,
  "memoryUsagePercent": 42.1,
  "status": "online"
}
```

### Read

查询 CPU 超过 50% 的服务器：

```json
{ "cpuUsagePercent": { "$gt": 50 } }
```

### Update

把一台服务器改为正常状态：

```json
{ "serverId": "demo-server-02" }
```

更新内容：

```json
{ "$set": { "status": "online", "cpuUsagePercent": 22.4 } }
```

### Delete

只删除自己创建的练习数据：

```json
{ "serverId": "compass-demo" }
```

正式业务数据删除前必须确认范围，不能直接执行无条件删除。

## 4. MongoDB 与项目的关系

当前监控快照主链路使用 MySQL，保证你同时练习结构化数据库；MongoDB 作为文档型数据库练习环境，用于理解集合、文档、灵活字段和 Compass 操作。后续如果需要保存非固定结构的事件详情，可以再把告警原始载荷迁移到 MongoDB。
