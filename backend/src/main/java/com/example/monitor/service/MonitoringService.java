package com.example.monitor.service;

import com.example.monitor.domain.ContainerSnapshot;
import com.example.monitor.domain.MetricIngestRequest;
import com.example.monitor.domain.ServerSnapshot;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);
    private static final String CACHE_KEY_PREFIX = "sentinel:server:";
    private static final String UPSERT_SQL =
            "INSERT INTO server_snapshots " +
                    "(server_id, hostname, platform_name, collected_at, received_at, cpu_usage_percent, load1, " +
                    "memory_used_bytes, memory_total_bytes, disk_used_bytes, disk_total_bytes, network_rx_bytes, " +
                    "network_tx_bytes, containers_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE hostname = VALUES(hostname), platform_name = VALUES(platform_name), " +
                    "collected_at = VALUES(collected_at), received_at = VALUES(received_at), " +
                    "cpu_usage_percent = VALUES(cpu_usage_percent), load1 = VALUES(load1), " +
                    "memory_used_bytes = VALUES(memory_used_bytes), memory_total_bytes = VALUES(memory_total_bytes), " +
                    "disk_used_bytes = VALUES(disk_used_bytes), disk_total_bytes = VALUES(disk_total_bytes), " +
                    "network_rx_bytes = VALUES(network_rx_bytes), network_tx_bytes = VALUES(network_tx_bytes), " +
                    "containers_json = VALUES(containers_json)";

    private final Map<String, ServerSnapshot> snapshots = new ConcurrentHashMap<String, ServerSnapshot>();
    private final Map<String, Deque<String>> healthHistory = new ConcurrentHashMap<String, Deque<String>>();
    private final long snapshotExpireSeconds;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AlertPublisher alertPublisher;
    private final CacheMetricsService cacheMetricsService;
    private final boolean persistenceEnabled;
    private final boolean redisEnabled;

    @Autowired
    public MonitoringService(
            @Value("${sentinel.snapshot-expire-seconds:120}") long snapshotExpireSeconds,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper,
            AlertPublisher alertPublisher,
            ObjectProvider<CacheMetricsService> cacheMetricsProvider,
            @Value("${sentinel.features.persistence:false}") boolean persistenceEnabled,
            @Value("${sentinel.features.redis:false}") boolean redisEnabled) {
        this.snapshotExpireSeconds = snapshotExpireSeconds;
        this.jdbcTemplate = persistenceEnabled ? jdbcTemplateProvider.getIfAvailable() : null;
        this.redisTemplate = redisEnabled ? redisTemplateProvider.getIfAvailable() : null;
        this.objectMapper = objectMapper;
        this.alertPublisher = alertPublisher;
        CacheMetricsService providedCacheMetrics = cacheMetricsProvider.getIfAvailable();
        this.cacheMetricsService = providedCacheMetrics == null ? new CacheMetricsService() : providedCacheMetrics;
        this.persistenceEnabled = persistenceEnabled && this.jdbcTemplate != null;
        this.redisEnabled = redisEnabled && this.redisTemplate != null;
    }

    public MonitoringService(long snapshotExpireSeconds) {
        this.snapshotExpireSeconds = snapshotExpireSeconds;
        this.jdbcTemplate = null;
        this.redisTemplate = null;
        this.objectMapper = new ObjectMapper();
        this.alertPublisher = null;
        this.cacheMetricsService = new CacheMetricsService();
        this.persistenceEnabled = false;
        this.redisEnabled = false;
    }

    public ServerSnapshot ingest(MetricIngestRequest request) {
        Instant receivedAt = Instant.now();
        Instant collectedAt = request.getCollectedAtEpochMs() == null
                ? receivedAt
                : Instant.ofEpochMilli(request.getCollectedAtEpochMs());

        ServerSnapshot snapshot = new ServerSnapshot();
        snapshot.setServerId(request.getServerId());
        snapshot.setHostname(request.getHostname());
        snapshot.setPlatform(request.getPlatform());
        snapshot.setCollectedAt(collectedAt);
        snapshot.setReceivedAt(receivedAt);
        snapshot.setCpuUsagePercent(request.getCpuUsagePercent());
        snapshot.setLoad1(request.getLoad1());
        snapshot.setMemoryUsedBytes(request.getMemoryUsedBytes());
        snapshot.setMemoryTotalBytes(request.getMemoryTotalBytes());
        snapshot.setDiskUsedBytes(request.getDiskUsedBytes());
        snapshot.setDiskTotalBytes(request.getDiskTotalBytes());
        snapshot.setNetworkRxBytes(request.getNetworkRxBytes());
        snapshot.setNetworkTxBytes(request.getNetworkTxBytes());
        snapshot.setContainers(copyContainers(request.getContainers()));

        snapshots.put(snapshot.getServerId(), snapshot);
        recordHistory("main", "operational");
        recordHistory("api", "operational");
        recordHistory("collector", isHealthy(snapshot) ? "operational" : "degraded");
        persist(snapshot);
        writeCache(snapshot);
        if (alertPublisher != null) {
            alertPublisher.publishIfNeeded(snapshot);
        }
        return snapshot;
    }

    public List<ServerSnapshot> list() {
        if (persistenceEnabled) {
            try {
                List<ServerSnapshot> stored = jdbcTemplate.query(
                        "SELECT server_id, hostname, platform_name, collected_at, received_at, " +
                                "cpu_usage_percent, load1, memory_used_bytes, memory_total_bytes, " +
                                "disk_used_bytes, disk_total_bytes, network_rx_bytes, network_tx_bytes, containers_json " +
                                "FROM server_snapshots ORDER BY server_id",
                        new SnapshotRowMapper());
                for (ServerSnapshot snapshot : stored) {
                    snapshots.put(snapshot.getServerId(), snapshot);
                }
                return stored;
            } catch (RuntimeException ex) {
                log.warn("读取 MySQL 快照失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        List<ServerSnapshot> result = new ArrayList<ServerSnapshot>(snapshots.values());
        result.sort(Comparator.comparing(ServerSnapshot::getServerId));
        return result;
    }

    public ServerSnapshot get(String serverId) {
        ServerSnapshot cached = readCache(serverId);
        if (cached != null) {
            snapshots.put(serverId, cached);
            return cached;
        }

        if (persistenceEnabled) {
            try {
                List<ServerSnapshot> stored = jdbcTemplate.query(
                        "SELECT server_id, hostname, platform_name, collected_at, received_at, " +
                                "cpu_usage_percent, load1, memory_used_bytes, memory_total_bytes, " +
                                "disk_used_bytes, disk_total_bytes, network_rx_bytes, network_tx_bytes, containers_json " +
                                "FROM server_snapshots WHERE server_id = ?",
                        new SnapshotRowMapper(), serverId);
                if (!stored.isEmpty()) {
                    ServerSnapshot snapshot = stored.get(0);
                    snapshots.put(serverId, snapshot);
                    writeCache(snapshot);
                    return snapshot;
                }
            } catch (RuntimeException ex) {
                log.warn("读取 MySQL 快照失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }
        return snapshots.get(serverId);
    }

    public Map<String, Object> overview() {
        List<ServerSnapshot> current = list();
        int total = current.size();
        int online = 0;
        double cpuTotal = 0.0;
        int cpuSamples = 0;

        for (ServerSnapshot snapshot : current) {
            if (isOnline(snapshot)) {
                online++;
            }
            if (snapshot.getCpuUsagePercent() != null) {
                cpuTotal += snapshot.getCpuUsagePercent();
                cpuSamples++;
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("totalServers", total);
        result.put("onlineServers", online);
        result.put("offlineServers", total - online);
        result.put("averageCpuUsagePercent", cpuSamples == 0 ? null : cpuTotal / cpuSamples);
        result.put("generatedAt", Instant.now());
        return result;
    }

    public Map<String, Object> statusPage() {
        Map<String, Object> overview = overview();
        int totalServers = ((Number) overview.get("totalServers")).intValue();
        int onlineServers = ((Number) overview.get("onlineServers")).intValue();
        String overallStatus = totalServers > 0 && onlineServers < totalServers
                ? "degraded"
                : "operational";

        List<Map<String, Object>> groups = new ArrayList<Map<String, Object>>();
        groups.add(statusGroup(
                "main",
                "Sentinel Monitor 主站",
                2,
                Arrays.asList("状态页", "监控面板"),
                "operational"));
        groups.add(statusGroup(
                "collector",
                "指标采集服务",
                1,
                Arrays.asList("服务器指标采集器"),
                totalServers > 0 && onlineServers == 0 ? "degraded" : "operational"));
        groups.add(statusGroup(
                "api",
                "监控 API",
                4,
                Arrays.asList("健康检查接口", "指标上报接口", "服务器概览接口", "Redis / MySQL 服务"),
                "operational"));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("overallStatus", overallStatus);
        result.put("overview", overview);
        result.put("groups", groups);
        result.put("incidents", new ArrayList<Object>());
        return result;
    }

    public boolean isOnline(ServerSnapshot snapshot) {
        if (snapshot == null || snapshot.getReceivedAt() == null) {
            return false;
        }
        return snapshot.getReceivedAt().plusSeconds(snapshotExpireSeconds).isAfter(Instant.now());
    }

    private boolean isHealthy(ServerSnapshot snapshot) {
        double cpu = snapshot.getCpuUsagePercent() == null ? 0.0 : snapshot.getCpuUsagePercent();
        double memory = memoryPercent(snapshot);
        double disk = diskPercent(snapshot);
        return cpu < 90.0 && memory < 90.0 && disk < 90.0;
    }

    private double memoryPercent(ServerSnapshot snapshot) {
        if (snapshot.getMemoryUsedBytes() == null || snapshot.getMemoryTotalBytes() == null
                || snapshot.getMemoryTotalBytes() == 0) {
            return 0.0;
        }
        return snapshot.getMemoryUsedBytes() * 100.0 / snapshot.getMemoryTotalBytes();
    }

    private double diskPercent(ServerSnapshot snapshot) {
        if (snapshot.getDiskUsedBytes() == null || snapshot.getDiskTotalBytes() == null
                || snapshot.getDiskTotalBytes() == 0) {
            return 0.0;
        }
        return snapshot.getDiskUsedBytes() * 100.0 / snapshot.getDiskTotalBytes();
    }

    private void recordHistory(String groupId, String status) {
        Deque<String> values = healthHistory.computeIfAbsent(groupId, key -> new ArrayDeque<String>());
        synchronized (values) {
            values.addLast(status);
            while (values.size() > 60) {
                values.removeFirst();
            }
        }
    }

    private Map<String, Object> statusGroup(
            String id,
            String name,
            int serviceCount,
            List<String> children,
            String currentStatus) {
        Deque<String> values = healthHistory.get(id);
        List<String> history = new ArrayList<String>();
        if (values != null) {
            synchronized (values) {
                history.addAll(values);
            }
        }
        if (history.isEmpty()) {
            history.add(currentStatus);
        }

        List<String> displayHistory = new ArrayList<String>();
        while (displayHistory.size() + history.size() < 60) {
            displayHistory.add("unknown");
        }
        displayHistory.addAll(history);

        Map<String, Object> group = new LinkedHashMap<String, Object>();
        group.put("id", id);
        group.put("name", name);
        group.put("serviceCount", serviceCount);
        group.put("uptime", calculateUptime(history));
        group.put("status", currentStatus);
        group.put("history", displayHistory);
        group.put("children", children);
        return group;
    }

    private String calculateUptime(List<String> history) {
        if (history.isEmpty()) {
            return "-";
        }
        int operational = 0;
        int samples = 0;
        for (String value : history) {
            if ("operational".equals(value)) {
                operational++;
            }
            if ("operational".equals(value) || "degraded".equals(value) || "major_outage".equals(value)) {
                samples++;
            }
        }
        return samples == 0 ? "-" : String.format(Locale.ROOT, "%.2f%%", operational * 100.0 / samples);
    }

    private void persist(ServerSnapshot snapshot) {
        if (!persistenceEnabled) {
            return;
        }
        try {
            jdbcTemplate.update(UPSERT_SQL,
                    snapshot.getServerId(),
                    snapshot.getHostname(),
                    snapshot.getPlatform(),
                    toTimestamp(snapshot.getCollectedAt()),
                    toTimestamp(snapshot.getReceivedAt()),
                    snapshot.getCpuUsagePercent(),
                    snapshot.getLoad1(),
                    snapshot.getMemoryUsedBytes(),
                    snapshot.getMemoryTotalBytes(),
                    snapshot.getDiskUsedBytes(),
                    snapshot.getDiskTotalBytes(),
                    snapshot.getNetworkRxBytes(),
                    snapshot.getNetworkTxBytes(),
                    serializeContainers(snapshot.getContainers()));
        } catch (RuntimeException ex) {
            log.warn("写入 MySQL 快照失败，保留在内存中: {}", ex.getMessage());
        }
    }

    private ServerSnapshot readCache(String serverId) {
        if (!redisEnabled) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(serverId));
            if (json == null) {
                cacheMetricsService.recordMiss();
                return null;
            }
            cacheMetricsService.recordHit();
            return objectMapper.readValue(json, ServerSnapshot.class);
        } catch (Exception ex) {
            cacheMetricsService.recordError();
            log.warn("读取 Redis 缓存失败: {}", ex.getMessage());
            return null;
        }
    }

    private void writeCache(ServerSnapshot snapshot) {
        if (!redisEnabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    cacheKey(snapshot.getServerId()),
                    objectMapper.writeValueAsString(snapshot),
                    snapshotExpireSeconds,
                    TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("写入 Redis 缓存失败: {}", ex.getMessage());
        }
    }

    private String cacheKey(String serverId) {
        return CACHE_KEY_PREFIX + serverId;
    }

    private String serializeContainers(List<ContainerSnapshot> containers) {
        try {
            return objectMapper.writeValueAsString(containers == null
                    ? new ArrayList<ContainerSnapshot>()
                    : containers);
        } catch (Exception ex) {
            throw new IllegalStateException("无法序列化容器指标", ex);
        }
    }

    private List<ContainerSnapshot> parseContainers(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<ContainerSnapshot>();
        }
        try {
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ContainerSnapshot.class);
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("解析容器指标 JSON 失败: {}", ex.getMessage());
            return new ArrayList<ContainerSnapshot>();
        }
    }

    private Timestamp toTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private List<ContainerSnapshot> copyContainers(List<ContainerSnapshot> source) {
        List<ContainerSnapshot> result = new ArrayList<ContainerSnapshot>();
        if (source == null) {
            return result;
        }
        for (ContainerSnapshot item : source) {
            result.add(new ContainerSnapshot(
                    item.getName(),
                    item.getStatus(),
                    item.getCpuPercent(),
                    item.getMemoryUsage(),
                    item.getNetworkIo()));
        }
        return result;
    }

    private class SnapshotRowMapper implements RowMapper<ServerSnapshot> {
        @Override
        public ServerSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
            ServerSnapshot snapshot = new ServerSnapshot();
            snapshot.setServerId(rs.getString("server_id"));
            snapshot.setHostname(rs.getString("hostname"));
            snapshot.setPlatform(rs.getString("platform_name"));
            snapshot.setCollectedAt(toInstant(rs.getTimestamp("collected_at")));
            snapshot.setReceivedAt(toInstant(rs.getTimestamp("received_at")));
            snapshot.setCpuUsagePercent(rs.getObject("cpu_usage_percent", Double.class));
            snapshot.setLoad1(rs.getObject("load1", Double.class));
            snapshot.setMemoryUsedBytes(rs.getObject("memory_used_bytes", Long.class));
            snapshot.setMemoryTotalBytes(rs.getObject("memory_total_bytes", Long.class));
            snapshot.setDiskUsedBytes(rs.getObject("disk_used_bytes", Long.class));
            snapshot.setDiskTotalBytes(rs.getObject("disk_total_bytes", Long.class));
            snapshot.setNetworkRxBytes(rs.getObject("network_rx_bytes", Long.class));
            snapshot.setNetworkTxBytes(rs.getObject("network_tx_bytes", Long.class));
            snapshot.setContainers(parseContainers(rs.getString("containers_json")));
            return snapshot;
        }

        private Instant toInstant(Timestamp timestamp) {
            return timestamp == null ? null : timestamp.toInstant();
        }
    }
}
