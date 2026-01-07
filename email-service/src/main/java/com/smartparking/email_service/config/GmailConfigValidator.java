package com.smartparking.email_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GmailConfigValidator {
    private static final Logger log = LoggerFactory.getLogger(GmailConfigValidator.class);
    
    private final String gmailUsername;
    private final String gmailAppPassword;
    
    public GmailConfigValidator(
            @Value("${spring.mail.username:}") String gmailUsername,
            @Value("${spring.mail.password:}") String gmailAppPassword) {
        this.gmailUsername = gmailUsername;
        this.gmailAppPassword = gmailAppPassword;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateGmailConfiguration() {
        boolean hasErrors = false;
        
        if (gmailUsername == null || gmailUsername.isBlank()) {
            log.error("================================================");
            log.error("GMAIL_USERNAME is not set or is empty!");
            log.error("Email service will not be able to send emails.");
            log.error("Please set GMAIL_USERNAME environment variable.");
            log.error("================================================");
            hasErrors = true;
        }
        
        if (gmailAppPassword == null || gmailAppPassword.isBlank()) {
            log.error("================================================");
            log.error("GMAIL_APP_PASSWORD is not set or is empty!");
            log.error("Email service will not be able to send emails.");
            log.error("Please set GMAIL_APP_PASSWORD environment variable.");
            log.error("To get Gmail App Password:");
            log.error("1. Go to https://myaccount.google.com/security");
            log.error("2. Enable '2-Step Verification' if not already enabled");
            log.error("3. Go to 'App passwords' section");
            log.error("4. Generate a new App Password for 'Mail'");
            log.error("5. Copy the 16-character password (without spaces)");
            log.error("6. Add it to your .env file as: GMAIL_APP_PASSWORD=your-password");
            log.error("================================================");
            hasErrors = true;
        }
        
        if (hasErrors) {
            log.warn("Email service started but Gmail configuration is incomplete. Emails will fail to send.");
        } else {
            log.info("Gmail configuration validated successfully. Email service is ready to send emails.");
            log.debug("Gmail username: {}", maskEmail(gmailUsername));
        }
    }
    
    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}

