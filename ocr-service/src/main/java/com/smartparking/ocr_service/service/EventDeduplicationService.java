package com.smartparking.ocr_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serwis deduplikacji eventów OCR w oknie czasowym.
 * Zapobiega przetwarzaniu tego samego eventu wjazdu/wyjazdu wielokrotnie
 * w krótkim przedziale czasu (np. 60 sekund).
 */
@Service
public class EventDeduplicationService {
    private static final Logger log = LoggerFactory.getLogger(EventDeduplicationService.class);
    
    // Okno czasowe deduplikacji w sekundach (60 sekund)
    private static final int DEDUPLICATION_WINDOW_SECONDS = 60;
    
    // Mapa przechowująca klucze ostatnich eventów: "plate|parkingId|direction" -> timestamp
    private final ConcurrentHashMap<String, Instant> recentEvents = new ConcurrentHashMap<>();
    
    // Executor do czyszczenia starych wpisów
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "event-dedup-cleanup");
        t.setDaemon(true);
        return t;
    });
    
    public EventDeduplicationService() {
        // Co 2 minuty czyść stare wpisy (starsze niż okno deduplikacji)
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldEntries, 2, 2, TimeUnit.MINUTES);
        log.info("EventDeduplicationService initialized with window={} seconds", DEDUPLICATION_WINDOW_SECONDS);
    }
    
    /**
     * Sprawdza czy event jest duplikatem.
     * 
     * @param plate Tablica rejestracyjna
     * @param parkingId ID parkingu
     * @param direction Kierunek ("entry" lub "exit")
     * @param timestamp Czas eventu
     * @return true jeśli event jest duplikatem (był już w ostatnich X sekundach), false w przeciwnym razie
     */
    public boolean isDuplicate(String plate, Long parkingId, String direction, Instant timestamp) {
        if (plate == null || parkingId == null || direction == null) {
            return false; // Nieprawidłowe dane - przepuść event
        }
        
        String normalizedPlate = plate.toUpperCase().trim();
        String normalizedDirection = direction.toLowerCase().trim();
        
        // Klucz: plate|parkingId|direction
        String eventKey = String.format("%s|%d|%s", normalizedPlate, parkingId, normalizedDirection);
        
        Instant now = timestamp != null ? timestamp : Instant.now();
        Instant lastEventTime = recentEvents.get(eventKey);
        
        if (lastEventTime != null) {
            long secondsBetween = ChronoUnit.SECONDS.between(lastEventTime, now);
            
            if (secondsBetween < DEDUPLICATION_WINDOW_SECONDS) {
                log.warn("DUPLICATE EVENT DETECTED: plate={}, parkingId={}, direction={}, " +
                        "secondsSinceLastEvent={}, window={}s - EVENT REJECTED",
                    normalizedPlate, parkingId, normalizedDirection, secondsBetween, DEDUPLICATION_WINDOW_SECONDS);
                return true; // To jest duplikat
            } else {
                // Stary wpis, ale w oknie czasowym - aktualizuj czas
                recentEvents.put(eventKey, now);
                log.debug("Event re-registered: plate={}, parkingId={}, direction={}, secondsSinceLastEvent={}",
                    normalizedPlate, parkingId, normalizedDirection, secondsBetween);
                return false;
            }
        } else {
            // Pierwszy event z tym kluczem - zapisz i przepuść
            recentEvents.put(eventKey, now);
            log.debug("New event registered: plate={}, parkingId={}, direction={}",
                normalizedPlate, parkingId, normalizedDirection);
            return false; // To nie jest duplikat
        }
    }
    
    /**
     * Rejestruje event (używane gdy event został przetworzony).
     * To jest pomocnicza metoda - główna logika jest w isDuplicate().
     */
    public void registerEvent(String plate, Long parkingId, String direction, Instant timestamp) {
        if (plate == null || parkingId == null || direction == null) {
            return;
        }
        
        String normalizedPlate = plate.toUpperCase().trim();
        String normalizedDirection = direction.toLowerCase().trim();
        String eventKey = String.format("%s|%d|%s", normalizedPlate, parkingId, normalizedDirection);
        
        Instant now = timestamp != null ? timestamp : Instant.now();
        recentEvents.put(eventKey, now);
    }
    
    /**
     * Czyści stare wpisy z mapy (starsze niż okno deduplikacji + margines).
     */
    private void cleanupOldEntries() {
        Instant cutoffTime = Instant.now().minus(DEDUPLICATION_WINDOW_SECONDS + 60, ChronoUnit.SECONDS);
        int beforeSize = recentEvents.size();
        
        recentEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoffTime));
        
        int afterSize = recentEvents.size();
        if (beforeSize > afterSize) {
            log.debug("Cleaned up {} old event entries (before: {}, after: {})",
                beforeSize - afterSize, beforeSize, afterSize);
        }
    }
    
    /**
     * Zwraca statystyki dla monitoringu.
     */
    public int getRecentEventsCount() {
        return recentEvents.size();
    }
}

