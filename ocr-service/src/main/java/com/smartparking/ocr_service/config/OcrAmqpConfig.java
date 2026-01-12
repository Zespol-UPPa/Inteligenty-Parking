package com.smartparking.ocr_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrAmqpConfig {
    public static final String OCR_EXCHANGE = "ocr.exchange";
    public static final String OCR_QUEUE = "ocr.queue";
    public static final String OCR_ROUTING = "ocr.routing";
    
    // Parking exchange for entry/exit events
    public static final String PARKING_EXCHANGE = "parking.exchange";
    public static final String PARKING_ENTRY_ROUTING = "parking.entry.detected";
    public static final String PARKING_EXIT_ROUTING = "parking.exit.detected";

    @Bean
    public TopicExchange exchange() { return new TopicExchange(OCR_EXCHANGE); }

    @Bean
    public TopicExchange parkingExchange() { return new TopicExchange(PARKING_EXCHANGE); }

    @Bean
    public Queue queue() { return new Queue(OCR_QUEUE); }

    @Bean
    public Binding binding(Queue q, @Qualifier("exchange") TopicExchange e) {
        return BindingBuilder.bind(q).to(e).with(OCR_ROUTING);
    }
}
