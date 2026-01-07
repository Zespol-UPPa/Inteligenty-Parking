package com.smartparking.customer_service.messaging;

import com.smartparking.customer_service.config.EmailAmqpConfig;
import com.smartparking.customer_service.dto.TopUpConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class TopUpEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(TopUpEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public TopUpEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishTopUpConfirmation(String email, Long accountId, Long amountMinor, java.math.BigDecimal newBalance) {
        log.info("Publishing top-up confirmation event: email={}, accountId={}, amount={}", 
            email, accountId, amountMinor);
        
        try {
            TopUpConfirmationEvent event = new TopUpConfirmationEvent(email, accountId, amountMinor, newBalance);
            rabbitTemplate.convertAndSend(
                EmailAmqpConfig.EMAIL_EXCHANGE,
                EmailAmqpConfig.EMAIL_TOPUP_ROUTING,
                event
            );
            log.info("Top-up confirmation event published successfully: email={}", email);
        } catch (Exception e) {
            log.error("Failed to publish top-up confirmation event: email={}", email, e);
            // Don't throw - email failure shouldn't fail the top-up
        }
    }
}

