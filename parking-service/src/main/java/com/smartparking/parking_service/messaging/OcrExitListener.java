package com.smartparking.parking_service.messaging;

import com.smartparking.parking_service.config.ParkingAmqpConfig;
import com.smartparking.parking_service.dto.OcrExitEvent;
import com.smartparking.parking_service.service.ParkingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrExitListener {
    private static final Logger log = LoggerFactory.getLogger(OcrExitListener.class);
    private final ParkingSessionService sessionService;

    public OcrExitListener(ParkingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @RabbitListener(queues = ParkingAmqpConfig.PARKING_EXIT_QUEUE)
    public void handleOcrExit(OcrExitEvent event) {
        try {
            log.info("Received OCR exit event: plate={}, parking={}, camera={}", 
                event.getLicencePlate(), event.getParkingId(), event.getCameraId());
            
            ParkingSessionService.PaymentResult result = sessionService.processExit(
                event.getLicencePlate(),
                event.getParkingId(),
                event.getCameraId(),
                event.getTimestamp()
            );
            
            log.info("Processed exit for plate {}: payment {}", 
                event.getLicencePlate(), result.isSuccess() ? "success" : "failed");
        } catch (Exception e) {
            log.error("Failed to process exit for OCR event: {}", event, e);
            // Don't throw - message will be retried or sent to DLQ
        }
    }
}

