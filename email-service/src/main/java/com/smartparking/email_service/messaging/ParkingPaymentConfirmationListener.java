package com.smartparking.email_service.messaging;

import com.smartparking.email_service.config.EmailAmqpConfig;
import com.smartparking.email_service.dto.ParkingPaymentConfirmationEvent;
import com.smartparking.email_service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ParkingPaymentConfirmationListener {
    private static final Logger log = LoggerFactory.getLogger(ParkingPaymentConfirmationListener.class);
    private final EmailService emailService;

    public ParkingPaymentConfirmationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = EmailAmqpConfig.EMAIL_PARKING_PAYMENT_QUEUE)
    public void onParkingPaymentConfirmation(ParkingPaymentConfirmationEvent event) {
        if (event == null || event.getEmail() == null) {
            log.error("Received invalid parking payment confirmation event: {}", event);
            return;
        }
        
        log.info("Received parking payment confirmation email request: email={}, sessionId={}", 
            event.getEmail(), event.getSessionId());
        
        try {
            double amount = (event.getAmountMinor() != null ? event.getAmountMinor() : 0) / 100.0;
            long durationHours = (event.getDurationMinutes() != null ? event.getDurationMinutes() : 0) / 60;
            long durationMinutes = (event.getDurationMinutes() != null ? event.getDurationMinutes() : 0) % 60;
            
            emailService.sendParkingPaymentConfirmationEmail(
                event.getEmail(),
                event.getEntryTime(),
                event.getExitTime(),
                amount,
                durationHours,
                durationMinutes
            );
            log.info("Parking payment confirmation email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send parking payment confirmation email to: {}", event.getEmail(), e);
            // Don't throw - email failure shouldn't fail the payment
        }
    }
}

