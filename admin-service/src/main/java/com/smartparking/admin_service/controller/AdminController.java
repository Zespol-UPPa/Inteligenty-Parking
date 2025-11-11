package com.smartparking.admin_service.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final JdbcTemplate jdbc;
    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/locations")
    public ResponseEntity<Map<String, Object>> addLocation(@RequestParam String name,
                                                           @RequestParam String address,
                                                           @RequestParam Long companyId) {
        Long id = jdbc.queryForObject(
                "INSERT INTO parking_location(name_parking, address_line, id_company) VALUES (?, ?, ?) RETURNING id_parking",
                Long.class, name, address, companyId);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PostMapping("/spots")
    public ResponseEntity<Map<String, Object>> addSpot(@RequestParam Long locationId,
                                                       @RequestParam String code,
                                                       @RequestParam Integer floorLvl,
                                                       @RequestParam(defaultValue = "false") boolean toReserved,
                                                       @RequestParam(defaultValue = "Available") String type) {
        Long id = jdbc.queryForObject(
                "INSERT INTO parking_spot(id_parking, code, floor_lvl, to_reserved, type) VALUES (?, ?, ?, ?, CAST(? AS status)) RETURNING id_spot",
                Long.class, locationId, code, floorLvl, toReserved, type);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping("/reports/usage")
    public ResponseEntity<List<Map<String, Object>>> usageReport() {
        List<Map<String, Object>> rows = jdbc.query("SELECT id_parking, type, COUNT(*) AS count FROM parking_spot GROUP BY id_parking, type",
                (rs, i) -> Map.of(
                        "id_parking", rs.getLong("id_parking"),
                        "type", rs.getString("type"),
                        "count", rs.getLong("count")
                ));
        return ResponseEntity.ok(rows);
    }
}


