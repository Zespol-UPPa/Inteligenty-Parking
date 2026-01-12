package com.smartparking.ocr_service.repo;

import com.smartparking.ocr_service.model.PlateRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PlateReadRepository {
    private static final Logger log = LoggerFactory.getLogger(PlateReadRepository.class);
    private final JdbcTemplate jdbc;

    public PlateReadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PlateRead> MAPPER = new RowMapper<PlateRead>() {
        @Override
        public PlateRead mapRow(ResultSet rs, int rowNum) throws SQLException {
            PlateRead p = new PlateRead();
            p.setReadId(rs.getInt("read_id"));
            p.setCameraId(rs.getInt("camera_id"));
            p.setRawPlate(rs.getString("raw_plate"));
            p.setEventTime(rs.getTimestamp("event_time").toLocalDateTime());
            return p;
        }
    };

    public PlateRead save(PlateRead p) {
        return save(p, 0);
    }

    private PlateRead save(PlateRead p, int retryCount) {
        if (retryCount > 2) {
            log.error("Maximum retry count exceeded for plate_read save, throwing exception");
            throw new IllegalStateException("Failed to save plate_read after multiple retry attempts - sequence synchronization may have failed");
        }

        try {
            jdbc.update("INSERT INTO plate_read(camera_id, raw_plate, event_time) VALUES (?, ?, ?)",
                    p.getCameraId(), p.getRawPlate(), java.sql.Timestamp.valueOf(p.getEventTime()));
            log.debug("Saved plate_read: plate={}, cameraId={}, eventTime={}", 
                    p.getRawPlate(), p.getCameraId(), p.getEventTime());
            return p;
        } catch (DuplicateKeyException e) {
            log.error("Duplicate key error when creating plate_read, sequence may be out of sync. " +
                    "Attempting to synchronize sequence. plate={}, cameraId={}, eventTime={}", 
                    p.getRawPlate(), p.getCameraId(), p.getEventTime());
            
            String errorMessage = e.getMessage();
            Long existingReadId = null;
            if (errorMessage != null && errorMessage.contains("Key (read_id)=(")) {
                try {
                    int startIdx = errorMessage.indexOf("Key (read_id)=(") + "Key (read_id)=(".length();
                    int endIdx = errorMessage.indexOf(")", startIdx);
                    if (endIdx > startIdx) {
                        existingReadId = Long.parseLong(errorMessage.substring(startIdx, endIdx));
                        log.info("Extracted existing read_id={} from error message", existingReadId);
                    }
                } catch (Exception ex) {
                    log.warn("Could not extract read_id from error message: {}", ex.getMessage());
                }
            }
            
            // Synchronizuj sekwencję i spróbuj ponownie
            log.warn("Duplicate key error, synchronizing sequence and retrying (attempt {})", retryCount + 1);
            synchronizeSequence();
            return save(p, retryCount + 1);
        }
    }

    private void synchronizeSequence() {
        try {
            Long maxId = jdbc.queryForObject(
                "SELECT COALESCE(MAX(read_id), 0) FROM plate_read", Long.class);
            jdbc.update("SELECT setval('plate_read_id_read_seq', ?, true)", maxId);
            log.info("Synchronized plate_read_id_read_seq to {}", maxId);
        } catch (Exception e) {
            log.error("Failed to synchronize plate_read_id_read_seq sequence", e);
        }
    }

    public List<PlateRead> findAll() {
        return jdbc.query("SELECT read_id, camera_id, raw_plate, event_time FROM plate_read", MAPPER);
    }
}

