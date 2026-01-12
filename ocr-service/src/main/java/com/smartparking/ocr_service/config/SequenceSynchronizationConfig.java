package com.smartparking.ocr_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

@Configuration
public class SequenceSynchronizationConfig {

    private static final Logger log = LoggerFactory.getLogger(SequenceSynchronizationConfig.class);
    private final JdbcTemplate jdbcTemplate;

    public SequenceSynchronizationConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void synchronizeSequences() {
        log.info("🔄 Attempting to synchronize database sequences in ocr-service...");
        synchronizeSequence("plate_read_id_read_seq", "plate_read", "read_id");
    }

    private void synchronizeSequence(String sequenceName, String tableName, String idColumnName) {
        try {
            Long maxId = jdbcTemplate.queryForObject(
                String.format("SELECT COALESCE(MAX(%s), 0) FROM %s", idColumnName, tableName), Long.class);
            // Użyj execute() zamiast update() dla SELECT setval()
            jdbcTemplate.execute(String.format("SELECT setval('%s', %d, true)", sequenceName, maxId));
            log.info("Synchronized sequence '{}' to MAX({})+1 = {}", sequenceName, idColumnName, maxId + 1);
        } catch (Exception e) {
            log.error("Failed to synchronize sequence '{}': {}", sequenceName, e.getMessage());
        }
    }
}

