# 运维脚本

## 生成日报

在部署服务器上运行：

```bash
STATUS_API_URL=http://127.0.0.1:8088 python3 ops/health-report.py
```

脚本默认只在本地生成日报，不会外发。配置 `FEISHU_WEBHOOK_URL` 后，还必须显式加 `--send-feishu` 才会把日报发送到飞书群机器人：

```bash
STATUS_API_URL=http://127.0.0.1:8088 \
FEISHU_WEBHOOK_URL='https://open.feishu.cn/...' \
FEISHU_SECRET='可选的机器人签名密钥' \
python3 ops/health-report.py --send-feishu
```

机器人开启签名校验时，设置 `FEISHU_SECRET`，脚本会按飞书要求生成时间戳和 HMAC-SHA256 签名。也可以只保存 JSON 日报，不发送：

```bash
python3 ops/health-report.py --format json --output /var/lib/sentinel-monitor/daily-report.json
```

Webhook、域名、邮件系统等生产凭据不写入仓库。

## Linux systemd 定时任务

将项目放到 `/opt/sentinel-monitor`，复制：

```bash
sudo cp ops/sentinel-daily-report.service /etc/systemd/system/
sudo cp ops/sentinel-daily-report.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now sentinel-daily-report.timer
```

默认每天 09:00 执行，当前 systemd 模板只生成本地日报，不会自动外发。正式环境确认 Webhook 目的地、接收群和 payload 后，再把 service 的 ExecStart 显式改为追加 `--send-feishu`，并通过 systemd EnvironmentFile 或 Docker Secret 注入 `FEISHU_WEBHOOK_URL`。

在没有真实 Webhook 时，不要追加 `--send-feishu`；这样可以继续验证接口、Redis 和 Docker 资源采集而不会发送数据。

