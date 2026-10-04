package com.example.monitor.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量级本地请求指标采集器，用于教学和开发环境观察 QPS 与长尾延迟。
 * 生产环境可替换为 Micrometer + Prometheus，而不改变业务接口。
 */
@Component
public class RequestMetricsService {

    private static final int MAX_SAMPLES = 10000;
    private static final long RETENTION_SECONDS = 15 * 60;

    private final Deque<RequestSample> samples = new ArrayDeque<RequestSample>();

    public synchronized void record(String path, int status, long durationMs) {
        Instant now = Instant.now();
        samples.addLast(new RequestSample(now, path, status, Math.max(0L, durationMs)));
        trim(now);
    }

    public synchronized Map<String, Object> summary(long requestedWindowSeconds) {
        long windowSeconds = Math.max(1L, Math.min(requestedWindowSeconds, 15 * 60));
        Instant since = Instant.now().minusSeconds(windowSeconds);
        List<RequestSample> window = new ArrayList<RequestSample>();
        int serverErrors = 0;
        for (RequestSample sample : samples) {
            if (sample.at.isAfter(since)) {
                window.add(sample);
                if (sample.status >= 500) {
                    serverErrors++;
                }
            }
        }

        List<Long> durations = new ArrayList<Long>();
        for (RequestSample sample : window) {
            durations.add(sample.durationMs);
        }
        Collections.sort(durations);

        Map<String, Object> latency = new LinkedHashMap<String, Object>();
        latency.put("p50Ms", percentile(durations, 0.50));
        latency.put("p90Ms", percentile(durations, 0.90));
        latency.put("p99Ms", percentile(durations, 0.99));
        latency.put("p999Ms", percentile(durations, 0.999));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("windowSeconds", windowSeconds);
        result.put("requestCount", window.size());
        result.put("qps", round(window.size() / (double) windowSeconds));
        result.put("serverErrorCount", serverErrors);
        result.put("latency", latency);
        result.put("generatedAt", Instant.now());
        return result;
    }

    private void trim(Instant now) {
        Instant cutoff = now.minusSeconds(RETENTION_SECONDS);
        while (!samples.isEmpty() && (samples.size() > MAX_SAMPLES || samples.peekFirst().at.isBefore(cutoff))) {
            samples.removeFirst();
        }
    }

    private long percentile(List<Long> sorted, double ratio) {
        if (sorted.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(ratio * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private static final class RequestSample {
        private final Instant at;
        private final String path;
        private final int status;
        private final long durationMs;

        private RequestSample(Instant at, String path, int status, long durationMs) {
            this.at = at;
            this.path = path;
            this.status = status;
            this.durationMs = durationMs;
        }
    }
}
