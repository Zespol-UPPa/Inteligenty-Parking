package com.smartparking.customer_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class VehicleRepository {
    private final JdbcTemplate jdbc;

    public VehicleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByAccountId(Long accountId) {
        return jdbc.query("SELECT vehicle_id, ref_account_id, licence_plate FROM vehicle WHERE ref_account_id = ? ORDER BY vehicle_id DESC",
                (rs, rowNum) -> Map.of(
                        "vehicle_id", rs.getLong("vehicle_id"),
                        "ref_account_id", rs.getLong("ref_account_id"),
                        "licence_plate", rs.getString("licence_plate")
                ), accountId);
    }

    public long add(Long accountId, String licencePlate) {
        return jdbc.queryForObject("INSERT INTO vehicle(ref_account_id, licence_plate) VALUES (?, ?) RETURNING vehicle_id", Long.class, accountId, licencePlate);
    }
}
