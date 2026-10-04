package com.example.monitor.controller;

import com.example.monitor.service.MonitoringService;
import com.example.monitor.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;
import com.example.monitor.domain.StatusIncident;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final MonitoringService monitoringService;
    private final IncidentService incidentService;

    public StatusController(MonitoringService monitoringService, IncidentService incidentService) {
        this.monitoringService = monitoringService;
        this.incidentService = incidentService;
    }

    @GetMapping
    public Map<String, Object> status() {
        Map<String, Object> result = monitoringService.statusPage();
        List<StatusIncident> incidents = incidentService.active();
        result.put("incidents", incidents);
        if (!incidents.isEmpty()) {
            boolean major = false;
            for (StatusIncident incident : incidents) {
                if ("major_outage".equals(incident.getImpact())) {
                    major = true;
                    break;
                }
            }
            result.put("overallStatus", major ? "major_outage" : "degraded");
        }
        return result;
    }
}
