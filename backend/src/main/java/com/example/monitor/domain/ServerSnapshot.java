package com.example.monitor.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ServerSnapshot {

    private String serverId;
    private String hostname;
    private String platform;
    private Instant collectedAt;
    private Instant receivedAt;
    private Double cpuUsagePercent;
    private Double load1;
    private Long memoryUsedBytes;
    private Long memoryTotalBytes;
    private Long diskUsedBytes;
    private Long diskTotalBytes;
    private Long networkRxBytes;
    private Long networkTxBytes;
    private List<ContainerSnapshot> containers = new ArrayList<ContainerSnapshot>();

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Instant collectedAt) {
        this.collectedAt = collectedAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public Double getLoad1() {
        return load1;
    }

    public void setLoad1(Double load1) {
        this.load1 = load1;
    }

    public Long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    public void setMemoryUsedBytes(Long memoryUsedBytes) {
        this.memoryUsedBytes = memoryUsedBytes;
    }

    public Long getMemoryTotalBytes() {
        return memoryTotalBytes;
    }

    public void setMemoryTotalBytes(Long memoryTotalBytes) {
        this.memoryTotalBytes = memoryTotalBytes;
    }

    public Long getDiskUsedBytes() {
        return diskUsedBytes;
    }

    public void setDiskUsedBytes(Long diskUsedBytes) {
        this.diskUsedBytes = diskUsedBytes;
    }

    public Long getDiskTotalBytes() {
        return diskTotalBytes;
    }

    public void setDiskTotalBytes(Long diskTotalBytes) {
        this.diskTotalBytes = diskTotalBytes;
    }

    public Long getNetworkRxBytes() {
        return networkRxBytes;
    }

    public void setNetworkRxBytes(Long networkRxBytes) {
        this.networkRxBytes = networkRxBytes;
    }

    public Long getNetworkTxBytes() {
        return networkTxBytes;
    }

    public void setNetworkTxBytes(Long networkTxBytes) {
        this.networkTxBytes = networkTxBytes;
    }

    public List<ContainerSnapshot> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerSnapshot> containers) {
        this.containers = containers;
    }
}

