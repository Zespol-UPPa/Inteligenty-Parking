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
        return jdbc.queryForObject(
                "INSERT INTO virtual_payment(id_account, id_session, amount_minor, currency_code, type) " +
                        "VALUES (?, ?, ?, ?, CAST(? AS status_paid)) RETURNING id_payment",
                Long.class, accountId, sessionId, amount, currencyCode, statusPaid);
    }
}


