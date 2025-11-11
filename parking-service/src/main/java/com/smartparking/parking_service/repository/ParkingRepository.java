package com.smartparking.parking_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ParkingRepository {
    private final JdbcTemplate jdbc;
    public ParkingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listLocations() {
        return jdbc.query("SELECT id_parking, name_parking, address_line FROM parking_location ORDER BY id_parking",
                (rs, i) -> Map.of(
                        "id_parking", rs.getLong("id_parking"),
                        "name_parking", rs.getString("name_parking"),
                        "address_line", rs.getString("address_line")
                ));
    }

    public List<Map<String, Object>> listSpots(Long locationId) {
        if (locationId == null) {
            return jdbc.query("SELECT id_spot, id_parking, code, floor_lvl, to_reserved, type FROM parking_spot ORDER BY id_spot",
                    (rs, i) -> Map.of(
                            "id_spot", rs.getLong("id_spot"),
                            "id_parking", rs.getLong("id_parking"),
                            "code", rs.getString("code"),
                            "floor_lvl", rs.getInt("floor_lvl"),
                            "to_reserved", rs.getBoolean("to_reserved"),
                            "type", rs.getString("type")
                    ));
        }
        return jdbc.query("SELECT id_spot, id_parking, code, floor_lvl, to_reserved, type FROM parking_spot WHERE id_parking = ? ORDER BY id_spot",
                (rs, i) -> Map.of(
                        "id_spot", rs.getLong("id_spot"),
                        "id_parking", rs.getLong("id_parking"),
                        "code", rs.getString("code"),
                        "floor_lvl", rs.getInt("floor_lvl"),
                        "to_reserved", rs.getBoolean("to_reserved"),
                        "type", rs.getString("type")
                ), locationId);
    }
}


