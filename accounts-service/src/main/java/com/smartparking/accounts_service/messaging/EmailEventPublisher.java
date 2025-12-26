package com.smartparking.accounts_service.messaging;

import com.smartparking.accounts_service.config.EmailAmqpConfig;
import com.smartparking.accounts_service.dto.EmailVerificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public EmailEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishVerificationEmail(EmailVerificationEvent event) {
        rabbitTemplate.convertAndSend(
                EmailAmqpConfig.EMAIL_EXCHANGE,
                EmailAmqpConfig.EMAIL_VERIFICATION_ROUTING,
                event
        );
    }
}

