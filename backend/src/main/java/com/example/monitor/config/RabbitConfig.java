package com.example.monitor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@ConditionalOnProperty(name = "sentinel.features.rabbitmq", havingValue = "true")
public class RabbitConfig {

    public static final String EXCHANGE = "sentinel.alerts";
    public static final String QUEUE = "sentinel.alerts.queue";
    public static final String ROUTING_KEY = "alert.created";

    @Bean
    public DirectExchange alertExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue alertQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding alertBinding(Queue alertQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(alertQueue).to(alertExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
