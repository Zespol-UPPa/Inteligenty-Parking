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
            String plate = event.getLicencePlate();
            log.info("License plate recognized: plate={}, parking={}, camera={}, timestamp={}", 
                plate, event.getParkingId(), event.getCameraId(), event.getTimestamp());
            
            Long sessionId = sessionService.createSessionOnEntry(
                event.getLicencePlate(),
                event.getParkingId(),
                event.getCameraId(),
                event.getTimestamp()
            );
            
            log.info("Entry processed: plate={}, parking={}, sessionId={}", 
                plate, event.getParkingId(), sessionId);
        } catch (IllegalStateException e) {
            // To może być błąd duplikatu - już zalogowano w ParkingSessionService
            log.warn("Entry event rejected: plate={}, parking={}, reason={}", 
                event.getLicencePlate(), event.getParkingId(), e.getMessage());
            // Nie rzucaj wyjątku - event był zduplikowany i został poprawnie obsłużony
        } catch (Exception e) {
            log.error("Failed to process OCR entry event: plate={}, parking={}, error={}", 
                event.getLicencePlate(), event.getParkingId(), e.getMessage(), e);
        }
    }
}

