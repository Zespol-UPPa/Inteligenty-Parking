package com.smartparking.ocr_service.messaging;

import com.smartparking.ocr_service.config.OcrAmqpConfig;
import com.smartparking.ocr_service.dto.OcrEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class OcrPublisher {
    private static final Logger log = LoggerFactory.getLogger(OcrPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public OcrPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDetected(OcrEventDto event) {
        rabbitTemplate.convertAndSend(OcrAmqpConfig.OCR_EXCHANGE, OcrAmqpConfig.OCR_ROUTING, event);
        
        // Jeśli event ma direction i parkingId, publikuj również event parkingowy
        if (event.getDirection() != null && event.getParkingId() != null) {
            try {
                Instant timestamp = parseTimestamp(event.getTimestamp());
                Integer cameraId = event.getCameraId() != null ? event.getCameraId() : 1; // Default camera
                
                if ("entry".equalsIgnoreCase(event.getDirection())) {
                    publishEntry(event.getPlate(), event.getParkingId(), cameraId, timestamp);
                } else if ("exit".equalsIgnoreCase(event.getDirection())) {
                    publishExit(event.getPlate(), event.getParkingId(), cameraId, timestamp);
                }
            } catch (Exception e) {
                log.warn("Failed to publish parking entry/exit event: {}", e.getMessage());
            }
        }
    }

    public void publishEntry(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp) {
        try {
            Map<String, Object> event = Map.of(
                "licencePlate", licencePlate,
                "parkingId", parkingId,
                "cameraId", cameraId,
                "timestamp", timestamp.toString()
            );
            rabbitTemplate.convertAndSend(OcrAmqpConfig.PARKING_EXCHANGE, OcrAmqpConfig.PARKING_ENTRY_ROUTING, event);
            log.debug("Published parking entry event: plate={}, parking={}", licencePlate, parkingId);
        } catch (Exception e) {
            log.error("Failed to publish parking entry event", e);
        }
    }

    public void publishExit(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp) {
        try {
            Map<String, Object> event = Map.of(
                "licencePlate", licencePlate,
                "parkingId", parkingId,
                "cameraId", cameraId,
                "timestamp", timestamp.toString()
            );
            rabbitTemplate.convertAndSend(OcrAmqpConfig.PARKING_EXCHANGE, OcrAmqpConfig.PARKING_EXIT_ROUTING, event);
            log.debug("Published parking exit event: plate={}, parking={}", licencePlate, parkingId);
        } catch (Exception e) {
            log.error("Failed to publish parking exit event", e);
        }
    }

    private Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null) {
            return Instant.now();
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
            return ldt.atZone(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            try {
                long epoch = Long.parseLong(timestampStr);
                return Instant.ofEpochSecond(epoch);
            } catch (Exception ex) {
                return Instant.now();
            }
        }
    }
}

