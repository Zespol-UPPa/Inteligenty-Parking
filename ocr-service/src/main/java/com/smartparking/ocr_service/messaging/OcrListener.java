package com.smartparking.ocr_service.messaging;

import com.smartparking.ocr_service.config.OcrAmqpConfig;
import com.smartparking.ocr_service.dto.OcrEventDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OcrListener {

    @RabbitListener(queues = OcrAmqpConfig.OCR_QUEUE)
    public void receive(OcrEventDto event) {
        // Tu możemy zapisać event do DB lub wysłać dalej jako PlateProcessed
        System.out.println("OCR Listener received: " + event.getPlate() + " at " + event.getTimestamp());
        // TODO: dalsze przetwarzanie
    }
}
