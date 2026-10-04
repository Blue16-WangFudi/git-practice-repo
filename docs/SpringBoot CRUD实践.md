# Spring Boot CRUD 实践

项目用 `monitored_services` 表管理被监控服务，完整接口如下：

| 操作 | 方法 | 地址 |
|---|---|---|
| 新增 | POST | `/api/v1/services` |
| 查询列表 | GET | `/api/v1/services` |
| 查询详情 | GET | `/api/v1/services/{id}` |
| 修改 | PUT | `/api/v1/services/{id}` |
| 删除 | DELETE | `/api/v1/services/{id}` |

## 示例请求

```json
{
  "serviceKey": "status-page",
  "name": "公共状态页 API",
  "groupName": "main",
  "endpointUrl": "http://backend:8080/api/v1/status",
  "description": "通过 Spring Boot 提供状态数据",
  "status": "operational"
}
```

代码分层：

```text
MonitoredServiceController  接收 HTTP 请求和返回状态码
MonitoredServiceService     业务逻辑、校验状态和持久化降级
JdbcTemplate                访问 MySQL
monitored_services          保存服务定义
```

Docker Profile 下数据写入 MySQL；默认本地 Profile 下没有数据库时会降级到内存，方便单元测试和接口学习。

本轮已实际验证：新增、查询详情、修改、删除临时记录，最终保留一条公共状态页服务记录。
