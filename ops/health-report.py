#!/usr/bin/env python3
"""生成 Sentinel Monitor 日报，并可选发送到飞书机器人 Webhook。"""

import json
import os
import subprocess
import urllib.request
from datetime import datetime, timezone


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
            return ["Docker 资源信息不可用：" + (result.stderr or "未知错误").strip()]
        rows = result.stdout.strip().splitlines()
        return ["- {}：CPU {}，内存 {}".format(*row.split("|", 2)) for row in rows if row.count("|") == 2]
    except (OSError, subprocess.SubprocessError) as exc:
        return ["Docker 资源信息不可用：{}".format(exc)]


def build_report(base_url):
    health = get_json(base_url.rstrip("/") + "/api/health")
    status = get_json(base_url.rstrip("/") + "/api/v1/status")
    metrics = get_json(base_url.rstrip("/") + "/api/v1/metrics/summary?windowSeconds=300")
    overview = status.get("overview", {})
    latency = metrics.get("latency", {})
    incident_count = len(status.get("incidents", []))
    generated = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M:%S %Z")

    lines = [
        "Sentinel Monitor 日报",
        "生成时间：{}".format(generated),
        "",
        "服务状态：{}（API {}）".format(status.get("overallStatus", "unknown"), health.get("status", "unknown")),
        "服务器：在线 {} / 总数 {}，离线 {}".format(
            overview.get("onlineServers", 0), overview.get("totalServers", 0), overview.get("offlineServers", 0)
        ),
        "活动事件：{} 条".format(incident_count),
        "",
        "最近 5 分钟请求：{} 次，QPS：{}，5xx：{}".format(
            metrics.get("requestCount", 0), metrics.get("qps", 0), metrics.get("serverErrorCount", 0)
        ),
        "延迟：P50 {}ms / P90 {}ms / P99 {}ms / P999 {}ms".format(
            latency.get("p50Ms", 0), latency.get("p90Ms", 0), latency.get("p99Ms", 0), latency.get("p999Ms", 0)
        ),
        "",
        "容器资源：",
    ]
    lines.extend(docker_stats())
    return "\n".join(lines)


def send_feishu(report):
    webhook = os.getenv("FEISHU_WEBHOOK_URL", "").strip()
    if not webhook:
        return False
    payload = json.dumps({"msg_type": "text", "content": {"text": report}}).encode("utf-8")
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
    api_url = os.getenv("STATUS_API_URL", "http://127.0.0.1:8088")
    report_text = build_report(api_url)
    print(report_text)
    if send_feishu(report_text):
        print("\n日报已发送到飞书。")
