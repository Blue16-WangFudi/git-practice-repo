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
python3 ops/health-report.py --send-feishu
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

默认每天 09:00 执行。正式环境应通过 systemd EnvironmentFile 或 Docker Secret 注入 `FEISHU_WEBHOOK_URL`。

