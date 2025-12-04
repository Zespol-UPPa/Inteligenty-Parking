package com.smartparking.ocr_service.repo;

import com.smartparking.ocr_service.model.PlateRead;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class PlateReadRepository {
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
        jdbc.update("INSERT INTO plate_read(camera_id, raw_plate, event_time) VALUES (?, ?, ?)",
                p.getCameraId(), p.getRawPlate(), java.sql.Timestamp.valueOf(p.getEventTime()));
        // Not fetching generated id for brevity
        return p;
    }

    public List<PlateRead> findAll() {
        return jdbc.query("SELECT read_id, camera_id, raw_plate, event_time FROM plate_read", MAPPER);
    }
}

