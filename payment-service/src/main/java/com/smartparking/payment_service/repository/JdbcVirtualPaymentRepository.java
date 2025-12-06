package com.smartparking.payment_service.repository;

import com.smartparking.payment_service.model.VirtualPayment;
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
public class JdbcVirtualPaymentRepository implements VirtualPaymentRepository {

    private final JdbcTemplate jdbc;

    public JdbcVirtualPaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<VirtualPayment> mapper = new RowMapper<>() {
        @Override
        public VirtualPayment mapRow(ResultSet rs, int rowNum) throws SQLException {
            VirtualPayment p = new VirtualPayment();
            p.setId(rs.getLong("payment_id"));
            p.setAmountMinor(rs.getInt("amount_minor"));
            p.setCurrencyCode(rs.getString("currency_code"));
            p.setStatusPaid(rs.getString("status_paid"));

            Timestamp ts = rs.getTimestamp("date_transaction");
            p.setDateTransaction(ts != null ? ts.toLocalDateTime() : null);

            p.setRefAccountId(rs.getLong("ref_account_id"));
            p.setRefSessionId(rs.getLong("ref_session_id"));
            p.setActivity(rs.getString("activity"));
            return p;
        }
    };

    @Override
    public Optional<VirtualPayment> findById(Long id) {
        var list = jdbc.query(
                "SELECT * FROM virtual_payment WHERE payment_id = ?",
                mapper, id
        );
        return list.stream().findFirst();
    }

    @Override
    public List<VirtualPayment> findAll() {
        return jdbc.query(
                "SELECT * FROM virtual_payment ORDER BY date_transaction DESC",
                mapper
        );
    }

    @Override
    public VirtualPayment save(VirtualPayment payment) {
        if (payment.getId() == null) {

            if (payment.getDateTransaction() == null)
                payment.setDateTransaction(LocalDateTime.now());

            Long id = jdbc.queryForObject(
                    "INSERT INTO virtual_payment(" +
                            "amount_minor, currency_code, status_paid, date_transaction, " +
                            "ref_account_id, ref_session_id, activity" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING payment_id",
                    Long.class,
                    payment.getAmountMinor(),
                    payment.getCurrencyCode(),
                    payment.getStatusPaid(),
                    Timestamp.valueOf(payment.getDateTransaction()),
                    payment.getRefAccountId(),
                    payment.getRefSessionId(),
                    payment.getActivity()
            );

            payment.setId(id);
            return payment;

        } else {
            jdbc.update(
                    "UPDATE virtual_payment SET " +
                            "amount_minor = ?, currency_code = ?, status_paid = ?, date_transaction = ?, " +
                            "ref_account_id = ?, ref_session_id = ?, activity = ? " +
                            "WHERE payment_id = ?",
                    payment.getAmountMinor(),
                    payment.getCurrencyCode(),
                    payment.getStatusPaid(),
                    Timestamp.valueOf(payment.getDateTransaction()),
                    payment.getRefAccountId(),
                    payment.getRefSessionId(),
                    payment.getActivity(),
                    payment.getId()
            );
            return payment;
        }
    }


    @Override
    public List<VirtualPayment> findByAccountId(Long accountId) {
        return jdbc.query(
                "SELECT * FROM virtual_payment WHERE ref_account_id = ? ORDER BY date_transaction DESC",
                mapper,
                accountId
        );
    }

    @Override
    public List<VirtualPayment> findBySessionId(Long sessionId) {
        return jdbc.query(
                "SELECT * FROM virtual_payment WHERE ref_session_id = ? ORDER BY date_transaction DESC",
                mapper,
                sessionId
        );
    }

    @Override
    public List<VirtualPayment> findByAccountIdAndActivity(Long accountId, String activity) {
        return jdbc.query(
                "SELECT * FROM virtual_payment WHERE ref_account_id = ? AND activity = ? " +
                        "ORDER BY date_transaction DESC",
                mapper,
                accountId,
                activity
        );
    }

    @Override
    public List<VirtualPayment> findByAccountIdAndDateRange(
            Long accountId, LocalDateTime from, LocalDateTime to) {

        return jdbc.query(
                "SELECT * FROM virtual_payment " +
                        "WHERE ref_account_id = ? AND date_transaction BETWEEN ? AND ? " +
                        "ORDER BY date_transaction DESC",
                mapper,
                accountId,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        );
    }

    @Override
    public Long sumByAccountId(Long accountId) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0) FROM virtual_payment WHERE ref_account_id = ?",
                Long.class,
                accountId
        );
    }

    @Override
    public Long sumByAccountIdAndActivity(Long accountId, String activity) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0) FROM virtual_payment " +
                        "WHERE ref_account_id = ? AND activity = ?",
                Long.class,
                accountId,
                activity
        );
    }

    @Override
    public Long sumAllPaid() {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0) FROM virtual_payment WHERE status_paid = 'Paid'",
                Long.class
        );
    }


    @Override
    public Long countByAccountId(Long accountId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM virtual_payment WHERE ref_account_id = ?",
                Long.class,
                accountId
        );
    }

    @Override
    public Long countAll() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM virtual_payment",
                Long.class
        );
    }

}
