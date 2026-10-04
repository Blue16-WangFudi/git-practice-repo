# 运维脚本

> 当前只在本地继续开发。已撤下原服务器上的学习项目，不要在原来的小规格服务器重新安装整套服务。

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

## 邮件发送（已实现，真实投递待配置）

脚本只用 Python 标准库，不需要额外安装邮件依赖。它是 SMTP 客户端，不是完整邮件服务器，也不会自动读取项目 `.env`。Windows 使用进程环境变量，Linux 可以使用下面的 systemd EnvironmentFile 注入配置。

| 变量 | 含义 |
|---|---|
| `SMTP_HOST` | 邮件服务商提供的 SMTP 主机 |
| `SMTP_SECURITY` | `starttls`（默认）或 `ssl`；拒绝明文模式 |
| `SMTP_PORT` | 默认 STARTTLS 587 / SSL 465，以服务商要求为准 |
| `SMTP_USER` / `SMTP_PASSWORD` | 账号和授权码，同时设置；密码保留原始内容 |
| `SMTP_FROM` | 一个发件邮箱，应为服务商允许的已验证地址 |
| `SMTP_TO` | 收件邮箱；多个地址用英文逗号分隔 |
| `SMTP_SUBJECT` | 可选主题 |

确认目标邮箱后，显式执行 `python3 ops/health-report.py --send-email`。同时发送到两个渠道用 `--send-email --send-feishu`。即使环境变量已有凭据，没有开关也不会发送。

SMTP 使用证书校验；STARTTLS 必须成功后才登录和发送，不会回退到明文。一个渠道失败仍尝试另一个已开启的渠道，最终以非零退出码表示发送失败。SMTP 接受邮件不代表对方已收到，真实验收还要查看收件箱；部分收件人被拒绝时不要自动重发，以免重复通知。

自有域名邮件优先采用托管邮件服务，不在当前小规格服务器增加完整邮件系统。按照邮件服务商要求配置 MX、SPF、DKIM、DMARC，再验证收发；仅实现发送脚本不能算完成域名邮件任务。

## 故障日报与统计边界

健康、状态、性能和缓存接口分别采集；一个接口超时、响应非 JSON 或结构异常，不会阻断整个日报。缺失指标标为“未知”，JSON 中的 `collectionErrors` 记录采集失败来源；不会把缺失数据显示为 0。Docker 不可用时也输出故障说明。

当前报告是**每天生成一次的巡检快照**：请求统计只覆盖最近 5 分钟，容器数据是瞬时值。它不是完整 24 小时资源峰值、累计请求数、全天可用率报告。后续要先保存历史数据并做日级聚合，才能声称支持这些指标。

飞书会检查 HTTP 状态和业务响应码，不把 HTTP 200 一律当成成功；签名函数有固定参考向量测试。协议参考：[飞书自定义机器人文档](https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot)。

## 本地自动测试

```powershell
python -B -m unittest discover -s ops -p "test_*.py" -v
```

测试中的网络、SMTP 和 Docker 调用均被隔离，不向真实收件方发送。覆盖全量/部分接口故障、异常数据、飞书签名和业务拒绝、SMTP 加密与校验、部分拒收、默认不外发和渠道互不阻塞。CI 已加入同一套测试；本地通过不等于 GitHub 云端 CI 已运行通过。

## Linux systemd 定时任务

将项目放到 `/opt/sentinel-monitor`，复制：

```bash
sudo cp ops/sentinel-daily-report.service /etc/systemd/system/
sudo cp ops/sentinel-daily-report.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now sentinel-daily-report.timer
```

默认每天 09:00 执行，当前 systemd 模板只生成本地日报，不会自动外发。正式环境确认 Webhook 目的地、接收群和 payload 后，再把 service 的 ExecStart 显式改为追加 `--send-feishu`，并通过 systemd EnvironmentFile 或 Docker Secret 注入 `FEISHU_WEBHOOK_URL`。

配置文件路径为 `/etc/sentinel-monitor/report.env`，每行填写 `KEY=value`，限制为服务账号可读，不提交 Git。邮件启用同理显式追加 `--send-email`。脚本不因 Docker 服务未启动而被阻止执行，能在容器服务故障时产生巡检快照。定时器按照服务器本地时区每天 09:00 执行；设置模板不等于在服务器上已经启用。

在没有真实 Webhook 时，不要追加 `--send-feishu`；这样可以继续验证接口、Redis 和 Docker 资源采集而不会发送数据。

