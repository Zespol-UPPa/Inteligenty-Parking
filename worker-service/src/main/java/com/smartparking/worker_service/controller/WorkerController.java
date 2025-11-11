package com.smartparking.worker_service.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/worker")
public class WorkerController {

    private final JdbcTemplate jdbc;
    public WorkerController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/reservations/active")
    public ResponseEntity<List<Map<String, Object>>> activeReservations() {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT id_reservation, id_account, id_parking, id_spot, start_time, end_time, type " +
                        "FROM reservation WHERE type IN ('Reserved') ORDER BY start_time DESC",
                (rs, i) -> Map.of(
                        "id_reservation", rs.getLong("id_reservation"),
                        "id_account", rs.getLong("id_account"),
                        "id_parking", rs.getLong("id_parking"),
                        "id_spot", rs.getLong("id_spot"),
                        "start_time", rs.getTimestamp("start_time").toInstant(),
                        "end_time", rs.getTimestamp("end_time").toInstant(),
                        "type", rs.getString("type")
                )
        );
        return ResponseEntity.ok(rows);
    }
}


