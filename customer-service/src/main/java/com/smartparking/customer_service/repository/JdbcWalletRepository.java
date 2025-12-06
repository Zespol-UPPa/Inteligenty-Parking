package com.smartparking.customer_service.repository;

import com.smartparking.customer_service.model.Wallet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcWalletRepository implements WalletRepository{
    private final JdbcTemplate jdbc;

    public JdbcWalletRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Wallet> mapper = new RowMapper<>() {
        @Override
        public Wallet mapRow(ResultSet rs, int rowNum) throws SQLException {
            Wallet w = new Wallet();
            w.setId(rs.getLong("wallet_id"));
            w.setBalanceMinor(rs.getBigDecimal("balance_minor"));
            w.setCurrencyCode(rs.getString("currency_code"));
            w.setCustomerId(rs.getLong("customer_id"));
            return w;
        }
    };

    @Override
    public Optional<Wallet> findById(Long id) {
        var list = jdbc.query(
                "SELECT wallet_id, balance_minor, currency_code, customer_id " +
                        "FROM wallet WHERE wallet_id = ?",
                mapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<Wallet> findByCustomerId(Long customerId) {
        var list = jdbc.query(
                "SELECT wallet_id, balance_minor, currency_code, customer_id " +
                        "FROM wallet WHERE customer_id = ?",
                mapper,
                customerId
        );
        return list.stream().findFirst();
    }

    @Override
    public List<Wallet> findAll() {
        return jdbc.query(
                "SELECT wallet_id, balance_minor, currency_code, customer_id FROM wallet",
                mapper
        );
    }

    @Override
    public Wallet save(Wallet wallet) {
        wallet.setCurrencyCode("PLN");
        if (wallet.getId() == null) {
            Long id = jdbc.queryForObject(
                    "INSERT INTO wallet(balance_minor, currency_code, customer_id) " +
                            "VALUES (?, ?, ?) RETURNING wallet_id",
                    Long.class,
                    wallet.getBalanceMinor(),
                    wallet.getCurrencyCode(),
                    wallet.getCustomerId()
            );
            wallet.setId(id);
            return wallet;
        } else {
            jdbc.update(
                    "UPDATE wallet SET balance_minor = ?, currency_code = ?, customer_id = ? " +
                            "WHERE wallet_id = ?",
                    wallet.getBalanceMinor(),
                    wallet.getCurrencyCode(),
                    wallet.getCustomerId(),
                    wallet.getId()
            );
            return wallet;
        }
    }
}

