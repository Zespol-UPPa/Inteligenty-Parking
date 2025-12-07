package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingPricing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcParkingPricingRepository implements ParkingPricingRepository {

    private final JdbcTemplate jdbc;

    public JdbcParkingPricingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ParkingPricing> mapper = new RowMapper<>() {
        @Override
        public ParkingPricing mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParkingPricing p = new ParkingPricing();
            p.setId(rs.getLong("pricing_id"));
            p.setCurrencyCode(rs.getString("curency_code"));
            p.setRatePerMin(rs.getInt("rate_per_min"));
            p.setFreeMinutes(rs.getInt("free_minutes"));
            p.setRoundingStepMin(rs.getInt("rounding_step_min"));
            p.setReservationFeeMinor(rs.getInt("reservation_fee_minor"));
            p.setParkingId(rs.getLong("parking_id"));
            return p;
        }
    };

    @Override
    public Optional<ParkingPricing> findById(Long id) {
        var list = jdbc.query(
                "SELECT pricing_id, curency_code, rate_per_min, free_minutes, rounding_step_min, " +
                        "reservation_fee_minor, parking_id " +
                        "FROM parking_pricing WHERE pricing_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<ParkingPricing> findByParkingId(Long parkingId) {
        var list = jdbc.query(
                "SELECT pricing_id, curency_code, rate_per_min, free_minutes, rounding_step_min, " +
                        "reservation_fee_minor, parking_id " +
                        "FROM parking_pricing WHERE parking_id = ?",
                mapper,
                parkingId
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ParkingPricing> findAll() {
        return jdbc.query(
                "SELECT pricing_id, curency_code, rate_per_min, free_minutes, rounding_step_min, " +
                        "reservation_fee_minor, parking_id " +
                        "FROM parking_pricing",
                mapper
        );
    }

    @Override
    public ParkingPricing save(ParkingPricing pricing) {
        if (pricing.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO parking_pricing (curency_code, rate_per_min, free_minutes, " +
                            "rounding_step_min, reservation_fee_minor, parking_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?) RETURNING pricing_id",
                    Long.class,
                    pricing.getCurrencyCode(),
                    pricing.getRatePerMin(),
                    pricing.getFreeMinutes(),
                    pricing.getRoundingStepMin(),
                    pricing.getReservationFeeMinor(),
                    pricing.getParkingId()
            );

            pricing.setId(id);
            return pricing;

        } else {
            jdbc.update(
                    "UPDATE parking_pricing SET curency_code = ?, rate_per_min = ?, free_minutes = ?, " +
                            "rounding_step_min = ?, reservation_fee_minor = ?, parking_id = ? " +
                            "WHERE pricing_id = ?",
                    pricing.getCurrencyCode(),
                    pricing.getRatePerMin(),
                    pricing.getFreeMinutes(),
                    pricing.getRoundingStepMin(),
                    pricing.getReservationFeeMinor(),
                    pricing.getParkingId(),
                    pricing.getId()
            );
            return pricing;
        }
    }
}