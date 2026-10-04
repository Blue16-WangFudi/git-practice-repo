package com.example.monitor.service;

import com.example.monitor.config.RabbitConfig;
import com.example.monitor.domain.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sentinel.features.rabbitmq", havingValue = "true")
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consume(AlertEvent event) {
        log.warn("收到监控告警 server={} type={} severity={} message={}",
                event.getServerId(), event.getType(), event.getSeverity(), event.getMessage());
        // 下一步在这里接邮件、飞书 CLI 或状态事件自动创建。
    }
}
