package com.example.monitor.controller;

import com.example.monitor.domain.MetricIngestRequest;
import com.example.monitor.domain.ServerSnapshot;
import com.example.monitor.service.MonitoringService;
import com.example.monitor.service.RequestMetricsService;
import com.example.monitor.service.CacheMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
public class MetricController {

    private final MonitoringService monitoringService;
    private final RequestMetricsService requestMetricsService;
    private final CacheMetricsService cacheMetricsService;

    public MetricController(MonitoringService monitoringService,
                            RequestMetricsService requestMetricsService,
                            CacheMetricsService cacheMetricsService) {
        this.monitoringService = monitoringService;
        this.requestMetricsService = requestMetricsService;
        this.cacheMetricsService = cacheMetricsService;
    }

    @PostMapping("/ingest")
    @ResponseStatus(HttpStatus.CREATED)
    public ServerSnapshot ingest(@Valid @RequestBody MetricIngestRequest request) {
        return monitoringService.ingest(request);
    }

    @GetMapping("/servers")
    public List<ServerSnapshot> servers() {
        return monitoringService.list();
    }

    @GetMapping("/servers/{serverId}")
    public ServerSnapshot server(@PathVariable String serverId) {
        return monitoringService.get(serverId);
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return monitoringService.overview();
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "300") long windowSeconds) {
        return requestMetricsService.summary(windowSeconds);
    }

    @GetMapping("/cache")
    public Map<String, Object> cache() {
        return cacheMetricsService.snapshot();
    }
}

