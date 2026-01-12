package com.smartparking.payment_service.repository;

import com.smartparking.payment_service.model.VirtualPayment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class JdbcVirtualPaymentRepository implements VirtualPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcVirtualPaymentRepository.class);
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

            // Obsługa NULL w ref_account_id - dla niezarejestrowanych klientów
            Object accountIdObj = rs.getObject("ref_account_id");
            if (accountIdObj != null) {
                p.setRefAccountId(rs.getLong("ref_account_id"));
            } else {
                p.setRefAccountId(null);
            }
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

            try {
                Long id = jdbc.queryForObject(
                        "INSERT INTO virtual_payment(" +
                                "amount_minor, currency_code, status_paid, date_transaction, " +
                                "ref_account_id, ref_session_id, activity" +
                                ") VALUES (?, ?, CAST(? AS public.status_paid), ?, ?, ?, CAST(? AS public.activity_type)) RETURNING payment_id",
                        Long.class,
                        payment.getAmountMinor(),
                        payment.getCurrencyCode(),
                        payment.getStatusPaid(),
                        Timestamp.valueOf(payment.getDateTransaction()),
                        payment.getRefAccountId() != null ? payment.getRefAccountId() : null,
                        payment.getRefSessionId(),
                        payment.getActivity()
                );

                payment.setId(id);
                return payment;
            } catch (DuplicateKeyException e) {
                // Sekwencja jest niezsynchronizowana - napraw ją i spróbuj ponownie
                log.warn("Duplicate key error detected, fixing sequence: {}", e.getMessage());
                try {
                    jdbc.execute("SELECT setval('virtual_payment_id_payment_seq', " +
                                "COALESCE((SELECT MAX(payment_id) FROM virtual_payment), 1), true)");
                    log.info("Sequence fixed, retrying insert");
                    
                    // Spróbuj ponownie
                    Long id = jdbc.queryForObject(
                            "INSERT INTO virtual_payment(" +
                                    "amount_minor, currency_code, status_paid, date_transaction, " +
                                    "ref_account_id, ref_session_id, activity" +
                                    ") VALUES (?, ?, CAST(? AS public.status_paid), ?, ?, ?, CAST(? AS public.activity_type)) RETURNING payment_id",
                            Long.class,
                            payment.getAmountMinor(),
                            payment.getCurrencyCode(),
                            payment.getStatusPaid(),
                            Timestamp.valueOf(payment.getDateTransaction()),
                            payment.getRefAccountId() != null ? payment.getRefAccountId() : null,
                            payment.getRefSessionId(),
                            payment.getActivity()
                    );
                    payment.setId(id);
                    return payment;
                } catch (Exception retryException) {
                    log.error("Failed to insert payment even after sequence fix", retryException);
                    throw new RuntimeException("Failed to save payment after sequence fix", retryException);
                }
            }

        } else {
            jdbc.update(
                    "UPDATE virtual_payment SET " +
                            "amount_minor = ?, currency_code = ?, status_paid = CAST(? AS public.status_paid), date_transaction = ?, " +
                            "ref_account_id = ?, ref_session_id = ?, activity = CAST(? AS public.activity_type) " +
                            "WHERE payment_id = ?",
                    payment.getAmountMinor(),
                    payment.getCurrencyCode(),
                    payment.getStatusPaid(),
                    Timestamp.valueOf(payment.getDateTransaction()),
                    payment.getRefAccountId() != null ? payment.getRefAccountId() : null,
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

    @Override
    public List<VirtualPayment> findBySessionIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Create placeholders for IN clause
        String placeholders = String.join(",", Collections.nCopies(sessionIds.size(), "?"));

        String sql = "SELECT * FROM virtual_payment WHERE ref_session_id IN (" + placeholders + ") " +
                "ORDER BY date_transaction DESC";

        return jdbc.query(sql, mapper, sessionIds.toArray());
    }

    @Override
    public List<VirtualPayment> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM virtual_payment " +
                "WHERE date_transaction BETWEEN ? AND ? " +
                "ORDER BY date_transaction DESC";

        return jdbc.query(sql, mapper,
                Timestamp.valueOf(startDate),
                Timestamp.valueOf(endDate));
    }

    @Override
    public Map<String, Object> getPaymentSummary(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
        SELECT 
            -- Total revenue (all payments in period)
            COALESCE(SUM(amount_minor), 0) / 100.0 as totalRevenue,
            
            -- Parking revenue (Paid parking sessions only)
            COALESCE(SUM(CASE 
                WHEN status_paid = 'Paid' AND activity = 'parking'
                THEN amount_minor 
                ELSE 0 
            END), 0) / 100.0 as parkingRevenue,
            
            -- Pending payments
            COALESCE(SUM(CASE 
                WHEN status_paid = 'Pending'
                THEN amount_minor 
                ELSE 0 
            END), 0) / 100.0 as pendingPayments,
            
            -- Reservation fees
            COALESCE(SUM(CASE 
                WHEN activity = 'reservation' AND status_paid = 'Paid'
                THEN amount_minor 
                ELSE 0 
            END), 0) / 100.0 as reservationFees,
            
            -- Deposit total
            COALESCE(SUM(CASE 
                WHEN activity = 'deposit' AND status_paid = 'Paid'
                THEN amount_minor 
                ELSE 0 
            END), 0) / 100.0 as depositsTotal,
            
            -- Total transaction count
            COUNT(*) as totalTransactions,
            
            -- Average transaction value
            COALESCE(AVG(amount_minor), 0) / 100.0 as avgTransactionValue,
            
            -- Count by status
            COUNT(CASE WHEN status_paid = 'Paid' THEN 1 END) as paidCount,
            COUNT(CASE WHEN status_paid = 'Pending' THEN 1 END) as pendingCount,
            COUNT(CASE WHEN status_paid = 'Failed' THEN 1 END) as failedCount
            
        FROM virtual_payment
        WHERE date_transaction BETWEEN ? AND ?
        """;

        try {
            return jdbc.queryForMap(sql,
                    Timestamp.valueOf(startDate),
                    Timestamp.valueOf(endDate));
        } catch (Exception e) {
            log.error("Error getting payment summary", e);
            return createEmptyPaymentSummary();
        }
    }

    @Override
    public List<Map<String, Object>> getPaymentsGroupedByPeriod(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String groupBy) {

        // Determine SQL grouping
        String dateFormat;
        String groupByClause;

        switch (groupBy.toLowerCase()) {
            case "day":
                dateFormat = "YYYY-MM-DD";
                groupByClause = "DATE(date_transaction)";
                break;
            case "week":
                dateFormat = "YYYY-'W'IW"; // ISO week format
                groupByClause = "DATE_TRUNC('week', date_transaction)";
                break;
            case "month":
            default:
                dateFormat = "YYYY-MM";
                groupByClause = "DATE_TRUNC('month', date_transaction)";
                break;
        }

        String sql = String.format("""
        SELECT 
            TO_CHAR(%s, '%s') as period,
            COALESCE(SUM(amount_minor), 0) / 100.0 as totalRevenue,
            COALESCE(SUM(CASE 
                WHEN status_paid = 'Paid' AND activity = 'parking'
                THEN amount_minor 
                ELSE 0 
            END), 0) / 100.0 as parkingRevenue,
            COUNT(*) as transactionCount
        FROM virtual_payment
        WHERE date_transaction BETWEEN ? AND ?
        GROUP BY %s
        ORDER BY %s
        """, groupByClause, dateFormat, groupByClause, groupByClause);

        try {
            return jdbc.queryForList(sql,
                    Timestamp.valueOf(startDate),
                    Timestamp.valueOf(endDate));
        } catch (Exception e) {
            log.error("Error getting payments grouped by period", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<VirtualPayment> findBySessionIdsAndDateRange(
            List<Long> sessionIds,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        String placeholders = String.join(",", Collections.nCopies(sessionIds.size(), "?"));

        String sql = "SELECT * FROM virtual_payment " +
                "WHERE ref_session_id IN (" + placeholders + ") " +
                "AND date_transaction BETWEEN ? AND ? " +
                "ORDER BY date_transaction DESC";

        List<Object> params = new ArrayList<>(sessionIds);
        params.add(Timestamp.valueOf(startDate));
        params.add(Timestamp.valueOf(endDate));

        return jdbc.query(sql, mapper, params.toArray());
    }

// ========================================
// HELPER METHODS
// ========================================

    /**
     * Create empty payment summary (fallback)
     */
    private Map<String, Object> createEmptyPaymentSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRevenue", 0.0);
        summary.put("parkingRevenue", 0.0);
        summary.put("pendingPayments", 0.0);
        summary.put("reservationFees", 0.0);
        summary.put("depositsTotal", 0.0);
        summary.put("totalTransactions", 0);
        summary.put("avgTransactionValue", 0.0);
        summary.put("paidCount", 0);
        summary.put("pendingCount", 0);
        summary.put("failedCount", 0);
        return summary;
    }

}
