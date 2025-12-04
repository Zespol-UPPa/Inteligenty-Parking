package com.smartparking.parking_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import com.smartparking.parking_service.dto.ParkingUsageDto;

@Repository
public class ParkingRepository {
    private final JdbcTemplate jdbc;
    public ParkingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listLocations() {
        // parking_db.parking_location: parking_id, name_parking, address_line, ref_company_id
        // Aliasujemy kolumny tak, aby API nadal zwracało id_parking
        return jdbc.query("SELECT parking_id AS id_parking, name_parking, address_line FROM parking_location ORDER BY parking_id",
                (rs, i) -> Map.of(
                        "id_parking", rs.getLong("id_parking"),
                        "name_parking", rs.getString("name_parking"),
                        "address_line", rs.getString("address_line")
                ));
    }

    public List<Map<String, Object>> listSpots(Long locationId) {
        if (locationId == null) {
            // parking_db.parking_spot: spot_id, code, floor_lvl, to_reserved, type, id_parking
            // Aliasujemy spot_id jako id_spot, żeby nie zmieniać kontraktu API
            return jdbc.query("SELECT spot_id AS id_spot, id_parking, code, floor_lvl, to_reserved, type FROM parking_spot ORDER BY spot_id",
                    (rs, i) -> Map.of(
                            "id_spot", rs.getLong("id_spot"),
                            "id_parking", rs.getLong("id_parking"),
                            "code", rs.getString("code"),
                            "floor_lvl", rs.getInt("floor_lvl"),
                            "to_reserved", rs.getBoolean("to_reserved"),
                            "type", rs.getString("type")
                    ));
        }
        return jdbc.query("SELECT spot_id AS id_spot, id_parking, code, floor_lvl, to_reserved, type FROM parking_spot WHERE id_parking = ? ORDER BY spot_id",
                (rs, i) -> Map.of(
                        "id_spot", rs.getLong("id_spot"),
                        "id_parking", rs.getLong("id_parking"),
                        "code", rs.getString("code"),
                        "floor_lvl", rs.getInt("floor_lvl"),
                        "to_reserved", rs.getBoolean("to_reserved"),
                        "type", rs.getString("type")
                ), locationId);
    }

    public long createLocation(String name, String address, Long companyId) {
        // parking_db.parking_location: parking_id, name_parking, address_line, ref_company_id
        String sql = "INSERT INTO parking_location(name_parking, address_line, ref_company_id) VALUES (?, ?, ?) RETURNING parking_id";
        return jdbc.queryForObject(sql, Long.class, name, address, companyId);
    }

    public long createSpot(Long locationId, String code, Integer floorLvl, boolean toReserved, String type) {
        // parking_db.parking_spot: spot_id, code, floor_lvl, to_reserved, type public.spot_status, id_parking
        String sql = "INSERT INTO parking_spot(id_parking, code, floor_lvl, to_reserved, type) VALUES (?, ?, ?, ?, CAST(? AS public.spot_status)) RETURNING spot_id";
        return jdbc.queryForObject(sql, Long.class, locationId, code, floorLvl, toReserved, type);
    }

    public List<ParkingUsageDto> usageReport() {
        String sql = "SELECT id_parking, type, COUNT(*) AS count FROM parking_spot GROUP BY id_parking, type";
        return jdbc.query(sql, (rs, i) ->
                new ParkingUsageDto(
                        rs.getLong("id_parking"),
                        rs.getString("type"),
                        rs.getLong("count")
                )
        );
    }

    // Reservation operations - reservation_spot is in parking_db
    public List<Map<String, Object>> findReservationsByAccountId(Long accountId) {
        // parking_db.reservation_spot: reservation_id, valid_until, status_reservation, spot_id, parking_id, ref_account_id
        // Note: reservation_spot only has valid_until (end time), not start_time. We use valid_until for both.
        String sql = "SELECT reservation_id AS id_reservation, ref_account_id AS id_account, parking_id AS id_parking, " +
                     "spot_id AS id_spot, valid_until, status_reservation AS status " +
                     "FROM reservation_spot WHERE ref_account_id = ? ORDER BY reservation_id DESC";
        return jdbc.query(sql, (rs, rowNum) -> {
            java.time.Instant validUntil = rs.getTimestamp("valid_until").toInstant();
            // Use valid_until for both start_time and end_time (since we don't have start_time in the schema)
            return Map.of(
                    "id_reservation", rs.getLong("id_reservation"),
                    "id_account", rs.getLong("id_account"),
                    "id_parking", rs.getLong("id_parking"),
                    "id_spot", rs.getLong("id_spot"),
                    "start_time", validUntil.minusSeconds(3600), // Assume 1 hour duration
                    "end_time", validUntil,
                    "status", rs.getString("status")
            );
        }, accountId);
    }

    public long createReservation(Long accountId, Long parkingId, Long spotId, java.time.Instant validUntil, String status) {
        // parking_db.reservation_spot: reservation_id, valid_until, status_reservation, spot_id, parking_id, ref_account_id
        String sql = "INSERT INTO reservation_spot(ref_account_id, parking_id, spot_id, valid_until, status_reservation) " +
                     "VALUES (?, ?, ?, ?, CAST(? AS public.reservation_status)) RETURNING reservation_id";
        return jdbc.queryForObject(sql, Long.class, accountId, parkingId, spotId, 
                java.sql.Timestamp.from(validUntil), status);
    }

    // Get all active reservations (for workers) - status = 'Paid' and valid_until > now
    public List<Map<String, Object>> findActiveReservations() {
        // parking_db.reservation_spot: reservation_id, valid_until, status_reservation, spot_id, parking_id, ref_account_id
        String sql = "SELECT reservation_id AS id_reservation, ref_account_id AS id_account, parking_id AS id_parking, " +
                     "spot_id AS id_spot, valid_until, status_reservation AS status " +
                     "FROM reservation_spot WHERE status_reservation = 'Paid' AND valid_until > now() " +
                     "ORDER BY valid_until ASC";
        return jdbc.query(sql, (rs, rowNum) -> {
            java.time.Instant validUntil = rs.getTimestamp("valid_until").toInstant();
            return Map.of(
                    "id_reservation", rs.getLong("id_reservation"),
                    "id_account", rs.getLong("id_account"),
                    "id_parking", rs.getLong("id_parking"),
                    "id_spot", rs.getLong("id_spot"),
                    "start_time", validUntil.minusSeconds(3600), // Assume 1 hour duration
                    "end_time", validUntil,
                    "type", "Reserved" // Map status to type for backward compatibility
            );
        });
    }
}
