package com.smartparking.ocr_service.web;

import com.smartparking.ocr_service.config.OcrAmqpConfig;
import com.smartparking.ocr_service.web.dto.OcrEventDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {
    private final RabbitTemplate rt;

    public OcrController(RabbitTemplate rt) { this.rt = rt; }

    @PostMapping("/event")
    public ResponseEntity<String> receive(@RequestBody OcrEventDto dto) {
        rt.convertAndSend(OcrAmqpConfig.OCR_EXCHANGE, OcrAmqpConfig.OCR_ROUTING, dto);
        return ResponseEntity.ok("ok");
    }
}
