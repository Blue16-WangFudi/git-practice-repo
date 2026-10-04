package com.example.monitor.service;

import com.example.monitor.domain.StatusSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final Map<String, StatusSubscription> memorySubscriptions = new ConcurrentHashMap<String, StatusSubscription>();
    private final AtomicLong memoryId = new AtomicLong(1);
    private final JdbcTemplate jdbcTemplate;
    private final boolean persistenceEnabled;

    @Autowired
    public SubscriptionService(
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            @Value("${sentinel.features.persistence:false}") boolean persistenceEnabled) {
        this.jdbcTemplate = persistenceEnabled ? jdbcTemplateProvider.getIfAvailable() : null;
        this.persistenceEnabled = persistenceEnabled && this.jdbcTemplate != null;
    }

    public StatusSubscription subscribe(String email) {
        String normalized = email.trim().toLowerCase();
        Instant now = Instant.now();
        if (persistenceEnabled) {
            try {
                jdbcTemplate.update(
                        "INSERT INTO status_subscriptions (email, subscription_status, created_at, updated_at) " +
                                "VALUES (?, 'active', ?, ?) ON DUPLICATE KEY UPDATE subscription_status = 'active', updated_at = VALUES(updated_at)",
                        normalized, Timestamp.from(now), Timestamp.from(now));
                return jdbcTemplate.queryForObject(
                        "SELECT id, email, subscription_status, created_at, updated_at FROM status_subscriptions WHERE email = ?",
                        (rs, rowNum) -> new StatusSubscription(
                                rs.getLong("id"),
                                rs.getString("email"),
                                rs.getString("subscription_status"),
                                rs.getTimestamp("created_at").toInstant(),
                                rs.getTimestamp("updated_at").toInstant()),
                        normalized);
            } catch (RuntimeException ex) {
                log.warn("保存订阅失败，暂时使用内存数据: {}", ex.getMessage());
            }
        }

        StatusSubscription subscription = memorySubscriptions.get(normalized);
        if (subscription == null) {
            subscription = new StatusSubscription(memoryId.getAndIncrement(), normalized, "active", now, now);
        } else {
            subscription.setStatus("active");
            subscription.setUpdatedAt(now);
        }
        memorySubscriptions.put(normalized, subscription);
        return subscription;
    }
}
