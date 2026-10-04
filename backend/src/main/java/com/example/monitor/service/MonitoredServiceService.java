package com.example.monitor.service;

import com.example.monitor.domain.MonitoredService;
import com.example.monitor.domain.MonitoredServiceRequest;
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
public class MonitoredServiceService {

    private static final Logger log = LoggerFactory.getLogger(MonitoredServiceService.class);
    private static final String SELECT_COLUMNS = "id, service_key, service_name, group_name, endpoint_url, description, service_status, created_at, updated_at";

    private final Map<Long, MonitoredService> memoryServices = new ConcurrentHashMap<Long, MonitoredService>();
    private final AtomicLong memoryId = new AtomicLong(1);
    private final JdbcTemplate jdbcTemplate;
    private final boolean persistenceEnabled;

    @Autowired
    public MonitoredServiceService(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            @Value("${sentinel.features.persistence:false}") boolean persistenceEnabled) {
        this.jdbcTemplate = persistenceEnabled ? jdbcTemplateProvider.getIfAvailable() : null;
        this.persistenceEnabled = persistenceEnabled && this.jdbcTemplate != null;
    }

    public MonitoredService create(MonitoredServiceRequest request) {
        Instant now = Instant.now();
        String status = normalizeStatus(request.getStatus());
        if (persistenceEnabled) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO monitored_services (service_key, service_name, group_name, endpoint_url, description, service_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        clean(request.getServiceKey()), clean(request.getName()), clean(request.getGroupName()),
                        cleanNullable(request.getEndpointUrl()), cleanNullable(request.getDescription()), status,
                        Timestamp.from(now), Timestamp.from(now));
                Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                return get(id);
            } catch (RuntimeException ex) {
                log.warn("保存被监控服务失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        MonitoredService service = build(memoryId.getAndIncrement(), request, status, now, now);
        memoryServices.put(service.getId(), service);
        return service;
    }

    public List<MonitoredService> list() {
        if (persistenceEnabled) {
            try {
                return jdbcTemplate.query(
                        "SELECT " + SELECT_COLUMNS + " FROM monitored_services ORDER BY id DESC",
                        new ServiceRowMapper());
            } catch (RuntimeException ex) {
                log.warn("读取被监控服务失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }
        List<MonitoredService> result = new ArrayList<MonitoredService>(memoryServices.values());
        result.sort(Comparator.comparing(MonitoredService::getId).reversed());
        return result;
    }

    public MonitoredService get(Long id) {
        if (id == null) {
            return null;
        }
        if (persistenceEnabled) {
            try {
                List<MonitoredService> result = jdbcTemplate.query(
                        "SELECT " + SELECT_COLUMNS + " FROM monitored_services WHERE id = ?",
                        new ServiceRowMapper(), id);
                return result.isEmpty() ? null : result.get(0);
            } catch (RuntimeException ex) {
                log.warn("读取被监控服务失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }
        return memoryServices.get(id);
    }

    public MonitoredService update(Long id, MonitoredServiceRequest request) {
        MonitoredService current = get(id);
        if (current == null) {
            return null;
        }

        Instant now = Instant.now();
        String status = normalizeStatus(request.getStatus());
        if (persistenceEnabled) {
            try {
                jdbcTemplate.update(
                        "UPDATE monitored_services SET service_key = ?, service_name = ?, group_name = ?, endpoint_url = ?, description = ?, service_status = ?, updated_at = ? WHERE id = ?",
                        clean(request.getServiceKey()), clean(request.getName()), clean(request.getGroupName()),
                        cleanNullable(request.getEndpointUrl()), cleanNullable(request.getDescription()), status,
                        Timestamp.from(now), id);
                return get(id);
            } catch (RuntimeException ex) {
                log.warn("更新被监控服务失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        MonitoredService updated = build(id, request, status, current.getCreatedAt(), now);
        memoryServices.put(id, updated);
        return updated;
    }

    public boolean delete(Long id) {
        if (persistenceEnabled) {
            try {
                return jdbcTemplate.update("DELETE FROM monitored_services WHERE id = ?", id) > 0;
            } catch (RuntimeException ex) {
                log.warn("删除被监控服务失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }
        return memoryServices.remove(id) != null;
    }

    private MonitoredService build(Long id, MonitoredServiceRequest request, String status, Instant createdAt, Instant updatedAt) {
        return new MonitoredService(id, clean(request.getServiceKey()), clean(request.getName()), clean(request.getGroupName()),
                cleanNullable(request.getEndpointUrl()), cleanNullable(request.getDescription()), status, createdAt, updatedAt);
    }

    private String normalizeStatus(String status) {
        if ("degraded".equals(status) || "major_outage".equals(status) || "unknown".equals(status)) {
            return status;
        }
        return "operational";
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanNullable(String value) {
        String cleaned = clean(value);
        return cleaned == null || cleaned.isEmpty() ? null : cleaned;
    }

    private static class ServiceRowMapper implements org.springframework.jdbc.core.RowMapper<MonitoredService> {
        @Override
        public MonitoredService mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            return new MonitoredService(
                    rs.getLong("id"),
                    rs.getString("service_key"),
                    rs.getString("service_name"),
                    rs.getString("group_name"),
                    rs.getString("endpoint_url"),
                    rs.getString("description"),
                    rs.getString("service_status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant());
        }
    }
}
