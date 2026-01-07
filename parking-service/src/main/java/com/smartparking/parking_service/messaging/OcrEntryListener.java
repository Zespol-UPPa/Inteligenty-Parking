package com.smartparking.parking_service.messaging;

import com.smartparking.parking_service.config.ParkingAmqpConfig;
import com.smartparking.parking_service.dto.OcrEntryEvent;
import com.smartparking.parking_service.service.ParkingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrEntryListener {
    private static final Logger log = LoggerFactory.getLogger(OcrEntryListener.class);
    private final ParkingSessionService sessionService;

    public OcrEntryListener(ParkingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @RabbitListener(queues = ParkingAmqpConfig.PARKING_ENTRY_QUEUE)
    public void handleOcrEntry(OcrEntryEvent event) {
        try {
            log.info("Received OCR entry event: plate={}, parking={}, camera={}", 
                event.getLicencePlate(), event.getParkingId(), event.getCameraId());
            
            Long sessionId = sessionService.createSessionOnEntry(
                event.getLicencePlate(),
                event.getParkingId(),
                event.getCameraId(),
                event.getTimestamp()
            );
            
            log.info("Created parking session {} for plate {} at parking {}", 
                sessionId, event.getLicencePlate(), event.getParkingId());
        } catch (Exception e) {
            log.error("Failed to create parking session for OCR entry event: {}", event, e);
            // Don't throw - message will be retried or sent to DLQ
        }
    }
}

