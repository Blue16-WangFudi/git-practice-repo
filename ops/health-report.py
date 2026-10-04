#!/usr/bin/env python3
"""Generate a health snapshot; send only to explicitly enabled destinations."""

import json
import os
import subprocess
import urllib.request
import argparse
import base64
import hashlib
import hmac
import time
import smtplib
import ssl
import sys
from datetime import datetime, timezone
from email.message import EmailMessage
from email.utils import getaddresses
from pathlib import Path
from urllib.parse import urlsplit


def get_json(url):
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.loads(response.read().decode("utf-8"))


def docker_stats():
    try:
        result = subprocess.run(
            ["docker", "stats", "--no-stream", "--format", "{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}"],
            capture_output=True,
            text=True,
            timeout=15,
            check=False,
        )
        if result.returncode != 0:
            return [{"error": "Docker 资源信息不可用：" + (result.stderr or "未知错误").strip()}]
        rows = result.stdout.strip().splitlines()
        return [
            {"name": values[0], "cpu": values[1], "memory": values[2]}
            for row in rows
            if row.count("|") == 2
            for values in [row.split("|", 2)]
        ]
    except (OSError, subprocess.SubprocessError) as exc:
        return [{"error": "Docker 资源信息不可用：{}".format(exc)}]


def build_report_data(base_url):
    errors = []

    def collect(name, path):
        try:
            data = get_json(base_url.rstrip("/") + path)
            if not isinstance(data, dict):
                raise ValueError("Expected a JSON object")
            for field, expected in {"overview": dict, "latency": dict, "incidents": list}.items():
                if field in data and not isinstance(data[field], expected):
                    raise ValueError("Unexpected field type")
            return data
        except (OSError, ValueError) as exc:
            # Exception messages can contain URLs/credentials. Keep diagnostics safe.
            errors.append({"source": name, "error": type(exc).__name__})
            return {}

    health = collect("health", "/api/health")
    status = collect("status", "/api/v1/status")
    metrics = collect("performance", "/api/v1/metrics/summary?windowSeconds=300")
    cache = collect("cache", "/api/v1/metrics/cache")
    overview = status.get("overview", {})
    incident_count = len(status.get("incidents", [])) if status else None
    generated = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S %Z")

    return {
        "generatedAt": generated,
        "service": {
            "status": status.get("overallStatus", "unknown"),
            "apiStatus": health.get("status", "unknown"),
            "activeIncidentCount": incident_count,
        },
        "servers": overview,
        "performance": metrics,
        "cache": cache,
        "containers": docker_stats(),
        "collectionErrors": errors,
    }


def format_text(report):
    service = report["service"]
    servers = report["servers"]
    metrics = report["performance"]
    latency = metrics.get("latency", {})
    cache = report["cache"]
    lines = [
        "Sentinel Monitor 日报",
        "生成时间：{}".format(report["generatedAt"]),
        "",
        "服务状态：{}（API {}）".format(service["status"], service["apiStatus"]),
        "服务器：在线 {} / 总数 {}，离线 {}".format(
            servers.get("onlineServers", "未知"), servers.get("totalServers", "未知"), servers.get("offlineServers", "未知")
        ),
        "活动事件：{} 条".format(service["activeIncidentCount"] if service["activeIncidentCount"] is not None else "未知"),
        "",
        "最近 5 分钟请求：{} 次，QPS：{}，5xx：{}".format(
            metrics.get("requestCount", "未知"), metrics.get("qps", "未知"), metrics.get("serverErrorCount", "未知")
        ),
        "延迟：P50 {}ms / P90 {}ms / P99 {}ms / P999 {}ms".format(
            latency.get("p50Ms", "未知"), latency.get("p90Ms", "未知"), latency.get("p99Ms", "未知"), latency.get("p999Ms", "未知")
        ),
        "Redis：命中 {} 次 / 未命中 {} 次 / 命中率 {}%".format(
            cache.get("hits", "未知"), cache.get("misses", "未知"), cache.get("hitRate", "未知")
        ),
        "",
        "容器资源：",
    ]
    if report.get("collectionErrors"):
        lines[0] += " [采集异常]"
        lines.extend("采集失败：{}（{}）".format(item["source"], item["error"])
                     for item in report["collectionErrors"])
    if not report["containers"]:
        lines.append("未发现运行中的容器。")
    for container in report["containers"]:
        if "error" in container:
            lines.append(container["error"])
        else:
            lines.append("- {}：CPU {}，内存 {}".format(container["name"], container["cpu"], container["memory"]))
    return "\n".join(lines)


def feishu_signature(timestamp, secret):
    # Feishu uses timestamp + LF + secret as the HMAC key and an empty message.
    key = "{}\n{}".format(timestamp, secret).encode("utf-8")
    return base64.b64encode(hmac.new(key, b"", hashlib.sha256).digest()).decode("utf-8")


