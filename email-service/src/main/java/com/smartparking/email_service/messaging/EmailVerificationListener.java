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
    public void recover(Exception e, EmailVerificationEvent event) {
        String errorDetails = extractErrorDetails(e);
        log.error("================================================");
        log.error("FAILED TO SEND VERIFICATION EMAIL AFTER {} ATTEMPTS", MAX_RETRY_ATTEMPTS);
        log.error("Recipient: {}", event.getEmail());
        log.error("Error type: {}", e.getClass().getSimpleName());
        log.error("Error message: {}", e.getMessage());
        if (errorDetails != null && !errorDetails.isEmpty()) {
            log.error("Error details: {}", errorDetails);
        }
        log.error("Message will be sent to Dead Letter Queue (DLQ)");
        log.error("Please check Gmail configuration (GMAIL_USERNAME and GMAIL_APP_PASSWORD)");
        log.error("================================================");
        // Message will be automatically sent to DLQ due to AmqpRejectAndDontRequeueException
        throw new AmqpRejectAndDontRequeueException(
                String.format("Failed to send verification email to %s after %d retries. Error: %s", 
                        event.getEmail(), MAX_RETRY_ATTEMPTS, errorDetails != null ? errorDetails : e.getMessage()), 
                e);
    }
    
    private String extractErrorDetails(Exception e) {
        if (e == null) {
            return null;
        }
        
        String message = e.getMessage();
        if (message != null) {
            // Check for common error patterns
            if (message.contains("Authentication failed") || message.contains("535")) {
                return "Gmail authentication failed. Please verify GMAIL_USERNAME and GMAIL_APP_PASSWORD. " +
                       "Make sure you are using Gmail App Password (not regular password).";
            }
            if (message.contains("Connection") || message.contains("timeout")) {
                return "Connection error. Please check network connectivity and Gmail SMTP settings.";
            }
            if (message.contains("not configured") || message.contains("GMAIL_USERNAME")) {
                return "Gmail configuration is missing. Please set GMAIL_USERNAME and GMAIL_APP_PASSWORD environment variables.";
            }
        }
        
        // Check cause
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage.contains("535") || causeMessage.contains("Username and Password not accepted")) {
                return "Gmail rejected credentials. Please verify GMAIL_APP_PASSWORD is correct and account has 2-Step Verification enabled.";
            }
        }
        
        return message;
    }
}

