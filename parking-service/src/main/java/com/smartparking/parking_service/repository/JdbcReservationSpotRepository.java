package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ReservationSpot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcReservationSpotRepository implements ReservationSpotRepository {

    private final JdbcTemplate jdbc;

    public JdbcReservationSpotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ReservationSpot> mapper = new RowMapper<>() {
        @Override
        public ReservationSpot mapRow(ResultSet rs, int rowNum) throws SQLException {
            ReservationSpot r = new ReservationSpot();
            r.setId(rs.getLong("reservation_id"));

            Timestamp ts = rs.getTimestamp("valid_until");
            r.setValidUntil(ts != null ? ts.toLocalDateTime() : null);

            r.setStatusReservation(rs.getString("status_reservation"));
            r.setSpotId(rs.getLong("spot_id"));
            r.setParkingId(rs.getLong("parking_id"));
            r.setRefAccountId(rs.getLong("ref_account_id"));
            return r;
        }
    };

    @Override
    public Optional<ReservationSpot> findById(Long id) {
        var list = jdbc.query(
                "SELECT reservation_id,valid_until, status_reservation , spot_id, parking_id , ref_account_id " +
                        " FROM reservation_spot WHERE reservation_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ReservationSpot> findByAccountId(Long accountId) {
        return jdbc.query(
                "SELECT reservation_id,valid_until, status_reservation , spot_id, parking_id , ref_account_id " +
                        " FROM reservation_spot WHERE ref_account_id = ? ORDER BY valid_until DESC",
                mapper,
                accountId
        );
    }

    @Override
    public List<ReservationSpot> findByParkingId(Long parkingId) {
        return jdbc.query(
                "SELECT reservation_id,valid_until, status_reservation , spot_id, parking_id , ref_account_id " +
                        "  FROM reservation_spot WHERE parking_id = ? ORDER BY valid_until DESC",
                mapper,
                parkingId
        );
    }

    @Override
    public List<ReservationSpot> findBySpotId(Long spotId) {
        return jdbc.query(
                "SELECT reservation_id,valid_until, status_reservation , spot_id, parking_id , ref_account_id " +
                        "  FROM reservation_spot WHERE spot_id = ? ORDER BY valid_until DESC",
                mapper,
                spotId
        );
    }

    @Override
    public List<ReservationSpot> findActiveByAccountId(Long accountId, LocalDateTime now) {
        return jdbc.query(
                "SELECT reservation_id,valid_until, status_reservation , spot_id, parking_id , ref_account_id  FROM reservation_spot " +
                        "WHERE ref_account_id = ? AND valid_until >= ? " +
                        "AND status_reservation = 'Paid' " +
                        "ORDER BY valid_until ASC",
                mapper,
                accountId,
                Timestamp.valueOf(now)
        );
    }

    @Override
    public List<ReservationSpot> findAll() {
        return jdbc.query(
                "SELECT reservation_id,valid_until, status_reservation , spot_id, parking_id , ref_account_id " +
                        "  FROM reservation_spot ORDER BY valid_until DESC",
                mapper
        );
    }

    @Override
    public ReservationSpot save(ReservationSpot reservation) {
        if (reservation.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO reservation_spot(" +
                            "valid_until, status_reservation, spot_id, parking_id, ref_account_id" +
                            ") VALUES (?, ?, ?, ?, ?) RETURNING reservation_id",
                    Long.class,
                    Timestamp.valueOf(reservation.getValidUntil()),
                    reservation.getStatusReservation(),
                    reservation.getSpotId(),
                    reservation.getParkingId(),
                    reservation.getRefAccountId()
            );
            reservation.setId(id);
            return reservation;
        } else {
            jdbc.update(
                    "UPDATE reservation_spot SET " +
                            "valid_until = ?, status_reservation = ?, spot_id = ?, parking_id = ?, ref_account_id = ? " +
                            "WHERE reservation_id = ?",
                    Timestamp.valueOf(reservation.getValidUntil()),
                    reservation.getStatusReservation(),
                    reservation.getSpotId(),
                    reservation.getParkingId(),
                    reservation.getRefAccountId(),
                    reservation.getId()
            );
            return reservation;
        }
    }
}
