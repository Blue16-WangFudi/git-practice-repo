package com.example.monitor;

import com.example.monitor.domain.MetricIngestRequest;
import com.example.monitor.domain.ServerSnapshot;
import com.example.monitor.service.MonitoringService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringServiceTest {

    @Test
    void ingestStoresLatestSnapshotAndReportsItOnline() {
        MonitoringService service = new MonitoringService(120);
        MetricIngestRequest request = new MetricIngestRequest();
        request.setServerId("test-server");
        request.setHostname("test-host");
        request.setCpuUsagePercent(12.5);

        ServerSnapshot snapshot = service.ingest(request);

        assertThat(snapshot.getServerId()).isEqualTo("test-server");
        assertThat(service.list()).hasSize(1);
        assertThat(service.isOnline(snapshot)).isTrue();
        assertThat(service.overview().get("onlineServers")).isEqualTo(1);
    }
}

