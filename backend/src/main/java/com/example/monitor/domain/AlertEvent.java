package com.example.monitor.domain;

import java.time.Instant;

public class AlertEvent {

    private String serverId;
    private String type;
    private String severity;
    private String message;
    private Instant occurredAt;

    public AlertEvent() {
    }

    public AlertEvent(String serverId, String type, String severity, String message, Instant occurredAt) {
        this.serverId = serverId;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
