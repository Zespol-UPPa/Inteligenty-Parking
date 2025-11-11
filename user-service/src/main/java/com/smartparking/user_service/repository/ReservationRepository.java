package com.smartparking.user_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public class ReservationRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<Map<String, Object>> mapper = (rs, rowNum) -> Map.of(
            "id_reservation", rs.getLong("id_reservation"),
            "id_account", rs.getLong("id_account"),
            "id_parking", rs.getLong("id_parking"),
            "id_spot", rs.getLong("id_spot"),
            "start_time", rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toInstant() : null,
            "end_time", rs.getTimestamp("end_time") != null ? rs.getTimestamp("end_time").toInstant() : null,
            "type", rs.getString("type")
    );

    public ReservationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByAccountId(Long accountId) {
        return jdbc.query("SELECT id_reservation, id_account, id_parking, id_spot, start_time, end_time, type FROM reservation WHERE id_account = ? ORDER BY start_time DESC",
                mapper, accountId);
    }

    public long create(Long accountId, Long parkingId, Long spotId, Instant start, Instant end, String type) {
        return jdbc.queryForObject(
                "INSERT INTO reservation(id_account, id_parking, id_spot, start_time, end_time, type) VALUES (?, ?, ?, ?, ?, ?) RETURNING id_reservation",
                Long.class, accountId, parkingId, spotId, start, end, type);
    }
}


