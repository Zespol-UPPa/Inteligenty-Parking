package com.smartparking.parking_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ParkingAmqpConfig {
    public static final String PARKING_EXCHANGE = "parking.exchange";
    public static final String PARKING_ENTRY_QUEUE = "parking.entry.queue";
    public static final String PARKING_ENTRY_ROUTING = "parking.entry.detected";
    public static final String PARKING_EXIT_QUEUE = "parking.exit.queue";
    public static final String PARKING_EXIT_ROUTING = "parking.exit.detected";

    @Bean
    public TopicExchange parkingExchange() {
        return new TopicExchange(PARKING_EXCHANGE);
    }

    @Bean
    public Queue parkingEntryQueue() {
        return QueueBuilder.durable(PARKING_ENTRY_QUEUE).build();
    }

    @Bean
    public Queue parkingExitQueue() {
        return QueueBuilder.durable(PARKING_EXIT_QUEUE).build();
    }

    @Bean
    public Binding parkingEntryBinding(Queue parkingEntryQueue, TopicExchange parkingExchange) {
        return BindingBuilder.bind(parkingEntryQueue)
                .to(parkingExchange)
                .with(PARKING_ENTRY_ROUTING);
    }

    @Bean
    public Binding parkingExitBinding(Queue parkingExitQueue, TopicExchange parkingExchange) {
        return BindingBuilder.bind(parkingExitQueue)
                .to(parkingExchange)
                .with(PARKING_EXIT_ROUTING);
    }
}

