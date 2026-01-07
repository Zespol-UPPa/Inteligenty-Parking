package com.smartparking.parking_service.messaging;

import com.smartparking.parking_service.client.AccountClient;
import com.smartparking.parking_service.dto.ParkingPaymentConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ParkingPaymentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ParkingPaymentEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final AccountClient accountClient;

    public ParkingPaymentEventPublisher(RabbitTemplate rabbitTemplate, AccountClient accountClient) {
        this.rabbitTemplate = rabbitTemplate;
        this.accountClient = accountClient;
    }

    public void publishParkingPaymentConfirmation(Long accountId, Long sessionId,
                                                 Instant entryTime, Instant exitTime,
                                                 Long amountMinor, Long durationMinutes) {
        String email = accountClient.getEmailByAccountId(accountId).orElse(null);
        if (email == null) {
            log.warn("No email found for accountId {}, skipping payment confirmation email", accountId);
            return;
        }
        
        log.info("Publishing parking payment confirmation event: email={}, accountId={}, sessionId={}", 
            email, accountId, sessionId);
        
        try {
            ParkingPaymentConfirmationEvent event = new ParkingPaymentConfirmationEvent(
                email, accountId, sessionId, entryTime, exitTime, amountMinor, durationMinutes
            );
            rabbitTemplate.convertAndSend(
                "email.exchange", // Użyj tego samego exchange co customer-service
                "email.parking.payment",
                event
            );
            log.info("Parking payment confirmation event published successfully: email={}", email);
        } catch (Exception e) {
            log.error("Failed to publish parking payment confirmation event: email={}", email, e);
            // Don't throw - email failure shouldn't fail the payment
        }
    }
}

