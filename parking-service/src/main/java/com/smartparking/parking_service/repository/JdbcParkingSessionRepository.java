package com.smartparking.parking_service.repository;

import com.smartparking.parking_service.model.ParkingSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcParkingSessionRepository implements ParkingSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcParkingSessionRepository.class);
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
            // Obsługa NULL w ref_account_id - dla niezarejestrowanych klientów
            Object accountIdObj = rs.getObject("ref_account_id");
            if (accountIdObj != null) {
                s.setRefAccountId(rs.getLong("ref_account_id"));
            } else {
                s.setRefAccountId(null);
            }
            // Obsługa NULL w reservation_id - tylko sesje z rezerwacji mają to pole ustawione
            Object reservationIdObj = rs.getObject("reservation_id");
            if (reservationIdObj != null) {
                s.setReservationId(rs.getLong("reservation_id"));
            } else {
                s.setReservationId(null);
            }
            return s;
        }
    };

    @Override
    public Optional<ParkingSession> findById(Long id) {
        var list = jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id," +
                        "ref_account_id, reservation_id FROM parking_session WHERE session_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<ParkingSession> findByAccountId(Long accountId) {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id, reservation_id FROM parking_session WHERE ref_account_id = ? ORDER BY entry_time DESC",
                mapper,
                accountId
        );
    }

    @Override
    public List<ParkingSession> findByVehicleId(Long vehicleId) {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id," +
                        " ref_account_id, reservation_id FROM parking_session WHERE ref_vehicle_id = ? ORDER BY entry_time DESC",
                mapper,
                vehicleId
        );
    }

    @Override
    public List<ParkingSession> findByParkingId(Long parkingId) {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id, reservation_id FROM parking_session WHERE parking_id = ? ORDER BY entry_time DESC",
                mapper,
                parkingId
        );
    }

    @Override
    public List<ParkingSession> findAll() {
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id, reservation_id FROM parking_session ORDER BY entry_time DESC",
                mapper
        );
    }

    @Override
    public List<ParkingSession> findActiveSession(){
        return jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id, reservation_id FROM parking_session WHERE exit_time is NULL",
                mapper
        );
    }

    @Override
    public Optional<ParkingSession> findActiveSessionByVehicleAndParking(Long vehicleId, Long parkingId) {
        var list = jdbc.query(
                "SELECT session_id , entry_time, exit_time, price_total_minor, payment_status,parking_id, spot_id, ref_vehicle_id, " +
                        " ref_account_id, reservation_id FROM parking_session " +
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
        return save(session, 0);
    }
    
    /**
     * Wewnętrzna metoda save z licznikiem rekurencji dla zabezpieczenia przed nieskończoną pętlą.
     */
    private ParkingSession save(ParkingSession session, int retryCount) {
        if (retryCount > 2) {
            log.error("Maximum retry count exceeded for session save, throwing exception");
            throw new IllegalStateException("Failed to save session after multiple retry attempts - sequence synchronization may have failed");
        }
        
        if (session.getId() == null) {
            if (session.getEntryTime() == null) {
                session.setEntryTime(LocalDateTime.now());
            }
            try {
                Long id = jdbc.queryForObject(
                        "INSERT INTO parking_session(" +
                                "entry_time, exit_time, price_total_minor, payment_status, " +
                                "parking_id, spot_id, ref_vehicle_id, ref_account_id, reservation_id" +
                                ") VALUES (?, ?, ?, ?::payment_status, ?, ?, ?, ?, ?) RETURNING session_id",
                        Long.class,
                        Timestamp.valueOf(session.getEntryTime()),
                        session.getExitTime() != null ? Timestamp.valueOf(session.getExitTime()) : null,
                        session.getPriceTotalMinor(),
                        session.getPaymentStatus(),
                        session.getParkingId(),
                        session.getSpotId(),
                        session.getRefVehicleId(),
                        session.getRefAccountId() != null ? session.getRefAccountId() : null,
                        session.getReservationId() != null ? session.getReservationId() : null
                );
                session.setId(id);
                return session;
            } catch (DuplicateKeyException e) {
                // Sekwencja może być niezsynchronizowana - spróbuj znaleźć istniejącą sesję
                log.error("Duplicate key error when creating session - sequence may be out of sync. " +
                        "Attempting to find existing session for vehicleId={}, parkingId={}, entryTime={}", 
                        session.getRefVehicleId(), session.getParkingId(), session.getEntryTime());
                
                // Spróbuj wyciągnąć session_id z błędu
                String errorMessage = e.getMessage();
                Long existingSessionId = null;
                if (errorMessage != null && errorMessage.contains("Key (session_id)=(")) {
                    try {
                        int startIdx = errorMessage.indexOf("Key (session_id)=(") + "Key (session_id)=(".length();
                        int endIdx = errorMessage.indexOf(")", startIdx);
                        if (endIdx > startIdx) {
                            existingSessionId = Long.parseLong(errorMessage.substring(startIdx, endIdx));
                            log.info("Extracted existing session_id={} from error message", existingSessionId);
                        }
                    } catch (Exception ex) {
                        log.warn("Could not extract session_id from error message: {}", ex.getMessage());
                    }
                }
                
                // Spróbuj znaleźć istniejącą sesję (aktywną lub zakończoną)
                Optional<ParkingSession> existing = Optional.empty();
                
                // Najpierw spróbuj znaleźć aktywną sesję
                existing = findActiveSessionByVehicleAndParking(
                    session.getRefVehicleId(), session.getParkingId());
                
                // Jeśli nie znaleziono aktywnej, ale mamy session_id z błędu, spróbuj znaleźć po ID
                if (existing.isEmpty() && existingSessionId != null) {
                    existing = findById(existingSessionId);
                    if (existing.isPresent()) {
                        ParkingSession foundSession = existing.get();
                        if (foundSession.getExitTime() != null) {
                            log.warn("Found existing session with id={} but it has exit_time set (exit_time={}) - " +
                                    "this is a sequence sync issue. Synchronizing sequence and creating new session.", 
                                    existingSessionId, foundSession.getExitTime());
                            // Zsynchronizuj sekwencję
                            synchronizeSequence();
                            // Spróbuj ponownie utworzyć sesję
                            return save(session, retryCount + 1); // Rekurencyjne wywołanie z licznikiem
                        } else {
                            log.warn("Found existing active session with id={} - returning it instead of creating new one", 
                                existingSessionId);
                            return foundSession;
                        }
                    }
                }
                
                if (existing.isPresent()) {
                    log.warn("Found existing active session with id={} - returning it instead of creating new one", 
                        existing.get().getId());
                    return existing.get();
                }
                
                // Jeśli nie znaleziono, zsynchronizuj sekwencję i spróbuj ponownie
                log.warn("Duplicate key error and no existing session found - synchronizing sequence and retrying (attempt {})", retryCount + 1);
                synchronizeSequence();
                return save(session, retryCount + 1); // Rekurencyjne wywołanie z licznikiem
            }
        } else {
            jdbc.update(
                    "UPDATE parking_session SET " +
                            "entry_time = ?, exit_time = ?, price_total_minor = ?, payment_status = ?::payment_status, " +
                            "parking_id = ?, spot_id = ?, ref_vehicle_id = ?, ref_account_id = ?, reservation_id = ? " +
                            "WHERE session_id = ?",
                    Timestamp.valueOf(session.getEntryTime()),
                    session.getExitTime() != null ? Timestamp.valueOf(session.getExitTime()) : null,
                    session.getPriceTotalMinor(),
                    session.getPaymentStatus(),
                    session.getParkingId(),
                    session.getSpotId(),
                    session.getRefVehicleId(),
                    session.getRefAccountId() != null ? session.getRefAccountId() : null,
                    session.getReservationId() != null ? session.getReservationId() : null,
                    session.getId()
            );
            return session;
        }
    }
    
    /**
     * Synchronizuje sekwencję session_id z maksymalnym ID w bazie danych.
     * Używane gdy sekwencja jest niezsynchronizowana (np. po operacjach COPY).
     */
    private void synchronizeSequence() {
        try {
            Long maxId = jdbc.queryForObject(
                "SELECT COALESCE(MAX(session_id), 0) FROM parking_session", Long.class);
            if (maxId != null) {
                jdbc.update("SELECT setval('parking_session_session_id_seq', ?, true)", maxId);
                log.info("Synchronized parking_session_session_id_seq to {}", maxId);
            }
        } catch (Exception e) {
            log.error("Failed to synchronize parking_session_session_id_seq: {}", e.getMessage(), e);
        }
    }
}
