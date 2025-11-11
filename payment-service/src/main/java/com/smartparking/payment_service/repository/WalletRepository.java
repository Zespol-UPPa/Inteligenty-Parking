package com.smartparking.payment_service.repository;

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
            Map<String, Object> result = jdbc.queryForMap(
                    "SELECT id_wallet, balance_minor, currency_code, id_account FROM wallet WHERE id_account = ?",
                    accountId);
            return Optional.of(result);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int updateBalance(Long walletId, BigDecimal newBalance) {
        return jdbc.update("UPDATE wallet SET balance_minor = ? WHERE id_wallet = ?", newBalance, walletId);
    }
}


