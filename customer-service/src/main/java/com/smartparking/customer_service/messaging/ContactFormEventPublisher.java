package com.smartparking.customer_service.messaging;

import com.smartparking.customer_service.config.EmailAmqpConfig;
import com.smartparking.customer_service.dto.ContactFormEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContactFormEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ContactFormEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ContactFormEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishContactForm(ContactFormEvent event) {
        log.info("Publishing contact form event: email={}, name={}, subject={}", 
            event.getUserEmail(), event.getUserName(), event.getSubject());
        
        try {
            rabbitTemplate.convertAndSend(
                    EmailAmqpConfig.EMAIL_EXCHANGE,
                    EmailAmqpConfig.EMAIL_CONTACT_ROUTING,
                    event
            );
            log.info("Contact form event published successfully: email={}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to publish contact form event: email={}", event.getUserEmail(), e);
            throw e;
        }
    }
}

