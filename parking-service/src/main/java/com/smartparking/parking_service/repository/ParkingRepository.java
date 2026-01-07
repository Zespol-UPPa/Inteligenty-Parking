package com.smartparking.parking_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.smartparking.parking_service.dto.ParkingUsageDto;

@Repository
public class ParkingRepository {
    private final JdbcTemplate jdbc;
    public ParkingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listLocations() {
        // parking_db.parking_location: parking_id, name_parking, address_line, ref_company_id
        // Rozszerzamy zapytanie o agregowane dane: total_spots, available_spots, occupied_spots, price_per_hour
        String sql = "SELECT " +
                "pl.parking_id AS id_parking, " +
                "pl.name_parking, " +
                "pl.address_line, " +
                "COUNT(DISTINCT ps.spot_id) AS total_spots, " +
                "COUNT(DISTINCT CASE WHEN ps.type = 'Available' " +
                "    AND NOT EXISTS (" +
                "        SELECT 1 FROM reservation_spot rs " +
                "        WHERE rs.spot_id = ps.spot_id " +
                "        AND rs.parking_id = pl.parking_id " +
                "        AND rs.status_reservation = 'Paid' " +
                "        AND rs.valid_until > NOW()" +
                "    ) " +
                "    AND NOT EXISTS (" +
                "        SELECT 1 FROM parking_session psess " +
                "        WHERE psess.spot_id = ps.spot_id " +
                "        AND psess.parking_id = pl.parking_id " +
                "        AND psess.exit_time IS NULL" +
                "    ) " +
                "THEN ps.spot_id END) AS available_spots, " +
                "COUNT(DISTINCT CASE WHEN psess_active.session_id IS NOT NULL " +
                "    THEN psess_active.spot_id END) AS occupied_spots, " +
                "COALESCE(MAX(pp.rate_per_min) * 60, 0) AS price_per_hour_minor, " +
                "MAX(pp.curency_code) AS currency_code, " +
                "MAX(pp.reservation_fee_minor) AS reservation_fee_minor, " +
                "MAX(pp.rate_per_min) AS rate_per_min, " +
                "MAX(pp.free_minutes) AS free_minutes, " +
                "MAX(pp.rounding_step_min) AS rounding_step_min " +
                "FROM parking_location pl " +
                "LEFT JOIN parking_spot ps ON pl.parking_id = ps.id_parking " +
                "LEFT JOIN parking_pricing pp ON pp.parking_id = pl.parking_id " +
                "LEFT JOIN parking_session psess_active ON " +
                "    psess_active.spot_id = ps.spot_id " +
                "    AND psess_active.parking_id = pl.parking_id " +
                "    AND psess_active.exit_time IS NULL " +
                "GROUP BY pl.parking_id, pl.name_parking, pl.address_line " +
                "ORDER BY pl.parking_id";
        
        return jdbc.query(sql, (rs, i) -> {
            long totalSpots = rs.getLong("total_spots");
            long availableSpots = rs.getLong("available_spots");
            long occupiedSpots = rs.getLong("occupied_spots");
            long pricePerHourMinor = rs.getLong("price_per_hour_minor");
            String currencyCode = rs.getString("currency_code");
            Long reservationFeeMinor = rs.getLong("reservation_fee_minor");
            if (rs.wasNull()) {
                reservationFeeMinor = 0L;
            }
            int ratePerMin = rs.getInt("rate_per_min");
            if (rs.wasNull()) {
                ratePerMin = 0;
            }
            int freeMinutes = rs.getInt("free_minutes");
            if (rs.wasNull()) {
                freeMinutes = 0;
            }
            int roundingStepMin = rs.getInt("rounding_step_min");
            if (rs.wasNull()) {
                roundingStepMin = 0;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("name_parking", rs.getString("name_parking"));
            result.put("address_line", rs.getString("address_line"));
            result.put("total_spots", totalSpots);
            result.put("available_spots", availableSpots);
            result.put("occupied_spots", occupiedSpots);
            result.put("price_per_hour_minor", pricePerHourMinor);
            result.put("currency_code", currencyCode != null ? currencyCode : "PLN");
            result.put("reservation_fee_minor", reservationFeeMinor);
            result.put("rate_per_min", ratePerMin);
            result.put("free_minutes", freeMinutes);
            result.put("rounding_step_min", roundingStepMin);
            return result;
        });
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

    public List<Map<String, Object>> listSpotsForReservation(Long locationId) {
        if (locationId == null) {
            throw new IllegalArgumentException("locationId is required for reservation spots");
        }
        
        String sql = "SELECT " +
                     "ps.spot_id AS id_spot, " +
                     "ps.id_parking, " +
                     "ps.code, " +
                     "ps.floor_lvl, " +
                     "ps.to_reserved, " +
                     "ps.type " +
                     "FROM parking_spot ps " +
                     "WHERE ps.id_parking = ? " +
                     "  AND ps.to_reserved = true " +
                     "  AND ps.type = 'Available' " +
                     "  AND NOT EXISTS (" +
                     "      SELECT 1 FROM reservation_spot rs " +
                     "      WHERE rs.spot_id = ps.spot_id " +
                     "      AND rs.status_reservation = 'Paid' " +
                     "      AND rs.valid_until > NOW()" +
                     "  ) " +
                     "ORDER BY ps.spot_id";
        
        return jdbc.query(sql, (rs, i) -> Map.of(
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
        String sql = "SELECT " +
                     "rs.reservation_id AS id_reservation, " +
                     "rs.ref_account_id AS id_account, " +
                     "rs.parking_id AS id_parking, " +
                     "rs.spot_id AS id_spot, " +
                     "rs.valid_until, " +
                     "rs.status_reservation AS status, " +
                     "pl.name_parking AS parkingName, " +
                     "pl.address_line AS address, " +
                     "ps.code AS spot_code " +
                     "FROM reservation_spot rs " +
                     "LEFT JOIN parking_location pl ON rs.parking_id = pl.parking_id " +
                     "LEFT JOIN parking_spot ps ON rs.spot_id = ps.spot_id " +
                     "WHERE rs.ref_account_id = ? " +
                     "ORDER BY rs.reservation_id DESC";
        return jdbc.query(sql, (rs, rowNum) -> {
            java.time.Instant validUntil = rs.getTimestamp("valid_until").toInstant();
            // Use valid_until for both start_time and end_time (since we don't have start_time in the schema)
            Map<String, Object> result = new HashMap<>();
            result.put("id_reservation", rs.getLong("id_reservation"));
            result.put("id_account", rs.getLong("id_account"));
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("id_spot", rs.getLong("id_spot"));
            result.put("start_time", validUntil.minusSeconds(3600)); // Assume 1 hour duration
            result.put("end_time", validUntil);
            result.put("status", rs.getString("status"));
            result.put("parkingName", rs.getString("parkingName") != null ? rs.getString("parkingName") : "Unknown");
            result.put("address", rs.getString("address") != null ? rs.getString("address") : "");
            result.put("spot_code", rs.getString("spot_code") != null ? rs.getString("spot_code") : "Unknown");
            return result;
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
        String sql = "SELECT " +
                     "rs.reservation_id AS id_reservation, " +
                     "rs.ref_account_id AS id_account, " +
                     "rs.parking_id AS id_parking, " +
                     "rs.spot_id AS id_spot, " +
                     "rs.valid_until, " +
                     "rs.status_reservation AS status, " +
                     "pl.name_parking AS parkingName, " +
                     "pl.address_line AS address, " +
                     "ps.code AS spot_code " +
                     "FROM reservation_spot rs " +
                     "LEFT JOIN parking_location pl ON rs.parking_id = pl.parking_id " +
                     "LEFT JOIN parking_spot ps ON rs.spot_id = ps.spot_id " +
                     "WHERE rs.status_reservation = 'Paid' AND rs.valid_until > now() " +
                     "ORDER BY rs.valid_until ASC";
        return jdbc.query(sql, (rs, rowNum) -> {
            java.time.Instant validUntil = rs.getTimestamp("valid_until").toInstant();
            Map<String, Object> result = new HashMap<>();
            result.put("id_reservation", rs.getLong("id_reservation"));
            result.put("id_account", rs.getLong("id_account"));
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("id_spot", rs.getLong("id_spot"));
            result.put("start_time", validUntil.minusSeconds(3600)); // Assume 1 hour duration
            result.put("end_time", validUntil);
            result.put("type", "Reserved"); // Map status to type for backward compatibility
            result.put("parkingName", rs.getString("parkingName") != null ? rs.getString("parkingName") : "Unknown");
            result.put("address", rs.getString("address") != null ? rs.getString("address") : "");
            result.put("spot_code", rs.getString("spot_code") != null ? rs.getString("spot_code") : "Unknown");
            return result;
        });
    }

    public Optional<Map<String, Object>> getLocationDetails(Long locationId) {
        String sql = "SELECT " +
                "pl.parking_id AS id_parking, " +
                "pl.name_parking, " +
                "pl.address_line, " +
                "COUNT(DISTINCT ps.spot_id) AS total_spots, " +
                "COUNT(DISTINCT CASE WHEN ps.type = 'Available' " +
                "    AND NOT EXISTS (" +
                "        SELECT 1 FROM reservation_spot rs " +
                "        WHERE rs.spot_id = ps.spot_id " +
                "        AND rs.parking_id = pl.parking_id " +
                "        AND rs.status_reservation = 'Paid' " +
                "        AND rs.valid_until > NOW()" +
                "    ) " +
                "    AND NOT EXISTS (" +
                "        SELECT 1 FROM parking_session psess " +
                "        WHERE psess.spot_id = ps.spot_id " +
                "        AND psess.parking_id = pl.parking_id " +
                "        AND psess.exit_time IS NULL" +
                "    ) " +
                "THEN ps.spot_id END) AS available_spots, " +
                "COUNT(DISTINCT CASE WHEN psess_active.session_id IS NOT NULL " +
                "    THEN psess_active.spot_id END) AS occupied_spots, " +
                "COALESCE(MAX(pp.rate_per_min) * 60, 0) AS price_per_hour_minor, " +
                "MAX(pp.curency_code) AS currency_code, " +
                "MAX(pp.reservation_fee_minor) AS reservation_fee_minor, " +
                "MAX(pp.rate_per_min) AS rate_per_min, " +
                "MAX(pp.free_minutes) AS free_minutes, " +
                "MAX(pp.rounding_step_min) AS rounding_step_min " +
                "FROM parking_location pl " +
                "LEFT JOIN parking_spot ps ON pl.parking_id = ps.id_parking " +
                "LEFT JOIN parking_pricing pp ON pp.parking_id = pl.parking_id " +
                "LEFT JOIN parking_session psess_active ON " +
                "    psess_active.spot_id = ps.spot_id " +
                "    AND psess_active.parking_id = pl.parking_id " +
                "    AND psess_active.exit_time IS NULL " +
                "WHERE pl.parking_id = ? " +
                "GROUP BY pl.parking_id, pl.name_parking, pl.address_line";
        
        var list = jdbc.query(sql, (rs, i) -> {
            long totalSpots = rs.getLong("total_spots");
            long availableSpots = rs.getLong("available_spots");
            long occupiedSpots = rs.getLong("occupied_spots");
            long pricePerHourMinor = rs.getLong("price_per_hour_minor");
            String currencyCode = rs.getString("currency_code");
            Long reservationFeeMinor = rs.getLong("reservation_fee_minor");
            if (rs.wasNull()) {
                reservationFeeMinor = 0L;
            }
            int ratePerMin = rs.getInt("rate_per_min");
            if (rs.wasNull()) {
                ratePerMin = 0;
            }
            int freeMinutes = rs.getInt("free_minutes");
            if (rs.wasNull()) {
                freeMinutes = 0;
            }
            int roundingStepMin = rs.getInt("rounding_step_min");
            if (rs.wasNull()) {
                roundingStepMin = 0;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("name_parking", rs.getString("name_parking"));
            result.put("address_line", rs.getString("address_line"));
            result.put("total_spots", totalSpots);
            result.put("available_spots", availableSpots);
            result.put("occupied_spots", occupiedSpots);
            result.put("price_per_hour_minor", pricePerHourMinor);
            result.put("currency_code", currencyCode != null ? currencyCode : "PLN");
            result.put("reservation_fee_minor", reservationFeeMinor);
            result.put("rate_per_min", ratePerMin);
            result.put("free_minutes", freeMinutes);
            result.put("rounding_step_min", roundingStepMin);
            return result;
        }, locationId);
        
        return list.stream().findFirst();
    }

    public Map<String, Object> getOccupancyData(Long locationId, Integer dayOfWeek) {
        // Calculate occupancy statistics based on parking sessions
        // parking_session table: session_id, entry_time, exit_time, parking_id, spot_id
        
        // Get total spots for the parking
        Long totalSpots = jdbc.queryForObject(
                "SELECT COUNT(*) FROM parking_spot WHERE id_parking = ?",
                Long.class, locationId);
        
        // Get current available spots (not reserved and not in active session)
        Long availableSpots = jdbc.queryForObject(
                "SELECT COUNT(*) FROM parking_spot ps " +
                "WHERE ps.id_parking = ? " +
                "AND ps.type = 'Available' " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM reservation_spot rs " +
                "    WHERE rs.spot_id = ps.spot_id " +
                "    AND rs.status_reservation = 'Paid' " +
                "    AND rs.valid_until > NOW()" +
                ") " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM parking_session psess " +
                "    WHERE psess.spot_id = ps.spot_id " +
                "    AND psess.exit_time IS NULL" +
                ")",
                Long.class, locationId);
        
        // Jeśli dayOfWeek == null, użyj aktualnego dnia tygodnia
        int targetDayOfWeek = (dayOfWeek != null) ? dayOfWeek : getCurrentDayOfWeek();
        
        // Pobierz statystyki rezerwacji dla wybranego dnia tygodnia z ostatnich 8 tygodni
        // PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday
        String reservationStatsSql = 
            "SELECT " +
            "    EXTRACT(HOUR FROM valid_until)::INTEGER AS hour_of_day, " +
            "    COUNT(*)::INTEGER AS reservation_count " +
            "FROM reservation_spot " +
            "WHERE parking_id = ? " +
            "    AND status_reservation IN ('Paid', 'Used') " +
            "    AND EXTRACT(DOW FROM valid_until) = ? " +
            "    AND valid_until >= NOW() - INTERVAL '8 weeks' " +
            "GROUP BY EXTRACT(HOUR FROM valid_until) " +
            "ORDER BY hour_of_day";

        List<Map<String, Object>> reservationStats = jdbc.query(reservationStatsSql, (rs, i) -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("hour", rs.getInt("hour_of_day"));
            stat.put("count", rs.getInt("reservation_count"));
            return stat;
        }, locationId, targetDayOfWeek);

        // Utwórz tablice dla wykresów (godziny 6-22)
        List<Integer> normalHours = new ArrayList<>();
        List<Integer> peakHours = new ArrayList<>();
        List<String> hourLabels = new ArrayList<>();

        // Inicjalizuj wszystkie godziny na 0
        for (int hour = 6; hour <= 22; hour++) {
            normalHours.add(0);
            peakHours.add(0);
            hourLabels.add(formatHour(hour));
        }

        // Wypełnij rzeczywistymi danymi
        for (Map<String, Object> stat : reservationStats) {
            int hour = (Integer) stat.get("hour");
            int count = (Integer) stat.get("count");
            
            if (hour >= 6 && hour <= 22) {
                int index = hour - 6;
                normalHours.set(index, count);
                // Peak hours = normal * 1.3 (można dostosować)
                peakHours.set(index, (int) Math.round(count * 1.3));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("normal", normalHours);
        result.put("peak", peakHours);
        result.put("hours", hourLabels);
        result.put("total_spots", totalSpots != null ? totalSpots : 0L);
        result.put("available_spots", availableSpots != null ? availableSpots : 0L);
        result.put("day_of_week", getDayOfWeekName(targetDayOfWeek));
        return result;
    }

    private String formatHour(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }

    /**
     * Konwersja Java DayOfWeek do PostgreSQL DOW
     * Java: Monday=1, Tuesday=2, ..., Sunday=7
     * PostgreSQL DOW: Sunday=0, Monday=1, ..., Saturday=6
     */
    private int getCurrentDayOfWeek() {
        int javaDayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue();
        // Konwersja: Java Monday(1) -> PostgreSQL Monday(1), Java Sunday(7) -> PostgreSQL Sunday(0)
        return javaDayOfWeek == 7 ? 0 : javaDayOfWeek;
    }

    /**
     * Zwraca nazwę dnia tygodnia dla PostgreSQL DOW
     * PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday
     */
    private String getDayOfWeekName(int dayOfWeek) {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        if (dayOfWeek >= 0 && dayOfWeek < days.length) {
            return days[dayOfWeek];
        }
        return "Unknown";
    }

    /**
     * Wybiera losowe wolne miejsce parkingowe dla danego parkingu
     */
    public Optional<Long> pickRandomFreeSpot(Long parkingId) {
        String sql = "SELECT ps.spot_id FROM parking_spot ps " +
                "WHERE ps.id_parking = ? " +
                "AND ps.type = 'Available' " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM reservation_spot rs " +
                "    WHERE rs.spot_id = ps.spot_id " +
                "    AND rs.status_reservation = 'Paid' " +
                "    AND rs.valid_until > NOW()" +
                ") " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM parking_session psess " +
                "    WHERE psess.spot_id = ps.spot_id " +
                "    AND psess.exit_time IS NULL" +
                ") " +
                "ORDER BY RANDOM() LIMIT 1";
        
        var list = jdbc.query(sql, (rs, i) -> rs.getLong("spot_id"), parkingId);
        return list.stream().findFirst();
    }

    /**
     * Znajduje aktywną rezerwację dla danego konta i parkingu w danym czasie
     */
    public Optional<Map<String, Object>> findActiveReservation(Long accountId, Long parkingId, java.time.Instant entryTime) {
        String sql = "SELECT reservation_id, spot_id, valid_until " +
                "FROM reservation_spot " +
                "WHERE ref_account_id = ? " +
                "AND parking_id = ? " +
                "AND status_reservation = 'Paid' " +
                "AND valid_until > ? " +
                "ORDER BY valid_until ASC LIMIT 1";
        
        var list = jdbc.query(sql, (rs, i) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("reservation_id", rs.getLong("reservation_id"));
            result.put("spot_id", rs.getLong("spot_id"));
            result.put("valid_until", rs.getTimestamp("valid_until").toInstant());
            return result;
        }, accountId, parkingId, java.sql.Timestamp.from(entryTime));
        
        return list.stream().findFirst();
    }

    /**
     * Aktualizuje status rezerwacji
     */
    public void updateReservationStatus(Long reservationId, String status) {
        jdbc.update(
                "UPDATE reservation_spot SET status_reservation = CAST(? AS public.reservation_status) WHERE reservation_id = ?",
                status, reservationId
        );
    }

}
