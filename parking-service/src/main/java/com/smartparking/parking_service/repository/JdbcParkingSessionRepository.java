package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcParkingSessionRepository implements ParkingSessionRepository {

    private final JdbcTemplate jdbc;

    public JdbcParkingSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ParkingSession> mapper = new RowMapper<>() {
        @Override
        public ParkingSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            ParkingSession s = new ParkingSession();
            s.setId(rs.getLong("session_id"));

            Timestamp entryTs = rs.getTimestamp("entry_time");
            s.setEntryTime(entryTs != null ? entryTs.toLocalDateTime() : null);

            Timestamp exitTs = rs.getTimestamp("exit_time");
            s.setExitTime(exitTs != null ? exitTs.toLocalDateTime() : null);

            s.setPriceTotalMinor(rs.getBigDecimal("price_total_minor"));
            s.setPaymentStatus(rs.getString("payment_status"));
            s.setParkingId(rs.getLong("parking_id"));
            s.setSpotId(rs.getLong("spot_id"));
            s.setRefVehicleId(rs.getLong("ref_vehicle_id"));
            s.setRefAccountId(rs.getLong("ref_account_id"));
            return s;
        }
    };

    @Override
    public Optional<ParkingSession> findById(Long id) {
        var list = jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id," +
                        "ref_account_id FROM parking_session WHERE session_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ParkingSession> findByAccountId(Long accountId) {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id FROM parking_session WHERE ref_account_id = ? ORDER BY entry_time DESC",
                mapper,
                accountId
        );
    }

    @Override
    public List<ParkingSession> findByVehicleId(Long vehicleId) {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id," +
                        " ref_account_id FROM parking_session WHERE ref_vehicle_id = ? ORDER BY entry_time DESC",
                mapper,
                vehicleId
        );
    }

    @Override
    public List<ParkingSession> findByParkingId(Long parkingId) {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id FROM parking_session WHERE parking_id = ? ORDER BY entry_time DESC",
                mapper,
                parkingId
        );
    }

    @Override
    public List<ParkingSession> findAll() {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id FROM parking_session ORDER BY entry_time DESC",
                mapper
        );
    }

    @Override
    public List<ParkingSession> findActiveSession(){
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id FROM parking_session WHERE exit_time is NULL",
                mapper
        );
    }

    @Override
    public Optional<ParkingSession> findActiveSessionByVehicleAndParking(Long vehicleId, Long parkingId) {
        var list = jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id FROM parking_session " +
                        "WHERE ref_vehicle_id = ? AND parking_id = ? AND exit_time IS NULL " +
                        "ORDER BY entry_time DESC LIMIT 1",
                mapper,
                vehicleId, parkingId
        );
        return list.stream().findFirst();
    }

    @Override
    public Long countActiveSessionsByParkingId(Long parkingId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM parking_session " +
                        "WHERE parking_id = ? AND exit_time IS NULL AND payment_status = 'Session'",
                Long.class,
                parkingId
        );
    }


    @Override
    public ParkingSession save(ParkingSession session) {
        if (session.getId() == null) {
            if (session.getEntryTime() == null) {
                session.setEntryTime(LocalDateTime.now());
            }
            Long id = jdbc.queryForObject(
                    "INSERT INTO parking_session(" +
                            "entry_time, exit_time, price_total_minor, payment_status, " +
                            "parking_id, spot_id, ref_vehicle_id, ref_account_id" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING session_id",
                    Long.class,
                    Timestamp.valueOf(session.getEntryTime()),
                    session.getExitTime() != null ? Timestamp.valueOf(session.getExitTime()) : null,
                    session.getPriceTotalMinor(),
                    session.getPaymentStatus(),
                    session.getParkingId(),
                    session.getSpotId(),
                    session.getRefVehicleId(),
                    session.getRefAccountId()
            );
            session.setId(id);
            return session;
        } else {
            jdbc.update(
                    "UPDATE parking_session SET " +
                            "entry_time = ?, exit_time = ?, price_total_minor = ?, payment_status = ?, " +
                            "parking_id = ?, spot_id = ?, ref_vehicle_id = ?, ref_account_id = ? " +
                            "WHERE session_id = ?",
                    Timestamp.valueOf(session.getEntryTime()),
                    session.getExitTime() != null ? Timestamp.valueOf(session.getExitTime()) : null,
                    session.getPriceTotalMinor(),
                    session.getPaymentStatus(),
                    session.getParkingId(),
                    session.getSpotId(),
                    session.getRefVehicleId(),
                    session.getRefAccountId(),
                    session.getId()
            );
            return session;
        }
    }
}
