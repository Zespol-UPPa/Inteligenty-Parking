package com.smartparking.customer_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailAmqpConfig {
    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_CONTACT_ROUTING = "email.contact";
    public static final String EMAIL_TOPUP_ROUTING = "email.topup";
    public static final String EMAIL_RESERVATION_ROUTING = "email.reservation";
    public static final String EMAIL_PARKING_PAYMENT_ROUTING = "email.parking.payment";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EMAIL_EXCHANGE);
    }
}

