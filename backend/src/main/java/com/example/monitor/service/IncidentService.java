package com.example.monitor.service;

import com.example.monitor.domain.StatusIncident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final Map<Long, StatusIncident> memoryIncidents = new ConcurrentHashMap<Long, StatusIncident>();
    private final AtomicLong memoryId = new AtomicLong(1);
    private final JdbcTemplate jdbcTemplate;
    private final boolean persistenceEnabled;

    @Autowired
    public IncidentService(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            @Value("${sentinel.features.persistence:false}") boolean persistenceEnabled) {
        this.jdbcTemplate = persistenceEnabled ? jdbcTemplateProvider.getIfAvailable() : null;
        this.persistenceEnabled = persistenceEnabled && this.jdbcTemplate != null;
    }

    public StatusIncident create(String title, String impact, String message) {
        Instant now = Instant.now();
        String normalizedImpact = "major_outage".equals(impact) ? "major_outage" : "degraded";
        if (persistenceEnabled) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO status_incidents (title, incident_status, impact, message, started_at, updated_at) VALUES (?, 'investigating', ?, ?, ?, ?)",
                        title.trim(), normalizedImpact, message.trim(), Timestamp.from(now), Timestamp.from(now));
                return jdbcTemplate.queryForObject(
                        "SELECT id, title, incident_status, impact, message, started_at, updated_at, resolved_at FROM status_incidents WHERE title = ? AND started_at = ? ORDER BY id DESC LIMIT 1",
                        new IncidentRowMapper(), title.trim(), Timestamp.from(now));
            } catch (RuntimeException ex) {
                log.warn("保存事件失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        StatusIncident incident = new StatusIncident(
                memoryId.getAndIncrement(), title.trim(), "investigating", normalizedImpact, message.trim(), now, now, null);
        memoryIncidents.put(incident.getId(), incident);
        return incident;
    }

    public List<StatusIncident> active() {
        if (persistenceEnabled) {
            try {
                return jdbcTemplate.query(
                        "SELECT id, title, incident_status, impact, message, started_at, updated_at, resolved_at " +
                                "FROM status_incidents WHERE incident_status <> 'resolved' ORDER BY started_at DESC",
                        new IncidentRowMapper());
            } catch (RuntimeException ex) {
                log.warn("读取事件失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        List<StatusIncident> result = new ArrayList<StatusIncident>();
        for (StatusIncident incident : memoryIncidents.values()) {
            if (!"resolved".equals(incident.getStatus())) {
                result.add(incident);
            }
        }
        result.sort(Comparator.comparing(StatusIncident::getStartedAt).reversed());
        return result;
    }

    public StatusIncident resolve(Long id) {
        Instant now = Instant.now();
        if (persistenceEnabled) {
            try {
                jdbcTemplate.update(
                        "UPDATE status_incidents SET incident_status = 'resolved', updated_at = ?, resolved_at = ? WHERE id = ?",
                        Timestamp.from(now), Timestamp.from(now), id);
                List<StatusIncident> result = jdbcTemplate.query(
                        "SELECT id, title, incident_status, impact, message, started_at, updated_at, resolved_at FROM status_incidents WHERE id = ?",
                        new IncidentRowMapper(), id);
                return result.isEmpty() ? null : result.get(0);
            } catch (RuntimeException ex) {
                log.warn("恢复事件失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        StatusIncident current = memoryIncidents.get(id);
        if (current == null) {
            return null;
        }
        StatusIncident resolved = new StatusIncident(
                current.getId(), current.getTitle(), "resolved", current.getImpact(), current.getMessage(),
                current.getStartedAt(), now, now);
        memoryIncidents.put(id, resolved);
        return resolved;
    }

    private static class IncidentRowMapper implements org.springframework.jdbc.core.RowMapper<StatusIncident> {
        @Override
        public StatusIncident mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            Timestamp resolvedAt = rs.getTimestamp("resolved_at");
            return new StatusIncident(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("incident_status"),
                    rs.getString("impact"),
                    rs.getString("message"),
                    rs.getTimestamp("started_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
                    resolvedAt == null ? null : resolvedAt.toInstant());
        }
    }
}
