# Metric collector

采集器默认每 15 秒把服务器指标发送到 Spring Boot API。

```text
python collector.py --server-id local-dev --api-url http://localhost:8080
```

只采集一次：

```text
python collector.py --server-id local-dev --api-url http://localhost:8080 --once
```

`psutil` 用于补充 CPU、内存和网络指标；如果没有安装，Linux 上仍会使用 `/proc` 和标准库的降级实现。

