package com.example.monitor.domain;

public class ContainerSnapshot {

    private String name;
    private String status;
    private Double cpuPercent;
    private String memoryUsage;
    private String networkIo;

    public ContainerSnapshot() {
    }

    public ContainerSnapshot(String name, String status, Double cpuPercent, String memoryUsage, String networkIo) {
        this.name = name;
        this.status = status;
        this.cpuPercent = cpuPercent;
        this.memoryUsage = memoryUsage;
        this.networkIo = networkIo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCpuPercent() {
        return cpuPercent;
    }

    public void setCpuPercent(Double cpuPercent) {
        this.cpuPercent = cpuPercent;
    }

    public String getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(String memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public String getNetworkIo() {
        return networkIo;
    }

    public void setNetworkIo(String networkIo) {
        this.networkIo = networkIo;
    }
}

