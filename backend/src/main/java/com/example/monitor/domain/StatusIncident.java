package com.example.monitor.domain;

import java.time.Instant;

public class StatusIncident {

    private Long id;
    private String title;
    private String status;
    private String impact;
    private String message;
    private Instant startedAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    public StatusIncident() {
    }

    public StatusIncident(Long id, String title, String status, String impact, String message,
                          Instant startedAt, Instant updatedAt, Instant resolvedAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.impact = impact;
        this.message = message;
        this.startedAt = startedAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getImpact() {
        return impact;
    }

    public String getMessage() {
        return message;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
