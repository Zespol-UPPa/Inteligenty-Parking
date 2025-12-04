package com.smartparking.ocr_service.controller;

import com.smartparking.ocr_service.dto.OcrEventDto;
import com.smartparking.ocr_service.messaging.OcrPublisher;
import com.smartparking.ocr_service.repo.PlateReadRepository;
import com.smartparking.ocr_service.model.PlateRead;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocr")
public class OcrWebhookController {

    private final OcrPublisher publisher;
    private final PlateReadRepository repo;

    public OcrWebhookController(OcrPublisher publisher, PlateReadRepository repo) {
        this.publisher = publisher;
        this.repo = repo;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ocr-service: OK");
    }

    // Endpoint który przyjmie POST od zewnętrznego skryptu z danymi (plate, timestamp, imageUrl)
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody OcrEventDto event) {
        // Parsowanie timestamp (ISO-8601 lub epoch seconds)
        LocalDateTime ts = null;
        if (event.getTimestamp() != null) {
            try {
                ts = LocalDateTime.parse(event.getTimestamp(), DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception ex) {
                try {
                    long epoch = Long.parseLong(event.getTimestamp());
                    ts = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
                } catch (Exception ex2) {
                    ts = LocalDateTime.now(ZoneOffset.UTC);
                }
            }
        } else {
            ts = LocalDateTime.now(ZoneOffset.UTC);
        }

        PlateRead p = new PlateRead();
        p.setCameraId(1); // TODO: map camera by imageUrl or camera_code
        p.setRawPlate(event.getPlate());
        p.setEventTime(ts);

        repo.save(p);
        try {
            publisher.publishDetected(event);
        } catch (Exception e) {
            // if rabbit not available, still accept
        }
        return ResponseEntity.ok("accepted");
    }
}
