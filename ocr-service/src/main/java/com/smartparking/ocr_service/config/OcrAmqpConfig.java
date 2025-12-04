package com.smartparking.ocr_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrAmqpConfig {
    public static final String OCR_EXCHANGE = "ocr.exchange";
    public static final String OCR_QUEUE = "ocr.queue";
    public static final String OCR_ROUTING = "ocr.routing";

    @Bean
    public TopicExchange exchange() { return new TopicExchange(OCR_EXCHANGE); }

    @Bean
    public Queue queue() { return new Queue(OCR_QUEUE); }

    @Bean
    public Binding binding(Queue q, TopicExchange e) {
        return BindingBuilder.bind(q).to(e).with(OCR_ROUTING);
    }
}
