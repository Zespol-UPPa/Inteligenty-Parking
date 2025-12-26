package com.smartparking.email_service.messaging;

import com.smartparking.email_service.config.EmailAmqpConfig;
import com.smartparking.email_service.dto.EmailVerificationEvent;
import com.smartparking.email_service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationListener {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationListener.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private final EmailService emailService;

    public EmailVerificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = EmailAmqpConfig.EMAIL_VERIFICATION_QUEUE)
    @Retryable(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 2000, multiplier = 2),
            retryFor = {Exception.class}
    )
    public void onVerificationEmail(EmailVerificationEvent event) {
        // Validate event data before processing
        if (event == null || event.getEmail() == null || event.getVerificationUrl() == null) {
            log.error("Received invalid email verification event: {}", event);
            throw new IllegalArgumentException("Invalid email verification event");
        }
        
        log.info("Received verification email request for: {} (attempt)", event.getEmail());
        emailService.sendVerificationEmail(event.getEmail(), event.getVerificationUrl());
        log.info("Verification email sent successfully to: {}", event.getEmail());
    }

    @Recover
    public void recover(AmqpRejectAndDontRequeueException e, EmailVerificationEvent event) {
        log.error("Failed to send verification email to: {} after {} attempts. Message will be sent to DLQ.", 
                event.getEmail(), MAX_RETRY_ATTEMPTS);
        // Message will be automatically sent to DLQ due to AmqpRejectAndDontRequeueException
        throw new AmqpRejectAndDontRequeueException("Failed to send verification email after " + MAX_RETRY_ATTEMPTS + " retries", e);
    }
}

