package com.smartparking.accounts_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailAmqpConfig {
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_VERIFICATION_QUEUE = "email.verification.queue";
    public static final String EMAIL_VERIFICATION_ROUTING = "email.verification";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Queue emailVerificationQueue() {
        return new Queue(EMAIL_VERIFICATION_QUEUE);
    }

    @Bean
    public Binding emailVerificationBinding(Queue emailVerificationQueue, TopicExchange emailExchange) {
        return BindingBuilder.bind(emailVerificationQueue)
                .to(emailExchange)
                .with(EMAIL_VERIFICATION_ROUTING);
    }
}

