package com.example.monitor;

import com.example.monitor.service.RequestMetricsService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestMetricsServiceTest {

    @Test
    void summaryContainsQpsAndLatencyPercentiles() {
        RequestMetricsService service = new RequestMetricsService();
        service.record("/api/health", 200, 10);
        service.record("/api/health", 200, 20);
        service.record("/api/health", 500, 100);

        Map<String, Object> summary = service.summary(300);
        Map<String, Object> latency = (Map<String, Object>) summary.get("latency");

        assertThat(summary.get("requestCount")).isEqualTo(3);
        assertThat(summary.get("serverErrorCount")).isEqualTo(1);
        assertThat(latency.get("p50Ms")).isEqualTo(20L);
        assertThat(latency.get("p90Ms")).isEqualTo(100L);
        assertThat(latency.get("p99Ms")).isEqualTo(100L);
        assertThat(latency.get("p999Ms")).isEqualTo(100L);
    }
}
