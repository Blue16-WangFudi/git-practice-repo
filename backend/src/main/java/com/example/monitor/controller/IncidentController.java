package com.example.monitor.controller;

import com.example.monitor.domain.StatusIncident;
import com.example.monitor.domain.StatusIncidentRequest;
import com.example.monitor.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/status/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public List<StatusIncident> active() {
        return incidentService.active();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusIncident create(@Valid @RequestBody StatusIncidentRequest request) {
        return incidentService.create(request.getTitle(), request.getImpact(), request.getMessage());
    }

    @PatchMapping("/{id}/resolve")
    public StatusIncident resolve(@PathVariable Long id) {
        StatusIncident incident = incidentService.resolve(id);
        if (incident == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "事件不存在");
        }
        return incident;
    }
}
