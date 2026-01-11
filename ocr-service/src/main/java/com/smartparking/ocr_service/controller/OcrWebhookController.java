package com.smartparking.ocr_service.controller;

import com.smartparking.ocr_service.dto.OcrEventDto;
import com.smartparking.ocr_service.messaging.OcrPublisher;
import com.smartparking.ocr_service.repo.PlateReadRepository;
import com.smartparking.ocr_service.model.PlateRead;
import com.smartparking.ocr_service.service.EventDeduplicationService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocr")
public class OcrWebhookController {
    private static final Logger log = LoggerFactory.getLogger(OcrWebhookController.class);
    
    private final OcrPublisher publisher;
    private final PlateReadRepository repo;
    private final EventDeduplicationService deduplicationService;

    public OcrWebhookController(OcrPublisher publisher, PlateReadRepository repo, 
                                EventDeduplicationService deduplicationService) {
        this.publisher = publisher;
        this.repo = repo;
        this.deduplicationService = deduplicationService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ocr-service: OK");
    }
    
    @GetMapping("/stats/dedup")
    public ResponseEntity<Map<String, Object>> getDedupStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("recentEventsCount", deduplicationService.getRecentEventsCount());
        stats.put("deduplicationWindowSeconds", 60);
        stats.put("status", "active");
        return ResponseEntity.ok(stats);
    }

    // Endpoint który przyjmie POST od zewnętrznego skryptu z danymi (plate, timestamp, imageUrl)
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody OcrEventDto event) {
        // Walidacja wymaganych pól
        if (event == null) {
            log.warn("Received null event in webhook");
            return ResponseEntity.badRequest().body("Event is required");
        }
        
        if (event.getPlate() == null || event.getPlate().trim().isEmpty()) {
            log.warn("Received event without plate");
            return ResponseEntity.badRequest().body("Plate is required");
        }
        
        // Parsowanie timestamp (ISO-8601 z 'Z', bez 'Z', lub epoch seconds)
        LocalDateTime ts = parseTimestamp(event.getTimestamp());
        
        // Użyj cameraId z eventu, jeśli dostępne, w przeciwnym razie domyślne 1
        Integer cameraId = event.getCameraId() != null ? event.getCameraId() : 1;

        // Zapisz do bazy danych
        PlateRead p = new PlateRead();
        p.setCameraId(cameraId);
        p.setRawPlate(event.getPlate().trim().toUpperCase());
        p.setEventTime(ts);
        
        try {
            repo.save(p);
            log.info("Saved plate read to database: plate={}, cameraId={}, timestamp={}", 
                    p.getRawPlate(), cameraId, ts);
        } catch (Exception e) {
            if (e instanceof org.springframework.dao.DuplicateKeyException) {
                log.warn("Duplicate key detected for plate={}, should have been handled by repository", 
                    event.getPlate());
            } else {
                log.error("Failed to save plate read to database: plate={}", event.getPlate(), e);
            }
        }
        
        // Publikuj event do RabbitMQ
        try {
            publisher.publishDetected(event);
            log.debug("Published OCR event: plate={}, direction={}, parkingId={}", 
                    event.getPlate(), event.getDirection(), event.getParkingId());
        } catch (Exception e) {
            log.error("Failed to publish event to RabbitMQ: plate={}", event.getPlate(), e);
            // Jeśli RabbitMQ nie jest dostępne, zwróć błąd 500
            return ResponseEntity.status(500).body("Failed to process event: " + e.getMessage());
        }
        
        return ResponseEntity.ok("accepted");
    }
    
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        
        try {
            // Try parsing as ISO-8601 with 'Z' suffix (e.g., "2026-01-07T14:00:00Z")
            if (timestampStr.endsWith("Z")) {
                Instant instant = Instant.parse(timestampStr);
                return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            }
            // Try parsing as ISO-8601 without timezone (assume UTC)
            if (timestampStr.contains("T")) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
                    return ldt;
                } catch (Exception e) {
                    // Try parsing with offset if present
                    Instant instant = Instant.parse(timestampStr);
                    return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
                }
            }
            // Try parsing as epoch seconds
            long epoch = Long.parseLong(timestampStr);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp '{}', using current time: {}", timestampStr, e.getMessage());
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}
