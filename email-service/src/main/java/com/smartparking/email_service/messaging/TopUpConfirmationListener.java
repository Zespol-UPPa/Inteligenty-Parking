package com.smartparking.email_service.messaging;

import com.smartparking.email_service.config.EmailAmqpConfig;
import com.smartparking.email_service.dto.TopUpConfirmationEvent;
import com.smartparking.email_service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TopUpConfirmationListener {
    private static final Logger log = LoggerFactory.getLogger(TopUpConfirmationListener.class);
    private final EmailService emailService;

    public TopUpConfirmationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = EmailAmqpConfig.EMAIL_TOPUP_QUEUE)
    public void onTopUpConfirmation(TopUpConfirmationEvent event) {
        if (event == null || event.getEmail() == null) {
            log.error("Received invalid top-up confirmation event: {}", event);
            return;
        }
        
        log.info("Received top-up confirmation email request: email={}, amount={}", 
            event.getEmail(), event.getAmountMinor());
        
        try {
            emailService.sendTopUpConfirmationEmail(
                event.getEmail(),
                event.getAmountMinor(),
                event.getNewBalance()
            );
            log.info("Top-up confirmation email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send top-up confirmation email to: {}", event.getEmail(), e);
            // Don't throw - email failure shouldn't fail the top-up
        }
    }
}

