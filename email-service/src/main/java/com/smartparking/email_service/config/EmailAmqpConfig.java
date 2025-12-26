package com.smartparking.email_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailAmqpConfig {
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_VERIFICATION_QUEUE = "email.verification.queue";
    public static final String EMAIL_VERIFICATION_ROUTING = "email.verification";
    public static final String EMAIL_DLX = "email.dlx";
    public static final String EMAIL_DLQ = "email.verification.dlq";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public TopicExchange emailDlx() {
        return new TopicExchange(EMAIL_DLX);
    }

    @Bean
    public Queue emailVerificationQueue() {
        return QueueBuilder.durable(EMAIL_VERIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailVerificationBinding(Queue emailVerificationQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailVerificationQueue)
                .to(emailExchange)
                .with(EMAIL_VERIFICATION_ROUTING);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, TopicExchange emailDlx) {
        return BindingBuilder.bind(emailDlq)
                .to(emailDlx)
                .with(EMAIL_DLQ);
    }
}

