package com.example.monitor.service;

import com.example.monitor.config.RabbitConfig;
import com.example.monitor.domain.AlertEvent;
import com.example.monitor.domain.ServerSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);
    private static final long ALERT_COOLDOWN_SECONDS = 300;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Map<String, Instant> lastPublished = new ConcurrentHashMap<String, Instant>();

    @Autowired
    public AlertPublisher(
            ObjectProvider<RabbitTemplate> rabbitTemplateProvider,
            ObjectMapper objectMapper,
            @Value("${sentinel.features.rabbitmq:false}") boolean enabled) {
        this.rabbitTemplate = enabled ? rabbitTemplateProvider.getIfAvailable() : null;
        this.objectMapper = objectMapper;
        this.enabled = enabled && this.rabbitTemplate != null;
    }

    public void publishIfNeeded(ServerSnapshot snapshot) {
        if (!enabled) {
            return;
        }

        String type = alertType(snapshot);
        if (type == null) {
            return;
        }

        Instant now = Instant.now();
        Instant previous = lastPublished.get(snapshot.getServerId());
        if (previous != null && previous.plusSeconds(ALERT_COOLDOWN_SECONDS).isAfter(now)) {
            return;
        }

        AlertEvent event = new AlertEvent(
                snapshot.getServerId(),
                type,
                "warning",
                alertMessage(snapshot, type),
                now);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);
            lastPublished.put(snapshot.getServerId(), now);
            log.info("发布监控告警 server={} type={}", snapshot.getServerId(), type);
        } catch (RuntimeException ex) {
            log.warn("发布 RabbitMQ 告警失败: {}", ex.getMessage());
        }
    }

    private String alertType(ServerSnapshot snapshot) {
        if (snapshot.getCpuUsagePercent() != null && snapshot.getCpuUsagePercent() >= 90.0) {
            return "high_cpu";
        }
        if (snapshot.getMemoryUsedBytes() != null && snapshot.getMemoryTotalBytes() != null
                && snapshot.getMemoryTotalBytes() > 0
                && snapshot.getMemoryUsedBytes() * 100.0 / snapshot.getMemoryTotalBytes() >= 90.0) {
            return "high_memory";
        }
        if (snapshot.getDiskUsedBytes() != null && snapshot.getDiskTotalBytes() != null
                && snapshot.getDiskTotalBytes() > 0
                && snapshot.getDiskUsedBytes() * 100.0 / snapshot.getDiskTotalBytes() >= 90.0) {
            return "high_disk";
        }
        return null;
    }

    private String alertMessage(ServerSnapshot snapshot, String type) {
        if ("high_cpu".equals(type)) {
            return "CPU 使用率达到 " + snapshot.getCpuUsagePercent() + "%";
        }
        if ("high_memory".equals(type)) {
            return "内存使用率达到 " + percentage(snapshot.getMemoryUsedBytes(), snapshot.getMemoryTotalBytes()) + "%";
        }
        return "磁盘使用率达到 " + percentage(snapshot.getDiskUsedBytes(), snapshot.getDiskTotalBytes()) + "%";
    }

    private double percentage(Long used, Long total) {
        return Math.round(used * 10000.0 / total) / 100.0;
    }
}
