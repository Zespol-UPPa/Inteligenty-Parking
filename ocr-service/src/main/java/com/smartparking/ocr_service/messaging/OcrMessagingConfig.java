package com.smartparking.ocr_service.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrMessagingConfig {
    public static final String EXCHANGE = "parking.exchange";
    public static final String ROUTING_DETECTED = "plate.detected";
    public static final String ROUTING_PROCESSED = "plate.processed";
    public static final String QUEUE_OCR = "ocr.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue ocrQueue() {
        return new Queue(QUEUE_OCR, true);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(ocrQueue()).to(exchange()).with(ROUTING_DETECTED);
    }
}
