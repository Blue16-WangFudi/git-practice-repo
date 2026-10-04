package com.example.monitor.domain;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MonitoredServiceRequest {

    @NotBlank
    @Size(max = 128)
    private String serviceKey;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(max = 128)
    private String groupName;

    @Size(max = 500)
    private String endpointUrl;

    @Size(max = 1000)
    private String description;

    @Size(max = 32)
    private String status;

    public String getServiceKey() { return serviceKey; }
    public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
