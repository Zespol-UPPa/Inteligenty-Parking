package com.smartparking.accounts_service.messaging;

import com.smartparking.accounts_service.config.EmailAmqpConfig;
import com.smartparking.accounts_service.dto.EmailVerificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EmailEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public EmailEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishVerificationEmail(EmailVerificationEvent event) {
        String tokenStr = event.getToken();
        String tokenPreview = tokenStr != null && tokenStr.length() > 16 
            ? tokenStr.substring(0, 8) + "..." + tokenStr.substring(tokenStr.length() - 8)
            : "***";
        log.info("Publishing verification email event: email={}, accountId={}, tokenPreview={}", 
            event.getEmail(), event.getAccountId(), tokenPreview);
        
        try {
            rabbitTemplate.convertAndSend(
                    EmailAmqpConfig.EMAIL_EXCHANGE,
                    EmailAmqpConfig.EMAIL_VERIFICATION_ROUTING,
                    event
            );
            log.info("Verification email event published successfully: email={}, tokenPreview={}", 
                event.getEmail(), tokenPreview);
        } catch (Exception e) {
            log.error("Failed to publish verification email event: email={}, tokenPreview={}", 
                event.getEmail(), tokenPreview, e);
            throw e;
        }
    }
}

