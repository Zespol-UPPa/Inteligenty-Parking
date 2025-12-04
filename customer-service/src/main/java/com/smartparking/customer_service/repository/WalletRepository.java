package com.smartparking.customer_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Repository
public class WalletRepository {
    private final JdbcTemplate jdbc;

    public WalletRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findByAccountId(Long accountId) {
        try {
            Map<String, Object> row = jdbc.queryForMap("SELECT wallet_id, balance_minor, currency_code, ref_account_id FROM wallet WHERE ref_account_id = ?", accountId);
            return Optional.of(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int updateBalanceByAccountId(Long accountId, BigDecimal newBalance) {
        return jdbc.update("UPDATE wallet SET balance_minor = ? WHERE ref_account_id = ?", newBalance, accountId);
    }
}

