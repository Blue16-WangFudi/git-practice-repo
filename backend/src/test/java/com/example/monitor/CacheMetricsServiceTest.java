package com.example.monitor;

import com.example.monitor.service.CacheMetricsService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheMetricsServiceTest {

    @Test
    void snapshotCalculatesCacheAsideHitRate() {
        CacheMetricsService metrics = new CacheMetricsService();
        metrics.recordHit();
        metrics.recordHit();
        metrics.recordMiss();
        metrics.recordError();

        assertThat(metrics.snapshot())
                .containsEntry("hits", 2L)
                .containsEntry("misses", 1L)
                .containsEntry("errors", 1L)
                .containsEntry("totalReads", 3L)
                .containsEntry("hitRate", 66.67);
    }
}
