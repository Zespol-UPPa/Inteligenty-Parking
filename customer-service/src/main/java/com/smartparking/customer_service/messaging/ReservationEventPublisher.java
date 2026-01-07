package com.smartparking.customer_service.messaging;

import com.smartparking.customer_service.config.EmailAmqpConfig;
import com.smartparking.customer_service.dto.ReservationConfirmationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ReservationEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public ReservationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishReservationConfirmation(String email, Long accountId, Long reservationId,
                                              Long parkingId, Long spotId, Instant startTime,
                                              Instant endTime, String parkingName) {
        log.info("Publishing reservation confirmation event: email={}, accountId={}, reservationId={}", 
            email, accountId, reservationId);
        
        try {
            ReservationConfirmationEvent event = new ReservationConfirmationEvent(
                email, accountId, reservationId, parkingId, spotId, startTime, endTime, parkingName
            );
            rabbitTemplate.convertAndSend(
                EmailAmqpConfig.EMAIL_EXCHANGE,
                EmailAmqpConfig.EMAIL_RESERVATION_ROUTING,
                event
            );
            log.info("Reservation confirmation event published successfully: email={}", email);
        } catch (Exception e) {
            log.error("Failed to publish reservation confirmation event: email={}", email, e);
            // Don't throw - email failure shouldn't fail the reservation
        }
    }
}

