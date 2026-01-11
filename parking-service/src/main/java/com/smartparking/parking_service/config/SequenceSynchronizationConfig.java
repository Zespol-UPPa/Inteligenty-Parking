package com.smartparking.parking_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Komponent synchronizujący sekwencje przy starcie aplikacji.
 * Zapewnia, że sekwencje są zsynchronizowane z maksymalnymi wartościami w tabelach.
 */
@Component
public class SequenceSynchronizationConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SequenceSynchronizationConfig.class);
    private final JdbcTemplate jdbc;

    public SequenceSynchronizationConfig(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        log.info("Starting sequence synchronization");
        synchronizeParkingSessionSequence();
        log.info("Sequence synchronization completed");
    }

    /**
     * Synchronizuje sekwencję parking_session_session_id_seq z maksymalnym session_id w tabeli.
     */
    private void synchronizeParkingSessionSequence() {
        try {
            Long maxId = jdbc.queryForObject(
                "SELECT COALESCE(MAX(session_id), 0) FROM parking_session", Long.class);
            if (maxId != null) {
                jdbc.update("SELECT setval('parking_session_session_id_seq', ?, true)", maxId);
                log.info("Synchronized parking_session_session_id_seq to {}", maxId);
            } else {
                log.warn("Could not determine max session_id, sequence may not be synchronized");
            }
        } catch (Exception e) {
            log.error("Failed to synchronize parking_session_session_id_seq: {}", e.getMessage(), e);
        }
    }
}

