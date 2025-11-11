package com.smartparking.user_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class VehicleRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<Map<String, Object>> mapper = (rs, rowNum) -> Map.of(
            "id_vehicle", rs.getLong("id_vehicle"),
            "id_account", rs.getLong("id_account"),
            "licence_plate", rs.getString("licence_plate")
    );

    public VehicleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findByAccountId(Long accountId) {
        return jdbc.query("SELECT id_vehicle, id_account, licence_plate FROM vehicle WHERE id_account = ?",
                mapper, accountId);
    }

    public long addVehicle(Long accountId, String licencePlate) {
        return jdbc.queryForObject(
                "INSERT INTO vehicle(id_account, licence_plate) VALUES (?, ?) RETURNING id_vehicle",
                Long.class, accountId, licencePlate);
    }
}


