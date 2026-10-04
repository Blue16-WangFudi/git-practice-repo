package com.example.monitor.domain;

import java.time.Instant;

public class MonitoredService {

    private Long id;
    private String serviceKey;
    private String name;
    private String groupName;
    private String endpointUrl;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public MonitoredService() {
    }

    public MonitoredService(Long id, String serviceKey, String name, String groupName,
                            String endpointUrl, String description, String status,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.serviceKey = serviceKey;
        this.name = name;
        this.groupName = groupName;
        this.endpointUrl = endpointUrl;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getServiceKey() { return serviceKey; }
    public String getName() { return name; }
    public String getGroupName() { return groupName; }
    public String getEndpointUrl() { return endpointUrl; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
