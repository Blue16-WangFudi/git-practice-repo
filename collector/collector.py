#!/usr/bin/env python3
"""Small cross-platform metric collector for Sentinel Monitor."""

import argparse
import json
import os
import platform
import shutil
import subprocess
import time
from datetime import datetime, timezone
from urllib import request

try:
    import psutil
except ImportError:  # Optional: Linux fallback still works without psutil.
    psutil = None


def bytes_from_memory_info():
    if psutil is not None:
        memory = psutil.virtual_memory()
        return memory.used, memory.total

    if os.path.exists("/proc/meminfo"):
        values = {}
        with open("/proc/meminfo", "r", encoding="utf-8") as stream:
            for line in stream:
                key, value = line.split(":", 1)
                values[key] = int(value.strip().split()[0]) * 1024
        total = values.get("MemTotal")
        available = values.get("MemAvailable", values.get("MemFree", 0))
        if total is not None:
            return total - available, total
    return None, None


def network_bytes():
    if psutil is None:
        return None, None
    counters = psutil.net_io_counters()
    return counters.bytes_recv, counters.bytes_sent


def load_average():
    if hasattr(os, "getloadavg"):
        return os.getloadavg()[0]
    return None


def parse_percent(value):
    if not value:
        return None
    return float(value.rstrip("%"))


def docker_stats():
    command = ["docker", "stats", "--no-stream", "--format", "{{json .}}"]
    try:
        completed = subprocess.run(
            command,
            check=False,
            capture_output=True,
            text=True,
            timeout=5,
        )
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return []

    if completed.returncode != 0:
        return []

    result = []
    for line in completed.stdout.splitlines():
        try:
            item = json.loads(line)
        except json.JSONDecodeError:
            continue
        result.append(
            {
                "name": item.get("Name") or item.get("Container"),
                "status": "running",
                "cpuPercent": parse_percent(item.get("CPUPerc")),
                "memoryUsage": item.get("MemUsage"),
                "networkIo": item.get("NetIO"),
            }
        )
    return result


def collect(server_id):
    memory_used, memory_total = bytes_from_memory_info()
    network_rx, network_tx = network_bytes()
    disk = shutil.disk_usage(os.path.abspath(os.sep))
    cpu = psutil.cpu_percent(interval=0.1) if psutil is not None else None

    return {
        "serverId": server_id,
        "hostname": platform.node() or "unknown",
        "platform": platform.platform(),
        "collectedAtEpochMs": int(time.time() * 1000),
        "cpuUsagePercent": cpu,
        "load1": load_average(),
        "memoryUsedBytes": memory_used,
        "memoryTotalBytes": memory_total,
        "diskUsedBytes": disk.used,
        "diskTotalBytes": disk.total,
        "networkRxBytes": network_rx,
        "networkTxBytes": network_tx,
        "containers": docker_stats(),
    }


def post_json(api_url, payload):
    body = json.dumps(payload).encode("utf-8")
    target = api_url.rstrip("/") + "/api/v1/metrics/ingest"
    request_object = request.Request(
        target,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with request.urlopen(request_object, timeout=10) as response:
        return response.status


def main():
    parser = argparse.ArgumentParser(description="Sentinel Monitor metric collector")
    parser.add_argument("--server-id", required=True)
    parser.add_argument("--api-url", default="http://localhost:8080")
    parser.add_argument("--interval", type=int, default=15)
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args()

    while True:
        payload = collect(args.server_id)
        try:
            status = post_json(args.api_url, payload)
            timestamp = datetime.now(timezone.utc).isoformat()
            print(f"{timestamp} submitted status={status} server={args.server_id}", flush=True)
        except Exception as exc:  # Keep the collector alive during temporary API failures.
            print(f"submit failed: {exc}", flush=True)

        if args.once:
            break
        time.sleep(max(args.interval, 1))


if __name__ == "__main__":
    main()

