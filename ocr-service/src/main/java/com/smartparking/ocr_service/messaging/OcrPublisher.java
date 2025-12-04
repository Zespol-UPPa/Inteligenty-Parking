package com.smartparking.ocr_service.messaging;

import com.smartparking.ocr_service.config.OcrAmqpConfig;
import com.smartparking.ocr_service.dto.OcrEventDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OcrPublisher {
    private final RabbitTemplate rabbitTemplate;

    public OcrPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDetected(OcrEventDto event) {
        rabbitTemplate.convertAndSend(OcrAmqpConfig.OCR_EXCHANGE, OcrAmqpConfig.OCR_ROUTING, event);
    }
}
