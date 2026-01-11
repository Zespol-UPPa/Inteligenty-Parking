package com.smartparking.parking_service.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ParkingRepository.class);
    private final JdbcTemplate jdbc;
    public ParkingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listLocations() {
        // parking_db.parking_location: parking_id, name_parking, address_line, ref_company_id
        // Rozszerzamy zapytanie o agregowane dane: total_spots, available_spots, price_per_hour
        // available_spots = miejsca nierezerwowalne (to_reserved = false) bez aktywnych sesji
        String sql = "SELECT " +
                "pl.parking_id AS id_parking, " +
                "pl.name_parking, " +
                "pl.address_line, " +
                "COUNT(DISTINCT ps.spot_id) AS total_spots, " +
                "COUNT(DISTINCT CASE WHEN ps.type = 'Available' " +
                "    AND ps.to_reserved = false " +  // TYLKO miejsca nierezerwowalne
                "    AND NOT EXISTS (" +
                "        SELECT 1 FROM parking_session psess " +
                "        WHERE psess.spot_id = ps.spot_id " +
                "        AND psess.parking_id = ps.id_parking " +  // Dodatkowa walidacja - parking_id
                "        AND psess.exit_time IS NULL" +  // Sprawdza aktywne sesje
                "    ) " +
                "THEN ps.spot_id END) AS available_spots, " +
                "COALESCE(MAX(pp.rate_per_min) * 60, 0) AS price_per_hour_minor " +
                "FROM parking_location pl " +
                "LEFT JOIN parking_spot ps ON pl.parking_id = ps.id_parking " +
                "LEFT JOIN parking_pricing pp ON pp.parking_id = pl.parking_id " +
                "GROUP BY pl.parking_id, pl.name_parking, pl.address_line " +
                "ORDER BY pl.parking_id";
        
        return jdbc.query(sql, (rs, i) -> {
            long totalSpots = rs.getLong("total_spots");
            long availableSpots = rs.getLong("available_spots");
            // price_per_hour_minor is in cents (rate_per_min * 60), convert to dollars
            long pricePerHourMinor = rs.getLong("price_per_hour_minor");
            
            return Map.of(
                    "id_parking", rs.getLong("id_parking"),
                    "name_parking", rs.getString("name_parking"),
                    "address_line", rs.getString("address_line"),
                    "total_spots", totalSpots,
                    "available_spots", availableSpots,
                    "price_per_hour_minor", pricePerHourMinor
            );
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

    public List<Map<String, Object>> listSpotsForReservation(Long locationId, java.time.Instant start, java.time.Instant end) {
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
                     "      AND rs.valid_from < ? " +
                     "      AND rs.valid_until > ?" +
                     "  ) " +
                     "ORDER BY ps.spot_id";
        
        return jdbc.query(sql, (rs, i) -> Map.of(
                "id_spot", rs.getLong("id_spot"),
                "id_parking", rs.getLong("id_parking"),
                "code", rs.getString("code"),
                "floor_lvl", rs.getInt("floor_lvl"),
                "to_reserved", rs.getBoolean("to_reserved"),
                "type", rs.getString("type")
        ), locationId, java.sql.Timestamp.from(end), java.sql.Timestamp.from(start));
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
        // parking_db.reservation_spot: reservation_id, valid_from (może nie istnieć), valid_until, status_reservation, spot_id, parking_id, ref_account_id, vehicle_id (może nie istnieć)
        // Używamy COALESCE dla valid_from - jeśli kolumna nie istnieje, użyj valid_until - 2h jako fallback
        String sql = "SELECT " +
                     "rs.reservation_id AS id_reservation, " +
                     "rs.ref_account_id AS id_account, " +
                     "rs.parking_id AS id_parking, " +
                     "rs.spot_id AS id_spot, " +
                     "rs.vehicle_id AS vehicle_id, " +
                     "COALESCE(rs.valid_from, rs.valid_until - INTERVAL '2 hours') AS valid_from, " +
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
            // SQL używa COALESCE, więc valid_from zawsze będzie miało wartość
            java.time.Instant validFrom = rs.getTimestamp("valid_from").toInstant();
            java.time.Instant validUntil = rs.getTimestamp("valid_until").toInstant();
            Map<String, Object> result = new HashMap<>();
            result.put("id_reservation", rs.getLong("id_reservation"));
            result.put("id_account", rs.getLong("id_account"));
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("id_spot", rs.getLong("id_spot"));
            
            // Bezpieczne pobieranie vehicle_id - może nie istnieć w starszych wersjach
            Object vehicleIdObj = null;
            try {
                vehicleIdObj = rs.getObject("vehicle_id");
            } catch (java.sql.SQLException e) {
                // Kolumna vehicle_id może nie istnieć - zignoruj
            }
            if (vehicleIdObj != null) {
                try {
                    // Konwertuj na Long niezależnie od typu (Integer, Long, BigInteger, itp.)
                    Long vehicleId;
                    if (vehicleIdObj instanceof Number) {
                        vehicleId = ((Number) vehicleIdObj).longValue();
                    } else {
                        vehicleId = Long.parseLong(vehicleIdObj.toString());
                    }
                    // Jeśli vehicleIdObj != null, to wartość istnieje - zapisz do result
                    result.put("vehicle_id", vehicleId);
                } catch (Exception e) {
                    // Loguj błąd, ale kontynuuj
                    System.err.println("Failed to convert vehicle_id for reservation " + 
                                     rs.getLong("id_reservation") + ": " + e.getMessage());
                }
            }
            // Jeśli vehicleIdObj == null, vehicle_id nie zostanie dodany do result (to jest OK)
            
            result.put("start_time", validFrom);
            result.put("end_time", validUntil);
            result.put("status", rs.getString("status"));
            result.put("parkingName", rs.getString("parkingName") != null ? rs.getString("parkingName") : "Unknown");
            result.put("address", rs.getString("address") != null ? rs.getString("address") : "");
            result.put("spot_code", rs.getString("spot_code") != null ? rs.getString("spot_code") : "Unknown");
            return result;
        }, accountId);
    }

    public long createReservation(Long accountId, Long parkingId, Long spotId, Long vehicleId, java.time.Instant validFrom, java.time.Instant validUntil, String status) {
        // parking_db.reservation_spot: reservation_id, valid_from, valid_until, status_reservation, spot_id, parking_id, ref_account_id
        String sql = "INSERT INTO reservation_spot(ref_account_id, parking_id, spot_id, valid_from, valid_until, status_reservation, vehicle_id) " +
                     "VALUES (?, ?, ?, ?, ?, CAST(? AS public.reservation_status), ?) RETURNING reservation_id";
        return jdbc.queryForObject(sql, Long.class, accountId, parkingId, spotId, 
                java.sql.Timestamp.from(validFrom), java.sql.Timestamp.from(validUntil), status, vehicleId);
    }

    /**
     * Sprawdza czy miejsce jest dostępne w danym przedziale czasowym
     * Weryfikuje wszystkie warunki:
     * - Czy miejsce jest rezerwowalne (to_reserved = true)
     * - Czy miejsce jest dostępne (type = 'Available')
     * - Czy nie ma konfliktów czasowych z innymi rezerwacjami
     * @param spotId ID miejsca
     * @param start Czas rozpoczęcia rezerwacji
     * @param end Czas zakończenia rezerwacji
     * @return true jeśli miejsce jest dostępne, false jeśli nie spełnia warunków
     */
    public boolean isSpotAvailableForTimeRange(Long spotId, java.time.Instant start, java.time.Instant end) {
        String sql = "SELECT COUNT(*) = 1 " +
                     "FROM parking_spot ps " +
                     "WHERE ps.spot_id = ? " +
                     "  AND ps.to_reserved = true " +           // Sprawdza czy miejsce jest rezerwowalne
                     "  AND ps.type = 'Available' " +          // Sprawdza czy miejsce jest dostępne
                     "  AND NOT EXISTS (" +
                     "      SELECT 1 FROM reservation_spot rs " +
                     "      WHERE rs.spot_id = ps.spot_id " +
                     "      AND rs.status_reservation = 'Paid' " +
                     "      AND rs.valid_from < ? " +          // Sprawdza konflikty czasowe (koniec nowej rezerwacji)
                     "      AND rs.valid_until > ?" +          // Sprawdza konflikty czasowe (start nowej rezerwacji)
                     "  )";
        Boolean result = jdbc.queryForObject(sql, Boolean.class, 
            spotId, 
            java.sql.Timestamp.from(end),    // valid_from < end (koniec nowej rezerwacji)
            java.sql.Timestamp.from(start)); // valid_until > start (start nowej rezerwacji)
        return result != null && result;
    }

    // Get all active reservations (for workers) - status = 'Paid' and valid_until > now
    public List<Map<String, Object>> findActiveReservations() {
        // parking_db.reservation_spot: reservation_id, valid_from, valid_until, status_reservation, spot_id, parking_id, ref_account_id
        String sql = "SELECT " +
                     "rs.reservation_id AS id_reservation, " +
                     "rs.ref_account_id AS id_account, " +
                     "rs.parking_id AS id_parking, " +
                     "rs.spot_id AS id_spot, " +
                     "rs.valid_from, " +
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
            java.time.Instant validFrom = rs.getTimestamp("valid_from").toInstant();
            java.time.Instant validUntil = rs.getTimestamp("valid_until").toInstant();
            Map<String, Object> result = new HashMap<>();
            result.put("id_reservation", rs.getLong("id_reservation"));
            result.put("id_account", rs.getLong("id_account"));
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("id_spot", rs.getLong("id_spot"));
            result.put("start_time", validFrom);
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
                "    AND ps.to_reserved = false " +  // TYLKO miejsca nierezerwowalne
                "    AND NOT EXISTS (" +
                "        SELECT 1 FROM parking_session psess " +
                "        WHERE psess.spot_id = ps.spot_id " +
                "        AND psess.parking_id = ps.id_parking " +  // Dodatkowa walidacja - parking_id
                "        AND psess.exit_time IS NULL" +  // Sprawdza aktywne sesje
                "    ) " +
                "THEN ps.spot_id END) AS available_spots, " +
                "COALESCE(MAX(pp.rate_per_min) * 60, 0) AS price_per_hour_minor, " +
                "COALESCE(MAX(pp.reservation_fee_minor), 0) AS reservation_fee_minor " +
                "FROM parking_location pl " +
                "LEFT JOIN parking_spot ps ON pl.parking_id = ps.id_parking " +
                "LEFT JOIN parking_pricing pp ON pp.parking_id = pl.parking_id " +
                "WHERE pl.parking_id = ? " +
                "GROUP BY pl.parking_id, pl.name_parking, pl.address_line";
        
        var list = jdbc.query(sql, (rs, i) -> {
            long totalSpots = rs.getLong("total_spots");
            long availableSpots = rs.getLong("available_spots");
            long pricePerHourMinor = rs.getLong("price_per_hour_minor");
            long reservationFeeMinor = rs.getLong("reservation_fee_minor");
            
            Map<String, Object> result = new HashMap<>();
            result.put("id_parking", rs.getLong("id_parking"));
            result.put("name_parking", rs.getString("name_parking"));
            result.put("address_line", rs.getString("address_line"));
            result.put("total_spots", totalSpots);
            result.put("available_spots", availableSpots);
            result.put("price_per_hour_minor", pricePerHourMinor);
            result.put("reservation_fee_minor", reservationFeeMinor);
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
        
        // Get current available spots (nierezerwowalne miejsca bez aktywnych sesji)
        Long availableSpots = jdbc.queryForObject(
                "SELECT COUNT(*) FROM parking_spot ps " +
                "WHERE ps.id_parking = ? " +
                "AND ps.type = 'Available' " +
                "AND ps.to_reserved = false " +  // TYLKO miejsca nierezerwowalne
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM parking_session psess " +
                "    WHERE psess.spot_id = ps.spot_id " +
                "    AND psess.parking_id = ps.id_parking " +  // Dodatkowa walidacja - parking_id
                "    AND psess.exit_time IS NULL" +  // Sprawdza aktywne sesje
                ")",
                Long.class, locationId);
        
        // Jeśli dayOfWeek == null, użyj aktualnego dnia tygodnia
        int targetDayOfWeek = (dayOfWeek != null) ? dayOfWeek : getCurrentDayOfWeek();
        
        // Pobierz statystyki obłożenia na podstawie parking_session dla wybranego dnia tygodnia z ostatnich 8 tygodni
        // PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday
        // Używamy entry_time do określenia godziny wejścia
        // Zakładamy że dane są zapisane jako lokalny czas (Europe/Warsaw)
        // Aby mieć spójność, używamy SET timezone='Europe/Warsaw' w sesji, lub konwertujemy w zapytaniu
        // Najprostsze: zakładamy że entry_time jest już w lokalnym czasie i używamy AT TIME ZONE
        // aby PostgreSQL interpretował je jako lokalny czas w Europe/Warsaw
        String occupancyStatsSql = 
            "SELECT " +
            "    EXTRACT(HOUR FROM (entry_time AT TIME ZONE 'Europe/Warsaw'))::INTEGER AS hour_of_day, " +
            "    COUNT(*)::INTEGER AS session_count " +
            "FROM parking_session " +
            "WHERE parking_id = ? " +
            "    AND EXTRACT(DOW FROM (entry_time AT TIME ZONE 'Europe/Warsaw')) = ? " +
            "    AND entry_time >= (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Warsaw')::timestamp - INTERVAL '8 weeks' " +
            "    AND EXTRACT(HOUR FROM (entry_time AT TIME ZONE 'Europe/Warsaw')) >= 6 " +
            "    AND EXTRACT(HOUR FROM (entry_time AT TIME ZONE 'Europe/Warsaw')) <= 22 " +
            "GROUP BY EXTRACT(HOUR FROM (entry_time AT TIME ZONE 'Europe/Warsaw')) " +
            "ORDER BY hour_of_day";

        List<Map<String, Object>> occupancyStats = jdbc.query(occupancyStatsSql, (rs, i) -> {
            Map<String, Object> stat = new HashMap<>();
            stat.put("hour", rs.getInt("hour_of_day"));
            stat.put("count", rs.getInt("session_count"));
            return stat;
        }, locationId, targetDayOfWeek);

        // Sprawdź czy są jakiekolwiek dane
        boolean hasData = !occupancyStats.isEmpty();

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

        // Jeśli brak danych, zwróć pusty wykres z flagą has_data = false
        if (!hasData) {
            Map<String, Object> result = new HashMap<>();
            result.put("has_data", false);
            result.put("normal", normalHours);
            result.put("peak", peakHours);
            result.put("hours", hourLabels);
            result.put("total_spots", totalSpots != null ? totalSpots : 0L);
            result.put("available_spots", availableSpots != null ? availableSpots : 0L);
            result.put("day_of_week", getDayOfWeekName(targetDayOfWeek));
            result.put("message", "No historical data available for " + getDayOfWeekName(targetDayOfWeek));
            return result;
        }

        // Oblicz percentyl 75 dla Peak Hours
        List<Integer> allCounts = new ArrayList<>();
        for (Map<String, Object> stat : occupancyStats) {
            allCounts.add((Integer) stat.get("count"));
        }
        allCounts.sort(Integer::compareTo);
        int peakThreshold = allCounts.isEmpty() ? 0 : 
            allCounts.get((int) Math.ceil(allCounts.size() * 0.75) - 1);

        // Wypełnij rzeczywistymi danymi
        for (Map<String, Object> stat : occupancyStats) {
            int hour = (Integer) stat.get("hour");
            int count = (Integer) stat.get("count");
            
            if (hour >= 6 && hour <= 22) {
                int index = hour - 6;
                normalHours.set(index, count);
                // Peak Hours = wartości powyżej percentyla 75, lub 1.2x normal jeśli poniżej
                if (count > peakThreshold) {
                    peakHours.set(index, count);
                } else {
                    peakHours.set(index, (int) Math.round(count * 1.2));
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("has_data", true);
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
     * Pobiera aktualny dzień tygodnia używając strefy czasowej użytkownika (Europe/Warsaw)
     * Konwersja: Java Monday(1) -> PostgreSQL Monday(1), Java Sunday(7) -> PostgreSQL Sunday(0)
     * PostgreSQL DOW: Sunday=0, Monday=1, ..., Saturday=6
     */
    private int getCurrentDayOfWeek() {
        // Użyj strefy czasowej użytkownika (Europe/Warsaw dla Polski)
        // To zapewnia poprawny dzień tygodnia niezależnie od strefy czasowej bazy danych/kontera
        java.time.ZoneId userZone = java.time.ZoneId.of("Europe/Warsaw");
        java.time.LocalDate localDate = java.time.LocalDate.now(userZone);
        int javaDayOfWeek = localDate.getDayOfWeek().getValue();
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
     * Znajduje aktywną, nieużytą rezerwację dla danego konta, parkingu i pojazdu w danym czasie.
     * Sprawdza status 'Paid' lub 'Active' (rezerwacja może być już rozpoczęta, ale sesja może nie istnieć).
     * Sprawdza czy pojazd pasuje (vehicle_id może być NULL - wtedy każdy pojazd z konta może użyć).
     * Pozwala na wjazd od 5 minut przed valid_from (grace period) przez cały okres rezerwacji (do valid_until włącznie).
     * 
     * @param accountId ID konta użytkownika
     * @param parkingId ID parkingu
     * @param vehicleId ID pojazdu (do weryfikacji czy pasuje do rezerwacji)
     * @param entryTime Czas wjazdu (sprawdza czy entryTime jest w przedziale valid_from - 5min do valid_until włącznie)
     * @return Rezerwacja jeśli znaleziona, Optional.empty() jeśli brak
     */
    public Optional<Map<String, Object>> findActiveReservation(Long accountId, Long parkingId, Long vehicleId, java.time.Instant entryTime) {
        log.info("Searching for active reservation: accountId={}, parkingId={}, vehicleId={}, entryTime={}", 
            accountId, parkingId, vehicleId, entryTime);
        
        // Debug query - sprawdź wszystkie rezerwacje dla tego konta i parkingu
        String debugSql = "SELECT reservation_id, spot_id, valid_from, valid_until, vehicle_id, status_reservation " +
                "FROM reservation_spot " +
                "WHERE ref_account_id = ? AND parking_id = ? " +
                "ORDER BY valid_until DESC";
        
        var debugList = jdbc.query(debugSql, (rs, i) -> {
            Map<String, Object> debug = new HashMap<>();
            debug.put("reservation_id", rs.getLong("reservation_id"));
            debug.put("spot_id", rs.getLong("spot_id"));
            debug.put("valid_from", rs.getTimestamp("valid_from").toInstant());
            debug.put("valid_until", rs.getTimestamp("valid_until").toInstant());
            Object vehicleIdObj = rs.getObject("vehicle_id");
            if (vehicleIdObj != null) {
                debug.put("vehicle_id", rs.getLong("vehicle_id"));
            } else {
                debug.put("vehicle_id", null);
            }
            debug.put("status", rs.getString("status_reservation"));
            return debug;
        }, accountId, parkingId);
        
        log.info("Found {} reservations for accountId={}, parkingId={}: {}", 
            debugList.size(), accountId, parkingId, debugList);
        
        java.time.Instant entryTimePlus5Min = entryTime.plus(5, java.time.temporal.ChronoUnit.MINUTES);
        
        String sql = "SELECT reservation_id, spot_id, valid_from, valid_until, vehicle_id, status_reservation " +
                "FROM reservation_spot " +
                "WHERE ref_account_id = ? " +
                "AND parking_id = ? " +
                "AND status_reservation IN ('Paid', 'Active') " +
                "AND valid_from <= ? " +
                "AND valid_until >= ? " +
                "AND (vehicle_id IS NULL OR vehicle_id = ?) " +
                "ORDER BY valid_until ASC LIMIT 1";
        
        var list = jdbc.query(sql, (rs, i) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("reservation_id", rs.getLong("reservation_id"));
            result.put("spot_id", rs.getLong("spot_id"));
            Object validFromObj = rs.getObject("valid_from");
            if (validFromObj != null) {
                result.put("valid_from", rs.getTimestamp("valid_from").toInstant());
            }
            result.put("valid_until", rs.getTimestamp("valid_until").toInstant());
            Object vehicleIdObj = rs.getObject("vehicle_id");
            if (vehicleIdObj != null) {
                result.put("vehicle_id", rs.getLong("vehicle_id"));
            } else {
                result.put("vehicle_id", null);
            }
            result.put("status", rs.getString("status_reservation"));
            return result;
        }, accountId, parkingId, 
           java.sql.Timestamp.from(entryTimePlus5Min),
           java.sql.Timestamp.from(entryTime),
           vehicleId);
        
        Optional<Map<String, Object>> result = list.stream().findFirst();
        if (result.isPresent()) {
            log.info("Found active reservation: reservationId={}, spotId={}, validFrom={}, validUntil={}, vehicleId={}, status={}", 
                result.get().get("reservation_id"), result.get().get("spot_id"), 
                result.get().get("valid_from"), result.get().get("valid_until"),
                result.get().get("vehicle_id"), result.get().get("status"));
        } else {
            log.warn("No active reservation found: accountId={}, parkingId={}, vehicleId={}, entryTime={}, entryTime+5min={}", 
                accountId, parkingId, vehicleId, entryTime, entryTimePlus5Min);
            if (!debugList.isEmpty()) {
                log.warn("Analyzing why reservations don't match:");
                for (Map<String, Object> res : debugList) {
                    String status = (String) res.get("status");
                    java.time.Instant validFrom = (java.time.Instant) res.get("valid_from");
                    java.time.Instant validUntil = (java.time.Instant) res.get("valid_until");
                    Object resVehicleId = res.get("vehicle_id");
                    
                    boolean statusOk = "Paid".equals(status) || "Active".equals(status);
                    boolean timeOk = validFrom.isBefore(entryTimePlus5Min) || validFrom.equals(entryTimePlus5Min); // valid_from <= entryTime + 5min
                    boolean validUntilOk = validUntil.isAfter(entryTime) || validUntil.equals(entryTime); // entryTime <= valid_until
                    boolean vehicleOk = resVehicleId == null || resVehicleId.equals(vehicleId);
                    
                    log.warn("  Reservation {}: status={} (OK: {}), validFrom={} <= entryTime+5min={} (OK: {}), " +
                            "validUntil={} >= entryTime={} (OK: {}), vehicleId={} matches {} (OK: {})",
                        res.get("reservation_id"), status, statusOk,
                        validFrom, entryTimePlus5Min, timeOk,
                        validUntil, entryTime, validUntilOk,
                        resVehicleId, vehicleId, vehicleOk);
                }
            }
        }
        
        return result;
    }

    /**
     * Znajduje aktywną sesję parkingową dla danego konta użytkownika
     * Zwraca sesję z wzbogaconymi danymi o parking i miejsce parkingowe
     */
    public Optional<Map<String, Object>> findActiveSessionByAccountId(Long accountId) {
        String sql = "SELECT " +
                "ps.session_id, " +
                "ps.entry_time, " +
                "ps.exit_time, " +
                "ps.price_total_minor, " +
                "ps.payment_status, " +
                "ps.parking_id, " +
                "ps.spot_id, " +
                "ps.ref_vehicle_id, " +
                "ps.ref_account_id, " +
                "ps.reservation_id, " +
                "pl.name_parking AS parking_name, " +
                "pl.address_line AS parking_address, " +
                "pspot.code AS spot_code, " +
                "pspot.floor_lvl AS spot_floor " +
                "FROM parking_session ps " +
                "LEFT JOIN parking_location pl ON ps.parking_id = pl.parking_id " +
                "LEFT JOIN parking_spot pspot ON ps.spot_id = pspot.spot_id " +
                "WHERE ps.ref_account_id = ? " +
                "AND ps.exit_time IS NULL " +
                "AND ps.payment_status = 'Session' " +
                "ORDER BY ps.entry_time DESC " +
                "LIMIT 1";
        
        List<Map<String, Object>> results = jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("session_id", rs.getLong("session_id"));
            result.put("entry_time", rs.getTimestamp("entry_time").toInstant());
            result.put("exit_time", rs.getTimestamp("exit_time") != null ? rs.getTimestamp("exit_time").toInstant() : null);
            result.put("price_total_minor", rs.getBigDecimal("price_total_minor"));
            result.put("payment_status", rs.getString("payment_status"));
            result.put("parking_id", rs.getLong("parking_id"));
            result.put("spot_id", rs.getLong("spot_id"));
            result.put("vehicle_id", rs.getLong("ref_vehicle_id"));
            // Obsługa NULL w ref_account_id - dla niezarejestrowanych klientów
            Object accountIdObj = rs.getObject("ref_account_id");
            if (accountIdObj != null) {
                result.put("account_id", rs.getLong("ref_account_id"));
            } else {
                result.put("account_id", null);
            }
            // Obsługa NULL w reservation_id - tylko sesje z rezerwacji mają to pole
            Object reservationIdObj = rs.getObject("reservation_id");
            if (reservationIdObj != null) {
                result.put("reservation_id", rs.getLong("reservation_id"));
            } else {
                result.put("reservation_id", null);
            }
            result.put("parking_name", rs.getString("parking_name"));
            result.put("parking_address", rs.getString("parking_address"));
            result.put("spot_code", rs.getString("spot_code"));
            result.put("spot_floor", rs.getInt("spot_floor"));
            return result;
        }, accountId);
        
        return results.stream().findFirst();
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

    /**
     * Automatycznie wygasa niewykorzystane rezerwacje (status "Paid" -> "Expired").
     * Rezerwacje, które minęły (valid_until < now()) i nie zostały użyte (nie ma sesji parkingowej),
     * są automatycznie oznaczane jako "Expired".
     * 
     * @return Liczba wygaszonych rezerwacji
     */
    public int expireUnusedReservations() {
        String sql = "UPDATE reservation_spot " +
                "SET status_reservation = 'Expired'::public.reservation_status " +
                "WHERE status_reservation = 'Paid' " +
                "AND valid_until < now() " +
                "AND NOT EXISTS (" +
                "    SELECT 1 FROM parking_session ps " +
                "    WHERE ps.reservation_id = reservation_spot.reservation_id " +
                ")";
        
        return jdbc.update(sql);
    }

    /**
     * Anuluje rezerwację (ustawia status na "Cancelled")
     * Można anulować tylko rezerwacje ze statusem 'Paid', które jeszcze się nie rozpoczęły (valid_from > now())
     * i które należą do danego konta użytkownika.
     * 
     * @param reservationId ID rezerwacji do anulowania
     * @param accountId ID konta użytkownika (do weryfikacji właściciela)
     * @return true jeśli anulowano, false jeśli nie można anulować (nieprawidłowy status, już rozpoczęta, nie należy do użytkownika)
     */
    public boolean cancelReservation(Long reservationId, Long accountId) {
        // Sprawdź czy rezerwacja istnieje, należy do użytkownika, ma status 'Paid' i jeszcze się nie rozpoczęła
        String sql = "UPDATE reservation_spot " +
                "SET status_reservation = 'Cancelled'::public.reservation_status " +
                "WHERE reservation_id = ? " +
                "AND ref_account_id = ? " +  // Tylko właściciel może anulować
                "AND status_reservation = 'Paid' " +  // Tylko nieużyte rezerwacje (wyklucza 'Active', 'Expired', 'Cancelled', 'Used')
                "AND valid_from > now() " +  // Tylko przed rozpoczęciem (valid_from musi być w przyszłości)
                "RETURNING reservation_id";
        
        try {
            Long updatedId = jdbc.queryForObject(sql, Long.class, reservationId, accountId);
            return updatedId != null;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Anuluje rezerwację bez sprawdzania właściciela (używane wewnętrznie)
     * @deprecated Użyj cancelReservation(reservationId, accountId) dla bezpieczeństwa
     */
    @Deprecated
    public boolean cancelReservation(Long reservationId) {
        int updated = jdbc.update(
                "UPDATE reservation_spot SET status_reservation = 'Cancelled'::public.reservation_status WHERE reservation_id = ? AND status_reservation = 'Paid'",
                reservationId
        );
        return updated > 0;
    }

    /**
     * Znajduje rezerwację po ID
     * 
     * @param reservationId ID rezerwacji
     * @return Rezerwacja jeśli znaleziona, Optional.empty() jeśli brak
     */
    public Optional<Map<String, Object>> findReservationById(Long reservationId) {
        String sql = "SELECT reservation_id, spot_id, valid_from, valid_until, status_reservation, " +
                "parking_id, ref_account_id, vehicle_id " +
                "FROM reservation_spot " +
                "WHERE reservation_id = ?";
        
        var list = jdbc.query(sql, (rs, i) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("reservation_id", rs.getLong("reservation_id"));
            result.put("spot_id", rs.getLong("spot_id"));
            Object validFromObj = rs.getObject("valid_from");
            if (validFromObj != null) {
                result.put("valid_from", rs.getTimestamp("valid_from").toInstant());
            }
            result.put("valid_until", rs.getTimestamp("valid_until").toInstant());
            result.put("status_reservation", rs.getString("status_reservation"));
            result.put("parking_id", rs.getLong("parking_id"));
            result.put("ref_account_id", rs.getLong("ref_account_id"));
            Object vehicleIdObj = rs.getObject("vehicle_id");
            if (vehicleIdObj != null) {
                result.put("vehicle_id", rs.getLong("vehicle_id"));
            }
            return result;
        }, reservationId);
        
        return list.stream().findFirst();
    }

    /**
     * Znajduje wszystkie sesje parkingowe dla danego konta użytkownika
     * @param accountId ID konta użytkownika
     * @param unpaidOnly jeśli true, zwraca tylko niezapłacone sesje (exit_time != NULL, status != 'Paid')
     * @return Lista sesji parkingowych z wzbogaconymi danymi o parking i miejsce parkingowe
     */
    public List<Map<String, Object>> findSessionsByAccountId(Long accountId, boolean unpaidOnly) {
        String sql = "SELECT " +
                "ps.session_id, " +
                "ps.entry_time, " +
                "ps.exit_time, " +
                "ps.price_total_minor, " +
                "ps.payment_status, " +
                "ps.parking_id, " +
                "ps.spot_id, " +
                "ps.ref_vehicle_id, " +
                "ps.ref_account_id, " +
                "ps.reservation_id, " +
                "pl.name_parking AS parking_name, " +
                "pl.address_line AS parking_address " +
                "FROM parking_session ps " +
                "LEFT JOIN parking_location pl ON ps.parking_id = pl.parking_id " +
                "WHERE ps.ref_account_id = ? " +
                (unpaidOnly ? "AND ps.exit_time IS NOT NULL AND ps.payment_status IN ('Unpaid') " : "") +
                "ORDER BY ps.entry_time DESC";
        
        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("session_id", rs.getLong("session_id"));
            result.put("entry_time", rs.getTimestamp("entry_time").toInstant());
            result.put("exit_time", rs.getTimestamp("exit_time") != null ? rs.getTimestamp("exit_time").toInstant() : null);
            result.put("price_total_minor", rs.getBigDecimal("price_total_minor"));
            result.put("payment_status", rs.getString("payment_status"));
            result.put("parking_id", rs.getLong("parking_id"));
            result.put("spot_id", rs.getLong("spot_id"));
            result.put("vehicle_id", rs.getLong("ref_vehicle_id"));
            // Obsługa NULL w ref_account_id - dla niezarejestrowanych klientów
            Object accountIdObj = rs.getObject("ref_account_id");
            if (accountIdObj != null) {
                result.put("account_id", rs.getLong("ref_account_id"));
            } else {
                result.put("account_id", null);
            }
            // Obsługa NULL w reservation_id - tylko sesje z rezerwacji mają to pole
            Object reservationIdObj = rs.getObject("reservation_id");
            if (reservationIdObj != null) {
                result.put("reservation_id", rs.getLong("reservation_id"));
            } else {
                result.put("reservation_id", null);
            }
            result.put("parking_name", rs.getString("parking_name"));
            result.put("parking_address", rs.getString("parking_address"));
            return result;
        }, accountId);
    }

    /**
     * Oblicza statystyki sesji parkingowych dla danego konta użytkownika
     * @param accountId ID konta użytkownika
     * @return Map ze statystykami: totalSessions (liczba zakończonych sesji), totalHours (łączna liczba godzin), totalSpent (łączna kwota wydana w groszach)
     */
    public Map<String, Object> getSessionStatistics(Long accountId) {
        String sql = "SELECT " +
                "COUNT(*) FILTER (WHERE exit_time IS NOT NULL) AS total_sessions, " +
                "COALESCE(SUM(EXTRACT(EPOCH FROM (exit_time - entry_time)) / 3600.0), 0) AS total_hours, " +
                "COALESCE(SUM(price_total_minor) FILTER (WHERE payment_status = 'Paid'), 0) AS total_spent_minor " +
                "FROM parking_session " +
                "WHERE ref_account_id = ?";
        
        return jdbc.queryForObject(sql, (rs, rowNum) -> {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSessions", rs.getLong("total_sessions"));
            stats.put("totalHours", rs.getDouble("total_hours"));
            // Konwersja z groszy na złotówki (dzielenie przez 100.0)
            stats.put("totalSpent", rs.getLong("total_spent_minor") / 100.0);
            return stats;
        }, accountId);
    }

}
