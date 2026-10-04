#!/usr/bin/env python3
"""生成 Sentinel Monitor 日报，并可选发送到飞书机器人 Webhook。"""

import json
import os
import subprocess
import urllib.request
import argparse
import base64
import hashlib
import hmac
import time
from datetime import datetime, timezone
from pathlib import Path


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
    health = get_json(base_url.rstrip("/") + "/api/health")
    status = get_json(base_url.rstrip("/") + "/api/v1/status")
    metrics = get_json(base_url.rstrip("/") + "/api/v1/metrics/summary?windowSeconds=300")
    cache = get_json(base_url.rstrip("/") + "/api/v1/metrics/cache")
    overview = status.get("overview", {})
    latency = metrics.get("latency", {})
    incident_count = len(status.get("incidents", []))
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
            servers.get("onlineServers", 0), servers.get("totalServers", 0), servers.get("offlineServers", 0)
        ),
        "活动事件：{} 条".format(service["activeIncidentCount"]),
        "",
        "最近 5 分钟请求：{} 次，QPS：{}，5xx：{}".format(
            metrics.get("requestCount", 0), metrics.get("qps", 0), metrics.get("serverErrorCount", 0)
        ),
        "延迟：P50 {}ms / P90 {}ms / P99 {}ms / P999 {}ms".format(
            latency.get("p50Ms", 0), latency.get("p90Ms", 0), latency.get("p99Ms", 0), latency.get("p999Ms", 0)
        ),
        "Redis：命中 {} 次 / 未命中 {} 次 / 命中率 {}%".format(
            cache.get("hits", 0), cache.get("misses", 0), cache.get("hitRate", 0)
        ),
        "",
        "容器资源：",
    ]
    for container in report["containers"]:
        if "error" in container:
            lines.append(container["error"])
        else:
            lines.append("- {}：CPU {}，内存 {}".format(container["name"], container["cpu"], container["memory"]))
    return "\n".join(lines)


def send_feishu(report):
    webhook = os.getenv("FEISHU_WEBHOOK_URL", "").strip()
    if not webhook:
        raise RuntimeError("未配置 FEISHU_WEBHOOK_URL")
    payload_data = {"msg_type": "text", "content": {"text": report}}
    secret = os.getenv("FEISHU_SECRET", "").strip()
    if secret:
        timestamp = str(int(time.time()))
        string_to_sign = "{}\n{}".format(timestamp, secret).encode("utf-8")
        signature = hmac.new(secret.encode("utf-8"), string_to_sign, hashlib.sha256).digest()
        payload_data.update({"timestamp": timestamp, "sign": base64.b64encode(signature).decode("utf-8")})
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
    return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="生成 Sentinel Monitor 日报")
    parser.add_argument("--base-url", default=os.getenv("STATUS_API_URL", "http://127.0.0.1:8088"))
    parser.add_argument("--format", choices=("text", "json"), default="text")
    parser.add_argument("--output", help="可选：把日报同时保存到本地文件")
    parser.add_argument(
        "--send-feishu",
        action="store_true",
        help="显式允许将日报发送到 FEISHU_WEBHOOK_URL；默认只打印到本地",
    )
    args = parser.parse_args()

    api_url = args.base_url
    report_data = build_report_data(api_url)
    report_text = format_text(report_data)
    output = report_text if args.format == "text" else json.dumps(report_data, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(output + "\n", encoding="utf-8")
    print(output)
    if args.send_feishu and send_feishu(report_text):
        print("\n日报已发送到飞书。")
