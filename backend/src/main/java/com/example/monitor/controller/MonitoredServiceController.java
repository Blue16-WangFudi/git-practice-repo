package com.example.monitor.controller;

import com.example.monitor.domain.MonitoredService;
import com.example.monitor.domain.MonitoredServiceRequest;
import com.example.monitor.service.MonitoredServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
public class MonitoredServiceController {

    private final MonitoredServiceService service;

    public MonitoredServiceController(MonitoredServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MonitoredService> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public MonitoredService get(@PathVariable Long id) {
        MonitoredService result = service.get(id);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "被监控服务不存在");
        }
        return result;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitoredService create(@Valid @RequestBody MonitoredServiceRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MonitoredService update(@PathVariable Long id, @Valid @RequestBody MonitoredServiceRequest request) {
        MonitoredService result = service.update(id, request);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "被监控服务不存在");
        }
        return result;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!service.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "被监控服务不存在");
        }
    }
}
