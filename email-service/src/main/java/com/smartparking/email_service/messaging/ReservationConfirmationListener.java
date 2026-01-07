package com.smartparking.email_service.messaging;

import com.smartparking.email_service.config.EmailAmqpConfig;
import com.smartparking.email_service.dto.ReservationConfirmationEvent;
import com.smartparking.email_service.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationConfirmationListener {
    private static final Logger log = LoggerFactory.getLogger(ReservationConfirmationListener.class);
    private final EmailService emailService;

    public ReservationConfirmationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = EmailAmqpConfig.EMAIL_RESERVATION_QUEUE)
    public void onReservationConfirmation(ReservationConfirmationEvent event) {
        if (event == null || event.getEmail() == null) {
            log.error("Received invalid reservation confirmation event: {}", event);
            return;
        }
        
        log.info("Received reservation confirmation email request: email={}, reservationId={}", 
            event.getEmail(), event.getReservationId());
        
        try {
            emailService.sendReservationConfirmationEmail(
                event.getEmail(),
                event.getParkingName() != null ? event.getParkingName() : "Unknown Parking",
                event.getSpotId(),
                event.getStartTime(),
                event.getEndTime()
            );
            log.info("Reservation confirmation email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send reservation confirmation email to: {}", event.getEmail(), e);
            // Don't throw - email failure shouldn't fail the reservation
        }
    }
}

