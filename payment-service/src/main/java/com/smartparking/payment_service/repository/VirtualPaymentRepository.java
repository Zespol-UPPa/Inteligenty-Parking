package com.smartparking.payment_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class VirtualPaymentRepository {
    private final JdbcTemplate jdbc;
    public VirtualPaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long create(Long accountId, Long sessionId, BigDecimal amount, String currencyCode, String statusPaid) {
        // Amount is expected as major units (e.g. 12.34). Convert to minor units (e.g. 1234)
        int amountMinor = amount.multiply(new BigDecimal(100)).intValue();
        // Insert using the actual schema of virtual_payment
        String sql = "INSERT INTO virtual_payment(amount_minor, currency_code, status_paid, date_transaction, ref_account_id, ref_session_id) " +
                "VALUES (?, ?, CAST(? AS public.status_paid), now(), ?, ?) RETURNING payment_id";
        return jdbc.queryForObject(sql, Long.class, amountMinor, currencyCode, statusPaid, accountId, sessionId);
    }
}