def send_feishu(report):
    webhook = os.getenv("FEISHU_WEBHOOK_URL", "").strip()
    if not webhook:
        raise RuntimeError("未配置 FEISHU_WEBHOOK_URL")
    if urlsplit(webhook).scheme != "https":
        raise RuntimeError("FEISHU_WEBHOOK_URL 必须使用 HTTPS")
    payload_data = {"msg_type": "text", "content": {"text": report}}
    secret = os.getenv("FEISHU_SECRET", "").strip()
    if secret:
        timestamp = str(int(time.time()))
        payload_data.update({"timestamp": timestamp, "sign": feishu_signature(timestamp, secret)})
    payload = json.dumps(payload_data).encode("utf-8")
    request = urllib.request.Request(
        webhook,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        if response.status >= 300:
            raise RuntimeError("飞书 Webhook 返回 HTTP {}".format(response.status))
        result = json.loads(response.read().decode("utf-8"))
        if not isinstance(result, dict):
            raise RuntimeError("飞书返回的响应格式异常")
        # Prefer the current code; accept the legacy StatusCode only if absent.
        code = result.get("code", result.get("StatusCode"))
        if type(code) is not int or code != 0:
            raise RuntimeError("飞书未确认发送成功；请检查机器人安全设置和业务响应码")
    return True


def mail_addresses(value, field, single=False):
    if "\r" in value or "\n" in value:
        raise RuntimeError("{} 不允许包含换行".format(field))
    addresses = [address for _, address in getaddresses([value])]
    if not addresses or any(address.count("@") != 1 or any(ch.isspace() for ch in address)
                            or not all(address.split("@")) for address in addresses):
        raise RuntimeError("{} 必须填写有效的邮箱地址".format(field))
    if single and len(addresses) != 1:
        raise RuntimeError("{} 只能填写一个邮箱地址".format(field))
    return addresses


def send_email(report):
    host = os.getenv("SMTP_HOST", "").strip()
    sender = os.getenv("SMTP_FROM", "").strip()
    recipients = os.getenv("SMTP_TO", "").strip()
    if not host or not sender or not recipients:
        raise RuntimeError("邮件需要配置 SMTP_HOST、SMTP_FROM、SMTP_TO")
    security = os.getenv("SMTP_SECURITY", "starttls").strip().lower()
    if security not in ("ssl", "starttls"):
        raise RuntimeError("SMTP_SECURITY 只允许 ssl 或 starttls；不支持明文发送")
    try:
        port = int(os.getenv("SMTP_PORT", "").strip() or ("465" if security == "ssl" else "587"))
    except ValueError:
        raise RuntimeError("SMTP_PORT 必须为整数") from None
    if not 1 <= port <= 65535:
        raise RuntimeError("SMTP_PORT 超出有效范围")
    user = os.getenv("SMTP_USER", "").strip()
    password = os.getenv("SMTP_PASSWORD", "")
    if bool(user) != bool(password):
        raise RuntimeError("SMTP_USER 与 SMTP_PASSWORD 必须同时配置或同时留空")
    from_address = mail_addresses(sender, "SMTP_FROM", single=True)[0]
    to_addresses = mail_addresses(recipients, "SMTP_TO")
    message = EmailMessage()
    message["From"] = sender
    message["To"] = ", ".join(to_addresses)
    message["Subject"] = os.getenv("SMTP_SUBJECT", "Sentinel Monitor 每日巡检")
    message.set_content(report)
    context = ssl.create_default_context()
    factory = smtplib.SMTP_SSL if security == "ssl" else smtplib.SMTP
    kwargs = {"timeout": 15}
    if security == "ssl":
        kwargs["context"] = context
    with factory(host, port, **kwargs) as smtp:
        if security == "starttls":
            smtp.ehlo()
            smtp.starttls(context=context)
            smtp.ehlo()
        if user:
            smtp.login(user, password)
        refused = smtp.send_message(message, from_addr=from_address, to_addrs=to_addresses)
        if refused:
            raise RuntimeError("SMTP 拒收了部分收件人；请检查配置，勿自动重复发送")
    return True


def main(argv=None):
    parser = argparse.ArgumentParser(description="生成 Sentinel Monitor 日报")
    parser.add_argument("--base-url", default=os.getenv("STATUS_API_URL", "http://127.0.0.1:8088"))
    parser.add_argument("--format", choices=("text", "json"), default="text")
    parser.add_argument("--output", help="可选：把日报同时保存到本地文件")
    parser.add_argument(
        "--send-feishu",
        action="store_true",
        help="显式允许将日报发送到 FEISHU_WEBHOOK_URL；默认只打印到本地",
    )
    parser.add_argument("--send-email", action="store_true", help="显式允许通过 SMTP 发送邮件；默认不发送")
    args = parser.parse_args(argv)

    api_url = args.base_url
    report_data = build_report_data(api_url)
    report_text = format_text(report_data)
    output = report_text if args.format == "text" else json.dumps(report_data, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(output + "\n", encoding="utf-8")
    print(output)
    failed = False
    for enabled, send, label in (
        (args.send_feishu, send_feishu, "飞书"),
        (args.send_email, send_email, "SMTP"),
    ):
        if not enabled:
            continue
        try:
            send(report_text)
            print("\n{} 已接受日报。".format(label))
        except (OSError, ValueError, RuntimeError, smtplib.SMTPException) as exc:
            # Never echo provider errors: they may include credentials or recipients.
            print("{} 发送失败（{}）；请检查凭据、网络和接收方配置。".format(label, type(exc).__name__), file=sys.stderr)
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
