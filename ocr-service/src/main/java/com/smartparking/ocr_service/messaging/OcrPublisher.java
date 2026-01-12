package com.smartparking.ocr_service.messaging;

import com.smartparking.ocr_service.config.OcrAmqpConfig;
import com.smartparking.ocr_service.dto.OcrEventDto;
import com.smartparking.ocr_service.dto.ParkingEntryEvent;
import com.smartparking.ocr_service.dto.ParkingExitEvent;
import com.smartparking.ocr_service.service.EventDeduplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class OcrPublisher {
    private static final Logger log = LoggerFactory.getLogger(OcrPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final EventDeduplicationService deduplicationService;

    public OcrPublisher(RabbitTemplate rabbitTemplate, EventDeduplicationService deduplicationService) {
        this.rabbitTemplate = rabbitTemplate;
        this.deduplicationService = deduplicationService;
    }

    public void publishDetected(OcrEventDto event) {
        rabbitTemplate.convertAndSend(OcrAmqpConfig.OCR_EXCHANGE, OcrAmqpConfig.OCR_ROUTING, event);
        
        // Jeśli event ma direction i parkingId, publikuj również event parkingowy
        if (event.getDirection() != null && event.getParkingId() != null) {
            try {
                Instant timestamp = parseTimestamp(event.getTimestamp());
                Integer cameraId = event.getCameraId() != null ? event.getCameraId() : 1; // Default camera
                
                // Sprawdź duplikaty przed publikacją
                boolean isDuplicate = deduplicationService.isDuplicate(
                    event.getPlate(), 
                    event.getParkingId(), 
                    event.getDirection(), 
                    timestamp
                );
                
                if (isDuplicate) {
                    log.warn("Duplicate event filtered out - not publishing: plate={}, parkingId={}, direction={}",
                        event.getPlate(), event.getParkingId(), event.getDirection());
                    return; // Nie publikuj duplikatu
                }
                
                // Event jest unikalny - zarejestruj go i publikuj
                deduplicationService.registerEvent(event.getPlate(), event.getParkingId(), event.getDirection(), timestamp);
                
                if ("entry".equalsIgnoreCase(event.getDirection())) {
                    publishEntryInternal(event.getPlate(), event.getParkingId(), cameraId, timestamp, false);
                } else if ("exit".equalsIgnoreCase(event.getDirection())) {
                    publishExitInternal(event.getPlate(), event.getParkingId(), cameraId, timestamp, false);
                }
            } catch (Exception e) {
                log.warn("Failed to publish parking entry/exit event: {}", e.getMessage());
            }
        }
    }

    public void publishEntry(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp) {
        publishEntryInternal(licencePlate, parkingId, cameraId, timestamp, true);
    }
    
    private void publishEntryInternal(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp, boolean checkDup) {
        try {
            Instant eventTime = timestamp != null ? timestamp : Instant.now();
            
            // Sprawdzenie duplikatów tylko jeśli wymagane (dla bezpośrednich wywołań)
            if (checkDup && deduplicationService.isDuplicate(licencePlate, parkingId, "entry", eventTime)) {
                log.warn("Duplicate entry event detected in publishEntry() - skipping: plate={}, parkingId={}",
                    licencePlate, parkingId);
                return;
            }
            
            // Rejestruj event tylko jeśli nie był wcześniej zarejestrowany
            if (checkDup) {
                deduplicationService.registerEvent(licencePlate, parkingId, "entry", eventTime);
            }
            
            ParkingEntryEvent event = new ParkingEntryEvent();
            event.setLicencePlate(licencePlate);
            event.setParkingId(parkingId);
            event.setCameraId(cameraId != null ? cameraId : 1); // Default camera ID
            event.setTimestamp(eventTime);
            
            rabbitTemplate.convertAndSend(OcrAmqpConfig.PARKING_EXCHANGE, OcrAmqpConfig.PARKING_ENTRY_ROUTING, event);
            log.info("Published parking entry event: plate={}, parking={}, camera={}, timestamp={}", 
                    licencePlate, parkingId, event.getCameraId(), event.getTimestamp());
        } catch (Exception e) {
            log.error("Failed to publish parking entry event: plate={}, parking={}", licencePlate, parkingId, e);
        }
    }

    public void publishExit(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp) {
        publishExitInternal(licencePlate, parkingId, cameraId, timestamp, true);
    }
    
    private void publishExitInternal(String licencePlate, Long parkingId, Integer cameraId, Instant timestamp, boolean checkDup) {
        try {
            Instant eventTime = timestamp != null ? timestamp : Instant.now();
            
            // Sprawdzenie duplikatów tylko jeśli wymagane (dla bezpośrednich wywołań)
            if (checkDup && deduplicationService.isDuplicate(licencePlate, parkingId, "exit", eventTime)) {
                log.warn("Duplicate exit event detected in publishExit() - skipping: plate={}, parkingId={}",
                    licencePlate, parkingId);
                return;
            }
            
            // Rejestruj event tylko jeśli nie był wcześniej zarejestrowany
            if (checkDup) {
                deduplicationService.registerEvent(licencePlate, parkingId, "exit", eventTime);
            }
            
            ParkingExitEvent event = new ParkingExitEvent();
            event.setLicencePlate(licencePlate);
            event.setParkingId(parkingId);
            event.setCameraId(cameraId != null ? cameraId : 1); // Default camera ID
            event.setTimestamp(eventTime);
            
            rabbitTemplate.convertAndSend(OcrAmqpConfig.PARKING_EXCHANGE, OcrAmqpConfig.PARKING_EXIT_ROUTING, event);
            log.info("Published parking exit event: plate={}, parking={}, camera={}, timestamp={}", 
                    licencePlate, parkingId, event.getCameraId(), event.getTimestamp());
        } catch (Exception e) {
            log.error("Failed to publish parking exit event: plate={}, parking={}", licencePlate, parkingId, e);
        }
    }

    private Instant parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return Instant.now();
        }
        try {
            // Try parsing as ISO-8601 with 'Z' suffix (e.g., "2026-01-07T14:00:00Z")
            if (timestampStr.endsWith("Z")) {
                return Instant.parse(timestampStr);
            }
            // Try parsing as ISO-8601 without timezone (assume UTC)
            if (timestampStr.contains("T")) {
                LocalDateTime ldt = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
                return ldt.atZone(ZoneOffset.UTC).toInstant();
            }
            // Try parsing as epoch seconds
            long epoch = Long.parseLong(timestampStr);
            return Instant.ofEpochSecond(epoch);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp '{}', using current time: {}", timestampStr, e.getMessage());
            return Instant.now();
        }
    }
}

